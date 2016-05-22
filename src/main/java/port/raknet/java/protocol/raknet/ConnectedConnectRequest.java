package port.raknet.java.protocol.raknet;

import port.raknet.java.protocol.Packet;

public class ConnectedConnectRequest extends Packet {

	public long clientId;
	public long timestamp;

	public ConnectedConnectRequest(Packet packet) {
		super(packet);
	}

	public ConnectedConnectRequest() {
		super(ID_CONNECTED_CLIENT_CONNECT_REQUEST);
	}

	@Override
	public void encode() {
		this.putLong(clientId);
		this.putLong(timestamp);
		this.putBoolean(false);
	}

	@Override
	public void decode() {
		this.clientId = this.getLong();
		this.timestamp = this.getLong();
		this.getBoolean(); // We never use security
	}

}
