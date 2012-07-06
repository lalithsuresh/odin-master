package net.floodlightcontroller.odinmaster;

import net.floodlightcontroller.util.MACAddress;

public interface ILvapManager {
	
	/**
	 * Get an LVAP to be assigned for a given client. The
	 * class that implements this interface is free to
	 * spawn a new LVAP for each request or maintain a pool
	 * of LVAPs.
	 * 
	 * @param clientHwAddress
	 * @return an OdinClient
	 */
	public OdinClient getLvap(MACAddress clientHwAddress);
}