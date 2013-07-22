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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class LogStatement extends Statement implements ITagsProperty {
	private static final long serialVersionUID = 4017811795869683136L;
	
	private String level = Level.INFO.toString();
	private String expression = "//todo";
	private boolean engine = false;
	
	public LogStatement() {
		super();
	}

	public LogStatement(String mode, String expression) {
		super();
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
		return "log."+level+"("+(expression.length()>20?expression.substring(0,20)+"...":expression)+")"+(!text.equals("") ? " // "+text:"");
	}

    @Override
	public boolean execute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			if (super.execute(javascriptContext, scope)) {
				Logger log = engine ? Engine.logEngine : Engine.logContext;
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

	public boolean getEngine() {
		return engine;
	}

	public void setEngine(boolean engine) {
		this.engine = engine;
	}

	public String[] getTagsForProperty(String propertyName) {
		if(propertyName.equals("level")){
			return new String[]{
					Level.ERROR.toString(),
					Level.WARN.toString(),
					Level.INFO.toString(),
					Level.DEBUG.toString(),
					Level.TRACE.toString(),
			};
		}
		return new String[0];
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
}