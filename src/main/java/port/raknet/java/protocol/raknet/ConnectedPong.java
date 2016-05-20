package port.raknet.java.protocol.raknet;

import port.raknet.java.protocol.Packet;

public class ConnectedPong extends Packet {

	public long pingId;

	public ConnectedPong(Packet packet) {
		super(packet);
	}

	public ConnectedPong() {
		super(ID_CONNECTED_PONG);
	}

	@Override
	public void encode() {
		this.putLong(pingId);
	}

}
