package port.raknet.java.scheduler;

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
