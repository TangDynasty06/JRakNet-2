package port.raknet.java.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import port.raknet.java.RakNet;
import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.raknet.internal.Acknowledge;
import port.raknet.java.protocol.raknet.internal.CustomPacket;
import port.raknet.java.session.ServerSession;

/**
 * The internal Netty handler for the client, handles ACK, NACK, and
 * CustomPackets on its own
 *
 * @author Trent Summerlin
 */
public class RakNetClientHandler extends SimpleChannelInboundHandler<DatagramPacket>implements RakNet {

	private final RakNetClient client;

	public RakNetClientHandler(RakNetClient client) {
		this.client = client;
	}

	@Override
	protected final void messageReceived(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
		ServerSession session = client.getSession();
		Packet packet = new Packet(msg.content().retain());
		short pid = packet.getId();

		if (session != null) {
			session.resetLastReceiveTime();
		}

		// Handle internal packets here
		if (pid >= ID_CUSTOM_0 && pid <= ID_CUSTOM_F) {
			if (session != null) {
				CustomPacket custom = new CustomPacket(packet);
				custom.decode();
				session.handleCustom0(custom);
			}
		} else if (pid == ID_ACK) {
			if (session != null) {
				Acknowledge ack = new Acknowledge(packet);
				ack.decode();
			}
		} else if (pid == ID_NACK) {
			if (session != null) {
				Acknowledge nack = new Acknowledge(packet);
				nack.decode();
				session.checkNACK(nack);
			}
		} else {
			client.handleRaw(packet, msg.sender());
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
	}

}
