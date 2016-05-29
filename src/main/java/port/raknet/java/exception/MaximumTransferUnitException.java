package port.raknet.java.exception;

/**
 * Occurs when the MTU size goes below the minimum MTU for RakNet
 *
 * @author Trent Summerlin
 */
public class MaximumTransferUnitException extends RakNetException {

	private static final long serialVersionUID = -6040478416974497890L;

	public MaximumTransferUnitException() {
		super("MTU size is too small!");
	}

}
