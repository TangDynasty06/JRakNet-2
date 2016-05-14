package port.raknet.java;

import java.util.ArrayList;
import java.util.Arrays;

public abstract class Utils {

	/**
	 * Cycles through an array, if none of the values are null then the array
	 * has been filled
	 * 
	 * @param src
	 * @return boolean
	 */
	public static boolean arrayFilled(Object[] src) {
		for (int i = 0; i < src.length; i++) {
			if (src[i] == null) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Splits an array into more chunks with the specified maximum size for each
	 * array chunk
	 * 
	 * @param src
	 * @param size
	 * @return byte[][]
	 */
	public static byte[][] splitArray(byte[] src, int size) {
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
	}

}
