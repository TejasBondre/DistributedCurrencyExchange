/* This class is runnable. It is spawned by middleware and handles communication on a particular socket. It sends the messages coming from middleware over a socket using
 * ObjectOutputStream() and reads incoming messages using ObjectInputStream().
 */

package middleware;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;


public class SocketThread implements Runnable {
	
	private Socket socket;
	private LinkedBlockingQueue<Message> s2m;
	private LinkedBlockingQueue<Message> m2s;
	private boolean exitFlag = false;
	private LogWriter logger;


	public SocketThread (Socket s, LogWriter l) {
		this.socket = s;
		s2m = new LinkedBlockingQueue<Message>();
		m2s = new LinkedBlockingQueue<Message>();
		logger = l;
	}
	
	
		
	
	private String getTimestamp() {
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a : ");
		String formattedDate = sdf.format(date);
		return formattedDate; // 09/01/2014 4:48:16 PM
	}





	/* The two queues for each socket handling thread are in socket thread object
	 * this means, we need to have two methods to push and pop from the queue of
	 * this thread so that upper layer (middleware) can push and pop from these queues
	 */
	
	public void putMessage (Message m) {
		//System.out.println(getTimestamp() + "Received message stamped " + m.getTimestamp() + " to send on socket");
		if (m.getAcks() != 0) {
			System.err.println("[ERROR] Ack counter is not proper " + m.getAcks());
		}
		m2s.add(m);
	}
	
	public Message getMessage () {
		return s2m.poll();
	}




	
	/* The thread will exit when it is interrupted and this flag is set.
	 * So providing a setter method for the class.
	 */
	public void setExitFlag () {
		exitFlag = true;
	}





	/* The run method of this thread has to poll the queue m2s. If
	 * it finds a message there, it has to send it on the socket. It also
	 * has to receive the message on the socket and put that message into
	 * the queue s2m which is polled by the middle ware. 
	 * After receiving the exit message, the socket thread waits for middle ware
	 * to pull the current messages and then exits.
	 */
	public void run() {
		
		// Phase 1: Initialization
		
		
		// announce to the world that we are here.
		//System.out.println(getTimestamp() + "[Socket " + socket.getLocalPort() + "] Starting socket thread handling socket to " + socket.getInetAddress());
		logger.log("[Socket " + socket.getLocalPort() + "] Starting socket thread handling socket to " + socket.getInetAddress());
		
		
		InputStream in = null;
		ObjectInputStream  oIn = null;
		OutputStream out = null;
		ObjectOutputStream oOut = null;
		
		/* First create streams on the socket so that we are good to go */
		try {
			/* Create the output streams first. This is because the ObjectInputStream class
			 * does a blocking read waiting for a header to be received. ObjectOutputStream
			 * sends this header when created. Hence create output streams first. And yes,
			 * flush them too.
			 */
			out = socket.getOutputStream();
			oOut = new ObjectOutputStream(out);
			oOut.flush();
			in = socket.getInputStream();
			oIn = new ObjectInputStream(in);
			// System.out.println("Done creating the streams on the socket");
			
		} catch (IOException e) {
			System.err.println(getTimestamp() + "[ERROR] IO Error while creating streams on socket");
			e.printStackTrace();
			return;
		}
		
		
		
		/* The read of the sockets are default blocking. This means there is a possibility when
		 * all the nodes are reading and no one is writing thus keeping the application dangling
		 * in the limbo. Better set the timeout so that reads will timeout and we will have chance
		 * to send any pending messages generated in that time.
		 */
		try {
			socket.setSoTimeout(1000);
		} catch (SocketException e) {
			System.err.println(getTimestamp() + "[ERROR] Error setting timeout for read");
			e.printStackTrace();
		}
		
		
		/* now indicate the parent that we are ready to go. This works same as middleware-application
		 * relationship.
		 */
		s2m.add(new Message('u',0.0,-1));
		
		
		
		
		// Phase 2: Start working :)
		
		
		/* This loop is the heart of the socket handling thread. It sends the messages pushed by the 
		 * middle ware on this socket. It also receives the messages sent to this process by the other
		 * node. After getting the exit signal, it waits for middle ware to pull all the messages in
		 * its inbound queue and then exits.
		 */
		while (true) {
			
			Message mo = null;
			Message mi = null;
						
		
			
			/* If we have a message from middleware, then send it
			 */
			while ((mo = m2s.poll()) != null) {
				try {
					/*System.out.println(getTimestamp() + "[Socket " + socket.getLocalPort() + "] Sending a '" + mo.getType() + "' Message stamped " + mo.getTimestamp() 
							+ " ack count " + mo.getAcks());*/
					oOut.writeObject(mo);
					oOut.flush();
		
				
				} catch (IOException e) {
					System.err.println(getTimestamp() + "[Socket " + socket.getLocalPort() + "] Error sending message");
					e.printStackTrace();
				}
			}
			
			/* Else, listen on the port and accept the message object
			 * then put it in the queue to middleware.
			 */
			try {
				mi = (Message) oIn.readObject();
				//System.out.println(getTimestamp() + "[Socket " + socket.getLocalPort() + "] Received a '" + mi.getType() + "' Message stamped " + mi.getTimestamp());


			} catch (ClassNotFoundException ce) {
				System.err.println(getTimestamp() + "[Socket " + socket.getLocalPort() + "] No such class found: Message");
				ce.printStackTrace();
			} catch (SocketTimeoutException te) {
				// nothing to do here. Its expected.
			} catch (EOFException end) {
				//System.err.println(getTimestamp() + "[Socket " + socket.getLocalPort() + "] Received EOF on the socket");
				return;
			} catch (IOException ioe) {
				System.err.println(getTimestamp() + "[Socket " + socket.getLocalPort() + "] Error while reading Message from socket");
				ioe.printStackTrace();
			} 

			// if we received something, then put it in the queue for middleware.
			if (mi != null) {
				//System.out.println(getTimestamp() + "[Socket " + socket.getLocalPort() + "] Adding received message to queue to middleware");
				s2m.add(mi);
			}

			
			if (Thread.interrupted() && exitFlag) {
				
				// send all the out bound messages
				while ( (mo = m2s.poll() ) != null) {
					try {
						//System.out.println(getTimestamp() + "[Socket " + socket.getLocalPort() + "] Sending a '" + mo.getType() + "' Message stamped " + mo.getTimestamp() );
						oOut.writeObject(mo);
			
					
					} catch (IOException e) {
						System.err.println(getTimestamp() + "[Socket " + socket.getLocalPort() + "] Error sending message");
						e.printStackTrace();
					}
				}

				try {
					socket.close();
				} catch (IOException e) {
					System.err.println(getTimestamp() + "[ERROR] Error closing the socket");
				}


				// wait for the middle ware to pull the newly received messages. 
				while (! s2m.isEmpty() ) {
					try {
						//System.out.println(getTimestamp() + "[Socket " + socket.getLocalPort() + "] Waiting for middleware to pull messages");
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// nothing
					}
				}
				// we are done. 
				return;
			}
		}


	}


}
