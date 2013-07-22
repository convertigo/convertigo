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

public class HostCookieCan {
	private String domainRoot;
	private List<ClientCookie> cookies = new ArrayList<ClientCookie>();

	public HostCookieCan(String domainRoot) {
		this.domainRoot = domainRoot;
	}

	synchronized public void addCookie(ClientCookie cookie) {
		int pos = cookies.indexOf(cookie);
		if (pos < 0) {
			if (!cookie.isTerminated())
				cookies.add(cookie);
		} else {
			if (cookie.isTerminated())
				cookies.remove(pos);
			else
				cookies.set(pos, cookie);
		}
	}

	synchronized public String getServerCookieString(
		String host,
		String path) {
		StringBuffer sbuf = new StringBuffer(64 * cookies.size());

		int index = 0;
		while (index < cookies.size()) {
			ClientCookie cookie = cookies.get(index);

			if (cookie.isExpired()) {
				cookies.remove(index);
				continue;
			}

			if (cookie.matchedBy(host) && path.startsWith(cookie.getPath())) {
				if (sbuf.length() > 0)
					sbuf.append("; ");

				sbuf.append(cookie.toServerCookieString());
			}

			index++;
		}

		return sbuf.length() == 0 ? null : sbuf.toString();
	}

	synchronized public String toString() {
		return toString("");
	}

	synchronized public String toString(String marginSpaces) {
		StringBuffer sb = new StringBuffer(128 * cookies.size() + 64);

		sb.append(marginSpaces).append("HostCookieCan: ").append(
			domainRoot).append(
			'\n');
		sb.append('\n');

		marginSpaces += "    ";
		for (int k = 0; k < cookies.size(); k++)
			sb.append(
				((ClientCookie) cookies.get(k)).toString(marginSpaces)).append(
				'\n');

		if (cookies.size() > 0)
			sb.append('\n');

		return sb.toString();
	}
}