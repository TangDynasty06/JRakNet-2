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
package port.raknet.java.session;

import java.net.InetSocketAddress;

import io.netty.channel.Channel;
import port.raknet.java.event.Hook;
import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.Reliability;
import port.raknet.java.protocol.raknet.ConnectedClientHandshake;
import port.raknet.java.protocol.raknet.ConnectedConnectRequest;
import port.raknet.java.protocol.raknet.ConnectedPing;
import port.raknet.java.protocol.raknet.ConnectedPong;
import port.raknet.java.protocol.raknet.ConnectedServerHandshake;
import port.raknet.java.protocol.raknet.internal.EncapsulatedPacket;
import port.raknet.java.server.RakNetServer;
import port.raknet.java.server.RakNetServerHandler;

/**
 * Used by <code>RakNetServer</code> to handle a connected client
 *
 * @author Trent Summerlin
 */
public class ClientSession extends RakNetSession {

	private final RakNetServerHandler handler;
	private final RakNetServer server;
	private SessionState state;

	public ClientSession(Channel channel, InetSocketAddress address, RakNetServerHandler handler, RakNetServer server) {
		super(channel, address);
		this.handler = handler;
		this.server = server;
		this.state = SessionState.DISCONNECTED;
	}

	/**
	 * Returns the client's current RakNet state
	 * 
	 * @return SessionState
	 */
	public SessionState getState() {
		return this.state;
	}

	/**
	 * Set the client's specified RakNet state
	 * 
	 * @param state
	 */
	public void setState(SessionState state) {
		this.state = state;
	}

	@Override
	public void handleEncapsulated(EncapsulatedPacket encapsulated) {
		Packet packet = encapsulated.convertPayload();
		short pid = packet.getId();

		// Handled depending on ClientState
		if (pid == ID_CONNECTED_PING) {
			if (this.getState().getOrder() >= SessionState.CONNECTING_1.getOrder()) {
				ConnectedPing cp = new ConnectedPing(packet);
				cp.decode();

				ConnectedPong sp = new ConnectedPong();
				sp.pingId = cp.pingId;
				sp.encode();
				this.sendPacket(Reliability.UNRELIABLE, sp);
			}
		} else if (pid == ID_CONNECTED_PONG) {
			this.resetLastReceiveTime();
		} else if (pid == ID_CONNECTED_CLIENT_CONNECT_REQUEST) {
			if (this.getState() == SessionState.CONNECTING_2) {
				ConnectedConnectRequest cchr = new ConnectedConnectRequest(packet);
				cchr.decode();

				ConnectedServerHandshake scha = new ConnectedServerHandshake();
				scha.clientAddress = this.getSocketAddress();
				scha.timestamp = cchr.timestamp;
				scha.serverTimestamp = server.getTimestamp();
				scha.encode();

				this.sendPacket(Reliability.UNRELIABLE, scha);
				this.setState(SessionState.HANDSHAKING);
			}
		} else if (pid == ID_CONNECTED_CLIENT_HANDSHAKE) {
			if (this.getState() == SessionState.HANDSHAKING) {
				ConnectedClientHandshake cch = new ConnectedClientHandshake(packet);
				cch.decode();

				this.setState(SessionState.CONNECTED);
				server.executeHook(Hook.SESSION_CONNECTED, this, System.currentTimeMillis());
			}
		} else if (pid == ID_CONNECTED_CANCEL_CONNECTION) {
			handler.removeSession(this, "Client disconnected");
		} else if (this.getState() == SessionState.CONNECTED) {
			server.executeHook(Hook.PACKET_RECEIVED, this, encapsulated);
		}
	}

}
