package net.floodlightcontroller.odinmaster;

import java.net.InetAddress;
import java.util.Map;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class AgentManagerResource extends ServerResource {

	@Get("json")
    public Map<InetAddress,IOdinAgent> retreive() {
    	OdinMaster oc = (OdinMaster) getContext().getAttributes().
        					get(OdinMaster.class.getCanonicalName());
    	
    	return oc.getOdinAgents(PoolManager.GLOBAL_POOL);
    }
}