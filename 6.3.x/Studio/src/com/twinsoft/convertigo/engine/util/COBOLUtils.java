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

import java.math.BigInteger;

public class COBOLUtils {
	
	public static final int COBOL_PICTURE_X			= 0x01;
	public static final int COBOL_PICTURE_9			= 0x02;
	public static final int COBOL_PICTURE_S9		= 0x03;
	public static final int COBOL_PICTURE_9V9		= 0x04;
	public static final int COBOL_PICTURE_S9V9		= 0x05;
	
	public static final int COBOL_FORMAT_DISPLAY	= 0x10;
	public static final int COBOL_FORMAT_COMP		= 0x11;
	public static final int COBOL_FORMAT_COMP_1		= 0x12;
	public static final int COBOL_FORMAT_COMP_2		= 0x13;
	public static final int COBOL_FORMAT_COMP_3		= 0x14;
	public static final int COBOL_FORMAT_COMP_5		= 0x15;
	public static final int COBOL_FORMAT_POINTER	= 0x20;
		
	private static final int SIZE = 95;
	
	private static final int COMP_S9[] = {1,1,2,2,3,3,4,4,4,5,5,6,6,6,7,7,8,8};
	
	private static final int COMP_9[] = {1,1,2,2,3,3,3,4,4,5,5,5,6,6,7,7,8,8};
	
	private static final int ASCII[] = {
			0x0020, 0x0021, 0x0022, 0x0023, 0x0024, 0x0025, 0x0026, 0x0027, 0x0028,
			0x0029, 0x002a, 0x002b, 0x002c, 0x002d, 0x002e, 0x002f,
			0x0030, 0x0031, 0x0032, 0x0033, 0x0034, 0x0035, 0x0036, 0x0037, 0x0038,
			0x0039, 0x003a, 0x003b, 0x003c, 0x003d, 0x003e, 0x003f,
			0x0040, 0x0041, 0x0042, 0x0043, 0x0044, 0x0045, 0x0046, 0x0047, 0x0048,
			0x0049, 0x004a, 0x004b, 0x004c, 0x004d, 0x004e, 0x004f,
			0x0050, 0x0051, 0x0052, 0x0053, 0x0054, 0x0055, 0x0056, 0x0057, 0x0058,
			0x0059, 0x005a, 0x005b, 0x005c, 0x005d, 0x005e, 0x005f,
			0x0060, 0x0061, 0x0062, 0x0063, 0x0064, 0x0065, 0x0066, 0x0067, 0x0068,
			0x0069, 0x006a, 0x006b, 0x006c, 0x006d, 0x006e, 0x006f,
			0x0070, 0x0071, 0x0072, 0x0073, 0x0074, 0x0075, 0x0076, 0x0077, 0x0078,
			0x0079, 0x007a, 0x007b, 0x007c, 0x007d, 0x007e
	};
	      
	private static final int EBCDIC[] = {
		0x0040, 0x005a, 0x007f, 0x007b, 0x005b, 0x006c, 0x0050, 0x007d, 0x004d,
		0x005d, 0x005c, 0x004e, 0x006b, 0x0060, 0x004b, 0x0061,
		0x00f0, 0x00f1, 0x00f2, 0x00f3, 0x00f4, 0x00f5, 0x00f6, 0x00f7, 0x00f8,
		0x00f9, 0x007a, 0x005e, 0x004c, 0x007e, 0x006e, 0x006f,
		0x007c, 0x00c1, 0x00c2, 0x00c3, 0x00c4, 0x00c5, 0x00c6, 0x00c7, 0x00c8,
		0x00c9, 0x00d1, 0x00d2, 0x00d3, 0x00d4, 0x00d5, 0x00d6,
		0x00d7, 0x00d8, 0x00d9, 0x00e2, 0x00e3, 0x00e4, 0x00e5, 0x00e6, 0x00e7,
		0x00e8, 0x00e9, 0x00ad, 0x00e0, 0x00bd, 0x005f, 0x006d,
		0x0079, 0x0081, 0x0082, 0x0083, 0x0084, 0x0085, 0x0086, 0x0087, 0x0088,
		0x0089, 0x0091, 0x0092, 0x0093, 0x0094, 0x0095, 0x0096,
		0x0097, 0x0098, 0x0099, 0x00a2, 0x00a3, 0x00a4, 0x00a5, 0x00a6, 0x00a7,
		0x00a8, 0x00a9, 0x00c0, 0x006a, 0x00d0, 0x00a1
	};      

	/**
	 * Translates a int from ASCII to EBCDIC
	 * @param int the int to be translated
	 * @return int the translated int
	 * @exception
	 **/                                                                    
	public final static int translateToEBCDIC(int i) {
		for (int j = 0; j<SIZE; j++) {
			if (i == ASCII[j]) {
				return EBCDIC[j];
			}
		}               
		return i;
	}

	/**
	 * Translates a int from EBCDIC to ASCII
	 * @param int the int to be translated
	 * @return int the translated int
	 * @exception
	 **/                                                                    
	public final static int translateToASCII(int i) {
		return COBOLUtils.translateToASCII(i, 0x20);
	}

	public final static int translateToASCII(int i, int nonPrintableCharacter) {
		for (int j = 0; j<SIZE; j++) {
			if (i == EBCDIC[j]) {
				return ASCII[j];
			}
		}               
		return (int) nonPrintableCharacter;
	}
	
	/**
	 * Encode an ASCII string to an EBCDIC bytes buffer
	 * @param s the string to encode
	 * @return the buffer of bytes
	 */
	public static byte[] encodeASCIIToEBCDIC(String s) {
		byte[] buf = new byte[]{};
		if (s != null) {
			int len = s.length();
			buf = s.getBytes();
			for (int i=0; i<len; i++)
				buf[i] = (byte)translateToEBCDIC((int)(buf[i] & 0xff));
		}
		return buf;
	}
	
	/**
	 * Decode an EBCDIC bytes buffer to an ASCII string
	 * @param buf the buffer to decode
	 * @return the ASCII resulting string
	 */
	public static String decodeEBCDICToASCII(byte[] buf) {
		return COBOLUtils.decodeEBCDICToASCII(buf , 0x20);
	}

	public static String decodeEBCDICToASCII(byte[] buf, int nonPrintableCharacter) {
		String s = "";
		if (buf != null) {
			int len = buf.length;
			byte[] ar = new byte[len]; 
			for (int i=0; i<len; i++)
				ar[i] = (byte)translateToASCII((int)(buf[i] & 0xff), nonPrintableCharacter);
			s = new String(ar);
		}
		return s;
	}

	/**
	 * Encode an ASCII string to COBOL 'DISPLAY' format into a bytes buffer
	 * @param s the string to encode
	 * @param type the COBOL picture type
	 * @return the bytes buffer
	 */
	public static byte[] encodeToDISPLAY(String s, int type, int size) {
		byte[] buf = new byte[]{};
		if (s != null) {
			if (type != COBOLUtils.COBOL_PICTURE_X) {
				while (s.length() < size)
					s = " " + s;
			}
			buf = encodeASCIIToEBCDIC(s);
		}
		return buf;
	}
	/**
	 * Decode a COBOL 'DISPLAY' format buffer of bytes to an ASCII string
	 * @param buf the buffer to decode
	 * @param type the COBOL picture type
	 * @return the resulting ASCII string
	 */
	public static String decodeFromDISPLAY(byte[] buf, int type, int size) {
		String s = "";
		if (buf != null) {
			s = decodeEBCDICToASCII(buf);
			if (type != COBOLUtils.COBOL_PICTURE_X) {
				while (s.length() < size)
					s = " " + s;
			}
		}
		return s;
	}
	
	/**
	 * Encode an ASCII string to COBOL 'COMP' format into a bytes buffer
	 * @param s the string to encode
	 * @param type the COBOL picture type
	 * @param size the COBOL picture size
	 * @return the bytes buffer
	 */
	public static byte[] encodeToCOMP(String s, int type, int size) {
		byte[] buf = new byte[]{};
		int nBytes = 0;
		if ((s != null) && !s.equals("")) {
			if (type == COBOLUtils.COBOL_PICTURE_9)
				nBytes = COBOLUtils.COMP_9[size-1];
			if (type == COBOLUtils.COBOL_PICTURE_S9)
				nBytes = COBOLUtils.COMP_S9[size-1];
			if (nBytes > 0) {
				buf = new byte[nBytes];
				s = s.trim();
				s = ((s.charAt(0) == '+') ? s.substring(1):s);
				BigInteger big = new BigInteger(s);
				byte[] ar = big.toByteArray();
				int i = buf.length -1;
				int j = ar.length -1;
				while (i >= 0) {
					if (j >= 0)
						buf[i--] = ar[j--];
					else
						buf[i--] = 0x0000;
				}
			}
		}
		return buf;
	}
	/**
	 * Decode a COBOL 'COMP' format buffer of bytes to an ASCII string
	 * @param buf the buffer to decode
	 * @param type the COBOL picture type
	 * @return the resulting ASCII string
	 */
	public static String decodeFromCOMP(byte[] buf, int type) {
		String s = "";
		if (buf != null) {
			if ((type == COBOLUtils.COBOL_PICTURE_9) || (type == COBOLUtils.COBOL_PICTURE_S9)) {
				BigInteger big = new BigInteger(buf);
				s = big.toString();
				
				if (type == COBOLUtils.COBOL_PICTURE_S9) {
					if (big.equals(big.abs()))
						s = "+" + s;
				}
			}
		}
		return s;
	}

	/**
	 * Encode an ASCII string to COBOL 'COMP-1' format into a bytes buffer
	 * @param s the string to encode
	 * @param type the COBOL picture type
	 * @return the bytes buffer
	 */
	public static byte[] encodeToCOMP1(String s, int type) {
		byte[] buf = new byte[]{};
		if (s != null) {
			
		}
		return buf;
	}
	/**
	 * Decode a COBOL 'COMP-1' format buffer of bytes to an ASCII string
	 * @param buf the buffer to decode
	 * @param type the COBOL picture type
	 * @return the resulting ASCII string
	 */
	public static String decodeFromCOMP1(byte[] buf, int type) {
		String s = "";
		if (buf != null) {
			
		}
		return s;
	}
	
	/**
	 * Encode an ASCII string to COBOL 'COMP-2' format into a bytes buffer
	 * @param s the string to encode
	 * @param type the COBOL picture type
	 * @return the bytes buffer
	 */
	public static byte[] encodeToCOMP2(String s, int type) {
		byte[] buf = new byte[]{};
		if (s != null) {
			
		}
		return buf;
	}
	/**
	 * Decode a COBOL 'COMP-2' format buffer of bytes to an ASCII string
	 * @param buf the buffer to decode
	 * @param type the COBOL picture type
	 * @return the resulting ASCII string
	 */
	public static String decodeFromCOMP2(byte[] buf, int type) {
		String s = "";
		if (buf != null) {
			
		}
		return s;
	}

	/**
	 * Encode an ASCII string to COBOL 'COMP-3' format into a bytes buffer
	 * @param s the string to encode
	 * @param type the COBOL picture type
	 * @return the bytes buffer
	 */
	public static byte[] encodeToCOMP3(String s, int type) {
		byte[] buf = new byte[]{};
		if (s != null) {
			
		}
		return buf;
	}
	/**
	 * Decode a COBOL 'COMP-3' format buffer of bytes to an ASCII string
	 * @param buf the buffer to decode
	 * @param type the COBOL picture type
	 * @return the resulting ASCII string
	 */
	public static String decodeFromCOMP3(byte[] buf, int type) {
		String s = "";
		if (buf != null) {
			
		}
		return s;
	}
	
	/**
	 * Encode an ASCII string to COBOL 'COMP-5' format into a bytes buffer
	 * @param s the string to encode
	 * @param type the COBOL picture type
	 * @return the bytes buffer
	 */
	public static byte[] encodeToCOMP5(String s, int type) {
		byte[] buf = new byte[]{};
		if (s != null) {
			
		}
		return buf;
	}
	/**
	 * Decode a COBOL 'COMP-5' format buffer of bytes to an ASCII string
	 * @param buf the buffer to decode
	 * @param type the COBOL picture type
	 * @return the resulting ASCII string
	 */
	public static String decodeFromCOMP5(byte[] buf, int type) {
		String s = "";
		if (buf != null) {
			
		}
		return s;
	}
	
	/**
	 * Encode an ASCII string to COBOL 'POINTER' format into a bytes buffer
	 * @param s the string to encode
	 * @param type the COBOL picture type
	 * @return the bytes buffer
	 */
	public static byte[] encodeToPOINTER(String s, int type) {
		byte[] buf = new byte[]{};
		if (s != null) {
			
		}
		return buf;
	}
	/**
	 * Decode a COBOL 'POINTER' format buffer of bytes to an ASCII string
	 * @param buf the buffer to decode
	 * @param type the COBOL picture type
	 * @return the resulting ASCII string
	 */
	public static String decodeFromPOINTER(byte[] buf, int type) {
		String s = "";
		if (buf != null) {
			
		}
		return s;
	}
	
	/**
	 * Padd a string
	 * @param s the string
	 * @param size the maximum size of string
	 * @param type the COBOL picture type
	 * @return the padded string
	 */
	public static String padd(String s, int size, int type) {
		String resu = null;
		if (s != null) {
			resu = s;
			if (type != COBOLUtils.COBOL_PICTURE_X) {
				/*while (resu.length() < size)
					resu = " " + resu;*/
				return resu;
			} else {
				while (resu.length() < size)
					resu = resu + " ";
				return resu;
			}
		}
		return s;		
	}
	
	/**
	 * Encode an ASCII string to a COBOL format into a bytes buffer
	 * @param s the string to encode
	 * @param picture the COBOL picture string
	 * @param format the COBOL format string
	 * @return the bytes buffer
	 */
	public static byte[] encode(String s, String picture, String format) {
		byte[] buf = new byte[]{};
		if (s != null) {
			
			int type = getPictureType(picture);
			int size = getPictureSize(picture);
			int code = getFormatType(format);
			
			s = padd(s,size,type);
			
			if (type == COBOL_PICTURE_X) {
				if ((size == 1) && (s.length()>1)) {
					int index = s.toUpperCase().indexOf("X");
					try {
						char c = (char)Integer.parseInt(s.substring(index+1),16);
						s = String.valueOf(c);
					}
					catch (NumberFormatException e) {;}
				}
				buf = encodeASCIIToEBCDIC(s);
			}
			else {
				switch (code) {
					case COBOL_FORMAT_COMP:   	buf = encodeToCOMP(s,type,size); break;//encodeToCOMP(s,type); break;
					case COBOL_FORMAT_COMP_1: 	buf = encodeToCOMP1(s,type); break;
					case COBOL_FORMAT_COMP_2: 	buf = encodeToCOMP2(s,type); break;
					case COBOL_FORMAT_COMP_3: 	buf = encodeToCOMP3(s,picture + " " + format); break;//buf = encodeToCOMP3(s,type); break;
					case COBOL_FORMAT_COMP_5: 	buf = encodeToCOMP5(s,type); break;
					case COBOL_FORMAT_POINTER:	buf = encodeToPOINTER(s,type); break;
					case COBOL_FORMAT_DISPLAY :
					default:					buf = encodeToDISPLAY(s,type,size); break;
				}
			}
		}
		return buf;
	}
	/**
	 * Decode a COBOL buffer of bytes to an ASCII string
	 * @param buf the buffer to decode
	 * @param picture the COBOL picture string
	 * @param format the COBOL format string
	 * @return the bytes buffer
	 */
	public static String decode(byte[] buf, String picture, String format) {
		String s = "";
		if (buf != null) {
			
			int type = getPictureType(picture);
			int size = getPictureSize(picture);
			int code = getFormatType(format);
			
			if (type == COBOL_PICTURE_X) {
				s = decodeEBCDICToASCII(buf);
			}
			else {
				switch (code) {
					case COBOL_FORMAT_COMP:   	s = decodeFromCOMP(buf,type); break;
					case COBOL_FORMAT_COMP_1: 	s = decodeFromCOMP1(buf,type); break;
					case COBOL_FORMAT_COMP_2: 	s = decodeFromCOMP2(buf,type); break;
					case COBOL_FORMAT_COMP_3: 	s = decodeFromCOMP3(buf,picture);break;//s = decodeFromCOMP3(buf,type); break;
					case COBOL_FORMAT_COMP_5: 	s = decodeFromCOMP5(buf,type); break;
					case COBOL_FORMAT_POINTER:	s = decodeFromPOINTER(buf,type); break;
					case COBOL_FORMAT_DISPLAY :
					default:					s = decodeFromDISPLAY(buf,type,size); break;
				}
				
				s = formatString(s,picture);
			}
		}
		return s;
	}

	public static byte[] encodeToCOMP3(String str, String picture) throws NumberFormatException {
		long value = Long.valueOf(str).longValue();
		String result = String.valueOf(value);
		int bufferSize = value>0 ? (result.length()+2)/2:(result.length()+1)/2;
		byte[] bufferArray = new byte[bufferSize];
		int nbZeros = 0;
		boolean hi = true;
		int lastp = 0;
		int v9 = 0;
		int x = 0;
		boolean signed = false;
		@SuppressWarnings("unused")
		char compAttribute = ' ';

		// remove leading and trailing spaces
		picture = picture.trim();
		
		if (picture.startsWith("PIC", 0))
			picture = picture.substring(4);			
		
		for(int index=0; index<picture.length(); index++) {
			switch(picture.charAt(index)) {
				case 'S':
					// first deal with SYNC attribute
					if (picture.startsWith("SYNC", index)) {
						index += 3;
						break;
					}
				
					// deal with sign
					signed = true;
					
					if (index != 0)
						throw new NumberFormatException();
					
					index++;
					//
					// FALL HTRU
					//

			case '9':
					// count the number of 9s
					for(index++, bufferSize=1; (index<picture.length()) && (picture.charAt(index) == '9'); index++)
							bufferSize++;
					
					// there should be at least one 9
					if (bufferSize < 1)
						throw new NumberFormatException();
					
					// is there some other parameters
					if (index < picture.length()) {						
						if (picture.charAt(index++) == '(') {	// yes, there is a opening parenthesis
							if (bufferSize > 1)
								throw new NumberFormatException();
							
							if ((lastp = picture.indexOf(")", index)) == -1) // look for closing matching parenthesis
								throw new NumberFormatException();
							
							// compute the value
							bufferSize = ((Integer.valueOf(picture.substring(index, lastp)).intValue()+2)/2);
							
							// adjust pointers
							index = lastp;
						}
					}
					
					if (bufferSize > 18)
						throw new NumberFormatException();
					break;

				case 'V':
					for(index++, v9=0; (index<picture.length()) && (picture.charAt(index) == '9'); index++)
							v9++;

					// there should be at least one 9
					if (v9 < 1)
						throw new NumberFormatException();

					// is there some other parameters
					if (index < picture.length()) {
						if (picture.charAt(index++) == '(') {	// yes, there is a opening parenthesis
							if ((lastp = picture.indexOf(")", index)) == -1) // look for closing matching parenthesis
								throw new NumberFormatException();
							
							// compute the value
							v9 = Integer.valueOf(picture.substring(index, lastp)).intValue();
							
							// adjust pointers
							index = lastp;
						}
					}
					break;

				case 'X':
					// count the number of Xs
					for(index++, x=0, bufferSize=0; (index<picture.length()) && (picture.charAt(index) == 'X'); index++)
							x++;

					if (x < 1)
						throw new NumberFormatException();

					// is there some other parameters
					if (index < picture.length()) {
						if (picture.charAt(++index) == '(') {	// yes, there is a opening parenthesis
							if (x > 1)
								throw new NumberFormatException();							
							
							if ((lastp = picture.indexOf(")", index)) == -1) // look for closing matching parenthesis
								throw new NumberFormatException();
							
							// compute the value
							x = (Integer.valueOf(picture.substring(index, lastp)).intValue()+2)/2;
							
							// adjust pointers
							index = lastp;
						}
					}
					
					if (x > 18)
						throw new NumberFormatException();
					break;

				case 'C':
					if (picture.startsWith("COMPUTATIONAL-", index)) {
						index += 14;
						
						if (index >= picture.length())
							throw new NumberFormatException();
						
						char c = picture.charAt(index++);
						if ((c == '3') || (c == '4') || (c == '5') || (c == 'X'))
							compAttribute = c;
						else
							throw new NumberFormatException();
					}
					else
					if (picture.startsWith("COMP-", index)) {
						index += 5;

						if (index >= picture.length())
							throw new NumberFormatException();

						char c = picture.charAt(index++);
						if ((c == '3') || (c == '4') || (c == '5') || (c == 'X'))
							compAttribute = c;
						else
							throw new NumberFormatException();
					}
					else
					if (picture.startsWith("COMPUTATIONAL", index))
						index += 13;
					else
					if (picture.startsWith("COMP", index))
						index += 3;
					break;

				case 'D':
					if (picture.startsWith("DISPLAY", index)) {
						index += 6;
					}
					break;

				case ' ':					
					break;

				case '.':
					index = picture.length();
					break;
			}				
		}

		// re-allocate buffer with new size
		if (x != 0)
			bufferSize = x;
		bufferArray = new byte[bufferSize];
		
		// store the sign character in array
		
		if (!signed)
			bufferArray[bufferArray.length-1] = 0x0f;
		else {
			if (str.indexOf('-') != -1) {
				bufferArray[bufferArray.length-1] = 0x0d;
				result = String.valueOf(Math.abs(value));
			}
			else
				bufferArray[bufferArray.length-1] = 0x0c;
		}
		
		if ((nbZeros = (bufferArray.length*2)-1-result.length()) > 0)
			result = new String("000000000000000000").substring(0, nbZeros)+result;

		try {
			// store the value in array by nibble
			for(int i=0; i<bufferArray.length*2-1; i++) {
				bufferArray[i/2] |= (byte)((hi)
												 ? ((result.charAt(i)-'0') << 4) & 0x00f0
												 : (result.charAt(i)-'0'));
				hi = hi ? false:true;
			}
		}
		catch(Exception e) {
			throw new NumberFormatException();
		}

		return bufferArray;
	}

	public static String decodeFromCOMP3(byte[] bufferArray, String picture) throws NumberFormatException {
		String result = "";
		int  len = bufferArray.length*2-1;
		boolean hi = true;
		
		try {
			for(int i=0; i<len; i++) {
				result += (char)('0'+(bufferArray[(i+2)/2-1] >> (hi ? 4:0) & 0x0f));
				hi = hi ? false:true;
			}
		}
		catch(Exception e) {
			throw new NumberFormatException();
		}
		
		result = String.valueOf(Long.valueOf(result).longValue());
		
		if ((bufferArray[bufferArray.length-1] & 0x0f) == 0x0c)
			result = '+'+result;
		else
		if ((bufferArray[bufferArray.length-1] & 0x0f) == 0x0d)
			result = '-'+result;
			
		return result;
	}
	
	/**
	 * Retrieve the COBOL format constant value
	 * @param sFormat the format string
	 * @return the value
	 */
	public static int getFormatType(String sFormat) {
		int format = 0;
		
		if (sFormat.equalsIgnoreCase("DISPLAY"))
			return COBOL_FORMAT_DISPLAY;
		if (sFormat.equalsIgnoreCase("COMP"))
			return COBOL_FORMAT_COMP;
		if (sFormat.equalsIgnoreCase("COMP-1"))
			return COBOL_FORMAT_COMP_1;
		if (sFormat.equalsIgnoreCase("COMP-2"))
			return COBOL_FORMAT_COMP_2;
		if (sFormat.equalsIgnoreCase("COMP-3"))
			return COBOL_FORMAT_COMP_3;
		if (sFormat.equalsIgnoreCase("COMP-5"))
			return COBOL_FORMAT_COMP_5;
		if (sFormat.equalsIgnoreCase("POINTER"))
			return COBOL_FORMAT_POINTER;
		
		return format;
	}
	
	/**
	 * Retrieve the COBOL picture constant value
	 * @param picture the picture string
	 * @return the value
	 */
	public static int getPictureType(String picture) {
/*
		Pattern p = null;
		String s = null;
		int type = 0;
		
		if (picture == null)
			return 0;
			
		// X(5) or XXXXX
		Pattern pX = Pattern.compile("X\\((\\d++)\\)|(X++)");
		Matcher mX = pX.matcher(picture);
		if (mX.matches())
			return COBOL_PICTURE_X;
		
		// 9(5) or 99999
		Pattern p9 = Pattern.compile("9\\((\\d++)\\)|(9++)");
		Matcher m9 = p9.matcher(picture);
		if (m9.matches())
			return COBOL_PICTURE_9;
		
		// S9(5) or S99999
		Pattern pS9 = Pattern.compile("S9\\((\\d++)\\)|S(9++)");
		Matcher mS9 = pS9.matcher(picture);
		if (mS9.matches())
			return COBOL_PICTURE_S9;
		
		return type;
*/
		String s = picture.trim();
		
		if (s.indexOf("X") != -1)
			return COBOL_PICTURE_X;
		
		if (s.indexOf("S") != -1) {
			if (s.indexOf("V") != -1)
				return COBOL_PICTURE_S9V9;
			else
				return COBOL_PICTURE_S9;
		}
		
		if (s.indexOf("V") != -1)
			return COBOL_PICTURE_9V9;
		else
			return COBOL_PICTURE_9;
	}

	/**
	 * Retrieve the size of a given COBOL picture
	 * @param picture the picture string
	 * @return the size value
	 */
	public static int getPictureSize(String picture) {
/*
		Pattern p = null;
		Matcher m = null;
		String s = null;
		int size = 0;

		if (picture == null)
			return 0;
		
		// X(5) or XXXXX
		Pattern pX = Pattern.compile("X\\((\\d++)\\)|(X++)");
		Matcher mX = pX.matcher(picture);
		
		// 9(5) or 99999
		Pattern p9 = Pattern.compile("9\\((\\d++)\\)|(9++)");
		Matcher m9 = p9.matcher(picture);
		
		// S9(5) or S99999
		Pattern pS9 = Pattern.compile("S9\\((\\d++)\\)|S(9++)");
		Matcher mS9 = pS9.matcher(picture);

		if (mX.matches())
			m = mX;
		if (m9.matches())
			m = m9;
		if (mS9.matches())
			m = mS9;
			
		if (m != null) {
			s = m.group(1);
			if (s != null) size = Integer.parseInt(s);
			s = m.group(2);
			if (s != null) size = s.length();
			return size;
		}
		
		return 0;
*/
		int size = 0;
		if ((picture != null) && (picture.length() > 0)) {
			char c = picture.charAt(0);
			if (c != '('){
				if ((c != 'S') && (c != 'V'))
					size++;
				size += getPictureSize(picture.substring(1));
			}
			else {
				int index = picture.indexOf(")");
				if (index != -1) {
					size += Integer.parseInt(picture.substring(1,index),10) - 1;
					size += getPictureSize(picture.substring(index+1));
				}
			}
			
		}
		return size;
	}
	
	public static int getSizeInBytes(String picture, String format) {
		int type = getPictureType(picture);
		int size = getPictureSize(picture);
		int code = getFormatType(format);
		
		if (picture == "")
			return 0;
		
		if (type == COBOL_PICTURE_X) {
			return size;
		}
		else {
			switch (code) {
				case COBOL_FORMAT_COMP_3:
					return (size/2)+1;
				case COBOL_FORMAT_COMP:
					if (type == COBOL_PICTURE_9)
						return COMP_9[size-1];
					if (type == COBOL_PICTURE_S9)
						return COMP_S9[size-1];
				case COBOL_FORMAT_COMP_1:
				case COBOL_FORMAT_COMP_2:
				case COBOL_FORMAT_COMP_5:
				case COBOL_FORMAT_POINTER:
				case COBOL_FORMAT_DISPLAY :
				default:
					return size;
			}
		}
	}
	
	public static String formatString(String s,String picture) {
		String ret = s;
		if (ret != null) {
			String begin, end;
			int index, i, j, k;
			char c;
			if ((index = picture.indexOf("V")) != -1) {
				i = getPictureSize(picture.substring(0,index));
				if (i > 0) {
					begin = formatString(s.substring(0,i),picture.substring(0,index));
					end = formatString(s.substring(i),picture.substring(index+1));
					ret = begin + "," + end;
				}
			}
			else if ((index = picture.indexOf("Z")) != -1) {
				String zReplace = "";
				if (index == 0) {
					ret = "";
					if (picture.charAt(index+1) == '(') {
						if ((j = picture.indexOf(")")) != -1) {
							i = getPictureSize(picture.substring(0,j+1));
							for (k=0; k<i; k++) {
								if ((c = s.charAt(k)) == '0')
									ret += zReplace;
								else
									break;
							}
							ret += formatString(s.substring(k),picture.substring(j+1));
						}
					}
					else {
						i = 0;
						while ((i < picture.length()) && (picture.charAt(i) == 'Z')) {
							if ((c = s.charAt(i)) == '0')
								ret += zReplace;
							else
								ret += c;
							i++;
						}
						ret += formatString(s.substring(i),picture.substring(i));
					}
				}
			}
			else if ((index = picture.indexOf("9")) != -1) {
				
			}
		}
		return ret;
	}

/*	
	private static void testStringASCII_EBCDIC(String s) {
		byte[] buf = encodeASCIIToEBCDIC(s);
		for (int i=0;i<buf.length;i++)
		   System.out.println(Integer.toHexString(buf[i]));
			   
		String ret = decodeEBCDICToASCII(buf);
		System.out.println(ret+"\n");
	}

	private static void testCOBOLPicture() {
		System.out.println(getPictureType("X(5)") + ":" + getPictureSize("X(5)"));
		System.out.println(getPictureType("XXXX") + ":" + getPictureSize("XXXX"));
		System.out.println(getPictureType("X") + ":" + getPictureSize("X"));
		System.out.println(getPictureType("9(5)") + ":" + getPictureSize("9(5)"));
		System.out.println(getPictureType("9999") + ":" + getPictureSize("9999"));
		System.out.println(getPictureType("9") + ":" + getPictureSize("9"));
		System.out.println(getPictureType("S9(5)") + ":" + getPictureSize("S9(5)"));
		System.out.println(getPictureType("S9999") + ":" + getPictureSize("S9999"));
		System.out.println(getPictureType("S9") + ":" + getPictureSize("S9"));
	}
	
	private static void testWordBoundary(String line){
		Pattern p = Pattern.compile("\\s");
		String[] words = p.split(line);
		for (int i=0;i<words.length;i++)
			System.out.println(i+ ":" + words[i]);
	}
	
	private static void testCOMP3(String s) {
		byte[] buf = encode(s,"S9(12)","COMP-3");
		for (int i=0;i<buf.length;i++)
			System.out.print(Integer.toHexString(buf[i]));
		System.out.print("\n");
		
		String resu = decode(buf,"S9(12)","COMP-3");
		System.out.println(resu);
	}
	
	private static void testCOMP(String s) {
		byte[] buf = encodeToCOMP(s,COBOLUtils.COBOL_PICTURE_S9,4);
		for (int i=0;i<buf.length;i++)
			System.out.println(Integer.toHexString(buf[i]));
		System.out.print("\n");
		
		String resu = decodeFromCOMP(buf,COBOLUtils.COBOL_PICTURE_S9);
		System.out.println(resu);
	}

	public static void main(String[] args) {
		//testStringASCII_EBCDIC("123");
		//testCOBOLPicture();
		//testWordBoundary("77 CICS-ECI-SYSTEM-MAX		PIC 9(4) COMP-5 VALUE 8.");
		//testCOMP3("123456789");
		//testCOMP("+999");
		
		String s = formatString("00001289","Z(5)9V9(2)");
		System.out.println(s);
		String s1 = formatString("00019","ZZZZZ");
		System.out.println(s1);
		String s2 = formatString("00019","Z(5)");
		System.out.println(s2);
	}
*/
}
