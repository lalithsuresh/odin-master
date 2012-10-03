package net.floodlightcontroller.odin.master;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

public class OdinAgentSerializer extends JsonSerializer<IOdinAgent> {

	@Override
	public void serialize(IOdinAgent agent, JsonGenerator jgen,
			SerializerProvider provider) throws IOException,
			JsonProcessingException {
		jgen.writeStartObject();
		String blah = agent.getIpAddress().getHostAddress();
		jgen.writeStringField("ipAddress", blah);
		jgen.writeStringField("lastHeard", String.valueOf(agent.getLastHeard()));
		jgen.writeEndObject();
	}
}
