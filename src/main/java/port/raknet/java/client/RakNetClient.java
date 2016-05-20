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
import port.raknet.java.client.task.AdvertiseTask;
import port.raknet.java.client.task.ClientTask;
import port.raknet.java.event.Hook;
import port.raknet.java.event.HookRunnable;
import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.SystemAddress;
import port.raknet.java.protocol.raknet.ClientHandshake;
import port.raknet.java.protocol.raknet.ConnectionOpenReplyOne;
import port.raknet.java.protocol.raknet.ConnectionOpenReplyTwo;
import port.raknet.java.protocol.raknet.ConnectionOpenRequestOne;
import port.raknet.java.protocol.raknet.ConnectionOpenRequestTwo;
import port.raknet.java.protocol.raknet.StatusResponse;
import port.raknet.java.scheduler.RakNetScheduler;
import port.raknet.java.session.ServerSession;
import port.raknet.java.session.SessionState;

public class RakNetClient implements RakNet {

	private boolean running;

	private final long clientId;
	private final RakNetOptions options;
	private final RakNetScheduler scheduler;
	private final RakNetClientHandler handler;
	private final HashMap<Hook, HookRunnable> hooks;
	private final HashMap<Long, String> advertisers;
	private SessionState state;
	private volatile Channel channel;

	public RakNetClient(RakNetOptions options) {
		this.clientId = new Random().nextLong();
		this.options = options;
		this.handler = new RakNetClientHandler(this);
		this.scheduler = new RakNetScheduler(options);
		if (options.maximumTransferSize % 2 != 0) {
			throw new RuntimeException("Invalid transfer size, must be divisble by 2!");
		}
		this.advertisers = new HashMap<Long, String>();
		this.state = SessionState.DISCONNECTED;
		this.hooks = Hook.getHooks();
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
	 * Returns the current server session
	 * 
	 * @return ServerSession
	 */
	public ServerSession getSession() {
		return handler.getSession();
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
		this.sendRaw("255.255.255.255", options.broadcastPort, packet);
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
		System.out.println("Received raw packet: " + pid);
		if (pid == ID_UNCONNECTED_OPEN_CONNECTION_REPLY_1) {
			if (state == SessionState.CONNECTING_1) {
				ConnectionOpenReplyOne scoro = new ConnectionOpenReplyOne(packet);
				scoro.decode();

				if (scoro.magic == true) {
					ServerSession session = new ServerSession(channel, sender, this);
					session.setSessionId(scoro.serverId);
					session.setMTUSize(scoro.mtuSize);
					handler.setSession(session);

					ConnectionOpenRequestTwo ccort = new ConnectionOpenRequestTwo();
					ccort.address = this.convertLocalAddress();
					ccort.clientId = this.clientId;
					ccort.mtuSize = scoro.mtuSize;
					ccort.encode();

					this.sendRaw(sender, packet);
					this.state = SessionState.CONNECTING_2;
				}
			}
		} else if (pid == ID_UNCONNECTED_OPEN_CONNECTION_REPLY_2) {
			if (state == SessionState.CONNECTING_2) {
				ConnectionOpenReplyTwo scort = new ConnectionOpenReplyTwo(packet);
				scort.decode();

				if (scort.magic == true) {
					// Most of the data here is the same, so don't bother
					ClientHandshake handshake = new ClientHandshake();
					handshake.address = this.convertLocalAddress();
					handshake.sendPing = new Random().nextLong();
					handshake.sendPong = new Random().nextLong();
					handshake.encode();

					ServerSession session = handler.getSession();
					session.sendPacket(handshake);
					this.state = SessionState.HANDSHAKING;
				}
			}
		} else if (pid == ID_INCOMPATIBLE_PROTOCOL_VERSION) {
			if (state.getOrder() >= SessionState.CONNECTING_1.getOrder()
					&& state.getOrder() <= SessionState.CONNECTING_2.getOrder() && handler.getSession() != null) {
				this.executeHook(Hook.SESSION_DISCONNECTED, handler.getSession(), "Incompatible protocol",
						System.currentTimeMillis());
				this.state = SessionState.DISCONNECTED;
				handler.setSession(null);
			}
		} else if (pid == ID_UNCONNECTED_STATUS_RESPONSE) {
			StatusResponse response = new StatusResponse(packet);
			response.decode();

			if (response.magic == true) {
				advertisers.put(response.serverId, response.identifier);
			}
		}

		// TODO: Get packets for login sending correctly
	}

	/**
	 * Connects to the specified address
	 */
	public void connect(String address, int port) {
		ConnectionOpenRequestOne cocro = new ConnectionOpenRequestOne();
		cocro.protocol = RakNet.NETWORK_PROTOCOL;
		cocro.mtuSize = (short) options.maximumTransferSize;
		cocro.encode();

		this.sendRaw(address, port, cocro);
		this.state = SessionState.CONNECTING_1;
	}

	/**
	 * Cancels a connection to the server
	 */
	public void cancelConnect() {
		Packet disconnected = new Packet(ID_CONNECTED_CANCEL_CONNECTION);
		handler.getSession().sendPacket(disconnected);
		// TODO: Finish this method
	}

	/**
	 * Converts the client's local address to a SystemAdress
	 * 
	 * @return SystemAddress
	 */
	private SystemAddress convertLocalAddress() {
		try {
			String localAddress = InetAddress.getLocalHost().getHostAddress();
			int localPort = ((InetSocketAddress) channel.localAddress()).getPort();
			System.out.println(localAddress + ":" + localPort);
			return new SystemAddress(localAddress, localPort, 4);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
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

			this.channel = b.bind(options.serverPort).sync().channel();
		} catch (Exception e) {
			group.shutdownGracefully();
			e.printStackTrace();
		}

		// Start scheduler
		scheduler.scheduleRepeatingTask(new ClientTask(this, handler), 1L);
		scheduler.scheduleRepeatingTask(new AdvertiseTask(this), 1000L);
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

	public static void main(String[] args) {
		RakNetOptions options = new RakNetOptions();
		RakNetClient client = new RakNetClient(options);
		client.startThreadedClient();

		while (!client.isReady())
			;
		System.out.println("Client ready!");
		client.connect("192.168.1.14", 19132);
	}

}
