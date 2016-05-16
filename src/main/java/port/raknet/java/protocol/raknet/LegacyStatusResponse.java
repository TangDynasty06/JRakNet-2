package port.raknet.java.protocol.raknet;

import port.raknet.java.protocol.Packet;

public class LegacyStatusResponse extends Packet {

	public long pingId;
	public long serverId;
	public boolean magic;
	public String data;

	public LegacyStatusResponse(Packet packet) {
		super(packet);
	}

	public LegacyStatusResponse() {
		super(ID_UNCONNECTED_LEGACY_STATUS_RESPONSE);
	}

	@Override
	public void encode() {
		this.putLong(pingId);
		this.putLong(serverId);
		this.putMagic();
		this.putString(data);
	}

	@Override
	public void decode() {
		this.pingId = this.getLong();
		this.serverId = this.getLong();
		this.magic = this.checkMagic();
		this.data = this.getString();
	}

}
