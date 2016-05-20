package port.raknet.java.scheduler;

public class RakNetTask {
	
	public final Runnable runnable;
	public long waitTime;
	
	public RakNetTask(Runnable runnable, long waitTime) {
		this.runnable = runnable;
		this.waitTime = waitTime;
	}

}
