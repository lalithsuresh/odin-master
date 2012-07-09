package net.floodlightcontroller.odinmaster;


/**
 * Base class for all Odin applications. They are
 * expected to run as a thread provided by the master. * 
 * 
 * @author Lalith Suresh <suresh.lalith@gmail.com>
 *
 */
public abstract class OdinApplication implements Runnable {

	protected IOdinApplicationInterface odinApplicationInterface;
	
	/**
	 * Set the OdinMaster to use
	 */
	public void setOdinInterface (IOdinApplicationInterface om) {
		odinApplicationInterface = om;
	}
	
	/**
	 * Needed to wrap OdinApplications into a thread, and is
	 * implemented by the specific application
	 */
	public abstract void run();

}
