package port.raknet.java.protocol.raknet;

import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.SystemAddress;

public class ServerHandshake extends Packet {

	public SystemAddress clientAddress;
	public long sendPing;
	public long sendPong;

	public ServerHandshake(Packet packet) {
		super(packet);
	}

	public ServerHandshake() {
		super(ID_SERVER_HANDSHAKE);
	}

	@Override
	public void encode() {
		this.putAddress(clientAddress);
		this.putShort(0);
		for (int i = 0; i < 10; i++) {
			this.putAddress(new SystemAddress("255.255.255.255", 19132, 4));
		}
		this.putLong(sendPing);
		this.putLong(sendPong);
	}

	@Override
	public void decode() {
		this.clientAddress = this.getAddress();
		this.getShort(); // Unknown use
		for (int i = 0; i < 10; i++) {
			this.getAddress(); // Unknown use
		}
		this.sendPing = this.getLong();
		this.sendPong = this.getLong();
	}

}
