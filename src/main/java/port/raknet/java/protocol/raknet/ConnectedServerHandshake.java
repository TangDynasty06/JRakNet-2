package port.raknet.java.protocol.raknet;

import java.net.InetSocketAddress;

import port.raknet.java.protocol.Packet;

public class ConnectedServerHandshake extends Packet {

	public InetSocketAddress clientAddress;
	public long clientId;
	public long timestamp;

	public ConnectedServerHandshake(Packet packet) {
		super(packet);
	}

	public ConnectedServerHandshake() {
		super(ID_CONNECTED_SERVER_HANDSHAKE);
	}

	@Override
	public void encode() {
		this.putAddress(clientAddress);
		this.putShort(0);
		for (int i = 0; i < 10; i++) {
			this.putAddress("255.255.255.255", 19132);
		}
		this.putLong(clientId);
		this.putLong(timestamp);
	}

	@Override
	public void decode() {
		this.clientAddress = this.getAddress();
		this.getShort(); // Unknown use
		for (int i = 0; i < 10; i++) {
			this.getAddress(); // Unknown use
		}
		this.clientId = this.getLong();
		this.timestamp = this.getLong();
	}

}
