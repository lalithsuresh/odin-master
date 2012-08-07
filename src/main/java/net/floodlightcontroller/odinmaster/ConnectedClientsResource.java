package net.floodlightcontroller.odinmaster;

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
    	
    	for (Entry<MACAddress, OdinClient> e: oc.getClients().entrySet()) {
    		if (!e.getValue().getIpAddress().getHostAddress().equals("0.0.0.0")) {
    			connectedClients.put(e.getKey(), e.getValue());
    		}
    	}
    	
    	return connectedClients;
    }
}