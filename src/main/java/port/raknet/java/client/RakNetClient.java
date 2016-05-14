package port.raknet.java.client;

import java.io.IOException;
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
import port.raknet.java.net.SessionState;
import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.SystemAddress;
import port.raknet.java.protocol.raknet.ConnectionOpenReplyOne;
import port.raknet.java.protocol.raknet.ConnectionOpenReplyTwo;
import port.raknet.java.protocol.raknet.ConnectionOpenRequestOne;
import port.raknet.java.protocol.raknet.ConnectionOpenRequestTwo;
import port.raknet.java.protocol.raknet.StatusRequest;
import port.raknet.java.protocol.raknet.StatusResponse;

public class RakNetClient extends Thread {

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

	public void advertise() throws InterruptedException {
		StatusRequest cpoc = new StatusRequest();
		cpoc.pingId = 1234567890L;
		cpoc.encode();
		channel.writeAndFlush(new DatagramPacket(cpoc.buffer(), new InetSocketAddress("255.255.255.255", 19132)))
				.sync();
	}

	public void sendServerPacket(Packet packet) {
		if (session == null) {
			try {
				throw new IOException("Server is not registered, unable to send packet!");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		session.sendPacket(packet);
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
		session.sendPacket(ccro);
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

								if (state != SessionState.DISCONNECTED) {
									if (state == SessionState.CONNECTING_1) {
										if (pid == 0x06) {
											if (msg.sender().equals(session.getAddress())) {
												ConnectionOpenReplyOne scro = new ConnectionOpenReplyOne(packet);
												scro.decode();

												if (scro.magic == true) {
													// Set session data
													session.mtuSize = scro.mtuSize;
													session.security = scro.security;
													session.serverId = scro.serverId;

													// Encode second response
													// and send it
													ConnectionOpenRequestTwo ccrt = new ConnectionOpenRequestTwo(packet);
													ccrt.address = new SystemAddress("127.0.0.1", 0, 4);
													ccrt.clientId = this.clientId;
													ccrt.mtuSize = scro.mtuSize;
													ccrt.encode();
													session.sendPacket(ccrt);

													state = SessionState.CONNECTING_2;
												}
											}
										}
									} else if (state == SessionState.CONNECTING_2) {
										if (pid == 0x08) {
											if (msg.sender().equals(session.getAddress())) {
												ConnectionOpenReplyTwo scrt = new ConnectionOpenReplyTwo();
												scrt.decode();

												if (scrt.magic == true) {
													System.out.println("CONNECTION SUCCESSFUL! Testing 0x09 send!");

												}
											}
										}
									}
								} else {
									System.out.println("Unknown packet! " + packet.getId());
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

};