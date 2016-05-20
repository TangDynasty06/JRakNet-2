package port.raknet.java.session;

import java.net.InetSocketAddress;

import io.netty.channel.Channel;
import port.raknet.java.client.RakNetClient;
import port.raknet.java.event.Hook;
import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.raknet.EncapsulatedPacket;

public class ServerSession extends RakNetSession {

	private final RakNetClient client;

	public ServerSession(Channel channel, InetSocketAddress address, RakNetClient client) {
		super(channel, address);
		this.client = client;
	}

	@Override
	public void handleEncapsulated(EncapsulatedPacket encapsulated) {
		Packet packet = encapsulated.convertPayload();
		short pid = packet.getId();

		if (client.getState() == SessionState.HANDSHAKING) {
			if (pid == ID_CONNECTED_SERVER_HANDSHAKE) {
				// TODO: sendPing and sendPong checking
				System.out.println("Connected to server!");
				client.setState(SessionState.CONNECTED);
			}
		} else if (client.getState() == SessionState.CONNECTED) {
			client.executeHook(Hook.PACKET_RECEIVED, client.getSession(), encapsulated);
		}
	}

}
