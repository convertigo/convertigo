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

package com.twinsoft.convertigo.engine.requesters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Locale;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.regexp.RE;
import org.apache.regexp.REUtil;
import org.apache.xml.resolver.tools.CatalogResolver;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import com.ms.xml.ParseError;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ISheetContainer;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Sheet;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EngineStatistics;
import com.twinsoft.convertigo.engine.ExpiredSecurityTokenException;
import com.twinsoft.convertigo.engine.JobManager;
import com.twinsoft.convertigo.engine.NoSuchSecurityTokenException;
import com.twinsoft.convertigo.engine.SecurityToken;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.util.Log4jHelper;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.Log4jHelper.mdcKeys;

public abstract class GenericRequester extends Requester {
	
    public GenericRequester() {
    }

    @Override
	public void checkSecuredConnection() throws EngineException {
		// Default implementation: nothing to check
	}
	
	@Override
	public void checkAccessibility() throws EngineException {
		// Default implementation: nothing to check
	}

//	@Override
//	public void checkAuthenticatedContext() throws EngineException {
//		
//		context.portalUserName = null;
//		
//		if (context.httpSession != null) {
//			Object authenticatedUser = context.httpSession.getAttribute("authenticatedUser");
//			if (authenticatedUser != null) {
//				// If there is an authenticated user in the HTTP session, we consider
//				// the context as authenticated "from the top", and then copy the user
//				// into the context.
//				context.portalUserName = authenticatedUser.toString();
//				Engine.logContext.debug("Authenticated user added in the context from the HTTP session");
//			}
//		}
//		
//		if (context.requestedObject.getAuthenticatedContextRequired()) {
//			Engine.logContext.debug("Authenticated context required");
//			if ("(anonymous)".equals(context.getAuthenticatedUser())) {
//				throw new EngineException("Authentication required");
//			}
//		}
//	}
	
	@Override
	public void checkAuthenticatedContext() throws EngineException {
		
		if ( context.getAuthenticatedUser() != null ) {
			Engine.logContext.debug("The context is authenticated via the HTTP session");
		} else if (context.requestedObject.getAuthenticatedContextRequired()) {
			Engine.logContext.debug("Authenticated context required");
			throw new EngineException("Authentication required");
		} 
	}

	private String findBrowserFromUserAgent(Project project, String userAgent) {
        Vector<?> browserDefinitions = project.getBrowserDefinitions();
        if (browserDefinitions != null) {
            browserDefinitions.trimToSize();
            int len = browserDefinitions.size();
            String keyword, browser;
            RE regexp;
            for (int i = 0 ; i < len ; i++) {
                browser = (String) ((Vector<?>) browserDefinitions.elementAt(i)).elementAt(0);
                keyword = (String) ((Vector<?>) browserDefinitions.elementAt(i)).elementAt(1);
                try {
                    regexp = REUtil.createRE(keyword);
                    if (regexp.match(userAgent)) {
                        return browser;
                    }
                }
                catch(Exception e) {
                    Engine.logContext.error("Unable to parse keyword regular expression for browser \"" + browser + "\"", e);
                }
            }
        }
        
        return Sheet.BROWSER_ALL;
    }
    
    @Override
    public synchronized org.w3c.dom.Document createDOM(String encodingCharSet) throws EngineException {
    	boolean bLog = !(context.requestedObject instanceof Sequence);

    	if (bLog) Engine.logContext.trace("[" + getName() + "] creating DOM");
    	Document document = null;

    	String xmlEngine = EnginePropertiesManager.getProperty(EnginePropertiesManager.PropertyName.DOCUMENT_XML_ENGINE);
    	if (bLog) Engine.logContext.trace("Required XML engine: " + xmlEngine);

    	// Java default XML engine
    	if (xmlEngine.equals("java")) {         	
    		document = XMLUtils.getDefaultDocumentBuilder().newDocument();
    	}
    	// MSXML document
    	else if (xmlEngine.equals("msxml")) {
    		document = new com.ms.xml.Document();
    	}
    	else {
    		throw new EngineException("Unknown XML engine (" + xmlEngine + "), please check your Convertigo engine properties!");
    	}

    	if (bLog) Engine.logContext.trace("XML class: " + document.getClass().getName());

    	ProcessingInstruction pi = document.createProcessingInstruction("xml", "version=\"1.0\" encoding=\"" + encodingCharSet + "\"");
    	document.appendChild(pi);

    	return document;
    }

    public org.w3c.dom.Document createDomWithNoXMLDeclaration(String encodingCharSet) throws EngineException, ParserConfigurationException {
        Document document = null;
        
        String xmlEngine = EnginePropertiesManager.getProperty(EnginePropertiesManager.PropertyName.DOCUMENT_XML_ENGINE);
        
        // Java default XML engine
        if (xmlEngine.equals("java")) {
            document = XMLUtils.getDefaultDocumentBuilder().newDocument();
        }
        // MSXML document
        else if (xmlEngine.equals("msxml")) {
            document = new com.ms.xml.Document();
        }
        else {
            throw new EngineException("Unknown XML engine (" + xmlEngine + "), please check your Convertigo engine properties!");
        }

        return document;
    }
    
    @Override
    public synchronized Document parseDOM(String xml) throws EngineException {
        Engine.logContext.debug("GenericRequester: parsing DOM");

        try {
            Document document = null;
            
            String xmlEngine = EnginePropertiesManager.getProperty(EnginePropertiesManager.PropertyName.DOCUMENT_XML_ENGINE);
            Engine.logContext.debug("Required XML engine: " + xmlEngine);
            
            // Java default XML engine
            if (xmlEngine.equals("java")) {
        		document = XMLUtils.getDefaultDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            }
            // MSXML engine
            else if (xmlEngine.equals("msxml")) {
            	com.ms.xml.Document msxmlDocument = new com.ms.xml.Document();
            	document = msxmlDocument;
            	msxmlDocument.setAsync(false);
                if (!msxmlDocument.loadXML(xml)) {
                	ParseError error = msxmlDocument.getParseError();
                	throw new Exception("Unable to parse the XML document.\nThe following error #" + error.getErrorCode() + " occurs at line " + error.getLine() + ", column " + error.getLinepos() + ": " + error.getReason() + ".\nLine: " + error.getSrcText());
                }
            }
            else {
                throw new EngineException("Unknown XML engine (" + xmlEngine + "), please check your Convertigo engine properties!");
            }
            
            return document;
        }
        catch(Exception e) {
        	if (context.isCacheEnabled) context.isCacheEnabled = false;
        	throw new EngineException("Unable to parse the XML document:\n" + xml, e);
        }
    }
    
	public abstract void preGetDocument() throws Exception;

	public Object postGetDocument(Document document) throws Exception {
		Engine.logContext.debug("postGetDocument()");

		String copyright = "\nGenerated by Convertigo Entreprise Mashup Server\n";
		copyright += "Requester: " + getName() + "\n";

		Comment comment = document.createComment(copyright);
		document.appendChild(comment);
		
		NodeList attachs = document.getDocumentElement().getElementsByTagName("attachment");
		if ((attachs != null) && (attachs.getLength()>0)) {
			return document;
		}
		
		if (context.isXsltRequest) {
			if (context.absoluteSheetUrl == null) {
	            String browser = findBrowserFromUserAgent(context.project, context.userAgent);
	            Engine.logContext.debug("Browser for stylesheet: \"" + browser + "\"");
	
	            findStyleSheet(browser);
			}
            setStyleSheet(document);

			Object result = performXSLT(document);
        	return result;
		}
		else {
			return document;
		}
    }

	@Override
    public final Object processRequest(Object inputData) throws Exception {
        if (Engine.theApp == null) throw new EngineException("Unable to process the request: the Convertigo engine is not started!");

		Object result = null;

    	try {
			this.inputData = inputData;
			context = getContext();

			Engine.logContext.debug("[" + getName() + "] Locking the working semaphore...");

			synchronized(context) {
				context.waitingRequests++;
				Engine.logContext.debug("[" + getName() + "] Working semaphore locked (" + context.waitingRequests + " requests(s) pending) [" + context.hashCode() + "]");

				// Update log4j context infos
				Log4jHelper.mdcInit(context);
				long uniqueRequestID = System.currentTimeMillis() + (long) (Math.random() * 1261440000000L);
				Log4jHelper.mdcPut(mdcKeys.UID, Long.toHexString(uniqueRequestID));
				Log4jHelper.mdcPut(mdcKeys.ContextID, context.contextID);
				Log4jHelper.mdcPut(mdcKeys.Project, context.projectName);

				Engine.logContext.trace("[" + getName() + "] start");
				
    			context.statistics.clearLatestDurations();
    			String t = context.statistics.start(EngineStatistics.REQUEST);
	    		
	            try {
	            	if ((context.isAsync) && (JobManager.jobExists(context.contextID))) { 
	                	Engine.logContext.debug("[" + getName() + "] Context is async and job is running, do not initialize context");
	                } else {
	                	Engine.logContext.trace("[" + getName() + "] Context is not async and no job are running");
	                	initContext(context);
	            		
	                	if (context.sequenceName != null) 

	                		Log4jHelper.mdcPut(mdcKeys.Sequence, context.sequenceName);
	                	if (context.transactionName != null) {
		            		Log4jHelper.mdcPut(mdcKeys.Connector, context.connectorName);
		            		Log4jHelper.mdcPut(mdcKeys.Transaction, context.transactionName);
	                	}
	            		Log4jHelper.mdcPut(mdcKeys.User, context.tasUserName == null ? "(anonymous)" : "'" + context.tasUserName + "'");

	                }
					
					result = coreProcessRequest();
					result = getTranslator().buildOutputData(context, result);
	            } finally {
    				context.statistics.stop(t);
    				
    				// Bugfix for #853 (Tomcat looses parameters without any explanation)
    				// Remove the request and session references from the context
    				// Moved here by #2254
    				Engine.logContext.trace("Bugfix #853, #2254");
    				Engine.logContext.trace("context=" + context);
    				if (context != null) {
    					Engine.logContext.trace("Removing request and session objects from the context");
    					context.clearRequest();
    				}
	            }
			}
    	} finally {
    		// Remove all MDC values for clean release of the thread
    		Log4jHelper.mdcClear();
    		
    		if (context != null) {
    			String stats = null;
    			
    			if (Engine.logStatistics.isInfoEnabled()) {
    				stats = context.statistics.printStatistics();
    				Engine.logStatistics.info("Statistics:\n" + stats);
    			}

    			String s = EnginePropertiesManager.getProperty(EnginePropertiesManager.PropertyName.DOCUMENT_INCLUDE_STATISTICS);
    			boolean bStatistics = (s.equalsIgnoreCase("true"));
    			if (bStatistics){
    				result = addStatisticsAsText(stats, result);
    			}
    			
    			// Requestable data statistics
    			addStatisticsAsData(result);
    			
    			context.waitingRequests--;
        		Engine.logContext.debug("[" + getName() + "] Working semaphore released (" + context.waitingRequests + " request(s) pending) [" + context.hashCode() + "]");
    		}
    		Engine.logContext.debug("[" + getName() + "] end of request");
    	}

    	return result;
    }
    
	abstract protected Object addStatisticsAsText(String stats, Object result) throws UnsupportedEncodingException;

	abstract protected Object addStatisticsAsData(Object result);
	
    protected String poolName = null;
    protected String projectName = null;
	protected String sequenceName = null;
	protected String connectorName = null;
    
    protected abstract void initInternalVariables() throws EngineException;

    @Override
    public void initContext(Context context) throws Exception {
        Engine.logContext.trace("Initializing context");

		String previousProjectName = (String) context.projectName;
		
		context.reset();

		long lastAccessTime = Calendar.getInstance().getTime().getTime();
		Engine.logContext.trace("(ContextManager) updating the last access time for context " + context.contextID + ": " + lastAccessTime);
		context.lastAccessTime = lastAccessTime;

		// Update project if user request requires it.
		if ((previousProjectName == null) || (!previousProjectName.equalsIgnoreCase(projectName))) {
	        Engine.logContext.debug("Project name differs from previous one; requiring new session");
			context.isNewSession = true;
			context.projectName = projectName;
		}
	}

	protected void handleParameter(Context context, String parameterName, String parameterValue) throws NoSuchSecurityTokenException, ExpiredSecurityTokenException {
		// This gives the required context name
		if (parameterName.equals(Parameter.Context.getName())) {
			Engine.logContext.debug("Required context: " + parameterValue);
			// This parameter is not handled here
		}
		// Parameter for removing namespaces
		else if (parameterName.equals(Parameter.RemoveNamespaces.getName())) {
			Engine.logContext.debug("Namespaces removal required");
			context.removeNamespaces = true;
		}
		// This means "Async mode"
		else if (parameterName.equals(Parameter.Async.getName())) {
			context.isAsync = (parameterValue.equalsIgnoreCase("true") || parameterValue.equalsIgnoreCase("1")) ? true : false;
			if (context.isAsync)
				Engine.logContext.debug("The transaction will be or is being processed asynchroneously.");
		}
		// This is the overridden sequence
		else if (parameterName.equals(Parameter.Sequence.getName())) {
			if ((parameterValue != null) && (!parameterValue.equals(""))) {
				if (!parameterValue.equals(context.sequenceName)) {
					context.isNewSession = true;
					context.sequenceName = parameterValue;
					Engine.logContext.debug("The sequence is overridden to \"" + context.sequenceName + "\".");
				}
			}
		}
		// This is the overridden connector
		else if (parameterName.equals(Parameter.Connector.getName())) {
			if ((parameterValue != null) && (!parameterValue.equals(""))) {
				context.connectorName = parameterValue;
				Engine.logContext.debug("The connector is overridden to \"" + context.connectorName + "\".");
			}
		}
		// This is the overridden transaction
		else if (parameterName.equals(Parameter.Transaction.getName())) {
			if ((parameterValue != null) && (!parameterValue.equals(""))) {
				context.transactionName = parameterValue;
				Engine.logContext.debug("The transaction is overridden to \"" + context.transactionName + "\".");
			}
		}
		// This is the overridden service code
		else if (parameterName.equals(Parameter.CariocaService.getName())) {
			if ((context.tasServiceCode == null) || (!context.tasServiceCode.equalsIgnoreCase(parameterValue))) {
		        Engine.logContext.debug("Service code differs from previous one; requiring new session");
				context.isNewSession = true;
				context.tasServiceCode = parameterValue;
				Engine.logContext.debug("The service code is overidden to \"" + parameterValue + "\".");
			}
		}
		// This is the portal authentication token
		else if (parameterName.equals(Parameter.SecurityToken.getName())) {
			if ((parameterValue != null) && (parameterValue.length() > 0)) {
				SecurityToken securityToken = Engine.theApp.securityTokenManager.consumeToken(parameterValue);
				Engine.logContext.debug("The security token is \"" + securityToken + "\".");
				
				// Update the context with the security token information
//				context.portalUserName = securityToken.userID;
				if (context.httpSession != null) {
//					context.httpSession.setAttribute("authenticatedUser", context.portalUserName);
					context.httpSession.setAttribute("authenticatedUser", securityToken.userID);
					Engine.logContext.debug("Authenticated user added in the HTTP session");
				}
				
				if (!securityToken.data.isEmpty()) {
					for (String key : securityToken.data.keySet()) {
						String value = securityToken.data.get(key);
						context.set(key, value);
						Engine.logContext.debug("Added security data in the context: " + key + "=" + value);
					}
				}
			}
		}
		// This is the key given by a Carioca request
		else if (parameterName.equals(Parameter.CariocaSesskey.getName())) {
			if ((parameterValue != null) && (parameterValue.length() > 0)) {
				context.tasSessionKey = parameterValue;
				Engine.logContext.debug("The Carioca key is \"" + parameterValue + "\".");
				
				// given key must be verified
				context.tasSessionKeyVerified = false;
			}
		}
		// Carioca trusted request
		else if (parameterName.equals(Parameter.Carioca.getName())) {
			if ((parameterValue != null) && (parameterValue.length() > 0)) {
				context.isTrustedRequest = (parameterValue.equalsIgnoreCase("true") ? true : false);
				Engine.logContext.debug("Is Carioca trusted request: " + parameterValue);
			}
		}
		// This is the Carioca user name
		else if (parameterName.equals(Parameter.CariocaUser.getName())) {
			if ((parameterValue != null) && (parameterValue.length() > 0)) {
				context.tasUserName = parameterValue;
				Engine.logContext.debug("The Carioca user name is \"" + parameterValue + "\".");
			}
		}
		// This is the Carioca user password
		else if (parameterName.equals(Parameter.CariocaPassword.getName())) {
			if ((parameterValue != null) && (parameterValue.length() > 0)) {
				context.tasUserPassword = parameterValue;
				Engine.logContext.debug("The Carioca user password is \"" + parameterValue + "\".");
			}
		}
		// VIC trusted request
		else if (parameterName.equals(Parameter.Vic.getName())) {
			if ((parameterValue != null) && (parameterValue.length() > 0)) {
				context.isTrustedRequest = (parameterValue.equalsIgnoreCase("true") ? true : false);
				Engine.logContext.debug("Is VIC trusted request: " + parameterValue);
			}
		}
		// This is the VIC user name
		else if (parameterName.equals(Parameter.VicUser.getName())) {
			if ((parameterValue != null) && (parameterValue.length() > 0)) {
				context.tasUserName = parameterValue;
				Engine.logContext.debug("The VIC user name is \"" + parameterValue + "\".");
				context.isRequestFromVic = true;
			}
		}
		// This is the VIC group
		else if (parameterName.equals(Parameter.VicGroup.getName())) {
			if ((parameterValue != null) && (parameterValue.length() > 0)) {
				int index = parameterValue.indexOf('@');
				if (index == -1) {
					context.tasUserGroup = parameterValue;
					context.tasVirtualServerName = "";
				}
				else {
					context.tasUserGroup = parameterValue.substring(0, index);
					context.tasVirtualServerName = parameterValue.substring(index + 1);
				}
				Engine.logContext.debug("The VIC group is \"" + context.tasUserGroup + "\".");
				Engine.logContext.debug("The VIC virtual server is \"" + context.tasVirtualServerName + "\".");
			}
			context.isRequestFromVic = true;
		}
		// This is the VIC service code
		else if (parameterName.equals(Parameter.VicServiceCode.getName())) {
			if ((parameterValue != null) && (parameterValue.length() > 0)) {
		        Engine.logContext.debug("Vic service code differs from previous one; requiring new session");
				context.isNewSession = true;
				context.tasServiceCode = parameterValue;
				Engine.logContext.debug("The VIC service code is \"" + parameterValue + "\".");
			}
			context.isRequestFromVic = true;
		}
		// This is the VIC dte address
		else if (parameterName.equals(Parameter.VicDteAddress.getName())) {
			if ((parameterValue != null) && (parameterValue.length() > 0)) {
				context.tasDteAddress = parameterValue;
				Engine.logContext.debug("The VIC dte address is \"" + parameterValue + "\".");
			}
			context.isRequestFromVic = true;
		}
		// This is the VIC comm device
		else if (parameterName.equals(Parameter.VicCommDevice.getName())) {
			if ((parameterValue != null) && (parameterValue.length() > 0)) {
				context.tasCommDevice = parameterValue;
				Engine.logContext.debug("The VIC comm device is \"" + parameterValue + "\".");
			}
			context.isRequestFromVic = true;
		}
		else if (parameterName.startsWith(Parameter.NoCache.getName())) {
			context.noCache = (parameterValue.equalsIgnoreCase("true") ? true : false);
			Engine.logContext.debug("Ignoring cache required: " + parameterValue);
		}
		else if (parameterName.startsWith(Parameter.TransactionMotherSequenceContext.getName())) {
			String motherContextID = parameterValue;
			Context motherContext = Engine.theApp.contextManager.get(motherContextID);
			if (motherContext != null) {
				if(context.httpSession == null)
					context.httpSession = motherContext.httpSession;
				context.set("motherContext", motherContext);
				Engine.logContext.debug("Setting mother sequence context: " + parameterValue);
			}
		}
		else if (parameterName.startsWith(Parameter.SequenceInheritedTransactionContext.getName())) {
			String inheritedContextName = parameterValue;
			if (inheritedContextName != null) {
				if (inheritedContextName.equals(""))
					context.remove("inheritedContext");
				else {
					context.set("inheritedContext", inheritedContextName);
					Engine.logContext.debug("Setting inherited transaction context: " + parameterValue);
				}
			}
		}
		// This is the overidden transaction
		else if (parameterName.equals(Parameter.Lang.getName())) {
			if (parameterValue != null) {
				context.lang = parameterValue;
				Engine.logContext.debug("The translation language requested is \"" + context.lang + "\".");
			}
		}
	}

	@Override
	public void makeInputDocument() throws Exception {
		context.inputDocument = createDOM("ISO-8859-1");
    	getTranslator().buildInputDocument(context, inputData);
	}
	
    protected Object coreProcessRequest() throws Exception {
		Object result;
		
		Document document = null;
        
        try {
        	// fire event for plugins
        	Engine.theApp.pluginsManager.fireRequesterCoreProcessRequestStart(context, inputData);
        	
            makeInputDocument();
            if (context.transactionName != null) Engine.logContext.info("Starting transaction");
            if (context.sequenceName != null) Engine.logContext.info("Starting sequence");
            preGetDocument();
            document = getDocument();
//            if (!document.getDocumentElement().hasChildNodes()) {
//            	Engine.log2_warning("The returned document does not contain any data");
//            }
            result = postGetDocument(document);
		}
		catch(Throwable e) {
			if (!(e instanceof EngineException)) {
				Engine.logContext.error("An unexpected error has occured while trying to execute the transaction.", e);
			}
			
			try {
				document = Engine.theApp.getErrorDocument(e, this, context);
				context.isErrorDocument = true;

				if (context.isXsltRequest) {
					// Find error.xsl (custom or common)
		            String errorXsl;
		            File file;
		            String localeExtension = "";
		            if (Locale.getDefault().toString().startsWith("fr")) localeExtension = "_fr";
		    		
		        	errorXsl = Engine.PROJECTS_PATH + "/" + context.projectName + "/error" + localeExtension +".xsl";
		        	context.sheetUrl = "error" + localeExtension +".xsl";
		        	
		        	file = new File(errorXsl);
		        	if (!file.exists()) {
			        	Engine.logContext.debug("File " + errorXsl + " not found");
		    	    	errorXsl = Engine.PROJECTS_PATH + "/" + context.projectName + "/error.xsl";
			        	context.sheetUrl = "error.xsl";
		    	    	file = new File(errorXsl);
		    	    	if (!file.exists()) {
				        	Engine.logContext.debug("File " + errorXsl + " not found");
		    	    		errorXsl = Engine.XSL_PATH + "/error" + localeExtension +".xsl";
				        	context.sheetUrl = "error" + localeExtension +".xsl";
		    		    	file = new File(errorXsl);

		    		    	if (!file.exists()) {
					        	Engine.logContext.debug("File " + errorXsl + " not found");
		    			    	errorXsl = Engine.XSL_PATH + "/error.xsl";
					        	context.sheetUrl = "error.xsl";

			    		    	file = new File(errorXsl);

			    		    	if (!file.exists()) {
			    		    		Engine.logContext.error("File " + errorXsl + " not found");
			    		    		throw new EngineException("Unable to process the request: no error XSL file has been found!");
			    		    	}
		    		    	}
		    	    	}
		        	}
		        	context.absoluteSheetUrl = errorXsl;
		        	Engine.logContext.debug("XSL error file: " + errorXsl);
				}

				result = postGetDocument(document);
			}
			catch(Exception ee) {
				Engine.logContext.error("An unexpected error has occured while trying to execute the error XSLT.", ee);
				throw ee;
			}
		}
		finally {
        	// fire event for plugins
        	Engine.theApp.pluginsManager.fireRequesterCoreProcessRequestEnd(context, inputData);
		}
        
		return result;
    }
    
    @Override
    public Document getDocument() throws Exception {
		Document document = Engine.theApp.getDocument(this, context);		
		return document;
    }

    protected void findStyleSheet(String browser) throws EngineException {
    	// The sheet url may have been already set by error handling...
    	if (context.absoluteSheetUrl != null) return;
    	
    	// TODO: chercher la feuille de style en fonction du locale du client
        RequestableObject requestedObject = context.requestedObject;
		ISheetContainer lastDetectedObject = context.lastDetectedObject;
        InputSource inputSource = null;
        
        try {
			if ((context.cacheEntry != null) && (context.cacheEntry.sheetUrl != null)) {
				context.sheetUrl = context.cacheEntry.sheetUrl;
				context.absoluteSheetUrl = context.cacheEntry.absoluteSheetUrl;
				context.contentType = context.cacheEntry.contentType;

				if (context.sheetUrl == null) {
					// No sheet has been defined: it may be in a pure xml producer
					// without sheet context...
					return;
				}
				Engine.logContext.debug("Sheet built from the cache");
			}
			else {
				Sheet sheet = null;
				
				int sheetLocation = requestedObject.getSheetLocation();
				if (sheetLocation == Transaction.SHEET_LOCATION_FROM_LAST_DETECTED_OBJECT_OF_REQUESTABLE) {
					Engine.logContext.debug("Sheet location: from last detected screen class");
				}
				else if (sheetLocation == Transaction.SHEET_LOCATION_FROM_REQUESTABLE) {
					Engine.logContext.debug("Sheet location: from transaction");
				}
				else {
					Engine.logContext.debug("Sheet location: none");
					return;
				}

				Engine.logContext.debug("Searching specific sheet for browser '" + browser + "'...");
				switch (sheetLocation) {
					default:
					case Transaction.SHEET_LOCATION_NONE:
						break;
					case Transaction.SHEET_LOCATION_FROM_REQUESTABLE:
						Engine.logContext.debug("Searching in the transaction");
						sheet = requestedObject.getSheet(browser);
						break;
					case Transaction.SHEET_LOCATION_FROM_LAST_DETECTED_OBJECT_OF_REQUESTABLE:
						if (lastDetectedObject != null) {
							Engine.logContext.debug("Searching in the last detected screen class (" + ((DatabaseObject)lastDetectedObject).getQName() + ")");
							sheet = lastDetectedObject.getSheet(browser);
						}
						break;
				}

				if (sheet == null) {
					Engine.logContext.debug("No specific sheet has been found; searching for common sheet...");
					switch (sheetLocation) {
						default:
						case Transaction.SHEET_LOCATION_FROM_REQUESTABLE:
							Engine.logContext.debug("Searching in the transaction");
							sheet = requestedObject.getSheet(Sheet.BROWSER_ALL);
							break;
						case Transaction.SHEET_LOCATION_FROM_LAST_DETECTED_OBJECT_OF_REQUESTABLE:
							if (lastDetectedObject != null) {
								Engine.logContext.debug("Searching in the last detected screen class");
								sheet = lastDetectedObject.getSheet(Sheet.BROWSER_ALL);
							}
							break;
					}
				}

				if (sheet == null) {
					// No sheet has been defined: it may be in a pure xml producer
					// without sheet context...
					return;
				}
				else {
					Engine.logContext.debug("Storing found sheet into the execution context (__currentSheet)");
					context.sheetUrl = sheet.getUrl();
				}

				String projectDirectoryName = context.project.getName();

				Engine.logContext.debug("Using XSL data from \"" + context.sheetUrl + "\"");

				// Search relatively to the Convertigo servlet application base directory
				context.absoluteSheetUrl = Engine.PROJECTS_PATH + "/" + projectDirectoryName + "/" + (context.subPath.length() > 0 ? context.subPath + "/" : "") + context.sheetUrl;
				Engine.logContext.debug("Url: " + context.absoluteSheetUrl);
				File xslFile =  new File(context.absoluteSheetUrl);
				if (!xslFile.exists()) {
					Engine.logContext.debug("The local xsl file (\"" + context.absoluteSheetUrl + "\") does not exist. Trying search in Convertigo XSL directory...");
					if (context.sheetUrl.startsWith("../../xsl/"))
						context.absoluteSheetUrl = Engine.XSL_PATH + "/" + context.sheetUrl.substring(10);
					else
						context.absoluteSheetUrl = Engine.XSL_PATH + "/" + context.sheetUrl;
					Engine.logContext.debug("Url: " + context.absoluteSheetUrl);
					xslFile =  new File(context.absoluteSheetUrl);
					if (!xslFile.exists()) {
						Engine.logContext.debug("The common xsl file (\"" + context.absoluteSheetUrl + "\") does not exist. Trying absolute search...");
						context.absoluteSheetUrl = context.sheetUrl;
						Engine.logContext.debug("Url: " + context.absoluteSheetUrl);
					}
				}

				Engine.logContext.debug("Retrieving content type from the XSL...");

				//inputSource = new InputSource(new FileInputStream(context.absoluteSheetUrl));
				inputSource = new InputSource(new File(context.absoluteSheetUrl).toURI().toASCIIString());
				
				DocumentBuilder documentBuilder = XMLUtils.getDefaultDocumentBuilder();
				documentBuilder.setEntityResolver(XMLUtils.getEntityResolver());
				
				Document document = documentBuilder.parse(inputSource);
				NodeList nodeList = document.getElementsByTagName("xsl:output");
				String contentType = "text/html";
				if (nodeList.getLength() != 0) {
					Element element = (Element) nodeList.item(0);
					contentType = element.getAttribute("media-type");
					if (contentType.length() == 0) {
						Engine.logContext.warn("No media type is specified into the XSL style sheet \"" + context.sheetUrl + "\"; using default (\"text/html\"). You should use <xsl:output media-type=\"...\" ...> directive.");
						contentType = "text/html";
					}
					Engine.logContext.debug("Content-type=" + contentType);
				}
				else {
					Engine.logContext.warn("No media type is specified into the XSL style sheet \"" + context.sheetUrl + "\"; using default (\"text/html\"). You should use <xsl:output media-type=\"...\" ...> directive.");
				}
            
				context.contentType = contentType;
			}

			// Updating the cache entry properties if needed
			if (context.cacheEntry != null) {
				context.cacheEntry.contentType = context.contentType;
				context.cacheEntry.sheetUrl = context.sheetUrl;
				context.cacheEntry.absoluteSheetUrl = context.absoluteSheetUrl;
				
				try {
					Engine.theApp.cacheManager.updateCacheEntry(context.cacheEntry);
					Engine.logContext.debug("Sheet stored into the cache entry; updating its XSL and content type properties...");
				}
				catch(Exception e) {
					Engine.logContext.error("(CacheManager) Unable to update the cache entry!", e);
				}
			}
        }
        catch(Exception e) {
            throw new EngineException("An unexpected error has occured while finding the sheet for the transaction \"" + requestedObject.getName() + "\".", e);
        }
        finally {
            try {
                if (inputSource != null) {
                    InputStream inputStream = inputSource.getByteStream();
                    if (inputStream != null) inputStream.close();
                }
            }
            catch(IOException e) {
                throw new EngineException("Unable to close the sheet file for the transaction \"" + requestedObject.getName() + "\".", e);
            }
        }
    }

    public abstract void setStyleSheet(Document document);

    protected Object performXSLT(Document document) throws Exception {
		String t1 = context.statistics.start(EngineStatistics.XSLT);

        try {
            Engine.logContext.debug("XSLT process is beginning");
            
        	String result = "";
        	
            Engine.logContext.debug("Sheet absolute URL: " + context.absoluteSheetUrl);
    		if (context.absoluteSheetUrl == null) throw new EngineException("You have required an XSLT process, but Convertigo has been unable to find a stylesheet for your request. Verify your project's settings.");

            String xsltEngine = EnginePropertiesManager.getProperty(EnginePropertiesManager.PropertyName.DOCUMENT_XSLT_ENGINE);
            Engine.logContext.debug("Required XSLT engine: " + xsltEngine);
            
            // Xalan XSLT engine
            if (xsltEngine.startsWith("xalan")) {
            	
                //StreamSource streamSource = null;
                InputSource inputSource = null;
                TransformerFactory tFactory = null;
                try {
                    // XSLT
                    if (xsltEngine.equals("xalan/xslt")) {
                        tFactory = new org.apache.xalan.processor.TransformerFactoryImpl();
                        Engine.logContext.debug("XSLT engine class: org.apache.xalan.processor.TransformerFactoryImpl");

                        //streamSource = new StreamSource(new File(context.absoluteSheetUrl).toURI().toASCIIString());
                        inputSource = new InputSource(new File(context.absoluteSheetUrl).toURI().toASCIIString());
                    }
                    // XSLTC
                    else if (xsltEngine.equals("xalan/xsltc") || xsltEngine.equals("xalan")) {
                    	tFactory = new org.apache.xalan.xsltc.trax.TransformerFactoryImpl();
                        Engine.logContext.debug("XSLT engine class: org.apache.xalan.xsltc.trax.TransformerFactoryImpl");

                    	String transletName = computeTransletName(context.absoluteSheetUrl);
                        Engine.logContext.debug("Translet name: " + transletName);

                        tFactory.setAttribute("translet-name", transletName);
                        tFactory.setAttribute("debug", (Engine.logContext.isDebugEnabled() ? Boolean.TRUE : Boolean.FALSE));
                        tFactory.setAttribute("generate-translet", Boolean.TRUE);
                        tFactory.setAttribute("auto-translet", Boolean.TRUE);
                        tFactory.setAttribute("enable-inlining", Boolean.FALSE);

                        //String transletOutput = (context.project == null ? Engine.PROJECTS_PATH : Engine.PROJECTS_PATH + "/" + context.projectName) + "/_private/xsltc/";
                        //transletOutput = new File(transletOutput).toURI().toASCIIString();
                        String transletOutput = (context.project == null ? Engine.PROJECTS_PATH : Engine.PROJECTS_PATH + "/" + context.projectName) + "/_private/xsltc";
                        transletOutput = new File(transletOutput).getCanonicalPath();
                        tFactory.setAttribute("destination-directory", transletOutput);
                        Engine.logContext.debug("Translet output: " + transletOutput);

                        //streamSource = new StreamSource(new File(context.absoluteSheetUrl).toURI().toASCIIString());
                        inputSource = new InputSource(new File(context.absoluteSheetUrl).toURI().toASCIIString());
                    }
                    else {
                        throw new EngineException("Unknown XSLT engine (" + xsltEngine + "), please check your Convertigo engine properties!");
                    }
                    
                    CatalogResolver cr = XMLUtils.getCatalogResolver();
                    
                    tFactory.setURIResolver(cr); // set URI resolver (for xsl include or import)
                    
                    SAXParserFactory spf = SAXParserFactory.newInstance();
                    SAXParser sp = spf.newSAXParser();
                    XMLReader xmlr = sp.getXMLReader();
                    xmlr.setEntityResolver(cr);	// set Entity resolver (for dtd)
                    
                    SAXSource ss = new SAXSource(xmlr, inputSource);
                    
                    //Transformer transformer = tFactory.newTransformer(streamSource);
                    Transformer transformer = tFactory.newTransformer(ss);
                    
                    StringWriter sw = new StringWriter();
                    Element element = document.getDocumentElement();                    
                    
                    /** Don't use DomSource because it makes a strange output, but work fine with StreamSource */
                    Engine.logContext.trace("Start to transform document to byteArray ...");
                    Transformer docToByte = new org.apache.xalan.processor.TransformerFactoryImpl().newTransformer();
                    ByteArrayOutputStream byteout = new ByteArrayOutputStream();
                    StreamResult stream = new StreamResult(byteout);
                    docToByte.transform(new DOMSource(element), stream);
                    ByteArrayInputStream byteInput = new ByteArrayInputStream(byteout.toByteArray());
                    Engine.logContext.trace("...finish to transform document to byteArray");
                    
        			transformer.transform(new StreamSource(byteInput), new StreamResult(sw));

                    result = sw.getBuffer().toString();
                    
                }
                catch (Exception e) {
                	throw e;
                }
                finally {
                    //if (streamSource != null) {
                    //    InputStream inputStream = streamSource.getInputStream();
                    //    if (inputStream != null) inputStream.close();
                    //}
                    if (inputSource != null) {
                        InputStream inputStream = inputSource.getByteStream();
                        if (inputStream != null) inputStream.close();
                    }
                }
            }
            // MSXML XSLT engine
            else if (xsltEngine.equals("msxml")) {
                com.ms.xml.Document xslDocument = new com.ms.xml.Document();
                com.ms.xml.Document msxmlDocument = ((com.ms.xml.Document) document);
                Engine.logContext.debug("XSLT engine class: com.ms.xml.Document");
                xslDocument.setAsync(false);
                if (!xslDocument.load(context.absoluteSheetUrl.replace('/', '\\'))) {
                	ParseError error = xslDocument.getParseError();
                	throw new EngineException("Unable to parse the XSL file \"" + context.absoluteSheetUrl + "\".\nThe following error #" + error.getErrorCode() + " occurs at line " + error.getLine() + ", column " + error.getLinepos() + ": " + error.getReason() + ".\nLine: " + error.getSrcText());
                }
                result = msxmlDocument.transformNode(xslDocument);
            }
            else {
                throw new EngineException("Unknown XSLT engine (" + xsltEngine + "), please check your Convertigo engine properties!");
            }
            
            Engine.logContext.trace("XSLT result:\n" + result);
            
            return result;
        }
        finally {
            Engine.logContext.debug("XSLT process has finished");
			context.statistics.stop(t1);
        }
    }

	public static String computeTransletName(String originalName) {
		int len = originalName.length() - 1;
		long computedNumber = (originalName.charAt(0) * originalName.charAt(len)) << 16;
		for (int i = 1 ; i < len ; i++) {
			computedNumber += i * originalName.charAt(i) * originalName.charAt(i+1);
		}
		return "_" + Long.toHexString(computedNumber) + Integer.toHexString(originalName.hashCode());
	}
}