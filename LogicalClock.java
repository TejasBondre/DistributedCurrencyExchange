/* This class is Lamport's logical clock. It provides methods like increment(), getTime(), 
 * setTime() which are used by middleware to increment the clock after event, get current 
 * time and adjust the clock if necessary.
 */

package middleware;

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
	}

	public void reset () {
	}

	public double getTime () {
	}

	public void setTime (double newTime) {
	}

	private String getTimestamp() {
	}
}
