package port.raknet.java.scheduler;

/**
 * Used to run a task at a certain time once
 *
 * @author Trent Summerlin
 */
public class RakNetTask {

	public final Runnable runnable;
	public long waitTime;

	public RakNetTask(Runnable runnable, long waitTime) {
		this.runnable = runnable;
		this.waitTime = waitTime;
	}

}
