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

package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import java.util.EventObject;

import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.EngineException;

public class StepSourceEvent extends EventObject {

	private static final long serialVersionUID = 7366613673147349254L;

	private String xpath = ".";
	
	public StepSourceEvent(Object step) {
		super(step);
	}
	
	public StepSourceEvent(StepSource source) throws EngineException {
		super(source.getStep());
		this.xpath = source.getXpath();
	}
	
	public StepSourceEvent(Object step, String xpath) {
		super(step);
		this.xpath = xpath;
	}

	public String getXPath() {
		return xpath;
	}

}
