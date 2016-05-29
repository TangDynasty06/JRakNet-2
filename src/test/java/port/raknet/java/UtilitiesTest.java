package port.raknet.java;

import static port.raknet.java.utils.RakNetUtils.getServerIdentifier;
import static port.raknet.java.utils.RakNetUtils.getSubnetMask;
import static port.raknet.java.utils.RakNetUtils.isServerCompatible;

/**
 * Used to test <code>RakNetUtils</code>, meant for testing with Minecraft:
 * Pocket Edition servers and clients
 *
 * @author Trent Summerlin
 */
public class UtilitiesTest {

	private static final String SERVER_ADDRESS = "sg.lbsg.net";
	private static final int SERVER_PORT = 19132;

	public static void main(String[] args) throws Exception {
		System.out.println("Server name: " + getServerIdentifier(SERVER_ADDRESS, SERVER_PORT));
		System.out.println("Server compatible?: " + isServerCompatible(SERVER_ADDRESS, SERVER_PORT));
		System.out.println("Local machine subnet mask: " + getSubnetMask());
	}

}
