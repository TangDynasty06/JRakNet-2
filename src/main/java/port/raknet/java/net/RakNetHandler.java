package port.raknet.java.net;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import port.raknet.java.RakNet;
import port.raknet.java.RakNetServer;
import port.raknet.java.event.Hook;
import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.raknet.Acknowledge;
import port.raknet.java.protocol.raknet.CustomPacket;

/**
 * The internal Netty handler for the server, handles ACK, NACK, and Custompackets on it's own. Unconnected 
 *
 * @author Trent Summerlin
 */
public class RakNetHandler extends SimpleChannelInboundHandler<DatagramPacket>implements RakNet {

	private final RakNetServer server;
	private final int maxSessions;
	private final ArrayList<InetSocketAddress> blocked;
	private final HashMap<InetSocketAddress, ClientSession> sessions;

	public RakNetHandler(RakNetServer server, int maxSessions) {
		this.server = server;
		this.maxSessions = maxSessions;
		this.blocked = new ArrayList<InetSocketAddress>();
		this.sessions = new HashMap<InetSocketAddress, ClientSession>();
	}

	/**
	 * Blocks the specified address
	 * 
	 * @param address
	 */
	public void blockAddress(InetSocketAddress address) {
		blocked.add(address);
	}

	/**
	 * Unblocks the specified address
	 * 
	 * @param address
	 */
	public void unblockAddress(InetSocketAddress address) {
		blocked.remove(address);
	}

	/**
	 * Returns the blocked addresses
	 * 
	 * @return InetSocketAddress
	 */
	public InetSocketAddress[] getBlockedAddress() {
		return blocked.toArray(new InetSocketAddress[blocked.size()]);
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
	public void removeSession(InetSocketAddress address, String reason) {
		server.executeHook(Hook.CLIENT_DISCONNECTED, sessions.get(address), reason, System.currentTimeMillis());
		sessions.remove(address);
	}

	/**
	 * Removes a ClientSession from the handler with the specified reason
	 * 
	 * @param session
	 * @param reason
	 */
	public void removeSession(ClientSession session, String reason) {
		this.removeSession(session.getSocketAddress(), reason);
	}

	@Override
	protected final void messageReceived(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
		System.out.println(Unpooled.copiedBuffer(msg.content()).array().length);
		if (!blocked.contains(msg.sender())) {
			// Verify session
			InetSocketAddress address = msg.sender();
			if (!sessions.containsKey(address)) {
				if (sessions.size() < maxSessions) {
					sessions.put(msg.sender(), new ClientSession(server, this, ctx, address));
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
			if (pid >= ID_CUSTOM_0 && pid <= ID_CUSTOM_F) {
				CustomPacket custom = new CustomPacket(packet);
				custom.decode();
				session.handleCustom(custom);
			} else if (pid == ACK) {
				Acknowledge ack = new Acknowledge(packet);
				ack.decode();
				session.checkACK(ack);
			} else if (pid == NACK) {
				Acknowledge nack = new Acknowledge(packet);
				nack.decode();
				session.checkNACK(nack);
			} else {
				server.handleRaw(packet, session);
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
	}

}