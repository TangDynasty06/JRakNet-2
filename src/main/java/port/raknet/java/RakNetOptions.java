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
	 * How long until a client is disconnected from the server due to
	 * inactivity, it is suggested be at least 10,000 MS (10 Seconds)
	 */
	public long timeout = 10000L;

}
