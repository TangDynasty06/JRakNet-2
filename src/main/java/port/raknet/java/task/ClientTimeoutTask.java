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
package port.raknet.java.task;

import port.raknet.java.RakNetOptions;
import port.raknet.java.protocol.Reliability;
import port.raknet.java.protocol.raknet.ConnectedPing;
import port.raknet.java.server.RakNetServer;
import port.raknet.java.server.RakNetServerHandler;
import port.raknet.java.session.ClientSession;

/**
 * Used by <code>RakNetServer</code> to make sure clients do not timeout
 *
 * @author Trent Summerlin
 */
public class ClientTimeoutTask implements Runnable {

	public static final long TICK = 1000L;

	private final RakNetServer server;
	private final RakNetServerHandler handler;
	private long pingId;

	public ClientTimeoutTask(RakNetServer server, RakNetServerHandler handler) {
		this.server = server;
		this.handler = handler;
	}

	@Override
	public void run() {
		RakNetOptions options = server.getOptions();
		for (ClientSession session : handler.getSessions()) {
			session.pushLastReceiveTime(TICK);
			if ((double) (options.timeout - session.getLastReceiveTime()) / options.timeout <= 0.5) {
				// Ping ID's do not need to match
				ConnectedPing ping = new ConnectedPing();
				ping.pingId = pingId++;
				ping.encode();
				session.sendPacket(Reliability.RELIABLE, ping);
			}
			if (session.getLastReceiveTime() >= options.timeout) {
				handler.removeSession(session, "Timeout");
			}
		}
	}

}
