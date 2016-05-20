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
import port.raknet.java.protocol.raknet.ConnectionOpenReplyOne;
import port.raknet.java.protocol.raknet.ConnectionOpenReplyTwo;
import port.raknet.java.protocol.raknet.ConnectionOpenRequestOne;
import port.raknet.java.protocol.raknet.ConnectionOpenRequestTwo;
import port.raknet.java.protocol.raknet.EncapsulatedPacket;
import port.raknet.java.protocol.raknet.IncompatibleProtocolVersion;
import port.raknet.java.protocol.raknet.LegacyStatusRequest;
import port.raknet.java.protocol.raknet.LegacyStatusResponse;
import port.raknet.java.protocol.raknet.StatusRequest;
import port.raknet.java.protocol.raknet.StatusResponse;
import port.raknet.java.scheduler.RakNetScheduler;
import port.raknet.java.server.task.ServerTask;
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
		if (pid == ID_UNCONNECTED_STATUS_REQUEST) {
			StatusRequest csr = new StatusRequest(packet);
			csr.decode();

			if (csr.magic == true) {
				StatusResponse ssr = new StatusResponse();
				ssr.pingId = csr.pingId;
				ssr.serverId = this.serverId;
				Object[] parameters = this.executeHook(Hook.STATUS_REQUEST, options.broadcastName,
						session.getAddress());
				ssr.identifier = parameters[0].toString();
				ssr.encode();

				session.sendRaw(ssr);
			}
		} else if (pid == ID_UNCONNECTED_LEGACY_STATUS_REQUEST) {
			LegacyStatusRequest clsr = new LegacyStatusRequest(packet);
			clsr.decode();

			if (clsr.magic == true) {
				LegacyStatusResponse slsr = new LegacyStatusResponse();
				slsr.pingId = clsr.pingId;
				slsr.serverId = this.serverId;
				Object[] parameters = this.executeHook(Hook.LEGACY_PING, options.broadcastName, session.getAddress());
				slsr.data = parameters[0].toString();
				slsr.encode();

				session.sendRaw(slsr);
			}
		} else if (pid == ID_UNCONNECTED_OPEN_CONNECTION_REQUEST_1) {
			if (session.getState() == SessionState.DISCONNECTED) {
				ConnectionOpenRequestOne ccro = new ConnectionOpenRequestOne(packet);
				ccro.decode();

				if (ccro.magic == true) {
					session.setState(SessionState.CONNECTING_1);

					if (ccro.protocol == NETWORK_PROTOCOL) {
						if (ccro.mtuSize <= options.maximumTransferSize) {
							ConnectionOpenReplyOne scro = new ConnectionOpenReplyOne();
							scro.serverId = this.serverId;
							scro.security = false;
							scro.mtuSize = ccro.mtuSize;
							scro.encode();

							session.sendRaw(scro);
						}
					} else {
						IncompatibleProtocolVersion sipv = new IncompatibleProtocolVersion();
						sipv.version = NETWORK_PROTOCOL;
						sipv.serverId = this.serverId;
						sipv.encode();

						session.sendRaw(sipv);
					}
				}
			}
		} else if (pid == ID_UNCONNECTED_OPEN_CONNECTION_REQUEST_2) {
			if (session.getState() == SessionState.CONNECTING_1) {
				ConnectionOpenRequestTwo ccrt = new ConnectionOpenRequestTwo(packet);
				ccrt.decode();

				if (ccrt.magic == true) {
					session.setState(SessionState.CONNECTING_2);
					session.setSessionId(ccrt.clientId);
					session.setMTUSize(ccrt.mtuSize);

					ConnectionOpenReplyTwo scrt = new ConnectionOpenReplyTwo();
					scrt.serverId = this.serverId;
					scrt.clientAddress = session.getSystemAddress();
					scrt.mtuSize = session.getMTUSize();
					scrt.useSecurity = 0x00;
					scrt.encode();

					session.sendRaw(scrt);
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
		scheduler.scheduleRepeatingTask(new ServerTask(this, handler), 1L);
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

	public static void main(String[] args) {
		RakNetOptions options = new RakNetOptions();
		options.serverPort = 19132;
		options.maximumTransferSize = 4096;
		options.broadcastName = "MCPE;A RakNet Server;60;0.14.2;0;10";

		RakNetServer server = new RakNetServer(options);
		server.addHook(Hook.SESSION_CONNECTED, new HookRunnable() {

			@Override
			public void run(Object... parameters) {
				ClientSession session = (ClientSession) parameters[0];
				System.out.println("Client from " + session.getAddress() + " with client ID " + session.getSessionId()
						+ " has connected to the server!");
			}

		});
		server.addHook(Hook.PACKET_RECEIVED, new HookRunnable() {

			@Override
			public void run(Object... parameters) {
				ClientSession session = (ClientSession) parameters[0];
				EncapsulatedPacket encapsulated = (EncapsulatedPacket) parameters[1];

				System.out.println("Received game packet from " + session.getAddress() + " with ID: "
						+ encapsulated.convertPayload().getId());
			}

		});
		server.addHook(Hook.SESSION_DISCONNECTED, new HookRunnable() {

			@Override
			public void run(Object... parameters) {
				ClientSession session = (ClientSession) parameters[0];
				String reason = (String) parameters[1];
				System.out.println(
						"Client from " + session.getAddress() + " disconnected for reason: \"" + reason + "\"");
			}

		});
		server.startServer();
	}

}
