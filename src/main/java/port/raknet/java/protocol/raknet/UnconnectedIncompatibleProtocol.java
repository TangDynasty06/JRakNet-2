package port.raknet.java.protocol.raknet;

import port.raknet.java.protocol.Packet;

public class UnconnectedIncompatibleProtocol extends Packet {

	public short protocol;
	public boolean magic;
	public long serverId;

	public UnconnectedIncompatibleProtocol(Packet packet) {
		super(packet);
	}

	public UnconnectedIncompatibleProtocol() {
		super(ID_UNCONNECTED_INCOMPATIBLE_PROTOCOL);
	}

	@Override
	public void encode() {
		this.putUByte(protocol);
		this.putMagic();
		this.putLong(serverId);
	}

	@Override
	public void decode() {
		this.protocol = this.getUByte();
		this.magic = this.checkMagic();
		this.serverId = this.getLong();
	}

}
