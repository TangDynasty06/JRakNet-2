/*
 *       _   _____            _      _   _          _   
 *      | | |  __ \          | |    | \ | |        | |  
 *      | | | |__) |   __ _  | | __ |  \| |   ___  | |_ 
 *  _   | | |  _  /   / _` | | |/ / | . ` |  / _ \ | __|
 * | |__| | | | \ \  | (_| | |   <  | |\  | |  __/ | |_ 
 *  \____/  |_|  \_\  \__,_| |_|\_\ |_| \_|  \___|  \__|
 *                                                  
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Trent Summerlin

 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.  
 */
package net.marfgamer.raknet.scheduler;

import java.util.ArrayList;

import net.marfgamer.raknet.task.TaskRunnable;

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
	 * Schedules a task to run once
	 * 
	 * @param task
	 * @return int
	 */
	public synchronized int scheduleTask(TaskRunnable task) {
		tasks.add(taskId++, new RakNetTask(task));
		return this.taskId;
	}

	/**
	 * Schedules a task to run once
	 * 
	 * @param task
	 * @param wait
	 * @return int
	 */
	public synchronized int scheduleTask(Runnable task, long wait) {
		return this.scheduleTask(new TaskRunnable() {

			@Override
			public long getWaitTimeMillis() {
				return wait;
			}

			@Override
			public void run() {
				task.run();
			}

		});
	}

	/**
	 * Cancels a task based on it's ID
	 * 
	 * @param taskId
	 */
	public synchronized void cancelTask(int taskId) {
		try {
			tasks.remove(taskId);
		} catch (IndexOutOfBoundsException e) {
			// Ignore, easily caused
		}
	}

	/**
	 * Schedules a repeating task
	 * 
	 * @param task
	 * @return int
	 */
	public synchronized int scheduleRepeatingTask(TaskRunnable task) {
		repeating.add(taskId++, new RakNetRepeatingTask(task));
		return this.taskId;
	}

	/**
	 * Schedules a repeating task
	 * 
	 * @param task
	 * @param wait
	 * @return int
	 */
	public synchronized int scheduleRepeatingTask(Runnable task, long wait) {
		return this.scheduleRepeatingTask(new TaskRunnable() {

			@Override
			public long getWaitTimeMillis() {
				return wait;
			}

			@Override
			public void run() {
				task.run();
			}

		});
	}

	/**
	 * Cancels a repeating task based on it's ID
	 * 
	 * @param taskId
	 */
	public synchronized void cancelRepeatingTask(int taskId) {
		try {
			repeating.remove(taskId);
		} catch (IndexOutOfBoundsException e) {
			// Ignore, easily caused
		}
	}

	@Override
	public synchronized void run() {
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
			synchronized (tasks) {
				for (int i = 0; i < tasks.size(); i++) {
					RakNetTask scheduledTask = tasks.get(i);
					scheduledTask.waitTime -= difference;
					if (scheduledTask.waitTime <= 0) {
						scheduledTask.runnable.run();
						tasks.remove(scheduledTask);
					}
					
					// Update repeating tasks
					for (int j = 0; j < repeating.size(); j++) {
						RakNetRepeatingTask repeatingTask = repeating.get(j);
						repeatingTask.waitTime -= difference;
						if (repeatingTask.waitTime <= 0) {
							repeatingTask.runnable.run();
							repeatingTask.waitTime = repeatingTask.reset;
						}
					}
				}
			}

			// Update time
			last = current;
		}
	}

}
