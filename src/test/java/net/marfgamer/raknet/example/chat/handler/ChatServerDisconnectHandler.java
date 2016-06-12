package net.marfgamer.raknet.example.chat.handler;

import net.marfgamer.raknet.event.HookRunnable;

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
