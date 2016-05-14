package port.raknet.java.net;

import java.net.InetSocketAddress;
import java.util.HashMap;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import port.raknet.java.RakNet;
import port.raknet.java.RakNetServer;
import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.raknet.Acknowledge;
import port.raknet.java.protocol.raknet.CustomPacket;

public class RakNetHandler extends SimpleChannelInboundHandler<DatagramPacket>implements RakNet {

	private final RakNetServer server;
	private final int maxSessions;
	private final HashMap<InetSocketAddress, ClientSession> sessions;

	public RakNetHandler(RakNetServer server, int maxSessions) {
		this.server = server;
		this.maxSessions = maxSessions;
		this.sessions = new HashMap<InetSocketAddress, ClientSession>();
	}

	/**
	 * Returns all currently connect ClientSessions
	 * 
	 * @return ClientSession[]
	 */
	public ClientSession[] getSessions() {
		return sessions.values().toArray(new ClientSession[sessions.size()]);
	}

	/**
	 * Returns a ClientSession based on it's InetSocketAddress
	 * 
	 * @param address
	 * @return ClientSession
	 */
	public ClientSession getSession(InetSocketAddress address) {
		return sessions.get(address);
	}

	/**
	 * Removes a ClientSession from the handler based on their remote address
	 * 
	 * @param address
	 */
	public void removeSession(InetSocketAddress address) {
		// TODO: Tell client why they were disconnected
		sessions.remove(address);
	}

	/**
	 * Removes a ClientSession from the handler
	 * 
	 * @param session
	 */
	public void removeSession(ClientSession session) {
		this.removeSession(session.getSocketAddress());
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
		// Verify session
		InetSocketAddress address = msg.sender();
		if (!sessions.containsKey(address)) {
			if (sessions.size() < maxSessions) {
				sessions.put(msg.sender(), new ClientSession(ctx, address));
			} else {
				System.err.println("Too many clients, rejected " + address + "!");
			}
		}

		// Get session
		ClientSession session = sessions.get(address);
		Packet packet = new Packet(msg.content());
		short pid = packet.getId();

		// Handle internal packets here
		session.resetLastReceiveTime();
		if (pid >= CUSTOM_0 && pid <= CUSTOM_F) {
			CustomPacket custom = new CustomPacket(packet);
			custom.decode();
			for (Packet dataPacket : session.handleCustom(custom)) {
				server.handlePacket(dataPacket.getId(), dataPacket, session);
			}
		} else if (pid == ACK) {
			Acknowledge ack = new Acknowledge(packet);
			ack.decode();
			session.checkACK(ack);
		} else if (pid == NACK) {
			Acknowledge nack = new Acknowledge(packet);
			nack.decode();
			session.checkNACK(nack);
		} else {
			server.handleRaw(pid, packet, session);
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		// ctx.close();
	}

}
