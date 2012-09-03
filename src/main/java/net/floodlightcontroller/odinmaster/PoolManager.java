package net.floodlightcontroller.odinmaster;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PoolManager {
	
	static public final String GLOBAL_POOL = "global";
	private final Map<InetAddress, List<String>> agentToPoolListMap = new ConcurrentHashMap<InetAddress, List<String>>();
	
	public List<String> getPoolsForAgent(InetAddress agentInetAddr) {
		if (agentToPoolListMap.containsKey(agentInetAddr))
			return Collections.unmodifiableList(agentToPoolListMap.get(agentInetAddr));
		else
			return new ArrayList<String>();
	}
	
	public void addPoolForAgent(InetAddress agentInetAddr, String pool) {
		if (agentToPoolListMap.containsKey(agentInetAddr)) {
			agentToPoolListMap.get(agentInetAddr).add(pool);
		}
		else {
			List<String> poolList = new ArrayList<String>();
			poolList.add(GLOBAL_POOL);
			poolList.add(pool);
			agentToPoolListMap.put(agentInetAddr, poolList);
		}
	}
}
