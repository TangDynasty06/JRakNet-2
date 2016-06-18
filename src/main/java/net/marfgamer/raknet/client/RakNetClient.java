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
package net.marfgamer.raknet.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import net.marfgamer.raknet.RakNet;
import net.marfgamer.raknet.RakNetOptions;
import net.marfgamer.raknet.event.Hook;
import net.marfgamer.raknet.event.HookRunnable;
import net.marfgamer.raknet.exception.MaximumTransferUnitException;
import net.marfgamer.raknet.exception.PacketOverloadException;
import net.marfgamer.raknet.exception.RakNetException;
import net.marfgamer.raknet.exception.UnexpectedPacketException;
import net.marfgamer.raknet.exception.client.ConnectionBannedException;
import net.marfgamer.raknet.exception.client.IncompatibleProtocolException;
import net.marfgamer.raknet.exception.client.ServerFullException;
import net.marfgamer.raknet.protocol.Packet;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.protocol.raknet.ConnectedCloseConnection;
import net.marfgamer.raknet.protocol.raknet.ConnectedConnectRequest;
import net.marfgamer.raknet.protocol.raknet.UnconnectedConnectionBanned;
import net.marfgamer.raknet.protocol.raknet.UnconnectedConnectionReplyOne;
import net.marfgamer.raknet.protocol.raknet.UnconnectedConnectionReplyTwo;
import net.marfgamer.raknet.protocol.raknet.UnconnectedConnectionRequestOne;
import net.marfgamer.raknet.protocol.raknet.UnconnectedConnectionRequestTwo;
import net.marfgamer.raknet.protocol.raknet.UnconnectedIncompatibleProtocol;
import net.marfgamer.raknet.protocol.raknet.UnconnectedPong;
import net.marfgamer.raknet.protocol.raknet.UnconnectedServerFull;
import net.marfgamer.raknet.protocol.raknet.internal.Acknowledge;
import net.marfgamer.raknet.protocol.raknet.internal.CustomPacket;
import net.marfgamer.raknet.scheduler.RakNetScheduler;
import net.marfgamer.raknet.session.ServerSession;
import net.marfgamer.raknet.session.SessionState;
import net.marfgamer.raknet.task.ServerAdvertiseTask;
import net.marfgamer.raknet.task.ServerReliabilityTask;
import net.marfgamer.raknet.task.ServerTimeoutTask;
import net.marfgamer.raknet.utils.RakNetUtils;

/**
 * Used to connect and send data to <code>RakNetServers</code> with ease
 *
 * @author Trent Summerlin
 */
public class RakNetClient implements RakNet {

	// Client data
	private boolean running;
	private final long clientId;
	private final long timestamp;
	private final RakNetOptions options;
	private final RakNetScheduler scheduler;
	private final ServerAdvertiseTask advertise;
	private final HashMap<Hook, HookRunnable> hooks;

	// Netty data
	private final RakNetClientHandler handler;
	private final EventLoopGroup group;
	private final Bootstrap bootstrap;
	private volatile Channel channel;

	// Session data
	private volatile ServerSession session;
	private volatile SessionState state = SessionState.DISCONNECTED;
	private volatile ArrayList<RakNetException> connectionErrors = new ArrayList<RakNetException>();

	public RakNetClient(RakNetOptions options) throws RakNetException {
		this.clientId = RakNetUtils.getRakNetID();
		this.timestamp = System.currentTimeMillis();
		this.options = options;
		this.scheduler = new RakNetScheduler();
		this.advertise = new ServerAdvertiseTask(this);
		this.hooks = new HashMap<Hook, HookRunnable>();

		// Check options
		if (options.maximumTransferUnit < MINIMUM_TRANSFER_UNIT) {
			throw new MaximumTransferUnitException(options.maximumTransferUnit);
		}

		// Setup netty channel
		this.handler = new RakNetClientHandler(this);
		this.group = new NioEventLoopGroup();
		this.bootstrap = new Bootstrap();
		bootstrap.group(group);
		bootstrap.channel(NioDatagramChannel.class);
		bootstrap.handler(handler);

		// Start scheduler here so we can discover
		this.bindChannel();
		scheduler.scheduleRepeatingTask(advertise);
		scheduler.start();
	}

	public RakNetClient(int broadcastPort) throws RakNetException {
		this(new RakNetOptions(broadcastPort));
	}

	public RakNetClient() throws RakNetException {
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
	 * Returns the ClientID which this instance uses
	 * 
	 * @return
	 */
	public long getClientId() {
		return this.clientId;
	}

	/**
	 * Returns the time the client started
	 * 
	 * @return long
	 */
	public long getTimestamp() {
		return this.timestamp;
	}

	/**
	 * Returns the session the client is connected to (or is connecting to)
	 * 
	 * @return ServerSession
	 */
	public ServerSession getSession() {
		return this.session;
	}

	/**
	 * Returns the client's current local address
	 * 
	 * @return InetSocketAddress
	 */
	public InetSocketAddress getLocalAddress() {
		try {
			int port = ((InetSocketAddress) channel.localAddress()).getPort();
			return new InetSocketAddress(InetAddress.getLocalHost(), port);
		} catch (UnknownHostException e) {
			return null;
		}
	}

	/**
	 * Returns the current state the client is in
	 * 
	 * @return SessionState
	 */
	public SessionState getState() {
		return this.state;
	}

	/**
	 * Sets the current state the client is in
	 * 
	 * @param state
	 */
	public void setState(SessionState state) {
		this.state = state;
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
	 * @param type
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
	 * Updates the <code>packetsThisSecond</code> for the current session if
	 * there is one
	 * 
	 * @throws PacketOverloadException
	 */
	protected void pushPacketsThisSecond() throws PacketOverloadException {
		if (session != null) {
			session.pushReceivedPacketsThisSecond();
			if (session.getReceivedPacketsThisSecond() > MAX_PACKETS_PER_SECOND) {
				throw new PacketOverloadException(session);
			}
		}
	}

	/**
	 * Handles a raw packet
	 * 
	 * @param packet
	 * @param sender
	 */
	protected void handleRaw(Packet packet, InetSocketAddress sender) throws RakNetException {
		short pid = packet.getId();
		if (pid == ID_UNCONNECTED_CONNECTION_REPLY_1) {
			if (state == SessionState.CONNECTING_1) {
				UnconnectedConnectionReplyOne ucro = new UnconnectedConnectionReplyOne(packet);
				ucro.decode();

				if (ucro.magic == true && session.isServer(sender)) {
					session.setSessionId(ucro.serverId);
					session.setMTUSize(ucro.mtuSize);

					UnconnectedConnectionRequestTwo ucrt = new UnconnectedConnectionRequestTwo();
					ucrt.clientId = this.clientId;
					ucrt.clientAddress = sender;
					ucrt.mtuSize = (short) options.maximumTransferUnit;
					ucrt.encode();

					session.sendRaw(ucrt);
					this.setState(SessionState.CONNECTING_2);
				}
			}
		} else if (pid == ID_UNCONNECTED_CONNECTION_REPLY_2) {
			if (state == SessionState.CONNECTING_2) {
				UnconnectedConnectionReplyTwo ucrt = new UnconnectedConnectionReplyTwo(packet);
				ucrt.decode();

				if (ucrt.magic == true && session.isServer(sender)) {
					ConnectedConnectRequest ccr = new ConnectedConnectRequest();
					ccr.clientId = this.clientId;
					ccr.timestamp = this.timestamp;
					ccr.encode();

					session.sendPacket(Reliability.RELIABLE, ccr);
					this.setState(SessionState.HANDSHAKING);
				}
			}
		} else if (pid == ID_UNCONNECTED_SERVER_FULL) {
			if (state != SessionState.CONNECTED && session.isServer(sender)) {
				UnconnectedServerFull full = new UnconnectedServerFull(packet);
				full.decode();
				connectionErrors.add(new ServerFullException(this, session));
			}
		} else if (pid == ID_UNCONNECTED_CONNECTION_BANNED) {
			if (state != SessionState.CONNECTED && session.isServer(sender)) {
				UnconnectedConnectionBanned banned = new UnconnectedConnectionBanned(packet);
				banned.decode();
				connectionErrors.add(new ConnectionBannedException(this, session));
			}
		} else if (pid == ID_UNCONNECTED_INCOMPATIBLE_PROTOCOL) {
			if (sender.equals(session.getSocketAddress())) {
				if (state != SessionState.CONNECTED && session.isServer(sender)) {
					UnconnectedIncompatibleProtocol incompatible = new UnconnectedIncompatibleProtocol(packet);
					incompatible.decode();
					connectionErrors.add(
							new IncompatibleProtocolException(this, incompatible.protocol, CLIENT_NETWORK_PROTOCOL));
				}
			}
		} else if (pid == ID_UNCONNECTED_PONG) {
			UnconnectedPong pong = new UnconnectedPong(packet);
			pong.decode();
			advertise.handlePong(pong, sender);
		}
	}

	/**
	 * Returns all the discovered servers from the
	 * <code>ServerAdvertiseTask</code>
	 * 
	 * @return DiscoveredRakNetServer[]
	 */
	public synchronized DiscoveredRakNetServer[] getDiscoveredServers() {
		return advertise.getDiscoveredServers();
	}

	/**
	 * Handles a CustomPacket
	 * 
	 * @param custom
	 * @param sender
	 */
	protected void handleCustom(CustomPacket custom, InetSocketAddress sender) throws RakNetException {
		if (session != null) {
			if (session.isServer(sender)) {
				session.handleCustom0(custom);
			}
		}
	}

	/**
	 * Handles an ACK packet
	 * 
	 * @param ack
	 * @param sender
	 */
	protected synchronized void handleAck(Acknowledge ack, InetSocketAddress sender) throws UnexpectedPacketException {
		if (session != null) {
			if (session.isServer(sender)) {
				session.handleAck(ack);
			}
		}
	}

	/**
	 * Handles a NACK packet
	 * 
	 * @param nack
	 * @param sender
	 */
	protected synchronized void handleNack(Acknowledge nack, InetSocketAddress sender)
			throws UnexpectedPacketException {
		if (session != null) {
			if (session.isServer(sender)) {
				session.handleNack(nack);
			}
		}
	}

	/**
	 * Broadcasts a packet to the entire local network, returns
	 * <code>true</code> if the packet was able to send successfully
	 * 
	 * @param packet
	 * @return boolean
	 */
	public boolean broadcastRaw(Packet packet) {
		if (channel != null) {
			if (channel.isOpen()) {
				channel.writeAndFlush(new DatagramPacket(packet.buffer(),
						new InetSocketAddress(RakNetUtils.getSubnetMask(), options.clientBroadcastPort)));
				return true;
			}
		}
		return false;
	}

	/**
	 * Binds the channel on a new port
	 */
	private void bindChannel() {
		try {
			this.unbindChannel();
			bootstrap.option(ChannelOption.SO_RCVBUF, options.maximumTransferUnit);
			bootstrap.option(ChannelOption.SO_SNDBUF, options.maximumTransferUnit);
			this.channel = bootstrap.bind(0).sync().channel();
			handler.setChannel(channel);
		} catch (Exception e) {
			e.printStackTrace();
			group.shutdownGracefully();
		}
	}

	/**
	 * Unbinds the channel and closes it
	 */
	private void unbindChannel() {
		if (channel != null) {
			channel.close();
			handler.setChannel(null);
		}
	}

	/**
	 * Connects to a server with the specified address
	 * 
	 * @param address
	 * @throws RaknetException
	 * @throws InterruptedException
	 */
	public void connect(InetSocketAddress address) throws RakNetException, InterruptedException {
		// Disconnect from current session
		this.disconnect();

		handler.resetHandler();
		this.bindChannel();
		int mtu = options.maximumTransferUnit;

		// Do not bind to "localhost", it locks up the handler
		this.session = new ServerSession(channel, address, this);
		this.setState(SessionState.CONNECTING_1);

		// Request connection until a condition fails to meet
		while (!handler.foundMtu && session != null && connectionErrors.isEmpty()) {
			if (mtu < MINIMUM_TRANSFER_UNIT) {
				throw new MaximumTransferUnitException(mtu);
			}

			UnconnectedConnectionRequestOne request = new UnconnectedConnectionRequestOne();
			request.mtuSize = (short) mtu;
			request.protocol = CLIENT_NETWORK_PROTOCOL;
			request.encode();

			bootstrap.option(ChannelOption.SO_SNDBUF, (int) request.mtuSize);
			session.sendRaw(request);

			mtu -= 100L;
			Thread.sleep(500L);
		}

		// Throw all caught exceptions
		for (RakNetException exception : connectionErrors) {
			group.shutdownGracefully();
			this.disconnect(exception);
			this.unbindChannel();
			throw exception;
		}

		// Start scheduler
		if (running == false) {
			scheduler.scheduleRepeatingTask(new ServerTimeoutTask(this));
			scheduler.scheduleRepeatingTask(new ServerReliabilityTask(this));
			this.running = true;
		}
	}

	/**
	 * Connects to a server with the specified address
	 * 
	 * @param address
	 * @param port
	 * @throws RakNetException
	 * @throws InterruptedException
	 */
	public void connect(String address, int port) throws RakNetException, InterruptedException {
		this.connect(new InetSocketAddress(address, port));
	}

	/**
	 * Connects to a server with the specified discovered server
	 * 
	 * @param server
	 * @throws RakNetException
	 * @throws InterruptedException
	 */
	public void connect(DiscoveredRakNetServer server) throws RakNetException, InterruptedException {
		this.connect(server.address);
	}

	/**
	 * Connects to a server with the specified address on it's own thread
	 * 
	 * @param address
	 * @return Thread
	 */
	public Thread connectThreaded(InetSocketAddress address) {
		RakNetClientThread thread = new RakNetClientThread(this, address);
		thread.start();
		return thread;
	}

	/**
	 * Connects to a server with the specified discovered server on it's own
	 * thread
	 * 
	 * @param server
	 * @return Thread
	 */
	public Thread connectThreaded(DiscoveredRakNetServer server) {
		return this.connectThreaded(server.address);
	}

	/**
	 * Connects to a server with the specified address on it's own thread
	 * 
	 * @param address
	 * @param port
	 * @return Thread
	 */
	public Thread connectThreaded(String address, int port) {
		return this.connectThreaded(new InetSocketAddress(address, port));
	}

	/**
	 * Cancels the current connection to the server with the specified reason
	 * 
	 * @param reason
	 */
	public void disconnect(String reason) {
		if (session != null) {
			session.sendPacket(Reliability.UNRELIABLE, new ConnectedCloseConnection());
			if (this.state == SessionState.CONNECTED) {
				this.executeHook(Hook.SESSION_DISCONNECTED, session, reason, System.currentTimeMillis());
			}
			this.setState(SessionState.DISCONNECTED);
		}
		this.session = null;
	}

	/**
	 * Cancels the current connection to the server with the specified error
	 * being the reason
	 * 
	 * @param cause
	 */
	public void disconnect(Throwable cause) {
		this.disconnect(cause.getLocalizedMessage());
	}

	/**
	 * Cancels the current connection to the server
	 */
	public void disconnect() {
		this.disconnect("Disconnected");
	}

}