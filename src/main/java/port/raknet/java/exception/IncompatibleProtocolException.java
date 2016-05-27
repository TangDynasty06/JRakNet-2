package port.raknet.java.exception;

/**
 * Occurs whenever the server sends an
 * <code>ID_UNCONNECTED_INCOMPATIBLE_PROTOCOL</code> packet
 *
 * @author Trent Summerlin
 */
public class IncompatibleProtocolException extends RakNetException {

	private static final long serialVersionUID = 3820073523553233311L;

	public IncompatibleProtocolException(int serverProtocol, int clientProtocol) {
		super(createErrorMessage(serverProtocol, clientProtocol));
	}

	private static String createErrorMessage(int serverProtocol, int clientProtocol) {
		if (serverProtocol > clientProtocol) {
			return "Client protocol is " + (serverProtocol - clientProtocol) + " versions behind!";
		} else if (clientProtocol > serverProtocol) {
			return "Server protocol is " + (clientProtocol - serverProtocol) + " versions behind!";
		}
		return "Unknown protocol error!";
	}

	@Override
	public String getLocalizedMessage() {
		return "Protocols do not match!";
	}

}
