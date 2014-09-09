/* This class is for the currency value which is initialized to (100,100). 
 * This class provides methods like updateValue() and getValue() which update the 
 * currency value by given delta and get the current currency value respectively.
 */

import middleware.*;

import java.text.SimpleDateFormat;
import java.util.Date;


public class CurrencyValue {

	private int sellRate;
	private int buyRate;
	private LogWriter logger;
	private static int updateCounter;

	public CurrencyValue (LogWriter l) {
		this.buyRate = 100;
		this.sellRate = 100;
		logger = l;
		updateCounter = 0;
	}

	public void updateValue (int update[], double t) {
		if (update.length != 2) {
			System.err.println(getTimestamp() + "[ERROR] Received " + update.length + " values to update");
			return;
		}
		sellRate += update[0];
		buyRate += update[1];
		//System.out.println(getTimestamp() + "[App-currency] Currency value is set to (" + sellRate + "," + buyRate + ") by (" + update[0] + "," + update[1] + ")");
		//logger.log("[App-currency][OP" + updateCounter + " : C" + t + "] Currency value is set to (" + sellRate + "," + buyRate + ") by (" + update[0] + "," + update[1] + ")");
		logger.log("[OP" + updateCounter + " : C" + t + "] Currency value is set to (" + sellRate + "," + buyRate + ") by (" + update[0] + "," + update[1] + ")");
		updateCounter++;
		return;
	}

	public int[] getValue () {
		int[] currVal = new int[2];
		currVal[0] = sellRate;
		currVal[1] = buyRate;
		return currVal;
	}


	private String getTimestamp() {
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a : ");
		String formattedDate = sdf.format(date);
		return formattedDate; // 09/01/2014 4:48:16 PM
	}
}
