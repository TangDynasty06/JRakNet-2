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
