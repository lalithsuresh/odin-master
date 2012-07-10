package net.floodlightcontroller.odinmaster;

import net.floodlightcontroller.util.MACAddress;

public class NotificationCallbackContext {
	public final MACAddress clientHwAddress;
	public final IOdinAgent agent;
	public final long value;
	
	public NotificationCallbackContext(final MACAddress clientHwAddress, final IOdinAgent agent, final long value) {
		this.clientHwAddress = clientHwAddress;
		this.agent = agent;
		this.value = value;
	}
}
