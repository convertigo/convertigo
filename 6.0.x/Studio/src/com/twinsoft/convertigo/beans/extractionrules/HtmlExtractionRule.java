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

package com.twinsoft.convertigo.beans.extractionrules;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.apache.xpath.objects.XObject;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.beans.core.ExtractionRule;
import com.twinsoft.convertigo.beans.core.IXPathable;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.KeyExpiredException;
import com.twinsoft.convertigo.engine.MaxCvsExceededException;
import com.twinsoft.convertigo.engine.util.TwsCachedXPathAPI;

/**
 * This class defines the base class for extraction rules.
 *
 * <p>An extraction rule is seen as a JavaBean. So it is serializable,
 * and can have its own properties editor.</p>
 */
public abstract class HtmlExtractionRule extends ExtractionRule implements IXPathable {
	private static final long serialVersionUID = -3795226757497040142L;

	protected String xpath= "";
	
	protected transient TwsCachedXPathAPI xpathApi = null;
	protected transient Context context = null;
	protected transient NodeList resultList = null;
	
	private transient Map<String, String> wsTypes = null;
	
	/**
     * Constructs a new ExtractionRule object.
     */
    public HtmlExtractionRule() {
        super();
    }

	/**
	 * @return Returns the xpath.
	 */
	public String getXpath() {
		return xpath;
	}

	/**
	 * @param xpath The xpath to set.
	 */
	public void setXpath(String xpath) {
		this.xpath = xpath;
	}
    
	public Map<String, String> getSchemaTypes() {
		if (wsTypes == null)
			getSchema("p_ns");
		if (wsTypes == null)
			wsTypes = new HashMap<String, String>();
		return new HashMap<String, String>(wsTypes);
	}

	protected void addWsType(String typeName, String schema) {
		if (wsTypes == null)
			wsTypes = new HashMap<String, String>();
		wsTypes.put(typeName, schema);
	}
	
	public abstract String getSchema(String tns);
	
	public abstract String getSchemaElementName();
	
	public abstract String getSchemaElementType();
	
	public abstract String getSchemaElementNSType(String tns);
	
	protected boolean isRequestedObjectRunning() {
		if (context != null)
			return context.requestedObject.runningThread.bContinue;
		return false;
	}
	
	public boolean apply(Document xmlDom, Context context) {
		NodeList nodeList = null;
		boolean applying = false;
		int length = 0;
		
		this.context = context;
		xpathApi = context.getXpathApi();
		if (xpathApi == null)
			return false;
		
		try {
			nodeList = xpathApi.selectNodeList(xmlDom, xpath);
		}
		catch (TransformerException e) {
			return false;
		}
		
		if (nodeList == null)
			return false;
		
		length = nodeList.getLength();
		applying = (length > 0) ? true:false;
		
		if (applying)
			appendToOutputDom(nodeList, context.outputDocument);
		
		resultList = nodeList;
		return applying;
	}
	
	protected String getReferer(Context context) throws MaxCvsExceededException, KeyExpiredException {
		HtmlConnector htmlConnector = (HtmlConnector)context.getConnector();
		return htmlConnector.getHtmlParser().getReferer(context);
	}
	
	protected void appendToOutputDom(NodeList nodeList, Document outputDom) {
		Element elt, doc;
		Node node = null;
		int length = nodeList.getLength();
		
		doc = outputDom.getDocumentElement();
		for (int i=0; i< length; i++) {
			if (!isRequestedObjectRunning()) break;
			
			node = nodeList.item(i);
			elt = (Element)outputDom.importNode(node,true);
			doc.appendChild(elt);
			Engine.logBeans.trace("node '" + node.getNodeName() + "' added to result document.");
		}
	}
	
	public void addToScope(Scriptable scope) {
		// Do not add result to scripting scope by default
	}

	/**
	 * Returns the String value of a Node. 
	 * 
	 * @param node
	 * @param recurse
	 * @return
	 */
	public String getStringValue(Node node, boolean recurse)
	{
		String 	value ="";
		Node	item;
		
		// first check if the node is already a text node
		if (node.getNodeType() == Node.TEXT_NODE) {
			// yes it is, return the trimmed value
			if (recurse) {
				try {
					XObject Xobj = xpathApi.eval(node, "normalize-space(.)");
					return (Xobj.str().trim());
				} catch (TransformerException e) {
					return null;
				} 
			} else {
				value = node.getNodeValue().trim();
			}
			return value;
		} else {
			// Two cases
			if (recurse) {
				// User asked for recurse: use the normalize-space XSLT function
				try {
					XObject Xobj = xpathApi.eval(node, "normalize-space(.)");
					return (Xobj.str().trim());
				} catch (TransformerException e) {
					return null;
				} 
			} else {
				// no recurse : just append all Text child nodes of this node
				NodeList nl = node.getChildNodes();
				for (int i=0; i < nl.getLength(); i++) {
					item = nl.item(i);
					if (item.getNodeType() == Node.TEXT_NODE) {
						value +=  item.getNodeValue().trim();
					}
				}
				return value;
			}
		}
	}
	//-------- Report 4.5-----------------------------------------
	protected transient Object evaluated = null;
	
	public void evaluate(org.mozilla.javascript.Context javascriptContext, Scriptable scope, String source, String sourceName, boolean bDialog) throws EngineException {
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
}
