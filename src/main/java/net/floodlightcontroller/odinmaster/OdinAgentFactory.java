package net.floodlightcontroller.odinmaster;

import net.floodlightcontroller.odinmaster.IOdinAgent;
import net.floodlightcontroller.odinmaster.OdinAgent;
import net.floodlightcontroller.odinmaster.StubOdinAgent;


public class OdinAgentFactory {
	
	private static String agentType = "OdinAgent";
	
	public static void setOdinAgentType(String type) {
		if (type.equals("OdinAgent") || type.equals("MockOdinAgent")) {
			agentType = type;
		}
		else {
			System.err.println("Unknown OdinAgent type: " + type);
			System.exit(-1);
		}
	}
	
	public static IOdinAgent getOdinAgent() {
		if (agentType.equals("OdinAgent")){
			return new OdinAgent();
		}
		else if (agentType.equals("MockOdinAgent")) {
			return new StubOdinAgent();
		}
		
		return null;
	}
}
