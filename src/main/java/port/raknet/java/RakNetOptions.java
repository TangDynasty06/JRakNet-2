package port.raknet.java;

/**
 * Used for setting the options for <code>RakNetServer</code> and
 * <code>RakNetClient</code>
 *
 * @author Trent Summerlin
 */
public class RakNetOptions {

	public RakNetOptions() {
	}

	public RakNetOptions(int serverPort, String serverIdentifier) {
		this.serverPort = serverPort;
		this.serverIdentifier = serverIdentifier;
	}

	public RakNetOptions(int broadcastPort) {
		this.broadcastPort = broadcastPort;
	}

	/**
	 * The port the server runs on
	 */
	public int serverPort = 19132;

	/**
	 * The identifier the server broadcasts when receiving a unconnected ping
	 */
	public String serverIdentifier = "";

	/**
	 * The port the client will use to find other servers on the network
	 */
	public int broadcastPort = this.serverPort;

	/**
	 * The maximum amount of data <code>RakNetServer</code> will allow for a
	 * client, this is what <code>RakNetClient</code> will start at as it
	 * gradually decreases until a response is received
	 */
	public int maximumTransferUnit = 2048;

	/**
	 * How long until a session is disconnected from the server due to
	 * inactivity, it is suggested be at least 10,000 MS (10 Seconds)
	 */
	public long timeout = 10000L;

}
