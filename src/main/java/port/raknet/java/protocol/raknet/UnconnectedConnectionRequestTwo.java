package port.raknet.java.protocol.raknet;

import java.net.InetSocketAddress;

import port.raknet.java.protocol.Packet;

public class UnconnectedConnectionRequestTwo extends Packet {

	public boolean magic;
	public InetSocketAddress clientAddress;
	public short mtuSize;
	public long clientId;

	public UnconnectedConnectionRequestTwo(Packet packet) {
		super(packet);
	}

	public UnconnectedConnectionRequestTwo() {
		super(ID_UNCONNECTED_CONNECTION_REQUEST_2);
	}

	@Override
	public void encode() {
		this.putMagic();
		this.putAddress(clientAddress);
		this.putShort(mtuSize);
		this.putLong(clientId);
	}

	@Override
	public void decode() {
		this.magic = this.checkMagic();
		this.clientAddress = this.getAddress();
		this.mtuSize = this.getShort();
		this.clientId = this.getLong();
	}

}
