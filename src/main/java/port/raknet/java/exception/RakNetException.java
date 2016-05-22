package port.raknet.java.exception;

/**
 * Represents a error in JRakNet
 *
 * @author Trent Summerlin
 */
public class RakNetException extends Exception {

	private static final long serialVersionUID = 6137150061303840459L;

	public RakNetException(String reason) {
		super(reason);
	}

}
