/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

import java.io.File;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class IfFileExistStep extends BlockStep {

	private static final long serialVersionUID = 1615428447261700976L;

	transient protected XMLVector<XMLVector<String>> testDefinition = new XMLVector<XMLVector<String>>();
	
	public IfFileExistStep() {
		super();
	}
	
	public IfFileExistStep(String sourcePath) {
		super(sourcePath);
	}

	@Override
    public IfFileExistStep clone() throws CloneNotSupportedException {
    	IfFileExistStep clonedObject = (IfFileExistStep) super.clone();
        return clonedObject;
    }

	@Override
	public IfFileExistStep copy() throws CloneNotSupportedException {
		IfFileExistStep copiedObject = (IfFileExistStep)super.copy();
		return copiedObject;
	}

	@Override
	public String toString() {
		String condition = getCondition();
		return "ifFileExists(" + (condition.equals("") ? "??" : condition) + ")";
	}

	@Override
	public String toJsString() {
		String code = "";
		String condition = getCondition();
		if (!condition.equals("")) {
			code += " ifFileExists ("+ condition +") {\n";
			code += super.toString();
			code += " \n}\n";
		}
		return code;
	}

	@Override
	protected boolean hasToEvaluateBeforeNextStep() throws EngineException {
		return true;
	}
	
	private String evaluateSourcePath(Context javascriptContext, Scriptable scope) throws EngineException {
		String condition = getCondition();
		return evaluateToString(javascriptContext, scope, condition, "sourcePath", false);
	}

	@Override
	protected boolean executeNextStep(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnabled()) {
			boolean test = evaluateStep(javascriptContext, scope);
			return super.executeNextStep(test, javascriptContext, scope);
		}
		return false;
	}
	
	@Override
	protected boolean evaluateStep(Context javascriptContext, Scriptable scope) throws EngineException {
		try {	
			String sourceFilePath = getAbsoluteFilePath(evaluateSourcePath(javascriptContext, scope));
			File sourcefile = new File(sourceFilePath);
			
			if (sourcefile.exists()) {
				Engine.logBeans.info("File '" + sourceFilePath + "' exists");
				return true;
			}
			else {
				Engine.logBeans.info("File '" + sourceFilePath + "' does not exist");
				return false;
			}
		} catch (Exception e) {
			setErrorStatus(true);
            Engine.logBeans.error("An error occured while testing if the file or directory exists.", e);
		}		       
		return false;
	}

	protected String getAbsoluteFilePath(String entry) throws EngineException {
		if (entry.equals(""))
			throw new EngineException("Please fill the Source property field.");
				
		return Engine.theApp.filePropertyManager.getFilepathFromProperty(entry, getProject().getName());
	}
}
