package port.raknet.java.protocol.raknet;

import port.raknet.java.protocol.Packet;

public class ConnectionReplyOne extends Packet {

	public boolean magic;
	public long serverId;
	public boolean security;
	public short mtuSize;

	public ConnectionReplyOne(Packet packet) {
		super(packet);
	}

	public ConnectionReplyOne() {
		super(ID_OPEN_CONNECTION_REPLY_1);
	}

	@Override
	public void encode() {
		this.putMagic();
		this.putLong(serverId);
		this.putBoolean(security);
		this.putShort(mtuSize);
	}

	@Override
	public void decode() {
		this.magic = this.checkMagic();
		this.serverId = this.getLong();
		this.security = this.getBoolean();
		this.mtuSize = this.getShort();
	}

}
