package port.raknet.java.client;

import java.net.InetSocketAddress;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import port.raknet.java.RakNet;
import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.raknet.internal.Acknowledge;
import port.raknet.java.protocol.raknet.internal.CustomPacket;

/**
 * The internal Netty handler for the client, sends ACK, NACK, and CustomPackets
 * to the client
 *
 * @author Trent Summerlin
 */
public class RakNetClientHandler extends SimpleChannelInboundHandler<DatagramPacket>implements RakNet {

	private final RakNetClient client;
	protected volatile boolean foundMtu;

	public RakNetClientHandler(RakNetClient client) {
		this.client = client;
	}

	@Override
	protected final void messageReceived(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
		InetSocketAddress sender = msg.sender();
		Packet packet = new Packet(msg.content().retain());
		short pid = packet.getId();

		// Make sure the server has accepted the MTU
		if (!foundMtu) {
			if (pid == ID_UNCONNECTED_CONNECTION_REPLY_1) {
				this.foundMtu = true;
			}
		}

		// Handle internal packets here
		if (pid >= ID_CUSTOM_0 && pid <= ID_CUSTOM_F) {
			CustomPacket custom = new CustomPacket(packet);
			custom.decode();
			client.handleCustom(custom, sender);
		} else if (pid == ID_ACK) {
			Acknowledge ack = new Acknowledge(packet);
			ack.decode();
			client.handleAck(ack, sender);
		} else if (pid == ID_NACK) {
			Acknowledge nack = new Acknowledge(packet);
			nack.decode();
			client.handleNack(nack, sender);
		} else {
			client.handleRaw(packet, sender);
		}
		msg.release(msg.refCnt() - 1);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
	}

}
