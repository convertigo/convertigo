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

package com.twinsoft.convertigo.engine.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpPropertyUtils {

	private static final Pattern HEADER_TRAILING_DIGITS = Pattern.compile("\\d+$");
	private static final Pattern STATUS_CODE_PATTERN = Pattern.compile("^_?(\\d+)$");

	private HttpPropertyUtils() {
	}

	public static String toHttpHeaderName(String beanName) {
		String normalizedBeanName = getNormalizedBeanName(beanName);
		if (normalizedBeanName.isEmpty()) {
			return "";
		}

		StringBuilder headerName = new StringBuilder();
		for (String token : normalizedBeanName.split("_+")) {
			if (token.isEmpty()) {
				continue;
			}
			if (headerName.length() > 0) {
				headerName.append('-');
			}
			headerName.append(Character.toUpperCase(token.charAt(0)));
			if (token.length() > 1) {
				headerName.append(token.substring(1).toLowerCase(Locale.ROOT));
			}
		}
		return headerName.toString();
	}

	public static boolean isBeanNameBasedHeader(String headerName, String beanName) {
		if (headerName == null || headerName.isEmpty()) {
			return true;
		}

		String normalizedBeanName = StringUtils.normalize(beanName);
		String baseBeanName = getNormalizedBeanName(beanName);
		return headerName.equals(normalizedBeanName)
				|| headerName.equals(baseBeanName)
				|| headerName.equals(toHttpHeaderName(beanName));
	}

	public static String toHttpStatusCode(String beanName) {
		if (beanName == null) {
			return "";
		}
		Matcher matcher = STATUS_CODE_PATTERN.matcher(StringUtils.normalize(beanName));
		return matcher.matches() ? matcher.group(1) : "";
	}

	public static boolean isBeanNameBasedStatusCode(String statusCode, String beanName) {
		if (statusCode == null || statusCode.isEmpty()) {
			return true;
		}

		String normalizedBeanName = StringUtils.normalize(beanName);
		return statusCode.equals(normalizedBeanName)
				|| statusCode.equals(toHttpStatusCode(beanName));
	}

	private static String getNormalizedBeanName(String beanName) {
		if (beanName == null) {
			return "";
		}
		String normalizedBeanName = StringUtils.normalize(beanName);
		return HEADER_TRAILING_DIGITS.matcher(normalizedBeanName).replaceFirst("");
	}
}
