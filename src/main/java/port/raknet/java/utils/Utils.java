package port.raknet.java.utils;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import port.raknet.java.RakNet;
import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.raknet.StatusRequest;
import port.raknet.java.protocol.raknet.StatusResponse;

public abstract class Utils implements RakNet {

	private static final Random generator = new Random();

	/**
	 * Cycles through an array, if none of the values are null then the array
	 * has been filled
	 * 
	 * @param src
	 * @return boolean
	 */
	public static boolean arrayFilled(Object[] src) {
		for (int i = 0; i < src.length; i++) {
			if (src[i] == null) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Splits an array into more chunks with the specified maximum size for each
	 * array chunk
	 * 
	 * @param src
	 * @param size
	 * @return byte[][]
	 */
	public static byte[][] splitArray(byte[] src, int size) {
		int index = 0;
		ArrayList<byte[]> split = new ArrayList<byte[]>();
		while (index < src.length) {
			if (index + size <= src.length) {
				split.add(Arrays.copyOfRange(src, index, index + size));
				index += size;
			} else {
				split.add(Arrays.copyOfRange(src, index, src.length));
				index = src.length;
			}
		}
		return split.toArray(new byte[split.size()][]);
	}

	/**
	 * Used to quickly send a packet to a sever and get it's response, not
	 * recommended for long time communication!
	 * 
	 * @param address
	 * @param port
	 * @param packet
	 * @param timeout
	 * @return Packet
	 */
	public static Packet createBootstrapAndSend(String address, int port, Packet packet, long timeout) {
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			// Create bootstrap, bind and send
			Bootstrap b = new Bootstrap();
			BootstrapCreateHandler handler = new BootstrapCreateHandler();
			b.group(group).channel(NioDatagramChannel.class).option(ChannelOption.SO_BROADCAST, true)
					.option(ChannelOption.SO_RCVBUF, 128).option(ChannelOption.SO_SNDBUF, 128).handler(handler);
			b.bind(0).sync().channel()
					.writeAndFlush(new DatagramPacket(packet.buffer(), new InetSocketAddress(address, port)));

			// Wait for packet to come in, return null on timeout
			long waitTime = System.currentTimeMillis();
			while (true) {
				if (System.currentTimeMillis() - waitTime >= timeout) {
					return null;
				} else if (packet != null) {
					return packet;
				}
			}
		} catch (Exception e) {
			group.shutdownGracefully();
			return null;
		}
	}

	/**
	 * Used to quickly send a packet to a sever and get it's response, not
	 * recommended for long time communication!
	 * 
	 * @param address
	 * @param port
	 * @param packet
	 * @return Packet
	 */
	public static Packet createBootstrapAndSend(String address, int port, Packet packet) {
		return createBootstrapAndSend(address, port, packet, 1000L);
	}

	public static String getServerIdentifier(String address, int port) {
		StatusRequest psr = new StatusRequest();
		psr.pingId = generator.nextLong();
		psr.encode();

		Packet sprr = createBootstrapAndSend(address, port, psr);
		if (sprr != null) {
			StatusResponse spr = new StatusResponse(sprr);
			spr.decode();
			if (spr.magic == true && spr.pingId == psr.pingId) {
				return spr.identifier;
			}
		}
		return null;
	}

	public static void main(String[] args) {
		System.out.println(getServerIdentifier("192.168.1.14", 19132));
	}

}
