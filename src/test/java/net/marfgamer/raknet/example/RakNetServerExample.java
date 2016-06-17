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

import net.marfgamer.raknet.RakNetOptions;
import net.marfgamer.raknet.event.Hook;
import net.marfgamer.raknet.exception.RakNetException;
import net.marfgamer.raknet.server.RakNetServer;
import net.marfgamer.raknet.session.RakNetSession;
import net.marfgamer.raknet.utils.RakNetUtils;

/**
 * A simple RakNet server, this can be tested using a Minecraft: Pocket Edition
 * client. Simply launch the game and click on "Play". Then, "A RakNet Server"
 * should pop up, just like when someone else is playing on the same network and
 * their name pops up.
 *
 * @author Trent Summerlin
 */
public class RakNetServerExample {

	public static void main(String[] args) throws RakNetException {
		// Create options and set identifier
		RakNetOptions options = new RakNetOptions();
		options.serverPort = 19132;
		options.serverMaxConnections = 10;
		options.serverIdentifier = "MCPE;A RakNet Server;80;0.15.0;0;10;" + RakNetUtils.getRakNetID() + ";";

		// Create server and add hooks
		RakNetServer server = new RakNetServer(options);

		// Client connected
		server.addHook(Hook.SESSION_CONNECTED, (Object[] parameters) -> {
			RakNetSession session = (RakNetSession) parameters[0];
			System.out.println("Client from address " + session.getSocketAddress() + " has connected to the server");
		});

		// Client disconnected
		server.addHook(Hook.SESSION_DISCONNECTED, (Object[] parameters) -> {
			RakNetSession session = (RakNetSession) parameters[0];
			String reason = parameters[1].toString();
			System.out.println("Client from address " + session.getSocketAddress()
					+ " has disconnected from the server for the reason \"" + reason + "\"");
		});

		// Start server
		server.start();
	}

}
