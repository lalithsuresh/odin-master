package net.floodlightcontroller.odinmaster;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.openflow.protocol.OFType;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.odinmaster.OdinClient;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.staticflowentry.IStaticFlowEntryPusherService;
import net.floodlightcontroller.util.MACAddress;

public class OdinMaster implements IFloodlightModule, IOFSwitchListener {

	private IFloodlightProviderService floodlightProvider;
	private IStaticFlowEntryPusherService staticFlowEntryPusher;
	private Executor executor = Executors.newFixedThreadPool(10);
	
	private AgentManager agentManager = new AgentManager();
	private ClientManager clientManager = new ClientManager();	

	
	public void receivePing (InetAddress odinAgentAddr) {
		if (agentManager.receivePing(odinAgentAddr)) {
			// push subscriptions
		}
	}
	
	public void receiveProbe (InetAddress odinAgentAddress, MACAddress staHwAddress) {
//		if (odinAgentAddress != null
//	    	&& staHwAddress != null
//	    	&& staHwAddress.isBroadcast() == false
//	    	&& staHwAddress.isMulticast() == false
//	    	&& agentManager.isTracked(odinAgentAddress) == true) {
//			
//	    	assignLvapToClient(staHwAddress, odinAgentAddress);	
//		}
	}
	
	public void receivePublish (MACAddress staHwAddress, InetAddress odinAgentAddr, Map<Long, Long> subscriptionIds) {
		
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

}
