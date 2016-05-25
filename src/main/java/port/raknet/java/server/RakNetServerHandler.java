package port.raknet.java.server;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import port.raknet.java.RakNet;
import port.raknet.java.event.Hook;
import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.raknet.internal.Acknowledge;
import port.raknet.java.protocol.raknet.internal.CustomPacket;
import port.raknet.java.session.ClientSession;

/**
 * The internal Netty handler for the server, handles ACK, NACK, and
 * CustomPackets on its own
 *
 * @author Trent Summerlin
 */
public class RakNetServerHandler extends SimpleChannelInboundHandler<DatagramPacket>implements RakNet {

	private final RakNetServer server;
	private final int maxSessions;
	private final ArrayList<InetSocketAddress> blocked;
	private final HashMap<InetSocketAddress, ClientSession> sessions;

	public RakNetServerHandler(RakNetServer server, int maxSessions) {
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
		server.executeHook(Hook.SESSION_DISCONNECTED, sessions.get(address), reason, System.currentTimeMillis());
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
		if (!blocked.contains(msg.sender())) {
			// Verify session
			InetSocketAddress address = msg.sender();
			if (!sessions.containsKey(address)) {
				if (sessions.size() < maxSessions) {
					sessions.put(msg.sender(), new ClientSession(ctx.channel(), address, this, server));
				} else {
					System.err.println("Too many clients, rejected " + address + "!");
				}
			}

			// Get session
			ClientSession session = sessions.get(address);
			Packet packet = new Packet(msg.content().retain());
			short pid = packet.getId();

			// Handle internal packets here
			session.resetLastReceiveTime();
			if (pid >= ID_CUSTOM_0 && pid <= ID_CUSTOM_F) {
				CustomPacket custom = new CustomPacket(packet);
				custom.decode();
				session.handleCustom0(custom);
			} else if (pid == ID_ACK) {
				Acknowledge ack = new Acknowledge(packet);
				ack.decode();
			} else if (pid == ID_NACK) {
				Acknowledge nack = new Acknowledge(packet);
				nack.decode();
				session.checkNACK(nack);
			} else {
				server.handleRaw(packet, session);
			}
		}
		msg.release(msg.refCnt() - 1);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
	}

}