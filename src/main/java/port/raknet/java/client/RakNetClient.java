package port.raknet.java.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Random;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import port.raknet.java.RakNet;
import port.raknet.java.net.SessionState;
import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.SystemAddress;
import port.raknet.java.protocol.raknet.ConnectionOpenReplyOne;
import port.raknet.java.protocol.raknet.ConnectionOpenReplyTwo;
import port.raknet.java.protocol.raknet.ConnectionOpenRequestOne;
import port.raknet.java.protocol.raknet.ConnectionOpenRequestTwo;
import port.raknet.java.protocol.raknet.Ping;
import port.raknet.java.protocol.raknet.StatusResponse;

public class RakNetClient extends Thread implements RakNet {

	private static final long tickTime = 100L;
	private static final Random generator = new Random();

	public static void main(String[] args) throws Exception {
		RakNetClient rnk = new RakNetClient();
		rnk.start();

		Thread.sleep(1000);
		rnk.connect("localhost", 19132);
	}

	private final HashMap<InetSocketAddress, String> advertisers;

	private ServerSession session;
	private SessionState state = SessionState.DISCONNECTED;
	private Channel channel;

	public RakNetClient() {
		this.advertisers = new HashMap<InetSocketAddress, String>();
	}

	public void connect(String address, int port) {
		InetSocketAddress location = new InetSocketAddress(address, port);
		this.session = new ServerSession(channel, location);

		// Send first request packet
		ConnectionOpenRequestOne ccro = new ConnectionOpenRequestOne();
		ccro.mtuSize = 2048;
		ccro.protocol = 7;
		ccro.encode();

		state = SessionState.CONNECTING_1;
		session.sendRaw(ccro);
	}

	@Override
	public void run() {
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			// Create bootstrap and configure it
			Bootstrap b = new Bootstrap();
			b.group(group).channel(NioDatagramChannel.class).option(ChannelOption.SO_BROADCAST, true)
					.handler(new SimpleChannelInboundHandler<DatagramPacket>() {
						long clientId = new Random().nextLong();

						@Override
						protected void messageReceived(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
							try {
								Packet packet = new Packet(msg.content());
								short pid = packet.getId();
								if (pid == 0x1C) {
									StatusResponse response = new StatusResponse(packet);
									response.decode();
									advertisers.put(msg.sender(), response.identifier);
								}

								if (pid == ID_INCOMPATIBLE_PROTOCOL_VERSION) {
									System.err.println("INCOMPATIBLE PROTOCOL!");
									System.exit(0);
								} else if (state == SessionState.CONNECTING_1) {
									if (pid == ID_UNCONNECTED_OPEN_CONNECTION_REPLY_1) {
										if (msg.sender().equals(session.getAddress())) {
											ConnectionOpenReplyOne scro = new ConnectionOpenReplyOne(packet);
											scro.decode();

											if (scro.magic == true) {
												session.setMTUSize(scro.mtuSize);
												session.setClientId(scro.serverId);
												ConnectionOpenRequestTwo ccrt = new ConnectionOpenRequestTwo(packet);
												ccrt.address = new SystemAddress(
														InetAddress.getLocalHost().getHostAddress().substring(1), 0, 4);
												ccrt.clientId = this.clientId;
												ccrt.mtuSize = scro.mtuSize;
												ccrt.encode();
												session.sendRaw(ccrt);

												state = SessionState.CONNECTING_2;
											}
										}
									}
								} else if (state == SessionState.CONNECTING_2) {
									if (pid == ID_UNCONNECTED_OPEN_CONNECTION_REPLY_2) {
										if (msg.sender().equals(session.getAddress())) {
											ConnectionOpenReplyTwo scrt = new ConnectionOpenReplyTwo();
											scrt.decode();

										}
									}
								}

							} catch (IndexOutOfBoundsException e) {
								// Do NOT handle, if the packet is bad it does
								// not mean the world is ending :)
							}
						}
					});

			this.channel = b.bind(0).sync().channel();
			while (true)
				;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			group.shutdownGracefully();
		}
	}

	/**
	 * Used to update ServerSession data without blocking the main thread
	 */
	private class RakNetTracker extends Thread {
		@Override
		public void run() {
			long last = System.currentTimeMillis();
			while (true) {
				long current = System.currentTimeMillis();
				if (current - last >= tickTime) {
					session.pushLastReceiveTime(tickTime);
					if (session.getLastReceiveTime() / 5000L == 0.5) {
						// Ping ID's do not need to match
						Ping ping = new Ping();
						ping.pingId = generator.nextLong();
						ping.encode();
						session.sendPacket(ping);
					}
					if (session.getLastReceiveTime() > 5000L) {
						System.out.println("SERVER TIMEOUT!");
					} else {
						session.resendACK();
					}

					last = current;
				}
			}
		}
	}

}