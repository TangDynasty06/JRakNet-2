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
package net.marfgamer.raknet.exception;

/**
 * Occurs when a packet does not begin with the specified header byte, this is
 * the only RakNet related exception that does not extend
 * <code>RakNetException</code> but rather <code>IllegalArgumentException</code>
 *
 * @author Trent Summerlin
 */
public class InvalidHeaderException extends IllegalArgumentException {

	private static final long serialVersionUID = -6795924002574253623L;

	private final int expectedHeader;
	private final int receivedHeader;

	public InvalidHeaderException(int expectedHeader, int receivedHeader) {
		super("Received packet with wrong header! Received header " + receivedHeader + " instead of " + expectedHeader);
		this.expectedHeader = expectedHeader;
		this.receivedHeader = receivedHeader;
	}

	/**
	 * Returns the expected header byte
	 * 
	 * @return int
	 */
	public int getExpectedHeader() {
		return this.expectedHeader;
	}

	/**
	 * Returns the received header byte
	 * 
	 * @return int
	 */
	public int getReceivedHeader() {
		return this.receivedHeader;
	}

	@Override
	public String getLocalizedMessage() {
		return "Packet had wrong header!";
	}

}
