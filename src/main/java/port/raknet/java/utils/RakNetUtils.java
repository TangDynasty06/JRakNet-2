package port.raknet.java.utils;

import java.net.InetSocketAddress;
import java.util.Random;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import port.raknet.java.RakNet;
import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.raknet.ConnectionOpenRequestOne;
import port.raknet.java.protocol.raknet.StatusRequest;
import port.raknet.java.protocol.raknet.StatusResponse;

public class RakNetUtils implements RakNet {

	private static final Random generator = new Random();
	private static final int mtuSize = 2048;

	/**
	 * Used to quickly send a packet to a sever and get it's response, do
	 * <b><i><u>NOT</u></i></b> use for long time communication!
	 * 
	 * @param address
	 * @param port
	 * @param packet
	 * @param timeout
	 * @return Packet
	 */
	public static Packet createBootstrapAndSend(String address, int port, Packet send, long timeout) {
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			// Create bootstrap, bind and send
			Bootstrap b = new Bootstrap();
			BootstrapHandler handler = new BootstrapHandler();
			b.group(group).channel(NioDatagramChannel.class).option(ChannelOption.SO_BROADCAST, true)
					.option(ChannelOption.SO_RCVBUF, mtuSize).option(ChannelOption.SO_SNDBUF, mtuSize).handler(handler);
			b.bind(0).sync().channel()
					.writeAndFlush(new DatagramPacket(send.buffer(), new InetSocketAddress(address, port)));

			// Wait for packet to come in, return null on timeout
			long waitTime = System.currentTimeMillis();
			synchronized (handler) {
				while (System.currentTimeMillis() - waitTime <= timeout) {
					if (handler.packet != null) {
						return handler.packet;
					}
				}
			}
			return null;
		} catch (Exception e) {
			group.shutdownGracefully();
			return null;
		}
	}

	/**
	 * Used to quickly send a packet to a sever and get it's response after no
	 * longer than 1000 MS, do <b><i><u>NOT</u></i></b> use for long time
	 * communication!
	 * 
	 * @param address
	 * @param port
	 * @param packet
	 * @param timeout
	 * @return Packet
	 */
	public static Packet createBootstrapAndSend(String address, int port, Packet packet) {
		return createBootstrapAndSend(address, port, packet, 1000L);
	}

	/**
	 * Returns the specified server's identifier
	 * 
	 * @param address
	 * @param port
	 * @return String
	 */
	public static String getServerIdentifier(String address, int port, long timeout) {
		StatusRequest psr = new StatusRequest();
		psr.pingId = generator.nextLong();
		psr.encode();

		Packet sprr = createBootstrapAndSend(address, port, psr, timeout);
		if (sprr != null) {
			if (sprr.getId() == ID_UNCONNECTED_STATUS_RESPONSE) {
				StatusResponse spr = new StatusResponse(sprr);
				spr.decode();
				if (spr.magic == true && spr.pingId == psr.pingId) {
					return spr.identifier;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the specified server's identifier
	 * 
	 * @param address
	 * @param port
	 * @return String
	 */
	public static String getServerIdentifier(String address, int port) {
		return getServerIdentifier(address, port, 1000L);
	}

	/**
	 * Makes sure the specified protocol is compatible with the server
	 * 
	 * @param address
	 * @param port
	 * @param protocol
	 * @return boolean
	 */
	public static boolean isServerCompatible(String address, int port, int protocol, long timeout) {
		ConnectionOpenRequestOne ccoro = new ConnectionOpenRequestOne();
		ccoro.mtuSize = mtuSize;
		ccoro.protocol = (short) protocol;
		ccoro.encode();

		Packet scopo = createBootstrapAndSend(address, port, ccoro, timeout);
		if (scopo != null) {
			return (scopo.getId() != ID_INCOMPATIBLE_PROTOCOL_VERSION
					&& scopo.getId() == ID_UNCONNECTED_OPEN_CONNECTION_REPLY_1);
		}
		return false;
	}

	/**
	 * Makes sure the specified protocol is compatible with the server
	 * 
	 * @param address
	 * @param port
	 * @param protocol
	 * @return boolean
	 */
	public static boolean isServerCompatible(String address, int port, int protocol) {
		return isServerCompatible(address, port, protocol, 1000L);
	}

	/**
	 * Makes sure the current protocol is compatible with the server
	 * 
	 * @param address
	 * @param port
	 * @param protocol
	 * @return boolean
	 */
	public static boolean isServerCompatible(String address, int port, long timeout) {
		return isServerCompatible(address, port, RakNet.NETWORK_PROTOCOL, timeout);
	}

	/**
	 * Makes sure the current protocol is compatible with the server
	 * 
	 * @param address
	 * @param port
	 * @param protocol
	 * @return boolean
	 */
	public static boolean isServerCompatible(String address, int port) {
		return isServerCompatible(address, port, 1000L);
	}

	private static class BootstrapHandler extends SimpleChannelInboundHandler<DatagramPacket> {

		public volatile Packet packet;

		@Override
		protected void messageReceived(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
			this.packet = new Packet(msg.content().retain());
		}

		@Override
		public void channelReadComplete(ChannelHandlerContext ctx) {
			ctx.flush();
		}

	}

}
