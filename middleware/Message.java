/* This class implements the messages to be sent to and from the sockets. 
 * It has to implement Serializable interface so that we can send the objects 
 * over the socket instead of stuggling with sending and decoding plain text data.
 */

package middleware;

import java.io.Serializable;

public class Message implements Serializable {


	private static final long serialVersionUID = -6489775508455470787L;
	private char messageType;		// whether the message is update or ack
	private double tstamp;			// lamport timestamp of the sender
	private int senderId;			// sendser's Pid
	private int[] update;			// if this is update, we need update vals.
	private double ackFor;			// this will tell us, which message the ack is for
	private int totalAcks;



	public Message (char mType, double t, int id) {
		this.messageType = mType;
		this.tstamp = t;
		this.senderId = id;
		this.ackFor = -1.0;
		this.totalAcks = 0;
	}


	/* Pretty much self-explanatory methods */


	public synchronized void incrementAck () {
		totalAcks++;
	}


	public int getAcks () {
		return totalAcks;
	}
	
	public void setAckFor (Message m) {
		if (messageType != 'a') {
			System.err.println("[ERROR] Acknowledgment is sent with messageType 'a' only");
			return;
		}
		ackFor = m.getTimestamp();
	}

	public double getAckFor () {
		if (messageType != 'a') {
			System.err.println("[ERROR] Acknowledgment-For is valid in messageType 'a' only");
			return -1.0;
		}
		return ackFor;
	}

	public void setUpdate (int[] up) {
		update = new int[2];
		update = up;
	}


	public char getType () {
		return messageType;
	}


	public double getTimestamp () {
		return tstamp;
	}


	public int getSender () {
		return senderId;
	}


	public int[] getUpdate () {
		return update;
	}


	public void setTime (double time) {
		tstamp = time;
	}
}
