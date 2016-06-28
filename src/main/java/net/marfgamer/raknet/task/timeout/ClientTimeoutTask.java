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

import java.util.concurrent.ConcurrentHashMap;

import net.marfgamer.raknet.event.Hook;
import net.marfgamer.raknet.exception.packet.UnexpectedPacketException;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.protocol.identifier.MessageIdentifiers;
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
public class ClientTimeoutTask implements TaskRunnable, MessageIdentifiers {

	private final RakNetServer server;
	private final RakNetServerHandler handler;
	private final ConcurrentHashMap<ClientSession, PingData> latencyTimes;

	public ClientTimeoutTask(RakNetServer server, RakNetServerHandler handler) {
		this.server = server;
		this.handler = handler;
		this.latencyTimes = new ConcurrentHashMap<ClientSession, PingData>();
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
				PingData ping = latencyTimes.remove(session);
				if (pong.pingTime == ping.ping.pingTime) {
					long latency = System.currentTimeMillis() - ping.pingTime;
					if (latency >= 0) {
						session.setLatency(latency);
						server.executeHook(Hook.LATENCY_UPDATED, session, latency);
					}
				}
			}
		} else {
			throw new UnexpectedPacketException(session, ID_CONNECTED_PONG, pong.getId());
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
		ping.pingTime = (System.currentTimeMillis() - server.getServerTimestamp());
		ping.encode();
		session.sendPacket(Reliability.RELIABLE, ping);
		latencyTimes.put(session, new PingData(System.currentTimeMillis(), ping));
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

	/**
	 * Contains the time this ping was sent and the
	 * <code>ID_CONNECTED_PING</code> packet
	 *
	 * @author Trent Summerlin
	 */
	private static class PingData {

		public final long pingTime;
		public final ConnectedPing ping;

		public PingData(long pingTime, ConnectedPing ping) {
			this.pingTime = pingTime;
			this.ping = ping;
		}

	}

}
