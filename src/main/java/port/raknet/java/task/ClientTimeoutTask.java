package port.raknet.java.task;

import port.raknet.java.RakNetOptions;
import port.raknet.java.protocol.Reliability;
import port.raknet.java.protocol.raknet.ConnectedPing;
import port.raknet.java.server.RakNetServer;
import port.raknet.java.server.RakNetServerHandler;
import port.raknet.java.session.ClientSession;

/**
 * Used by <code>RakNetServer</code> to make sure clients do not timeout
 *
 * @author Trent Summerlin
 */
public class ClientTimeoutTask implements Runnable {

	public static final long TICK = 1000L;

	private final RakNetServer server;
	private final RakNetServerHandler handler;
	private long pingId;

	public ClientTimeoutTask(RakNetServer server, RakNetServerHandler handler) {
		this.server = server;
		this.handler = handler;
	}

	@Override
	public void run() {
		RakNetOptions options = server.getOptions();
		for (ClientSession session : handler.getSessions()) {
			session.pushLastReceiveTime(TICK);
			if (session.getLastReceiveTime() / options.timeout == 0.5) {
				// Ping ID's do not need to match
				ConnectedPing ping = new ConnectedPing();
				ping.pingId = pingId++;
				ping.encode();
				session.sendPacket(Reliability.UNRELIABLE, ping);
			}
			if (session.getLastReceiveTime() >= options.timeout) {
				handler.removeSession(session, "Timeout");
			}
		}
	}

}
