package port.raknet.java;

public class RakNetOptions {

	/**
	 * The port the server runs on
	 */
	public int port = 19132;

	/**
	 * The maximum amount of data the server can send and receive
	 */
	public int maximumTransferSize = 2048;

	/**
	 * The server's broadcast name
	 */
	public String broadcastName = "";

	/**
	 * How long the server will wait before updating tracker
	 */
	public long trackerWait = 100L;

	/**
	 * How long until a client is disconnected from the server due to inactivity
	 */
	public long timeout = 5000L;

}
