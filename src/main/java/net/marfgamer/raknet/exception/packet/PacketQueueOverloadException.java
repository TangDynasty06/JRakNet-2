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

import net.marfgamer.raknet.exception.RakNetException;
import net.marfgamer.raknet.session.RakNetSession;

/**
 * Occurs when a queue for a <code>RakNetSession</code> is too big
 *
 * @author Trent Summerlin
 */
public class PacketQueueOverloadException extends RakNetException {

	private static final long serialVersionUID = -289422497689147588L;

	private final RakNetSession session;
	private final String queueName;
	private final int queueSize;

	public PacketQueueOverloadException(RakNetSession session, String queueName, int queueSize) {
		super("Packet queue \"" + queueName + "\" exceeded it's limits of " + queueSize + "limit");
		this.session = session;
		this.queueName = queueName;
		this.queueSize = queueSize;
	}

	/**
	 * Returns the session that caused the error
	 * 
	 * @return RakNetSession
	 */
	public RakNetSession getSession() {
		return this.session;
	}

	/**
	 * Returns the name of the queue that overloaded
	 * 
	 * @return String
	 */
	public String getQueueName() {
		return this.queueName;
	}

	/**
	 * Returns the queues maximum size before it overloads
	 * 
	 * @return int
	 */
	public int getQueueSize() {
		return this.queueSize;
	}

	@Override
	public String getLocalizedMessage() {
		return "Packet queue overloaded!";
	}

}
