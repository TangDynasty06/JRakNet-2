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
package port.raknet.java.broadcast;

/**
 * Represents a Minecraft: Pocket Edition server that has been discovered on the
 * local network
 *
 * @author Trent Summerlin
 */
public class DiscoveredMinecraftServer {

	public final String name;
	public final int protocol;
	public final String version;
	public final int online;
	public final int max;

	public DiscoveredMinecraftServer(String name, int protocol, String version, int online, int max) {
		this.name = name;
		this.protocol = protocol;
		this.version = version;
		this.online = online;
		this.max = max;
	}

	public DiscoveredMinecraftServer(String identifier) {
		// Check data
		String[] chunks = identifier.split(";");
		if (!chunks[0].equals("MCPE")) {
			throw new IllegalArgumentException(
					"Invalid identifier! Must start with \"MCPE\", not \"" + chunks[0] + "\"");
		}

		// Set data
		this.name = chunks[1];
		this.protocol = parseIgnoreError(chunks[2]);
		this.version = chunks[3];
		this.online = parseIgnoreError(chunks[4]);
		this.max = parseIgnoreError(chunks[5]);
	}

	private static int parseIgnoreError(String parse) {
		try {
			return Integer.parseInt(parse);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

}
