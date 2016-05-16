package port.raknet.java.protocol.raknet;

import port.raknet.java.protocol.Packet;

public class LegacyStatusRequest extends Packet {

	public long pingId;
	public boolean magic;

	public LegacyStatusRequest(Packet packet) {
		super(packet);
	}

	public LegacyStatusRequest() {
		super(ID_UNCONNECTED_LEGACY_STATUS_REQUEST);
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
