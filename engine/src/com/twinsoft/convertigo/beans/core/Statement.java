/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

package com.twinsoft.convertigo.beans.core;

import java.lang.reflect.Method;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.beans.core.DatabaseObject.DboCategoryInfo;
import com.twinsoft.convertigo.beans.statements.HTTPStatement;
import com.twinsoft.convertigo.beans.statements.IThenElseStatementContainer;
import com.twinsoft.convertigo.beans.statements.IfStatement;
import com.twinsoft.convertigo.beans.statements.IfThenElseStatement;
import com.twinsoft.convertigo.beans.statements.IfXpathExistsStatement;
import com.twinsoft.convertigo.beans.statements.IfXpathExistsThenElseStatement;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineEvent;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.RhinoUtils;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

/**
 * The Statement class is the base class for all statements.
 */
@DboCategoryInfo(
		getCategoryId = "Statement",
		getCategoryName = "Statement",
		getIconClassCSS = "convertigo-action-newStatement"
	)
public abstract class Statement extends DatabaseObject implements IEnableAble {
	private static final long serialVersionUID = 1113997185686423262L;
    
	private boolean isEnabled = true;
    
    public Statement() {
        super();
		databaseType = "Statement";
		
		// Set priority to creation time since version 4.0.1
		this.priority = getNewOrderValue();
	}
    
    /*public Statement(String expression) {
        super();
		databaseType = "Statement";
	}*/
	
    /**
     * Get order for quick sort.
     */
    @Override
    public Object getOrderedValue() {
    	return priority;
    }

	@Override
	public HtmlConnector getConnector(){
		return (HtmlConnector) super.getConnector();
	}

	public HtmlTransaction getParentTransaction() {
		HtmlTransaction transaction = null;
    	while (parent instanceof Statement)
    		return ((Statement)parent).getParentTransaction();
    	if (parent instanceof HtmlTransaction)
    		transaction = (HtmlTransaction)parent;
    	return transaction;
    }
    
	protected void reset() {
		// Do nothing by default
	}
	
	public boolean execute(org.mozilla.javascript.Context javascriptContext, org.mozilla.javascript.Scriptable scope) throws EngineException
	{
		reset();
		HtmlTransaction htmlTransaction = (HtmlTransaction)getParentTransaction();
		if (isEnabled && htmlTransaction.runningThread.bContinue) {
			htmlTransaction.currentStatement = this;
			Engine.logBeans.debug("Executing statement named '"+ this.getName() +"' ("+ this.getClass().getName() +")");
			
			// We fire engine events only in studio mode.
            if (Engine.isStudioMode()) {
            	Engine.theApp.fireObjectDetected(new EngineEvent(this));
            }
			return true;
		}
		return false;
	}
    
	@Override
	public boolean isEnabled() {
		return isEnabled;
	}

	@Override
	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	protected transient Object evaluated = null;
	
	protected void evaluate(Context javascriptContext, Scriptable scope, String source, String sourceName, boolean bDialog) throws EngineException {
		String message = null;
		evaluated = null;
		try {
			evaluated = RhinoUtils.evalCachedJavascript(javascriptContext, scope, source, sourceName, 1, null);
		}
		catch(EcmaError e) {
			message = "Unable to evaluate statement expression code for '"+ sourceName +"' property or variable.\n" +
			"Statement: \"" + getName() + "\"\n" +
			"A Javascript runtime error has occured at line " + 
			e.lineNumber() + ", column " + e.columnNumber() + ": " +
			e.getMessage() + " \n" + e.lineSource();
			logException(e,message, bDialog);
		}
		catch(EvaluatorException e) {
			message = "Unable to evaluate statement expression code for '"+ sourceName +"' property or variable.\n" +
			"Statement: \"" + getName() + "\"\n" +
			"A Javascript evaluation error has occured: " + e.getMessage();
			logException(e,message, bDialog);
		}
		catch(JavaScriptException e) {
			message = "Unable to evaluate statement expression code for '"+ sourceName +"' property or variable.\n" +
			"Statement: \"" + getName() + "\"\n" +
			"A Javascript error has occured: " + e.getMessage();
			logException(e,message, bDialog);
		}
		catch(Exception e) {
			message = "unknown exception : " + e.getMessage();
		}
		finally {
			if (message != null) {
				EngineException ee = new EngineException(message);
				throw ee;
			}
		}
	}
		
	protected void logException(Throwable e, String message, boolean bDialog) {
   		try {
			Class<?> c = Class.forName("com.twinsoft.convertigo.eclipse.ConvertigoPlugin");
			if (c != null) {
				Object args[] = {null,"error",Boolean.TRUE};
				args[0] = e;
				args[1] = message;
				args[2] = Boolean.valueOf(bDialog);
				try {
					Method method = c.getMethod("logException", new Class[] {Throwable.class, String.class, Boolean.class});
					method.invoke(c,args);
				} catch (Exception ee) {
					;
				}
			}
		} catch (ClassNotFoundException ee) {
			;
		}
	}
	
	protected void logWarning(Throwable e, String message, boolean bDialog) {
   		try {
			Class<?> c = Class.forName("com.twinsoft.convertigo.eclipse.ConvertigoPlugin");
			if (c != null) {
				Object args[] = {e,"warning",Boolean.TRUE};
				args[0] = e;
				args[1] = message;
				args[2] = Boolean.valueOf(bDialog);
				try {
					Method method = c.getMethod("logWarning", new Class[] {Throwable.class, String.class, Boolean.class});
					method.invoke(c,args);
				} catch (Exception ee) {
					;
				}
			}
		} catch (ClassNotFoundException ee) {
			;
		}
	}

	protected void logInfo(String message) {
   		try {
			Class<?> c = Class.forName("com.twinsoft.convertigo.eclipse.ConvertigoPlugin");
			if (c != null) {
				Object args[] = {"info"};
				args[0] = message;
				try {
					Method method = c.getMethod("logInfo", new Class[] {String.class});
					method.invoke(c,args);
				} catch (Exception ee) {
					;
				}
			}
		} catch (ClassNotFoundException ee) {
			;
		}
	}
	
	public abstract String toJsString();
	
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.DatabaseObject#clone()
	 */
	@Override
	public Statement clone() throws CloneNotSupportedException {
		Statement clonedObject = (Statement) super.clone();
		return clonedObject;
	}
    
	@Override
	public void preconfigure(Element element) throws Exception {
		super.preconfigure(element);
		
		String version = element.getAttribute("version");
		if (VersionUtils.compare(version, "7.5.0") < 0) {
			NodeList properties = element.getElementsByTagName("property");
			
			Element propName = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "isEnable");
			if (propName != null) {
				propName.setAttribute("name", "isEnabled");
				hasChanged = true;
			}
			
			Engine.logBeans.warn("[Statement] The object \"" + getName() + "\" has been updated to version 7.5.0 (property \"isEnable\" changed to \"isEnabled\")");
		}
	}

	@Override
	public boolean testAttribute(String name, String value) {
		if (name.equals("isEnable")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(isEnabled()));
		}
		if (name.equals("isHttpStatement")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(this instanceof HTTPStatement));
		}
		if (name.equals("isThenElseStatement")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(this instanceof IThenElseStatementContainer));
		}
		if (name.equals("canChangeTo")) {
			Boolean bool = Boolean.valueOf(value);
			return 	bool.equals(Boolean.valueOf(this instanceof IfStatement)) ||
					bool.equals(Boolean.valueOf(this instanceof IfThenElseStatement)) ||
					bool.equals(Boolean.valueOf(this instanceof IfXpathExistsStatement)) ||
					bool.equals(Boolean.valueOf(this instanceof IfXpathExistsThenElseStatement));
		}
		return super.testAttribute(name, value);
	}

}