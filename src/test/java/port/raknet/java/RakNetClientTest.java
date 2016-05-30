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
package port.raknet.java;

import port.raknet.java.client.RakNetClient;
import port.raknet.java.event.Hook;
import port.raknet.java.event.HookRunnable;
import port.raknet.java.protocol.raknet.internal.EncapsulatedPacket;
import port.raknet.java.session.RakNetSession;

/**
 * Used to test <code>RakNetClient</code>, meant for testing with Minecraft:
 * Pocket Edition servers
 *
 * @author Trent Summerlin
 */
public class RakNetClientTest {

	private static final String SERVER_ADDRESS = "sg.lbsg.net";
	private static final int SERVER_PORT = 19132;

	public static void main(String[] args) throws Exception {
		RakNetClient client = new RakNetClient(new RakNetOptions());

		// Client disconnected
		client.addHook(Hook.SESSION_CONNECTED, new HookRunnable() {
			@Override
			public void run(Object... parameters) {
				RakNetSession session = (RakNetSession) parameters[0];
				System.out.println("Client has connected to server with address " + session.getSocketAddress());
			}
		});

		// Client connected
		client.addHook(Hook.PACKET_RECEIVED, new HookRunnable() {
			@Override
			public void run(Object... parameters) {
				RakNetSession session = (RakNetSession) parameters[0];
				EncapsulatedPacket encapsulated = (EncapsulatedPacket) parameters[1];
				System.out.println("Received packet from server " + session.getSocketAddress() + " with packet ID: 0x"
						+ Integer.toHexString(encapsulated.payload[0] & 0xFF).toUpperCase());
			}
		});

		// Client disconnected
		client.addHook(Hook.SESSION_DISCONNECTED, new HookRunnable() {
			@Override
			public void run(Object... parameters) {
				RakNetSession session = (RakNetSession) parameters[0];
				String reason = parameters[1].toString();
				System.out.println("Server with address " + session.getSocketAddress()
						+ " has been disconnected for the reason \"" + reason + "\"");
			}
		});

		client.connect(SERVER_ADDRESS, SERVER_PORT);
	}

}
