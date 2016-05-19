package port.raknet.java.event;

/**
 * Executed when the hook is ran, only one HookRunnable is allowed per-hook.
 *
 * @author Trent Summerlin
 */
public abstract class HookRunnable {

	public abstract void run(Object... parameters);

}
