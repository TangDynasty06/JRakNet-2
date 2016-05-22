package port.raknet.java.protocol.raknet;

import port.raknet.java.protocol.Packet;

public class UnconnectedLegacyPing extends Packet {

	public long pingId;
	public boolean magic;

	public UnconnectedLegacyPing(Packet packet) {
		super(packet);
	}

	public UnconnectedLegacyPing() {
		super(ID_UNCONNECTED_LEGACY_PING);
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
