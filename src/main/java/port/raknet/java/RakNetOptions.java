package port.raknet.java;

public class RakNetOptions {

	/**
	 * The port the server runs on
	 */
	public int port = 19132;

	/**
	 * The port the client will use to find other servers on the network
	 */
	public int broadcastPort = 19132;

	/**
	 * The maximum amount of data the session can send and receive
	 */
	public int maximumTransferSize = 2048;

	/**
	 * The broadcast name
	 */
	public String broadcastName = "";

	/**
	 * How long until a session is disconnected from the server due to
	 * inactivity, it is suggested be at least 10,000 MS (10 Seconds)
	 */
	public long timeout = 10000L;

}
