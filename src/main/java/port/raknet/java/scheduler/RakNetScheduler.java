package port.raknet.java.scheduler;

import java.util.ArrayList;

/**
 * Used to run tasks at certain times
 *
 * @author Trent Summerlin
 */
public class RakNetScheduler extends Thread {

	private boolean running;
	private int taskId;

	private final ArrayList<RakNetTask> tasks;
	private final ArrayList<RakNetRepeatingTask> repeating;

	public RakNetScheduler() {
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
		this.running = true;
		long last = System.currentTimeMillis();

		// Start loop
		while (true) {
			long current = System.currentTimeMillis();
			long difference = (current - last);

			// Update tasks
			for (int i = 0; i < tasks.size(); i++) {
				RakNetTask task = tasks.get(i);
				task.waitTime -= difference;
				if (task.waitTime <= 0) {
					task.runnable.run();
					tasks.remove(task);
				}
			}

			// Update repeating tasks
			for (int i = 0; i < repeating.size(); i++) {
				RakNetRepeatingTask task = repeating.get(i);
				task.waitTime -= difference;
				if (task.waitTime <= 0) {
					task.runnable.run();
					task.waitTime = task.reset;
				}
			}

			// Update time
			last = current;
		}
	}

}
