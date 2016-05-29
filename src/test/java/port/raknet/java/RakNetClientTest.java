package port.raknet.java;

import port.raknet.java.client.RakNetClient;
import port.raknet.java.event.Hook;
import port.raknet.java.event.HookRunnable;
import port.raknet.java.protocol.raknet.internal.EncapsulatedPacket;
import port.raknet.java.session.RakNetSession;

/**
 * Used to test <code>RakNetClient</code>, meant for testing with Minecraft:
 * Pocket Edition servers
 *
 * @author Trent Summerlin
 */
public class RakNetClientTest {

	private static final String SERVER_ADDRESS = "sg.lbsg.net";
	private static final int SERVER_PORT = 19132;

	public static void main(String[] args) throws Exception {
		RakNetClient client = new RakNetClient(new RakNetOptions());

		// Client disconnected
		client.addHook(Hook.SESSION_CONNECTED, new HookRunnable() {
			@Override
			public void run(Object... parameters) {
				RakNetSession session = (RakNetSession) parameters[0];
				System.out.println("Client has connected to server with address " + session.getSocketAddress());
			}
		});

		// Client connected
		client.addHook(Hook.PACKET_RECEIVED, new HookRunnable() {
			@Override
			public void run(Object... parameters) {
				RakNetSession session = (RakNetSession) parameters[0];
				EncapsulatedPacket encapsulated = (EncapsulatedPacket) parameters[1];
				System.out.println("Received packet from server " + session.getSocketAddress() + " with packet ID: 0x"
						+ Integer.toHexString(encapsulated.payload[0] & 0xFF).toUpperCase());
			}
		});

		// Client disconnected
		client.addHook(Hook.SESSION_DISCONNECTED, new HookRunnable() {
			@Override
			public void run(Object... parameters) {
				RakNetSession session = (RakNetSession) parameters[0];
				String reason = parameters[1].toString();
				System.out.println("Server with address " + session.getSocketAddress()
						+ " has been disconnected for the reason \"" + reason + "\"");
			}
		});

		client.connect(SERVER_ADDRESS, SERVER_PORT);
	}

}
