package port.raknet.java.client;

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
import port.raknet.java.SessionState;
import port.raknet.java.event.Hook;
import port.raknet.java.event.HookRunnable;
import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.raknet.Ping;
import port.raknet.java.protocol.raknet.StatusRequest;
import port.raknet.java.protocol.raknet.StatusResponse;
import port.raknet.java.session.ServerSession;

public class RakNetClient implements RakNet {

	private static final Random generator = new Random();
	private static final long tickTime = 100L;

	private final long clientId;
	private final RakNetOptions options;
	private final RakNetTracker tracker;
	private final HashMap<Hook, HookRunnable> hooks;
	private final HashMap<Long, String> advertisers;
	private RakNetClientHandler handler;
	private Channel channel;
	private boolean running;

	private long lastPingId = 0L;

	public RakNetClient(RakNetOptions options) {
		this.clientId = generator.nextLong();
		this.options = options;
		this.tracker = new RakNetTracker();
		if (options.maximumTransferSize % 2 != 0) {
			throw new RuntimeException("Invalid transfer size, must be divisble by 2!");
		}
		this.advertisers = new HashMap<Long, String>();
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
	 * Broadcasts a packet to the entire network to the specified port
	 * 
	 * @param port
	 * @param packet
	 */
	public void broadcastPacket(int port, Packet packet) {
		channel.writeAndFlush(new DatagramPacket(packet.buffer(), new InetSocketAddress("255.255.255.255", port)));
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
		if (pid == ID_UNCONNECTED_STATUS_RESPONSE) {
			StatusResponse response = new StatusResponse(packet);
			response.decode();

			if (response.pingId == lastPingId && response.magic == true) {
				advertisers.put(response.serverId, response.identifier);
			}
		}
	}

	/**
	 * Starts the client
	 */
	public void startClient() {
		if (running == true) {
			throw new RuntimeException("Client is already running!");
		}
		this.handler = new RakNetClientHandler(this, 10);
		tracker.start();

		// Bind socket and start receiving data
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			Bootstrap b = new Bootstrap();
			b.group(group).channel(NioDatagramChannel.class).option(ChannelOption.SO_BROADCAST, true)
					.option(ChannelOption.SO_RCVBUF, options.maximumTransferSize)
					.option(ChannelOption.SO_SNDBUF, options.maximumTransferSize).handler(handler);

			this.channel = b.bind(options.port).sync().channel();
			channel.closeFuture().await();
		} catch (Exception e) {
			group.shutdownGracefully();
			e.printStackTrace();
		}
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

	/**
	 * Used to update ClientSession data without blocking the main thread
	 */
	private class RakNetTracker extends Thread {
		@Override
		public void run() {
			long last = System.currentTimeMillis();
			while (true) {
				long current = System.currentTimeMillis();
				if (current - last >= 1000L) {
					StatusRequest request = new StatusRequest();
					request.pingId = generator.nextLong();
					request.encode();

					broadcastPacket(options.broadcastPort, request);
				}
				if (current - last >= tickTime) {
					ServerSession session = handler.getSession();
					if (session != null) {
						session.pushLastReceiveTime(tickTime);
						if (session.getLastReceiveTime() / options.timeout == 0.5) {
							// Ping ID's do not need to match
							Ping ping = new Ping();
							ping.pingId = lastPingId = generator.nextLong();
							ping.encode();
							session.sendPacket(ping);
						} else if (session.getLastReceiveTime() > options.timeout) {
							if (session.getState() == SessionState.CONNECTED) {
								handler.removeSession("Timeout");
							}
						} else {
							session.resendACK();
						}
					}
					last = current;
				}
			}
		}
	}

}
