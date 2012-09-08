package net.floodlightcontroller.odinmaster;

import java.util.Map;
import java.util.Set;

import net.floodlightcontroller.odinmaster.OdinMaster;
import net.floodlightcontroller.util.MACAddress;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class AllClientsResource extends ServerResource {

	@Get("json")
    public Set<OdinClient> retreive() {
    	OdinMaster oc = (OdinMaster) getContext().getAttributes().
        					get(OdinMaster.class.getCanonicalName());
    	
    	return oc.getClients(PoolManager.GLOBAL_POOL);
    }
}
