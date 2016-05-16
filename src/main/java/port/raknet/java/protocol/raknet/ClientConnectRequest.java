package port.raknet.java.protocol.raknet;

import port.raknet.java.protocol.Packet;

public class ClientConnectRequest extends Packet {

	public long serverId;
	public long sendPing;
	public boolean useSecurity = false;

	public ClientConnectRequest(Packet packet) {
		super(packet);
	}

	public ClientConnectRequest() {
		super(ID_CONNECTED_CLIENT_CONNECT_REQUEST);
	}

	@Override
	public void encode() {
		this.putLong(serverId);
		this.putLong(sendPing);
		this.putBoolean(useSecurity);
	}

	@Override
	public void decode() {
		this.serverId = this.getLong();
		this.sendPing = this.getLong();
		this.useSecurity = this.getBoolean();
	}

}
