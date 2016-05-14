package port.raknet.java.protocol.raknet;

import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.SystemAddress;

public class ConnectionOpenRequestTwo extends Packet {

	public boolean magic;
	public SystemAddress address;
	public short mtuSize;
	public long clientId;

	public ConnectionOpenRequestTwo(Packet packet) {
		super(packet);
	}

	public ConnectionOpenRequestTwo() {
		super(ID_OPEN_CONNECTION_REQUEST_2);
	}

	@Override
	public void encode() {
		this.putMagic();
		this.putAddress(address);
		this.putShort(mtuSize);
		this.putLong(clientId);
	}

	@Override
	public void decode() {
		this.magic = this.checkMagic();
		this.address = this.getAddress();
		this.mtuSize = this.getShort();
		this.clientId = this.getLong();
	}

}
