package port.raknet.java.server;

import java.util.HashMap;
import java.util.Random;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import port.raknet.java.RakNet;
import port.raknet.java.RakNetOptions;
import port.raknet.java.event.Hook;
import port.raknet.java.event.HookRunnable;
import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.raknet.UnconnectedConnectionReplyOne;
import port.raknet.java.protocol.raknet.UnconnectedConnectionReplyTwo;
import port.raknet.java.protocol.raknet.UnconnectedConnectionRequestOne;
import port.raknet.java.protocol.raknet.UnconnectedConnectionRequestTwo;
import port.raknet.java.protocol.raknet.UnconnectedLegacyPing;
import port.raknet.java.protocol.raknet.UnconnectedLegacyPong;
import port.raknet.java.protocol.raknet.UnconnectedPing;
import port.raknet.java.protocol.raknet.UnconnectedPong;
import port.raknet.java.scheduler.RakNetScheduler;
import port.raknet.java.session.ClientSession;
import port.raknet.java.session.SessionState;

/**
 * A RakNet server instance, used to handle the main packets and track
 * ClientSession states
 *
 * @author Trent Summerlin
 */
public class RakNetServer implements RakNet {

	private boolean running;

	private final long serverId;
	private final RakNetOptions options;
	private final RakNetScheduler scheduler;
	private final RakNetServerHandler handler;
	private final HashMap<Hook, HookRunnable> hooks;

	public RakNetServer(RakNetOptions options) {
		this.serverId = new Random().nextLong();
		this.options = options;
		this.handler = new RakNetServerHandler(this, 10);
		this.scheduler = new RakNetScheduler(options);
		if (options.maximumTransferSize % 2 != 0) {
			throw new RuntimeException("Invalid transfer size, must be divisble by 2!");
		}
		this.hooks = Hook.getHooks();
	}

	/**
	 * Returns the server options which this instance uses
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
	 * Handles a raw packet
	 * 
	 * @param pid
	 * @param packet
	 * @param session
	 */
	public void handleRaw(Packet packet, ClientSession session) {
		short pid = packet.getId();
		if (pid == ID_UNCONNECTED_PING) {
			UnconnectedPing ping = new UnconnectedPing(packet);
			ping.decode();

			if (ping.magic == true) {
				UnconnectedPong pong = new UnconnectedPong();
				pong.pingId = ping.pingId;
				pong.serverId = this.serverId;
				Object[] parameters = this.executeHook(Hook.STATUS_REQUEST, options.serverIdentifier,
						session.getAddress());
				pong.identifier = parameters[0].toString();
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
				Object[] parameters = this.executeHook(Hook.LEGACY_PING, options.serverIdentifier, session.getAddress());
				legacyPong.data = parameters[0].toString();
				legacyPong.encode();

				session.sendRaw(legacyPong);
			}
		} else if (pid == ID_UNCONNECTED_CONNECTION_REQUEST_1) {
			if (session.getState() == SessionState.DISCONNECTED) {
				UnconnectedConnectionRequestOne request = new UnconnectedConnectionRequestOne(packet);
				request.decode();

				if (request.magic == true && request.protocol == NETWORK_PROTOCOL
						&& request.mtuSize <= options.maximumTransferSize) {
					UnconnectedConnectionReplyOne response = new UnconnectedConnectionReplyOne();
					response.serverId = this.serverId;
					response.mtuSize = request.mtuSize;
					response.encode();

					session.sendRaw(response);
					session.setState(SessionState.CONNECTING_1);
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
	public void startServer() {
		if (running == true) {
			throw new RuntimeException("Server is already running!");
		}

		// Bind socket and start receiving data
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			Bootstrap b = new Bootstrap();
			b.group(group).channel(NioDatagramChannel.class).option(ChannelOption.SO_BROADCAST, true)
					.option(ChannelOption.SO_RCVBUF, options.maximumTransferSize)
					.option(ChannelOption.SO_SNDBUF, options.maximumTransferSize).handler(handler);

			b.bind(options.serverPort);
		} catch (Exception e) {
			group.shutdownGracefully();
			e.printStackTrace();
		}

		// Start scheduler
		scheduler.scheduleRepeatingTask(new RakNetServerTask(this, handler), RakNetServerTask.TICK);
		scheduler.start();
		this.running = true;
	}

	/**
	 * Starts a server on it's own thread
	 * 
	 * @return Thread
	 */
	public Thread startThreadedServer() {
		RakNetServerThread thread = new RakNetServerThread(this);
		thread.start();
		return thread;
	}

}
