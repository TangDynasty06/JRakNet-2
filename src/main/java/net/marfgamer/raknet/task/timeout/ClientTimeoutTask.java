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

import java.util.HashMap;

import net.marfgamer.raknet.RakNet;
import net.marfgamer.raknet.exception.UnexpectedPacketException;
import net.marfgamer.raknet.protocol.raknet.ConnectedPing;
import net.marfgamer.raknet.protocol.raknet.ConnectedPong;
import net.marfgamer.raknet.server.RakNetServer;
import net.marfgamer.raknet.server.RakNetServerHandler;
import net.marfgamer.raknet.session.ClientSession;
import net.marfgamer.raknet.task.TaskRunnable;

/**
 * Used by <code>RakNetServer</code> to make sure clients do not timeout
 *
 * @author Trent Summerlin
 */
public class ClientTimeoutTask implements TaskRunnable, RakNet {

	private final RakNetServer server;
	private final RakNetServerHandler handler;
	private final HashMap<ClientSession, ConnectedPing> latencyTimes;

	public ClientTimeoutTask(RakNetServer server, RakNetServerHandler handler) {
		this.server = server;
		this.handler = handler;
		this.latencyTimes = new HashMap<ClientSession, ConnectedPing>();
	}

	/**
	 * Handles a <code>ID_CONNECTED_PONG</code> packet and sets the client's
	 * latency data if the packet data is correct
	 * 
	 * @param session
	 * @param pong
	 */

	public void handleConnectedPong(ClientSession session, ConnectedPong pong) throws UnexpectedPacketException {
		if (pong.getId() == ID_CONNECTED_PONG) {
			if (latencyTimes.containsKey(session)) {
				long pingTime = latencyTimes.get(session).pingTime;
				System.out.println(pong.pingTime + " - " + pingTime);
				if (pong.pingTime == pingTime) {
					long latency = pong.pingTime - pingTime;
					if (latency >= 0) {
						session.setLatency(latency);
					} else {
						handler.removeSession(session, "Invalid pong");
					}
				} else {
					handler.removeSession(session, "Invalid pong");
				}
			}
		} else {
			throw new UnexpectedPacketException(ID_CONNECTED_PONG, pong.getId());
		}
	}

	/**
	 * Sends a <code>ID_CONNECTED_PING</code> to the client and gets data ready
	 * for calculating latency
	 * 
	 * @param session
	 */
	public void sendConnectedPing(ClientSession session) {
		ConnectedPing ping = new ConnectedPing();
		ping.pingTime = System.currentTimeMillis();
		ping.encode();
		session.sendPacket(RELIABLE, ping);
		latencyTimes.put(session, ping);
	}

	@Override
	public long getWaitTimeMillis() {
		return 100L;
	}

	@Override
	public void run() {
		for (ClientSession session : handler.getSessions()) {
			session.resetReceivedPacketsThisSecond();
			session.pushLastReceiveTime(this.getWaitTimeMillis());
			if ((double) (server.getClientTimeout() - session.getLastReceiveTime())
					/ server.getClientTimeout() <= 0.5) {
				this.sendConnectedPing(session);
			}
			if (session.getLastReceiveTime() >= server.getClientTimeout()) {
				handler.removeSession(session, "Timeout");
			}
		}
	}

}
