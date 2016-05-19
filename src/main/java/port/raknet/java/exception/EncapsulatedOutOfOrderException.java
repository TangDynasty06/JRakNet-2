package port.raknet.java.exception;

/**
 * Thrown when a <code>ORDERED</code> encapsulated packet is sent out of order
 *
 * @author Trent Summerlin
 */
public class EncapsulatedOutOfOrderException extends RakNetException {

	private static final long serialVersionUID = 837025622677858102L;

	public EncapsulatedOutOfOrderException(int orderIndex, int lastOrderIndex) {
		super("EncapsulatedPacket orderIndex should 1 greater than lastOrderIndex but is "
				+ (orderIndex - lastOrderIndex) + " greater!");
	}

	@Override
	public String getLocalizedMessage() {
		return "Packet out of order";
	}

}
