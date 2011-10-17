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

import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector.Shuttle;
import com.twinsoft.convertigo.engine.Engine;

public abstract class ModifyHeader extends Header {

	private static final long serialVersionUID = -8295461182389086882L;

	public ModifyHeader() {
		super();
	}

	protected boolean modifyHeader(Shuttle shuttle) {
		try {			
			if (!headerName.equals("")) {
				if (shuttle.getCustomHeader(headerName) != null) {
					Engine.logSiteClipper.trace("(ModifyHeader) Modifying header " + headerName + "=" + headerValue);
					shuttle.setCustomHeader(headerName, headerValue);
					return true;
				} else {
					Engine.logSiteClipper.trace("(ModifyHeader) Modifying header " + headerName + " failed because header doesn't exist");
				}
			}
		} catch (Exception e) {
			Engine.logSiteClipper.warn("Unable to apply 'ModifyHeader' rule : "+ name, e);
		}
		return false;
	}
}
