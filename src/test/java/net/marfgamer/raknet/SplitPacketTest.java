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

	private static final short SPLIT_START_ID = 0xFE;
	private static final short SPLIT_END_ID = 0xFF;

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
			System.exit(1);
		});

		// Packet received
		server.addHook(Hook.PACKET_RECEIVED, (Object[] parameters) -> {
			RakNetSession session = (RakNetSession) parameters[0];
			EncapsulatedPacket encapsulated = (EncapsulatedPacket) parameters[1];
			Packet packet = encapsulated.convertPayload();
			System.out.println("Server: Received packet of " + encapsulated.payload.length + " bytes from "
					+ session.getAddress() + ", checking data...");

			// Check packet ID
			if (packet.getId() != SPLIT_START_ID) {
				System.err.println("Packet header is " + packet.getId() + " when it should be " + SPLIT_START_ID + "!");
				System.exit(1);
			}

			// Check shorts
			int lastShort = -1;
			while (packet.remaining() >= 2) {
				int currentShort = packet.getUShort();
				if (currentShort - lastShort != 1) {
					System.err.println("Short data was not split correctly!");
					System.exit(1);
				} else {
					lastShort = currentShort;
				}
			}

			// Check ending byte
			if (packet.getUByte() != SPLIT_END_ID) {
				System.err.println("Packet footer is " + packet.getId() + " when it should be " + SPLIT_START_ID + "!");
				System.exit(1);
			}

			System.out.println("Split packet test passed! ｡◕‿‿◕｡");
			System.exit(0);
		});

		server.start();
		return server;
	}

	private static RakNetClient createClient() throws RakNetException {
		// Create client and add hooks
		RakNetClient client = new RakNetClient(RakNetClient.NO_DISCOVERY, MINIMUM_TRANSFER_UNIT);

		// Server connected
		client.addHook(Hook.SESSION_CONNECTED, (Object[] parameters) -> {
			RakNetSession session = (RakNetSession) parameters[0];
			System.out.println("Client: Connected to server with MTU " + session.getMaximumTransferUnit());

			// Send huge packet of doom
			Packet packet = new Packet(SPLIT_START_ID);
			for (int i = 0; i < (EncapsulatedPacket.getMaxPacketSize(Reliability.RELIABLE_ORDERED,
					MINIMUM_TRANSFER_UNIT) - 2) / 2; i++) { // Subtract 1 for
															// packet ID (0xFE)
															// and 1 for end
															// packet byte
															// (0xFF)
				packet.putUShort(i);
			}
			packet.putUByte(SPLIT_END_ID);

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
