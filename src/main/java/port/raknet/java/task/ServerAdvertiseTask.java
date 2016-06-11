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

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;

import port.raknet.java.RakNet;
import port.raknet.java.client.DiscoveredRakNetServer;
import port.raknet.java.client.RakNetClient;
import port.raknet.java.exception.UnexpectedPacketException;
import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.raknet.UnconnectedPing;
import port.raknet.java.protocol.raknet.UnconnectedPong;

/**
 * Used by <code>RakNetClient</code> to discover other servers on the network
 *
 * @author Trent Summerlin
 */
public class ServerAdvertiseTask implements TaskRunnable, RakNet {

	private static final int CYCLE_START = 5;

	private final RakNetClient client;
	private final HashMap<InetSocketAddress, DiscoveredRakNetServer> servers;

	public ServerAdvertiseTask(RakNetClient client) {
		this.client = client;
		this.servers = new HashMap<InetSocketAddress, DiscoveredRakNetServer>();
	}

	public void handlePong(Packet packet, InetSocketAddress sender) throws UnexpectedPacketException {
		if (packet.getId() == ID_UNCONNECTED_PONG) {
			UnconnectedPong pong = new UnconnectedPong(packet);
			pong.decode();

			if (!servers.containsKey(sender)) {
				servers.put(sender, new DiscoveredRakNetServer(sender, pong.serverId, pong.identifier));
			}
			servers.get(sender).cyclesLeft = CYCLE_START;
		} else {
			throw new UnexpectedPacketException(ID_UNCONNECTED_PING, packet.getId());
		}
	}

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
		UnconnectedPing ping = new UnconnectedPing();
		ping.pingId = client.getClientId();
		ping.encode();
		client.broadcastRaw(ping);

		// Make sure servers haven't timed-out
		Iterator<DiscoveredRakNetServer> iServers = servers.values().iterator();
		while (iServers.hasNext()) {
			DiscoveredRakNetServer server = iServers.next();
			if (server.cyclesLeft <= 0) {
				iServers.remove();
			} else {
				server.cyclesLeft--;
			}
		}
	}

}
