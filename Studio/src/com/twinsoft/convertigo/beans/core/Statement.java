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

package com.twinsoft.convertigo.beans.core;

import java.lang.reflect.Method;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineEvent;
import com.twinsoft.convertigo.engine.EngineException;

/**
 * The Statement class is the base class for all statements.
 */
public abstract class Statement extends DatabaseObject {
	private static final long serialVersionUID = 1113997185686423262L;
    
    public boolean isEnable = true;
    
    public Statement() {
        super();
		databaseType = "Statement";
		
		// Set priority to creation time since version 4.0.1
		this.priority = getNewOrderValue();
		this.newPriority = priority;
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
    	return new Long(priority);
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
		if (isEnable && htmlTransaction.runningThread.bContinue) {
			htmlTransaction.currentStatement = this;
			Engine.logBeans.debug("Executing statement named '"+ this.name +"' ("+ this.getClass().getName() +")");
			
			// We fire engine events only in studio mode.
            if (Engine.isStudioMode()) {
            	Engine.theApp.fireObjectDetected(new EngineEvent(this));
            }
			return true;
		}
		return false;
	}
    
	public boolean isEnable() {
		return isEnable;
	}

	public void setEnable(boolean isEnable) {
		this.isEnable = isEnable;
	}

	protected transient Object evaluated = null;
	
	protected void evaluate(Context javascriptContext, Scriptable scope, String source, String sourceName, boolean bDialog) throws EngineException {
		String message = null;
		evaluated = null;
		try {
			evaluated = javascriptContext.evaluateString(scope, source, sourceName, 1, null);
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
		clonedObject.newPriority = newPriority;
		return clonedObject;
	}
    
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.DatabaseObject#configure(org.w3c.dom.Element)
	 */
	@Override
	public void configure(Element element) throws Exception {
		super.configure(element);
		
		try {
			newPriority = new Long(element.getAttribute("newPriority")).longValue();
			if (newPriority != priority)
				hasChanged = true;
        }
        catch(Exception e) {
        	newPriority = getNewOrderValue();
        	Engine.logBeans.warn("The "+getClass().getName() +" object \"" + getName() + "\" has been updated to version \"4.0.1\"");
        	hasChanged = true;
        }
	}

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.DatabaseObject#write(java.lang.String)
	 */
	@Override
	public void write(String databaseObjectQName) throws EngineException {
		long l = priority;
		if (hasChanged && !isImporting)
			priority = newPriority;
		try {
			super.write(databaseObjectQName);
		}
		catch (EngineException e) {
			priority = l;
			throw e;
		}
	}
	
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.DatabaseObject#toXml(org.w3c.dom.Document)
	 */
	@Override
	public Element toXml(Document document) throws EngineException {
		Element element =  super.toXml(document);
		
        // Storing the object "newPriority" value
        element.setAttribute("newPriority", new Long(newPriority).toString());
		
		return element;
	}
}