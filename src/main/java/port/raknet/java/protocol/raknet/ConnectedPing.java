package port.raknet.java.protocol.raknet;

import port.raknet.java.protocol.Packet;

public class ConnectedPing extends Packet {
	
	public long pingId;
	
	public ConnectedPing(Packet packet) {
		super(packet);
	}
	
	public ConnectedPing() {
		super(ID_CONNECTED_PING);
	}
	
	@Override
	public void encode() {
		this.putLong(pingId);
	}
	
}
