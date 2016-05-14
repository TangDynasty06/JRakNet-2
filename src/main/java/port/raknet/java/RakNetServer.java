package port.raknet.java;

import java.util.HashMap;
import java.util.Random;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import port.raknet.java.event.Hook;
import port.raknet.java.event.HookRunnable;
import port.raknet.java.net.ClientSession;
import port.raknet.java.net.RakNetHandler;
import port.raknet.java.net.SessionState;
import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.raknet.ClientConnectRequest;
import port.raknet.java.protocol.raknet.ClientHandshake;
import port.raknet.java.protocol.raknet.ConnectionOpenReplyOne;
import port.raknet.java.protocol.raknet.ConnectionOpenReplyTwo;
import port.raknet.java.protocol.raknet.ConnectionOpenRequestOne;
import port.raknet.java.protocol.raknet.ConnectionOpenRequestTwo;
import port.raknet.java.protocol.raknet.IncompatibleProtocolVersion;
import port.raknet.java.protocol.raknet.LegacyStatusRequest;
import port.raknet.java.protocol.raknet.LegacyStatusResponse;
import port.raknet.java.protocol.raknet.ServerHandshake;
import port.raknet.java.protocol.raknet.StatusRequest;
import port.raknet.java.protocol.raknet.StatusResponse;

/**
 * A RakNet server instance, used to handle the main packets and track
 * ClientSession states
 *
 * @author Trent Summerlin
 */
public class RakNetServer implements RakNet {

	private final long serverId;
	private final RakNetOptions options;
	private final RakNetTracker tracker;
	private final HashMap<Hook, HookRunnable> hooks;
	private RakNetHandler handler;

	public RakNetServer(RakNetOptions options) {
		this.serverId = new Random().nextLong();
		this.options = options;
		this.tracker = new RakNetTracker();
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
	public void handleRaw(short pid, Packet packet, ClientSession session) {
		if (pid == ID_STATUS_REQUEST) {
			StatusRequest csr = new StatusRequest(packet);
			csr.decode();

			if (csr.magic == true) {
				StatusResponse ssr = new StatusResponse();
				ssr.pingId = csr.pingId;
				ssr.serverId = this.serverId;
				Object[] parameters = this.executeHook(Hook.SERVER_PING, options.broadcastName, session.getAddress());
				ssr.identifier = parameters[0].toString();
				ssr.encode();

				session.sendRaw(ssr);
			}
		} else if (pid == ID_LEGACY_STATUS_REQUEST) {
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
		} else if (pid == ID_OPEN_CONNECTION_REQUEST_1) {
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
		} else if (pid == ID_OPEN_CONNECTION_REQUEST_2) {
			if (session.getState() == SessionState.CONNECTING_1) {
				ConnectionOpenRequestTwo ccrt = new ConnectionOpenRequestTwo(packet);
				ccrt.decode();

				if (ccrt.magic == true) {
					session.setState(SessionState.CONNECTING_2);
					session.setClientId(ccrt.clientId);
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
	 * Handles a packet
	 * 
	 * @param pid
	 * @param packet
	 * @param session
	 */
	public void handlePacket(short pid, Packet packet, ClientSession session) {
		if (session.getState() == SessionState.CONNECTED) {
			this.executeHook(Hook.PACKET_RECEIVED, pid, packet, session);
		} else {
			if (session.getState() == SessionState.CONNECTING_2) {
				if (pid == ID_CLIENT_CONNECT_REQUEST) {
					ClientConnectRequest cchr = new ClientConnectRequest(packet);
					cchr.decode();
					session.setState(SessionState.HANDSHAKING);

					ServerHandshake scha = new ServerHandshake();
					scha.clientAddress = session.getSystemAddress();
					scha.sendPing = cchr.sendPing;
					scha.sendPong = System.currentTimeMillis();
					scha.encode();

					session.sendPacket(scha);
				}
			} else if (session.getState() == SessionState.HANDSHAKING) {
				if (pid == ID_CLIENT_HANDSHAKE) {
					ClientHandshake cch = new ClientHandshake(packet);
					cch.decode();

					session.setState(SessionState.CONNECTED);
					this.executeHook(Hook.CLIENT_CONNECTED, session, System.currentTimeMillis());
				}
			}
		}
	}

	/**
	 * Starts the server
	 */
	public void startServer() {
		// Create tracker and start it
		this.handler = new RakNetHandler(this, 10);
		tracker.start();

		// Bind socket and start receiving data
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			Bootstrap b = new Bootstrap();
			b.group(group).channel(NioDatagramChannel.class).option(ChannelOption.SO_BROADCAST, true)
					.option(ChannelOption.SO_RCVBUF, options.maximumTransferSize)
					.option(ChannelOption.SO_SNDBUF, options.maximumTransferSize).handler(handler);

			b.bind(options.port).sync().channel().closeFuture().await();
		} catch (Exception e) {
			group.shutdownGracefully();
			e.printStackTrace();
		}
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
				if (current - last >= options.trackerWait) {
					for (ClientSession session : handler.getSessions()) {
						session.updateLastReceiveTime(options.trackerWait);
						if (session.getLastReceiveTime() > options.timeout) {
							handler.removeSession(session);
							executeHook(Hook.CLIENT_DISCONNECTED, session, "Timeout", System.currentTimeMillis());
						} else {
							session.resendACK();
						}
					}
					last = current;
				}
			}
		}
	}

	public static void main(String[] args) {
		RakNetOptions options = new RakNetOptions();
		options.port = 19132;
		options.maximumTransferSize = 4096;
		options.broadcastName = "MCPE;A RakNet Server;60;0.14.2;0;10";

		RakNetServer server = new RakNetServer(options);
		server.addHook(Hook.CLIENT_CONNECTED, new HookRunnable() {

			@Override
			public void run(Object... parameters) {
				ClientSession session = (ClientSession) parameters[0];
				System.out.println("Client from " + session.getAddress() + " with client ID " + session.getClientId()
						+ " has connected to the server!");
			}

		});
		server.addHook(Hook.PACKET_RECEIVED, new HookRunnable() {

			@Override
			public void run(Object... parameters) {
				short pid = (short) parameters[0];
				Packet packet = (Packet) parameters[1];
				ClientSession session = (ClientSession) parameters[2];

				System.out.println("Received game packet from " + session.getAddress() + "!");
				for (byte b : packet.array()) {
					System.out.print(Integer.toHexString(b).toUpperCase() + " ");
				}
				System.out.println();
			}

		});
		server.addHook(Hook.CLIENT_DISCONNECTED, new HookRunnable() {

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
