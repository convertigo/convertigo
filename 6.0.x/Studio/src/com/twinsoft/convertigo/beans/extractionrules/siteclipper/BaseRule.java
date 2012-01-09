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

import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector;
import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector.Shuttle;
import com.twinsoft.convertigo.beans.core.ExtractionRule;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineEvent;

public abstract class BaseRule extends ExtractionRule {

	private static final long serialVersionUID = 2299923639115515335L;
	
	public BaseRule() {
		super();
	}

	public boolean applyOnRequest(Shuttle shuttle) {
		return false;
	}

	public boolean applyOnResponse(Shuttle shuttle) {
		return false;
	}
	
	@Override
	public SiteClipperConnector getConnector() {
		return (SiteClipperConnector) super.getConnector();
	}
	
	public void fireEvents() {
		if (Engine.isStudioMode()) {
			Engine.theApp.fireObjectDetected(new EngineEvent(this));
			if (getConnector().isDebugging()) {
				Engine.logSiteClipper.trace("(BaseRule) Step reached before applying rule \"" + getName() + "\".");
				Engine.theApp.fireStepReached(new EngineEvent(this));
			}
		}			
	}
}
