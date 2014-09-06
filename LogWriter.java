/* This class is used for writing all the logs in a particular format. 
 */

package middleware;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogWriter {
	private BufferedWriter out;

	public LogWriter (BufferedWriter o) {
		out = o;
	}

	public synchronized void log  (String message) throws Exception {
		out.write(getTimestamp() + message);
		out.newLine();
		out.flush();
	}

	private String getTimestamp() {
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("[ MM/dd H:mm:ss ] : ");
		String formattedDate = sdf.format(date);
		return formattedDate; // 09/01/2014 4:48:16 PM
	}

	public void closeLog() throws IOException {
		out.close();
	}
}
