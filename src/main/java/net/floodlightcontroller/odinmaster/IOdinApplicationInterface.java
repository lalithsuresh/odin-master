package net.floodlightcontroller.odinmaster;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import net.floodlightcontroller.odinmaster.OdinClient;
import net.floodlightcontroller.util.MACAddress;

public interface IOdinApplicationInterface {

	/**
	 * VAP-Handoff a client to a new AP. This operation is idempotent.
	 * 
	 * @param newApIpAddr IPv4 address of new access point
	 * @param hwAddrSta Ethernet address of STA to be handed off
	 */
	public void handoffClientToAp (MACAddress staHwAddr, InetAddress newApIpAddr);

	/**
	 * Get the list of clients currently registered with Odin
	 * 
	 * @return a map of OdinClient objects keyed by HW Addresses
	 */
	public Map<MACAddress, OdinClient> getClients ();
	
	/**
	 * Get a list of Odin agents from the agent tracker
	 * @return a map of OdinAgent objects keyed by Ipv4 addresses
	 */
	public Map<InetAddress, IOdinAgent> getOdinAgents ();
	
	/**
	 * Add a subscription for a particular event defined by oes. cb is
	 * defines the application specified callback to be invoked during
	 * notification. If the application plans to delete the subscription,
	 * later, the onus is upon it to keep track of the subscription
	 * id for removal later.
	 * 
	 * @param oes the susbcription
	 * @param cb the callback
	 */
	public long registerSubscription (OdinEventSubscription oes, NotificationCallback cb);
	
	/**
	 * Remove a subscription from the list
	 * 
	 * @param id subscription id to remove
	 * @return
	 */
	public void unregisterSubscription (long id);
}
