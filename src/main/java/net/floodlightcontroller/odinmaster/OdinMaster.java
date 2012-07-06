package net.floodlightcontroller.odinmaster;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.openflow.protocol.OFType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.odinmaster.IOdinAgent;
import net.floodlightcontroller.odinmaster.OdinClient;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.staticflowentry.IStaticFlowEntryPusherService;
import net.floodlightcontroller.util.MACAddress;

public class OdinMaster implements IFloodlightModule, IOFSwitchListener {
	protected static Logger log = LoggerFactory.getLogger(OdinMaster.class);

	private IFloodlightProviderService floodlightProvider;
	private IStaticFlowEntryPusherService staticFlowEntryPusher;
	private Executor executor = Executors.newFixedThreadPool(10);
	
	private AgentManager agentManager = new AgentManager();
	private ClientManager clientManager = new ClientManager();	
	private ILvapManager lvapManager = new LvapManagerImpl();

	
	public OdinMaster(AgentManager agentManager, ClientManager clientManager, ILvapManager lvapManager){
		this.agentManager = agentManager;
		this.clientManager = clientManager;
		this.lvapManager = lvapManager;
	}
	
	public void receivePing (InetAddress odinAgentAddr) {
		if (agentManager.receivePing(odinAgentAddr)) {
			// push subscriptions
		}
	}
	
	public void receiveProbe (InetAddress odinAgentAddress, MACAddress clientHwAddress) {
		if (odinAgentAddress != null
	    	&& clientHwAddress != null
	    	&& clientHwAddress.isBroadcast() == false
	    	&& clientHwAddress.isMulticast() == false
	    	&& agentManager.isTracked(odinAgentAddress) == true) {
				    	
	    	OdinClient oc = clientManager.getClient(clientHwAddress);
	    	
	    	// Hearing from this client for the first time
	    	if (oc == null) {
	    		oc = lvapManager.getLvap(clientHwAddress);
	    		clientManager.addClient(oc);
	    	}
	    	
			if (oc.getOdinAgent() == null) {
				// client is connecting for the
				// first time, had explicitly
				// disconnected, or knocked
				// out at as a result of an agent
				// failure.
				handoffClientToAp(clientHwAddress, odinAgentAddress);
			}
		}
	}
	
	public void receivePublish (MACAddress staHwAddress, InetAddress odinAgentAddr, Map<Long, Long> subscriptionIds) {
		
	}

	/** Odin Methods **/
	
	/**
	 * VAP-Handoff a client to a new AP. This operation is idempotent.
	 * 
	 * @param newApIpAddr IPv4 address of new access point
	 * @param hwAddrSta Ethernet address of STA to be handed off
	 */
	public void handoffClientToAp (MACAddress staHwAddr, InetAddress newApIpAddr){
		// As an optimisation, we probably need to get the accessing done first,
		// prime both nodes, and complete a handoff. 
		
		if (staHwAddr == null || newApIpAddr == null) {
			log.error("null argument in handoffClientToAp(): staHwAddr:" + staHwAddr + " newApIpAddr:" + newApIpAddr);
			return;
		}
		
		IOdinAgent newAgent = agentManager.getOdinAgents().get(newApIpAddr);
		
		// If new agent doesn't exist, ignore request
		if (newAgent == null) {
			log.error("Handoff request ignored: OdinAgent " + newApIpAddr + " doesn't exist");
			return;
		}
		
		OdinClient client = clientManager.getClient(staHwAddr);
		
		// Ignore request if we don't know the client
		if (client == null) {
			log.error("Handoff request ignored: OdinClient " + staHwAddr + " doesn't exist");
			return;
		}
		
		// If the client is connecting for the first time, then it
		// doesn't have a VAP associated with it already
		if (client.getOdinAgent() == null) {
			log.info ("Client: " + staHwAddr + " connecting for first time. Assigning to: " + newAgent.getIpAddress());
			//FIXME: setupFlows(newAgent, client.getIpAddress().getHostAddress());
			newAgent.addLvap(client);
			client.setOdinAgent(newAgent);
			return;
		}
		
		// If the client is already associated with AP-newIpAddr, we ignore
		// the request.
		InetAddress currentApIpAddress = client.getOdinAgent().getIpAddress();
		if (currentApIpAddress.getHostAddress().equals(newApIpAddr.getHostAddress())) {
			log.info ("Client " + staHwAddr + " is already associated with AP " + newApIpAddr);
			return;
		}

		// We can do better than this :)
		//FIXME: setupFlows(newAgent, client.getIpAddress().getHostAddress());
		
		// Client is with another AP. We remove the VAP from
		// the current AP of the client, and spawn it on the new one.
		// We split the add and remove VAP operations across two threads
		// to make it faster. Note that there is a temporary inconsistent 
		// state between setting the agent for the client and it actually 
		// being reflected in the network
		client.setOdinAgent(newAgent);
		executor.execute(new OdinAgentLvapAddRunnable(newAgent, client));
		executor.execute(new OdinAgentLvapRemoveRunnable(agentManager.getOdinAgents().get(currentApIpAddress), client));
	}
	
	/** IFloodlightModule methods **/
	
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l =
	        new ArrayList<Class<? extends IFloodlightService>>();
	    l.add(IFloodlightProviderService.class);
        l.add(IRestApiService.class);
        l.add(IStaticFlowEntryPusherService.class);
		return l;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);		
		staticFlowEntryPusher = context.getServiceImpl(IStaticFlowEntryPusherService.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) {
		floodlightProvider.addOFSwitchListener(this);
		agentManager.setFloodlightProvider (floodlightProvider);
		agentManager.setClientManager (clientManager);
		
		// Spawn threads
		executor.execute(new OdinAgentProtocolServer());
	}

	/** IOFSwitchListener methods **/
	
	@Override
	public void addedSwitch(IOFSwitch sw) {
		// inform-agent manager
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removedSwitch(IOFSwitch sw) {
		// TODO Auto-generated method stub
		
	}

	private class OdinAgentLvapAddRunnable implements Runnable {
		final IOdinAgent oa;
		final OdinClient oc;
		
		public OdinAgentLvapAddRunnable(IOdinAgent newAgent, OdinClient oc) {
			this.oa = newAgent;
			this.oc = oc;
		}
		@Override
		public void run() {
			oa.addLvap(oc);
		}
		
	}
	
	private class OdinAgentLvapRemoveRunnable implements Runnable {
		final IOdinAgent oa;
		final OdinClient oc;
		
		public OdinAgentLvapRemoveRunnable(IOdinAgent oa, OdinClient oc) {
			this.oa = oa;
			this.oc = oc;
		}
		@Override
		public void run() {
			oa.removeLvap(oc);
		}
		
	}
}