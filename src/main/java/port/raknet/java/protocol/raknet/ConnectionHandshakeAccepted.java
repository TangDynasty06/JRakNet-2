package port.raknet.java.protocol.raknet;

import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.SystemAddress;

public class ConnectionHandshakeAccepted extends Packet {

	public SystemAddress clientAddress;
	public long requestTime;
	public long time;

	public ConnectionHandshakeAccepted(Packet packet) {
		super(packet);
	}

	public ConnectionHandshakeAccepted() {
		super(ID_CONNECTION_HANDSHAKE_ACCEPTED);
	}

	@Override
	public void encode() {
		this.putAddress(clientAddress);
		this.putShort(0);
		for (int i = 0; i < 10; i++) {
			this.putAddress(new SystemAddress("255.255.255.255", 19132, 4));
		}
		this.putLong(requestTime);
		this.putLong(time);
	}

}
