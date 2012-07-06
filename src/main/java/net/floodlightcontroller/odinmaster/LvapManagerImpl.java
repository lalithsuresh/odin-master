package net.floodlightcontroller.odinmaster;

import java.net.InetAddress;
import java.net.UnknownHostException;

import net.floodlightcontroller.odinmaster.OdinClient;
import net.floodlightcontroller.util.MACAddress;

public class LvapManagerImpl implements ILvapManager{
	
	private int bssidStart = 1;
	private final String DEFAULT_SSID = "odin";
	
	@Override
	public OdinClient getLvap(MACAddress clientHwAddress) {

		// Generate random BSSID
		// FIXME: This code can seriously be improved :)
		byte[] bssidBytes = clientHwAddress.toBytes();
		bssidBytes[0] = (byte) 0x00;
		bssidBytes[1] = (byte) 0x1b;
		bssidBytes[2] = (byte) 0xb1;
		
		MACAddress bssid = MACAddress.valueOf(bssidBytes);
		
		return new OdinClient(clientHwAddress, null, bssid, DEFAULT_SSID);
		
	}
}
