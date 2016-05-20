package port.raknet.java.scheduler;

import java.util.ArrayList;
import java.util.Random;

import port.raknet.java.RakNetOptions;
import port.raknet.java.protocol.raknet.StatusRequest;

public class RakNetScheduler extends Thread {

	private static final Random generator = new Random();
	public static final long TICK = 1L;

	private boolean running;
	private int taskId;

	private final ArrayList<RakNetTask> tasks;
	private final ArrayList<RakNetRepeatingTask> repeating;

	public RakNetScheduler(RakNetOptions options) {
		this.tasks = new ArrayList<RakNetTask>();
		this.repeating = new ArrayList<RakNetRepeatingTask>();
	}

	/**
	 * Schedules a task to run once, the wait time is in milliseconds
	 * 
	 * @param task
	 * @param wait
	 */
	public int scheduleTask(Runnable task, long wait) {
		tasks.add(taskId++, new RakNetTask(task, wait));
		return this.taskId;
	}

	/**
	 * Cancels a task based on it's ID
	 * 
	 * @param taskId
	 */
	public void cancelTask(int taskId) {
		try {
			tasks.remove(taskId);
		} catch (IndexOutOfBoundsException e) {
			// Ignore, easily caused
		}
	}

	/**
	 * Schedules a repeating task, the wait time is in milliseconds
	 * 
	 * @param task
	 * @param wait
	 */
	public int scheduleRepeatingTask(Runnable task, long wait) {
		repeating.add(taskId++, new RakNetRepeatingTask(task, wait));
		return this.taskId;
	}

	/**
	 * Cancels a repeating task based on it's ID
	 * 
	 * @param taskId
	 */
	public void cancelRepeatingTask(int taskId) {
		try {
			repeating.remove(taskId);
		} catch (IndexOutOfBoundsException e) {
			// Ignore, easily caused
		}
	}

	@Override
	public void run() {
		// Check and initialize data
		if (running == true) {
			throw new RuntimeException("Scheduler is already running!");
		}
		long lastTick = System.currentTimeMillis();
		this.running = true;

		// Start loop
		while (true) {
			long current = System.currentTimeMillis();
			if (current - lastTick >= 1000L) {
				StatusRequest request = new StatusRequest();
				request.pingId = generator.nextLong();
				request.encode();

				// client.broadcastPacket(options.broadcastPort, request);
			}

			if (current - lastTick >= TICK) {
				// Update tasks
				for (int i = 0; i < tasks.size(); i++) {
					RakNetTask task = tasks.get(i);
					task.waitTime -= TICK;
					if (task.waitTime <= 0) {
						task.runnable.run();
						tasks.remove(task);
					}
				}

				// Update repeating tasks
				for (int i = 0; i < repeating.size(); i++) {
					RakNetRepeatingTask task = repeating.get(i);
					task.waitTime -= TICK;
					if (task.waitTime <= 0) {
						task.runnable.run();
						task.waitTime = task.reset;
					}
				}

				lastTick = current;
			}
		}
	}

}
