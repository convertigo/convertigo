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

import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector.Shuttle;
import com.twinsoft.convertigo.engine.enums.HttpPool;

public class SetHttpPoolPolicy extends BaseRule implements IRequestRule {

	private static final long serialVersionUID = 954421238020843645L;
	
	private HttpPool httpPool = HttpPool.no;

	public SetHttpPoolPolicy() {
		super();
	}

	@Override
	public boolean applyOnRequest(Shuttle shuttle) {
		shuttle.setHttpPool(httpPool);
		return true;
	}

	public HttpPool getHttpPool() {
		return httpPool;
	}

	public void setHttpPool(HttpPool httpPool) {
		this.httpPool = httpPool;
	}
}
