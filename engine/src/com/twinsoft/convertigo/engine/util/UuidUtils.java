/*
 * Copyright (c) 2001-2022 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.util;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class UuidUtils {
	
	public static UUID toUUID(String str) {
		str = str.toLowerCase();
		StringBuffer uuid = new StringBuffer();
		Pattern noHex = Pattern.compile("(?:([g-z])|([^0-9a-f])|$)");
		Matcher mHex = noHex.matcher(str);
		int id = 0;
		while (mHex.find() && uuid.length() < 32) {
			uuid.append(str.subSequence(id, mHex.start()));
			String g = mHex.group(1);
			if (g != null) {
				uuid.append(Integer.toHexString(g.charAt(0)));
			}
			id = mHex.end();
		} 
		str = StringUtils.leftPad(uuid.substring(0, Math.min(uuid.length(), 32)), 32, '0');
		str = str.replaceFirst("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5");
		return UUID.fromString(str);
	}

}
