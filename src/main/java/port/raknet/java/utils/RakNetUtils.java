/*
 *       _   _____            _      _   _          _   
 *      | | |  __ \          | |    | \ | |        | |  
 *      | | | |__) |   __ _  | | __ |  \| |   ___  | |_ 
 *  _   | | |  _  /   / _` | | |/ / | . ` |  / _ \ | __|
 * | |__| | | | \ \  | (_| | |   <  | |\  | |  __/ | |_ 
 *  \____/  |_|  \_\  \__,_| |_|\_\ |_| \_|  \___|  \__|
 *                                                  
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Trent Summerlin

 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.  
 */
package port.raknet.java.utils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;

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
import port.raknet.java.protocol.raknet.UnconnectedConnectionRequestOne;
import port.raknet.java.protocol.raknet.UnconnectedPing;
import port.raknet.java.protocol.raknet.UnconnectedPong;

/**
 * Used to easily accomplish RakNet related tasks
 *
 * @author Trent Summerlin
 */
public class RakNetUtils implements RakNet {

	private static int pingId;

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
					.option(ChannelOption.SO_RCVBUF, MINIMUM_TRANSFER_UNIT)
					.option(ChannelOption.SO_SNDBUF, MINIMUM_TRANSFER_UNIT).handler(handler);
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
		UnconnectedPing psr = new UnconnectedPing();
		psr.pingId = pingId++;
		psr.encode();

		Packet sprr = createBootstrapAndSend(address, port, psr, timeout);
		if (sprr != null) {
			if (sprr.getId() == ID_UNCONNECTED_PONG) {
				UnconnectedPong spr = new UnconnectedPong(sprr);
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
		UnconnectedConnectionRequestOne ccoro = new UnconnectedConnectionRequestOne();
		ccoro.mtuSize = MINIMUM_TRANSFER_UNIT;
		ccoro.protocol = (short) protocol;
		ccoro.encode();

		Packet scopo = createBootstrapAndSend(address, port, ccoro, timeout);
		if (scopo != null) {
			return (scopo.getId() == ID_UNCONNECTED_CONNECTION_REPLY_1);
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

	/**
	 * Return's the machines current subnet mask
	 * 
	 * @return InetAddress
	 */
	public static InetAddress getSubnetMask() {
		try {
			NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
			int prefix = networkInterface.getInterfaceAddresses().get(1).getNetworkPrefixLength();
			int shiftby = (1 << 31);
			for (int i = prefix - 1; i > 0; i--) {
				shiftby = (shiftby >> 1);
			}
			String maskString = Integer.toString((shiftby >> 24) & 255) + "." + Integer.toString((shiftby >> 16) & 255)
					+ "." + Integer.toString((shiftby >> 8) & 255) + "." + Integer.toString(shiftby & 255);
			return InetAddress.getByName(maskString);
		} catch (Exception e) {
			return null;
		}
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
