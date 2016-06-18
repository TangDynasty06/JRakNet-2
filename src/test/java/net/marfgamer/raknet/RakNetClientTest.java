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
package net.marfgamer.raknet;

import java.net.InetSocketAddress;
import java.util.Scanner;

import net.marfgamer.raknet.client.RakNetClient;
import net.marfgamer.raknet.event.Hook;
import net.marfgamer.raknet.exception.RakNetException;
import net.marfgamer.raknet.session.RakNetSession;

/**
 * Used to test <code>RakNetClient</code>, meant for testing with Minecraft:
 * Pocket Edition servers
 *
 * @author Trent Summerlin
 */
public class RakNetClientTest {

	private static final String SERVER_ADDRESS = "sg.lbsg.net";
	private static final int SERVER_PORT = 19132;

	public static void main(String[] args) throws RakNetException {
		// Create client
		RakNetClient client = new RakNetClient();

		// Client connected
		client.addHook(Hook.SESSION_CONNECTED, (Object[] parameters) -> {
			RakNetSession session = (RakNetSession) parameters[0];
			System.out.println(
					"Client has connected to server with address " + session.getSocketAddress() + ", disconnecting...");
			client.disconnect();
		});

		// Client disconnected
		client.addHook(Hook.SESSION_DISCONNECTED, (Object[] parameters) -> {
			RakNetSession session = (RakNetSession) parameters[0];
			String reason = parameters[1].toString();
			System.out.println("Server with address " + session.getSocketAddress()
					+ " has been disconnected for the reason \"" + reason + "\"");
		});

		// Exception caught
		client.addHook(Hook.HANDLER_EXCEPTION_OCCURED, (Object[] parameters) -> {
			Throwable throwable = (Throwable) parameters[0];
			InetSocketAddress naughtyAddress = (InetSocketAddress) parameters[1];
			System.out.println(
					"Handler exception " + throwable.getClass().getSimpleName() + " caused by " + naughtyAddress);
		});

		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(System.in);
		System.out.println("Press enter to connect to the server!");
		while (true) {
			try {
				while (!scanner.hasNextLine())
					;
				scanner.nextLine();
				client.connect(SERVER_ADDRESS, SERVER_PORT);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
