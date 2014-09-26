/* This class implements the core functionality of middleware and handles the messages and 
 * co-ordinates between communications as well as exit strategy. It also handles the
 * Lamport's logical clock and hence the timestamps as well as the ordering of the messages. 
 * It starts with creating number of sockets connected to other nodes. It then spawns a thread 
 * to handle each of those sockets.
 * Middleware accepts messages from the application layer, stamps them with lamport's logical 
 * time and puts them in queue of each socket thread. It also polls the incoming queue of each 
 * socket thread to pull any received messages. 
 * It processes the messages according to their type. Update messages are put in a priority queue 
 * and acknowledgments are sent for them. Acknowledgment messages are used for updating the 
 * acknowledgment counter of update messages in the queue. It also sends the exit messages to 
 * other queues as well as 'poke' messages to request exiting process to wait.
 * When we are done, it sends exit signals to the socket threads and then exits when cleanup is done.
 * Hence the meethods provided by this class include deliverMessages(), getMessages(), 
 * sendMessages(), createClientSockets(), createServerSockets() etc.
 */

package middleware;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Date;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.*;
import java.net.*;

public class DistributionLayer implements Runnable {

	private int pid;
	private LogicalClock clock;
	private PriorityQueue<Message> queue;
	private Comparator<Message> comp;
	private LinkedBlockingQueue<Message> app2mid;
	private LinkedBlockingQueue<Message> mid2app;
	private final int otherNodes = 2;
	private boolean exitFlag;
	private boolean exitOk;
	private boolean letExit;
	private LinkedList<Double> ackList;
	private LogWriter logger;



	public DistributionLayer (int pid, int step, LinkedBlockingQueue<Message> a2m, LinkedBlockingQueue<Message> m2a, LogWriter l) {
		this.pid = pid;
		clock = new LogicalClock(pid, step);
		comp = new LogicalTimeComparator();
		queue = new PriorityQueue<Message>(50,comp);
		logger = l;
		app2mid = a2m;
		mid2app = m2a;
		exitFlag = false;
		letExit = false;
		exitOk = false;
		ackList = new LinkedList<Double>();
	}





	/* run: This method is the heart of the middleware. It runs in a loop waiting for messages
	 * to send to other nodes and receive messages from other nodes. It handles the communication
	 * as well as the logical clock and the ordering of the messages. It receives the messages and
	 * sends the acknowledgments. It also takes the exit decision. Hence it is by and large the 
	 * most important and the most complicated part of the whole system.
	 */
	public void run() {
		/* here we have to write the logic for creating TCP connections,
		 * Managing the queue, sending messages to other peers and
		 * deliver received messages to the main thread.
		 */

		// part 1: TCP connections
		
		// count how many servers and clients we need to create.
		int numServerSockets = otherNodes - pid;
		int numClientSockets = pid;
		

		/* First accept connections from others. Then we will connect to 
		 * others.
		 */
		Socket[] serverSockets = createServerSockets(numServerSockets);
		//logger.log("[Middleware] Waiting for all to be connected");
		logger.log("Waiting for all to be connected");
		Socket[] clientSockets = createClientSockets(numClientSockets);
		//logger.log("[Middleware] All connected");
		logger.log("All connected");
		int totalThreads = serverSockets.length + clientSockets.length;
		
		// create array of threads which handle our sockets
		SocketThread[] socketRunnables = new SocketThread[totalThreads];
		Thread[] socketThreads = new Thread[totalThreads];
		
		/* Now start the threads to handle each server socket */
		Message temp;
		for (int i = 0; i < serverSockets.length; i++) {
			socketRunnables[i] = new SocketThread(serverSockets[i],logger);
			socketThreads[i] = new Thread(socketRunnables[i]);
			socketThreads[i].start();
			
			/* The thread has some initialization tasks to do before it is functional. It might 
			 * take a little while to initialize. We should wait till it is ready.
			 * The thread sends a message to middleware with sender set to -1 when it is ready to roll.
			 * Wait for this message here.
			 */
			while (true) {
				if ( (temp = socketRunnables[i].getMessage()) != null ) {
					if (temp.getSender() == -1 ) {
						break;
					}
				}
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// nothing
				}
			}
		}
		
		/* do the same thing for client sockets */
		for (int i = 0; i < clientSockets.length; i++) {
			int offset =  serverSockets.length;
			socketRunnables[i + offset] = new SocketThread(clientSockets[i],logger);
			socketThreads[i + offset] = new Thread(socketRunnables[i + offset]);
			socketThreads[i + offset].start();
			while (true) {
				if ( (temp = socketRunnables[i + offset].getMessage()) != null ) {
					if (temp.getSender() == -1 ) {		// wait till coast is clear
						break;
					}
				}
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// nothing
				}
			}
		}
		/* Socket threads indicated that they are ready to function. Same thing is 
		 * true between application and middleware. First part of our task is complete.
		 * Now send a message to the application indicating we are initialized and are
		 * ready to start functioning. Do this by sending a message that has sender set 
		 * to -1. 
		 */
		mid2app.add(new Message('u',clock.getTime(),-1));
		
		
		// Part 2: Message handling and ordering
		
		/* Now is part 2 of the plan. This is related to message handling
		 * We have to accept the messages from middleware and socket threads and put them
		 * in the priority queue. We also need to send acknowledgments to appropriate 
		 * processes. Then if all the acknowledgments for a message are received, we 
		 * need to pop the message and give it to the application layer. 
		 */
		
		while (true) {
						
			// first lets send all the messages in out outbound queue
			
			sendMessages(socketRunnables);
			
			// now check if we have received any new message
			getMessages(socketRunnables);
			
			
			/* Now the next step is, to check if head of the priority queue has all the
			 * acknowledgments received. If so, then we will pop it and give it to the
			 * application 
			 */
			
			deliverMessages();
			
			
			/* The application will generate an interrupt and set our exit flag when it is
			 * done generating all its updates. But we can not be sure that other applications are done 
			 * with the updates too. Also, application layer has no idea if there are any pending
			 * messages in the priority queue.
			 */
			if (Thread.interrupted() && exitFlag) {
				/* Now we have to check if our priority queue is empty. Till the time the queue is
				 * empty, we are definitely not ready to exit.
				 */
				
				if (! queue.isEmpty()) {
					
					exitOk = false;			// we are not ready to exit
					letExit = false;		// we are not OK with other people exiting
					deliverMessages();
					Thread.currentThread().interrupt();
					continue;
				}
				
				/* If our priority queue is empty, we can send message to others asking if it is 
				 * OK to exit.
				 */
				
				/* Here we assume that it is OK for us to exit (we will test that theory soon)
				 * also, we are here means:
				 * 		1. Our update generation is complete
				 * 		2. Our priority queue is empty (all messages delivered)
				 * Hence we can let other people exit and assume that we can exit too. 
				 */
				exitOk = true;
				letExit = true;
				
				// send the exit messages
				clock.increment();
				// System.out.println(getTimestamp() + "[Middleware] Sending exit probe");
				app2mid.add(new Message('e',clock.getTime(),pid));
				sendMessages(socketRunnables);
				
				// wait for it...
				try {
					Thread.sleep(10000);		/* other nodes may be overwhelmed. Give them decent amout of
												 * time to say "No" to our exit request. Only thing to lose here
												 * is that we will wait a little longer. Thats OK.
												 */ 
				} catch (InterruptedException e) {
					/* If we are interrupted, maybe we did not give enough time for other threads to respond
					 * Take no risks. Do one more interation.
					 */
					exitOk = false;
					letExit = false;
					Thread.currentThread().interrupt();
					continue;
				}
				
				// now check if anyone sent is a probbing message which will indicate that we need to wait
				getMessages(socketRunnables);
				
				/* now check if everyone is OK with us exiting. this is done by checking the flag that is
				 * probably un-set by getMessages method
				 */
				if (exitOk && queue.isEmpty()) {
					
					/* Kill all the socket threads */
					killSockets(socketRunnables, socketThreads);
					// System.out.println(getTimestamp() + "[middleware] Exiting now");
					/* Tell application layer that we (this thread) are exiting */
					mid2app.add(new Message('e',0.0,pid));
					return;
				}
				Thread.currentThread().interrupt();
			}
		}
	}






	/* deliverMessages: This method is to deliver the messages to the application layer.
	 * It basically takes a peek at the head of the priority queue. If we have received
	 * enough acknowledgments for that message, then we can deliver that message. If not,
	 * we have to wait. If the head is delivered, then check for the next head too.
	 */
	
	private void deliverMessages() {
		
		while (! queue.isEmpty()) {
			while ( ackList.remove(queue.peek().getTimestamp()) ) {
				
				queue.peek().incrementAck();
				// System.out.println(getTimestamp() + "[dbg] removing ack for " + queue.peek().getTimestamp() + " new counter " + queue.peek().getAcks());
			}
			if (readyToPop()) {
				mid2app.add(queue.poll());
			} else {
				return;
			}
		}
	}






	/* killSockets: This method tells the threads handling the sockets to exit.
	 * This is a little risky task. Unlike the application layer and the middleware,
	 * we do not have any shared data structure here. Hence a thread should not exit
	 * before we pull all the messages. That means it has to wait on us. And if we
	 * rush into message checking and proceed to exit, we will get stuck in the 
	 * situation where child is waiting for us to pull the messages and we are waiting
	 * for it to join.
	 * The join method returns void, hence we cant detect a failure to join there.
	 * otherwise, we could come back and check more messages. Anyway...
	 */

	private void killSockets (SocketThread[] s, Thread[] t) {
		// first, push all our pending messages out.
		sendMessages(s);
		
		for (int i = 0; i < s.length; i++) {
			s[i].setExitFlag();
			t[i].interrupt();
		}
		/* this is risky. if socket thread receives some messages while we call getMessages() 
		 * then it will wait for us to pull that message and we will never do. so add some 
		 * sleep.
		 */
		try {
			Thread.sleep(2000);							// this should be enough (?)
		} catch (InterruptedException e) {
			System.err.println("[ERROR] Interrupted while waiting for socket threads");
		}

		// now check the messages we pulled
		getMessages(s);


		for (int i=0; i < s.length; i++) {
			try {
				t[i].join();
				// System.out.println(getTimestamp() + "[Middleware] Thread " + i + " exited");
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.err.println("[ERROR] Interrupted while waiting for socket thread to exit");
			}
		}
		getMessages(s);
	}






	/* This method is for getting the timestamp. We are not logging anything here, and logger
	 * has its own timestamp, but error messages need this. so, keep it. 
	 */
	private String getTimestamp() {
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a : ");
		String formattedDate = sdf.format(date);
		return formattedDate; // 09/01/2014 4:48:16 PM
	}






	/* readyToPop: This method tells us whether the head of the priority queue is ready to be
	 * delivered to the application layer.
	 */	
	private boolean readyToPop () {
		
		// if this message is not update message, then it is in wrong place
		if (queue.peek().getType() != 'u') {
			System.err.println(getTimestamp() + "[ERROR] Wrong message in the queue. Type: " + queue.peek().getType());
			return true;
		}
		
		// if we had sent this update, and we have received acknowledgment from all others
		if ( (queue.peek().getAcks() == otherNodes ) && (queue.peek().getSender() == pid) ) {
			return true;
			
		// if we received this update, and we received acknowledgment from all others but sender,
		} else if ((queue.peek().getAcks() == otherNodes-1 ) && (queue.peek().getSender() != pid) ) {
			return true;
			
		// if we received this update, and we received acknowledgment from all others, then something is wrong
		} else if ((queue.peek().getAcks() == otherNodes ) && (queue.peek().getSender() != pid) ) {
			System.err.println(getTimestamp() + "[WARNING] Possible wrong ack update");
			return false;
		
		// this means the head is not ready to be popped yet.
		} else {
			return false;
		}
	}






	/* getMessages: This method polls the inbound queues of all the sockets
	 * that we have. 
	 * Depending upon whether the received message is an acknowledgment or a
	 * new update, it takes appropriate action.
	 */
	private void getMessages (SocketThread[] socketRunnables) {
		Message mi;
		
		for (int i=0; i < socketRunnables.length; i++) {
			/* we received a message from a sender. We need to check if it
			 * is acknowledgment or an update or an exit probe.
			 */

			// pull all messages from this socket-thread.
			while ((mi = socketRunnables[i].getMessage()) != null) {
				
				//System.out.println(getTimestamp() + "[Middleware] received '" + mi.getType() + "' message from socket " + i);
				clock.increment();
				 /* If it is an acknowledgment, we need to increment the ack flag
				 * of the message.
				 */
				if (mi.getType() == 'a') {
					/* If this is an acknowledgment, we need to find the message
					 * for which this is an acknowledgment and then increment
					 * that message's acknowledgment count. This is going to be
					 * ugly
					 */
					processAckMessage(mi);
					
				} else if (mi.getType() == 'u') {
					/* If it is an update message, we have to multicast an acknowledgment,
					 * do the clock adjustment if necessary, and then put the message in
					 * the queue
					 */
					processUpdateMessage(mi, socketRunnables);

				} else if (mi.getType() == 'e') {
					/* If it is an exit request, check if we are ready to let other people exit. If so, do nothing.
					 * when the timer of the other node runs off, it will exit.
					 * If we want others to wait, just multicast a poking message asking them to wait.
					 */
					//System.out.println(getTimestamp() + "[Middleware] Process " + mi.getSender() + " asking permission to exit");
					if (! letExit) {
						clock.increment();
						for (int j=0; j < socketRunnables.length; j++) {
							//System.out.println(getTimestamp() + "[Middleware] Requesting process " + mi.getSender() + " to wait");
							Message tempM = new Message('p',clock.getTime(),pid);
							socketRunnables[j].putMessage(tempM);
						}
					} else {
						/* Log saying the particular process has finished. 
						 * Notice that the remaining node may yet ask it to wait, hence this message CAN appear 
						 * multiple times.
						 */
						//System.out.println(getTimestamp() + "[Middleware] Letting process " + mi.getSender() + " exit");
						//System.out.println(getTimestamp() + "[Middleware] P" + mi.getSender() + " finished");
						//logger.log("[Middleware] P" + mi.getSender() + " finished");
						logger.log("P" + mi.getSender() + " finished");
					}
					
				} else if (mi.getType() == 'p') {
					/* If we get a poking message, we need to wait. 
					 * Notice that the pocking message may be for other nodes exit request. But if he needs to wait,
					 * so do we.
					 */
					//System.out.println(getTimestamp() + "[Middleware] Process " + mi.getSender() + " needs us to wait");
					
					exitOk = false;		// this doesnt mean that we do not let others exit :)
				}		
			}
		}
	}






	/* processAckMessage: This method takes an acknowledgment message and does following:
	 * 		1. Finds out what message is the acknowledgment for
	 * 		2. Increment the acknowledgment counter of the message
	 * 		3. If there is no message for which the acknowledgment is received, then we
	 * 		   must wait for it to arrive. Put the acknowledgment in its own linked list.
	 */
		
	private void processAckMessage (Message m) {
		Message temp;
		Iterator<Message> queueIterator = queue.iterator();
		int a = 0;						// counter of messages for this acknowledgment is identified. (Should go to 1)
		
		/* This is not efficient. We have to iterate over entire queue. If queue's 'contains()'
		 * method would have given a way to match a particular field of the object in the queue, we could
		 * have used that and put the message in acknowledgment list knowing that someone is there for it.
		 * For now, this should do. We can remove it altogether (I think) but just to be safe..
		 */
		while (queueIterator.hasNext()) {
			temp = queueIterator.next();
			if (temp.getTimestamp() == m.getAckFor() ) {
				//System.out.println(getTimestamp() + "[Middleware] Updated ack counter for message with sendtime " + temp.getTimestamp() + " ack time: " + m.getTimestamp());
				temp.incrementAck();
				//System.out.println(getTimestamp() + "[Middleware] New counter is " + temp.getAcks());
				a++;
			}
		}
		
		// we might as well process the acknowledgments who reached here before their respective update messages
		deliverMessages();
		
		/* now check how many update messages we matched this acknowledgment to. Lamport's clock should make sure that
		 * the match is always 1.
		 */
		if (a < 1) {
			//System.out.println(getTimestamp() + "[Middleware] Queuing ack for " + m.getAckFor());
			ackList.add(m.getAckFor());
		} else if (a > 1) {
			System.err.println(getTimestamp() + "[WARNING] potential acknowledgment update for multiple messages");
		}
		
	}






	/* processUpdateMessage: This method takes an update message and does following:
	 * 		1. adjust the logical clock if necessary
	 * 		2. Multicast an acknowledgment
	 * 		3. Add the message to the ordered queue 
	 */
		
	private void processUpdateMessage (Message mi, SocketThread[] socketRunnables) {
		
		/* Check if we need to adjust our clock. If so, do it */
		if (clock.getTime() <= mi.getTimestamp()) {
			clock.setTime( Math.ceil(mi.getTimestamp()) + 1);
		}
		
		
		if (mi.getAcks() != 0) {	// received message should not have acknowledgment count anything but 0
			System.err.println("[ERROR] Received message with ack count " + mi.getAcks());
		}
		
		// increment the clock
		clock.increment();
		
		/* Now multicast an acknowledgment */
		for (int j=0; j < socketRunnables.length; j++) {
			//System.out.println(">>> Sending ack for " + mi.getTimestamp() + " ackTime: " + clock.getTime());
			
			Message tempM = new Message('a',clock.getTime(),pid);
			tempM.setAckFor(mi);
			socketRunnables[j].putMessage(tempM);
		}
		
		/* If acknowledgment for this message is already here, we might as well pull it out. */
		while (ackList.remove(mi.getTimestamp())) {	
			mi.incrementAck();
			//System.out.println("removing ack for recent message " + mi.getTimestamp() + " new counter " + mi.getAcks());
		}
		
		//System.out.println(getTimestamp() + "Adding '" + mi.getType() + "'to priority queue");
		queue.add(mi);
		deliverMessages();
		
	}






	/* sendMessages: This method polls the outbound queue and if it
	 * finds a message there, then it puts it outbound queue of every
	 * socket we are handling
	 */
	
	private void sendMessages (SocketThread[] socketRunnables) {
		Message m;
				
		// Check if we have a message to send
		while ((m = app2mid.poll()) != null) {
			
			// set the timestamp on this message
			clock.increment();
			m.setTime(clock.getTime());
			//System.out.println(getTimestamp() + "[Middleware] sending '" + m.getType() + "' message stamped " + m.getTimestamp());
			// and send it to each socket thread
			for (int i=0; i < socketRunnables.length; i++) {
				
				
				/* Create a new message object here. Otherwise, only a reference will be passed. This means,
				 * all the socket threads and the priority queue will have the same object's reference. This
				 * means that if an acknowledgment for this message comes before the remaining socket thread 
				 * can send this message, the acknowledgment counter increment will reflect in that socket thread's 
				 * queue thus resulting in message sent with acknowledgment count 1. This will no doubt baffle
				 * the receiving guy cause it will receive more acknowledgments than it is supposed to receive.
				 * This bug took so many precious hours to debug with the help of log messages :'(
				 */
				Message t = new Message(m.getType(),m.getTimestamp(),m.getSender());
				t.setUpdate(m.getUpdate());
				socketRunnables[i].putMessage(t);
			}
			
			if (m.getType() == 'u') {
				// now put the message in the priority queue
				//System.out.println(getTimestamp() + "Adding '" + m.getType() + "'to priority queue");
				queue.add(m);
			}
			
		}
	}





	/* setExitFlag: Sets exitFlag for this object */
	
	public void setExitFlag () {
		exitFlag = true;
	}






	/* createClientSockets: This method will return the array of sockets
	 * It will connect to 'n' servers and return the array having sockets
	 * to those servers.
	 */
	private Socket[] createClientSockets (int n) {
		Socket[] cliSocks = new Socket[n];
		
		/* open the file info.txt first
		 */
		FileReader f = null;
		BufferedReader in = null;
		
		try {
			f = new FileReader("info.txt");
			in = new BufferedReader(f);
		} catch (IOException e) {
			System.err.println(getTimestamp() + "[ERROR] Error opening the file info.txt");
			e.printStackTrace();
			return null;
		}
		
		/* The logic behind pid is, every process having smaller last digit of ip
		 * address will have smaller pid. So we need to get the our ip to compare
		 * Here we take meaning of 'digit' as 'number' otherwise numbers like 9 and 10
		 * will give us problem.
		 */
		String[] ipFields = null;
		try {
			Socket s = new Socket("google.com", 80);
			ipFields = s.getLocalAddress().getHostAddress().split("\\.");
			s.close();
		} catch (Exception e) {
			System.err.println(getTimestamp() + "[ERROR] Error while getting our own ip");
			e.printStackTrace();
		}
		
		int myIpLastDig = Integer.parseInt(ipFields[3]);
		
		/* Now read the ips one by one. We are supposed to connect to
		 * the machines which will have pids lower than us. meaning last digit of ip lower
		 * than our ip.
		 */
		String line;
		int totalSockets = 0;
		try {
			while ((line = in.readLine()) != null) {
				String[] fields = line.split("\\s+");		// first part is ip, second is port
				ipFields = fields[0].split("\\.");
				
				/* If this machine has last digit larger than us, then we
				 * can skip this one.
				 */
				if (Integer.parseInt(ipFields[3]) >= myIpLastDig ) {
					continue;
				}
				
				/* otherwise create a socket to this machine and put it in the array we will return */
				
				int numAttempts = 10;
				int connFlag = 0;
				while (connFlag < numAttempts) {
					try {
						cliSocks[totalSockets++] = new Socket(fields[0],Integer.parseInt(fields[1]));
						break;
					} catch (ConnectException ce) {
						// probably server is not up.
						connFlag++;
						totalSockets--;
						Thread.sleep(3000);
						if (connFlag == 9) {
							System.err.println("[ERROR] Server connection failed. Check if server is running");
							in.close();
							throw ce;
						}
					}
				}
				//System.out.println(getTimestamp() + "P" + pid + " is connected to (" + fields[0] + ")");
				//logger.log("[Middleware] P" + pid + " is connected to (" + fields[0] + ":" + fields[1] +  ")");
				logger.log("P" + pid + " is connected to (" + fields[0] + ":" + fields[1] +  ")");
			}
			
			in.close();
			if ((totalSockets) != n ) {
				System.err.println(getTimestamp() + "[ERROR] Connected to unexpected number of servers:" + totalSockets);
			}
			
			
		} catch (IOException e) {
			System.err.println(getTimestamp() + "[ERROR] Error reading the file info.txt");
			e.printStackTrace();
			return null;
		} catch (InterruptedException ie) {
			// nothing here
		}
		
		//System.out.println(getTimestamp() + "[Middleware] Connected to " + totalSockets + " number of servers");
		return cliSocks;
	}





	/* createServerSockets: This method takes number of server sockets to
	 * be created and returns the array of those many sockets after
	 * accepting the connections on those.
	 */
	
	private Socket[] createServerSockets (int num) {
		
		FileReader f = null;
		BufferedReader in = null;
		ServerSocket s = null;
		HashMap<String,Integer> ipHash = new HashMap<String,Integer>();
		try {
			s = new ServerSocket(9746);
			PriorityQueue<String> ipQ = new PriorityQueue<String>();
			f = new FileReader("info.txt");
			in = new BufferedReader(f);
			String line;
			while ( (line = in.readLine()) != null ) {
				ipQ.add(line.split("\\s+")[0]);
			}
			in.close();
			String connMsg = null;
			for (int i = 0; ipQ.peek() != null ; i++) {
				if (i == pid) {
					//connMsg = "[Middleware] P" + pid + " (" + ipQ.peek() + ") is listening on port 9746";
					connMsg = "P" + pid + " (" + ipQ.peek() + ") is listening on port 9746";
				}
				ipHash.put(ipQ.peek(), i);
				logger.log("[P" + i + "] " + ipQ.poll());
			}
			logger.log(connMsg);
			/* All this to print those useless and potentially wrong PIDs */
			
			
			//System.out.println(getTimestamp() + "[Middleware] P" + pid + " (" + InetAddress.getLocalHost().toString() + ") is listening on port 9746");
			//logger.log("[Middleware] P" + pid + " (" + InetAddress.getLocalHost().getHostAddress() + ") is listening on port 9746");
			
			
			/* now open the file to print those pid and ip pair. The thing is that PID is given from command line. So it can be
			 * very well be 15 or 15000. Still, as we need to print it before communication (as shown and required in sample logs)
			 * lets assume they start with 0 and go sequentially.
			 */
			
			
		} catch (IOException e) {
			System.err.println(getTimestamp() + "[ERROR] Could not listen to port 9746");
			return null;
		}
				
		Socket[] servSocks = new Socket[num];
		
		for (int i=0; i < num; i++) {
			try {
				/* Accept a connection and put the socket in the array. */
				servSocks[i] = s.accept();
				//int b = i + 1;		// this is sad, but to display the message, I need (i+1). and I cant just put that expression in println.
				//System.out.println(getTimestamp() + "[Middleware] P" + pid + " is connected from "+ b + " process(es)");	// correct this message
				String c = servSocks[i].getInetAddress().toString().replace('/',' ').trim();
				//logger.log("[Middleware] P" + pid + " is connected from P"+ ipHash.get(c)+ " (" + c + ") process(es)");	// correct this message
				logger.log("P" + pid + " is connected from P"+ ipHash.get(c)+ " (" + c + ")");	// correct this message
			} catch (IOException e) {
				System.err.println(getTimestamp() + "[ERROR] Accept failed");
				e.printStackTrace();
				try {
					s.close();
				} catch (IOException e1) {
					System.err.println(getTimestamp() + "[ERROR] Could not close server socket");
					e1.printStackTrace();
					return null;
				}
				return null;
			}
		}
		
		try {
			s.close();
		} catch (IOException e) {
			System.err.println(getTimestamp() + "[ERROR] Could not close server socket");
			e.printStackTrace();
		}
		
		
		//System.out.println(getTimestamp() + "[Middleware] Done creating server sockets");
		return servSocks;
	}
}
