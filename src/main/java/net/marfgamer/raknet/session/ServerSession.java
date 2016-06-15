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
package net.marfgamer.raknet.session;

import java.net.InetSocketAddress;

import io.netty.channel.Channel;
import net.marfgamer.raknet.client.RakNetClient;
import net.marfgamer.raknet.event.Hook;
import net.marfgamer.raknet.protocol.Packet;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.protocol.raknet.ConnectedClientHandshake;
import net.marfgamer.raknet.protocol.raknet.ConnectedPing;
import net.marfgamer.raknet.protocol.raknet.ConnectedPong;
import net.marfgamer.raknet.protocol.raknet.ConnectedServerHandshake;
import net.marfgamer.raknet.protocol.raknet.internal.EncapsulatedPacket;

/**
 * Used by <code>RakNetClient</code> to handle the connected server
 *
 * @author Trent Summerlin
 */
public class ServerSession extends RakNetSession {

	private final RakNetClient client;

	public ServerSession(Channel channel, InetSocketAddress address, RakNetClient client) {
		super(channel, address);
		this.client = client;
	}

	/**
	 * Returns whether or not the address is the same address as the server's
	 * address
	 * 
	 * @param address
	 */
	public boolean isServer(InetSocketAddress address) {
		return this.getSocketAddress().equals(address);
	}

	@Override
	public void handleEncapsulated(EncapsulatedPacket encapsulated) {
		Packet packet = encapsulated.convertPayload();
		short pid = packet.getId();

		if (pid == ID_CONNECTED_SERVER_HANDSHAKE) {
			if (client.getState() == SessionState.HANDSHAKING) {
				ConnectedServerHandshake serverHandshake = new ConnectedServerHandshake(packet);
				serverHandshake.decode();

				if (serverHandshake.timestamp == client.getTimestamp()) {
					ConnectedClientHandshake clientHandshake = new ConnectedClientHandshake();
					clientHandshake.clientAddress = client.getLocalAddress();
					clientHandshake.serverTimestamp = serverHandshake.serverTimestamp;
					clientHandshake.timestamp = client.getTimestamp();
					clientHandshake.encode();

					this.sendPacket(Reliability.RELIABLE, clientHandshake);
				}

				client.setState(SessionState.CONNECTED);
				client.executeHook(Hook.SESSION_CONNECTED, client.getSession(), System.currentTimeMillis());
			}
		} else if (pid == ID_CONNECTED_PING) {
			ConnectedPing ping = new ConnectedPing(packet);
			ping.decode();

			ConnectedPong pong = new ConnectedPong();
			pong.pingTime = ping.pingTime;
			pong.pongTime = System.currentTimeMillis();
			pong.encode();

			this.sendPacket(Reliability.RELIABLE, pong);
		} else if (pid == ID_CONNECTED_PONG) {
			this.resetLastReceiveTime();
		} else if (client.getState() == SessionState.CONNECTED) {
			client.executeHook(Hook.PACKET_RECEIVED, client.getSession(), encapsulated);
		}
	}

}
