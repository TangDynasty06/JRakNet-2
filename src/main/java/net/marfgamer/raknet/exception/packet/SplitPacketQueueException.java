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
package net.marfgamer.raknet.exception.packet;

import net.marfgamer.raknet.RakNet;
import net.marfgamer.raknet.protocol.raknet.internal.EncapsulatedPacket;
import net.marfgamer.raknet.session.RakNetSession;

/**
 * Occurs whenever a split packet has too many split packets needed to complete
 * it
 *
 * @author Trent Summerlin
 */
public class SplitPacketQueueException extends RakNetPacketException {

	private static final long serialVersionUID = 6437847918319611506L;

	private final EncapsulatedPacket packet;

	public SplitPacketQueueException(RakNetSession session, EncapsulatedPacket packet) {
		super(session, "EncapsulatedPacket has splitCount over " + RakNet.MAX_SPLIT_COUNT + "!");
		this.packet = packet;
	}

	/**
	 * Returns the packet that caused the error
	 * 
	 * @return EncapsulatedPacket
	 */
	public EncapsulatedPacket getPacket() {
		return this.packet;
	}

}
