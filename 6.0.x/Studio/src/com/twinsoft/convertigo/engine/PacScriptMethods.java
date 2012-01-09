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

package com.twinsoft.convertigo.engine;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;

public class PacScriptMethods {
	enum jsFunctions {
		shExpMatch, 
		dnsResolve, 
		isResolvable,
		isInNet, 
		dnsDomainIs, 
		isPlainHostName, 
		myIpAddress,
		dnsDomainLevels,
		localHostOrDomainIs, 
		weekdayRange, 
		dateRange, 
		timeRange;
	}

	public static final String OVERRIDE_LOCAL_IP = "com.btr.proxy.pac.overrideLocalIP";

	private final static String GMT = "GMT";

	private final static List<String> DAYS = Collections.unmodifiableList(
			Arrays.asList("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")); 

	private final static List<String> MONTH = Collections.unmodifiableList(
			Arrays.asList("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"));

	private static Calendar currentTime; 



	public PacScriptMethods() {
		super();
	}

	public boolean isPlainHostName(String host) {
		return host.indexOf(".") < 0;
	}


	public boolean dnsDomainIs(String host, String domain) {
		return host.endsWith(domain);
	}

	public boolean localHostOrDomainIs(String host, String domain) {
		return domain.startsWith(host);
	}


	public boolean isResolvable(String host) {
		try {
			InetAddress.getByName(host).getHostAddress();
			return true;
		} catch (UnknownHostException ex) {
			Engine.logProxyManager.debug("Proxy auto config error : Hostname not resolvable " + host);
			// Not resolvable
		}
		return false;
	}


	public boolean isInNet(String host, String pattern, String mask) {
		// int count= number of 255's in mask
		int count = 0;
		int startPos = 0;
		while ((startPos = mask.indexOf("255", startPos + 1)) > -1) {
			count++;
		}

		// String tokenize host and pattern with "." as delimiter
		StringTokenizer hostTok = new StringTokenizer(host, ".");
		StringTokenizer patternTok = new StringTokenizer(pattern, ".");

		for (int i = 0; i <= count; i++) {
			if ((!hostTok.hasMoreTokens()) || (!patternTok.hasMoreTokens())) {
				return false;
			}
			if (!(hostTok.nextToken()).equals(patternTok.nextToken())) {
				return false;
			}
		}
		return true;
	}


	public String dnsResolve(String host) {
		try {
			return InetAddress.getByName(host).getHostAddress();
		} catch (UnknownHostException e) {
			Engine.logProxyManager.debug("Proxy auto config error : DNS name not resolvable " + host);
			// Not resolvable.
		}
		return "";
	}


	public String myIpAddress() {
		try {
			String overrideIP = System.getProperty(OVERRIDE_LOCAL_IP);
			if (overrideIP != null && overrideIP.trim().length() > 0) {
				return overrideIP.trim(); 
			}
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			Engine.logProxyManager.debug("Proxy auto config error : Local address not resolvable");
			return "";
		}
	}


	public int dnsDomainLevels(String host) {
		int count = 0;
		int startPos = 0;
		while ((startPos = host.indexOf(".", startPos + 1)) > -1) {
			count++;
		}
		return count;
	}

	public boolean shExpMatch(String str, String shexp) {
		StringTokenizer tokenizer = new StringTokenizer(shexp, "*");
		int startPos = 0;
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			// 07.05.2009 Incorrect? first token can be startsWith and last one
			// can be endsWith
			int temp = str.indexOf(token, startPos);
			if (temp == -1) {
				return false;
			} else {
				startPos = temp + token.length();
			}
		}
		return true;
	}


	public boolean weekdayRange(String wd1, String wd2, String gmt) {
		boolean useGmt = GMT.equalsIgnoreCase(wd2) || GMT.equalsIgnoreCase(gmt);
		Calendar cal = getCurrentTime(useGmt);

		int currentDay = cal.get(Calendar.DAY_OF_WEEK) - 1;
		int from = DAYS.indexOf(wd1 == null ? null : wd1.toUpperCase());
		int to = DAYS.indexOf(wd2 == null ? null : wd2.toUpperCase());
		if (to == -1) {
			to = from;
		}

		if (to < from) {
			return currentDay >= from || currentDay <= to;
		} else {
			return currentDay >= from && currentDay <= to;
		}
	}

	public void setCurrentTime(Calendar cal) {
		currentTime = cal;
	}

	private Calendar getCurrentTime(boolean useGmt) {
		if (currentTime != null) { // Only used for unit tests
			return (Calendar) currentTime.clone();
		}
		return Calendar.getInstance(useGmt ? TimeZone.getTimeZone(GMT)
				: TimeZone.getDefault());
	}


	public boolean dateRange(Object day1, Object month1, Object year1,
			Object day2, Object month2, Object year2, Object gmt) {

		// Guess the parameter meanings.
		Map<String, Integer> params = new HashMap<String, Integer>();
		parseDateParam(params, day1);
		parseDateParam(params, month1);
		parseDateParam(params, year1);
		parseDateParam(params, day2);
		parseDateParam(params, month2);
		parseDateParam(params, year2);
		parseDateParam(params, gmt);

		// Get current date
		boolean useGmt = params.get("gmt") != null;
		Calendar cal = getCurrentTime(useGmt);
		Date current = cal.getTime();

		// Build the "from" date
		if (params.get("day1") != null) {
			cal.set(Calendar.DAY_OF_MONTH, params.get("day1"));
		}
		if (params.get("month1") != null) {
			cal.set(Calendar.MONTH, params.get("month1"));
		}
		if (params.get("year1") != null) {
			cal.set(Calendar.YEAR, params.get("year1"));
		}
		Date from = cal.getTime();

		// Build the "to" date
		Date to;
		if (params.get("day2") != null) {
			cal.set(Calendar.DAY_OF_MONTH, params.get("day2"));
		}
		if (params.get("month2") != null) {
			cal.set(Calendar.MONTH, params.get("month2"));
		}
		if (params.get("year2") != null) {
			cal.set(Calendar.YEAR, params.get("year2"));
		}
		to = cal.getTime();

		// Need to increment to the next month?
		if (to.before(from)) {
			cal.add(Calendar.MONTH, +1);
			to = cal.getTime();
		}
		// Need to increment to the next year?
		if (to.before(from)) {
			cal.add(Calendar.YEAR, +1);
			cal.add(Calendar.MONTH, -1);
			to = cal.getTime();
		}

		return current.compareTo(from) >= 0 && current.compareTo(to) <= 0;
	}

	private void parseDateParam(Map<String, Integer> params, Object value) {
		if (value instanceof Number) {
			int n = ((Number) value).intValue();
			if (n <= 31) {
				// Its a day
				if (params.get("day1") == null) {
					params.put("day1", n);
				} else {
					params.put("day2", n);
				}
			} else {
				// Its a year
				if (params.get("year1") == null) {
					params.put("year1", n);
				} else {
					params.put("year2", n);
				}
			}
		}

		if (value instanceof String) {
			int n = MONTH.indexOf(((String) value).toUpperCase());
			if (n > -1) {
				// Its a month
				if (params.get("month1") == null) {
					params.put("month1", n);
				} else {
					params.put("month2", n);
				}
			}
		}

		if (GMT.equalsIgnoreCase(String.valueOf(value))) {
			params.put("gmt", 1);
		}
	}

	public boolean timeRange(Object hour1, Object min1, Object sec1,
			Object hour2, Object min2, Object sec2, Object gmt) {
		boolean useGmt = GMT.equalsIgnoreCase(String.valueOf(min1))
				|| GMT.equalsIgnoreCase(String.valueOf(sec1))
				|| GMT.equalsIgnoreCase(String.valueOf(min2))
				|| GMT.equalsIgnoreCase(String.valueOf(gmt));

		Calendar cal = getCurrentTime(useGmt);
		cal.set(Calendar.MILLISECOND, 0);
		Date current = cal.getTime();
		Date from;
		Date to;
		if (sec2 instanceof Number) {
			cal.set(Calendar.HOUR_OF_DAY, ((Number) hour1).intValue());
			cal.set(Calendar.MINUTE, ((Number) min1).intValue());
			cal.set(Calendar.SECOND, ((Number) sec1).intValue());
			from = cal.getTime();

			cal.set(Calendar.HOUR_OF_DAY, ((Number) hour2).intValue());
			cal.set(Calendar.MINUTE, ((Number) min2).intValue());
			cal.set(Calendar.SECOND, ((Number) sec2).intValue());
			to = cal.getTime();
		} else if (hour2 instanceof Number) {
			cal.set(Calendar.HOUR_OF_DAY, ((Number) hour1).intValue());
			cal.set(Calendar.MINUTE, ((Number) min1).intValue());
			cal.set(Calendar.SECOND, 0);
			from = cal.getTime();

			cal.set(Calendar.HOUR_OF_DAY, ((Number) sec1).intValue());
			cal.set(Calendar.MINUTE, ((Number) hour2).intValue());
			cal.set(Calendar.SECOND, 59);
			to = cal.getTime();
		} else if (min1 instanceof Number) {
			cal.set(Calendar.HOUR_OF_DAY, ((Number) hour1).intValue());
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			from = cal.getTime();

			cal.set(Calendar.HOUR_OF_DAY, ((Number) min1).intValue());
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 59);
			to = cal.getTime();
		} else {
			cal.set(Calendar.HOUR_OF_DAY, ((Number) hour1).intValue());
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			from = cal.getTime();

			cal.set(Calendar.HOUR_OF_DAY, ((Number) hour1).intValue());
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 59);
			to = cal.getTime();
		}

		if (to.before(from)) {
			cal.setTime(to);
			cal.add(Calendar.DATE, +1);
			to = cal.getTime();
		}

		return current.compareTo(from) >= 0 && current.compareTo(to) <= 0;
	}
}
