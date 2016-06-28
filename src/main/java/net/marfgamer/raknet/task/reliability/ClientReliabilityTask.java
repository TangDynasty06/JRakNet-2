/*
c *       _   _____            _      _   _          _   
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
package net.marfgamer.raknet.task.reliability;

import net.marfgamer.raknet.RakNet;
import net.marfgamer.raknet.protocol.raknet.internal.CustomPacket;
import net.marfgamer.raknet.server.RakNetServerHandler;
import net.marfgamer.raknet.session.ClientSession;
import net.marfgamer.raknet.task.TaskRunnable;

/**
 * Used to make sure all packets lost are resent to the receivers. If too many
 * packets have not been acknowledged by the client it is kicked and its address
 * is blocked for ten minutes.
 * 
 * @author Trent Summerlin
 */
public class ClientReliabilityTask implements TaskRunnable, RakNet {

	private final RakNetServerHandler handler;

	public ClientReliabilityTask(RakNetServerHandler handler) {
		this.handler = handler;
	}

	@Override
	public long getWaitTimeMillis() {
		return 1500L;
	}

	@Override
	public void run() {
		for (ClientSession session : handler.getSessions()) {
			CustomPacket[] reliablePackets = session.getReliableQueue();
			CustomPacket[] recoveryPackets = session.getRecoveryQueue();
			
			// Remove unreliable packets from queue if needed
			if(recoveryPackets.length > MAX_PACKETS_PER_QUEUE) {
				session.cleanRecoveryQueue();
			}
			
			// Make sure client is not trying to do a back-off attack
			if (reliablePackets.length > MAX_PACKETS_PER_QUEUE || recoveryPackets.length > MAX_PACKETS_PER_QUEUE) {
				handler.removeSession(session, "Too many packets in queue!");
				handler.blockAddress(session.getAddress(), FIVE_MINUTES_MILLIS);
			} else {
				// Resend all lost packets
				for (CustomPacket packet : reliablePackets) {
					session.sendRaw(packet);
				}
			}
		}
	}

}
