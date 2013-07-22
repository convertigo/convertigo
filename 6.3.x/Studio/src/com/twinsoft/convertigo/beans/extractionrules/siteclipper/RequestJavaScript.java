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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/branches/6.2.x/Studio/src/com/twinsoft/convertigo/beans/extractionrules/siteclipper/AddHeaderToRequest.java $
 * $Author: fabienb $
 * $Revision: 28379 $
 * $Date: 2011-09-27 11:38:59 +0200 (mar., 27 sept. 2011) $
 */

package com.twinsoft.convertigo.beans.extractionrules.siteclipper;

import java.util.HashMap;
import java.util.Map;

import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector.Shuttle;

public class RequestJavaScript extends JavaScript implements IRequestRule {

	private static final long serialVersionUID = 954421238020843645L;

	public RequestJavaScript() {
		super();
	}

	@Override
	public boolean applyOnRequest(Shuttle shuttle) {
		Map<String, Object> objectsToAddInScope = new HashMap<String, Object>();
		objectsToAddInScope.put("shuttle", shuttle);
		return executeJavaScript(shuttle, objectsToAddInScope);
	}
}
