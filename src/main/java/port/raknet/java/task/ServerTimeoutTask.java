package port.raknet.java.task;

import port.raknet.java.RakNetOptions;
import port.raknet.java.client.RakNetClient;
import port.raknet.java.protocol.Reliability;
import port.raknet.java.protocol.raknet.ConnectedPing;
import port.raknet.java.session.ServerSession;

/**
 * Used by <code>RakNetClient</code> to make sure the server does not timeout
 *
 * @author Trent Summerlin
 */
public class ServerTimeoutTask implements Runnable {

	public static final long TICK = 1000L;

	private final RakNetClient client;
	private long pingId;

	public ServerTimeoutTask(RakNetClient client) {
		this.client = client;
	}

	@Override
	public void run() {
		RakNetOptions options = client.getOptions();
		ServerSession session = client.getSession();
		if (session != null) {
			session.pushLastReceiveTime(TICK);
			if (session.getLastReceiveTime() / options.timeout == 0.5) {
				// Ping ID's do not need to match
				ConnectedPing ping = new ConnectedPing();
				ping.pingId = pingId++;
				ping.encode();
				session.sendPacket(Reliability.UNRELIABLE, ping);
			}
			if (session.getLastReceiveTime() >= options.timeout) {
				client.removeSession("Timeout");
			}
		}
	}

}
