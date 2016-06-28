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
package net.marfgamer.raknet.client;

import java.net.InetSocketAddress;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import net.marfgamer.raknet.event.Hook;
import net.marfgamer.raknet.exception.InvalidChannelException;
import net.marfgamer.raknet.protocol.Packet;
import net.marfgamer.raknet.protocol.identifier.MessageIdentifiers;
import net.marfgamer.raknet.protocol.raknet.internal.Acknowledge;
import net.marfgamer.raknet.protocol.raknet.internal.CustomPacket;
import net.marfgamer.raknet.session.ServerSession;

/**
 * The internal Netty handler for the client, sends ACK, NACK, and CustomPackets
 * to the client
 *
 * @author Trent Summerlin
 */
@Sharable // Socket will be bound to a socket multiple times
public class RakNetClientHandler extends SimpleChannelInboundHandler<DatagramPacket>implements MessageIdentifiers {

	private final RakNetClient client;
	private volatile Channel channel;
	protected volatile boolean foundMtu;

	// Used in exception handling
	private InetSocketAddress lastSender;

	public RakNetClientHandler(RakNetClient client) {
		this.client = client;
	}

	protected void resetHandler() {
		this.foundMtu = false;
	}

	protected void setChannel(Channel channel) {
		this.channel = channel;
	}

	protected Channel getChannel() {
		return this.channel;
	}

	@Override
	protected final void messageReceived(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
		if (channel != null) {
			// Make sure current channel is the one on this context's channel
			if (!channel.equals(ctx.channel())) {
				throw new InvalidChannelException(ctx.channel(), channel);
			}
			this.lastSender = msg.sender();
			InetSocketAddress sender = msg.sender();

			// Get packet
			Packet packet = new Packet(msg.content().retain());
			short pid = packet.getId();

			// Update client server info
			if (client.isServer(sender)) {
				client.resetLastReceiveTime();
				client.pushPacketsThisSecond();

				if (!foundMtu) {
					if (pid == ID_UNCONNECTED_CONNECTION_REPLY_1) {
						this.foundMtu = true;
					}
				}
			}

			// Handle internal packets
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

			// Release message
			while (msg.refCnt() > 1) {
				msg.release();
			}
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		// Disconnect only if it's the server
		ServerSession session = client.getSession();
		if (session != null) {
			if (session.isServer(lastSender)) {
				client.disconnect(cause);
				ctx.close();

				// We only care if it's the server
				client.executeHook(Hook.HANDLER_EXCEPTION_OCCURED, cause, session);
			}
		}

		// cause.printStackTrace(); /* <- Uncomment for debug */
	}

}
