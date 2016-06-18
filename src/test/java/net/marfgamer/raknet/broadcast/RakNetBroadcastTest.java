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
package net.marfgamer.raknet.broadcast;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import net.marfgamer.raknet.client.DiscoveredRakNetServer;
import net.marfgamer.raknet.client.RakNetClient;
import net.marfgamer.raknet.event.Hook;

/**
 * Used to discover Minecraft: Pocket Edition servers on the network
 *
 * @author Trent Summerlin
 */
public class RakNetBroadcastTest {

	public static void main(String[] args) throws Exception {
		HashMap<InetSocketAddress, DiscoveredRakNetServer> discoveredServers = new HashMap<InetSocketAddress, DiscoveredRakNetServer>();
		RakNetBroadcastFrame frame = new RakNetBroadcastFrame();
		RakNetClient client = new RakNetClient();
		frame.setVisible(true);

		// Server found on local network
		client.addHook(Hook.SERVER_DISCOVERED, (Object[] parameters) -> {
			DiscoveredRakNetServer server = (DiscoveredRakNetServer) parameters[0];
			InetSocketAddress address = (InetSocketAddress) parameters[1];
			discoveredServers.put(address, server);
			updateFrame(discoveredServers.values(), frame);
		});

		// Server can no longer be found on local network
		client.addHook(Hook.SERVER_UNDISCOVERED, (Object[] parameters) -> {
			InetSocketAddress address = (InetSocketAddress) parameters[1];
			discoveredServers.remove(address);
			updateFrame(discoveredServers.values(), frame);
		});
	}

	private static void updateFrame(Collection<DiscoveredRakNetServer> servers, RakNetBroadcastFrame frame) {
		ArrayList<DiscoveredRakNetServer> serverName = new ArrayList<DiscoveredRakNetServer>();
		for (DiscoveredRakNetServer server : servers) {
			if (server.identifier.startsWith("MCPE;")) {
				serverName.add(server);
			}
		}
		frame.setServers(serverName);
	}

}
