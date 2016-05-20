package port.raknet.java.client.task;

import port.raknet.java.RakNetOptions;
import port.raknet.java.client.RakNetClient;
import port.raknet.java.client.RakNetClientHandler;
import port.raknet.java.protocol.raknet.ConnectedPing;
import port.raknet.java.scheduler.RakNetScheduler;
import port.raknet.java.session.ServerSession;
import port.raknet.java.session.SessionState;

public class ClientTask implements Runnable {
	
	private final RakNetClient client;
	private final RakNetClientHandler handler;
	private long pingId = 0L;
	
	public ClientTask(RakNetClient client, RakNetClientHandler handler) {
		this.client = client;
		this.handler = handler;
	}

	// Update server and client data
	public void run() {
		ServerSession session = handler.getSession();
		if (session != null) {
			RakNetOptions options = client.getOptions();
			session.pushLastReceiveTime(RakNetScheduler.TICK);
			if (session.getLastReceiveTime() / options.timeout == 0.5) {
				// Ping ID's do not need to match
				ConnectedPing ping = new ConnectedPing();
				ping.pingId = pingId++;
				ping.encode();
				session.sendPacket(ping);
			} else if (session.getLastReceiveTime() > options.timeout) {
				if (client.getState() == SessionState.CONNECTED) {
					handler.removeSession("Timeout");
				}
			} else {
				session.resendACK();
			}
		}
	}

}
