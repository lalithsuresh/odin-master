package net.floodlightcontroller.odinmaster;


/**
 * Base class for all Odin applications. They are
 * expected to run as a thread provided by the master. * 
 * 
 * @author Lalith Suresh <suresh.lalith@gmail.com>
 *
 */
public abstract class OdinApplication implements Runnable {

	protected OdinMaster odinMaster;
	
	/**
	 * Set the OdinMaster to use
	 */
	public void setOdinMaster (OdinMaster om) {
		odinMaster = om;
	}
	
	/**
	 * Needed to wrap OdinApplications into a thread, and is
	 * implemented by the specific application
	 */
	public abstract void run();

}
