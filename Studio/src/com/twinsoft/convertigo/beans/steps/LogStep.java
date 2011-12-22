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
 * $URL: http://sourceus/svn/CEMS_opensource/branches/6.0.x/Studio/src/com/twinsoft/convertigo/beans/statements/LogStatement.java $
 * $Author: fabienb $
 * $Revision: 28379 $
 * $Date: 2011-09-27 11:38:59 +0200 (mar., 27 sept. 2011) $
 */

package com.twinsoft.convertigo.beans.steps;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class LogStep extends Step implements ITagsProperty {

	/**
	 * 
	 */
	private static final long serialVersionUID = -700241502764617513L;
	private String level = Level.INFO.toString();
	private String logger = null;
	private String expression = "//todo";
	
	public LogStep() {
		super();
		if (this.logger == null) {
			this.logger = Engine.logContext.getName();
		}
	}

	public LogStep(String logger, String mode, String expression) {
		super();
		if (this.logger == null) {
			this.logger = Engine.logContext.getName();
		}
		this.logger = logger;
		this.level = mode;
		this.expression = expression;
	}
	
	protected String getEvalString(Context javascriptContext, Scriptable scope) throws EngineException {
		evaluate(javascriptContext, scope, expression, "LogStatement", true);
		return evaluated!=null?evaluated.toString():"";
	}

    @Override
	public String toString() {
		String text = this.getComment();
		return "(" + logger + ") : log."+level+"("+(expression.length()>20?expression.substring(0,20)+"...":expression)+")"+(!text.equals("") ? " // "+text:"");
	}
    

    @Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable) {
			if (super.stepExecute(javascriptContext, scope)) {
				Logger log = null;
				if(logger.equals(Engine.logEngine.getName())) 
					log = Engine.logEngine;
				else if(logger.equals(Engine.logContext.getName()))
					log = Engine.logContext;
				else if(logger.equals(Engine.logUser.getName()))
					log = Engine.logUser;
				else if(logger.equals(Engine.logAudit.getName())) 
					log = Engine.logAudit;
				if(level.equals(Level.WARN.toString()) && log.isEnabledFor(Level.WARN))
					log.warn(getEvalString(javascriptContext, scope));
				else if(level.equals(Level.INFO.toString()) && log.isInfoEnabled())
					log.info(getEvalString(javascriptContext, scope));
				else if(level.equals(Level.DEBUG.toString()) && log.isDebugEnabled())
					log.debug(getEvalString(javascriptContext, scope));
				else if(level.equals(Level.TRACE.toString()) && log.isTraceEnabled())
					log.trace(getEvalString(javascriptContext, scope));
				else if(level.equals(Level.ERROR.toString()) && log.isEnabledFor(Level.ERROR))
					log.error(getEvalString(javascriptContext, scope));
				return true;
			}
		}
		return false;
	}

	@Override
	public void configure(Element element) throws Exception {
		super.configure(element);
		
		String version = element.getAttribute("version");
        
		if (version == null) {
			String s = XMLUtils.prettyPrintDOM(element);
			EngineException ee = new EngineException("Unable to find version number for the database object \"" + getName() + "\".\nXML data: " + s);
			throw ee;
		}

		if (VersionUtils.compare(version, "4.6.0") < 0) {
			NodeList properties = element.getElementsByTagName("property");
			Element propValue = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "level");
	
			Node xmlNode = null;
			NodeList nl = propValue.getChildNodes();
			int len_nl = nl.getLength();
			for (int j = 0 ; j < len_nl ; j++) {
				xmlNode = nl.item(j);
				if (xmlNode.getNodeType() == Node.ELEMENT_NODE) {
					String sLevel = (String) XMLUtils.readObjectFromXml((Element) xmlNode);
					if (sLevel.equals("warning")) setLevel(Level.WARN.toString());
					else if (sLevel.equals("message")) setLevel(Level.INFO.toString());
					else if (sLevel.equals("debug")) setLevel(Level.DEBUG.toString());
					else if (sLevel.equals("debug2")) setLevel(Level.TRACE.toString());
					else if (sLevel.equals("debug3")) setLevel(Level.TRACE.toString());
					continue;
				}
			}
			
			hasChanged = true;
			Engine.logBeans.warn("(Transaction) The object \"" + getName() + "\" has been updated to version 4.6.0");
		}		
	}
    
	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("level")) {
			return new String[]{
					Level.ERROR.toString(),
					Level.WARN.toString(),
					Level.INFO.toString(),
					Level.DEBUG.toString(),
					Level.TRACE.toString(),
			};
		}
		if (propertyName.equals("logger")) {
			return new String[]{
					Engine.logContext.getName(),
					Engine.logAudit.getName(),
					Engine.logUser.getName(),
					Engine.logEngine.getName(),
			};
		}
		return new String[0];
	}

	@Override
	protected boolean workOnSource() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected StepSource getSource() {
		// TODO Auto-generated method stub
		return null;
	}
	
    @Override
	public String toJsString() {
		return expression;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	public String getLogger() {
		return logger;
	}

	public void setLogger(String logger) {
		this.logger = logger;
	}
}