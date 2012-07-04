package net.floodlightcontroller.odinmaster;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.openflow.protocol.OFType;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.staticflowentry.IStaticFlowEntryPusherService;
import net.floodlightcontroller.util.MACAddress;

public class OdinMaster implements IFloodlightModule, IOFSwitchListener {

	private IFloodlightProviderService floodlightProvider;
	private IStaticFlowEntryPusherService staticFlowEntryPusher;
	
	private AgentManager agentManager;
	private ClientManager clientManager;

	
	
	
	public void receivePing (InetAddress odinAgentAddr) {
		
	}
	
	public void receiveProbe (InetAddress odinAgentAddress, MACAddress staHwAddress) {
		
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
		
		// Spawn threads
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
