package port.raknet.java.protocol.raknet;

import port.raknet.java.protocol.Packet;

public class IncompatibleProtocolVersion extends Packet {

	public byte version;
	public boolean magic;
	public long serverId;

	public IncompatibleProtocolVersion(Packet packet) {
		super(packet);
	}

	public IncompatibleProtocolVersion() {
		super(ID_INCOMPATIBLE_PROTOCOL_VERSION);
	}

	@Override
	public void encode() {
		this.putByte(version);
		this.putMagic();
		this.putLong(serverId);
	}

	@Override
	public void decode() {
		this.version = this.getByte();
		this.magic = this.checkMagic();
		this.serverId = this.getLong();
	}

}
