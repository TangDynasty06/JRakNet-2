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

import net.marfgamer.raknet.RakNet;
import net.marfgamer.raknet.client.RakNetClient;
import net.marfgamer.raknet.event.Hook;
import net.marfgamer.raknet.exception.RakNetException;
import net.marfgamer.raknet.protocol.Packet;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.protocol.raknet.internal.EncapsulatedPacket;
import net.marfgamer.raknet.server.RakNetServer;
import net.marfgamer.raknet.session.RakNetSession;

/**
 * Used for making sure split packets work to their full capacity without
 * problems
 *
 * @author Trent Summerlin
 */
public class SplitPacketTest implements RakNet {

	public static void main(String[] args) throws RakNetException {
		System.out.println("Creating server...");
		createServer();

		System.out.println("Creating client...");
		createClient();
	}

	private static RakNetServer createServer() throws RakNetException {
		RakNetServer server = new RakNetServer(30851, 1, MINIMUM_TRANSFER_UNIT);

		// Client connected
		server.addHook(Hook.SESSION_CONNECTED, (Object[] parameters) -> {
			RakNetSession session = (RakNetSession) parameters[0];
			System.out.println("Server: Client connected from " + session.getAddress() + "!");
		});

		// Client disconnected
		server.addHook(Hook.SESSION_DISCONNECTED, (Object[] parameters) -> {
			RakNetSession session = (RakNetSession) parameters[0];
			System.out
					.println("Server: Client from " + session.getAddress() + " disconnected! (" + parameters[1] + ")");
			System.exit(0);
		});

		// Packet received
		server.addHook(Hook.PACKET_RECEIVED, (Object[] parameters) -> {
			RakNetSession session = (RakNetSession) parameters[0];
			EncapsulatedPacket encapsulated = (EncapsulatedPacket) parameters[1];

			System.out.println("Server: Received packet of " + encapsulated.payload.length + " bytes from "
					+ session.getAddress());
			System.exit(0);
		});

		server.start();
		return server;
	}

	private static RakNetClient createClient() throws RakNetException {
		// Create client and add hooks
		RakNetClient client = new RakNetClient();

		// Server connected
		client.addHook(Hook.SESSION_CONNECTED, (Object[] parameters) -> {
			RakNetSession session = (RakNetSession) parameters[0];
			System.out.println("Client: Connected to server with MTU " + session.getMaximumTransferUnit());

			// Send huge packet of doom
			Packet packet = new Packet(0xFF);
			for (int i = 0; i < EncapsulatedPacket.getMaxPacketSize(Reliability.RELIABLE_ORDERED, MINIMUM_TRANSFER_UNIT)
					- 1; i++) {
				packet.putUByte(0x00);
			}
			System.out.println("Client: Sending giant packet... (" + packet.size() + " bytes)");
			session.sendPacket(Reliability.RELIABLE_ORDERED, packet);
		});

		// Server disconnect
		client.addHook(Hook.SESSION_DISCONNECTED, (Object[] parameters) -> {
			System.out.println("Client: Lost connection to server! (" + parameters[1] + ")");
		});

		// Server disconnect
		client.addHook(Hook.HANDLER_EXCEPTION_OCCURED, (Object[] parameters) -> {
			Throwable throwable = (Throwable) parameters[0];
			throwable.printStackTrace();
		});

		// Connect to server
		client.connect("localhost", 30851);
		return client;
	}

}
