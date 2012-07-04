package net.floodlightcontroller.odinmaster;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.odinmaster.IOdinAgent;
import net.floodlightcontroller.odinmaster.OdinAgentFactory;
import net.floodlightcontroller.odinmaster.OdinClient;
import net.floodlightcontroller.odinmaster.OdinMaster;


public class AgentManager {
	private ConcurrentHashMap<InetAddress, IOdinAgent> agentMap = new ConcurrentHashMap<InetAddress, IOdinAgent> ();
    protected static Logger log = LoggerFactory.getLogger(OdinMaster.class);
    private IFloodlightProviderService floodlightProvider;
    private ClientManager clientManager;
	private Timer failureDetectionTimer = new Timer();
	private final int AGENT_TIMEOUT = 6000;

 
    public void setFloodlightProvider(IFloodlightProviderService provider) {
    	floodlightProvider = provider;
    }
    
    /**
     * Handle a ping from an agent. If an agent was added to the
     * agent map, return true.
     * 
     * @param odinAgentAddr
     * @return true if an agent was added
     */
	public boolean receivePing(InetAddress odinAgentAddr) {
		log.info("Ping message from: " + odinAgentAddr);
		
		// If this is the first time we're hearing from this
    	// agent, then keep track of it.
    	if (odinAgentAddr != null && !agentMap.containsKey (odinAgentAddr))
    	{
    		// If the OFSwitch corresponding to the agent has already
    		// registered here, then set it in the OdinAgent object.
    		// Else, we'll handle it when the switch eventually registers.
    		for (IOFSwitch sw: floodlightProvider.getSwitches().values())
    		{
    			// We're binding by IP addresses now, because we want to pool
    			// an OFSwitch with its corresponding OdinAgent, if any.
    			String switchIpAddr = ((InetSocketAddress) sw.getChannel().getRemoteAddress()).getAddress().getHostAddress();
    			if (switchIpAddr.equals(odinAgentAddr.getHostAddress())){
    				
    				IOdinAgent oa = OdinAgentFactory.getOdinAgent();
            		oa.init(odinAgentAddr);
    				oa.setSwitch(sw);
    				oa.setLastHeard(System.currentTimeMillis());
            		
            		// It is possible that the controller is recovering from a failure,
            		// so query the agent to see what VAPs it hosts, and add them
            		// to our client tracker accordingly.
            		for (OdinClient client: oa.getLvapsRemote()) {
            			
            			if (clientManager.getClients().get(client.getMacAddress()) == null){
            				clientManager.addClient(client.getMacAddress(), client.getIpAddress(),
            									client.getLvapBssid() , client.getLvapSsid());
            			}	
            			
            			if (clientManager.getClients().get(client.getMacAddress()).getOdinAgent() == null) {
            				clientManager.getClients().get(client.getMacAddress()).setOdinAgent(oa);
            			}
            			else {
                			// Race condition: 
                			// - client associated at AP1 before the master failure,
                			// - master crashes.
                			// - master re-starts, AP2 connects to the master first.
                			// - client scans, master assigns it to AP2.
                			// - AP1 now joins the master again, but it has the client's LVAP as well.
                			// - Master should now clear the LVAP from AP1.
            				oa.removeLvap(client);
            			}
            		}
            		
            		agentMap.put(odinAgentAddr, oa);
            		log.info("Adding OdinAgent to map: " + odinAgentAddr.getHostAddress());
            		
            		// TODO: push subscriptions in face of failure
            		//agentMap.pushSubscriptionListToAgent(oa);
            		
            		// This TimerTask checks the lastHeard value
            		// of the agent in order to handle failure detection
            		failureDetectionTimer.scheduleAtFixedRate(new OdinAgentFailureDetectorTask(oa), 1, AGENT_TIMEOUT);
            		
    				return true;
    			}
    		}
    	}
    	else if (agentMap.containsKey (odinAgentAddr)) {
    		agentMap.get(odinAgentAddr).setLastHeard(System.currentTimeMillis());
    	}
    	
    	return false;
	}
	
	
	/**
	 * Confirm if the agent corresponding to an InetAddress
	 * is being tracked.
	 * 
	 * @param odinAgentInetAddress
	 * @return true if the agent is being tracked
	 */
	public boolean isTracked(InetAddress odinAgentInetAddress) {
		return agentMap.containsKey(odinAgentInetAddress);
	}

	/**
	 * set the client manager
	 * @param clientManager
	 */
	public void setClientManager(ClientManager clientManager) {
		this.clientManager = clientManager;
	}
	
	private class OdinAgentFailureDetectorTask extends TimerTask {
		private IOdinAgent agent;
		
		public OdinAgentFailureDetectorTask (IOdinAgent oa){
			this.agent = oa;
		}
		
		@Override
		public void run() {
			log.info("Executing failure check against: " + agent.getIpAddress());
			if ((System.currentTimeMillis() - agent.getLastHeard()) >= AGENT_TIMEOUT) {
				log.error("Agent: " + agent.getIpAddress() + " has timed out");
				
				/* This is default behaviour, maybe we should
				 * re-assign the client based on some specific
				 * behaviour
				 */
				
				// TODO: There should be a way to lock the master
				// during such operations
				for (OdinClient oc: agent.getLvapsLocal()) {
					oc.setOdinAgent(null);
				}
				
				// Client should now be flushed out
				agentMap.remove(agent.getIpAddress());
				this.cancel();
			}
		}
		
	}
}