package port.raknet.java;

import java.util.Scanner;

import port.raknet.java.event.Hook;
import port.raknet.java.event.HookRunnable;
import port.raknet.java.protocol.raknet.internal.EncapsulatedPacket;
import port.raknet.java.server.RakNetServer;
import port.raknet.java.session.RakNetSession;

/**
 * Used to test <code>RakNetServer</code>, meant for testing with Minecraft:
 * Pocket Edition clients
 *
 * @author Trent Summerlin
 */
public class RakNetServerTest {

	private static String identifier = "A RakNet Server";

	public static void main(String[] args) throws Exception {
		// Set server options
		RakNetOptions options = new RakNetOptions();
		options.serverPort = 19132;
		options.serverIdentifier = "MCPE;_IDENTIFIER_;70;0.14.3;0;10";
		RakNetServer server = new RakNetServer(options);

		// Client disconnected
		server.addHook(Hook.SESSION_CONNECTED, new HookRunnable() {
			@Override
			public void run(Object... parameters) {
				RakNetSession session = (RakNetSession) parameters[0];
				System.out
						.println("Client from address " + session.getSocketAddress() + " has connected to the server");
			}
		});

		// Client connected
		server.addHook(Hook.PACKET_RECEIVED, new HookRunnable() {
			@Override
			public void run(Object... parameters) {
				RakNetSession session = (RakNetSession) parameters[0];
				EncapsulatedPacket encapsulated = (EncapsulatedPacket) parameters[1];
				System.out.println("Received packet from client with address " + session.getSocketAddress()
						+ " with packet ID: 0x" + Integer.toHexString(encapsulated.payload[0] & 0xFF).toUpperCase());
			}
		});

		// Client disconnected
		server.addHook(Hook.SESSION_DISCONNECTED, new HookRunnable() {
			@Override
			public void run(Object... parameters) {
				RakNetSession session = (RakNetSession) parameters[0];
				String reason = parameters[1].toString();
				System.out.println("Client from address " + session.getSocketAddress()
						+ " has disconnected from the server for the reason \"" + reason + "\"");
			}
		});

		// Server has been pinged
		server.addHook(Hook.SERVER_PING, new HookRunnable() {
			@Override
			public void run(Object... parameters) {
				parameters[1] = parameters[1].toString().replace("_IDENTIFIER_", identifier);
			}
		});

		server.startThreadedServer();

		// Wait for input from console
		@SuppressWarnings("resource")
		Scanner s = new Scanner(System.in);
		System.out.println("Type something in and press enter to change the server name!");
		while (true) {
			if (s.hasNextLine()) {
				identifier = s.nextLine();
				System.out.println("Set server name to: " + identifier);
			}
		}
	}

}
