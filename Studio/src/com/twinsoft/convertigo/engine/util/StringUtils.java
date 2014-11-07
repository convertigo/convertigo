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

import java.io.PrintWriter;
import java.io.StringWriter;

import com.twinsoft.util.*;

public class StringUtils {
    
    public static String pad(String msg, char paddingChar, boolean bRight, int maxLength) {
        int len = msg.length();
        for (int i = len ; i < maxLength ; i++) {
            if (bRight) {
                msg += paddingChar;
            }
            else {
                msg = paddingChar + msg;
            }
        }
        return msg;
    }

    public static String leftPadWithZero(int i, int length) {
    	if (length < 0) throw new IllegalArgumentException("length < 0");
    	
    	String padZero = "";
    	for (int j = 0 ; j < length ; j++) {
    		padZero += "0";
    	}
		return java.text.MessageFormat.format("{0,number," + padZero + "}", new Object[] { new Integer(i) });
    }
    
    public static String escape(String str) {
        StringEx sx = new StringEx(str);
        sx.replaceAll("&", "&#38;");
        sx.replaceAll("\"", "&#34;");
        sx.replaceAll("<", "&#60;");
        sx.replaceAll(">", "&#62;");
        sx.replaceAll("^", "&#94;");
        sx.replaceAll("%", "&#37;");
        return sx.toString();
    }
    
    /**
     * Normalizes a string, i.e. replaces all blank spaces by underline character,
     * all accentuated characters by their unaccentuated character, and makes
     * the first character non digit if needed. It also deletes starting and
     * trailing spaces.
     *
     * @param text the text to normalize.
     *
     * @return the normalized text.
     */
    public static String normalize(String text) {
        return StringUtils.normalize(text, true);
    }
    
    /**
     * Normalizes a string, i.e. replaces all blank spaces by underline character,
     * all accentuated characters by their unaccentuated character, and makes
     * the first character non digit if needed. It also deletes starting and
     * trailing spaces.
     *
     * @param text the text to normalize.
     * @param bIncludeNonAlphanumericCharacters defines if non alphanumeric characters
     * should be included (as '_' character).
     *
     * @return the normalized text.
     */
    public static String normalize(String text, boolean bIncludeNonAlphanumericCharacters) {
        // First trim the text
        text = text.trim();
        
        if (text.length() == 0)
            return ("");
        
        char[] aText = new char[text.length()];
        
        int strLen = text.length();
        char c;
        
        int len = 0;
        
        for (int i = 0 ; i < strLen ; i++) {
            c = text.charAt(i);
            
            if (c == ' ') {
                aText[len++] = '_';
            }
            else if ((c == 'à') || (c == 'â') || (c == 'ä')) {
                aText[len++] = 'a';
            }
            else if ((c == 'é') || (c == 'è') || (c == 'ê') || (c == 'ë')) {
                aText[len++] = 'e';
            }
            else if ((c == 'î') || (c == 'ï')) {
                aText[len++] = 'i';
            }
            else if ((c == 'ô') || (c == 'ö')) {
                aText[len++] = 'o';
            }
            else if ((c == 'ù') || (c == 'û') || (c == 'ü')) {
                aText[len++] = 'u';
            }
            else if ((c == 'ÿ')) {
                aText[len++] = 'y';
            }
            else if ((c == 'ç')) {
                aText[len++] = 'c';
            }
            else if ((c >= (char) 48) && (c <= (char) 57)) { // Numbers
                aText[len++] = c;
            }
            else if ((c >= (char) 65) && (c <= (char) 90)) { // Uppercase letters
                aText[len++] = c;
            }
            else if ((c >= (char) 97) && (c <= (char) 122)) { // Lowercase letters
                aText[len++] = c;
            }
            else if (bIncludeNonAlphanumericCharacters) {
                aText[len++] = '_';
            }
        }
        
        String res = new String(aText, 0, len);
        
        // First char must only be a letter, if not '_' is legal
        if ((res.length() > 0) && (Character.isDigit(res.charAt(0)))) {
            res = "_" + res;
        }
        
        return res;
    }
    
    public static boolean isNormalized(String txt){
    	return StringUtils.normalize(txt, true).equalsIgnoreCase(txt);
    }
    
    public static String join(String[] strings, String separator){
    	StringBuffer sb = new StringBuffer();
    	if(strings.length>0){
    		sb.append(strings[0]);
    		for(int i=1;i<strings.length;i++) sb.append(separator).append(strings[i]);
    	}
    	return sb.toString();
    }
    
    /**
     * Format ts from "0m00" to "59m59", then "1h00" to "xxxxh59"
     * 
     * @param ts
     * @return
     */
    public static String timestampToPrettyString(long ts){
		long s = (ts/=1000)%60;
		long m = (ts/=60)%60;
		long h = ts/60;
		return (h>0?(h+"h"):"")+((m<10&&h>0)?"0":"")+m+(h==0?("m"+((s<10)?"0":"")+s):"");
    }
    
    public static String readStackTrace(Exception e) { 
        StringWriter sw = new StringWriter(); 
        PrintWriter pw = new PrintWriter(sw); 
        e.printStackTrace(pw); 
        return sw.toString(); 
    }

    public static String readStackTraceCauses(Exception e) {
    	String s = e.getMessage();
    	Throwable cause = e.getCause();
    	while (cause != null) {
    		s += "\n"+ cause.getMessage();
    		cause = cause.getCause();
    	}
    	return s;
    }
    
	/*
	 * Justify a string, here only left justification is implemented
	 */
	public static String justifyLeft(int width, String st) {
		StringBuffer buf = new StringBuffer(st);
		int lastspace = -1;
		int linestart = 0;
		int i = 0;

		while (i < buf.length()) {
			if (buf.charAt(i) == ' ')
				lastspace = i;
			if (buf.charAt(i) == '\n') {
				lastspace = -1;
				linestart = i + 1;
			}
			if (i > linestart + width - 1) {
				if (lastspace != -1) {
					buf.setCharAt(lastspace, '\n');
					linestart = lastspace + 1;
					lastspace = -1;
				} else {
					buf.insert(i, '\n');
					linestart = i + 1;
				}
			}
			i++;
		}
		return buf.toString();
	}

	public static String reduce(String str, int max) {
		if (str.length() > max) {
			return str.subSequence(0, max - 1) + "…";
		}
		return str;
	}
}
