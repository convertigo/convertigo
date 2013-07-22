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

import java.util.*;

public class CookieManager {
	static private CookieManager defMgr = new CookieManager();
	private CookieManager() {
		lastCleanupTime = lastVisitTime = System.currentTimeMillis();
	}
	static public CookieManager getDefaultInstance() {
		return defMgr;
	}

	static public final long HARD_TIMEOUT_MILLIS = 14400000; // 4 hours
	static public final long SOFT_TIMEOUT_MILLIS = 3600000; // 1 hour
	static public final long SOFT_GAP_MILLIS = 10000; // 10 seconds
	static public final long USER_TIMEOUT_MILLIS = 3600000; // 1 hour

	private long lastCleanupTime;
	private long lastVisitTime;

	public long getLastVisitTime() {
		return lastVisitTime;
	}

	public void cleanup() {
		long nowMillis = System.currentTimeMillis();
		long gapMillis = nowMillis - lastCleanupTime;

		if (gapMillis < SOFT_TIMEOUT_MILLIS)
			return;

		if (gapMillis < HARD_TIMEOUT_MILLIS
			&& nowMillis - lastVisitTime < SOFT_GAP_MILLIS)
			return;

		removeCookies(nowMillis);
	}

	synchronized private void removeCookies(long nowMillis) {
		List<String> timeoutUserList = new ArrayList<String>(userCans.size());

		for (String userId : userCans.keySet()) {
			UserCookieCan userCan = userCans.get(userId);

			if (nowMillis - userCan.getLastVisitTime() >= USER_TIMEOUT_MILLIS)
				timeoutUserList.add(userId);
		}
		
		for (int k = 0; k < timeoutUserList.size(); k++)
			userCans.remove((String) timeoutUserList.get(k));

		lastCleanupTime = System.currentTimeMillis();
	}

	synchronized public void remove(String userId) {
		userCans.remove(userId);
	}

	private Map<String, UserCookieCan> userCans = new HashMap<String, UserCookieCan>();

	synchronized public void addCookie(String userId, String userInfo, ClientCookie cookie) {
		cleanup();
		lastVisitTime = System.currentTimeMillis();

		UserCookieCan userCan = null;

		userCan = userCans.get(userId);

		if (userCan == null) {
			userCan = new UserCookieCan(userId, userInfo);
			userCans.put(userId, userCan);
		}

		userCan.addCookie(cookie);
	}

	public String getServerCookieString(
		String userId,
		String host,
		String path) {
		cleanup();
		lastVisitTime = System.currentTimeMillis();

		UserCookieCan userCan = userCans.get(userId);

		if (userCan == null)
			return null;

		return userCan.getServerCookieString(host, path);
	}

	public String toString() {
		return toString("");
	}

	synchronized public String toString(String marginSpaces) {
		StringBuffer sb = new StringBuffer(1024 * userCans.size());

		sb.append(marginSpaces).append("CookieManager: ").append('\n');
		sb.append('\n');
		marginSpaces += "    ";

		for (UserCookieCan userCan : userCans.values()) {
			sb.append(userCan.toString(marginSpaces));
		}

		return sb.toString();
	}
}