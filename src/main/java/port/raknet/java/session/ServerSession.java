package port.raknet.java.session;

import java.net.InetSocketAddress;

import io.netty.channel.Channel;
import port.raknet.java.client.RakNetClient;
import port.raknet.java.event.Hook;
import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.Reliability;
import port.raknet.java.protocol.raknet.ConnectedClientHandshake;
import port.raknet.java.protocol.raknet.ConnectedPing;
import port.raknet.java.protocol.raknet.ConnectedPong;
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

	/**
	 * Returns whether or not the address is the same address as the server's
	 * address
	 * 
	 * @param address
	 */
	public boolean isServer(InetSocketAddress address) {
		return this.getSocketAddress().equals(address);
	}

	@Override
	public void handleEncapsulated(EncapsulatedPacket encapsulated) {
		Packet packet = encapsulated.convertPayload();
		short pid = packet.getId();

		if (pid == ID_CONNECTED_SERVER_HANDSHAKE) {
			if (client.getState() == SessionState.HANDSHAKING) {
				ConnectedServerHandshake serverHandshake = new ConnectedServerHandshake(packet);
				serverHandshake.decode();

				if (serverHandshake.timestamp == client.getTimestamp()) {
					ConnectedClientHandshake clientHandshake = new ConnectedClientHandshake();
					clientHandshake.clientAddress = client.getLocalAddress();
					clientHandshake.serverTimestamp = serverHandshake.serverTimestamp;
					clientHandshake.timestamp = client.getTimestamp();
					clientHandshake.encode();

					this.sendPacket(Reliability.UNRELIABLE, clientHandshake);
				}

				client.setState(SessionState.CONNECTED);
				client.executeHook(Hook.SESSION_CONNECTED, client.getSession(), System.currentTimeMillis());
			}
		} else if (pid == ID_CONNECTED_PING) {
			ConnectedPing ping = new ConnectedPing(packet);
			ping.decode();

			ConnectedPong pong = new ConnectedPong();
			pong.pingId = ping.pingId;
			pong.encode();

			this.sendPacket(Reliability.UNRELIABLE, pong);
		} else if (client.getState() == SessionState.CONNECTED) {
			client.executeHook(Hook.PACKET_RECEIVED, client.getSession(), encapsulated);
		}
	}

}
