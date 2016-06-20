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
package net.marfgamer.raknet.server;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import net.marfgamer.raknet.RakNet;
import net.marfgamer.raknet.event.Hook;
import net.marfgamer.raknet.event.HookRunnable;
import net.marfgamer.raknet.exception.MaximumTransferUnitException;
import net.marfgamer.raknet.exception.RakNetException;
import net.marfgamer.raknet.exception.UnexpectedPacketException;
import net.marfgamer.raknet.protocol.Packet;
import net.marfgamer.raknet.protocol.raknet.ConnectedPong;
import net.marfgamer.raknet.protocol.raknet.UnconnectedConnectionReplyOne;
import net.marfgamer.raknet.protocol.raknet.UnconnectedConnectionReplyTwo;
import net.marfgamer.raknet.protocol.raknet.UnconnectedConnectionRequestOne;
import net.marfgamer.raknet.protocol.raknet.UnconnectedConnectionRequestTwo;
import net.marfgamer.raknet.protocol.raknet.UnconnectedIncompatibleProtocol;
import net.marfgamer.raknet.protocol.raknet.UnconnectedLegacyPing;
import net.marfgamer.raknet.protocol.raknet.UnconnectedLegacyPong;
import net.marfgamer.raknet.protocol.raknet.UnconnectedPing;
import net.marfgamer.raknet.protocol.raknet.UnconnectedPong;
import net.marfgamer.raknet.protocol.raknet.UnconnectedServerFull;
import net.marfgamer.raknet.scheduler.RakNetScheduler;
import net.marfgamer.raknet.session.ClientSession;
import net.marfgamer.raknet.session.SessionState;
import net.marfgamer.raknet.task.ClientUnblockTask;
import net.marfgamer.raknet.task.reliability.ClientReliabilityTask;
import net.marfgamer.raknet.task.timeout.ClientTimeoutTask;
import net.marfgamer.raknet.utils.RakNetUtils;

/**
 * A RakNet server instance, used to handle the main packets and track
 * ClientSession states
 *
 * @author Trent Summerlin
 */
public class RakNetServer implements RakNet {

	// Server options
	private final int port;
	private final int maxConnections;
	private final String identifier;
	private final int maxTransferUnit;
	private final long clientTimeout;

	// Server info
	private boolean running;
	private final long serverId;
	private final long serverTimestamp;
	private ClientTimeoutTask timeout;
	private final RakNetScheduler scheduler;
	private final RakNetServerHandler handler;
	private final HashMap<Hook, HookRunnable> hooks;

	public RakNetServer(int port, int maxConnections, String identifier, int maxTransferUnit, long clientTimeout) {
		// Set server options
		this.port = port;
		this.maxConnections = maxConnections;
		this.identifier = identifier;
		this.maxTransferUnit = maxTransferUnit;
		this.clientTimeout = clientTimeout;

		// Generate server info
		this.serverId = RakNetUtils.getRakNetID();
		this.serverTimestamp = System.currentTimeMillis();
		this.handler = new RakNetServerHandler(this);
		this.scheduler = new RakNetScheduler();
		this.hooks = new HashMap<Hook, HookRunnable>();
	}

	public RakNetServer(int port, int maxConnections, String identifier) {
		this(port, maxConnections, identifier, RakNetUtils.getNetworkInterfaceMTU(), CLIENT_TIMEOUT);
	}

	public RakNetServer(int port, int maxConnections) {
		this(port, maxConnections, null);
	}

	/**
	 * Returns the port which the server runs on
	 * 
	 * @return int
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * Returns the amount of clients that can be connected or be connecting to a
	 * server at once
	 * 
	 * @return int
	 */
	public int getMaxConnections() {
		return this.maxConnections;
	}

	/**
	 * Returns the server's identifier
	 * 
	 * @return String
	 */
	public String getIdentifier() {
		return this.identifier;
	}

	/**
	 * Returns how many bytes can be sent or received before the packet must be
	 * split
	 * 
	 * @return int
	 */
	public int getMaxTransferUnit() {
		return this.maxTransferUnit;
	}

	/**
	 * Returns how long before a client is disconnected because of a timeout in
	 * milliseconds
	 * 
	 * @return long
	 */
	public long getClientTimeout() {
		return this.clientTimeout;
	}

	/**
	 * Returns the ServerID which the server uses
	 * 
	 * @return long
	 */
	public long getServerId() {
		return this.serverId;
	}

	/**
	 * Returns the server timestamp
	 * 
	 * @return long
	 */
	public long getServerTimestamp() {
		return this.serverTimestamp;
	}

	/**
	 * Updates the client latency based on the pong packet
	 * 
	 * @param session
	 * @param pong
	 * @throws UnexpectedPacketException
	 */
	public void updateClientLatency(ClientSession session, ConnectedPong pong) {
		if (timeout != null) {
			try {
				timeout.handleConnectedPong(session, pong);
			} catch (UnexpectedPacketException e) {
				handler.removeSession(session, "Invalid pong packet!");
			}
		}
	}

	/**
	 * Sends an <code>ID_UNCONNECTED_PING</code> to the client to check it's
	 * latency
	 * 
	 * @param session
	 */
	public void checkClientLatency(ClientSession session) {
		if (timeout != null) {
			timeout.sendConnectedPing(session);
		}
	}

	/**
	 * Sets the HookRunnable for the specified Hook
	 * 
	 * @param hook
	 * @param runnable
	 */
	public void addHook(Hook hook, HookRunnable runnable) {
		hooks.put(hook, runnable);
	}

	/**
	 * Removes the current HookRunnable for the specified Hook
	 * 
	 * @param hook
	 */
	public void removeHook(Hook hook) {
		hooks.remove(hook);
	}

	/**
	 * Executes a Hook event with the specified parameters, the returned
	 * Object[] is what has been passed in and possibly modified by the
	 * HookRunnable
	 * 
	 * @param hook
	 * @param parameters
	 * @return Object[]
	 */
	public Object[] executeHook(Hook hook, Object... parameters) {
		if (hooks.containsKey(hook)) {
			hooks.get(hook).run(parameters);
		}
		return parameters;
	}

	/**
	 * Blocks the specified address with the specified reason and the specified
	 * time in milliseconds
	 * 
	 * @param address
	 * @param reason
	 * @param time
	 */
	public void blockAddress(InetAddress address, long time) {
		handler.blockAddress(address, time);
	}

	/**
	 * Unblocks the specified address
	 * 
	 * @param address
	 */
	public void unblockAddress(InetAddress address) {
		handler.unblockAddress(address);
	}

	/**
	 * Unblocks the specified blocked address
	 * 
	 * @param address
	 */
	public void unblockAddress(BlockedAddress address) {
		handler.unblockAddress(address);
	}

	/**
	 * Kicks the specified client with the specified reason
	 * 
	 * @param session
	 * @param reason
	 */
	public void kickClient(ClientSession session, String reason) {
		handler.removeSession(session, reason);
	}

	/**
	 * Kicks the specified client
	 * 
	 * @param session
	 */
	public void kickClient(ClientSession session) {
		this.kickClient(session, "Kicked from server");
	}

	/**
	 * Returns how much time is left for an address block
	 * 
	 * @param address
	 * @return long
	 */
	public long getBlockTime(InetAddress address) {
		BlockedAddress blocked = handler.getBlockedAddress(address);
		if (blocked != null) {
			return blocked.time;
		}
		return 0;
	}

	/**
	 * Returns all the blocked clients
	 * 
	 * @return BlockedAddress[]
	 */
	public BlockedAddress[] getBlockedAddresses() {
		return handler.getBlockedAddresses();
	}

	/**
	 * Returns a blocked address depending by its address
	 * 
	 * @param address
	 * @return BlockAddress
	 */
	public BlockedAddress getBlockedAddress(InetAddress address) {
		return handler.getBlockedAddress(address);
	}

	/**
	 * Returns all the client's that are connected to the server
	 * 
	 * @return ClientSession[]
	 */
	public ClientSession[] getClients() {
		ArrayList<ClientSession> connected = new ArrayList<ClientSession>();
		for (ClientSession session : handler.getSessions()) {
			if (session.getState() == SessionState.CONNECTED) {
				connected.add(session);
			}
		}
		return connected.toArray(new ClientSession[connected.size()]);
	}

	/**
	 * Returns all the client's that are connected or are connecting to the
	 * server at the current time
	 * 
	 * @return int
	 */
	public int getConnectionCount() {
		int connections = 0;
		for (ClientSession session : handler.getSessions()) {
			if (session.getState().getOrder() >= SessionState.CONNECTING_1.getOrder()) {
				connections++;
			}
		}
		return connections;
	}

	/**
	 * Handles a raw packet
	 * 
	 * @param pid
	 * @param packet
	 * @param session
	 */
	protected void handleRaw(Packet packet, ClientSession session) {
		short pid = packet.getId();
		if (pid == ID_UNCONNECTED_PING) {
			UnconnectedPing ping = new UnconnectedPing(packet);
			ping.decode();

			if (ping.magic == true) {
				UnconnectedPong pong = new UnconnectedPong();
				pong.pingId = ping.pingId;
				pong.serverId = this.serverId;
				Object[] parameters = this.executeHook(Hook.SERVER_PING, session.getAddress(), this.identifier);
				pong.identifier = parameters[1].toString();
				pong.encode();

				// Make sure identifier is not null before encoding
				if (parameters[1] != null) {
					session.sendRaw(pong);
				}
			}
		} else if (pid == ID_UNCONNECTED_LEGACY_PING) {
			UnconnectedLegacyPing legacyPing = new UnconnectedLegacyPing(packet);
			legacyPing.decode();

			if (legacyPing.magic == true) {
				UnconnectedLegacyPong legacyPong = new UnconnectedLegacyPong();
				legacyPong.pingId = legacyPing.pingId;
				legacyPong.serverId = this.serverId;
				Object[] parameters = this.executeHook(Hook.SERVER_LEGACY_PING, this.identifier, session.getAddress());
				legacyPong.data = parameters[0].toString();
				legacyPong.encode();

				// Make sure identifier is not null before encoding
				if (parameters[1] != null) {
					session.sendRaw(legacyPong);
				}
			}
		} else if (pid == ID_UNCONNECTED_CONNECTION_REQUEST_1) {
			if (session.getState() == SessionState.DISCONNECTED) {
				UnconnectedConnectionRequestOne request = new UnconnectedConnectionRequestOne(packet);
				request.decode();

				if (request.magic == true && request.protocol == SERVER_NETWORK_PROTOCOL
						&& request.mtuSize <= this.maxTransferUnit) {
					if (this.getConnectionCount() >= this.maxConnections) {
						session.sendRaw(new UnconnectedServerFull());
						handler.removeSession(session, "Server is full");
					} else {
						UnconnectedConnectionReplyOne response = new UnconnectedConnectionReplyOne();
						response.serverId = this.serverId;
						response.mtuSize = (short) (request.mtuSize + 46);
						response.encode();

						session.sendRaw(response);
						session.setState(SessionState.CONNECTING_1);
					}
				} else if (request.protocol != SERVER_NETWORK_PROTOCOL) {
					UnconnectedIncompatibleProtocol incompatible = new UnconnectedIncompatibleProtocol();
					incompatible.protocol = SERVER_NETWORK_PROTOCOL;
					incompatible.serverId = this.serverId;
					incompatible.encode();

					session.sendRaw(incompatible);
					handler.removeSession(session, "Incorrect protocol");
				}
			}
		} else if (pid == ID_UNCONNECTED_CONNECTION_REQUEST_2) {
			if (session.getState() == SessionState.CONNECTING_1) {
				UnconnectedConnectionRequestTwo request = new UnconnectedConnectionRequestTwo(packet);
				request.decode();

				if (request.magic == true) {
					UnconnectedConnectionReplyTwo response = new UnconnectedConnectionReplyTwo();
					response.serverId = this.serverId;
					response.clientAddress = session.getSocketAddress();
					response.mtuSize = session.getMaximumTransferUnit();
					response.encode();

					session.sendRaw(response);
					session.setMTUSize(request.mtuSize);
					session.setSessionId(request.clientId);
					session.setState(SessionState.CONNECTING_2);
				}
			}
		}
	}

	/**
	 * Starts the server
	 */
	public void start() throws RakNetException {
		if (running == true) {
			throw new RakNetException("Server is already running!");
		}

		// Check options
		if (this.maxTransferUnit < MINIMUM_TRANSFER_UNIT) {
			throw new MaximumTransferUnitException(this.maxTransferUnit);
		}

		// Bind socket and start receiving data
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			Bootstrap bootstrap = new Bootstrap();
			bootstrap.group(group).channel(NioDatagramChannel.class).option(ChannelOption.SO_BROADCAST, true)
					.option(ChannelOption.SO_REUSEADDR, false).option(ChannelOption.SO_SNDBUF, this.maxTransferUnit)
					.option(ChannelOption.SO_RCVBUF, this.maxTransferUnit).handler(handler);
			bootstrap.bind(this.port);
		} catch (Exception e) {
			group.shutdownGracefully();
			throw new RakNetException(e);
		}

		// Start scheduler
		scheduler.scheduleRepeatingTask(this.timeout = new ClientTimeoutTask(this, handler));
		scheduler.scheduleRepeatingTask(new ClientUnblockTask(this.handler));
		scheduler.scheduleRepeatingTask(new ClientReliabilityTask(this.handler));
		scheduler.start();
		this.running = true;
	}

	/**
	 * Starts a server on it's own thread
	 * 
	 * @return Thread
	 */
	public Thread startThreaded() {
		RakNetServerThread thread = new RakNetServerThread(this);
		thread.start();
		return thread;
	}

}
