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
package net.marfgamer.raknet.example;

import java.net.InetSocketAddress;

import net.marfgamer.raknet.RakNetOptions;
import net.marfgamer.raknet.client.RakNetClient;
import net.marfgamer.raknet.event.Hook;
import net.marfgamer.raknet.event.HookRunnable;
import net.marfgamer.raknet.exception.RakNetException;
import net.marfgamer.raknet.session.RakNetSession;

/**
 * A simple RakNet client, this example attempts to connect to the main LBSG
 * server. When it is connected, it closes the connection and shuts down.
 *
 * @author Trent Summerlin
 */
public class RakNetClientExample {

	// Server address and port
	private static final String SERVER_ADDRESS = "sg.lbsg.net";
	private static final int SERVER_PORT = 19132;

	public static void main(String[] args) throws RakNetException {
		// There are no special options needed for clients
		RakNetClient client = new RakNetClient(new RakNetOptions());

		// Server connected
		client.addHook(Hook.SESSION_CONNECTED, new HookRunnable() {

			@Override
			public void run(Object... parameters) {
				RakNetSession session = (RakNetSession) parameters[0];
				System.out.println("Successfully connected to server with address " + session.getSocketAddress());
				client.disconnect();
			}

		});

		// Server disconnected
		client.addHook(Hook.SESSION_DISCONNECTED, new HookRunnable() {

			@Override
			public void run(Object... parameters) {
				RakNetSession session = (RakNetSession) parameters[0];
				String reason = parameters[1].toString();
				System.out.println("Successfully disconnected from server with address " + session.getSocketAddress()
						+ " for the reason \"" + reason + "\"");
				System.exit(0);
			}

		});
		
		// Attempt to connect to server
		client.connect(new InetSocketAddress(SERVER_ADDRESS, SERVER_PORT));
	}

}
