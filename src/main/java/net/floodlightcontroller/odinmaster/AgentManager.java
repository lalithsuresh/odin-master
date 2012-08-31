package net.floodlightcontroller.odinmaster;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.odinmaster.IOdinAgent;
import net.floodlightcontroller.odinmaster.OdinAgentFactory;
import net.floodlightcontroller.odinmaster.OdinClient;
import net.floodlightcontroller.odinmaster.OdinMaster;


public class AgentManager {
	private final ConcurrentHashMap<String, ConcurrentHashMap<InetAddress, IOdinAgent>> agentMap = new ConcurrentHashMap<String, ConcurrentHashMap<InetAddress,IOdinAgent>>();
    protected static Logger log = LoggerFactory.getLogger(OdinMaster.class);
    
    private IFloodlightProviderService floodlightProvider;
    private final ClientManager clientManager;
    private final PoolManager poolManager;
    
	private final Timer failureDetectionTimer = new Timer();
	private int agentTimeout = 6000;
	
	private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock ();
	private final Lock readLock = readWriteLock.readLock();
	private final Lock writeLock = readWriteLock.writeLock();

	public AgentManager (ClientManager clientManager, PoolManager poolManager) {
		this.clientManager = clientManager;
		this.poolManager = poolManager;
	}
 
    public void setFloodlightProvider(final IFloodlightProviderService provider) {
    	floodlightProvider = provider;
    }
    
    
    public void setAgentTimeout (final int timeout) {
    	assert (timeout > 0);
    	agentTimeout = timeout;
    }
    
    
    /**
	 * Confirm if the agent corresponding to an InetAddress
	 * is being tracked.
	 * 
	 * @param odinAgentInetAddress
	 * @return true if the agent is being tracked
	 */
	public boolean isTracked(final InetAddress odinAgentInetAddress) {
		readLock.lock();
		try {
			return agentMap.containsKey(PoolManager.GLOBAL_POOL) &&
					agentMap.get(PoolManager.GLOBAL_POOL).containsKey(odinAgentInetAddress);
		}
		finally {
			readLock.unlock();
		}
	}
	
	
	/**
	 * Get the list of agents being tracked for a particular pool
	 * @return agentMap
	 */
	public Map<InetAddress, IOdinAgent> getAgents(final String pool) {
		readLock.lock();
		try {
			if (agentMap.containsKey(pool)) {
				return Collections.unmodifiableMap(agentMap.get(pool));
			}
			else {
				return new HashMap<InetAddress, IOdinAgent>();
			}
		}
		finally {
			readLock.unlock();
		}
	}
	
	
	/**
	 * Get a reference to an agent
	 * 
	 * @param agentInetAddr
	 */
	public IOdinAgent getAgent(final InetAddress agentInetAddr) {
		assert (agentInetAddr != null);
		readLock.lock();
		try {
			final IOdinAgent agentRef = agentMap.get(PoolManager.GLOBAL_POOL).get(agentInetAddr);
			return agentRef;
		}
		finally {
			readLock.unlock();
		}
	}
	
	
	/**
	 * Removes an agent from the agent manager
	 * 
	 * @param agentInetAddr
	 */
	public void removeAgent(InetAddress agentInetAddr) {
		writeLock.lock();
		
		try {
			for (String pool: poolManager.getPoolsForAgent(agentInetAddr)) {
				agentMap.get(pool).remove(agentInetAddr);
			}
		}
		finally {
			writeLock.unlock();
		}
	}
	
	// Handle protocol messages here
	
	/**
     * Handle a ping from an agent. If an agent was added to the
     * agent map, return true.
     * 
     * @param odinAgentAddr
     * @return true if an agent was added
     */
	public boolean receivePing(final InetAddress odinAgentAddr) {
		log.info("Ping message from: " + odinAgentAddr);
		
		// If this is the first time we're hearing from this
    	// agent, then keep track of it.
    	if (odinAgentAddr != null && !isTracked (odinAgentAddr)) {
    		
    		// If the OFSwitch corresponding to the agent has already
    		// registered here, then set it in the OdinAgent object.
    		// Else, we'll handle it when the switch eventually registers.
    		for (IOFSwitch sw: floodlightProvider.getSwitches().values()) {
    			
    			// We're binding by IP addresses now, because we want to pool
    			// an OFSwitch with its corresponding OdinAgent, if any.
    			String switchIpAddr = ((InetSocketAddress) sw.getChannel().getRemoteAddress()).getAddress().getHostAddress();
    			if (switchIpAddr.equals(odinAgentAddr.getHostAddress())) {
    				
    				IOdinAgent oa = OdinAgentFactory.getOdinAgent();
    				oa.setSwitch(sw);
            		oa.init(odinAgentAddr);
    				oa.setLastHeard(System.currentTimeMillis());
            		
            		// It is possible that the controller is recovering from a failure,
            		// so query the agent to see what VAPs it hosts, and add them
            		// to our client tracker accordingly.
            		for (OdinClient client: oa.getLvapsRemote()) {
            			
            			OdinClient trackedClient = clientManager.getClients().get(client.getMacAddress());
            			
            			if (trackedClient == null){
            				clientManager.addClient(client.getMacAddress(), client.getIpAddress(),
            									client.getLvap());
            				trackedClient = clientManager.getClients().get(client.getMacAddress());
            			}	
            			
            			if (trackedClient.getLvap().getAgent() == null) {
            				trackedClient.getLvap().setAgent(oa);
            			}
            			else if (!trackedClient.getLvap().getAgent().getIpAddress().equals(odinAgentAddr)) {
                			// Race condition: 
                			// - client associated at AP1 before the master failure,
                			// - master crashes.
                			// - master re-starts, AP2 connects to the master first.
                			// - client scans, master assigns it to AP2.
                			// - AP1 now joins the master again, but it has the client's LVAP as well.
                			// - Master should now clear the LVAP from AP1.
            				oa.removeClientLvap(client);
            			}
            		}
            		
            		writeLock.lock();
            		try {
            			for (String pool: poolManager.getPoolsForAgent(odinAgentAddr)) {
            				if (!agentMap.containsKey(pool)) {
            					agentMap.put(pool, new ConcurrentHashMap<InetAddress, IOdinAgent>());
            				}
            				agentMap.get(pool).put(odinAgentAddr, oa);
            			}
            		}
            		finally {
            			writeLock.unlock();
            		}
            		
            		log.info("Adding OdinAgent to map: " + odinAgentAddr.getHostAddress());
            		
            		// This TimerTask checks the lastHeard value
            		// of the agent in order to handle failure detection
            		failureDetectionTimer.scheduleAtFixedRate(new OdinAgentFailureDetectorTask(oa), 1, agentTimeout/2);
            		
    				return true;
    			}
    		}
    	}
    	
    	return false;
	}
	
	
	private class OdinAgentFailureDetectorTask extends TimerTask {
		private final IOdinAgent agent;
		
		public OdinAgentFailureDetectorTask (final IOdinAgent oa){
			this.agent = oa;
		}
		
		@Override
		public void run() {
			log.info("Executing failure check against: " + agent.getIpAddress());
			if ((System.currentTimeMillis() - agent.getLastHeard()) >= agentTimeout) {
				log.error("Agent: " + agent.getIpAddress() + " has timed out");
				
				/* This is default behaviour, maybe we should
				 * re-assign the client based on some specific
				 * behaviour
				 */
				
				// TODO: There should be a way to lock the master
				// during such operations	
				for (OdinClient oc: agent.getLvapsLocal()) {
					clientManager.getClients().get(oc.getMacAddress()).getLvap().setAgent(null);
				}
				
				// Agent should now be cleared out
				removeAgent(agent.getIpAddress());
				this.cancel();
			}
		}
		
	}

}