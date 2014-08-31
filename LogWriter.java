/* This class is used for writing all the logs in a particular format. 
 */

package middleware;

public class LogWriter {

	private BufferedWriter out;

	public LogWriter (BufferedWriter o) {
		out = o;
	}

	public synchronized void log  (String message) {
	}

	private String getTimestamp() {
	}

	public void closeLog() throws IOException {
		out.close();
	}
}
