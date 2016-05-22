package port.raknet.java.session;

/**
 * Used to track the session of state in order to make sure packets are
 * handled at the right time
 *
 * @author Trent Summerlin
 */
public enum SessionState {

	DISCONNECTED(0), CONNECTING_1(1), CONNECTING_2(2), HANDSHAKING(3), CONNECTED(4);

	private final int order;

	private SessionState(int order) {
		this.order = order;
	}

	/**
	 * Get's the order the state is in as a integer value
	 * 
	 * @return int
	 */
	public int getOrder() {
		return this.order;
	}

	/**
	 * Gets the state by it's numerical order
	 * 
	 * @param order
	 * @return SessionState
	 */
	public static SessionState getState(int order) {
		for (SessionState state : SessionState.values()) {
			if (state.getOrder() == order) {
				return state;
			}
		}
		return null;
	}

}
