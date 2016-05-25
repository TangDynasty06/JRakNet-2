package port.raknet.java.client;

import port.raknet.java.RakNetOptions;
import port.raknet.java.protocol.Reliability;
import port.raknet.java.protocol.raknet.ConnectedPing;
import port.raknet.java.session.ServerSession;
import port.raknet.java.session.SessionState;

/**
 * The tracker for the client, used to make sure the server does not timeout
 *
 * @author Trent Summerlin
 */
public class RakNetClientTask implements Runnable {

	public static final long TICK = 1000L;

	private final RakNetClient client;
	private long pingId = 0L;

	public RakNetClientTask(RakNetClient client) {
		this.client = client;
	}

	@Override
	public void run() {
		ServerSession session = client.getSession();
		if (session != null) {
			RakNetOptions options = client.getOptions();
			session.pushLastReceiveTime(TICK);
			if (session.getLastReceiveTime() / options.timeout == 0.5) {
				ConnectedPing ping = new ConnectedPing();
				ping.pingId = pingId++;
				ping.encode();
				session.sendPacket(Reliability.UNRELIABLE, ping);
			} else if (session.getLastReceiveTime() > options.timeout) {
				if (client.getState() == SessionState.CONNECTED) {
					client.removeSession("Timeout");
				}
			}
		}
	}

}
