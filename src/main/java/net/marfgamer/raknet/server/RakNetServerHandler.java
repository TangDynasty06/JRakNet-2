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
package net.marfgamer.raknet.server;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import net.marfgamer.raknet.RakNet;
import net.marfgamer.raknet.event.Hook;
import net.marfgamer.raknet.protocol.Packet;
import net.marfgamer.raknet.protocol.raknet.internal.Acknowledge;
import net.marfgamer.raknet.protocol.raknet.internal.CustomPacket;
import net.marfgamer.raknet.session.ClientSession;

/**
 * The internal Netty handler for the server, handles ACK, NACK, and
 * CustomPackets on its own
 *
 * @author Trent Summerlin
 */
public class RakNetServerHandler extends SimpleChannelInboundHandler<DatagramPacket>implements RakNet {

	private final RakNetServer server;
	private final ArrayList<InetSocketAddress> blocked;
	private final HashMap<InetSocketAddress, ClientSession> sessions;

	public RakNetServerHandler(RakNetServer server) {
		this.server = server;
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
				sessions.put(msg.sender(), new ClientSession(ctx.channel(), address, this, server));
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
				session.handleAck(ack);
			} else if (pid == ID_NACK) {
				Acknowledge nack = new Acknowledge(packet);
				nack.decode();
				session.handleNack(nack);
			} else {
				server.handleRaw(packet, session);
			}
		}
		msg.release(msg.refCnt() - 1);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		if (cause instanceof IndexOutOfBoundsException) {
			// A bad packet read will not kill us all
		} else {
			server.executeHook(Hook.HANDLER_EXCEPTION_OCCURED, cause, ctx, System.currentTimeMillis());
		}
	}

}