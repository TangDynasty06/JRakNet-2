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
package port.raknet.java.utils;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Used for easily manipulation of arrays
 *
 * @author Trent Summerlin
 */
public abstract class ArrayUtils {

	/**
	 * Splits an array into more chunks with the specified maximum size for each
	 * array chunk
	 * 
	 * @param src
	 * @param size
	 * @return byte[][]
	 */
	public static byte[][] splitArray(byte[] src, int size) {
		if (size > 0) {
			int index = 0;
			ArrayList<byte[]> split = new ArrayList<byte[]>();
			while (index < src.length) {
				if (index + size <= src.length) {
					split.add(Arrays.copyOfRange(src, index, index + size));
					index += size;
				} else {
					split.add(Arrays.copyOfRange(src, index, src.length));
					index = src.length;
				}
			}
			return split.toArray(new byte[split.size()][]);
		} else {
			return new byte[0][0];
		}
	}

}
