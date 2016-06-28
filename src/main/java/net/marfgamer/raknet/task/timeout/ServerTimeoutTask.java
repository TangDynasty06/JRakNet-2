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
package net.marfgamer.raknet.task.timeout;

import net.marfgamer.raknet.client.RakNetClient;
import net.marfgamer.raknet.event.Hook;
import net.marfgamer.raknet.exception.packet.UnexpectedPacketException;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.protocol.identifier.MessageIdentifiers;
import net.marfgamer.raknet.protocol.raknet.ConnectedPing;
import net.marfgamer.raknet.protocol.raknet.ConnectedPong;
import net.marfgamer.raknet.session.ServerSession;
import net.marfgamer.raknet.task.TaskRunnable;

/**
 * Used by <code>RakNetClient</code> to make sure the server does not timeout
 *
 * @author Trent Summerlin
 */
public class ServerTimeoutTask implements TaskRunnable, MessageIdentifiers {

	private final RakNetClient client;
	private ConnectedPing lastPing;
	private long lastPingTime;

	public ServerTimeoutTask(RakNetClient client) {
		this.client = client;
	}

	@Override
	public long getWaitTimeMillis() {
		return 100L;
	}

	/**
	 * Handles a <code>ID_CONNECTED_PONG</code> packet and sets the server's
	 * latency data if the packet data is correct
	 * 
	 * @param session
	 * @param pong
	 * @throws UnexpectedPacketException
	 */
	public void handledConnectedPong(ConnectedPong pong) throws UnexpectedPacketException {
		ServerSession session = client.getSession();
		if (session != null && lastPing != null && lastPingTime != 0) {
			if (pong.getId() == ID_CONNECTED_PONG) {
				if (pong.pingTime == lastPing.pingTime) {
					long latency = System.currentTimeMillis() - lastPingTime;
					if (latency >= 0) {
						session.setLatency(latency);
						client.executeHook(Hook.LATENCY_UPDATED, session, latency);
					}
				}
			} else {
				throw new UnexpectedPacketException(session, ID_CONNECTED_PONG, pong.getId());
			}
		}
	}

	/**
	 * Sends a <code>ID_CONNECTED_PING</code> to the server and gets data ready
	 * for calculating latency
	 * 
	 * @param session
	 */
	public void sendConnectedPing() {
		ServerSession session = client.getSession();
		if (session != null) {
			ConnectedPing ping = new ConnectedPing();
			ping.pingTime = (System.currentTimeMillis() - client.getClientTimestamp());
			ping.encode();

			session.sendPacket(Reliability.RELIABLE, this.lastPing = ping);
			this.lastPingTime = System.currentTimeMillis();
		}
	}

	@Override
	public void run() {
		ServerSession session = client.getSession();
		if (session != null) {
			session.resetReceivedPacketsThisSecond();
			session.pushLastReceiveTime(this.getWaitTimeMillis());
			if ((double) (client.getServerTimeout() - session.getLastReceiveTime())
					/ client.getServerTimeout() <= 0.5) {
				this.sendConnectedPing();
			}
			if (session.getLastReceiveTime() >= client.getServerTimeout()) {
				client.disconnect("Timeout");
			}
		}
	}

}
