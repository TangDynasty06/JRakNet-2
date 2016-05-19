package port.raknet.java.session;

import java.net.InetSocketAddress;

import io.netty.channel.ChannelHandlerContext;
import port.raknet.java.protocol.raknet.EncapsulatedPacket;

public class ServerSession extends RakNetSession {

	public ServerSession(ChannelHandlerContext context, InetSocketAddress address) {
		super(context, address);
	}

	@Override
	public void handleEncapsulated(EncapsulatedPacket encapsulated) {
		// TODO Auto-generated method stub
		
	}

}
