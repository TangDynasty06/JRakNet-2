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
package net.marfgamer.raknet.utils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import net.marfgamer.raknet.RakNet;
import net.marfgamer.raknet.protocol.Message;
import net.marfgamer.raknet.protocol.identifier.MessageIdentifiers;
import net.marfgamer.raknet.protocol.raknet.UnconnectedConnectionRequestOne;
import net.marfgamer.raknet.protocol.raknet.UnconnectedPing;
import net.marfgamer.raknet.protocol.raknet.UnconnectedPong;

/**
 * Used to easily accomplish RakNet related tasks
 *
 * @author Trent Summerlin
 */
public abstract class RakNetUtils implements RakNet, MessageIdentifiers {

	private static long raknetId = getRakNetID();

	/**
	 * Used to get the ID used by RakNet assigned to this system
	 * 
	 * @return long
	 */
	public static long getRakNetID() {
		try {
			InetAddress localHost = InetAddress.getLocalHost();
			String systemName = localHost.getHostName();
			return systemName.hashCode();
		} catch (UnknownHostException e) {
			return -1;
		}
	}

	/**
	 * Used to get the maximum transfer unit on the device based on the network
	 * interface address
	 * 
	 * @return int
	 */
	public static int getNetworkInterfaceMTU() {
		try {
			Enumeration<NetworkInterface> ni = NetworkInterface.getNetworkInterfaces();
			while (ni.hasMoreElements()) {
				NetworkInterface networkInterface = ni.nextElement();
				Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress address = addresses.nextElement();
					if (address.equals(InetAddress.getLocalHost())) {
						return networkInterface.getMTU();
					}
				}
			}
			return MINIMUM_TRANSFER_UNIT; // We couldn't find a match
		} catch (SocketException | UnknownHostException e) {
			return MINIMUM_TRANSFER_UNIT; // An error occurred
		}
	}

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
	public static Message createBootstrapAndSend(String address, int port, Message send, long timeout) {
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
	public static Message createBootstrapAndSend(String address, int port, Message packet) {
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
		UnconnectedPing ping = new UnconnectedPing();
		ping.pingId = System.currentTimeMillis();
		ping.clientId = raknetId;
		ping.encode();

		Message sprr = createBootstrapAndSend(address, port, ping, timeout);
		if (sprr != null) {
			if (sprr.getId() == ID_UNCONNECTED_PONG) {
				UnconnectedPong pong = new UnconnectedPong(sprr);
				pong.decode();
				if (pong.magic == true && pong.pingId == ping.pingId) {
					return pong.identifier;
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
	 * Makes sure the server with the specified address is online
	 * 
	 * @param address
	 * @param port
	 * @param timeout
	 * @return boolean
	 */
	public static boolean isServerOnline(String address, int port, long timeout) {
		UnconnectedConnectionRequestOne request = new UnconnectedConnectionRequestOne();
		request.mtuSize = MINIMUM_TRANSFER_UNIT;
		request.protocol = CLIENT_NETWORK_PROTOCOL;
		request.encode();

		Message response = createBootstrapAndSend(address, port, request, timeout);
		return (response != null);
	}

	/**
	 * Makes sure the server with the specified address is online
	 * 
	 * @param address
	 * @param port
	 * @return boolean
	 */
	public static boolean isServerOnline(String address, int port) {
		return isServerOnline(address, port, 1000L);
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
		UnconnectedConnectionRequestOne request = new UnconnectedConnectionRequestOne();
		request.mtuSize = MINIMUM_TRANSFER_UNIT;
		request.protocol = (short) protocol;
		request.encode();

		Message response = createBootstrapAndSend(address, port, request, timeout);
		if (response != null) {
			return (response.getId() == ID_UNCONNECTED_CONNECTION_REPLY_1
					&& response.getId() == ID_UNCONNECTED_CONNECTION_REPLY_1);
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
		return isServerCompatible(address, port, CLIENT_NETWORK_PROTOCOL, timeout);
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

		public volatile Message packet;

		@Override
		protected void messageReceived(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
			this.packet = new Message(msg.content().retain());
		}

		@Override
		public void channelReadComplete(ChannelHandlerContext ctx) {
			ctx.flush();
		}

	}

}
