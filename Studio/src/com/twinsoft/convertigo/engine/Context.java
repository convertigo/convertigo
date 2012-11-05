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

package com.twinsoft.convertigo.engine;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpState;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.core.Block;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.ISheetContainer;
import com.twinsoft.convertigo.beans.core.Pool;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.transactions.HttpTransaction;
import com.twinsoft.convertigo.engine.cache.CacheEntry;
import com.twinsoft.convertigo.engine.parsers.HtmlParser;
import com.twinsoft.convertigo.engine.parsers.XulRecorder;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;
import com.twinsoft.convertigo.engine.util.Crypto2;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.TwsCachedXPathAPI;
import com.twinsoft.convertigo.engine.util.URLrewriter;
import com.twinsoft.twinj.Javelin;

public class Context extends AbstractContext {

	public LogParameters logParameters = new LogParameters();
	
	public String name;
	
	public CacheEntry cacheEntry;
	public boolean noCache = false;
	
	public boolean isDestroying = false;
	public boolean isErrorDocument = false;

	public boolean isXsltRequest = false;
	
	public boolean isAsync;
	public int waitingRequests = 0;
	
	public boolean isNewSession;
	public boolean isRequestFromVic = false;
	public boolean isTrustedRequest = false;
	
	public long documentSignatureSent = 0;
	public long documentSignatureReceived = 0;
	
	public final EngineStatistics statistics = new EngineStatistics();

	public Map<String, Block> previousFields = new HashMap<String, Block>();

	public String absoluteSheetUrl;
	public String sheetUrl;
	public String contentType;
	public String cacheControl;

	public Project project;
	public Connector connector;
	public Pool pool;
	public int poolContextNumber;
	public String projectName;
	public String sequenceName;
	public String transactionName;
	public String connectorName;
	public RequestableObject requestedObject;
	public ISheetContainer lastDetectedObject;
	
	// compatibility with older versions
	public ScreenClass lastDetectedScreenClass = null;
	public Transaction transaction = null;//
	
	public String subPath = "";
	
	public HtmlParser htmlParser = null;
	public HttpState httpState = null; 
	private Header[] responseHeaders = new Header[]{};
	private TwsCachedXPathAPI xpathApi = null;
	
	public boolean tasSessionKeyVerified = false;
	
	public IdToXpathManager idToXpathManager = null;
	public URLrewriter urlRewriter = null;
	
	private Map<String, Connector> used_connectors = new HashMap<String, Connector>();
	private Set<Connector> opened_connectors = new HashSet<Connector>();

	private boolean requireRemoval = false;
	
	private Map<String, List<String>> requestHeaders = null;
	
	private Scriptable sharedScope = null;
	
	private XulRecorder xulRecorder = null;
	
	private List<File> temporaryFiles = null;
	
	public Context(String contextID) {
		this.contextID = contextID;
		
//		boolean bContextsWriteLogs = (Engine.getProperty(Engine.ConfigurationProperties.CONTEXTS_WRITE_LOGS).equalsIgnoreCase("true"));
//		if (Engine.isStudioMode()) {
//			log.setOutputStream(Engine.objectsProvider.getContextOutputStream());
//			Engine.logContext.debug("Context log output stream: studio");
//		}
//		else if (bContextsWriteLogs) {
//			try {
//				String contextLogOutputStream = Engine.LOG_DIRECTORY + "/" + getLogFilename();
//				Engine.logContext.debug("Context log output stream: " + contextLogOutputStream);
//				log.setOutputStream(new FileOutputStream(contextLogOutputStream, true));
//			}
//			catch(Exception e) {
//				Engine.log.exception(e, "Unable to set the output stream for context log");
//			}
//		}
	}
	
	public void reset() {
		isXsltRequest = false;
		isErrorDocument = false;
		isNewSession = false;
		
		isCacheEnabled = true;
		cacheEntry = null;
		noCache = false;
		
		sheetUrl = null;
		absoluteSheetUrl = null;
        contentType = null;
        cacheControl = null;
        
        connectorName = null;
        
        sequenceName = null;
        transactionName = null;

        requestedObject = null;
		lastDetectedObject = null;

		// For compatibility with older javelin projects
		lastDetectedScreenClass = null;
		transaction = null;
		
		if (steps != null)
			steps.clear();
		
		Engine.logContext.debug("Context reset");
	}
	
	public String getProjectDirectory() {
		String dir = null;
		if (projectName != null) {
			dir = Engine.PROJECTS_PATH + "/" + projectName;
		}
		return dir;
	}

	public String getProjectName() {
		String sName = null;
		if (projectName != null) {
			sName = projectName;
		}
		return sName;
	}
	
	private static String webinfPath = null;
	
	private static String getWebInfPath() {
		if (webinfPath == null) {
        	URL engineProps = Engine.class.getResource(EnginePropertiesManager.PROPERTIES_FILE_NAME);
        	String path = engineProps.getPath();
        	path = path.replaceAll("%20", " ");
        	int start = Engine.isLinux()?0:1;
        	int end = path.lastIndexOf(EnginePropertiesManager.PROPERTIES_FILE_NAME);
        	webinfPath = path.substring(start, end+1);
        	Engine.logContext.debug("Convertigo bin path : "+ webinfPath);
		}
		return webinfPath;
	}
	
	/**
	 * Loads a Properties object from a file.
	 * 
	 */
	public Properties loadPropertiesFromWebInf(String fileName) {
		if ((fileName != null) && (fileName.length() != 0)) {
			String path = getWebInfPath() + fileName;
			return loadProperties(path);
		}
		return null;
	}
	
	/**
	 * Loads a Properties object from a file.
	 * 
	 */
	public Properties loadPropertiesFromProject(String fileName) {
		if ((fileName != null) && (fileName.length() != 0)) {
			String path = getProjectDirectory() + "/" + fileName;
			return loadProperties(path);
		}
		return null;
	}
	
	private Properties loadProperties(String path) {
		File file = new File(path);
		// Loads properties from file
		if (file.exists()) {
			try {
    			FileInputStream fin = new FileInputStream(file);
	            Properties properties = new Properties();
	            properties.load(fin);
	            return properties;
			} catch (FileNotFoundException e) {
				Engine.logContext.warn("Problems occured while loading properties : file  '"+ path +"' not found!");
			} catch (IOException e) {
				Engine.logContext.error("Problems occured while loading properties from  file  '"+ path +"'", e);
			}
		}
		return null;
	}
	
	/**
	 * Store a Properties object to a file.
	 */
	public boolean savePropertiesToWebInf(String fileName, Properties properties) {
		if ((fileName != null) && (properties != null) && (fileName.length() != 0)) {
			String path = getWebInfPath() + fileName;
    		return saveProperties(path, properties);
		}	
		return false;
	}
	
	/**
	 * Store a Properties object to a file.
	 */
	public boolean savePropertiesToProject(String fileName, Properties properties) {
		if ((fileName != null) && (properties != null) && (fileName.length() != 0)) {
			String path = getProjectDirectory() + "/" + fileName;
    		return saveProperties(path, properties);
		}	
		return false;
	}
	
	private boolean saveProperties(String path, Properties properties) {
		File file = new File(path);
		// Creates file if needed
		if (!file.exists()) {
			try {
				if (file.createNewFile()) {
					Engine.logContext.warn("File '"+ path +"' has been created");
				}
				else {
					Engine.logContext.warn("Problems occured while creating file  '"+ path +"'");
				}
			} catch (Exception e) {
				Engine.logContext.error("Problems occured while creating file  '"+ path +"'", e);
			} 
		}
		// Store properties to file
		if (file.exists()) {
			try {
				FileOutputStream fos = new FileOutputStream(file);
				properties.store(fos, "");
				fos.flush();
				fos.close();
				return true;
			} catch (FileNotFoundException e) {
				Engine.logContext.warn("Problems occured while saving properties '"+ name +"': file  '"+ path +"' not found!");
			} catch (IOException e) {
				Engine.logContext.error("Problems occured while saving properties '"+ name +"' to  file  '"+ path +"'", e);
			}
		}
		return false;
	}
	
	public Header[] getResponseHeaders() {
		return responseHeaders;
	}

	public void setResponseHeaders(Header[] responseHeaders) {
		this.responseHeaders = responseHeaders;
	}
	
	public Vector<String> getCookieStrings() {
		// Use the HandleCookie Property of the transaction to return or not the cookies.
		//
		// We noticed a Bug in tomcat when too much cookies where set in the response to the client. This causes a 
		// IndexOutOfBoundException:  4096 in coyote.
		// To overcome this situation, now you can configure HandleCookies to false in the transaction to prevent cookies to be reflected
		// to the client.
		if (requestedObject instanceof HttpTransaction ) {
			if (!((HttpTransaction)requestedObject).isHandleCookie())
				return new Vector<String>();
		}
		
		Vector<String> cookies = new Vector<String>();
		if (httpState != null) {
			Cookie[] httpCookies = httpState.getCookies();
			int len = httpCookies.length;
			Cookie cookie = null;
			String sCookie;
			
			DateFormat df = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss z", Locale.US);
			df.setTimeZone(TimeZone.getTimeZone("GMT"));
			for (int i=0; i<len; i++) {
				cookie = httpCookies[i];
				sCookie = 	cookie.getName() + "=" + cookie.getValue() + ";";
				sCookie +=	(cookie.getExpiryDate() != null) ? "expires=" + df.format(cookie.getExpiryDate())+ ";":"";
				sCookie +=	"path=" + cookie.getPath() + ";";
				sCookie +=	"domain=" + cookie.getDomain() + ";";
				sCookie +=	cookie.getSecure() ? "secure": "";
				cookies.addElement(sCookie);
			}
		}
		return cookies;
	}
	
	public String getSequenceTransactionSessionId() {
		if (requestedObject instanceof Sequence) {
			return ((Sequence)requestedObject).getTransactionSessionId();
		}
		return null;
	}
	
	public Node addTextNodeUnderRoot(String tagName, String text) {
		return addTextNode(outputDocument.getDocumentElement(), tagName, text);
	}
	
	public Node addTextNodeUnderBlocks(String tagName, String text) {
		return addTextNode(outputDocument.getDocumentElement().getElementsByTagName("blocks").item(0), tagName, text);
	}
	
	public Node addTextNode(Node parentNode, String tagName, String text) {
		Element newElement = outputDocument.createElement(tagName);
		if(text!=null){
			Text textElement = outputDocument.createTextNode(text);
			newElement.appendChild(textElement);
		}
		parentNode.appendChild(newElement);
		return newElement;
	}
	
	public boolean waitNextPage(String action, int timeout, int hardDelay) throws EngineException {
		return ((JavelinConnector) connector).waitNextPage(this, action, timeout, hardDelay);
	}
	
	public boolean waitNextPage(Javelin javelin, String action, int timeout, int hardDelay) throws EngineException {
		return waitNextPage(action, timeout, hardDelay);
	}
	
	public boolean waitAtScreenClass(int timeout, int hardDelay) throws EngineException {
		return ((JavelinConnector) connector).waitAtScreenClass(this, timeout, hardDelay);
	}
    
	public boolean waitAtScreenClass(Javelin javelin, int timeout, int hardDelay) throws EngineException {
		return waitAtScreenClass(timeout, hardDelay);
	}
	
	public IdToXpathManager getIdToXpathManager(){
		if(idToXpathManager==null)idToXpathManager = new IdToXpathManager();
		return idToXpathManager;
	}
	
	public TwsCachedXPathAPI getXpathApi(){
		if(xpathApi==null)xpathApi = new TwsCachedXPathAPI();
		return xpathApi;
	}
	
	public void cleanXpathApi(){
		xpathApi=null;
	}
	
	public Object getTransactionProperty(String propertyName) {
		if (requestedObject == null)
			return null;
		BeanInfo bi;
		try {
			bi = CachedIntrospector.getBeanInfo(requestedObject.getClass());
		} catch (IntrospectionException e) {
			Engine.logContext.error("getTransactionProperty : Exception while finding the bean info for transaction class '" + requestedObject.getClass() + "'", e);
			return null;
		}
		PropertyDescriptor[] propertyDescriptors = bi.getPropertyDescriptors();
		int len2 = propertyDescriptors.length;
		PropertyDescriptor propertyDescriptor = null;
		Object propertyValue;
		int j;
		String propertyDescriptorName = "";
		for (j = 0 ; j < len2 ; j++) {
		    propertyDescriptor = propertyDescriptors[j];
		    propertyDescriptorName = propertyDescriptor.getName();
		    if (propertyDescriptorName.equals(propertyName)) break;
		}
		if (j == len2 && !propertyDescriptorName.equals(propertyName)) { 
			Engine.logContext.debug("getTransactionProperty : no property descriptor found for the property '" + propertyName +"'");
			return null;
		}
	    
		Method getter = propertyDescriptor.getReadMethod();
	    Object args[] = { };
	    try {
			propertyValue = getter.invoke(requestedObject, args);
		} catch (Exception e) {
			Engine.logContext.error("getTransactionProperty : Exception while executing the property getter '" + getter.getName() + "'", e);
			return null;
		}
		return propertyValue;
	}

	public boolean isSOAPRequest() {
		String servletPath = httpServletRequest.getServletPath();
		return (servletPath.endsWith(".ws") || servletPath.endsWith(".wsl"));
	}

	public Connector getConnector() {
		return connector;
	}
	
	public Connector loadConnector(String connectorName) throws EngineException {
		this.connectorName = connectorName;
		loadConnector();
		return getConnector();
	}
	
	public void loadConnector() throws EngineException {
		if(connectorName == null) connectorName = project.getDefaultConnector().getName();
		String key = project.getName()+'\n'+connectorName;
		if(used_connectors.containsKey(key)){
			setConnector(used_connectors.get(key));
			Engine.logContext.debug("Re-use connector: " + connectorName);
		}else{
			setConnector(project.getConnectorByName(connectorName));
			Engine.logContext.debug("Loaded connector: " + connectorName);
			used_connectors.put(key, getConnector());
		}
	}
	
	public void loadSequence() throws EngineException {
		requestedObject = project.getSequenceByName(sequenceName);
		Engine.logContext.debug("Sequence loaded: " + requestedObject.getName());
		
		setConnector(null);
		transaction = null;
		transactionName = null;
	}

	public void setConnector(Connector connector) {
		if(this.connector!=null && this.connector!=connector)
			Engine.logContext.debug("Connector name differs from previous one; switch connector");
		
		this.connector = connector;
		if(connector!=null){
			this.connectorName = connector.getName();
			connector.context = this;
			isNewSession = opened_connectors.add(connector);
		}else{
			this.connectorName = null;
		}
	}

	public void abortRequestable() {
		if (requestedObject != null) {
			requestedObject.abort();
		}
	}
	
	public Collection<Connector> getOpenedConnectors(){
		return Collections.unmodifiableCollection(opened_connectors);
	}
	
	public void clearConnectors(){
		used_connectors.clear();
		opened_connectors.clear();
	}
	
	public void requireRemoval(boolean remove) {
		this.requireRemoval = remove;
	}
	
	public boolean removalRequired() {
		return this.requireRemoval;
	}
	
	public String getLogFilename(){
		Timestamp ts = new Timestamp(System.currentTimeMillis());
		String escapedContextID = contextID;
		escapedContextID = escapedContextID.substring(0, escapedContextID.indexOf("_"));
		escapedContextID = escapedContextID.replace('/', '_');
		escapedContextID = escapedContextID.replace('\\', '_');
		String logFilename = ts.toString().substring(0,10) + "_context_" + escapedContextID + ".log";
		return logFilename;
	}

	public String decodeFromHexString(String s) {
		return Crypto2.decodeFromHexString(s);
	}

	public String encodeToHexString(String s) {
		return Crypto2.encodeToHexString(s);
	}
	
	public String decodeFromHexString(String passphrase, String s) {
		return Crypto2.decodeFromHexString(passphrase, s);
	}

	public String encodeToHexString(String passphrase, String s) {
		return Crypto2.encodeToHexString(passphrase, s);
	}
	
	public Map<String, List<String>> getRequestHeaders(){
		if(requestHeaders==null){
			requestHeaders = new HashMap<String, List<String>>();
			if(httpServletRequest!=null){
				 for(String name: Collections.list(GenericUtils.<Enumeration<String>>cast(httpServletRequest.getHeaderNames()))){
					 String lowName = name.toLowerCase();
					 for(String value:Collections.list(GenericUtils.<Enumeration<String>>cast(httpServletRequest.getHeaders(name)))){
						 List<String> values = requestHeaders.get(lowName);
						 if(values==null){
							 values = new LinkedList<String>();
							 requestHeaders.put(lowName, values);
						 }
						 values.add(value);
					 }
				 }
			}
			requestHeaders = Collections.unmodifiableMap(requestHeaders);
		}
		return requestHeaders;
	}
	
	public void setRequest(HttpServletRequest request){
		httpServletRequest = request;
		httpSession = request.getSession();
		requestHeaders = null;
	}
	
	public void clearRequest(){
		httpServletRequest = null;
		httpSession = null;
		requestHeaders = null;
	}
	
	public Scriptable getSharedScope() {
		return sharedScope;
	}
	
	public void setSharedScope(Scriptable sharedScope) {
		this.sharedScope = sharedScope;
	}

	protected void checkXulRecorder() {
		if (xulRecorder != null) {
			if (xulRecorder.checkExpired()) {
				xulRecorder = null;
			}
		}
	}
	
	public void newXulRecorder(String urlRegex, int entryLifetime) {
		this.xulRecorder = new XulRecorder(urlRegex, entryLifetime);
	}
	
	public XulRecorder getXulRecorder() {
		return xulRecorder;
	}
	
	public boolean isXulRecording(String url) {
		return xulRecorder != null ? xulRecorder.isRecording(url) : false;
	}
	
	public void stopXulRecording() {
		if (xulRecorder != null) {
			xulRecorder.stopRecording();
		}
	}
	
	synchronized public void addTemporaryFile(File file) {
		if (temporaryFiles == null) {
			temporaryFiles = new LinkedList<File>();
		}
		temporaryFiles.add(file);
	}
	
	synchronized public void deleteTemporaryFiles() {
		if (temporaryFiles != null) {
			for (File file : temporaryFiles) {
				try {
					Engine.logContext.debug("(Context.deleteTemporaryFiles) Removing the temporary file : " + file.getAbsolutePath());
					FileUtils.deleteDirectory(file);
				} catch (IOException e) { }
			}
		}
	}
}
