package port.raknet.java.client;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Random;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import port.raknet.java.RakNet;
import port.raknet.java.RakNetOptions;
import port.raknet.java.event.Hook;
import port.raknet.java.event.HookRunnable;
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

/**
 * Used to connect and send data to <code>RakNetServers</code> with ease
 *
 * @author Trent Summerlin
 */
public class RakNetClient implements RakNet {

	private final long clientId;
	private final long timestamp;
	private final RakNetOptions options;
	private final PacketTask packetTask;
	private final RakNetScheduler scheduler;
	private final HashMap<Hook, HookRunnable> hooks;

	private int packetTaskId;
	private Channel channel;
	private volatile ServerSession session;
	private volatile SessionState state = SessionState.DISCONNECTED;

	public RakNetClient(RakNetOptions options) {
		this.clientId = new Random().nextLong();
		this.timestamp = System.currentTimeMillis();
		this.options = options;
		this.packetTask = new PacketTask(this);
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
	 * Removes the current session with the specified reason
	 * 
	 * @param reason
	 */
	private void removeSession(String reason) {
		this.executeHook(Hook.SESSION_DISCONNECTED, session, reason, System.currentTimeMillis());
		scheduler.cancelRepeatingTask(this.packetTaskId);
		this.session = null;
		this.channel = null;
		this.state = SessionState.DISCONNECTED;
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
	 * Returns the address the client's current channel is bound to
	 * 
	 * @return InetSocketAddress
	 */
	public InetSocketAddress getLocalAddress() {
		if (channel != null) {
			return (InetSocketAddress) channel.localAddress();
		} else {
			return new InetSocketAddress("0.0.0.0", 0);
		}
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
		if (hooks.containsKey(type)) {
			hooks.get(type).run(parameters);
		}
		return parameters;
	}

	/**
	 * Handles a raw packet
	 * 
	 * @param packet
	 * @param sender
	 */
	public void handleRaw(Packet packet, InetSocketAddress sender) {
		short pid = packet.getId();
		System.out.println("received packet " + pid);
		if (pid == ID_UNCONNECTED_CONNECTION_REPLY_1) {
			if (state == SessionState.CONNECTING_1) {
				UnconnectedConnectionReplyOne ucro = new UnconnectedConnectionReplyOne(packet);
				ucro.decode();

				if (ucro.magic == true && session.isServer(sender)) {
					session.setSessionId(ucro.serverId);
					session.setMTUSize(ucro.mtuSize);

					UnconnectedConnectionRequestTwo ucrt = new UnconnectedConnectionRequestTwo();
					ucrt.clientId = this.clientId;
					ucrt.clientAddress = this.getLocalAddress();
					ucrt.mtuSize = (short) options.maximumTransferSize;
					ucrt.encode();

					this.state = SessionState.CONNECTING_2;
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
					this.state = SessionState.HANDSHAKING;
				}
			}
		} else if (pid == ID_UNCONNECTED_INCOMPATIBLE_PROTOCOL) {
			UnconnectedIncompatibleProtocol ucp = new UnconnectedIncompatibleProtocol(packet);
			ucp.decode();

			if (ucp.magic == true && session.isServer(sender)) {
				this.removeSession("Incompatible protocol");
			}
		} else if (pid == ID_UNCONNECTED_PONG) {
			packet.getLong();
			packet.getLong();
			packet.checkMagic();
			System.out.println("RECEIVED STATUS: " + packet.getString());
		}
	}

	/**
	 * Handles a CustomPacket
	 * 
	 * @param custom
	 * @param sender
	 */
	public void handleCustom(CustomPacket custom, InetSocketAddress sender) {
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
	public void handleAck(Acknowledge ack, InetSocketAddress sender) {
		if (session != null) {
			if (session.isServer(sender)) {
				// TODO
			}
		}
	}

	/**
	 * Handles a NACK packet
	 * 
	 * @param nack
	 * @param sender
	 */
	public void handleNack(Acknowledge nack, InetSocketAddress sender) {
		if (session != null) {
			if (session.isServer(sender)) {
				try {
					session.handleNack(nack);
				} catch (UnexpectedPacketException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Connects to a server with the specified address
	 * 
	 * @param address
	 */
	public void connect(InetSocketAddress address) {
		// Disconnect from current session
		if (session != null) {
			if (state != SessionState.CONNECTED) {
				this.cancelConnect();
			}
			this.session = null;
			this.state = SessionState.DISCONNECTED;
		}

		// Create socket and bind
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			Bootstrap b = new Bootstrap();
			b.group(group).channel(NioDatagramChannel.class).option(ChannelOption.SO_BROADCAST, true)
					.option(ChannelOption.SO_RCVBUF, options.maximumTransferSize)
					.option(ChannelOption.SO_SNDBUF, options.maximumTransferSize)
					.handler(new RakNetClientHandler(this));
			this.channel = (DatagramChannel) b.bind(new InetSocketAddress("localhost", 0)).sync().channel();
			this.session = new ServerSession(channel, address, this);
		} catch (Exception e) {
			e.printStackTrace();
			group.shutdownGracefully();
		}

		// Create packet
		UnconnectedConnectionRequestOne ucro = new UnconnectedConnectionRequestOne();
		ucro.mtuSize = (short) options.maximumTransferSize;
		ucro.protocol = NETWORK_PROTOCOL;
		ucro.encode();

		// Start scheduler
		this.state = SessionState.CONNECTING_1;
		this.packetTaskId = scheduler.scheduleRepeatingTask(packetTask, PacketTask.TICK);
		scheduler.start();
	}

	/**
	 * Connects to a server with the specified address
	 * 
	 * @param address
	 * @param port
	 */
	public void connect(String address, int port) {
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

	public static void main(String[] args) throws Exception {
		RakNetOptions options = new RakNetOptions();
		options.maximumTransferSize = 1024;
		RakNetClient client = new RakNetClient(options);
		client.connect(new InetSocketAddress("192.168.1.14", 19132));
		while (client.getState() != SessionState.CONNECTED)
			;
		System.out.println("Connected to server! Now cancelling connection lmao");
		client.cancelConnect();
	}

}
