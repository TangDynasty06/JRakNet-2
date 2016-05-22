package port.raknet.java.session;

import java.net.InetSocketAddress;

import io.netty.channel.Channel;
import port.raknet.java.client.RakNetClient;
import port.raknet.java.event.Hook;
import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.raknet.ConnectedClientHandshake;
import port.raknet.java.protocol.raknet.ConnectedServerHandshake;
import port.raknet.java.protocol.raknet.internal.EncapsulatedPacket;

/**
 * Used by <code>RakNetClient</code> to handle the connected server
 *
 * @author Trent Summerlin
 */
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
				ConnectedServerHandshake serverHandshake = new ConnectedServerHandshake(packet);
				serverHandshake.decode();

				ConnectedClientHandshake clientHandshake = new ConnectedClientHandshake();
				clientHandshake.address = client.getLocalAddress();
				clientHandshake.timestamp = serverHandshake.timestamp;
				clientHandshake.serverTimestamp = System.currentTimeMillis();
				clientHandshake.encode();

				this.sendPacket(clientHandshake);
				client.setState(SessionState.CONNECTED);
				client.executeHook(Hook.SESSION_CONNECTED, this, System.currentTimeMillis());
			}
		} else if (client.getState() == SessionState.CONNECTED) {
			client.executeHook(Hook.PACKET_RECEIVED, client.getSession(), encapsulated);
		}
	}

}
