package port.raknet.java.protocol.raknet;

import port.raknet.java.protocol.Packet;

public class ConnectionHandshakeRequest extends Packet {
	
	public long serverId;
	public long requestTime;

	public ConnectionHandshakeRequest(Packet packet) {
		super(packet);
	}

	public ConnectionHandshakeRequest() {
		super(ID_CONNECTION_HANDSHAKE_REQUEST);
	}

	@Override
	public void decode() {
		this.serverId = this.getLong();
		this.requestTime = this.getLong();
	}

}
