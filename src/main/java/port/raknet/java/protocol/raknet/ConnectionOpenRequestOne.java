package port.raknet.java.protocol.raknet;

import port.raknet.java.protocol.Packet;

public class ConnectionOpenRequestOne extends Packet {

	public boolean magic;
	public short protocol;
	public short mtuSize;

	public ConnectionOpenRequestOne(Packet packet) {
		super(packet);
	}

	public ConnectionOpenRequestOne() {
		super(ID_OPEN_CONNECTION_REQUEST_1);
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