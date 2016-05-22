package port.raknet.java;

/**
 * Used for setting the options for <code>RakNetServer</code> and
 * <code>RakNetClient</code>
 *
 * @author Trent Summerlin
 */
public class RakNetOptions {

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
	 * The maximum amount of data the session can send and receive
	 */
	public int maximumTransferSize = 2048;

	/**
	 * How long until a session is disconnected from the server due to
	 * inactivity, it is suggested be at least 10,000 MS (10 Seconds)
	 */
	public long timeout = 10000L;

}
