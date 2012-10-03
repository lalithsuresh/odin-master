package net.floodlightcontroller.odin.master;

import java.util.ArrayList;
import java.util.List;

public class OdinAgentFactory {
	
	private static String agentType = "OdinAgent";
	private static List<OdinClient> lvapList = new ArrayList<OdinClient> ();
	
	public static void setOdinAgentType(String type) {
		if (type.equals("OdinAgent") 
				|| type.equals("MockOdinAgent")) {
			agentType = type;
		}
		else {
			System.err.println("Unknown OdinAgent type: " + type);
			System.exit(-1);
		}
	}
	
	public static void setMockOdinAgentLvapList(List<OdinClient> list) {
		if (agentType.equals("MockOdinAgent")) {
			lvapList = list;
		}
	}
	
	public static IOdinAgent getOdinAgent() {
		if (agentType.equals("OdinAgent")){
			return new OdinAgent();
		}
		else if (agentType.equals("MockOdinAgent")) {
			StubOdinAgent soa = new StubOdinAgent();
			
			for (OdinClient client: lvapList) {
				soa.addClientLvap(client);
			}
			
			return soa;
		}
		
		return null;
	}
}
