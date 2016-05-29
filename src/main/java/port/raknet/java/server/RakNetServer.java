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
import port.raknet.java.exception.RakNetException;
import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.raknet.UnconnectedConnectionReplyOne;
import port.raknet.java.protocol.raknet.UnconnectedConnectionReplyTwo;
import port.raknet.java.protocol.raknet.UnconnectedConnectionRequestOne;
import port.raknet.java.protocol.raknet.UnconnectedConnectionRequestTwo;
import port.raknet.java.protocol.raknet.UnconnectedIncompatibleProtocol;
import port.raknet.java.protocol.raknet.UnconnectedLegacyPing;
import port.raknet.java.protocol.raknet.UnconnectedLegacyPong;
import port.raknet.java.protocol.raknet.UnconnectedPing;
import port.raknet.java.protocol.raknet.UnconnectedPong;
import port.raknet.java.scheduler.RakNetScheduler;
import port.raknet.java.session.ClientSession;
import port.raknet.java.session.SessionState;
import port.raknet.java.task.ClientTimeoutTask;

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
		this.serverId = new Random().nextLong();
		this.timestamp = System.currentTimeMillis();
		this.options = options;
		this.handler = new RakNetServerHandler(this);
		this.scheduler = new RakNetScheduler();
		if (options.maximumTransferUnit % 2 != 0) {
			throw new RuntimeException("Invalid transfer size, must be divisble by 2!");
		}
		this.hooks = new HashMap<Hook, HookRunnable>();
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
	public void startServer() throws RakNetException {
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
		scheduler.scheduleRepeatingTask(new ClientTimeoutTask(this, handler), ClientTimeoutTask.TICK);
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
