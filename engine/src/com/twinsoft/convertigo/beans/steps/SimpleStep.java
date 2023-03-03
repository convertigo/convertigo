/*
 * Copyright (c) 2001-2022 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.beans.steps;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.core.IJScriptContainer;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.StringUtils;

/**
 * 
 * @author nathalieh
 *
 */public class SimpleStep extends Step implements IJScriptContainer {

	private static final long serialVersionUID = -3775791876054667383L;

	private String expression = "//todo";
	
	public SimpleStep() {
		super();
	}

	public SimpleStep(String expression) {
		super();
		this.expression = expression;
	}

	@Override
    public SimpleStep clone() throws CloneNotSupportedException {
    	SimpleStep clonedObject = (SimpleStep) super.clone();
        return clonedObject;
    }

	@Override
    public SimpleStep copy() throws CloneNotSupportedException {
    	SimpleStep copiedObject = (SimpleStep) super.copy();
        return copiedObject;
    }

	@Override
	public String toString() {
		return this.getName();
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		if (!this.expression.equals(expression)) {
			this.expression = expression;
			changed();
		}
	}
	
	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnabled()) {
			if (super.stepExecute(javascriptContext, scope)) {
				evaluate(javascriptContext, scope, getExpression(), "expression", true);
				sequence.context.updateUserInLog();
				return true;
			}
		}
		return false;
	}
	
	public String toJsString() {
		return expression;
	}
	
	@Override
	protected void onBeanNameChanged(String oldName, String newName) {
		if (oldName.startsWith(StringUtils.normalize(expression))) {
			expression = newName;
			hasChanged = true;
		}
	}
}
