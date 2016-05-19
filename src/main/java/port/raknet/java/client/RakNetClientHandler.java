package port.raknet.java.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import port.raknet.java.RakNet;
import port.raknet.java.event.Hook;
import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.raknet.Acknowledge;
import port.raknet.java.protocol.raknet.CustomPacket;
import port.raknet.java.session.ServerSession;

public class RakNetClientHandler extends SimpleChannelInboundHandler<DatagramPacket>implements RakNet {

	private final RakNetClient client;
	private final int maxSessions;
	private ServerSession session;

	public RakNetClientHandler(RakNetClient client, int maxSessions) {
		this.client = client;
		this.maxSessions = maxSessions;
	}

	/**
	 * Returns the current ServerSession
	 * 
	 * @return ServerSession
	 */
	public ServerSession getSession() {
		return this.session;
	}

	public void removeSession(String reason) {
		client.executeHook(Hook.SESSION_DISCONNECTED, session);
		this.session = null;
	}

	@Override
	protected final void messageReceived(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
		Packet packet = new Packet(msg.content().retain());
		short pid = packet.getId();

		// Handle internal packets here
		if (session != null) {
			session.resetLastReceiveTime();
		}
		
		if (pid >= ID_CUSTOM_0 && pid <= ID_CUSTOM_F) {
			CustomPacket custom = new CustomPacket(packet);
			custom.decode();
			session.handleCustom0(custom);
		} else if (pid == ID_ACK) {
			Acknowledge ack = new Acknowledge(packet);
			ack.decode();
			session.checkACK(ack);
		} else if (pid == ID_NACK) {
			Acknowledge nack = new Acknowledge(packet);
			nack.decode();
			session.checkNACK(nack);
		} else {
			client.handleRaw(packet, msg.sender());
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
	}

}
