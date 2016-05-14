package port.raknet.java.client.handler;

import port.raknet.java.client.RakNetClient;
import port.raknet.java.protocol.Packet;

public class StatusHandler extends ClientPacketHandler {

	@Override
	public void handlePacket(short pid, Packet packet, RakNetClient client) {
		if (pid == ID_STATUS_RESPONSE) {
			
		}
	}

}
