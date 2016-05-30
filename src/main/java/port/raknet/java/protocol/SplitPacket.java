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
package port.raknet.java.protocol;

import java.util.ArrayList;

import port.raknet.java.protocol.raknet.internal.CustomPacket;
import port.raknet.java.protocol.raknet.internal.EncapsulatedPacket;
import port.raknet.java.utils.ArrayUtils;

/**
 * A packet that has been split up into multiple packets since it was too big to
 * be sent at once
 *
 * @author Trent Summerlin
 */
public class SplitPacket {

	public static EncapsulatedPacket[] createSplit(EncapsulatedPacket packet, int mtuSize, int splitId) {
		byte[][] splitData = ArrayUtils.splitArray(packet.payload, mtuSize - CustomPacket.DEFAULT_SIZE);
		ArrayList<EncapsulatedPacket> packets = new ArrayList<EncapsulatedPacket>();
		for (int i = 0; i < splitData.length; i++) {
			// Copy packet data
			EncapsulatedPacket encapsulated = new EncapsulatedPacket();
			encapsulated.messageIndex = packet.messageIndex;
			encapsulated.orderChannel = packet.orderChannel;
			encapsulated.orderIndex = packet.orderIndex;

			// Set split data
			encapsulated.split = true;
			encapsulated.splitIndex = i;
			encapsulated.splitId = splitId;
			encapsulated.splitCount = splitData.length;

			// Set payload data
			encapsulated.payload = splitData[i];
			packets.add(encapsulated);
		}
		return packets.toArray(new EncapsulatedPacket[packets.size()]);
	}

}
