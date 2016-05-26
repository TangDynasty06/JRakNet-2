package port.raknet.java.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import port.raknet.java.RakNetOptions;
import port.raknet.java.protocol.Packet;

public class RakNetClientBootstrap {
	
	private final RakNetOptions options;
	private Channel channel;
	
	public RakNetClientBootstrap(RakNetOptions options) {
		this.options = options;
	}

	public void run() {
		// Bind socket and start receiving data
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			Bootstrap b = new Bootstrap();
			b.group(group).channel(NioDatagramChannel.class).option(ChannelOption.SO_BROADCAST, true)
					.option(ChannelOption.SO_RCVBUF, options.maximumTransferSize)
					.option(ChannelOption.SO_SNDBUF, options.maximumTransferSize).handler(null);

			this.channel = b.bind(new InetSocketAddress(InetAddress.getLocalHost(), 0)).sync().channel();
		} catch (Exception e) {
			group.shutdownGracefully();
			e.printStackTrace();
		}
	}
	
	public void sendPacket(InetSocketAddress address, Packet packet) {
		channel.writeAndFlush(new DatagramPacket(packet.buffer(), address));
	}

}
