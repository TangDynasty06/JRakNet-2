package port.raknet.java;

import port.raknet.java.client.RakNetClient;

/**
 * Used to test the client class, meant for testing with Minecraft: Pocket
 * Edition client
 *
 * @author Trent Summerlin
 */
public class RakNetClientTest {
	
	public static void main(String[] args) {
		// Set options
		RakNetOptions options = new RakNetOptions();
		options.broadcastPort = 19132;

		// Create client
		RakNetClient client = new RakNetClient(options);
		client.startThreadedClient();
		client.connect("localhost", 19132);
	}

}
