package port.raknet.java.protocol;

/**
 * Contains all the reliability types for RakNet.
 * 
 * @author Trent Summerlin
 */
public enum Reliability {

	UNRELIABLE(0, false, false, false), UNRELIABLE_SEQUENCED(1, false, false, true), RELIABLE(2, true, false,
			false), RELIABLE_ORDERED(3, true, true, false), RELIABLE_SEQUENCED(4, true, false,
					true), UNRELIABLE_WITH_ACK_RECEIPT(5, false, false, false), RELIABLE_WITH_ACK_RECEIPT(6, true,
							false, false), RELIABLE_ORDERED_WITH_ACK_RECEIPT(7, true, true, false);

	private final byte reliability;
	private final boolean reliable;
	private final boolean ordered;
	private final boolean sequenced;

	private Reliability(int reliability, boolean reliable, boolean ordered, boolean sequenced) {
		this.reliability = (byte) reliability;
		this.reliable = reliable;
		this.ordered = ordered;
		this.sequenced = sequenced;
	}

	public byte asByte() {
		return this.reliability;
	}

	public boolean isReliable() {
		return this.reliable;
	}

	public boolean isOrdered() {
		return this.ordered;
	}

	public boolean isSequenced() {
		return this.sequenced;
	}

	public static Reliability lookup(byte reliability) {
		Reliability[] reliabilities = Reliability.values();
		for (Reliability sReliability : reliabilities) {
			if (sReliability.asByte() == reliability) {
				return sReliability;
			}
		}
		return null;
	}

}
