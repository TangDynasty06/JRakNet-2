package port.raknet.java;

import port.raknet.java.event.Hook;
import port.raknet.java.event.HookRunnable;
import port.raknet.java.protocol.raknet.internal.EncapsulatedPacket;
import port.raknet.java.server.RakNetServer;
import port.raknet.java.session.RakNetSession;

/**
 * Used to test the server class, meant for testing with Minecraft: Pocket
 * Edition clients
 *
 * @author Trent Summerlin
 */
public class RakNetServerTest {

	public static void main(String[] args) {
		// Set server options
		RakNetOptions options = new RakNetOptions();
		options.serverPort = 19132;
		options.serverIdentifier = "MCPE;A RakNetServer;70;0.14.3;0;10";

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

		server.startServer();
	}

}
