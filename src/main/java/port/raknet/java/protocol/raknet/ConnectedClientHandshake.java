package port.raknet.java.protocol.raknet;

import java.net.InetSocketAddress;

import port.raknet.java.protocol.Packet;

public class ConnectedClientHandshake extends Packet {

	public InetSocketAddress clientAddress;
	public long serverTimestamp;
	public long timestamp;

	public ConnectedClientHandshake(Packet packet) {
		super(packet);
	}

	public ConnectedClientHandshake() {
		super(ID_CONNECTED_CLIENT_HANDSHAKE);
	}

	@Override
	public void encode() {
		this.putAddress(clientAddress);
		for (int i = 0; i < 10; i++) {
			this.putAddress("255.255.255.255", 19132);
		}
		this.putLong(serverTimestamp);
		this.putLong(timestamp);
	}

	@Override
	public void decode() {
		this.clientAddress = this.getAddress();
		for (int i = 0; i < 10; i++) {
			this.getAddress();
		}
		this.serverTimestamp = this.getLong();
		this.timestamp = this.getLong();
	}

}
