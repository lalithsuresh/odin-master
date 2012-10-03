package net.floodlightcontroller.odin.master;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.floodlightcontroller.util.MACAddress;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class ConnectedClientsResource extends ServerResource {

	@Get("json")
    public Map<MACAddress, OdinClient> retreive() {
    	OdinMaster oc = (OdinMaster) getContext().getAttributes().
        					get(OdinMaster.class.getCanonicalName());
    	
    	Map<MACAddress, OdinClient> connectedClients = new HashMap<MACAddress, OdinClient> ();
    	
    	for (OdinClient e: oc.getClients(PoolManager.GLOBAL_POOL)) {
    		if (!e.getIpAddress().getHostAddress().equals("0.0.0.0")) {
    			connectedClients.put(e.getMacAddress(), e);
    		}
    	}
    	
    	return connectedClients;
    }
}