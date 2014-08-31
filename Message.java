/* This class implements the messages to be sent to and from the sockets. 
 * It has to implement Serializable interface so that we can send the objects 
 * over the socket instead of stuggling with sending and decoding plain text data.
 */

package middleware;

import java.io.Serializable;

public class Message implements Serializable {

	public synchronized void incrementAck () {
	}

	public int getAcks () {
	}
	
	public void setAckFor (Message m) {
	}

	public double getAckFor () {
	}

	public void setUpdate (int[] up) {
	}

	public char getType () {
	}

	public double getTimestamp () {
	}

	public int getSender () {
	}

	public int[] getUpdate () {
	}

	public void setTime (double time) {
	}
}
