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
package net.marfgamer.raknet.task.reliability;

import net.marfgamer.raknet.RakNet;
import net.marfgamer.raknet.client.RakNetClient;
import net.marfgamer.raknet.protocol.raknet.internal.CustomPacket;
import net.marfgamer.raknet.session.ServerSession;
import net.marfgamer.raknet.task.TaskRunnable;

/**
 * Used to make sure all lost packets are sent back to the server. If too many
 * packets have not been acknowledged by the server the client will disconnect.
 *
 * @author Trent Summerlin
 */
public class ServerReliabilityTask implements TaskRunnable, RakNet {

	private final RakNetClient client;

	public ServerReliabilityTask(RakNetClient client) {
		this.client = client;
	}

	@Override
	public long getWaitTimeMillis() {
		return 1500L;
	}

	@Override
	public void run() {
		ServerSession session = client.getSession();
		if (session != null) {
			CustomPacket[] reliablePackets = session.getReliableQueue();
			CustomPacket[] recoveryPackets = session.getRecoveryQueue();

			// Remove unreliable packets from queue if needed
			if (recoveryPackets.length > MAX_PACKETS_PER_QUEUE) {
				session.cleanRecoveryQueue();
			}

			// Make sure the server is not trying to do a back-off attack
			if (reliablePackets.length > MAX_PACKETS_PER_QUEUE || recoveryPackets.length > MAX_PACKETS_PER_QUEUE) {
				client.disconnect("Too many packets in queue!");
			} else {
				// Resend all lost packets
				for (CustomPacket packet : reliablePackets) {
					session.sendRaw(packet);
				}
			}
		}
	}

}
