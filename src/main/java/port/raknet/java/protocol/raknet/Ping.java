package port.raknet.java.protocol.raknet;

import port.raknet.java.protocol.Packet;

public class Ping extends Packet {
	
	public long pingId;
	
	public Ping(Packet packet) {
		super(packet);
	}
	
	public Ping() {
		super(ID_CONNECTED_PING);
	}
	
	@Override
	public void encode() {
		this.putLong(pingId);
	}
	
}
