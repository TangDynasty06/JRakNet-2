package port.raknet.java.protocol.raknet;

import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.SystemAddress;

public class ConnectionOpenReplyTwo extends Packet {

	public boolean magic;
	public long serverId;
	public SystemAddress clientAddress;
	public short mtuSize;
	public byte useSecurity;

	public ConnectionOpenReplyTwo(Packet packet) {
		super(packet);
	}

	public ConnectionOpenReplyTwo() {
		super(ID_UNCONNECTED_OPEN_CONNECTION_REPLY_2);
	}

	@Override
	public void encode() {
		this.putMagic();
		this.putLong(serverId);
		this.putAddress(clientAddress);
		this.putShort(mtuSize);
		this.putByte(useSecurity);
	}

	@Override
	public void decode() {
		this.magic = this.checkMagic();
		this.serverId = this.getLong();
		this.clientAddress = this.getAddress();
		this.mtuSize = this.getShort();
		this.useSecurity = this.getByte();
	}

}
