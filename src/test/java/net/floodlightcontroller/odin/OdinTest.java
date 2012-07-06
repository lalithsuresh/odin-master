package net.floodlightcontroller.odin;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.test.MockFloodlightProvider;
import net.floodlightcontroller.odinmaster.AgentManager;
import net.floodlightcontroller.odinmaster.ClientManager;
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
        odinMaster = new OdinMaster();
        
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
    	
    	// 3. now try again. Client should be authorised
    	odinMaster.receiveProbe(InetAddress.getByName(ipAddress1), clientMacAddr1);
    	assertEquals(clientManager.getClients().size(), 1);
    	
    	// 4. client should have been handed off as well
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
    	
    	// 8. Authorise client2
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
    	
    	// 11. Authorise client3
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
}
