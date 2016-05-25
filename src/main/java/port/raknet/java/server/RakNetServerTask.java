package port.raknet.java.server;

import port.raknet.java.RakNetOptions;
import port.raknet.java.protocol.Reliability;
import port.raknet.java.protocol.raknet.ConnectedPing;
import port.raknet.java.session.ClientSession;
import port.raknet.java.session.SessionState;

/**
 * The tracker for the server, used to make sure clients do not timeout
 *
 * @author Trent Summerlin
 */
public class RakNetServerTask implements Runnable {
	
	public static final long TICK = 1000L;
	
	private final RakNetServer server;
	private final RakNetServerHandler handler;
	private long pingId;

	public RakNetServerTask(RakNetServer server, RakNetServerHandler handler) {
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
				if (session.getState() == SessionState.CONNECTED) {
					handler.removeSession(session, "Timeout");
				}
			}
		}
	}

}
