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
package net.marfgamer.raknet.latency;

import net.marfgamer.raknet.event.Hook;
import net.marfgamer.raknet.server.RakNetServer;
import net.marfgamer.raknet.utils.RakNetUtils;

/**
 * Used to make sure the latency feature works properly
 *
 * @author Trent Summerlin
 */
public class RakNetLatencyTest {

	public static void main(String[] args) throws InterruptedException {
		RakNetLatencyFrame frame = new RakNetLatencyFrame();
		RakNetServer server = new RakNetServer(19132, 10,
				"MCPE;A RakNet Latency Test;80;0.15.0;0;10;" + RakNetUtils.getRakNetID());
		server.startThreaded();
		frame.setVisible(true);

		// Client connected
		server.addHook(Hook.SESSION_CONNECTED, (Object[] parameters) -> {
			frame.setClients(server.getClients());
		});

		// Client disconnected
		server.addHook(Hook.SESSION_DISCONNECTED, (Object[] parameters) -> {
			frame.setClients(server.getClients());
		});

		// Latency updated
		server.addHook(Hook.LATENCY_UPDATED, (Object[] parameters) -> {
			frame.setClients(server.getClients());
		});
	}

}
