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
import port.raknet.java.protocol.raknet.Acknowledge;
import port.raknet.java.protocol.raknet.ConnectionHandshakeAccepted;
import port.raknet.java.protocol.raknet.ConnectionHandshakeRequest;
import port.raknet.java.protocol.raknet.ConnectionReplyOne;
import port.raknet.java.protocol.raknet.ConnectionReplyTwo;
import port.raknet.java.protocol.raknet.ConnectionRequestOne;
import port.raknet.java.protocol.raknet.ConnectionRequestTwo;
import port.raknet.java.protocol.raknet.IncompatibleProtocolVersion;
import port.raknet.java.protocol.raknet.LegacyStatusRequest;
import port.raknet.java.protocol.raknet.LegacyStatusResponse;
import port.raknet.java.protocol.raknet.StatusRequest;
import port.raknet.java.protocol.raknet.StatusResponse;

public class RakNetServer implements RakNet {

	private final long serverId;
	private final RakNetOptions options;
	private final HashMap<Hook, HookRunnable> hooks;

	public RakNetServer(RakNetOptions options) {
		this.serverId = new Random().nextLong();
		this.options = options;
		if (options.maximumTransferSize % 2 != 0) {
			throw new RuntimeException("Invalid transfer size, must be divisble by 2!");
		}
		this.hooks = Hook.getHooks();
	}

	public RakNetOptions getOptions() {
		return this.options;
	}

	public long getServerId() {
		return this.serverId;
	}

	public void addHook(Hook type, HookRunnable runnable) {
		hooks.put(type, runnable);
	}

	public void removeHook(Hook type) {
		hooks.put(type, null);
	}

	public Object[] executeHook(Hook type, Object... parameters) {
		HookRunnable hook = hooks.get(type);
		if (hook != null) {
			hook.run(parameters);
		}
		return parameters;
	}

	public void startServer() {
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			Bootstrap b = new Bootstrap();
			b.group(group).channel(NioDatagramChannel.class).option(ChannelOption.SO_BROADCAST, true)
					.option(ChannelOption.SO_RCVBUF, options.maximumTransferSize)
					.option(ChannelOption.SO_SNDBUF, options.maximumTransferSize).handler(new RakNetHandler(this, 10));

			b.bind(options.port).sync().channel().closeFuture().await();
		} catch (Exception e) {
			group.shutdownGracefully();
			e.printStackTrace();
		}
	}

	public void handlePacket(short pid, Packet packet, ClientSession session) {
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
			ConnectionRequestOne ccro = new ConnectionRequestOne(packet);
			ccro.decode();

			if (ccro.magic == true) {
				session.setState(SessionState.CONNECTING_1);

				if (ccro.protocol == NETWORK_PROTOCOL) {
					if (ccro.mtuSize <= options.maximumTransferSize) {
						ConnectionReplyOne scro = new ConnectionReplyOne();
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
		} else if (pid == ID_OPEN_CONNECTION_REQUEST_2) {
			ConnectionRequestTwo ccrt = new ConnectionRequestTwo(packet);
			ccrt.decode();

			if (ccrt.magic == true) {
				session.setState(SessionState.CONNECTING_2);
				session.setClientId(ccrt.clientId);
				session.setMTUSize(ccrt.mtuSize);

				ConnectionReplyTwo scrt = new ConnectionReplyTwo();
				scrt.serverId = this.serverId;
				scrt.clientAddress = session.getSystemAddress();
				scrt.mtuSize = session.getMTUSize();
				scrt.useSecurity = 0x00;
				scrt.encode();

				session.sendRaw(scrt);
			}
		} else if (pid == ACK) {
			Acknowledge ack = new Acknowledge(packet);
			ack.decode();
			System.out.println("RECEIVED ACK FOR " + ack.packets[0]);
		}
	}

	public void handleDataPacket(short pid, Packet packet, ClientSession session) {
		if (pid == ID_CONNECTION_HANDSHAKE_REQUEST) {
			session.setState(SessionState.HANDSHAKING);
			System.out.println("Received 0x09!");
			ConnectionHandshakeRequest cchr = new ConnectionHandshakeRequest(packet);
			cchr.decode();

			ConnectionHandshakeAccepted scha = new ConnectionHandshakeAccepted();
			scha.clientAddress = session.getSystemAddress();
			scha.requestTime = cchr.requestTime;
			scha.time = System.currentTimeMillis();
			scha.encode();

			session.sendPacket(scha);
		} else {
			System.out.println("UNKNWON PID: " + pid);
		}
	}

	public static void main(String[] args) {
		RakNetOptions options = new RakNetOptions();
		options.port = 19132;
		options.maximumTransferSize = 4096;
		options.broadcastName = "MCPE;A RakNet Server;60;0.14.2;0;10";

		RakNetServer server = new RakNetServer(options);
		server.startServer();
	}

}
