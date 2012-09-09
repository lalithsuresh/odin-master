package net.floodlightcontroller.odinmaster;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import net.floodlightcontroller.util.MACAddress;

public class PoolManager {
	
	static public final String GLOBAL_POOL = "global";
	private final byte[] oui = {(byte) 0x00, (byte) 0x1b, (byte) 0xb3};
	private final Map<InetAddress, List<String>> agentToPoolListMap = new ConcurrentHashMap<InetAddress, List<String>>();
	private final Map<String, Set<String>> poolToSsidListMap = new ConcurrentHashMap<String, Set<String>>();
	private final Map<String, Set<OdinClient>> poolToClientSetMap = new ConcurrentHashMap<String, Set<OdinClient>> ();
	private final Map<OdinClient, String> clientToPoolMap = new ConcurrentHashMap<OdinClient, String>();
	private int numNetworks = 0;
	
	public PoolManager () {
		poolToSsidListMap.put(GLOBAL_POOL, new TreeSet<String>());
		poolToClientSetMap.put(GLOBAL_POOL, new TreeSet<OdinClient>());
	}
		
	public List<String> getPoolsForAgent(InetAddress agentInetAddr) {
		if (agentToPoolListMap.containsKey(agentInetAddr))
			return Collections.unmodifiableList(agentToPoolListMap.get(agentInetAddr));
		else
			return Collections.<String>emptyList();
	}
	
	public void addPoolForAgent(InetAddress agentInetAddr, String pool) {
		if (agentToPoolListMap.containsKey(agentInetAddr)) {
			agentToPoolListMap.get(agentInetAddr).add(pool);
		}				
		else {
			List<String> poolList = new ArrayList<String>();
			poolList.add(pool);
			agentToPoolListMap.put(agentInetAddr, poolList);
		}
		
		if (!poolToClientSetMap.containsKey(pool)) {
			poolToClientSetMap.put(pool, new TreeSet<OdinClient>());
			poolToSsidListMap.put(pool, new TreeSet<String>());
		}
	}
	
	public Set<String> getSsidListForPool(String pool){
		if (!poolToSsidListMap.containsKey(pool)) {
			return Collections.emptySet();
		}
		return Collections.unmodifiableSet(poolToSsidListMap.get(pool));
	}
	
	public boolean addNetworkForPool(String pool, String ssid) {
		assert (pool != GLOBAL_POOL);
		/*
		 * First add to the global pool. If this addition
		 * is successful, it follows that the SSID is unique,
		 * and can be added to the specified pool as well.
		 */
		if (poolToSsidListMap.get(GLOBAL_POOL).add(ssid)) {
			poolToSsidListMap.get(pool).add(ssid);
			numNetworks++;
			return true;
		}
		
		return false;
	}
	
	public boolean removeNetworkFromPool(String pool, String ssid) {
		assert (pool != GLOBAL_POOL);
		/*
		 * First remove from local pool. If that succeeds, remove
		 * from the global pool as well.		 * 
		 */
		if (poolToSsidListMap.get(pool).remove(ssid)) {
			poolToSsidListMap.get(GLOBAL_POOL).remove(ssid);
			numNetworks--;
			return true;
		}
		
		return false;
	}
	
	public MACAddress generateBssidForClient(MACAddress clientHwAddress) {
		byte[] bssidBytes = clientHwAddress.toBytes();
		bssidBytes[0] = oui[0];
		bssidBytes[1] = oui[1];
		bssidBytes[2] = oui[2];
		final MACAddress bssid = MACAddress.valueOf(bssidBytes);
		
		return bssid;
	}
	
	public int getNumNetworks() {
		return numNetworks;
	}
	
	public void mapClientToPool(OdinClient client, String pool) {
		assert (pool != null);
		assert (pool != GLOBAL_POOL);
						
		String currentPool = clientToPoolMap.put(client, pool);
				
		if (currentPool != null) {
			poolToClientSetMap.get(currentPool).remove(client);
		}
		
		poolToClientSetMap.get(GLOBAL_POOL).add(client);
		poolToClientSetMap.get(pool).add(client);
	}
	
	public void removeClientPoolMapping(OdinClient client) {
		String currentPool = clientToPoolMap.get(client);
		
		if (currentPool != null) {
			poolToClientSetMap.get(currentPool).remove(client);
			clientToPoolMap.remove(client);
		}
	}
	
	public Set<OdinClient> getClientsFromPool(String pool) {
		return Collections.unmodifiableSet(poolToClientSetMap.get(pool));
	}
	
	public String getPoolForClient(OdinClient client) {
		return clientToPoolMap.get(client);
	}
}