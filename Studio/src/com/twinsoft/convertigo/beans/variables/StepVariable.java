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

package com.twinsoft.convertigo.beans.variables;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.engine.EngineException;

public class StepVariable extends Variable implements IStepSourceContainer {

	private static final long serialVersionUID = 170880313545326385L;

	private XMLVector<String> sourceDefinition = new XMLVector<String>();
	
	public StepVariable() {
		super();
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		StepVariable clonedObject = (StepVariable)super.clone();
		return clonedObject;
	}

	public XMLVector<String> getSourceDefinition() {
		return sourceDefinition;
	}

	public void setSourceDefinition(XMLVector<String> sourceDefinition) {
		this.sourceDefinition = sourceDefinition;
	}
	
	public Object getSourceValue() throws EngineException {
		Object value = null;
		if (sourceDefinition.size() != 0 && parent != null) {
			StepSource source = new StepSource((Step)parent, sourceDefinition);
			value = source.getContextValues();
		}
		return value;
	}
	
	@Override
	protected String getLabel() throws EngineException {
		if (sourceDefinition.size() > 0) {
			StepSource source = new StepSource((Step)parent, sourceDefinition);
			if (source != null) {
				return " ("+source.getLabel()+")";
			}
		}
		return super.getLabel();
	}	
	
}
