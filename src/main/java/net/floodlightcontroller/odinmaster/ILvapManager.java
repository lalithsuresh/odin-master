package net.floodlightcontroller.odinmaster;

import java.net.InetAddress;
import java.util.List;

import org.openflow.protocol.OFMessage;

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
	public OdinClient assignLvapWithNullIp(MACAddress clientHwAddress);
	
	
	/**
	 * Add an SSID to the Odin network.
	 * 
	 * @param networkName
	 * @return true if the network could be added, false otherwise
	 */
	public boolean addNetwork (String ssid);
	
	
	/**
	 * Remove an SSID from the Odin network.
	 * 
	 * @param networkName
	 * @return true if the network could be removed, false otherwise
	 */
	public boolean removeNetwork (String ssid);
	
	
	/**
	 * Returns the number of networks that have been registered
	 * 
	 * @return number of SSIDs registered
	 */
	public int getNumNetworks ();
	
	
	/**
	 * Get a default list of OFMod messages to associate
	 * with the LVAP
	 * 
	 * @param inetAddr
	 * @return a list of OFMod messages
	 */
	public List<OFMessage> getDefaultOFModList(InetAddress inetAddr);
}