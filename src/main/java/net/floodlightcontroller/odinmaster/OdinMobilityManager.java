package net.floodlightcontroller.odinmaster;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.floodlightcontroller.odinmaster.NotificationCallback;
import net.floodlightcontroller.odinmaster.NotificationCallbackContext;
import net.floodlightcontroller.odinmaster.OdinApplication;
import net.floodlightcontroller.odinmaster.OdinClient;
import net.floodlightcontroller.odinmaster.OdinEventSubscription;
import net.floodlightcontroller.odinmaster.OdinEventSubscription.Relation;
import net.floodlightcontroller.util.MACAddress;

public class OdinMobilityManager extends OdinApplication {
	
	private ConcurrentMap<MACAddress, Long> clientCurrentSignalMap = new ConcurrentHashMap<MACAddress, Long> ();
	
	private void init () {
		OdinEventSubscription oes1 = new OdinEventSubscription();
		oes1.setSubscription("*", "signal", Relation.GREATER_THAN, 180);		
		
		NotificationCallback cb = new NotificationCallback() {
			
			@Override
			public void exec(OdinEventSubscription oes, NotificationCallbackContext cntx) {
				handler(oes, cntx);
			}
		};
		
		odinApplicationInterface.registerSubscription(oes1, cb);
	}
	
	@Override
	public void run() {
		init ();
	}
	
	/**
	 * This handler will handoff a client in the event of its
	 * agent having failed.
	 * 
	 * @param oes
	 * @param cntx
	 */
	private void handler (OdinEventSubscription oes, NotificationCallbackContext cntx) {
		// Check to see if this is an associated client
		OdinClient client = odinApplicationInterface.getClients().get(cntx.clientHwAddress);

		if (client != null) {
			// The agent that triggered this handler is the one
			// hosting the client's LVAP
			if (client.getOdinAgent() != null && client.getOdinAgent().getIpAddress() == cntx.agent.getIpAddress()) {
				clientCurrentSignalMap.put(cntx.clientHwAddress, cntx.value);
			}
			else if ((client.getOdinAgent() == null) || 
					 (client.getOdinAgent().getIpAddress() != cntx.agent.getIpAddress()
					  && clientCurrentSignalMap.get(cntx.clientHwAddress) != null
					  && clientCurrentSignalMap.get(cntx.clientHwAddress) + 10 < cntx.value)) {
				odinApplicationInterface.handoffClientToAp(cntx.clientHwAddress, cntx.agent.getIpAddress());
				clientCurrentSignalMap.put(cntx.clientHwAddress, cntx.value);
			}
		}
	}
}
