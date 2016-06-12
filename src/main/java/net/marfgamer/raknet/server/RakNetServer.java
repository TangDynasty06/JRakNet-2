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

import java.util.HashMap;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import net.marfgamer.raknet.RakNet;
import net.marfgamer.raknet.RakNetOptions;
import net.marfgamer.raknet.event.Hook;
import net.marfgamer.raknet.event.HookRunnable;
import net.marfgamer.raknet.exception.RakNetException;
import net.marfgamer.raknet.protocol.Packet;
import net.marfgamer.raknet.protocol.raknet.UnconnectedConnectionReplyOne;
import net.marfgamer.raknet.protocol.raknet.UnconnectedConnectionReplyTwo;
import net.marfgamer.raknet.protocol.raknet.UnconnectedConnectionRequestOne;
import net.marfgamer.raknet.protocol.raknet.UnconnectedConnectionRequestTwo;
import net.marfgamer.raknet.protocol.raknet.UnconnectedIncompatibleProtocol;
import net.marfgamer.raknet.protocol.raknet.UnconnectedLegacyPing;
import net.marfgamer.raknet.protocol.raknet.UnconnectedLegacyPong;
import net.marfgamer.raknet.protocol.raknet.UnconnectedPing;
import net.marfgamer.raknet.protocol.raknet.UnconnectedPong;
import net.marfgamer.raknet.scheduler.RakNetScheduler;
import net.marfgamer.raknet.session.ClientSession;
import net.marfgamer.raknet.session.SessionState;
import net.marfgamer.raknet.task.ClientTimeoutTask;
import net.marfgamer.raknet.utils.RakNetUtils;

/**
 * A RakNet server instance, used to handle the main packets and track
 * ClientSession states
 *
 * @author Trent Summerlin
 */
public class RakNetServer implements RakNet {

	private boolean running;

	private final long serverId;
	private final long timestamp;
	private final RakNetOptions options;
	private final RakNetScheduler scheduler;
	private final RakNetServerHandler handler;
	private final HashMap<Hook, HookRunnable> hooks;

	public RakNetServer(RakNetOptions options) {
		this.serverId = RakNetUtils.getRakNetID();
		this.timestamp = System.currentTimeMillis();
		this.options = options;
		this.handler = new RakNetServerHandler(this);
		this.scheduler = new RakNetScheduler();
		this.hooks = new HashMap<Hook, HookRunnable>();
	}

	public RakNetServer(int serverPort, String serverIdentifier) {
		this(new RakNetOptions(serverPort, serverIdentifier));
	}

	public RakNetServer() {
		this(new RakNetOptions());
	}

	/**
	 * Returns the options this instance uses
	 * 
	 * @return RakNetOptions
	 */
	public RakNetOptions getOptions() {
		return this.options;
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
	public long getTimestamp() {
		return this.timestamp;
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
				Object[] parameters = this.executeHook(Hook.SERVER_PING, session.getAddress(),
						options.serverIdentifier);
				pong.identifier = parameters[1].toString();
				pong.encode();

				session.sendRaw(pong);
			}
		} else if (pid == ID_UNCONNECTED_LEGACY_PING) {
			UnconnectedLegacyPing legacyPing = new UnconnectedLegacyPing(packet);
			legacyPing.decode();

			if (legacyPing.magic == true) {
				UnconnectedLegacyPong legacyPong = new UnconnectedLegacyPong();
				legacyPong.pingId = legacyPing.pingId;
				legacyPong.serverId = this.serverId;
				Object[] parameters = this.executeHook(Hook.LEGACY_PING, options.serverIdentifier,
						session.getAddress());
				legacyPong.data = parameters[0].toString();
				legacyPong.encode();

				session.sendRaw(legacyPong);
			}
		} else if (pid == ID_UNCONNECTED_CONNECTION_REQUEST_1) {
			if (session.getState() == SessionState.DISCONNECTED) {
				UnconnectedConnectionRequestOne request = new UnconnectedConnectionRequestOne(packet);
				request.decode();

				if (request.magic == true && request.protocol == NETWORK_PROTOCOL
						&& request.mtuSize <= options.maximumTransferUnit) {
					UnconnectedConnectionReplyOne response = new UnconnectedConnectionReplyOne();
					response.serverId = this.serverId;
					response.mtuSize = (short) (request.mtuSize + 46);
					response.encode();

					session.sendRaw(response);
					session.setState(SessionState.CONNECTING_1);
				} else if (request.protocol != NETWORK_PROTOCOL) {
					UnconnectedIncompatibleProtocol incompatible = new UnconnectedIncompatibleProtocol();
					incompatible.protocol = NETWORK_PROTOCOL;
					incompatible.serverId = this.serverId;
					incompatible.encode();

					session.sendRaw(incompatible);
					handler.removeSession(session, "Incompatible protocol");
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
					response.mtuSize = session.getMTUSize();
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

		// Bind socket and start receiving data
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			Bootstrap bootstrap = new Bootstrap();
			bootstrap.group(group).channel(NioDatagramChannel.class).option(ChannelOption.SO_BROADCAST, true)
					.option(ChannelOption.SO_SNDBUF, options.maximumTransferUnit)
					.option(ChannelOption.SO_RCVBUF, options.maximumTransferUnit).handler(handler);
			bootstrap.bind(options.serverPort);
		} catch (Exception e) {
			group.shutdownGracefully();
			throw new RakNetException(e);
		}

		// Start scheduler
		scheduler.scheduleRepeatingTask(new ClientTimeoutTask(this, handler));
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
