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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import net.marfgamer.raknet.RakNet;
import net.marfgamer.raknet.event.Hook;
import net.marfgamer.raknet.protocol.MessageIdentifiers;
import net.marfgamer.raknet.protocol.Packet;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.protocol.raknet.ConnectedCloseConnection;
import net.marfgamer.raknet.protocol.raknet.UnconnectedConnectionBanned;
import net.marfgamer.raknet.protocol.raknet.internal.Acknowledge;
import net.marfgamer.raknet.protocol.raknet.internal.CustomPacket;
import net.marfgamer.raknet.session.ClientSession;
import net.marfgamer.raknet.session.SessionState;

/**
 * The internal Netty handler for the server, handles ACK, NACK, and
 * CustomPackets on its own
 *
 * @author Trent Summerlin
 */
public class RakNetServerHandler extends SimpleChannelInboundHandler<DatagramPacket>
		implements RakNet, MessageIdentifiers {

	private final RakNetServer server;
	private final ConcurrentHashMap<InetSocketAddress, ClientSession> sessions;
	private final ConcurrentHashMap<InetAddress, BlockedAddress> blocked;

	// Used in exception handling
	private InetSocketAddress lastSender;

	public RakNetServerHandler(RakNetServer server) {
		this.server = server;
		this.sessions = new ConcurrentHashMap<InetSocketAddress, ClientSession>();
		this.blocked = new ConcurrentHashMap<InetAddress, BlockedAddress>();
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
		if (sessions.containsKey(address)) {
			ClientSession session = sessions.remove(address);
			session.sendPacket(Reliability.RELIABLE, new ConnectedCloseConnection());
			if (session.getState() == SessionState.CONNECTED) {
				server.executeHook(Hook.SESSION_DISCONNECTED, session, reason);
			}
		}
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

	/**
	 * Blocks the specified address with the specified reason and the specified
	 * time in milliseconds
	 * 
	 * @param address
	 * @param reason
	 * @param time
	 */
	public void blockAddress(InetAddress address, long time) {
		if (!blocked.containsKey(address)) {
			for (ClientSession session : this.getSessions()) {
				if (session.getAddress().equals(address)) {
					this.removeSession(session, "Address blocked");
				}
			}
			blocked.put(address, new BlockedAddress(address, time));
			server.executeHook(Hook.CLIENT_ADDRESS_BLOCKED, blocked.get(address));
		}
	}

	/**
	 * Unblocks the specified address
	 * 
	 * @param address
	 */
	public void unblockAddress(InetAddress address) {
		if (blocked.containsKey(address)) {
			server.executeHook(Hook.CLIENT_ADDRESS_UNBLOCKED, blocked.get(address));
			blocked.remove(address);
		}
	}

	/**
	 * Unblocks the specified blocked address
	 * 
	 * @param client
	 */
	public void unblockAddress(BlockedAddress address) {
		this.unblockAddress(address.address);
	}

	/**
	 * Returns all the blocked clients
	 * 
	 * @return BlockedAddress[]
	 */
	public BlockedAddress[] getBlockedAddresses() {
		return blocked.values().toArray(new BlockedAddress[blocked.size()]);
	}

	/**
	 * Returns a blocked address depending by its address
	 * 
	 * @param address
	 * @return BlockAddress
	 */
	public BlockedAddress getBlockedAddress(InetAddress address) {
		for (BlockedAddress blocked : getBlockedAddresses()) {
			if (blocked.address.equals(address)) {
				return blocked;
			}
		}
		return null;
	}

	@Override
	protected final void messageReceived(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
		// Make sure client address is not blocked
		if (!blocked.containsKey(msg.sender().getAddress())) {
			// Verify session
			InetSocketAddress address = msg.sender();
			if (!sessions.containsKey(address)) {
				sessions.put(msg.sender(), new ClientSession(ctx.channel(), address, this, server));
			}
			this.lastSender = msg.sender();

			// Get session
			ClientSession session = sessions.get(address);
			Packet packet = new Packet(msg.content().retain());
			short pid = packet.getId();

			// Make sure we haven't received too many packets too fast
			session.pushReceivedPacketsThisSecond();
			if (session.getReceivedPacketsThisSecond() > MAX_PACKETS_PER_SECOND) {
				this.blockAddress(session.getAddress(), FIVE_MINUTES_MILLIS);
			}

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
		} else {
			ctx.writeAndFlush(new DatagramPacket(new UnconnectedConnectionBanned().buffer(), msg.sender()));
			ctx.writeAndFlush(new DatagramPacket(new ConnectedCloseConnection().buffer(), msg.sender()));
		}

		// Release message
		while (msg.refCnt() > 1) {
			msg.release();
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		ClientSession session = this.getSession(lastSender);
		if (session != null) {
			this.removeSession(this.getSession(lastSender), cause.getLocalizedMessage());
			this.blockAddress(lastSender.getAddress(), FIVE_MINUTES_MILLIS);
		}
		server.executeHook(Hook.HANDLER_EXCEPTION_OCCURED, cause, lastSender);
	}

}