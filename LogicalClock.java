/* This class is Lamport's logical clock. It provides methods like increment(), getTime(), 
 * setTime() which are used by middleware to increment the clock after event, get current 
 * time and adjust the clock if necessary.
 */

package middleware;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LogicalClock {

	private int pid;
	private int step;
	private double currentTime;

	public LogicalClock (int pid, int step) {
		this.pid = pid;
		this.step = step;
		this.currentTime = this.pid * Math.pow(10, -(String.valueOf(this.pid).length()) );	// this is complicated but general
	}

	public double increment () {
		currentTime += step;
		return currentTime;
	}

	public void reset () {
		System.out.println(getTimestamp() + "[Middleware-clock] Resetting logical clock");
		currentTime = Math.pow(10, -(String.valueOf(this.pid).length()) );
	}

	public double getTime () {
		return currentTime;
	}

	public void setTime (double newTime) {
		double appendTime = this.pid * Math.pow(10, -(String.valueOf(this.pid).length()) );	
		if ( (newTime - Math.floor(newTime)) != appendTime ) {
			newTime = Math.floor(newTime) + appendTime;
		}
		System.out.println(getTimestamp() + "[Middleware-clock] Adjusting clock to " + newTime);
		currentTime = newTime;
	}

	private String getTimestamp() {
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a : ");
		String formattedDate = sdf.format(date);
		return formattedDate; // 09/01/2014 4:48:16 PM
	}
}
