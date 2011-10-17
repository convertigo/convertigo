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
import com.twinsoft.convertigo.engine.siteclipper.clientinstruction.SetChecked;

public class ClientInstructionSetChecked extends AbstractClientInstructionWithPath {
	private static final long serialVersionUID = -286007303732191409L;
	
	private String targetValue = "true";
	
	public ClientInstructionSetChecked() {
		super();
	}

	@Override
	public boolean applyOnResponse(Shuttle shuttle) {
		String targetPath = getEvaluatedTargetPath(shuttle);
		String value = shuttle.evalJavascript(targetValue).toString();
		boolean checked = "true".equalsIgnoreCase(value);
		Engine.logSiteClipper.trace("(ClientInstructionSetChecked) JQuery selector '" + targetPath + "' with value '" + value + "'");
		shuttle.addPostInstruction(new SetChecked(targetPath, checked));
		return true;
	}

	public String getTargetValue() {
		return targetValue;
	}

	public void setTargetValue(String targetValue) {
		this.targetValue = targetValue;
	}
}
