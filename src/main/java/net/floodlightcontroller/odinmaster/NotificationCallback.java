package net.floodlightcontroller.odinmaster;

import net.floodlightcontroller.odinmaster.NotificationCallbackContext;
import net.floodlightcontroller.odinmaster.OdinEventSubscription;

public interface NotificationCallback {

	/**
	 * The application should implement an anonymous
	 * class for this method, and pass it to the master. Normally,
	 * this should plumb to some existing internal method
	 * implemented by the application.
	 * 
	 * @param oes
	 * @param cntx
	 */
	public void exec(OdinEventSubscription oes, NotificationCallbackContext cntx);
}