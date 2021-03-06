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
package net.marfgamer.raknet.task;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import net.marfgamer.raknet.client.DiscoverMode;
import net.marfgamer.raknet.client.DiscoveredRakNetServer;
import net.marfgamer.raknet.client.RakNetClient;
import net.marfgamer.raknet.event.Hook;
import net.marfgamer.raknet.exception.packet.UnexpectedPacketException;
import net.marfgamer.raknet.protocol.identifier.MessageIdentifiers;
import net.marfgamer.raknet.protocol.raknet.UnconnectedPing;
import net.marfgamer.raknet.protocol.raknet.UnconnectedPingOpenConnections;
import net.marfgamer.raknet.protocol.raknet.UnconnectedPong;

/**
 * Used by <code>RakNetClient</code> to discover other servers on the network
 *
 * @author Trent Summerlin
 */
public class ServerAdvertiseTask implements TaskRunnable, MessageIdentifiers {

	private static final int CYCLE_START = 5;

	private final RakNetClient client;
	private final ConcurrentHashMap<InetSocketAddress, DiscoveredRakNetServer> servers;

	public ServerAdvertiseTask(RakNetClient client) {
		this.client = client;
		this.servers = new ConcurrentHashMap<InetSocketAddress, DiscoveredRakNetServer>();
	}

	/**
	 * Handles a pong packet and sets the server identifier by address
	 * 
	 * @param packet
	 * @param sender
	 * @throws UnexpectedPacketException
	 */
	public void handleUnconnectedPong(UnconnectedPong pong, InetSocketAddress sender) throws UnexpectedPacketException {
		if (pong.getId() == ID_UNCONNECTED_PONG) {
			servers.put(sender, new DiscoveredRakNetServer(sender, pong.serverId, pong.identifier));
			servers.get(sender).cyclesLeft = CYCLE_START;
			client.executeHook(Hook.SERVER_DISCOVERED, servers.get(sender), sender);
		} else {
			throw new UnexpectedPacketException(null, ID_UNCONNECTED_PING, pong.getId());
		}
	}

	/**
	 * Returns all the discovered servers that have been found on the network
	 * 
	 * @return DiscoveredRakNetServer[]
	 */
	public DiscoveredRakNetServer[] getDiscoveredServers() {
		return servers.values().toArray(new DiscoveredRakNetServer[servers.size()]);
	}

	@Override
	public long getWaitTimeMillis() {
		return 1000L;
	}

	@Override
	public void run() {
		// Broadcast ping to network
		UnconnectedPing ping = (client.getDiscoverMode() == DiscoverMode.ALL_CONNECTIONS ? new UnconnectedPing()
				: new UnconnectedPingOpenConnections());
		ping.pingId = System.currentTimeMillis();
		ping.clientId = client.getClientId();
		ping.encode();
		client.broadcastRaw(ping);

		// Make sure servers haven't timed-out
		Iterator<DiscoveredRakNetServer> iServers = servers.values().iterator();
		while (iServers.hasNext()) {
			DiscoveredRakNetServer server = iServers.next();
			if (server.cyclesLeft <= 0) {
				iServers.remove();
				client.executeHook(Hook.SERVER_UNDISCOVERED, server, server.address);
			} else {
				server.cyclesLeft--;
			}
		}
	}

}
