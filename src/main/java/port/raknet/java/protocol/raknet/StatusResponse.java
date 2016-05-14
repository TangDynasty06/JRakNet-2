package port.raknet.java.protocol.raknet;

import port.raknet.java.protocol.Packet;

public class StatusResponse extends Packet {

	public long pingId;
	public long serverId;
	public boolean magic;
	public String identifier;

	public StatusResponse(Packet packet) {
		super(packet);
	}

	public StatusResponse() {
		super(ID_STATUS_RESPONSE);
	}

	@Override
	public void encode() {
		this.putLong(pingId);
		this.putLong(serverId);
		this.putMagic();
		this.putString(identifier);
	}

	@Override
	public void decode() {
		this.pingId = this.getLong();
		this.serverId = this.getLong();
		this.magic = this.checkMagic();
		this.identifier = this.getString();
	}

}
