package port.raknet.java.server;

import port.raknet.java.exception.RakNetException;

/**
 * Starts the server on it's own thread
 *
 * @author Trent Summerlin
 */
public class RakNetServerThread extends Thread {

	private final RakNetServer server;

	public RakNetServerThread(RakNetServer server) {
		this.server = server;
	}

	@Override
	public void run() {
		try {
			server.startServer();
		} catch (RakNetException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

}
