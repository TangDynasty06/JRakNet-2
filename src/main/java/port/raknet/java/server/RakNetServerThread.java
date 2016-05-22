package port.raknet.java.server;

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
		server.startServer();
	}

}
