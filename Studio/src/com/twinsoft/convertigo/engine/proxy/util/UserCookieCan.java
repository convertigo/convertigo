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

public class UserCookieCan {
	private String userId, userInfo;
	private Map<String, HostCookieCan> hostCans = new HashMap<String, HostCookieCan>();

	private long lastVisitTime;

	public long getLastVisitTime() {
		return lastVisitTime;
	}

	public UserCookieCan(String userId, String userInfo) {
		this.userId = userId;
		this.userInfo = userInfo;
		lastVisitTime = System.currentTimeMillis();
	}

	public void addCookie(ClientCookie cookie) {
		lastVisitTime = System.currentTimeMillis();

		HostCookieCan hostCan = null;

		synchronized (hostCans) {
			String domainRoot = ClientCookie.calcDomainRoot(cookie.getDomain());
			hostCan = hostCans.get(domainRoot);

			if (hostCan == null) {
				hostCan = new HostCookieCan(domainRoot);
				hostCans.put(domainRoot, hostCan);
			}
		}

		hostCan.addCookie(cookie);
	}

	public String getServerCookieString(String host, String path) {
		lastVisitTime = System.currentTimeMillis();

		HostCookieCan hostCan = hostCans.get(ClientCookie.calcDomainRoot(host));

		return hostCan == null
			? null
			: hostCan.getServerCookieString(host, path);
	}

	public String toString() {
		return toString("");
	}

	public String toString(String marginSpaces) {
		StringBuffer sb = new StringBuffer(512 * hostCans.size());

		sb
			.append(marginSpaces)
			.append("UserCookieCan: ")
			.append(userId)
			.append('(')
			.append(userInfo)
			.append(')')
			.append('\n')
			.append('\n');
		marginSpaces += "    ";

		synchronized (hostCans) {
			for (HostCookieCan hostCan : hostCans.values()) {
				sb.append(hostCan.toString(marginSpaces));
			}
		}

		return sb.toString();
	}
}