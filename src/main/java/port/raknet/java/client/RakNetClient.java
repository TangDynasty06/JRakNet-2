package port.raknet.java.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Random;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import port.raknet.java.RakNet;
import port.raknet.java.RakNetOptions;
import port.raknet.java.event.Hook;
import port.raknet.java.event.HookRunnable;
import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.Reliability;
import port.raknet.java.protocol.raknet.ConnectedConnectRequest;
import port.raknet.java.protocol.raknet.UnconnectedConnectionReplyOne;
import port.raknet.java.protocol.raknet.UnconnectedConnectionReplyTwo;
import port.raknet.java.protocol.raknet.UnconnectedConnectionRequestOne;
import port.raknet.java.protocol.raknet.UnconnectedConnectionRequestTwo;
import port.raknet.java.scheduler.RakNetScheduler;
import port.raknet.java.session.ServerSession;
import port.raknet.java.session.SessionState;
import port.raknet.java.utils.RakNetUtils;

/**
 * A RakNet client instance, used to connect to RakNet servers
 *
 * @author Trent Summerlin
 */
public class RakNetClient implements RakNet {

	private static final InetAddress subnet = RakNetUtils.getSubnetMask();

	// Thread data
	private boolean running;

	// Client data
	private SessionState state;
	private final long clientId;
	private final RakNetOptions options;
	private final RakNetScheduler scheduler;
	private final RakNetClientHandler handler;
	private final HashMap<Hook, HookRunnable> hooks;
	private final HashMap<Long, String> advertisers;

	// Server and channel data
	private ServerSession session;
	private volatile Channel channel;

	public RakNetClient(RakNetOptions options) {
		this.state = SessionState.DISCONNECTED;
		this.clientId = new Random().nextLong();
		this.options = options;
		this.handler = new RakNetClientHandler(this);
		this.scheduler = new RakNetScheduler(options);
		if (options.maximumTransferSize % 2 != 0) {
			throw new RuntimeException("Invalid transfer size, must be divisble by 2!");
		}
		this.hooks = Hook.getHooks();
		this.advertisers = new HashMap<Long, String>();
	}

	/**
	 * Returns the client options which this instance uses
	 * 
	 * @return RakNetOptions
	 */
	public RakNetOptions getOptions() {
		return this.options;
	}

	/**
	 * Returns the ClientID
	 * 
	 * @return long
	 */
	public long getClientId() {
		return this.clientId;
	}

	/**
	 * Returns the client's current RakNet state
	 * 
	 * @return SessionState
	 */
	public SessionState getState() {
		return this.state;
	}

	/**
	 * Sets the client's current RakNet state
	 * 
	 * @param state
	 */
	public void setState(SessionState state) {
		this.state = state;
	}

	/**
	 * Sets the session using the specified values
	 * 
	 * @param sender
	 * @param serverId
	 * @param mtuSize
	 */
	private void addSession(InetSocketAddress sender, long serverId, short mtuSize) {
		ServerSession session = new ServerSession(channel, sender, this);
		session.setSessionId(serverId);
		session.setMTUSize(mtuSize);
		this.session = session;
	}

	/**
	 * Leaves the current session
	 * 
	 * @param reason
	 */
	public void removeSession(String reason) {
		this.executeHook(Hook.SESSION_DISCONNECTED, session, reason, System.currentTimeMillis());
		this.session = null;
	}

	/**
	 * Returns the current server session
	 * 
	 * @return ServerSession
	 */
	public ServerSession getSession() {
		return this.session;
	}

	/**
	 * Returns the address the client is bound to
	 * 
	 * @return InetSocketAddress
	 */
	public InetSocketAddress getLocalAddress() {
		return (InetSocketAddress) channel.localAddress();
	}

	/**
	 * Returns all the servers that have advertised to the client
	 * 
	 * @return long[]
	 */
	public long[] getAdvertisers() {
		long[] servers = new long[advertisers.size()];
		Long[] boxed = advertisers.keySet().toArray(new Long[advertisers.size()]);
		for (int i = 0; i < servers.length; i++) {
			servers[i] = boxed[i].longValue();
		}
		return servers;
	}

	/**
	 * Returns all the advertisements have advertised to the client
	 * 
	 * @return
	 */
	public String[] getAdvertisements() {
		return advertisers.values().toArray(new String[advertisers.size()]);
	}

	/**
	 * Returns an advertisement based on it's serverId
	 * 
	 * @param serverId
	 * @return
	 */
	public String getAdvertisement(long serverId) {
		return advertisers.get(serverId);
	}

	/**
	 * Removes an advertisement based on it's serverId
	 * 
	 * @param serverId
	 */
	public void removeAdvertiser(long serverId) {
		advertisers.remove(serverId);
	}

	/**
	 * Sets the HookRunnable for the specified Hook
	 * 
	 * @param type
	 * @param runnable
	 */
	public void addHook(Hook type, HookRunnable runnable) {
		hooks.put(type, runnable);
	}

	/**
	 * Removes the current HookRunnable for the specified Hook
	 * 
	 * @param type
	 */
	public void removeHook(Hook type) {
		hooks.put(type, null);
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
	public Object[] executeHook(Hook type, Object... parameters) {
		HookRunnable hook = hooks.get(type);
		if (hook != null) {
			hook.run(parameters);
		}
		return parameters;
	}

	/**
	 * Sends a raw packet to the specified address
	 * 
	 * @param address
	 * @param packet
	 */
	public void sendRaw(InetSocketAddress address, Packet packet) {
		channel.writeAndFlush(new DatagramPacket(packet.buffer(), address));
	}

	/**
	 * Sends a raw packet to the specified address
	 * 
	 * @param address
	 * @param port
	 * @param packet
	 */
	public void sendRaw(String address, int port, Packet packet) {
		this.sendRaw(new InetSocketAddress(address, port), packet);
	}

	/**
	 * Broadcasts a packet to the entire network
	 * 
	 * @param serverPort
	 * @param packet
	 */
	public void broadcastRaw(Packet packet) {
		if (subnet != null) {
			this.sendRaw(subnet.getHostAddress(), options.broadcastPort, packet);
		}
	}

	/**
	 * Handles a raw packet
	 * 
	 * @param pid
	 * @param packet
	 * @param session
	 */
	public void handleRaw(Packet packet, InetSocketAddress sender) {
		short pid = packet.getId();
		if (pid == ID_UNCONNECTED_CONNECTION_REPLY_1) {
			if (state == SessionState.CONNECTING_1) {
				UnconnectedConnectionReplyOne response = new UnconnectedConnectionReplyOne(packet);
				response.decode();

				if (response.magic == true) {
					this.addSession(sender, response.serverId, response.mtuSize);

					UnconnectedConnectionRequestTwo request = new UnconnectedConnectionRequestTwo();
					request.clientAddress = this.getLocalAddress();
					request.mtuSize = response.mtuSize;
					request.clientId = this.clientId;
					request.encode();

					session.sendRaw(request);
					this.state = SessionState.CONNECTING_2;
				}
			}
		} else if (pid == ID_UNCONNECTED_CONNECTION_REPLY_2) {
			if (state == SessionState.CONNECTING_2) {
				UnconnectedConnectionReplyTwo response = new UnconnectedConnectionReplyTwo(packet);
				response.decode();

				if (response.magic == true) {
					ConnectedConnectRequest request = new ConnectedConnectRequest();
					request.clientId = this.clientId;
					request.timestamp = System.currentTimeMillis();
					request.encode();

					session.sendPacket(Reliability.UNRELIABLE, request);
					this.state = SessionState.HANDSHAKING;
				}
			}
		} else if (pid == ID_UNCONNECTED_PONG) {

		}
	}

	/**
	 * Connects to the specified address
	 */
	public void connect(String address, int port) {
		UnconnectedConnectionRequestOne request = new UnconnectedConnectionRequestOne();
		request.protocol = RakNet.NETWORK_PROTOCOL;
		request.mtuSize = (short) options.maximumTransferSize;
		request.encode();

		this.sendRaw(address, port, request);
		this.state = SessionState.CONNECTING_1;
		// TODO: Repeatedly send 0x05 until we get a response
	}

	/**
	 * Returns whether or not the client is ready for use
	 * 
	 * @return boolean
	 */
	public boolean isReady() {
		return (this.channel != null);
	}

	/**
	 * Starts the client
	 */
	public void startClient() {
		if (running == true) {
			throw new RuntimeException("Client is already running!");
		}

		// Bind socket and start receiving data
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			Bootstrap b = new Bootstrap();
			b.group(group).channel(NioDatagramChannel.class).option(ChannelOption.SO_BROADCAST, true)
					.option(ChannelOption.SO_RCVBUF, options.maximumTransferSize)
					.option(ChannelOption.SO_SNDBUF, options.maximumTransferSize).handler(handler);

			this.channel = b.bind(new InetSocketAddress(InetAddress.getLocalHost(), 0)).sync().channel();
		} catch (Exception e) {
			group.shutdownGracefully();
			e.printStackTrace();
		}

		// Start scheduler
		scheduler.scheduleRepeatingTask(new RakNetClientTask(this), RakNetClientTask.TICK);
		scheduler.scheduleRepeatingTask(new RakNetAdvertiseTask(this), RakNetAdvertiseTask.TICK);
		scheduler.start();
		this.running = true;
	}

	/**
	 * Starts a client on it's own thread
	 * 
	 * @return Thread
	 */
	public Thread startThreadedClient() {
		RakNetClientThread thread = new RakNetClientThread(this);
		thread.start();
		return thread;
	}

}
