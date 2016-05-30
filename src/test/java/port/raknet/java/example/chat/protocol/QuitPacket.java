package port.raknet.java.example.chat.protocol;

import port.raknet.java.protocol.Packet;

public class QuitPacket extends Packet {

	public QuitPacket(Packet packet) {
		super(packet);
	}
	
	public QuitPacket() {
		super(Info.ID_QUIT);
	}

}
