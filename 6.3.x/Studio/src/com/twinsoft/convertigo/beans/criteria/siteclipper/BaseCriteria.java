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

package com.twinsoft.convertigo.beans.criteria.siteclipper;

import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector;
import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector.Shuttle;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Criteria;

public abstract class BaseCriteria extends Criteria {

	private static final long serialVersionUID = 5764882110150448490L;

	public BaseCriteria() {
		super();
	}

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.Criteria#isMatching0(com.twinsoft.convertigo.beans.core.Connector)
	 */
	@Override
	protected boolean isMatching0(Connector connector) {
		Shuttle shuttle = ((SiteClipperConnector)connector).getShuttle();
		switch (shuttle.getProcessState()) {
			case request: 	return isMatchingRequest(shuttle);
			case response:	return isMatchingRequest(shuttle) && isMatchingResponse(shuttle);
			default:		return true;
		}
	}

	public boolean isMatchingRequest(Shuttle shuttle) {
		return true;
	}

	public boolean isMatchingResponse(Shuttle shuttle) {
		return true;
	}

}
