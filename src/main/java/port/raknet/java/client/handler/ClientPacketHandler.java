package port.raknet.java.client.handler;

import port.raknet.java.RakNet;
import port.raknet.java.client.RakNetClient;
import port.raknet.java.protocol.Packet;

public abstract class ClientPacketHandler implements RakNet {

	public abstract void handlePacket(short pid, Packet packet, RakNetClient client);

}
