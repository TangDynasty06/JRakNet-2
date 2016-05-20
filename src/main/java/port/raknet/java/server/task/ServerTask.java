package port.raknet.java.server.task;

import port.raknet.java.RakNetOptions;
import port.raknet.java.protocol.raknet.ConnectedPing;
import port.raknet.java.scheduler.RakNetScheduler;
import port.raknet.java.server.RakNetServer;
import port.raknet.java.server.RakNetServerHandler;
import port.raknet.java.session.ClientSession;
import port.raknet.java.session.SessionState;

public class ServerTask implements Runnable {

	private final RakNetServer server;
	private final RakNetServerHandler handler;
	private long pingId;

	public ServerTask(RakNetServer server, RakNetServerHandler handler) {
		this.server = server;
		this.handler = handler;
	}

	@Override
	public void run() {
		RakNetOptions options = server.getOptions();
		for (ClientSession session : handler.getSessions()) {
			session.pushLastReceiveTime(RakNetScheduler.TICK);
			if (session.getLastReceiveTime() / options.timeout == 0.5) {
				// Ping ID's do not need to match
				ConnectedPing ping = new ConnectedPing();
				ping.pingId = pingId++;
				ping.encode();
				session.sendPacket(ping);
			}
			if (session.getLastReceiveTime() > options.timeout) {
				if (session.getState() == SessionState.CONNECTED) {
					handler.removeSession(session, "Timeout");
				}
			} else {
				session.resendACK();
			}
		}
	}

}
