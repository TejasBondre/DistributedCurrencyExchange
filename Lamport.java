/* This class is the main application thread. It gets an object of currency value. 
 * It also starts the thread for the distribution layer (middleware). 
 * It then runs a loop where it generates random currency update and passes the 
 * message to the middleware. It also polls for the messages passed to it by the 
 * middleware and performs updates in those messages to the currency value.
 */

import middleware.*;

public class Lamport {

	private static int pid;			// id of this process
	private static int iterations;		// number of iterations of currency updates
	private static CurrencyValue curr;
	private static DistributionLayer myDistLayer;





	/* generateUpdate: This method takes in a handle to a Random() obj
	 * and returns an array of 2 ints representing new update value
	 */
	private static int[] generateUpdate (Random r) {
	}





	/* getTimestamp: Method to get current timestamp */
	
	private static String getTimestamp() {
	}





	public static void main (String argv[]) {
		
		/* parse the commandline first */

		/* read input file */

		/* initialize currency values */

		/* use middleware to apply timestamps to queued objects*/

		/* will need separate threads for Lamport clock, connections and queues */
		/* will need synchronization mechanisms */

		/* keep logging everything */

		/* once our primary job is done here, keep crunching updates from middleware */

	}
}
