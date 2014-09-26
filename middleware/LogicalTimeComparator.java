/* This class implements Comparator interface to provide a comparator for Message objects. 
 * The comparison is done based on the timestamp of the Message object. This is used as the 
 * comparator of PriorityQueue used by the middleware.
 */

package middleware;

import java.util.Comparator;

public class LogicalTimeComparator implements Comparator<Message> {

	public int compare (Message a, Message b) {
		if (a.getTimestamp() < b.getTimestamp() ) {
			return -1;
		} else if (a.getTimestamp() > b.getTimestamp() ) {
			return 1;
		}
		return 0;
	}

}
