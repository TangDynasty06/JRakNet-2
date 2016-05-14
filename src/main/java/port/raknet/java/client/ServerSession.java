package port.raknet.java.client;

import java.net.InetSocketAddress;

import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import port.raknet.java.protocol.Packet;

public class ServerSession {
	
	public int mtuSize;
	public boolean security;
	public long serverId;
	
	private final Channel channel;
	private final InetSocketAddress address;

	public ServerSession(Channel channel, InetSocketAddress address) {
		this.channel = channel;
		this.address = address;
	}

	public int getMTU() {
		return this.mtuSize;
	}

	public boolean hasSecurity() {
		return this.security;
	}

	public long getServerId() {
		return this.serverId;
	}
	
	public InetSocketAddress getAddress() {
		return this.address;
	}

	public void sendPacket(Packet packet) {
		channel.writeAndFlush(new DatagramPacket(packet.buffer(), address));
	}

}
