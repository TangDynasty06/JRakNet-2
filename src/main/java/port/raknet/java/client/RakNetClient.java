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
package port.raknet.java.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Random;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import port.raknet.java.RakNet;
import port.raknet.java.RakNetOptions;
import port.raknet.java.event.Hook;
import port.raknet.java.event.HookRunnable;
import port.raknet.java.exception.IncompatibleProtocolException;
import port.raknet.java.exception.MaximumTransferUnitException;
import port.raknet.java.exception.RakNetException;
import port.raknet.java.exception.UnexpectedPacketException;
import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.Reliability;
import port.raknet.java.protocol.raknet.ConnectedCancelConnection;
import port.raknet.java.protocol.raknet.ConnectedConnectRequest;
import port.raknet.java.protocol.raknet.UnconnectedConnectionReplyOne;
import port.raknet.java.protocol.raknet.UnconnectedConnectionReplyTwo;
import port.raknet.java.protocol.raknet.UnconnectedConnectionRequestOne;
import port.raknet.java.protocol.raknet.UnconnectedConnectionRequestTwo;
import port.raknet.java.protocol.raknet.UnconnectedIncompatibleProtocol;
import port.raknet.java.protocol.raknet.internal.Acknowledge;
import port.raknet.java.protocol.raknet.internal.CustomPacket;
import port.raknet.java.scheduler.RakNetScheduler;
import port.raknet.java.session.ServerSession;
import port.raknet.java.session.SessionState;
import port.raknet.java.task.ServerTimeoutTask;

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
	private final HashMap<Hook, HookRunnable> hooks;

	// Netty data
	private volatile Channel channel;
	private volatile ServerSession session;
	private volatile SessionState state = SessionState.DISCONNECTED;

	public RakNetClient(RakNetOptions options) {
		this.clientId = new Random().nextLong();
		this.timestamp = System.currentTimeMillis();
		this.options = options;
		this.scheduler = new RakNetScheduler();
		this.hooks = new HashMap<Hook, HookRunnable>();
	}

	public RakNetOptions getOptions() {
		return this.options;
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
	 * Removes the current session with the specified reason
	 * 
	 * @param reason
	 */
	public void removeSession(String reason) {
		this.executeHook(Hook.SESSION_DISCONNECTED, session, reason, System.currentTimeMillis());
		this.session = null;
		this.channel = null;
		this.setState(SessionState.DISCONNECTED);
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
			hooks.get(hook).run(hook, parameters);
		}
		return parameters;
	}

	/**
	 * Handles a raw packet
	 * 
	 * @param packet
	 * @param sender
	 */
	public void handleRaw(Packet packet, InetSocketAddress sender) throws RakNetException {
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

					session.sendPacket(Reliability.UNRELIABLE, ccr);
					this.setState(SessionState.HANDSHAKING);
				}
			}
		} else if (pid == ID_UNCONNECTED_INCOMPATIBLE_PROTOCOL) {
			UnconnectedIncompatibleProtocol ucp = new UnconnectedIncompatibleProtocol(packet);
			ucp.decode();

			if (ucp.magic == true && session.isServer(sender)) {
				throw new IncompatibleProtocolException(ucp.protocol, NETWORK_PROTOCOL);
			}
		} else if (pid == ID_UNCONNECTED_PONG) {
			// TODO
		}
	}

	/**
	 * Handles a CustomPacket
	 * 
	 * @param custom
	 * @param sender
	 */
	public void handleCustom(CustomPacket custom, InetSocketAddress sender) throws RakNetException {
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
	public void handleAck(Acknowledge ack, InetSocketAddress sender) throws UnexpectedPacketException {
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
	public void handleNack(Acknowledge nack, InetSocketAddress sender) throws UnexpectedPacketException {
		if (session != null) {
			if (session.isServer(sender)) {
				session.handleNack(nack);
			}
		}
	}

	/**
	 * Connects to a server with the specified address
	 * 
	 * @param address
	 */
	public void connect(InetSocketAddress address) throws RakNetException {
		// Disconnect from current session
		if (session != null) {
			if (state != SessionState.CONNECTED) {
				this.cancelConnect();
			}
			this.removeSession("Disconnected");
		}

		// Create bootstrap along with handler and set options
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			int mtu = options.maximumTransferUnit;
			RakNetClientHandler handler = new RakNetClientHandler(this);
			Bootstrap bootstrap = new Bootstrap().group(group).channel(NioDatagramChannel.class);
			bootstrap.option(ChannelOption.SO_RCVBUF, mtu).option(ChannelOption.SO_SNDBUF, mtu).handler(handler);

			// Do not bind to "localhost", it locks up the handler
			this.channel = bootstrap.bind(0).sync().channel();
			this.session = new ServerSession(channel, address, this);
			this.setState(SessionState.CONNECTING_1);

			// Request connection until response is received
			while (!handler.foundMtu) {
				if (mtu < MINIMUM_TRANSFER_UNIT) {
					throw new MaximumTransferUnitException();
				}

				UnconnectedConnectionRequestOne request = new UnconnectedConnectionRequestOne();
				request.mtuSize = (short) mtu;
				request.protocol = 7;
				request.encode();

				bootstrap.option(ChannelOption.SO_SNDBUF, (int) request.mtuSize);
				session.sendRaw(request);

				mtu -= 100L;
				Thread.sleep(500L);
			}
		} catch (Exception e) {
			e.printStackTrace();
			group.shutdownGracefully();
		}

		// Start scheduler
		if (running == false) {
			scheduler.scheduleRepeatingTask(new ServerTimeoutTask(this), ServerTimeoutTask.TICK);
			scheduler.start();
			this.running = true;
		}
	}

	/**
	 * Connects to a server with the specified address
	 * 
	 * @param address
	 * @param port
	 * @throws RakNetException
	 */
	public void connect(String address, int port) throws RakNetException {
		this.connect(new InetSocketAddress(address, port));
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
	 * Cancels the current connection to the server
	 */
	public void cancelConnect() {
		if (session != null) {
			session.sendPacket(Reliability.UNRELIABLE, new ConnectedCancelConnection());
		}
	}

}