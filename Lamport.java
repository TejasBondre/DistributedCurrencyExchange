/* This class is the main application thread. It gets an object of currency value. 
 * It also starts the thread for the distribution layer (middleware). 
 * It then runs a loop where it generates random currency update and passes the 
 * message to the middleware. It also polls for the messages passed to it by the 
 * middleware and performs updates in those messages to the currency value.
 */

import middleware.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.*;

public class Lamport {

	private static int pid;			// id of this process
	private static int iterations;		// number of iterations of currency updates
	private static CurrencyValue curr;
	private static DistributionLayer myDistLayer;





	/* generateUpdate: This method takes in a handle to a Random() obj
	 * and returns an array of 2 ints representing new update value
	 */
	private static int[] generateUpdate (Random r) {
		int[] update = new int[2];
		
		update[0] = r.nextInt(160) - 80;	// to generate random value between -80 and +80
		update[1] = r.nextInt(160) - 80;
		//System.out.println(getTimestamp() + "[App] Generated random update: " + update[0] + " " + update[1]);
		return update;
	}





	/* getTimestamp: Method to get current timestamp */
	
	private static String getTimestamp() {
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a : ");
		String formattedDate = sdf.format(date);
		return formattedDate; // 09/01/2014 4:48:16 PM
	}





	public static void main (String argv[]) {
		
		/* parse the commandline first */
		pid = Integer.parseInt(argv[0]);
		iterations = Integer.parseInt(argv[1]);
		int clock_rate = Integer.parseInt(argv[2]);

		String filename = "log" + pid;
		FileWriter f = null;
		BufferedWriter out = null;
		LogWriter logger = null;
		try {
			f = new FileWriter(filename);
			out = new BufferedWriter(f);
			logger = new LogWriter(out);
			
		} catch (IOException e) {
			System.err.println(getTimestamp() + "[ERROR] Could not open log file");
			e.printStackTrace();
			return;
		}
		curr = new CurrencyValue(logger);

		/* Now we need an object for middleware which will start the lamport
		 * logical clock, set up the connections and handle the queue.
		 * As we need to set up the connections according to PID, we need to
		 * give the middleware our pid and the clock rate. We also need two
		 * synchronized queues for consumer-producer interactions
		 */

		LinkedBlockingQueue<Message> appToMid = new LinkedBlockingQueue<Message>();
		LinkedBlockingQueue<Message> midToApp = new LinkedBlockingQueue<Message>();
		myDistLayer = new DistributionLayer(pid, clock_rate, appToMid, midToApp, logger);

		/* We need to have a separate thread running which will take care of the
		 * logical clock, the message queue and the connections (which in turn
		 * may need more threads). This thread is going to be complicated, but it
		 * should not be much of an issue as there is not much we need to share and
		 * sync
		 */

		//System.out.println(getTimestamp() + "[App] Starting the middleware");
		logger.log("Starting the middleware");
		//logger.log("[App] Starting the middleware");
		Thread distThread = new Thread(myDistLayer);
		distThread.start();

		Message t;
		while (true) {
			if ((t = midToApp.poll()) != null) {
				if (t.getSender() == -1) {
					break;
				}
			}
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// nothing
			}
		}

		Random r = new Random();
		int sleepTime;

		for (int i=0; i < iterations; i++) {

			//System.out.println("------- Iteration: " + i + " --------");

			// sleep random amount of time
			sleepTime = r.nextInt(1000);
			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				// nothing we can do except printing stack and ignoring.
				System.err.println(getTimestamp() + "[ERROR] Unexpected interuption to the application");
				e.printStackTrace();
			}


			// Now generate random update

			int[] updateVal = generateUpdate(r);
			Message m = new Message('u',0.0,pid);
			m.setUpdate(updateVal);

			// put it in the queue
			appToMid.add(m);

			while (! midToApp.isEmpty()) {
				Message temp = midToApp.poll();
				curr.updateValue(temp.getUpdate(),temp.getTimestamp());
			}
		}

		/* We are here means that we have completed generation of all the updates.
		 * This however does not mean that we have completed all the currency updates.
		 * This means we don't have anything to send now. Tell this to the middle-ware.
		 */
		//System.out.println(getTimestamp() + "[App] Done with update generation");
		myDistLayer.setExitFlag();
		distThread.interrupt();

		/* Now we have keep crunching whatever middle-ware feeds us until it sends a 
		 * message having type 'e'
		 */

		while (true) {
			if (! midToApp.isEmpty()) {
				Message m = midToApp.poll();
				if (m.getType() == 'e') {
					break;
				} else if (m.getType() == 'u') {
					curr.updateValue(m.getUpdate(),m.getTimestamp());
				} else {
					System.err.println(getTimestamp() + "[ERROR] Message type '" + m.getType() + "' is not expected in application layer");
				}

			}
			try {
				//System.out.println(getTimestamp() + "[App] Waiting for others to finish generating updates");
				Thread.sleep(2000);
			} catch (InterruptedException e) {

			}
		}

		//System.out.println(getTimestamp() + "Final currency value is [" + curr.getValue()[0] + "," + curr.getValue()[1] + "]");
		logger.log("Final currency value is [" + curr.getValue()[0] + "," + curr.getValue()[1] + "]");
		try {
			logger.closeLog();
		} catch (IOException e) {
			System.err.println(getTimestamp() + "[ERROR] Error closing the log file");
		}

	}
}
