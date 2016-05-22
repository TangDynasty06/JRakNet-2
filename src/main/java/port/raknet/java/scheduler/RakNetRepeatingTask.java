package port.raknet.java.scheduler;

/**
 * A repeating task which continuously executes at the specified times until it
 * is removed manually
 *
 * @author Trent Summerlin
 */
public class RakNetRepeatingTask {

	public final Runnable runnable;
	public long waitTime;
	public final long reset;

	public RakNetRepeatingTask(Runnable runnable, long waitTime) {
		this.runnable = runnable;
		this.waitTime = waitTime;
		this.reset = waitTime;
	}

}
