package port.raknet.java.protocol.raknet;

import port.raknet.java.protocol.Packet;

public class UnconnectedPing extends Packet {

	public long pingId;
	public boolean magic;

	public UnconnectedPing(Packet packet) {
		super(packet);
	}

	public UnconnectedPing() {
		super(ID_UNCONNECTED_PING);
	}

	@Override
	public void encode() {
		this.putLong(pingId);
		this.putMagic();
	}

	@Override
	public void decode() {
		this.pingId = this.getLong();
		this.magic = this.checkMagic();
	}

}
