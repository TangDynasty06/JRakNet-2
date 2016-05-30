package port.raknet.java.example.chat.protocol;

import port.raknet.java.protocol.Packet;

public class ChatPacket extends Packet {

	public String message;

	public ChatPacket(Packet packet) {
		super(packet);
	}

	public ChatPacket() {
		super(Info.ID_CHAT);
	}

	@Override
	public void encode() {
		this.putString(message);
	}

	@Override
	public void decode() {
		this.message = this.getString();
	}

}
