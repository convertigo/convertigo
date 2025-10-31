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

package com.twinsoft.convertigo.engine.servlets;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import com.twinsoft.convertigo.engine.util.HttpSessionTwsWrapper;

public class SessionSerializationListener implements HttpSessionListener {
	@Override
	public void sessionCreated(HttpSessionEvent se) {
		// no-op
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent se) {
		var session = se.getSession();
		HttpSessionTwsWrapper.cleanTransientAttributes(session.getId());
	}
}
