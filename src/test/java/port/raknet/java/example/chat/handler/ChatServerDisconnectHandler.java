package port.raknet.java.example.chat.handler;

import port.raknet.java.event.HookRunnable;

/**
 * Handles all server disconnections
 *
 * @author Trent Summerlin
 */
public class ChatServerDisconnectHandler implements HookRunnable {

	@Override
	public void run(Object... parameters) {
		System.out.println("Lost connection to server!");
		System.exit(0);
	}

}
