/* This class is runnable. It is spawned by middleware and handles communication on a particular socket. It sends the messages coming from middleware over a socket using
 * ObjectOutputStream() and reads incoming messages using ObjectInputStream().
 */

package middleware;

public class SocketThread implements Runnable {
	
	private Socket socket;
	private LinkedBlockingQueue<Message> s2m;
	private LinkedBlockingQueue<Message> m2s;
	private LogWriter logger;


	public SocketThread (Socket s, LogWriter l) {
		this.socket = s;
		s2m = new LinkedBlockingQueue<Message>();
		m2s = new LinkedBlockingQueue<Message>();
		logger = l;
	}
	
	
		
	
	private String getTimestamp() {
	}





	/* The two queues for each socket handling thread are in socket thread object
	 * this means, we need to have two methods to push and pop from the queue of
	 * this thread so that upper layer (middleware) can push and pop from these queues
	 */
	
	public void putMessage (Message m) {
	}
	
	public Message getMessage () {
	}




	// need to invent some exit condition



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
		
		
		// Phase 2: Start working :)
		
		
		/* This loop is the heart of the socket handling thread. It sends the messages pushed by the 
		 * middle ware on this socket. It also receives the messages sent to this process by the other
		 * node. After getting the exit signal, it waits for middle ware to pull all the messages in
		 * its inbound queue and then exits.
		 */
		while (true) {
		}


	}


}
