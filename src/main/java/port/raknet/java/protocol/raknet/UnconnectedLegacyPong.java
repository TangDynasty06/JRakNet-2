package port.raknet.java.protocol.raknet;

import port.raknet.java.protocol.Packet;

public class UnconnectedLegacyPong extends Packet {

	public long pingId;
	public long serverId;
	public boolean magic;
	public String data;

	public UnconnectedLegacyPong(Packet packet) {
		super(packet);
	}

	public UnconnectedLegacyPong() {
		super(ID_UNCONNECTED_LEGACY_PONG);
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
