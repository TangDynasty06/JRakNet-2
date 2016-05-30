package port.raknet.java.example.chat.protocol;

import port.raknet.java.protocol.Packet;

public class LoginPacket extends Packet {

	public String username;

	public LoginPacket(Packet packet) {
		super(packet);
	}

	public LoginPacket() {
		super(Info.ID_LOGIN);
	}

	@Override
	public void encode() {
		this.putString(username);
	}

	@Override
	public void decode() {
		this.username = this.getString();
	}

}
