package port.raknet.java.protocol.raknet;

import java.net.InetSocketAddress;

import port.raknet.java.protocol.Packet;

public class UnconnectedConnectionReplyTwo extends Packet {

	public boolean magic;
	public long serverId;
	public InetSocketAddress clientAddress;
	public short mtuSize;

	public UnconnectedConnectionReplyTwo(Packet packet) {
		super(packet);
	}

	public UnconnectedConnectionReplyTwo() {
		super(ID_UNCONNECTED_CONNECTION_REPLY_2);
	}

	@Override
	public void encode() {
		this.putMagic();
		this.putLong(serverId);
		this.putAddress(clientAddress);
		this.putShort(mtuSize);
		this.putBoolean(false);
	}

	@Override
	public void decode() {
		this.magic = this.checkMagic();
		this.serverId = this.getLong();
		this.clientAddress = this.getAddress();
		this.mtuSize = this.getShort();
		this.getBoolean(); // We never use security
	}

}
