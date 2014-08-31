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


public class DistributionLayer implements Runnable {

	public DistributionLayer (int pid, int step, LinkedBlockingQueue<Message> a2m, LinkedBlockingQueue<Message> m2a, LogWriter l) {
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
		

		/* First accept connections from others. Then we will connect to 
		 * others.
		 */
		
		// create array of threads which handle our sockets
		
		/* start the threads to handle each server socket */
		for (each server socket) {
			/* The thread has some initialization tasks to do before it is functional. It might 
			 * take a little while to initialize. We should wait till it is ready.
			 * The thread sends a message to middleware with sender set to -1 when it is ready to roll.
			 * Wait for this message here.
			 */
		}
		
		/* do the same thing for client sockets */
		for (each client socket) {
		}

		/* Socket threads indicated that they are ready to function. Same thing is 
		 * true between application and middleware. First part of our task is complete.
		 * Now send a message to the application indicating we are initialized and are
		 * ready to start functioning. Do this by sending a message that has sender set 
		 * to -1. 
		 */
		
		// Part 2: Message handling and ordering
		
		/* Now is part 2 of the plan. This is related to message handling
		 * We have to accept the messages from middleware and socket threads and put them
		 * in the priority queue. We also need to send acknowledgments to appropriate 
		 * processes. Then if all the acknowledgments for a message are received, we 
		 * need to pop the message and give it to the application layer. 
		 */
	}






	/* deliverMessages: This method is to deliver the messages to the application layer.
	 * It basically takes a peek at the head of the priority queue. If we have received
	 * enough acknowledgments for that message, then we can deliver that message. If not,
	 * we have to wait. If the head is delivered, then check for the next head too.
	 */
	
	private void deliverMessages() {
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
	}






	/* This method is for getting the timestamp. We are not logging anything here, and logger
	 * has its own timestamp, but error messages need this. so, keep it. 
	 */
	private String getTimestamp() {
	}






	/* readyToPop: This method tells us whether the head of the priority queue is ready to be
	 * delivered to the application layer.
	 */	
	private boolean readyToPop () {
	}






	/* getMessages: This method polls the inbound queues of all the sockets
	 * that we have. 
	 * Depending upon whether the received message is an acknowledgment or a
	 * new update, it takes appropriate action.
	 */
	private void getMessages (SocketThread[] socketRunnables) {
	}






	/* processAckMessage: This method takes an acknowledgment message and does following:
	 * 		1. Finds out what message is the acknowledgment for
	 * 		2. Increment the acknowledgment counter of the message
	 * 		3. If there is no message for which the acknowledgment is received, then we
	 * 		   must wait for it to arrive. Put the acknowledgment in its own linked list.
	 */
	private void processAckMessage (Message m) {
	}






	/* processUpdateMessage: This method takes an update message and does following:
	 * 		1. adjust the logical clock if necessary
	 * 		2. Multicast an acknowledgment
	 * 		3. Add the message to the ordered queue 
	 */
		
	private void processUpdateMessage (Message mi, SocketThread[] socketRunnables) {
	}






	/* sendMessages: This method polls the outbound queue and if it
	 * finds a message there, then it puts it outbound queue of every
	 * socket we are handling
	 */
	
	private void sendMessages (SocketThread[] socketRunnables) {
	}




	/* createClientSockets: This method will return the array of sockets
	 * It will connect to 'n' servers and return the array having sockets
	 * to those servers.
	 */
	private Socket[] createClientSockets (int n) {
	}





	/* createServerSockets: This method takes number of server sockets to
	 * be created and returns the array of those many sockets after
	 * accepting the connections on those.
	 */
	
	private Socket[] createServerSockets (int num) {
	}
}
