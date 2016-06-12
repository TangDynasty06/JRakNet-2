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
package port.raknet.java.client;

import java.net.InetSocketAddress;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import port.raknet.java.RakNet;
import port.raknet.java.event.Hook;
import port.raknet.java.exception.InvalidChannelException;
import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.raknet.internal.Acknowledge;
import port.raknet.java.protocol.raknet.internal.CustomPacket;

/**
 * The internal Netty handler for the client, sends ACK, NACK, and CustomPackets
 * to the client
 *
 * @author Trent Summerlin
 */
@Sharable // Socket will be bound to a socket multiple times
public class RakNetClientHandler extends SimpleChannelInboundHandler<DatagramPacket>implements RakNet {

	private final RakNetClient client;
	private volatile Channel channel;
	protected volatile boolean foundMtu;

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
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		if (cause instanceof IndexOutOfBoundsException) {
			// A bad packet read will not kill us all
		} else {
			client.disconnect(cause);
			client.executeHook(Hook.HANDLER_EXCEPTION_OCCURED, cause, ctx, System.currentTimeMillis());
		}
	}

}
