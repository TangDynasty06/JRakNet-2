package port.raknet.java.client;

import java.net.InetSocketAddress;

import port.raknet.java.exception.RakNetException;

/**
 * Starts a <code>RakNetClient</code> on it's own thread
 *
 * @author Trent Summerlin
 */
public class RakNetClientThread extends Thread {

	private final RakNetClient client;
	private final InetSocketAddress address;

	public RakNetClientThread(RakNetClient client, InetSocketAddress address) {
		this.client = client;
		this.address = address;
	}

	@Override
	public void run() {
		try {
			client.connect(address);
		} catch (RakNetException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

}
