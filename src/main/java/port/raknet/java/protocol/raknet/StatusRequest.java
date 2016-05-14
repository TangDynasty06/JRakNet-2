package port.raknet.java.protocol.raknet;

import port.raknet.java.protocol.Packet;

public class StatusRequest extends Packet {

	public long pingId;
	public boolean magic;

	public StatusRequest(Packet packet) {
		super(packet);
	}

	public StatusRequest() {
		super(ID_STATUS_REQUEST);
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
