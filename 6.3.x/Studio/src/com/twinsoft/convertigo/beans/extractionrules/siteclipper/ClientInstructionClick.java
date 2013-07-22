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
import com.twinsoft.convertigo.engine.siteclipper.clientinstruction.Click;

public class ClientInstructionClick extends AbstractClientInstructionWithPath {
	private static final long serialVersionUID = -286007303795131409L;
	
	public ClientInstructionClick() {
		super();
	}

	@Override
	public boolean applyOnResponse(Shuttle shuttle) {
		String targetPath = getEvaluatedTargetPath(shuttle);
		Engine.logSiteClipper.trace("(ClientInstructionClick) JQuery selector '" + targetPath + "'");
		shuttle.addPostInstruction(new Click(targetPath));
		return true;
	}
}
