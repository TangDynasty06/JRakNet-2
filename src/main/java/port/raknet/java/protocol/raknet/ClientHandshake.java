package port.raknet.java.protocol.raknet;

import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.SystemAddress;

public class ClientHandshake extends Packet {

	public SystemAddress address;
	public long sendPing;
	public long sendPong;

	public ClientHandshake(Packet packet) {
		super(packet);
	}

	public ClientHandshake() {
		super(ID_CONNECTED_CLIENT_HANDSHAKE);
	}

	@Override
	public void encode() {
		this.putAddress(address);
		for (int i = 0; i < 10; i++) {
			this.putAddress(new SystemAddress("255.255.255.255", 19132, 4));
		}
		this.putLong(sendPing);
		this.putLong(sendPong);
	}

	@Override
	public void decode() {
		this.address = this.getAddress();
		for (int i = 0; i < 10; i++) {
			this.getAddress();
		}
		this.sendPing = this.getLong();
		this.sendPong = this.getLong();
	}

}
