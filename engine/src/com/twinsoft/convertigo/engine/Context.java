/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.engine;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
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
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
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
import com.twinsoft.convertigo.beans.transactions.AbstractHttpTransaction;
import com.twinsoft.convertigo.engine.cache.CacheEntry;
import com.twinsoft.convertigo.engine.enums.HttpPool;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.enums.RequestAttribute;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;
import com.twinsoft.convertigo.engine.enums.SessionStoreMode;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;
import com.twinsoft.convertigo.engine.util.Crypto2;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.HttpUtils;
import com.twinsoft.convertigo.engine.util.HttpUtils.HttpClientInterface;
import com.twinsoft.convertigo.engine.util.PropertiesUtils;
import com.twinsoft.convertigo.engine.util.SimpleMap;
import com.twinsoft.convertigo.engine.util.TwsCachedXPathAPI;
import com.twinsoft.twinj.Javelin;

public class Context extends AbstractContext implements Cloneable, Serializable {
	private static final long serialVersionUID = 1L;

	public transient LogParameters logParameters = new LogParameters();

	public String name;

	public CacheEntry cacheEntry;
	public boolean noCache = false;

	public boolean isDestroying = false;
	public boolean isErrorDocument = false;

	public boolean isXsltRequest = false;

	public boolean isAsync;
	public boolean isStubRequested;
	public int waitingRequests = 0;

	public boolean isNewSession;
	public boolean isRequestFromVic = false;
	public boolean isTrustedRequest = false;

	public long documentSignatureSent = 0;
	public long documentSignatureReceived = 0;

	public transient EngineStatistics statistics = new EngineStatistics();

	public Map<String, Block> previousFields = new HashMap<String, Block>();

	public String absoluteSheetUrl;
	public String sheetUrl;
	public String contentType;
	public String cacheControl;

	public transient Project project;
	public transient Connector connector;
	public transient Pool pool;
	public int poolContextNumber;
	public String projectName;
	public String sequenceName;
	public String transactionName;
	public String connectorName;
	public transient RequestableObject requestedObject;
	public transient ISheetContainer lastDetectedObject;

	public boolean removeNamespaces = false;

	// compatibility with older versions
	public transient ScreenClass lastDetectedScreenClass = null;
	public transient Transaction transaction = null;//

	public String subPath = "";

	public transient HttpState httpState = null;
	private transient Header[] responseHeaders = new Header[]{};
	private transient TwsCachedXPathAPI xpathApi = null;

	public boolean tasSessionKeyVerified = false;

	public transient SimpleMap server = Engine.theApp.getShareServerMap();

	private transient Map<String, Connector> used_connectors = new HashMap<String, Connector>();
	private transient Set<Connector> opened_connectors = new HashSet<Connector>();

	private boolean requireRemoval = false;

	private transient Map<String, List<String>> requestHeaders = null;

	private transient Scriptable sharedScope = null;

	private transient HttpClient httpClient3 = null;
	private transient HttpClientInterface httpClient = null;

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
		isStubRequested = false;
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

		userReference = null;

		removeNamespaces = false;

		// For compatibility with older javelin projects
		lastDetectedScreenClass = null;
		transaction = null;

		if (steps != null)
			steps.clear();

		// Reset last responseExpiryDate set (#4201)
		remove(Parameter.ResponseExpiryDate.getName());
		remove(Parameter.StubFilename.getName());
		
		Engine.logContext.debug("Context reset");
	}

	public String getProjectDirectory() {
		String dir = null;
		if (projectName != null) {
			dir = Engine.projectDir(projectName);
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
				Properties properties = PropertiesUtils.load(file);
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
				PropertiesUtils.store(properties, file);
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
		if (requestedObject instanceof AbstractHttpTransaction ) {
			if (!((AbstractHttpTransaction)requestedObject).isHandleCookie())
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
				cookies.add(sCookie);
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
		Document doc = parentNode.getOwnerDocument();
		Element newElement = doc.createElement(tagName);
		if(text!=null){
			Text textElement = doc.createTextNode(text);
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

	public TwsCachedXPathAPI getXpathApi(){
		if (xpathApi == null) {
			xpathApi = new TwsCachedXPathAPI(project);
		}
		return xpathApi;
	}

	public void cleanXpathApi(){
		xpathApi = null;
	}

	public Object getTransactionProperty(String propertyName) {
		if (requestedObject == null) {
			return null;
		}
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
		if (connectorName == null) {
			connectorName = project.getDefaultConnector().getName();
		}
		String key = project.getName() + '\n' + connectorName;
		Connector connector = used_connectors.get(key);
		if (connector != null && connector.getProject() == project) {
			Engine.logContext.debug("Re-use connector: " + connectorName);
		} else {
			used_connectors.remove(key);
			connector = project.getConnectorByName(connectorName);
			Engine.logContext.debug("Loaded connector: " + connectorName);
			used_connectors.put(key, connector);
		}
		setConnector(connector);
		connector.checkSymbols();
	}

	public void loadSequence() throws EngineException {
		Sequence sequence = project.getSequenceByName(sequenceName);

		// clone sequence in studio, needed by #3188 - Order parallel step response
		if (sequence.isOriginal()) {
			try {
				sequence = sequence.cloneKeepParent();
			} catch (CloneNotSupportedException e) {
				Engine.logContext.error("Clone of sequence failed ! Use the original.", e);
			}
		}
		requestedObject = sequence;

		Engine.logContext.debug("Sequence loaded: " + requestedObject.getName());
		requestedObject.checkSymbols();

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
		return this.requireRemoval && !this.isDestroying;
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
		parentContext = null;
		requestedObject = null;
	}

	public Scriptable getSharedScope() {
		return sharedScope;
	}

	public void setSharedScope(Scriptable sharedScope) {
		this.sharedScope = sharedScope;
	}

	public Context getRootContext() {
		return parentContext == null ? this : parentContext.getRootContext();
	}

	@Override
	public Context clone() throws CloneNotSupportedException {
		Context clone = (Context) super.clone();
		if (logParameters != null) {
			clone.logParameters = (LogParameters) logParameters.clone();
		}
		return clone;
	}

	public HttpClient getHttpClient3(HttpPool httpPool) {
		switch (httpPool) {
		case no:
			return HttpUtils.makeHttpClient3(false);
		case context:
			synchronized (used_connectors) {
				if (httpClient3 == null) {
					Engine.logContext.debug("Create a new context HTTP pool.");
					httpClient3 = HttpUtils.makeHttpClient3(true);
				}
			}

			return httpClient3;
		case session:
			HttpClient httpClient;

			synchronized (used_connectors) {
				httpClient = SessionAttribute.httpClient3.get(httpSession);
				if (httpClient == null) {
					Engine.logContext.debug("Create a new session HTTP pool.");
					httpClient = HttpUtils.makeHttpClient3(true);
					SessionAttribute.httpClient3.set(httpSession, httpClient);
				}
			}

			return httpClient;
		case global:
			return Engine.theApp.httpClient;
		}
		return null;
	}

	public HttpClientInterface getHttpClient(HttpPool httpPool) {
		switch (httpPool) {
		case no:
			return HttpUtils.makeHttpClient(false);
		case context:
			if (httpClient == null) {
				httpClient = HttpUtils.makeHttpClient(true);
			}
			return httpClient;
		case session:
			HttpClientInterface httpClient = SessionAttribute.httpClient4.get(httpSession);
			if (httpClient == null) {
				httpClient = HttpUtils.makeHttpClient(true);
				SessionAttribute.httpClient4.set(httpSession, httpClient);
			}
			return httpClient;
		case global:
			return Engine.theApp.httpClient4;
		}
		return null;
	}

	@Override
	public void setResponseHeader(String name, String value) {
		if (httpServletRequest != null) {
			Map<String, String> headers = RequestAttribute.responseHeader.get(httpServletRequest);

			if (headers == null) {
				RequestAttribute.responseHeader.set(httpServletRequest, headers = new HashMap<String, String>());
			}

			headers.put(name, value);
		}
	}

	@Override
	public void setResponseStatus(Integer code, String text) {
		if (httpServletRequest != null) {
			Map<Integer, String> status = RequestAttribute.responseStatus.get(httpServletRequest);

			if (status == null) {
				RequestAttribute.responseStatus.set(httpServletRequest, status = new HashMap<Integer, String>());
			}

			status.clear();
			status.put(code, text);
		}
	}

	public void addFileToDeleteAtEndOfContext(File file) {
		Set<File> files = GenericUtils.cast(get("fileToDeleteAtEndOfContext"));
		if (files == null) {
			files = new HashSet<>();
			set("fileToDeleteAtEndOfContext", files);
		}
		files.add(file);
	}

	public void addFileToDeleteAtEndOfSession(File file) {
		Set<File> files = GenericUtils.cast(httpSession.getAttribute("fileToDeleteAtEndOfContext"));
		if (files == null) {
			files = new HashSet<>();
			httpSession.setAttribute("fileToDeleteAtEndOfContext", files);
		}
		files.add(file);
	}

	@Serial
	private void writeObject(ObjectOutputStream out) throws IOException {
		var mode = EnginePropertiesManager.getProperty(EnginePropertiesManager.PropertyName.SESSION_STORE_MODE);
		if (!SessionStoreMode.redis.name().equalsIgnoreCase(mode)) {
			throw new NotSerializableException("Context serialization disabled for mode=" + mode);
		}
		try {
			if (Engine.logEngine.isDebugEnabled()) {
				Engine.logEngine.debug("(Context) writeObject [" + contextID + "]");
			}
		} catch (Exception e) {
			// ignore logging issues
		}
		out.defaultWriteObject();
		out.writeObject(StoredHttpState.from(httpState));
	}

	@Serial
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		var storedState = (StoredHttpState) in.readObject();
		httpState = storedState != null ? storedState.toHttpState() : null;
		logParameters = new LogParameters();
		statistics = new EngineStatistics();
		server = Engine.theApp != null ? Engine.theApp.getShareServerMap() : new SimpleMap();
		responseHeaders = new Header[]{};
		requestHeaders = null;
		xpathApi = null;
		sharedScope = null;
		used_connectors = new HashMap<String, Connector>();
		opened_connectors = new HashSet<Connector>();
		httpClient3 = null;
		httpClient = null;
	}

	private static final class StoredHttpState implements Serializable {
		@Serial
		private static final long serialVersionUID = 1L;
		private final List<StoredCookie> cookies;
		private final Integer cookiePolicy;

		private StoredHttpState(List<StoredCookie> cookies, Integer cookiePolicy) {
			this.cookies = cookies;
			this.cookiePolicy = cookiePolicy;
		}

		static StoredHttpState from(HttpState state) {
			if (state == null) {
				return null;
			}
			var cookies = new ArrayList<StoredCookie>();
			for (Cookie cookie : state.getCookies()) {
				var storedCookie = StoredCookie.from(cookie);
				if (storedCookie != null) {
					cookies.add(storedCookie);
				}
			}
			Integer storedPolicy = null;
			try {
				storedPolicy = Integer.valueOf(state.getCookiePolicy());
			} catch (Exception e) {
				// ignore and keep null
			}
			if (cookies.isEmpty() && storedPolicy == null) {
				return null;
			}
			return new StoredHttpState(cookies, storedPolicy);
		}

		HttpState toHttpState() {
			if ((cookies == null || cookies.isEmpty()) && cookiePolicy == null) {
				return null;
			}
			var state = new HttpState();
			if (cookiePolicy != null) {
				try {
					state.setCookiePolicy(cookiePolicy.intValue());
				} catch (Exception e) {
					// ignore if policy cannot be applied
				}
			}
			if (cookies != null) {
				for (var storedCookie : cookies) {
					var cookie = storedCookie.toCookie();
					if (cookie != null) {
						state.addCookie(cookie);
					}
				}
			}
			return state;
		}
	}

	private static final class StoredCookie implements Serializable {
		@Serial
		private static final long serialVersionUID = 1L;
		private final String name;
		private final String value;
		private final String domain;
		private final String path;
		private final Long expiry;
		private final boolean secure;
		private final int version;
		private final String comment;

		private StoredCookie(String name, String value, String domain, String path, Long expiry, boolean secure, int version, String comment) {
			this.name = name;
			this.value = value;
			this.domain = domain;
			this.path = path;
			this.expiry = expiry;
			this.secure = secure;
			this.version = version;
			this.comment = comment;
		}

		static StoredCookie from(Cookie cookie) {
			if (cookie == null) {
				return null;
			}
			var expiryDate = cookie.getExpiryDate();
			return new StoredCookie(
				cookie.getName(),
				cookie.getValue(),
				cookie.getDomain(),
				cookie.getPath(),
				expiryDate != null ? expiryDate.getTime() : null,
				cookie.getSecure(),
				cookie.getVersion(),
				cookie.getComment()
			);
		}

		Cookie toCookie() {
			if (name == null || domain == null) {
				return null;
			}
			try {
				var cookie = new Cookie(domain, name, value);
				if (path != null) {
					cookie.setPath(path);
				}
				if (expiry != null) {
					cookie.setExpiryDate(new Date(expiry));
				}
				cookie.setSecure(secure);
				cookie.setVersion(version);
				if (comment != null) {
					cookie.setComment(comment);
				}
				return cookie;
			} catch (Exception e) {
				return null;
			}
		}
	}
}
