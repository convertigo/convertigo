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

package com.twinsoft.convertigo.beans.statements;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.parsers.events.AbstractEvent;
import com.twinsoft.convertigo.engine.parsers.events.MouseEvent;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class MouseAdvanceStatement extends MouseStatement {
	private static final long serialVersionUID = -8671654909869364944L;

	private String screenX = "-1";
	private String screenY = "-1";
	private String clientX = "-1";
	private String clientY = "-1";
	private String ctrlKey = "false";
	private String altKey = "false";
	private String shiftKey = "false";
	private String metKey = "false";
	private String button = "0";
	
	public MouseAdvanceStatement() {
		super();
	}

	public MouseAdvanceStatement(String xpath) {
		super(xpath);
	}

	public MouseAdvanceStatement(String action, String xpath) {
		super(action, xpath);
	}

	@Override
	public AbstractEvent getEvent(Context javascriptContext, Scriptable scope) {		
		int screenX = -1;
		int screenY = -1;
		int clientX = -1;
		int clientY = -1;
		boolean ctrlKey = false;
		boolean altKey = false;
		boolean shiftKey = false;
		boolean metKey = false;
		short button = 0;
		
		try {
			Object evaluated = javascriptContext.evaluateString(scope, this.screenX, "screenX", 0, null);
			evaluated = Context.toNumber(evaluated);
			screenX = (Integer) Context.jsToJava(evaluated, Integer.class);
		} catch (Exception e) {
			Engine.logBeans.warn("(MouseAdvanceStatement) failed to evaluate 'screenX' property. " + e.getClass().getCanonicalName());
		}
		
		try {
			Object evaluated = javascriptContext.evaluateString(scope, this.screenY, "screenY", 0, null);
			evaluated = Context.toNumber(evaluated);
			screenY = (Integer) Context.jsToJava(evaluated, Integer.class);
		} catch (Exception e) {
			Engine.logBeans.warn("(MouseAdvanceStatement) failed to evaluate 'screenY' property. " + e.getClass().getCanonicalName());
		}
		
		try {
			Object evaluated = javascriptContext.evaluateString(scope, this.clientX, "clientX", 0, null);
			evaluated = Context.toNumber(evaluated);
			clientX = (Integer) Context.jsToJava(evaluated, Integer.class);
		} catch (Exception e) {
			Engine.logBeans.warn("(MouseAdvanceStatement) failed to evaluate 'clientX' property. " + e.getClass().getCanonicalName());
		}
		
		try {
			Object evaluated = javascriptContext.evaluateString(scope, this.clientY, "clientY", 0, null);
			evaluated = Context.toNumber(evaluated);
			clientY = (Integer) Context.jsToJava(evaluated, Integer.class);
		} catch (Exception e) {
			Engine.logBeans.warn("(MouseAdvanceStatement) failed to evaluate 'clientY' property. " + e.getClass().getCanonicalName());
		}
		
		try {
			Object evaluated = javascriptContext.evaluateString(scope, this.ctrlKey, "ctrlKey", 0, null);
			ctrlKey = Context.toBoolean(evaluated);
		} catch (Exception e) {
			Engine.logBeans.warn("(MouseAdvanceStatement) failed to evaluate 'ctrlKey' property. " + e.getClass().getCanonicalName());
		}
		
		try {
			Object evaluated = javascriptContext.evaluateString(scope, this.altKey, "altKey", 0, null);
			altKey = Context.toBoolean(evaluated);
		} catch (Exception e) {
			Engine.logBeans.warn("(MouseAdvanceStatement) failed to evaluate 'altKey' property. " + e.getClass().getCanonicalName());
		}
		
		try {
			Object evaluated = javascriptContext.evaluateString(scope, this.shiftKey, "shiftKey", 0, null);
			shiftKey = Context.toBoolean(evaluated);
		} catch (Exception e) {
			Engine.logBeans.warn("(MouseAdvanceStatement) failed to evaluate 'shiftKey' property. " + e.getClass().getCanonicalName());
		}
		
		try {
			Object evaluated = javascriptContext.evaluateString(scope, this.metKey, "metKey", 0, null);
			metKey = Context.toBoolean(evaluated);
		} catch (Exception e) {
			Engine.logBeans.warn("(MouseAdvanceStatement) failed to evaluate 'metKey' property. " + e.getClass().getCanonicalName());
		}
		
		try {
			Object evaluated = javascriptContext.evaluateString(scope, this.button, "button", 0, null);
			evaluated = Context.toNumber(evaluated);
			button = (Short) Context.jsToJava(evaluated, Short.class);
		} catch (Exception e) {
			Engine.logBeans.warn("(MouseAdvanceStatement) failed to evaluate 'button' property. " + e.getClass().getCanonicalName());
		}
		
		return new MouseEvent(getXpath(), getAction(), screenX, screenY, clientX, clientY, ctrlKey, altKey, shiftKey, metKey, button);
	}
	
	@Override
	public String toString() {
		return 	("true".equals(ctrlKey) ? " + ctrl" : "") +
			("true".equals(altKey) ? " + alt" : "") +
			("true".equals(shiftKey) ? " + shift" : "") +
			("true".equals(metKey) ? " + meta" : "") +
			(!"-1".equals(screenX) || !"-1".equals(screenY) ? " + screen(" + screenX + "," + screenY + ")" : "") +
			(!"-1".equals(clientX) || !"-1".equals(clientY) ? " + client(" + clientX + "," + clientY + ")" : "") +
			" button(" + button + ")" +
			super.toString();
	}
	
	public String getClientX() {
		return clientX;
	}

	public void setClientX(String clientX) {
		this.clientX = clientX;
	}

	public String getClientY() {
		return clientY;
	}

	public void setClientY(String clientY) {
		this.clientY = clientY;
	}

	public String getScreenX() {
		return screenX;
	}

	public void setScreenX(String screenX) {
		this.screenX = screenX;
	}

	public String getScreenY() {
		return screenY;
	}

	public void setScreenY(String screenY) {
		this.screenY = screenY;
	}

	public String getAltKey() {
		return altKey;
	}

	public void setAltKey(String altKey) {
		this.altKey = altKey;
	}

	public String getButton() {
		return button;
	}

	public void setButton(String button) {
		this.button = button;
	}

	public String getCtrlKey() {
		return ctrlKey;
	}

	public void setCtrlKey(String ctrlKey) {
		this.ctrlKey = ctrlKey;
	}

	public String getMetKey() {
		return metKey;
	}

	public void setMetKey(String metKey) {
		this.metKey = metKey;
	}

	public String getShiftKey() {
		return shiftKey;
	}

	public void setShiftKey(String shiftKey) {
		this.shiftKey = shiftKey;
	}
	
    @Override
    public void configure(Element element) throws Exception {
    	super.configure(element);
    	
    	String version = element.getAttribute("version");
    	
    	if (version!= null && VersionUtils.compare(version, "7.0.0") < 0) {
    		Engine.logDatabaseObjectManager.info("Migration to 7.0.0 for MouseAdvanceStatement, migrate to javascriptable properties.");
    		Document doc = element.getOwnerDocument();
    		
    		for (String tagName : new String[]{"java.lang.Short", "java.lang.Integer", "java.lang.Boolean"}) {
        		for (Node node : XMLUtils.toNodeArray(element.getElementsByTagName(tagName))) {
        			Element exElt = (Element) node;
        			Element newElt = doc.createElement("java.lang.String");
        			newElt.setAttribute("value", exElt.getAttribute("value"));
        			exElt.getParentNode().replaceChild(exElt, newElt);
        		}    			
    		}
    	}
    }
}