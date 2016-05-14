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
		new ClientChecker().start();
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

		// Handle special packet types here so the server doesn't have too
		session.updateLastReceiveTime();
		if (pid >= CUSTOM_0 && pid <= CUSTOM_F) {
			CustomPacket custom = new CustomPacket(packet);
			custom.decode();
			for (Packet dataPacket : session.handleCustom(custom)) {
				server.handleDataPacket(dataPacket.getId(), dataPacket, session);
			}
		} else if (pid == ACK) {
			Acknowledge ack = new Acknowledge(ACK);
			ack.decode();
			session.checkACK(ack);
		} else if (pid == NACK) {
			Acknowledge nack = new Acknowledge(NACK);
			nack.decode();
			session.checkNACK(nack);
		} else {
			server.handlePacket(pid, packet, session);
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

	class ClientChecker extends Thread {
		public void run() {
			long last = System.currentTimeMillis();
			while (true) {
				long current = System.currentTimeMillis();
				if (current - last == 50) { // Check once every 20th of a second
					for (ClientSession session : sessions.values()) {
						if(session.getLastReceiveTime() > 5000L) {
							System.out.println("Removed session " + session.getAddress() + " due to timeout");
						}
					}
					last = current;
				}
			}
		}

	}

}
