package port.raknet.java.example.chat.protocol;

import port.raknet.java.protocol.Packet;

public class KickPacket extends Packet {

	public String reason;

	public KickPacket(Packet packet) {
		super(packet);
	}

	public KickPacket() {
		super(Info.ID_KICK);
	}

	@Override
	public void encode() {
		this.putString(reason);
	}

	@Override
	public void decode() {
		this.reason = this.getString();
	}

}
