package net.floodlightcontroller.odinmaster;

import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.floodlightcontroller.odinmaster.OdinClient;
import net.floodlightcontroller.util.MACAddress;

public class ClientManager {
	private ConcurrentMap<MACAddress, OdinClient> odinClientMap = new ConcurrentHashMap<MACAddress, OdinClient> ();

	
	/**
	 * Add a client to the client tracker
	 * 
	 * @param hwAddress Client's hw address
	 * @param ipv4Address Client's IPv4 address
	 * @param vapBssid Client specific VAP bssid
	 * @param vapEssid Client specific VAP essid
	 */
	public void addClient (MACAddress hwAddress, InetAddress ipv4Address, MACAddress vapBssid, String vapEssid) {
		odinClientMap.put(hwAddress, new OdinClient (hwAddress, ipv4Address, vapBssid, vapEssid));
	}
	
	
	/**
	 * Add a client to the client tracker
	 * 
	 * @param hwAddress Client's hw address
	 * @param ipv4Address Client's IPv4 address
	 * @param vapBssid Client specific VAP bssid
	 * @param vapEssid Client specific VAP essid
	 */
	public void addClient (OdinClient oc) {
		odinClientMap.put(oc.getMacAddress(), oc);
	}
	
	
	/**
	 * Removes a client from the tracker
	 * 
	 * @param hwAddress Client's hw address
	 */
	public void removeClient (MACAddress hwAddress) {
		odinClientMap.remove(hwAddress);
	}
	
	
	/**
	 * Get a client by hw address
	 */
	public OdinClient getClient (MACAddress hwAddress) {
		return odinClientMap.get(hwAddress);
	}
	
	
	/**
	 * Get the client Map from the manager
	 * @return client map
	 */
	public ConcurrentMap<MACAddress, OdinClient> getClients () {
		return odinClientMap;
	}
}
