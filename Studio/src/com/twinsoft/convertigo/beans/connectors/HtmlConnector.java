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

package com.twinsoft.convertigo.beans.connectors;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URIException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IScreenClassContainer;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.screenclasses.HtmlScreenClass;
import com.twinsoft.convertigo.beans.statements.HTTPStatement;
import com.twinsoft.convertigo.beans.statements.HTTPUploadStatement;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.KeyExpiredException;
import com.twinsoft.convertigo.engine.MaxCvsExceededException;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.helpers.ScreenClassHelper;
import com.twinsoft.convertigo.engine.parsers.HtmlParser;
import com.twinsoft.convertigo.engine.parsers.events.IEvent;
import com.twinsoft.convertigo.engine.parsers.triggers.AbstractTrigger;
import com.twinsoft.convertigo.engine.parsers.triggers.TriggerTimeoutException;
import com.twinsoft.convertigo.engine.util.URLUtils;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class HtmlConnector extends HttpConnector implements IScreenClassContainer<HtmlScreenClass> {

	public static final String ParseMode_4_0 = "4.0";
	public static final String ParseMode_4_5 = "4.5";
	
	private static final long serialVersionUID = -9124803554595732247L;

	private boolean ignoreEmptyAttributes = false; 
	private String parseMode = ParseMode_4_5;

	transient private HtmlParser htmlParser = null;
	transient private Document currentXmlDocument = null;
	
	transient private ScreenClassHelper<HtmlScreenClass> screenClassHelper = new ScreenClassHelper<HtmlScreenClass>(this);
	
	public HtmlConnector() {
		super();
	}
    
	@Override
	public HtmlConnector clone() throws CloneNotSupportedException {
		HtmlConnector htmlConnector = (HtmlConnector) super.clone();
		htmlConnector.screenClassHelper = new ScreenClassHelper<HtmlScreenClass>(htmlConnector);
		htmlConnector.currentXmlDocument = currentXmlDocument;
		return htmlConnector;
	}
	
	@Override
	protected void finalize() throws Throwable {
		setCurrentXmlDocument(null);
		
		removeHttpStateListener(htmlParser);
		setHtmlParser(null);
		
		super.finalize();
	}

	@Override
	public byte[] getData(Context context) throws IOException, ConnectionException, EngineException {
		return super.getData(context);
	}

	@Override
	protected int doExecuteMethod(final HttpMethod method, Context context) throws ConnectionException, URIException, MalformedURLException {
		try {
			HtmlTransaction htmlTransaction = getCurrentHtmlTransaction(context);
			if (htmlTransaction.currentStatement instanceof HTTPUploadStatement) {
				((HTTPUploadStatement) htmlTransaction.currentStatement).handleUpload(method, context);
			}
		} catch (EngineException e) {
			Engine.logBeans.error("(HtmlConnector) unexpected error", e);
		}
		return super.doExecuteMethod(method, context);
	}
	
	public HtmlParser getHtmlParser() {
		return htmlParser;
	}
	
	public void setHtmlParser(HtmlParser htmlParser) {
		this.htmlParser = htmlParser;
	}
	
	public Cookie[] getCookies() {
		Cookie[] cookies = new Cookie[]{};
		if ((httpState != null) && handleCookie) {
			cookies = httpState.getCookies();
		}
		return cookies;
	}

	public Document parseData(byte[] httpData, String uri, String charset, AbstractTrigger trigger) throws TriggerTimeoutException, MaxCvsExceededException, KeyExpiredException {
		return getHtmlParser().parse(httpData, context, uri, charset, trigger);
	}
	
	public boolean dispatchEvent(IEvent evt, Context context, AbstractTrigger trigger) throws TriggerTimeoutException, MaxCvsExceededException, KeyExpiredException {
		return getHtmlParser().dispatchEvent(evt, context, trigger);
	}
	
	/**
	 * @return Returns the currentXmlDocument.
	 */
	public Document getCurrentXmlDocument() {
		return currentXmlDocument;
	}

	/**
	 * @param currentXmlDocument The currentXmlDocument to set.
	 */
	public synchronized void setCurrentXmlDocument(Document currentXmlDocument) {
		this.currentXmlDocument = currentXmlDocument;
	}

	public HtmlScreenClass getDefaultScreenClass() {
		return screenClassHelper.getDefaultScreenClass();
	}
    
	public void setDefaultScreenClass(ScreenClass defaultScreenClass) throws EngineException {
		screenClassHelper.setDefaultScreenClass(defaultScreenClass);
	}
    
	public List<HtmlScreenClass> getAllScreenClasses() {
		return sort(screenClassHelper.getAllScreenClasses());
	}
	
	public HtmlScreenClass getScreenClassByName(String screenClassName) {
		return screenClassHelper.getScreenClassByName(screenClassName);
	}
	
	@Override
	public void add(DatabaseObject databaseObject) throws EngineException {
		if (!screenClassHelper.add(databaseObject)) {
			super.add(databaseObject);
		}
	}

	
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.Connector#addTransaction(com.twinsoft.convertigo.beans.core.Transaction)
	 */
	@Override
	protected void addTransaction(Transaction transaction) throws EngineException {
		if (!(transaction instanceof HtmlTransaction))
			throw new EngineException("You cannot add to an HTML connector a database object of type " + transaction.getClass().getName());
		super.addTransaction(transaction);
	}

	public void checkForStateless() {
		boolean trStateLess = false;
		boolean trStateFull = false;
		for (Transaction transaction : getTransactionsList()) {
			HtmlTransaction htmlTransaction = (HtmlTransaction) transaction;
			if (htmlTransaction.isStateFull())
				trStateFull = true;
			else
				trStateLess = true;
		}
		
		if (trStateFull && !trStateLess)
			Engine.logBeans.warn("(HtmlConnector) Connector '"+ getName() + "' must define at least one stateless transaction!");
	}
	
	/**
	 * Returns the current screen class.
	 * 
	 * @return the current screen class.
	 * 
	 * @EngineException exception if any error occurs.
	 */
	public final HtmlScreenClass getCurrentScreenClass() {
		return screenClassHelper.getCurrentScreenClass();
	}
	
	@Override
	protected String getUserAgent(Context context) throws ConnectionException {
		try {
			return getHtmlParser().getUserAgent(context);
		} catch (MaxCvsExceededException e) {
			throw new ConnectionException("HtmlConnector : could not retrieve user agent from Mozilla!",e);
		} catch (KeyExpiredException e) {
			throw new ConnectionException("HtmlConnector : could not retrieve user agent from Mozilla!",e);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.connectors.HttpConnector#prepareForTransaction(com.twinsoft.convertigo.engine.Context)
	 */
	@Override
	public void prepareForTransaction(Context context) throws EngineException {
		Engine.logBeans.trace("(HtmlConnector) Retrieving or Initializing HtmlParser");
		
		// Engine mode : retrieve HtmlParser from context
		if (Engine.isEngineMode()) {
			if(this.htmlParser==null){
				Engine.logBeans.trace("(HtmlConnector) Creating new HtmlParser for context id "+ context.contextID);
				HtmlParser htmlParser = new HtmlParser(null);
				this.addHttpStateListener(htmlParser);
				setHtmlParser(htmlParser);
			} else Engine.logBeans.trace("(HtmlConnector) Using HtmlParser of HTML Connector for context id "+ context.contextID);
		}
		// Studio mode
		else {
			if(this.htmlParser==null){
				throw new EngineException("Studio mode: the HTML connector must be open in order to execute transactions");
			}
			Engine.logBeans.trace("(HtmlConnector) Using HtmlParser of Studio for context id "+ context.contextID);
		}

		if(context.requestedObject!=null && context.requestedObject instanceof HtmlTransaction){
			HtmlTransaction htmlTransaction = (HtmlTransaction)context.requestedObject;
			if(!htmlTransaction.isStateFull())
				getHtmlParser().resetBrowserProperty(context);
		}

		super.prepareForTransaction(context);
	}

	public void prepareForHTTPStatement(Context context) throws EngineException {
		Engine.logBeans.debug("(HtmlConnector) Preparing for http statement");

		// Retrieve current executing transaction
		HtmlTransaction htmlTransaction = getCurrentHtmlTransaction(context);
		
		if ((htmlTransaction == null) || (!htmlTransaction.runningThread.bContinue)) {
			return;
		}
		
		// Retrieve current statement : the statement being executed
		Statement statement = htmlTransaction.currentStatement;
		if (statement == null) {
			return;
		}
		
		if (!(statement instanceof HTTPStatement)) {
			return;
		}
		
		HTTPStatement httpStatement = (HTTPStatement)statement;
		
		handleCookie = httpStatement.isHandleCookie();

		httpParameters = httpStatement.getHttpParameters();

		sUrl = httpStatement.getUrl(isHttps(),getServer(),getPort());
		Engine.logBeans.debug("(HtmlConnector) URL: " + sUrl);
		
		// Parse input document for HTTPStatement variables
		httpStatement.parseInputDocument(context);
		
		// Getting all input variables marked as GET
		Engine.logBeans.trace("(HtmlConnector) Loading all GET input variables");
		String queryString = httpStatement.getQueryString(context);
		if (Engine.logBeans.isDebugEnabled())
			Engine.logBeans.debug("(HtmlConnector) GET query: " + Visibility.Logs.replaceVariables(httpStatement.getVariables(), queryString));
		
		// Encodes URL if it contains special characters
		sUrl = URLUtils.encodeAbsoluteURL(sUrl, htmlTransaction.getComputedUrlEncodingCharset());
		if (queryString.length() != 0) {
			sUrl += (sUrl.indexOf('?') == -1 ? "?" : "&") + queryString;
		}

		// Posting all input variables marked as POST
		Engine.logBeans.trace("(HtmlConnector) Loading all POST input variables");
		postQuery = httpStatement.getPostQuery(context);
		if (Engine.logBeans.isDebugEnabled()) {
			Engine.logBeans.debug("(HtmlConnector) POST query: " + Visibility.Logs.replaceVariables(httpStatement.getVariables(), postQuery));
		}
		
		// Setup the SSL properties if needed
		if (isHttps() || httpStatement.isHttps()) {
			Engine.logBeans.debug("(HtmlConnector) Setting up SSL properties");
			certificateManager.collectStoreInformation(context);
		}

		Engine.logBeans.debug("(HtmlConnector) Connector successfully prepared for statement");
	}
	
	public boolean getIgnoreEmptyAttributes() {
		return ignoreEmptyAttributes;
	}

	public void setIgnoreEmptyAttributes(boolean ignoreEmptyAttributes) {
		this.ignoreEmptyAttributes = ignoreEmptyAttributes;
	}
	
	@Override
	public void configure(Element element) throws Exception {
		super.configure(element);
		
        String version = element.getAttribute("version");
        
        if (version == null) {
            String s = XMLUtils.prettyPrintDOM(element);
            EngineException ee = new EngineException(
                "Unable to find version number for the database object \"" + getName() + "\".\n" +
                "XML data: " + s
            );
            throw ee;
        }
        
        if (VersionUtils.compare(version, "4.4.2") < 0) {
        	ignoreEmptyAttributes = true; // view #230        	
			hasChanged = true;
			Engine.logBeans.warn("[HttpStatement] The object \"" + getName()+ "\" has been updated to version 4.4.2");
        }
	}
	
	@Override
	public void release(){
		if(htmlParser!=null){
			htmlParser.release();
			htmlParser = null;
		}
	}

	public boolean isParseMode(String parseMode) {
		return this.parseMode.equals(parseMode);
	}
	
	@Override
	public String[] getTagsForProperty(String propertyName) {
		if("parseMode".equals(propertyName)){
			return new String[] {
					HtmlConnector.ParseMode_4_0,
					HtmlConnector.ParseMode_4_5
			};
		}
		return super.getTagsForProperty(propertyName);
	}

	public String getParseMode() {
		return parseMode;
	}

	public void setParseMode(String parseMode) {
		if(Arrays.asList(getTagsForProperty("parseMode")).contains(parseMode))
			this.parseMode = parseMode;
	}
	
	@Override
	public List<DatabaseObject> getAllChildren() {	
		List<DatabaseObject> rep = super.getAllChildren();
		rep.add(0, getDefaultScreenClass());
		return rep;
	}
	
	@Override
	public HtmlTransaction newTransaction() {
		return new HtmlTransaction();
	}
	
	public HtmlScreenClass newScreenClass() {
		return new HtmlScreenClass();
	}
	
	protected HtmlTransaction getCurrentHtmlTransaction(Context context) throws EngineException {
		// Retrieve current executing transaction
		HtmlTransaction htmlTransaction = null;
		try {
			htmlTransaction = (HtmlTransaction) context.requestedObject;
		}
		catch (ClassCastException e) {
			throw new EngineException("Requested object is not a transaction",e);
		}
		return htmlTransaction;
	}
}
