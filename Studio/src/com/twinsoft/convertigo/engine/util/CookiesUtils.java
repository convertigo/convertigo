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

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpState;

import com.twinsoft.convertigo.engine.Engine;

public class CookiesUtils {

	public static String formatCookie(Cookie cookie) {
		StringBuffer buf = new StringBuffer();
		Date d = cookie.getExpiryDate();
		String[][] datas = {
				// {"$Version",Integer.toString(cookie.getVersion())},
				{ cookie.getName(), cookie.getValue() }, { "$Domain", cookie.getDomain() },
				{ "$Path", cookie.getPath() }, { "$Secure", Boolean.toString(cookie.getSecure()) },
				{ "$Date", d == null ? "null" : DateFormat.getDateTimeInstance().format(d) } };
		buf.append(datas[0][0] + "=" + datas[0][1]);
		for (int i = 1; i < datas.length; i++) {
			if (datas[i][1] != null)
				buf.append("; " + datas[i][0] + "=" + datas[i][1]);
		}
		return buf.toString();
	}

	public static void addCookie(HttpState httpState, String cook) {
		String name = "";
		String domain = "";
		String path = "";
		String value = "";
		boolean secure = false;
		Date expires = new Date(Long.MAX_VALUE);

		String[] fields = cook.split(";");
		for (int i = 0; i < fields.length; i++) {
			String[] half = fields[i].trim().split("=");
			if (half.length == 2) {
				if (fields[i].startsWith("$")) {
					if (half[0].equals("$Domain"))
						domain = half[1];
					else if (half[0].equals("$Path"))
						path = half[1];
					else if (half[0].equals("$Secure"))
						secure = Boolean.getBoolean(half[1]);
					else if (half[0].equals("$Date"))
						try {
							expires = DateFormat.getDateTimeInstance().parse(half[1]);
						} catch (ParseException e) {
						}
				} else {
					name = half[0];
					value = half[1];
				}
			}
		}

		Cookie cookie = null;
		try {
			cookie = new Cookie(domain, name, value, path, expires, secure);
			if (cookie != null)
				httpState.addCookie(cookie);
		} catch (Exception e) {
			Engine.logBeans.debug("(CookiesUtils) failed to parse cookie: " + cook);
		}
	}

	public static void addCookie(HttpState httpState, String domain, String name, String value,
			String path, Date expires, boolean secure) {
		Cookie cookie = null;
		try {
			Engine.logBeans.debug(String.format("(CookiesUtils) Adding cookie: " +
					"domain=%s, name=%s, value=%s, path=%s, expires=%s, secure=%s",
					domain, name, value, path, expires.toString(), Boolean.toString(secure)));
			
			cookie = new Cookie(domain, name, value, path, expires, secure);
			if (cookie != null) {
				Engine.logBeans.debug("(CookiesUtils) added cookie: " + cookie);
				httpState.addCookie(cookie);
			}
		} catch (Exception e) {
			Engine.logBeans.debug("(CookiesUtils) failed to parse cookie: " + cookie);
		}
	}
}
