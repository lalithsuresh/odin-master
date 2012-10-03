package net.floodlightcontroller.odin.master;

import java.util.ArrayList;
import java.util.List;

import net.floodlightcontroller.util.MACAddress;

import org.openflow.protocol.OFMessage;

/**
 * This class represents an LVAP that comprises a
 * BSSID and a set of SSIDs on top of it.
 * 
 * @author Lalith Suresh <suresh.lalith@gmail.com>
 *
 */
public class Lvap {
	private final MACAddress lvapBssid;
	private final List<String> lvapSsids;
	private IOdinAgent odinAgent;
	private List<OFMessage> msgList = new ArrayList<OFMessage>();
	
	Lvap(MACAddress bssid, List<String> ssidList) {
		lvapBssid = bssid;
		lvapSsids = ssidList;
	}
	
	protected void setAgent(IOdinAgent agent) {
		this.odinAgent = agent;
	}
	
	// ***** Getters and setters ***** //
	
	public MACAddress getBssid() {
		return lvapBssid;
	}
	
	public List<String> getSsids() {
		return lvapSsids;
	}
	
	public IOdinAgent getAgent() {
		return odinAgent;
	}
	
	public List<OFMessage> getOFMessageList() {
		return msgList;
	}
	
	public void setOFMessageList(List<OFMessage> msglist) {
		this.msgList = msglist;
	}
}
