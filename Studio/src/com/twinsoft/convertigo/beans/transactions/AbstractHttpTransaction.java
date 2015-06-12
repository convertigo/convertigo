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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/branches/6.2.x/Studio/src/com/twinsoft/convertigo/beans/transactions/HttpTransaction.java $
 * $Author: julienda $
 * $Revision: 32341 $
 * $Date: 2012-10-30 15:07:42 +0100 (mar., 30 oct. 2012) $
 */

package com.twinsoft.convertigo.beans.transactions;

import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.xpath.XPathAPI;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.JavaScriptException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.beans.variables.RequestableHttpVariable;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.AttachmentManager;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EngineStatistics;
import com.twinsoft.convertigo.engine.enums.HttpMethodType;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public abstract class AbstractHttpTransaction extends TransactionWithVariables {
    
	private static final long serialVersionUID = 391756586112290476L;

	public static final String EVENT_DATA_RETRIEVED = "DataRetrieved";

    /** Holds value of property httpParameters. */
    private XMLVector<XMLVector<String>> httpParameters = new XMLVector<XMLVector<String>>();
    
    /** Stores value of running transaction's httpParameters. */
    transient private XMLVector<XMLVector<String>> currentHttpParameters = null;
    
    /** Holds value of property handleCookie. */
    private boolean handleCookie = true;
    
    /** Holds value of property statusCodeInfo. */
    private boolean httpInfo = false;
    private String httpInfoTagName = "HttpInfo";
    
    /** Holds value of property httpVerb. */
    private HttpMethodType httpVerb = HttpMethodType.GET;
    private String customHttpVerb = "";
    
    public HttpMethodType getHttpVerb() {
		return httpVerb;
	}

	public void setHttpVerb(HttpMethodType httpVerb) {
		this.httpVerb = httpVerb;
	}

	/** Holds value of property subDir. */
    private String subDir = "";
    
    /** Stores value of property subDir. */
    transient private String originalSubDir = null;
    
    /** Holds value of property requestTemplate. */
    private String requestTemplate = "";
    
    transient private AttachmentManager attachmentManager = null;

	private String urlEncodingCharset = "";

    public AbstractHttpTransaction() {
		super();
		
		XMLVector<String> line;
		line = new XMLVector<String>();
		line.add("Content-Type");
		line.add("application/x-www-form-urlencoded");
		httpParameters.add(line);

    }

    @Override
	public AbstractHttpTransaction clone() throws CloneNotSupportedException {
    	AbstractHttpTransaction abstractHttpTransaction = (AbstractHttpTransaction) super.clone();
    	abstractHttpTransaction.attachmentManager = null;
    	abstractHttpTransaction.currentHttpParameters = null;
    	abstractHttpTransaction.originalSubDir = null;
    	return abstractHttpTransaction;
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

		if (VersionUtils.compare(version, "3.1.8") < 0) {
			NodeList properties = element.getElementsByTagName("property");
			Element propValue = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "httpVariables");

			XMLVector<XMLVector<Long>> httpVariables = null;
			
			Node xmlNode = null;
			NodeList nl = propValue.getChildNodes();
			int len_nl = nl.getLength();
			for (int j = 0 ; j < len_nl ; j++) {
				xmlNode = nl.item(j);
				if (xmlNode.getNodeType() == Node.ELEMENT_NODE) {
					httpVariables = GenericUtils.cast(XMLUtils.readObjectFromXml((Element) xmlNode));
					continue;
				}
			}
			
			XMLVector<XMLVector<Long>> orderedVariables = getOrderedVariables();
			
			int len = orderedVariables.size();
			XMLVector<Long> line;
			for (int i = 0 ; i < len ; i++) {
				line = orderedVariables.elementAt(i);
				if (httpVariables.size()>0) {
					line.add(httpVariables.elementAt(i).elementAt(1));
					line.add(httpVariables.elementAt(i).elementAt(2));
				}
			}
			
			hasChanged = true;
			Engine.logBeans.warn("[HttpTransaction] The object \"" + getName() + "\" has been updated to version 3.1.8");
		}
		

		try {
			Node node = XPathAPI.selectSingleNode(element, "property[@name='httpVerb']/java.lang.Integer/@value");
			if (node != null) {
				httpVerb = HttpMethodType.values()[Integer.parseInt(node.getNodeValue())];
				hasChanged = true;
				Engine.logBeans.warn("[HttpTransaction] The object \"" + getName() + "\" has been updated to use the new 'httpVerb' format");
			}
		} catch (Throwable t) {
			// ignore migration errors
		}
    }
    
    /** Compatibility for version older than 4.6.0 **/
    @Deprecated
    public XMLVector<XMLVector<Object>> getVariablesDefinition() {
    	XMLVector<XMLVector<Object>> xmlv = new XMLVector<XMLVector<Object>>();
    	getVariablesList();
    	if (hasVariables()) {
    		for (int i=0; i<numberOfVariables(); i++) {
    			RequestableHttpVariable variable = (RequestableHttpVariable)getVariable(i);
    			
    			XMLVector<Object> v = new XMLVector<Object>();
    			v.addElement(variable.getName());
    			v.addElement(variable.getDescription());
    			v.addElement(variable.getDefaultValue());
    			v.addElement(variable.isWsdl());
    			v.addElement(variable.isMultiValued());
    			v.addElement(variable.isPersonalizable());
    			v.addElement(variable.isCachedKey());
    			v.addElement(variable.getHttpMethod());
    			v.addElement(variable.getHttpName());
    			
    			xmlv.addElement(v);
    		}
    	}
    	return xmlv;
    }

    @Override
	public void setStatisticsOfRequestFromCache() {
		context.statistics.add(EngineStatistics.APPLY_USER_REQUEST, 0);
	}

    @Override
	public void runCore() throws EngineException {
		HttpConnector connector = (HttpConnector) parent;			
		byte[] httpData = null;
		try {
            String t = context.statistics.start(EngineStatistics.APPLY_USER_REQUEST);

           
            try {
    			Engine.logBeans.debug("(HttpTransaction) Retrieving data...");
    			httpData = connector.getData(context);
    			Engine.logBeans.debug("(HttpTransaction) Data retrieved!");			
            }
            finally {
                context.statistics.stop(t);
            }

            // Applying handler
            executeHandler(EVENT_DATA_RETRIEVED, ((RequestableThread) Thread.currentThread()).javascriptContext);
			
			// Applying the underlying process
			makeDocument(httpData);
			
			score +=1;
		}
		catch(EngineException e) {
			//If we have an error we put the pure HTTP data
			if (httpData != null && getHttpInfo()) {
				Engine.logEngine.warn("(AbstractHttpTransaction) EngineException during transaction execution", e);
				
				Document document = connector.httpInfoElement.getOwnerDocument();
				Element err = document.createElement("errors");
				Element puredata = document.createElement("puredata");

				err.setAttribute("class", e.getClass().getCanonicalName());
				err.setTextContent(e.getLocalizedMessage());
				try{
					//if we have a text
					String stringData;
					if (requester.context.contentType.contains("text")) {
						stringData = new String ( httpData, requester.context.contentType.substring(requester.context.contentType.indexOf("=") + 1 ));
					//else a binary content
					} else {
						stringData = new String ( httpData );
					}
						//http://qualifpc:18080/convertigo/admin/services/logs.Get
					puredata.appendChild(document.createCDATASection(stringData));
				}catch(Exception e2){
					throw new EngineException("An unexpected exception occured while trying to decode the HTTP data.", e2);
				}
				connector.httpInfoElement.appendChild(err);
				connector.httpInfoElement.appendChild(puredata);
			} else {
				throw e;
			}
		}
		catch(MalformedURLException e) {
			throw new EngineException("The URL is malformed: " + connector.sUrl + "\nPlease check your project and/or transaction settings...", e);
		}
		catch(IOException e) {
			throw new EngineException("An IO exception occured while trying to connect to the URL.\nURL: " + connector.sUrl + "\nPost query: " + connector.postQuery, e);
		}
		catch(Exception e) {
			throw new EngineException("An unexpected exception occured while trying to get the document via HTTP.", e);
		}
		finally {
			//restoreVariablesDefinition();
			restoreVariables();
		}
	}
	
    @Override
	protected void restoreVariables() {
		if (needRestoreVariables) {
			resetSubDirToOriginal();
			resetHttpParametersToOriginal();
		}
		super.restoreVariables();
	}

    @Override
	protected void executeHandlerCore(String handlerType, org.mozilla.javascript.Context javascriptContext) throws EcmaError, EvaluatorException, JavaScriptException, EngineException {
		if (!AbstractHttpTransaction.EVENT_DATA_RETRIEVED.equals(handlerType)) {
			super.executeHandlerCore(handlerType, javascriptContext);
			return;
		}

		executeSimpleHandlerCore(handlerType, javascriptContext);
	}

    /* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.TransactionWithVariables#parseInputDocument(com.twinsoft.convertigo.engine.Context)
	 */
    @Override
	public void parseInputDocument(Context context) {
		super.parseInputDocument(context);
		
		// Overrides uri using given __uri request parameter
		NodeList uriNodes = context.inputDocument.getElementsByTagName("uri");
		if (uriNodes.getLength() == 1) {
			Element uriNode = (Element) uriNodes.item(0);
			String uri  = uriNode.getAttribute("value");
			if (!uri.equals("")) {
				setSubDir(uri);
				//needRestoreVariablesDefinition = true;
				needRestoreVariables = true;
			}
		}
		
		// Overrides static HTTP headers using __header_ request parameters 
		NodeList headerNodes = context.inputDocument.getElementsByTagName("header");
		int len = headerNodes.getLength();
		if (len > 0) {
			XMLVector<XMLVector<String>> headers = getCurrentHttpParameters();
			for (int i=0; i<len; i++) {
				Element headerNode = (Element) headerNodes.item(i);
				XMLVector<String> header = new XMLVector<String>();
				header.add(headerNode.getAttribute("name"));
				header.add(headerNode.getAttribute("value"));
				headers.add(header);
			}
			setCurrentHttpParameters(headers);
			//needRestoreVariablesDefinition = true;
			needRestoreVariables = true;
		}
	}

	public abstract void makeDocument(byte[] httpData) throws Exception;
    
	public AttachmentManager getAttachmentManager(){
		if(attachmentManager==null) attachmentManager = new AttachmentManager(this);
		return attachmentManager;
	}
    
    /** Getter for property httpParameters.
     * @return Value of property httpParameters.
     */
    public XMLVector<XMLVector<String>> getHttpParameters() {
        return this.httpParameters;
    }
    
    /** Setter for property httpParameters.
     * @param httpParameters New value of property httpParameters.
     */
    public void setHttpParameters(XMLVector<XMLVector<String>> httpParameters) {
        this.httpParameters = httpParameters;
    }
    
    public XMLVector<XMLVector<String>> getCurrentHttpParameters() {
    	if (currentHttpParameters == null) {
    		currentHttpParameters = GenericUtils.cast(httpParameters.clone());
    	}
        return currentHttpParameters;
    }

    public void setCurrentHttpParameters(XMLVector<XMLVector<String>> currentHttpParameters) {
    	this.currentHttpParameters = currentHttpParameters;
    }
    
    private void resetHttpParametersToOriginal() {
    	currentHttpParameters = null;
    }
    
    /** Getter for property handleCookie.
     * @return Value of property handleCookie.
     */
    public boolean isHandleCookie() {
        return this.handleCookie;
    }
    
    /** Setter for property handleCookie.
     * @param handleCookie New value of property handleCookie.
     */
    public void setHandleCookie(boolean handleCookie) {
        this.handleCookie = handleCookie;
    }
    
    /** Getter for property subDir.
     * @return Value of property subDir.
     */
    public String getSubDir() {
        return this.subDir;
    }
    
    /** Setter for property subDir.
     * @param subDir New value of property subDir.
     */
    public void setSubDir(String subDir) {
        this.subDir = subDir;
        if ((originalSubDir == null) || (originalSubDir.equals(this.subDir))) {
        	originalSubDir = subDir;
        }
    }
    
    private void resetSubDirToOriginal() {
    	if (originalSubDir != null)
    		this.subDir = originalSubDir;
    }

    /** Getter for property requestTemplate.
     * @return Value of property requestTemplate.
     */
	public String getRequestTemplate() {
		return requestTemplate;
	}

    /** Setter for property requestTemplate.
     * @param requestTemplate New value of property requestTemplate.
     */
	public void setRequestTemplate(String requestTemplate) {
		this.requestTemplate = requestTemplate;
	}
	
	/** Getter for property httpInfo.
     * @return Value of property httpInfo.
     */
	public boolean getHttpInfo(){
		return httpInfo;
	}
	
    /** Setter for property httpInfo.
     * @param httpInfo New value of property httpInfo.
     */
	public void setHttpInfo(boolean httpInfo) {
		this.httpInfo = httpInfo;
	}
	
	/** Getter for property httpInfoTagName.
     * @return Value of property httpInfoTagName.
     */
	public String getHttpInfoTagName() {
		return httpInfoTagName;
	}
	
    /** Setter for property httpInfoTagName.
     * @param httpInfoTagName New value of property httpInfoTagName.
     */
	public void setHttpInfoTagName(String httpInfoTagName) {
		this.httpInfoTagName = httpInfoTagName;
	}
	
    @Override
    public void add(DatabaseObject databaseObject) throws EngineException {
        if (databaseObject instanceof RequestableVariable) {
        	if (databaseObject instanceof RequestableHttpVariable)
        		addVariable((RequestableHttpVariable) databaseObject);
        	else throw new EngineException("You cannot add to an HttpTransaction object a database object of type " + databaseObject.getClass().getName());
        }
        else {
            super.add(databaseObject);
        }
    }
    
    @Override
    public void remove(DatabaseObject databaseObject) throws EngineException {
        if (databaseObject instanceof RequestableVariable) {
        	if (databaseObject instanceof RequestableHttpVariable)
        		removeVariable((RequestableHttpVariable) databaseObject);
        	else throw new EngineException("You cannot remove from an HttpTransaction object a database object of type " + databaseObject.getClass().getName());
        }
        else {
        	super.remove(databaseObject);
        }
    }

	public String getUrlEncodingCharset() {
		return urlEncodingCharset;
	}

	public void setUrlEncodingCharset(String urlEncodingCharset) {
		this.urlEncodingCharset = urlEncodingCharset;
	}
	
	public String getComputedUrlEncodingCharset() {
		String encoding = getUrlEncodingCharset();
		if (encoding == null || encoding.length() == 0) {
			encoding = getConnector().getUrlEncodingCharset();
		}
		return encoding;
	}
	
	@Override
	public HttpConnector getConnector() {
		return (HttpConnector) super.getConnector();
	}

	public String getCustomHttpVerb() {
		return customHttpVerb;
	}

	public void setCustomHttpVerb(String customHttpVerb) {
		this.customHttpVerb = customHttpVerb;
	}
}