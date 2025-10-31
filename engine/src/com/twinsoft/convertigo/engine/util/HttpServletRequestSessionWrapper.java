/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

public class HttpServletRequestSessionWrapper extends HttpServletRequestWrapper {
	public HttpServletRequestSessionWrapper(HttpServletRequest request) {
		super(request);
	}

	@Override
	public HttpSession getSession() {
		var session = super.getSession();
		return HttpSessionTwsWrapper.wrap(session);
	}

	@Override
	public HttpSession getSession(boolean create) {
		var session = super.getSession(create);
		return HttpSessionTwsWrapper.wrap(session);
	}
}
