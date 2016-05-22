package port.raknet.java.protocol.raknet;

import port.raknet.java.protocol.Packet;

public class UnconnectedConnectionReplyOne extends Packet {

	public boolean magic;
	public long serverId;
	public short mtuSize;

	public UnconnectedConnectionReplyOne(Packet packet) {
		super(packet);
	}

	public UnconnectedConnectionReplyOne() {
		super(ID_UNCONNECTED_CONNECTION_REPLY_1);
	}

	@Override
	public void encode() {
		this.putMagic();
		this.putLong(serverId);
		this.putBoolean(false);
		this.putShort(mtuSize);
	}

	@Override
	public void decode() {
		this.magic = this.checkMagic();
		this.serverId = this.getLong();
		this.getBoolean(); // We never use security
		this.mtuSize = this.getShort();
	}

}
