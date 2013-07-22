package com.twinsoft.convertigo.engine.util;

public class HexUtils {

	/** The hexadecimal digits "0" through "f". */
	private static char[] NIBBLE = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
			'e', 'f', };

	public static final String toHexString(byte a) {
		StringBuffer sb = new StringBuffer(2);
		sb.append(NIBBLE[(a >>> 4) & 0xf]);
		sb.append(NIBBLE[a & 0xf]);

		return sb.toString();
	}

	/**
	 * Convert a byte array to a string of hexadecimal digits.
	 */
	public static final String toHexString(byte[] buf) {
		StringBuffer sb = new StringBuffer(buf.length * 2);

		for (int i = 0; i < buf.length; i++) {
			sb.append(NIBBLE[(buf[i] >>> 4) & 15]);
			sb.append(NIBBLE[buf[i] & 15]);
		}

		return sb.toString();
	}

	/**
	 * Convert a string of hexadecimal digits to a byte array.
	 */
	public static byte[] fromHexString(String hex) {
		int l = (hex.length() + 1) / 2;
		byte[] r = new byte[l];
		int i = 0;
		int j = 0;

		if ((hex.length() & 1) == 1) {
			// Odd number of characters: must handle half byte first.
			r[0] = HexUtils.fromHexNibble(hex.charAt(0));
			i = j = 1;
		}

		while (i < l)
			r[i++] = (byte) ((HexUtils.fromHexNibble(hex.charAt(j++)) << 4) |
					HexUtils.fromHexNibble(hex.charAt(j++)));

		return r;
	}

	/**
	 * Convert a hexadecimal digit to a byte.
	 */
	private static byte fromHexNibble(char n) {
		if (n <= '9')
			return (byte) (n - '0');

		if (n <= 'G')
			return (byte) (n - ('A' - 10));

		return (byte) (n - ('a' - 10));
	}

}
