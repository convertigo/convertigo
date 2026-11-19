/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.engine;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.mozilla.javascript.Undefined;

public class PacScriptMethods {
	enum jsFunctions {
		shExpMatch, dnsResolve, isResolvable, isInNet, dnsDomainIs, isPlainHostName,
		myIpAddress, dnsDomainLevels, localHostOrDomainIs, weekdayRange, dateRange, timeRange,
		isInNetEx, dnsResolveEx, myIpAddressEx, isResolvableEx;
	}

	public static final String OVERRIDE_LOCAL_IP = "com.btr.proxy.pac.overrideLocalIP";
	private static final List<String> DAYS = List.of("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT");
	private static final List<String> MONTHS = List.of("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC");
	private Calendar currentTime;

	public boolean isPlainHostName(String host) { return host.indexOf('.') < 0 && host.indexOf(':') < 0; }
	public boolean dnsDomainIs(String host, String domain) { return host.endsWith(domain); }
	public boolean localHostOrDomainIs(String host, String domain) {
		return host.equals(domain) || (isPlainHostName(host) && domain.startsWith(host + "."));
	}
	public int dnsDomainLevels(String host) { return (int) host.chars().filter(c -> c == '.').count(); }

	/** Match the portable PAC shell syntax: '*' and '?', over the complete string. */
	public boolean shExpMatch(String text, String pattern) {
		int i = 0, j = 0, star = -1, mark = 0;
		while (i < text.length()) {
			if (j < pattern.length() && (pattern.charAt(j) == '?' || pattern.charAt(j) == text.charAt(i))) { i++; j++; }
			else if (j < pattern.length() && pattern.charAt(j) == '*') { star = j++; mark = i; }
			else if (star >= 0) { j = star + 1; i = ++mark; }
			else return false;
		}
		while (j < pattern.length() && pattern.charAt(j) == '*') j++;
		return j == pattern.length();
	}

	protected InetAddress[] resolve(String host) throws UnknownHostException {
		if (host == null || host.isBlank()) throw new UnknownHostException("Empty PAC hostname");
		return InetAddress.getAllByName(host);
	}

	public String dnsResolve(String host) {
		try { for (var address : resolve(host)) if (address instanceof Inet4Address) return address.getHostAddress(); }
		catch (UnknownHostException ignored) { }
		return null;
	}
	public boolean isResolvable(String host) { return dnsResolve(host) != null; }
	public String dnsResolveEx(String host) {
		try { return addressList(Arrays.asList(resolve(host))); }
		catch (UnknownHostException ignored) { return ""; }
	}
	public boolean isResolvableEx(String host) { return !dnsResolveEx(host).isEmpty(); }

	protected List<InetAddress> localAddresses() throws SocketException {
		var result = new ArrayList<InetAddress>();
		var interfaces = NetworkInterface.getNetworkInterfaces();
		while (interfaces != null && interfaces.hasMoreElements()) {
			var network = interfaces.nextElement();
			if (!network.isUp() || network.isLoopback()) continue;
			var addresses = network.getInetAddresses();
			while (addresses.hasMoreElements()) result.add(addresses.nextElement());
		}
		return result;
	}
	public String myIpAddress() {
		String override = System.getProperty(OVERRIDE_LOCAL_IP);
		if (override != null && ipv4(override.trim()) != null) return override.trim();
		try {
			var local = InetAddress.getLocalHost();
			if (local instanceof Inet4Address && !local.isLoopbackAddress()) return local.getHostAddress();
		} catch (UnknownHostException ignored) { }
		try { for (var address : localAddresses()) if (address instanceof Inet4Address) return address.getHostAddress(); }
		catch (SocketException ignored) { }
		return "127.0.0.1";
	}
	public String myIpAddressEx() {
		String override = System.getProperty(OVERRIDE_LOCAL_IP);
		if (override != null && literal(override.trim()) != null) return override.trim();
		try { return addressList(localAddresses()); }
		catch (SocketException ignored) { return ""; }
	}
	private static String addressList(List<InetAddress> addresses) {
		var values = new LinkedHashSet<String>();
		addresses.stream().sorted(Comparator.comparingInt(a -> a instanceof Inet4Address ? 1 : 0))
				.forEach(a -> values.add(a.getHostAddress()));
		return String.join(";", values);
	}

	public boolean isInNet(String host, String pattern, String mask) {
		byte[] address = ipv4(host), network = ipv4(pattern), bits = ipv4(mask);
		if (network == null || bits == null) return false;
		if (address == null) address = ipv4(dnsResolve(host));
		if (address == null) return false;
		for (int i = 0; i < 4; i++) if ((address[i] & bits[i]) != (network[i] & bits[i])) return false;
		return true;
	}

	public boolean isInNetEx(String address, String prefix) {
		if (address == null || prefix == null) return false;
		String[] parts = prefix.split("/", -1);
		if (parts.length != 2 || !parts[1].matches("[0-9]{1,3}")) return false;
		byte[] ip = literal(address), network = literal(parts[0]);
		if (ip == null || network == null || ip.length != network.length) return false;
		int bits = Integer.parseInt(parts[1]);
		if (bits > ip.length * 8) return false;
		for (int i = 0; i < ip.length && bits > 0; i++, bits -= 8) {
			int mask = (0xff << (8 - Math.min(bits, 8))) & 0xff;
			if ((ip[i] & mask) != (network[i] & mask)) return false;
		}
		return true;
	}
	private static byte[] ipv4(String text) {
		if (text == null || !text.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")) return null;
		String[] parts = text.split("\\."); byte[] bytes = new byte[4];
		for (int i = 0; i < 4; i++) { int value = Integer.parseInt(parts[i]); if (value > 255) return null; bytes[i] = (byte) value; }
		return bytes;
	}
	private static byte[] literal(String text) {
		if (text == null) return null;
		if (text.startsWith("[") && text.endsWith("]")) text = text.substring(1, text.length() - 1);
		if (!text.contains(":")) return ipv4(text);
		int zone = text.indexOf('%'); if (zone >= 0) text = text.substring(0, zone);
		if (!text.matches("[0-9a-fA-F:.]+")) return null;
		try {
			byte[] bytes = InetAddress.getByName(text).getAddress(); // A validated literal never performs DNS.
			if (bytes.length == 4) {
				// Java normalizes mapped IPv6 literals to Inet4Address; retain the prefix family.
				byte[] mapped = new byte[16]; mapped[10] = mapped[11] = (byte) 0xff;
				System.arraycopy(bytes, 0, mapped, 12, 4); return mapped;
			}
			return bytes;
		} catch (UnknownHostException ignored) { return null; }
	}

	public void setCurrentTime(Calendar time) { currentTime = time == null ? null : (Calendar) time.clone(); }
	private Calendar current(boolean gmt) {
		TimeZone zone = gmt ? TimeZone.getTimeZone("GMT") : currentTime == null ? TimeZone.getDefault() : currentTime.getTimeZone();
		Calendar time = Calendar.getInstance(zone);
		if (currentTime != null) time.setTimeInMillis(currentTime.getTimeInMillis());
		return time;
	}
	private static Object[] arguments(Object[] values) {
		return Arrays.stream(values).filter(value -> value != null && !Undefined.isUndefined(value)).toArray();
	}
	private static boolean gmt(Object[] args) { return args.length > 0 && "GMT".equalsIgnoreCase(String.valueOf(args[args.length - 1])); }
	private static boolean range(int value, int from, int to) { return from <= to ? value >= from && value <= to : value >= from || value <= to; }
	private static int number(Object value, int min, int max) {
		if (!(value instanceof Number n) || !Double.isFinite(n.doubleValue()) || n.doubleValue() != n.intValue()) return -1;
		int result = n.intValue(); return result >= min && result <= max ? result : -1;
	}
	private static int month(Object value) { return MONTHS.indexOf(String.valueOf(value).toUpperCase(Locale.ROOT)) + 1; }

	// Keep the existing Java signatures; varargs also expose all documented PAC arities to Rhino.
	public boolean weekdayRange(String first, String last, String zone) { return weekdayRange(new Object[] {first, last, zone}); }
	public boolean weekdayRange(Object... values) {
		Object[] args = arguments(values); boolean utc = gmt(args); int size = args.length - (utc ? 1 : 0);
		if (size < 1 || size > 2) return false;
		int first = DAYS.indexOf(String.valueOf(args[0]).toUpperCase(Locale.ROOT));
		int last = size == 1 ? first : DAYS.indexOf(String.valueOf(args[1]).toUpperCase(Locale.ROOT));
		return first >= 0 && last >= 0 && range(current(utc).get(Calendar.DAY_OF_WEEK) - 1, first, last);
	}
	public boolean timeRange(Object h1, Object m1, Object s1, Object h2, Object m2, Object s2, Object zone) {
		return timeRange(new Object[] {h1, m1, s1, h2, m2, s2, zone});
	}
	public boolean timeRange(Object... values) {
		Object[] args = arguments(values); boolean utc = gmt(args); int size = args.length - (utc ? 1 : 0);
		if (size != 1 && size != 2 && size != 4 && size != 6) return false;
		Calendar now = current(utc); int seconds = now.get(Calendar.HOUR_OF_DAY) * 3600 + now.get(Calendar.MINUTE) * 60 + now.get(Calendar.SECOND);
		int width = size <= 2 ? 1 : size / 2;
		int first = time(args, 0, width), last = size == 1 ? first : time(args, width, width);
		if (first < 0 || last < 0) return false;
		last += width == 1 ? 3599 : width == 2 ? 59 : 0;
		return range(seconds, first, last);
	}
	private static int time(Object[] args, int offset, int size) {
		int h = number(args[offset], 0, 23), m = size > 1 ? number(args[offset + 1], 0, 59) : 0;
		int s = size > 2 ? number(args[offset + 2], 0, 59) : 0;
		return h < 0 || m < 0 || s < 0 ? -1 : h * 3600 + m * 60 + s;
	}
	public boolean dateRange(Object d1, Object m1, Object y1, Object d2, Object m2, Object y2, Object zone) {
		return dateRange(new Object[] {d1, m1, y1, d2, m2, y2, zone});
	}
	public boolean dateRange(Object... values) {
		Object[] args = arguments(values); boolean utc = gmt(args); int size = args.length - (utc ? 1 : 0);
		Calendar now = current(utc); int day = now.get(Calendar.DAY_OF_MONTH), mon = now.get(Calendar.MONTH) + 1, year = now.get(Calendar.YEAR);
		try {
			if (size == 1) return month(args[0]) > 0 ? mon == month(args[0]) : number(args[0], 1, 31) > 0 ? day == number(args[0], 1, 31) : year == number(args[0], 32, 9999);
			if (size == 2) {
				if (month(args[0]) > 0 && month(args[1]) > 0) return range(mon, month(args[0]), month(args[1]));
				if (number(args[0], 1, 31) > 0 && month(args[1]) > 0) return day == number(args[0], 1, 31) && mon == month(args[1]);
				if (month(args[0]) > 0 && number(args[1], 32, 9999) > 0) return mon == month(args[0]) && year == number(args[1], 32, 9999);
				int min = number(args[0], 1, 31) > 0 ? 1 : 32, max = min == 1 ? 31 : 9999;
				int first = number(args[0], min, max), last = number(args[1], min, max);
				return first > 0 && last > 0 && range(min == 1 ? day : year, first, last);
			}
			if (size == 3) return date(args, 0).equals(LocalDate.of(year, mon, day));
			if (size == 4) {
				if (month(args[0]) > 0) {
					int firstYear = number(args[1], 32, 9999), lastYear = number(args[3], 32, 9999);
					return firstYear > 0 && lastYear > 0 && month(args[2]) > 0 && range(year * 12 + mon, firstYear * 12 + month(args[0]), lastYear * 12 + month(args[2]));
				}
				int first = LocalDate.of(2000, month(args[1]), number(args[0], 1, 31)).getDayOfYear();
				int last = LocalDate.of(2000, month(args[3]), number(args[2], 1, 31)).getDayOfYear();
				return range(LocalDate.of(2000, mon, day).getDayOfYear(), first, last);
			}
			if (size == 6) return range((int) LocalDate.of(year, mon, day).toEpochDay(), (int) date(args, 0).toEpochDay(), (int) date(args, 3).toEpochDay());
		} catch (DateTimeException invalid) { return false; }
		return false;
	}
	private static LocalDate date(Object[] args, int offset) {
		int year = number(args[offset + 2], 32, 9999);
		if (year < 0) throw new DateTimeException("Invalid PAC year");
		return LocalDate.of(year, month(args[offset + 1]), number(args[offset], 1, 31));
	}
}
