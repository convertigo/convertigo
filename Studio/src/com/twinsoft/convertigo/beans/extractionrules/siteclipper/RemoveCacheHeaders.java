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

package com.twinsoft.convertigo.beans.extractionrules.siteclipper;

import java.util.Collection;

import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector.Shuttle;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.HeaderName;

public abstract class RemoveCacheHeaders extends BaseRule {

	private static final long serialVersionUID = -7619663938358141563L;
	
	public RemoveCacheHeaders() {
		super();
	}

	protected boolean removeCacheHeaders(Shuttle shuttle, Collection<HeaderName> headerNames) {
		boolean status = false;
		for (HeaderName headerName : headerNames) {
			try {
					if (shuttle.getCustomHeader(headerName) != null) {
						Engine.logSiteClipper.trace("(RemoveHeader) Removing header " + headerName);
						shuttle.setCustomHeader(headerName, null);
						status = true;
					} else {
						Engine.logSiteClipper.trace("(RemoveHeader) Removing header " + headerName + " failed because this header does not exist");
					} 
			} catch (Exception e) {
				Engine.logSiteClipper.warn("Unable to apply 'RemoveHeader' rule : "+ name, e);
			}
		}
		return status;
	}
}
