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

/**
 * Used for setting the options for <code>RakNetServer</code> and
 * <code>RakNetClient</code>
 *
 * @author Trent Summerlin
 */
public class RakNetOptions {

	public RakNetOptions() {
	}

	public RakNetOptions(int serverPort, String serverIdentifier) {
		this.serverPort = serverPort;
		this.serverIdentifier = serverIdentifier;
	}

	public RakNetOptions(int broadcastPort) {
		this.clientBroadcastPort = broadcastPort;
	}

	/**
	 * The port the server runs on
	 */
	public int serverPort = 19132;

	/**
	 * The identifier the server broadcasts when receiving a unconnected ping
	 */
	public String serverIdentifier = "";

	/**
	 * The port the client will use to find other servers on the network
	 */
	public int clientBroadcastPort = this.serverPort;

	/**
	 * The maximum amount of data <code>RakNetServer</code> will allow for a
	 * client, this is what <code>RakNetClient</code> will start at as it
	 * gradually decreases until a response is received
	 */
	public int maximumTransferUnit = 2048;

	/**
	 * How long until a session is disconnected from the server due to
	 * inactivity, it is suggested be at least 10,000 MS (10 Seconds)
	 */
	public long timeout = 10000L;

}
