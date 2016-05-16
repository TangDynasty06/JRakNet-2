package port.raknet.java.protocol.raknet;

import port.raknet.java.protocol.Packet;

public class Pong extends Packet {

	public long pingId;

	public Pong(Packet packet) {
		super(packet);
	}

	public Pong() {
		super(ID_CONNECTED_PONG);
	}

	@Override
	public void encode() {
		this.putLong(pingId);
	}

}
