package port.raknet.java.protocol.raknet;

import port.raknet.java.protocol.Packet;

public class UnconnectedConnectionRequestOne extends Packet {

	public boolean magic;
	public short protocol;
	public short mtuSize;

	public UnconnectedConnectionRequestOne(Packet packet) {
		super(packet);
	}

	public UnconnectedConnectionRequestOne() {
		super(ID_UNCONNECTED_CONNECTION_REQUEST_1);
	}

	@Override
	public void encode() {
		this.putMagic();
		this.putUByte(protocol);
		this.pad(mtuSize);
	}

	@Override
	public void decode() {
		this.magic = this.checkMagic();
		this.protocol = this.getUByte();
		this.mtuSize = (short) this.get(this.remaining()).length;
	}

}