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

package com.twinsoft.convertigo.engine.translators;

import java.util.List;

import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.ContextManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class PoolTranslator implements Translator {

	public void buildInputDocument(Context context, Object inputData) throws Exception {
        Engine.logBeans.debug("Making input document");

		String projectName = context.project.getName();
		String connectorName = context.getConnector().getName();
		String poolName = context.pool.getName();
		String poolContextID = ContextManager.getPoolContextID(projectName, connectorName, poolName, "" + context.poolContextNumber); 
        
		Element root = context.inputDocument.createElement("input");
		Element transactionVariablesElement = context.inputDocument.createElement("transaction-variables");
		Element javelinActionElement = context.inputDocument.createElement("javelin-action");

		context.inputDocument.appendChild(root);
		root.appendChild(transactionVariablesElement);
		root.appendChild(javelinActionElement);
        
		String sContextNumber, variableName = "n/a", variableValue = "n/a";

		org.mozilla.javascript.Context javascriptContext = org.mozilla.javascript.Context.enter();
		Scriptable scope = javascriptContext.initStandardObjects(null);

		try {		
			Integer iContextNumber = new Integer(context.poolContextNumber);
			Scriptable jsContextNumber = org.mozilla.javascript.Context.toObject(iContextNumber, scope);
			scope.put("contextNumber", scope, jsContextNumber);

			for(List<String> row : context.pool.getStartTransactionVariables()){
				try {
					variableName = row.get(1);
					sContextNumber = row.get(0);
		
					if (sContextNumber.equals("*") || (Integer.parseInt(sContextNumber) == context.poolContextNumber)) {
	
						variableValue = row.get(2);
						try {
							variableValue  = (String) javascriptContext.evaluateString(scope, variableValue, "", 1, null);
						}
						catch(EcmaError e) {
							Engine.logBeans.warn("(ContextManager) " + poolContextID + " ECMA error for the transaction variable \"" + variableName + "\": " + e.getMessage() + ". Setting the value without evaluation...");
						}
						catch(JavaScriptException e) {
							Engine.logBeans.warn("(ContextManager) " + poolContextID + " JavaScript error for the transaction variable \"" + variableName + "\": " + e.getMessage() + ". Setting the value without evaluation...");
						}
						catch(EvaluatorException e) {
							Engine.logBeans.warn("(ContextManager) " + poolContextID + " Unable to evaluate the transaction variable \"" + variableName + "\": " + e.getMessage() + ". Setting the value without evaluation...");
						}
						catch(ClassCastException e) {
							Engine.logBeans.warn("(ContextManager) " + poolContextID + " Unexpected type for the transaction variable \"" + variableName + "\": " + e.getMessage() + ". (java.lang.String was expected). Setting the value without evaluation...");
						}
						catch(Exception e) {
							Engine.logBeans.error("(ContextManager) " + poolContextID + " Unable to evaluate the transaction variable \"" + variableName + "\". Setting the value without evaluation...", e);
						}
	
						Engine.logBeans.debug("(ContextManager) " + poolContextID + " Add transation variable \"" + variableName + "\"=\"" + variableValue + "\"");
			
						Element item = context.inputDocument.createElement("variable");
						item.setAttribute("name", variableName);
						item.setAttribute("value", variableValue);
						Engine.logBeans.debug("(ContextManager) Added transaction variable '" + variableName + "' = '" + variableValue + "'");
						transactionVariablesElement.appendChild(item);
					}
				}
				catch(NumberFormatException e) {
					Engine.logBeans.warn("(ContextManager) " + poolContextID + " Unable to analyze the transation variable \"" + variableName + "\" (wrong session number)");
				}
			}
		}
		finally {
			if (javascriptContext != null) {
				org.mozilla.javascript.Context.exit();
			}
		}
	
		Engine.logBeans.debug("Input document created");
    }

	public Object buildOutputData(Context context, Object convertigoResponse) throws Exception {
		return convertigoResponse;
	}

	public String getContextName(byte[] data) throws Exception {
		throw new EngineException("The NullTranslator translator does not support the getContextName() method");
	}

	public String getProjectName(byte[] data) throws Exception {
		throw new EngineException("The NullTranslator translator does not support the getProjectName() method");
	}

}
