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

package com.twinsoft.convertigo.engine.proxy.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import com.twinsoft.convertigo.engine.Engine;

public class ClientCookie {
	private String domain;
	private String path;
	private long expireMillis;

	private String name;
	private String value;

	private String serverCookieStr;

	static private SimpleDateFormat rfc1123Parser = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
	static private SimpleDateFormat rfc850Parser = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss z", Locale.US);
	static private SimpleDateFormat asctimeParser = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.US);
	static private SimpleDateFormat dateFormat1 = new SimpleDateFormat("dd-MMM-yyyy HH:mm z", Locale.US);
	static private Calendar calendar = Calendar.getInstance();
	static {
		dateFormat1.setTimeZone(TimeZone.getTimeZone("EST"));
		calendar.setLenient(false);
	}

	public ClientCookie(String cookieStr, String reqHost, String reqPath) {
		if (reqHost == null)
			throw new IllegalArgumentException("no request host found for cookie.");

		if (reqPath == null || reqPath.length() == 0)
			reqPath = "/";

		// initial variables
		domain = path = null;
		expireMillis = -1;

		// parse cookie string
		String[] pair = new String[2];

		int pos = parsePair(cookieStr, 0, pair);
		name = pair[0];
		value = pair[1];
		if (name == null || value == null)
			throw new IllegalArgumentException("no NAME-VALUE found in cookie.");

		while (pos < cookieStr.length()) {
			pos = parsePair(cookieStr, pos, pair);

			if (pair[1] == null)
				continue;

			if (pair[0].equalsIgnoreCase("domain"))
				domain = pair[1];
			else if (pair[0].equalsIgnoreCase("path"))
				path = pair[1];
			else if (pair[0].equalsIgnoreCase("expires"))
				expireMillis = calcExpireDate(pair[1]);
			else if (pair[0].equalsIgnoreCase("max-age"))
				expireMillis = calcExpireSecond(pair[1]);
		}

		// compose of serverCookieStr
		pos = cookieStr.indexOf(';');
		serverCookieStr = (pos < 0 ? cookieStr : cookieStr.substring(0, pos));

		// reject cookie in errors and set default values
		if (path == null) {
			int p = reqPath.lastIndexOf('/');
			if (p <= 0)
				path = "/";
			else
				path = reqPath.substring(0, p);
		}
		else if (!reqPath.startsWith(path))
			throw new IllegalArgumentException("cookie path does not match the path in URL.");

		if (domain == null)
			domain = reqHost;
		else if (!matchedBy(reqHost))
			throw new IllegalArgumentException("request host does not domain-match the cookie domain.");
	}

	private int parsePair(String str, int startPos, String[] pair) {
		int endPos = str.indexOf(';', startPos);
		if (endPos < 0)
			endPos = str.length();

		int pos = str.indexOf('=', startPos);
		if (pos < 0 || pos > endPos) {
			pair[0] = str.substring(startPos, endPos).trim();
			pair[1] = null;
		}
		else {
			pair[0] = str.substring(startPos, pos).trim();
			pair[1] = str.substring(pos + 1, endPos).trim();
		}

		return endPos + 1;
	}

	private long calcExpireDate(String dateStr) {
		try {
			Date expires = null;

			if (dateStr.endsWith("GMT")) {
				if (dateStr.indexOf('-') > -1)
					expires = rfc850Parser.parse(dateStr);
				else
					expires = rfc1123Parser.parse(dateStr);
			}
			else {
				expires = asctimeParser.parse(dateStr);
			}

			calendar.setTime(expires);
			if (calendar.get(Calendar.YEAR) < 10)
				calendar.add(Calendar.YEAR, 2000);

			return calendar.getTimeInMillis();
		}
		catch (Exception e) {
			Engine.logEngine.error("Unexpected exception", e);
			throw new IllegalArgumentException("cookie expire date in wrong format.");
		}
	}

	private long calcExpireSecond(String seconds) {
		try {
			long sec = Long.parseLong(seconds);
			if (sec <= 0)
				return sec;

			return System.currentTimeMillis() + sec * 1000;
		}
		catch (Exception e) {
			Engine.logEngine.error("Unexpected exception", e);
			throw new IllegalArgumentException("cookie expire seconds in wrong format.");
		}
	}

	public String getDomain() {
		return domain;
	}

	public String getPath() {
		return path;
	}

	public long getExpireTime() {
		return expireMillis;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

	public boolean isExpired() {
		return expireMillis > 0 && System.currentTimeMillis() >= expireMillis;
	}

	public boolean isTerminated() {
		return expireMillis == 0;
	}

	static public String calcDomainRoot(String fullDomain) {
		if (fullDomain == null || fullDomain.trim().length() == 0)
			return null;

		boolean allDigits = true;
		int flds = 0;
		int rootDotPos = -1;

		for (int k = fullDomain.length() - 1; k >= 0; k--) {
			char ch = fullDomain.charAt(k);

			if (ch != '.' && allDigits)
				allDigits = (ch >= '0' && ch <= '9');

			if (ch == '.' || k == 0) {
				flds++;
				if (flds <= 2) {
					if (ch == '.')
						rootDotPos = k;
					else
						rootDotPos = -1;
				}
			}
		}

		if (flds == 4 && allDigits)
			return fullDomain;

		if (rootDotPos == -1)
			return fullDomain;

		return fullDomain.substring(rootDotPos + 1);
	}

	public String toServerCookieString() {
		return serverCookieStr;
	}

	public boolean matchedBy(String fullDomain) {
		return fullDomain.equals(domain) || fullDomain.endsWith(domain);
	}

	public boolean equals(Object obj) {
		if (obj instanceof ClientCookie) {
			ClientCookie cookie = (ClientCookie) obj;
			return domain.equals(cookie.domain) && path.equals(cookie.path) && name.equals(cookie.name);
		}

		return false;
	}

	public String toString() {
		return toString("");
	}

	public String toString(String marginSpaces) {
		return marginSpaces + "ClientCookie: " + serverCookieStr + "; domain=" + domain +
			"; path=" + path + "; expires=" + (expireMillis > 0 ? dateFormat1.format(new Date(expireMillis)) : String.valueOf(expireMillis));
	}
}