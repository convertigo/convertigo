/*
 * Copyright (c) 2001-2016 Convertigo SA.
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

package com.twinsoft.convertigo.beans.mobile.components;

import com.twinsoft.convertigo.beans.core.IVariableContainer;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class UIControlCallSequence extends UIControlCallAction {
	
	private static final long serialVersionUID = 237124673723392698L;

	public UIControlCallSequence() {
		super();
	}

	@Override
	public UIControlCallSequence clone() throws CloneNotSupportedException {
		UIControlCallSequence cloned = (UIControlCallSequence) super.clone();
		return cloned;
	}

	@Override
	protected String getRequestableTarget() {
		String requestableTarget = getTarget();
		/*try {
			requestableTarget = requestableTarget.replaceFirst(this.getProject().getName(), "");
		} catch (Exception e) {}*/
		return requestableTarget;
	}
	
	@Override
	public void importVariableDefinition() {
		Sequence targetSequence = getTargetSequence();
		if (targetSequence != null && targetSequence instanceof GenericSequence) {
			try {
				importVariableDefinition(targetSequence);
			} catch (Exception e) {}
		}
	}
	
    private Sequence getTargetSequence() {
    	try {
    		String projectName = target.substring(0, target.indexOf('.'));
    		String sequenceName = target.substring(target.indexOf('.')+1);
    		Project p = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
    		return p.getSequenceByName(sequenceName);
    	} catch (Exception e) {}
		return null;
	}

	private void importVariableDefinition(RequestableObject requestable) throws EngineException {
    	if (!(requestable instanceof IVariableContainer))
    		return;
    	
    	IVariableContainer container = (IVariableContainer)requestable;
    	
		int size = container.numberOfVariables();
		for (int i=0; i<size; i++) {
			RequestableVariable variable = (RequestableVariable)container.getVariable(i);
			if (variable != null) {
				String variableName = variable.getName();
				if (getVariable(variableName) == null) {
					if (!StringUtils.isNormalized(variableName))
						throw new EngineException("Variable name is not normalized : \""+variableName+"\".");
					
					UIControlVariable uiVariable = new UIControlVariable();
					uiVariable.setName(variableName);
					uiVariable.setComment(variable.getDescription());
					uiVariable.setVarSmartType(new MobileSmartSourceType(variable.getDefaultValue().toString()));
					addUIComponent(uiVariable);

					uiVariable.bNew = true;
					uiVariable.hasChanged = true;
					hasChanged = true;
				}
			}
		}
    }
}
