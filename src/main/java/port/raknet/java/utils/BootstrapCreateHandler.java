package port.raknet.java.utils;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import port.raknet.java.protocol.Packet;

public class BootstrapCreateHandler extends SimpleChannelInboundHandler<DatagramPacket> {
	
	public Packet packet;
	
	@Override
	protected void messageReceived(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
		this.packet = new Packet(msg.content());
	}
	
}
