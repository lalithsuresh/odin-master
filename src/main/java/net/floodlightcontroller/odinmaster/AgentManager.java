package net.floodlightcontroller.odinmaster;

import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;

public class AgentManager {
	private ConcurrentHashMap<InetAddress, IOdinAgent> agentMap = new ConcurrentHashMap<InetAddress, IOdinAgent> ();
	
	public void receivePing() {
		
	}
	
}