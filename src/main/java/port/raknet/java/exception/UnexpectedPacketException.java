package port.raknet.java.exception;

import port.raknet.java.RakNet;

/**
 * Thrown when a handler is expecting a packet and receives something else
 * instead
 *
 * @author Trent Summerlin
 */
public class UnexpectedPacketException extends RakNetException {

	private static final long serialVersionUID = -3793043367215871424L;

	public UnexpectedPacketException(int requiredId, int retrievedId) {
		super("Packet must be " + RakNet.getName(requiredId) + " but instead got a "
				+ (RakNet.getName(retrievedId) != null ? RakNet.getName(retrievedId) : "unknown packet") + "!");
	}

	@Override
	public String getLocalizedMessage() {
		return "Packet has wrong ID";
	}

}
