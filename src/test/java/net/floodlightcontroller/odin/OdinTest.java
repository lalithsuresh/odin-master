package net.floodlightcontroller.odin;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.test.MockFloodlightProvider;
import net.floodlightcontroller.odinmaster.AgentManager;
import net.floodlightcontroller.odinmaster.ClientManager;
import net.floodlightcontroller.odinmaster.ILvapManager;
import net.floodlightcontroller.odinmaster.LvapManagerImpl;
import net.floodlightcontroller.odinmaster.OdinAgentFactory;
import net.floodlightcontroller.odinmaster.OdinClient;
import net.floodlightcontroller.odinmaster.OdinMaster;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.restserver.RestApiServer;
import net.floodlightcontroller.staticflowentry.IStaticFlowEntryPusherService;
import net.floodlightcontroller.staticflowentry.StaticFlowEntryPusher;
import net.floodlightcontroller.util.MACAddress;

import org.easymock.EasyMock;
import org.jboss.netty.channel.Channel;
import org.junit.Before;
import org.junit.Test;

public class OdinTest {
	protected MockFloodlightProvider mockFloodlightProvider;
    protected FloodlightModuleContext cntx;
    protected OdinMaster odinMaster;
    protected AgentManager agentManager;
    protected ClientManager clientManager;
    protected ILvapManager lvapManager;
    protected StaticFlowEntryPusher staticFlowEntryPusher;
    protected long switchId = 1L;
    
    /**
     * Use this to add a mock agent on IP:ipAddress and port:port.
     * 
     * @param ipAddress
     * @param port
     * @throws Exception
     */
    private void addAgentWithMockSwitch (String ipAddress, int port) throws Exception {
        int size = agentManager.getOdinAgents().size();
    	
        agentManager.receivePing(InetAddress.getByName(ipAddress));
        
    	assertEquals(agentManager.getOdinAgents().size(), size);

        long id = switchId++;
        // Now register a switch
    	IOFSwitch sw1;
    	sw1 = EasyMock.createNiceMock(IOFSwitch.class);
    	InetSocketAddress sa = new InetSocketAddress(ipAddress, port);
    	Channel ch = EasyMock.createMock(Channel.class);
        expect(sw1.getChannel()).andReturn(ch).anyTimes();
        expect(ch.getRemoteAddress()).andReturn((SocketAddress)sa).anyTimes();
        expect(sw1.getId()).andReturn(id).anyTimes();

        
        EasyMock.replay(sw1);
        EasyMock.replay(ch);

        // Update the switch map
        mockFloodlightProvider.getSwitches().put(id, sw1);

        // Let's try again
        agentManager.receivePing(InetAddress.getByName(ipAddress));
        
        assertEquals(agentManager.getOdinAgents().size(),size + 1);
        
        assertEquals(agentManager.getOdinAgents().get(InetAddress.getByName(ipAddress)).getSwitch(), sw1);
    }
    
    @Before
    public void setup() throws FloodlightModuleException{
        // Mock context
        cntx = new FloodlightModuleContext();
        RestApiServer restApi = new RestApiServer();
        mockFloodlightProvider = new MockFloodlightProvider();
        
        OdinAgentFactory.setOdinAgentType("MockOdinAgent");
        
        agentManager = new AgentManager();
        clientManager = new ClientManager();
        lvapManager = new LvapManagerImpl();
        odinMaster = new OdinMaster(agentManager, clientManager, lvapManager);
        
        cntx.addService(IFloodlightProviderService.class, mockFloodlightProvider);
        cntx.addService(IRestApiService.class, restApi);
        
        try {
			restApi.init(cntx);
	        odinMaster.init(cntx);
	        
		} catch (FloodlightModuleException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        restApi.startUp(cntx);
        
        mockFloodlightProvider.addOFSwitchListener(odinMaster);
        agentManager.setClientManager(clientManager);
        agentManager.setFloodlightProvider(mockFloodlightProvider);
    }
    

    /**
     * Make sure the client tracker doesn't duplicate
     * client MAC addresses.
     * 
     * @throws Exception
     */
    @Test
    public void testClientTracker() throws Exception {
    	assertEquals(clientManager.getClients().size(),0);
    	clientManager.addClient(MACAddress.valueOf("00:00:00:00:00:01"),
								 InetAddress.getByName("172.17.1.1"),
								 MACAddress.valueOf("00:00:00:00:00:01"),
								 "g9");
		
		assertEquals(clientManager.getClients().size(),1);
		clientManager.addClient(MACAddress.valueOf("00:00:00:00:00:01"),
								 InetAddress.getByName("172.17.1.2"),
								 MACAddress.valueOf("00:00:00:00:00:02"),
								 "g9");
		assertEquals(clientManager.getClients().size(),1); // Same hw-addr cant exist twice

		// TODO: None of the other parameters should repeat either!
		clientManager.removeClient(MACAddress.valueOf("00:00:00:00:00:02"));
		assertEquals(clientManager.getClients().size(),1);
		
		clientManager.removeClient(MACAddress.valueOf("00:00:00:00:00:01"));
		assertEquals(clientManager.getClients().size(),0);
		
		clientManager.removeClient(MACAddress.valueOf("00:00:00:00:00:01"));
		assertEquals(clientManager.getClients().size(),0);
    }
    
    /**
     *  Make sure that the agent tracker does not
     *  track an agent if there isn't a corresponding
     *  switch associated with it
     *  
     * @throws Exception
     */
    @Test
    public void testAgentTracker() throws Exception {
        
        // Send a ping to the OdinAgentTracker to have it
        // register the agent
    	agentManager.receivePing(InetAddress.getByName("127.0.0.1"));
        
        // We haven't registered a switch yet, so this should
        // still be zero
        assertEquals(agentManager.getOdinAgents().size(),0);
        
        // Now register a switch
    	IOFSwitch sw1;
    	sw1 = EasyMock.createNiceMock(IOFSwitch.class);
    	InetSocketAddress sa= new InetSocketAddress("127.0.0.1", 12345);
    	Channel ch = EasyMock.createMock(Channel.class);
        expect(sw1.getChannel()).andReturn(ch).anyTimes();
        expect(ch.getRemoteAddress()).andReturn((SocketAddress)sa).anyTimes();
        
        EasyMock.replay(sw1);
        EasyMock.replay(ch);
        // Load the switch map
        Map<Long, IOFSwitch> switches = new HashMap<Long, IOFSwitch>();
        switches.put(1L, sw1);
        mockFloodlightProvider.setSwitches(switches);

        // Let's try again
        agentManager.receivePing(InetAddress.getByName("127.0.0.1"));
        
        assertEquals(agentManager.getOdinAgents().size(),1);
    }
    
    
    /**
     * Test to see if OdinAgentTracker.receiveProbe()
     * works correctly
     * 
     * @throws Exception
     */
    @Test
    public void testReceiveProbe() throws Exception {
    	String ipAddress1 = "172.17.2.161";
    	String ipAddress2 = "172.17.2.162";
    	String ipAddress3 = "172.17.2.163";
    	MACAddress clientMacAddr1 = MACAddress.valueOf("00:00:00:00:00:01");
    	MACAddress clientMacAddr2 = MACAddress.valueOf("00:00:00:00:00:02");
    	MACAddress clientMacAddr3 = MACAddress.valueOf("00:00:00:00:00:03");
  	
    	addAgentWithMockSwitch(ipAddress1, 12345);
    	addAgentWithMockSwitch(ipAddress2, 12345);
    	
    	// 1. Things shouldn't explode when this is called
    	odinMaster.receiveProbe(null, null);
    	
    	assertEquals(agentManager.getOdinAgents().size(), 2);
    	
    	odinMaster.receiveProbe(InetAddress.getByName(ipAddress1), clientMacAddr1);
    	
    	// 2. Client should be added
    	assertEquals(clientManager.getClients().size(), 1);
    	
    	clientManager.addClient(clientMacAddr1, InetAddress.getByName("172.17.2.51"), MACAddress.valueOf("00:00:00:00:11:11"), "odin");
    	assertEquals(clientManager.getClients().get(clientMacAddr1).getOdinAgent(), null);
    	
    	// 3, 4. now try again. Client should be assigned an AP
    	odinMaster.receiveProbe(InetAddress.getByName(ipAddress1), clientMacAddr1);
    	assertEquals(clientManager.getClients().size(), 1);
    	assertEquals(clientManager.getClients().get(clientMacAddr1).getOdinAgent().getIpAddress(), InetAddress.getByName(ipAddress1));
    	
    	// 5. another probe from the same AP/client should
    	// 	not be handed off, and should not feature
    	// 	in the client list a second time
    	odinMaster.receiveProbe(InetAddress.getByName(ipAddress1), clientMacAddr1);
    	assertEquals(clientManager.getClients().size(), 1);
    	assertEquals(clientManager.getClients().get(clientMacAddr1).getOdinAgent().getIpAddress(), InetAddress.getByName(ipAddress1));
    	
    	// 6. probe scan from new AP. client should not be
    	// handed off to the new one, nor should a new
    	// client be registered.
    	odinMaster.receiveProbe(InetAddress.getByName(ipAddress2), clientMacAddr1);
    	assertEquals(clientManager.getClients().size(), 1);
    	assertEquals(clientManager.getClients().get(clientMacAddr1).getOdinAgent().getIpAddress(), InetAddress.getByName(ipAddress1));
    	
    	// 7. New client performs a scan at AP1
    	odinMaster.receiveProbe(InetAddress.getByName(ipAddress1), clientMacAddr2);
    	assertEquals(clientManager.getClients().size(), 2);
    	
    	// 8. Add client2
    	clientManager.addClient(clientMacAddr2, InetAddress.getByName("172.17.2.52"), MACAddress.valueOf("00:00:00:00:11:12"), "odin");
    	assertEquals(clientManager.getClients().size(), 2);
    	assertEquals(clientManager.getClients().get(clientMacAddr2).getOdinAgent(), null);
    	
    	// 9. Receive probe from both APs one after the other.
    	// Client should be assigned to the first AP
    	odinMaster.receiveProbe(InetAddress.getByName(ipAddress1), clientMacAddr2);
    	odinMaster.receiveProbe(InetAddress.getByName(ipAddress2), clientMacAddr2);
    	assertEquals(clientManager.getClients().size(), 2);
    	assertEquals(clientManager.getClients().get(clientMacAddr2).getOdinAgent().getIpAddress(), InetAddress.getByName(ipAddress1));
    	
    	
    	// 10. Receive probe from an AP which is yet to register,
    	// for an unauthorised client which is scanning for the first time.
    	// This can occur as a race condition.
    	odinMaster.receiveProbe(InetAddress.getByName(ipAddress3), clientMacAddr3);
    	assertNull(agentManager.getOdinAgents().get(InetAddress.getByName(ipAddress3)));
    	assertEquals(clientManager.getClients().size(), 2);
    	
    	// 11. Add client3
    	clientManager.addClient(clientMacAddr3, InetAddress.getByName("172.17.2.53"), MACAddress.valueOf("00:00:00:00:11:13"), "odin");
    	assertEquals(clientManager.getClients().size(), 3);
    	assertEquals(clientManager.getClients().get(clientMacAddr3).getOdinAgent(), null);

    	// 10. Receive probe from an AP which is yet to register,
    	// for an authorised client which is scanning for the first time.
    	// This can occur as a race condition.
    	odinMaster.receiveProbe(InetAddress.getByName(ipAddress3), clientMacAddr3);
    	assertNull(agentManager.getOdinAgents().get(InetAddress.getByName(ipAddress3)));
    	assertEquals(clientManager.getClients().size(), 3);
    	
    	// 11. Now add agent3
    	addAgentWithMockSwitch(ipAddress3, 12345);

    	// 12. Receive probe from an AP which has registered,
    	// for an authorised client which is scanning for the first time.
    	odinMaster.receiveProbe(InetAddress.getByName(ipAddress3), clientMacAddr3);
    	assertEquals(clientManager.getClients().size(), 3);
    	assertEquals(clientManager.getClients().get(clientMacAddr3).getOdinAgent().getIpAddress(), InetAddress.getByName(ipAddress3));
    	
    	odinMaster.receiveProbe(InetAddress.getByName(ipAddress3), null);
    }
    

    /**
     * Test to see if OdinMaster.handoff()
     * works correctly
     * 
     * @throws Exception
     */
    @Test
    public void testHandoff() throws Exception {
    	String ipAddress1 = "172.17.2.161";
    	String ipAddress2 = "172.17.2.162";
    	String ipAddress3 = "172.17.2.163";
    	String ipAddress4 = "172.17.2.164";
    	
    	MACAddress clientMacAddr1 = MACAddress.valueOf("00:00:00:00:00:01");
    	MACAddress clientMacAddr2 = MACAddress.valueOf("00:00:00:00:00:02");
    	
    	// Things shouldn't explode if this is called
    	odinMaster.handoffClientToAp(null, null);
        
    	
    	clientManager.addClient(clientMacAddr1, InetAddress.getByName("172.17.2.51"), MACAddress.valueOf("00:00:00:00:11:11"), "odin");
  	
    	addAgentWithMockSwitch(ipAddress1, 12345);
    	addAgentWithMockSwitch(ipAddress2, 12345);
    	addAgentWithMockSwitch(ipAddress3, 12345);
    	
    	assertEquals(clientManager.getClients().size(), 1);
    	assertNull(clientManager.getClients().get(clientMacAddr1).getOdinAgent());
    	assertEquals(agentManager.getOdinAgents().size(), 3);

    	///// Sane cases /////
    	
    	// Handoff client for the first time to AP that exists
    	odinMaster.handoffClientToAp(clientMacAddr1, InetAddress.getByName(ipAddress1));
    	assertEquals(clientManager.getClients().get(clientMacAddr1).getOdinAgent().getIpAddress(), InetAddress.getByName(ipAddress1));
    	
    	// Handoff client to the same AP, nothing should change
    	odinMaster.handoffClientToAp(clientMacAddr1, InetAddress.getByName(ipAddress1));
    	assertEquals(clientManager.getClients().get(clientMacAddr1).getOdinAgent().getIpAddress(), InetAddress.getByName(ipAddress1));
    	
    	// Handoff client to AP2 that exists
    	odinMaster.handoffClientToAp(clientMacAddr1, InetAddress.getByName(ipAddress2));
    	assertEquals(clientManager.getClients().get(clientMacAddr1).getOdinAgent().getIpAddress(), InetAddress.getByName(ipAddress2));
    	
    	// Handoff client to AP3 that exists
    	odinMaster.handoffClientToAp(clientMacAddr1, InetAddress.getByName(ipAddress3));
    	assertEquals(clientManager.getClients().get(clientMacAddr1).getOdinAgent().getIpAddress(), InetAddress.getByName(ipAddress3));
    	
    	// And back to AP1
    	odinMaster.handoffClientToAp(clientMacAddr1, InetAddress.getByName(ipAddress1));
    	assertEquals(clientManager.getClients().get(clientMacAddr1).getOdinAgent().getIpAddress(), InetAddress.getByName(ipAddress1));
    	
    	
    	///// Less sane cases /////
    	
    	// Handoff unauthorised client around, it should never
    	// be assigned an LVAP
    	odinMaster.handoffClientToAp(clientMacAddr2, InetAddress.getByName(ipAddress1));
    	assertNull(clientManager.getClients().get(clientMacAddr2)); // If this is null, it can never be assigned an LVAP
    	
    	odinMaster.handoffClientToAp(clientMacAddr2, InetAddress.getByName(ipAddress2));
    	assertNull(clientManager.getClients().get(clientMacAddr2));
    	
    	// Now try handing off to non-existent AP
    	odinMaster.handoffClientToAp(clientMacAddr2, InetAddress.getByName(ipAddress4));
    	assertNull(clientManager.getClients().get(clientMacAddr2));
    	
    	
    	// now authorise the client
    	clientManager.addClient(clientMacAddr2, InetAddress.getByName("172.17.2.52"), MACAddress.valueOf("00:00:00:00:11:12"), "odin");
    	assertNotNull(clientManager.getClients().get(clientMacAddr2));
    	assertNull(clientManager.getClients().get(clientMacAddr2).getOdinAgent());
    	
    	// Handoff authorised client to non-existent agent,
    	// it should still not have an LVAP
    	odinMaster.handoffClientToAp(clientMacAddr2, InetAddress.getByName(ipAddress4));
    	assertNotNull(clientManager.getClients().get(clientMacAddr2));
    	assertNull(clientManager.getClients().get(clientMacAddr2).getOdinAgent());
    	
    	// Now handoff to an existing agent
    	odinMaster.handoffClientToAp(clientMacAddr2, InetAddress.getByName(ipAddress1));
    	assertNotNull(clientManager.getClients().get(clientMacAddr2));
    	assertEquals(clientManager.getClients().get(clientMacAddr2).getOdinAgent().getIpAddress(), InetAddress.getByName(ipAddress1));
    	
    	// Now handoff to a non-existing agent, the client's
    	// LVAP should not have changed
    	odinMaster.handoffClientToAp(clientMacAddr2, InetAddress.getByName(ipAddress4));
    	assertNotNull(clientManager.getClients().get(clientMacAddr2));
    	assertEquals(clientManager.getClients().get(clientMacAddr2).getOdinAgent().getIpAddress(), InetAddress.getByName(ipAddress1));
    	
    	// Now handoff to a null agent, the client's agent
    	// assignment shouldn't have changed
    	odinMaster.handoffClientToAp(clientMacAddr2, null);
    	assertNotNull(clientManager.getClients().get(clientMacAddr2));
    	assertEquals(clientManager.getClients().get(clientMacAddr2).getOdinAgent().getIpAddress(), InetAddress.getByName(ipAddress1));
    }


    /**
     * Test to see if we can handle agent
     * failures correctly
     * 
     * @throws Exception
     */
    @Test
    public void testAgentLeave() throws Exception {
    	String ipAddress1 = "172.17.2.161";
    	String ipAddress2 = "172.17.2.162";
    	MACAddress clientMacAddr1 = MACAddress.valueOf("00:00:00:00:00:01");
    	MACAddress clientMacAddr2 = MACAddress.valueOf("00:00:00:00:00:02");
    	agentManager.setAgentTimeout(1000);
    	
    	// Add an agent and associate a client to it
    	addAgentWithMockSwitch(ipAddress1, 12345);
    	clientManager.addClient(clientMacAddr1, InetAddress.getByName("172.17.2.51"), MACAddress.valueOf("00:00:00:00:11:11"), "odin");
    	odinMaster.receiveProbe(InetAddress.getByName(ipAddress1), clientMacAddr1);
    	
    	// There should be an agent, and a client recorded at the master
    	assertEquals(agentManager.getOdinAgents().size(), 1);
    	assertEquals(clientManager.getClients().size(), 1);

    	Thread.sleep(1500);
    	
    	// Agent should have been removed by now, and the associated
    	// client should have no agent assigned to it
    	assertEquals(agentManager.getOdinAgents().size(), 0);
    	assertEquals(clientManager.getClients().get(clientMacAddr1).getOdinAgent(), null);
    	
    	// Now ping again to revive the agent
    	odinMaster.receivePing(InetAddress.getByName(ipAddress1));
    	
       	// Agent should be setup again
    	assertEquals(agentManager.getOdinAgents().size(), 1);
 
    	// Client should remain unassigned
    	assertEquals(clientManager.getClients().get(clientMacAddr1).getOdinAgent(), null);
    	
    	// Time it out again
    	Thread.sleep(1500);
    	assertEquals(agentManager.getOdinAgents().size(), 0);
    	assertEquals(clientManager.getClients().get(clientMacAddr1).getOdinAgent(), null);
    	
    	// There is no instance for the agent at the master, but we
    	// mock a client scan that is forwarded by the agent to the
    	// controller. The client shouldn't be assigned to the agent
    	// yet.
    	odinMaster.receiveProbe(InetAddress.getByName(ipAddress1), clientMacAddr1);
    	assertEquals(clientManager.getClients().get(clientMacAddr1).getOdinAgent(), null);

    	// Now let the agent ping again. Master will track it, but client
    	// will still remain unassigned
    	odinMaster.receivePing(InetAddress.getByName(ipAddress1));
    	assertEquals(agentManager.getOdinAgents().size(), 1);
    	assertEquals(clientManager.getClients().get(clientMacAddr1).getOdinAgent(), null);
    	
    	// Now mock a client probe. Client should be assigned now.
    	odinMaster.receiveProbe(InetAddress.getByName(ipAddress1), clientMacAddr1);
    	assertEquals(clientManager.getClients().get(clientMacAddr1).getOdinAgent().getIpAddress(), InetAddress.getByName(ipAddress1));

    	
    	// The following tests the LVAP syncing mechanism
    	
    	// Add another agent that already is hosting an LVAP. Make it join.
    	
    	List<OdinClient> lvapList = new ArrayList<OdinClient>();
    	OdinClient oc = new OdinClient(clientMacAddr2, InetAddress.getByName("172.17.1.52"), MACAddress.valueOf("00:00:00:00:11:12"), "odin");
    	lvapList.add(oc);
        OdinAgentFactory.setOdinAgentType("MockOdinAgent");
    	OdinAgentFactory.setMockOdinAgentLvapList(lvapList);
    	
    	addAgentWithMockSwitch(ipAddress2, 12345);
    	
    	assertEquals(clientManager.getClients().size(), 2);
    	assertEquals(agentManager.getOdinAgents().size(), 2);
    	assertEquals(clientManager.getClients().get(clientMacAddr2).getOdinAgent().getIpAddress(), InetAddress.getByName(ipAddress2));
    }
    
}
