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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexpUtils {
	public final static Pattern pattern_and = Pattern.compile("&");
	public final static Pattern pattern_equals = Pattern.compile("=");
	
	static public String inject(Matcher matcher, String injection) {
		if (matcher.find()) {
			injection = Matcher.quoteReplacement(injection);
			StringBuffer sb = new StringBuffer();
			String sub = matcher.group();
			int id = matcher.start(1) - matcher.start();
			String sub_start = sub.substring(0, id);
			String sub_end = sub.substring(id, sub.length());
			sub = sub_start + injection + sub_end;
			matcher.appendReplacement(sb, sub);
			matcher.appendTail(sb);
			return sb.toString();
		}
		return null;
	}
}
