package port.raknet.java.protocol.raknet;

import port.raknet.java.protocol.Packet;

public class UnconnectedPong extends Packet {

	public long pingId;
	public long serverId;
	public boolean magic;
	public String identifier;

	public UnconnectedPong(Packet packet) {
		super(packet);
	}

	public UnconnectedPong() {
		super(ID_UNCONNECTED_PONG);
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
