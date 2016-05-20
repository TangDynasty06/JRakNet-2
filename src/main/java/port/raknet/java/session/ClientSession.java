package port.raknet.java.session;

import java.net.InetSocketAddress;

import io.netty.channel.Channel;
import port.raknet.java.event.Hook;
import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.raknet.ClientConnectRequest;
import port.raknet.java.protocol.raknet.ClientHandshake;
import port.raknet.java.protocol.raknet.ConnectedPing;
import port.raknet.java.protocol.raknet.ConnectedPong;
import port.raknet.java.protocol.raknet.EncapsulatedPacket;
import port.raknet.java.protocol.raknet.ServerHandshake;
import port.raknet.java.server.RakNetServer;
import port.raknet.java.server.RakNetServerHandler;

public class ClientSession extends RakNetSession {

	private final RakNetServerHandler handler;
	private final RakNetServer server;
	private SessionState state;

	public ClientSession(Channel channel, InetSocketAddress address, RakNetServerHandler handler, RakNetServer server) {
		super(channel, address);
		this.handler = handler;
		this.server = server;
		this.state = SessionState.DISCONNECTED;
	}

	/**
	 * Returns the client's current RakNet state
	 * 
	 * @return SessionState
	 */
	public SessionState getState() {
		return this.state;
	}

	/**
	 * Set the client's specified RakNet state
	 * 
	 * @param state
	 */
	public void setState(SessionState state) {
		this.state = state;
	}

	@Override
	public void handleEncapsulated(EncapsulatedPacket encapsulated) {
		Packet packet = encapsulated.convertPayload();
		short pid = packet.getId();

		// Handled depending on ClientState
		if (pid == ID_CONNECTED_PING) {
			if (this.getState().getOrder() >= SessionState.CONNECTING_1.getOrder()) {
				ConnectedPing cp = new ConnectedPing(packet);
				cp.decode();

				ConnectedPong sp = new ConnectedPong();
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
