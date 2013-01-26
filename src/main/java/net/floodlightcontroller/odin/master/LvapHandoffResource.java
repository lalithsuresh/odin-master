package net.floodlightcontroller.odin.master;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;

import net.floodlightcontroller.util.MACAddress;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

public class LvapHandoffResource extends ServerResource {
	@SuppressWarnings("unchecked")
	@Post
    public void store(String flowmod) {
    	OdinMaster oc = (OdinMaster) getContext().getAttributes().
        					get(OdinMaster.class.getCanonicalName());
    	
    	ObjectMapper mapper = new ObjectMapper();

		HashMap<String, String> fmdata;
		try {
			fmdata = mapper.readValue(flowmod, HashMap.class);

			String staHwAddress = fmdata.get("clientHwAddress");
	        String apIpAddress= fmdata.get("apIpAddress");
	        String poolName = fmdata.get("poolName");
	    
	        oc.handoffClientToAp(poolName, MACAddress.valueOf(staHwAddress), InetAddress.getByName(apIpAddress));
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
