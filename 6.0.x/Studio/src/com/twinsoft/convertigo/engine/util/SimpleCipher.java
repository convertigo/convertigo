/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.engine.util;

public class SimpleCipher {

	private static final String CIPHER = "aZbYcXdWeVfUgThSiRjQkPlOmNnMoLpKqJrIsHtGuFvEwDxCyBzA1029384756";
	
	public static String encode(String stringToEncode) {
		String coded = "";

		String encodedString = Base64.encodeString(stringToEncode);

		int shift = encodedString.length();
		int cipherLen = SimpleCipher.CIPHER.length();
		char chr;

		for (int i = 0; i < shift; i++) {
			coded += (SimpleCipher.CIPHER.indexOf(chr = encodedString.charAt(i)) == -1) ? chr
					: SimpleCipher.CIPHER.charAt((SimpleCipher.CIPHER.indexOf(chr) + shift) % cipherLen);
		}

		return coded;
	}

	public static String decode(String encodedString) {
		String link = "";
		int shift = encodedString.length();
		int cipherLen = SimpleCipher.CIPHER.length();
		char chr;

		for (int i = 0; i < shift; i++)
			link += (SimpleCipher.CIPHER.indexOf(chr = encodedString.charAt(i)) == -1) ? chr
					: SimpleCipher.CIPHER.charAt((SimpleCipher.CIPHER.indexOf(chr) - shift + cipherLen * Math.round(encodedString.length() / SimpleCipher.CIPHER.length() + 1)) % cipherLen);

		try {
			return Base64.decodeToString(link);
		} catch (Exception e) {
		}

		return link;
	}

}
