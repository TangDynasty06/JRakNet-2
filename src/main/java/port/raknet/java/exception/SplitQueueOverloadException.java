package port.raknet.java.exception;

import port.raknet.java.session.RakNetSession;

/**
 * Occurs when the split queue for a <code>RakNetSession</code> is too big
 *
 * @author Trent Summerlin
 */
public class SplitQueueOverloadException extends RakNetException {

	private static final long serialVersionUID = -289422497689147588L;

	private final RakNetSession session;

	public SplitQueueOverloadException(RakNetSession session) {
		super("The split queue is too big!");
		this.session = session;
	}

	/**
	 * Returns the session that caused the error
	 * 
	 * @return RakNetSession
	 */
	public RakNetSession getSession() {
		return this.session;
	}

}
