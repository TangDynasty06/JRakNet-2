package port.raknet.java.client;

import port.raknet.java.RakNet;
import port.raknet.java.protocol.raknet.UnconnectedConnectionRequestOne;
import port.raknet.java.session.ServerSession;

/**
 * Used by the <code>RakNetClient</code> to resend packets until a response is
 * received, only used for the unconnected stage of the login sequence
 *
 * @author Trent Summerlin
 */
public class PacketTask implements Runnable {

	public static final long TICK = 1000L;

	private short mtuSize;
	private final RakNetClient client;

	public PacketTask(RakNetClient client) {
		this.client = client;
		this.mtuSize = (short) client.getOptions().maximumTransferSize;
	}

	@Override
	public void run() {
		ServerSession session = client.getSession();
		if (session != null) {
			UnconnectedConnectionRequestOne ucro = new UnconnectedConnectionRequestOne();
			ucro.mtuSize = this.mtuSize;
			ucro.protocol = RakNet.NETWORK_PROTOCOL;
			ucro.encode();

			session.sendRaw(ucro);
			this.mtuSize -= 100;
			System.out.println("MTU SIZE: " + ucro.mtuSize);
		}
	}

}
