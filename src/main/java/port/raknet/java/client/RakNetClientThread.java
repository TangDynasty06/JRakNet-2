package port.raknet.java.client;

/**
 * Used to start the client on it's own thread
 *
 * @author Trent Summerlin
 */
public class RakNetClientThread extends Thread {

	private final RakNetClient client;

	public RakNetClientThread(RakNetClient client) {
		this.client = client;
	}

	@Override
	public void run() {
		client.startClient();
	}
	
}
