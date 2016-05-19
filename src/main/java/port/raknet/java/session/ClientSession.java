package port.raknet.java.session;

import java.net.InetSocketAddress;

import io.netty.channel.ChannelHandlerContext;
import port.raknet.java.SessionState;
import port.raknet.java.event.Hook;
import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.raknet.ClientConnectRequest;
import port.raknet.java.protocol.raknet.ClientHandshake;
import port.raknet.java.protocol.raknet.EncapsulatedPacket;
import port.raknet.java.protocol.raknet.Ping;
import port.raknet.java.protocol.raknet.Pong;
import port.raknet.java.protocol.raknet.ServerHandshake;
import port.raknet.java.server.RakNetServerHandler;
import port.raknet.java.server.RakNetServer;

/**
 * Represents a RakNet session, used to sending data to clients and tracking
 * it's state
 *
 * @author Trent Summerlin
 */
public class ClientSession extends RakNetSession {

	private final RakNetServerHandler handler;
	private final RakNetServer server;

	public ClientSession(ChannelHandlerContext context, InetSocketAddress address, RakNetServerHandler handler,
			RakNetServer server) {
		super(context, address);
		this.handler = handler;
		this.server = server;
	}

	@Override
	public void handleEncapsulated(EncapsulatedPacket encapsulated) {
		Packet packet = encapsulated.convertPayload();
		short pid = packet.getId();

		// Handled depending on ClientState
		if (pid == ID_CONNECTED_PING) {
			if (this.getState().getOrder() >= SessionState.CONNECTING_1.getOrder()) {
				Ping cp = new Ping(packet);
				cp.decode();

				Pong sp = new Pong();
				sp.pingId = cp.pingId;
				sp.encode();
				this.sendPacket(sp);
			}
		} else if (pid == ID_CONNECTED_PONG) {
			// Ignore, keep-alive only
		} else if (pid == ID_CONNECTED_CLIENT_CONNECT_REQUEST) {
			if (this.getState() == SessionState.CONNECTING_2) {
				ClientConnectRequest cchr = new ClientConnectRequest(packet);
				cchr.decode();

				ServerHandshake scha = new ServerHandshake();
				scha.clientAddress = this.getSystemAddress();
				scha.sendPing = cchr.sendPing;
				scha.sendPong = System.currentTimeMillis();
				scha.encode();

				this.sendPacket(scha);
				this.setState(SessionState.HANDSHAKING);
			}
		} else if (pid == ID_CONNECTED_CLIENT_HANDSHAKE) {
			if (this.getState() == SessionState.HANDSHAKING) {
				ClientHandshake cch = new ClientHandshake(packet);
				cch.decode();

				this.setState(SessionState.CONNECTED);
				server.executeHook(Hook.SESSION_CONNECTED, this, System.currentTimeMillis());
			}
		} else if (pid == ID_CONNECTED_CANCEL_CONNECTION) {
			if (this.getState().getOrder() >= SessionState.CONNECTING_1.getOrder()) {
				handler.removeSession(this, "Client cancelled connection");
			}
		} else if (this.getState() == SessionState.CONNECTED) {
			server.executeHook(Hook.PACKET_RECEIVED, this, encapsulated);
		}
	}

}
