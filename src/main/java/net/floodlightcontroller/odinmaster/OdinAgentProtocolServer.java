package net.floodlightcontroller.odinmaster;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.util.MACAddress;

public class OdinAgentProtocolServer implements Runnable {
    protected static Logger log = LoggerFactory.getLogger(OdinAgentProtocolServer.class);

	// Odin Message types
	private final String ODIN_MSG_PING = "ping";
	private final String ODIN_MSG_PROBE = "probe";
	private final String ODIN_MSG_PUBLISH = "publish";

	private final int ODIN_SERVER_PORT = 2818;
	private final int ODIN_AGENT_PORT = 6777;
	
	private DatagramSocket controllerSocket;
	private final ExecutorService executor = Executors.newFixedThreadPool(4);
	private OdinMaster odinMaster;

	public void setOdinMaster (OdinMaster om) {
		this.odinMaster = om; 
	}
	
	@Override
	public void run() {
		
		try {
			controllerSocket = new DatagramSocket(ODIN_SERVER_PORT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		while(true)	{
			
			try {
				final byte[] receiveData = new byte[1024]; // We can probably live with less
				final DatagramPacket receivedPacket = new DatagramPacket(receiveData, receiveData.length);
				controllerSocket.receive(receivedPacket);
				
				executor.execute(new OdinAgentConnectionHandler(receivedPacket));
			}
			catch (IOException e) {
				log.error("controllerSocket.accept() failed: " + ODIN_SERVER_PORT);
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}
	
	/** Protocol handlers **/
	
	private void receivePing (final InetAddress odinAgentAddr) {
		odinMaster.receivePing(odinAgentAddr);
	}
	
	private void receiveProbe (final InetAddress odinAgentAddress, final MACAddress clientHwAddress) {
		odinMaster.receiveProbe(odinAgentAddress, clientHwAddress);
	}
	
	private void receivePublish (final MACAddress clientHwAddress, final InetAddress odinAgentAddr, final Map<Long, Long> subscriptionIds) {
		odinMaster.receivePublish(clientHwAddress, odinAgentAddr, subscriptionIds);
	}
	
	private class OdinAgentConnectionHandler implements Runnable {
		final DatagramPacket receivedPacket;
		
		public OdinAgentConnectionHandler(final DatagramPacket dp) {
			receivedPacket = dp;
		}
		
		// Agent message handler
		public void run() {			
			final String msg = new String(receivedPacket.getData()).trim().toLowerCase();
			final String msg_type = msg.split(" ")[0];
			final InetAddress odinAgentAddr = receivedPacket.getAddress();
            
            if (msg_type.equals(ODIN_MSG_PING)) {
            	receivePing(odinAgentAddr);
            }
            else if (msg_type.equals(ODIN_MSG_PROBE)) {
            	// 2nd part of message should contain
            	// the STA's MAC address
            	final String staAddress = msg.split(" ")[1];
            	receiveProbe(odinAgentAddr, MACAddress.valueOf(staAddress));
            }
            else if (msg_type.equals(ODIN_MSG_PUBLISH)) {
            	final String entries[] = msg.split(" ");
            	final String staAddress = entries[1];
            	final int count = Integer.parseInt(entries[2]);
            	final Map<Long, Long> matchingIds = new HashMap<Long,Long> ();
     
            	for (int i = 0; i < count; i++) {
            		matchingIds.put(Long.parseLong(entries[3 + i].split(":")[0]), Long.parseLong(entries[3 + i].split(":")[1]));
            	}
            	
            	receivePublish(MACAddress.valueOf(staAddress), odinAgentAddr, matchingIds);
            }
		}
	}
}
