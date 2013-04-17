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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.ConnectorEvent;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IDomainsFilterContainer;
import com.twinsoft.convertigo.beans.core.IScreenClassContainer;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.extractionrules.siteclipper.IRequestRule;
import com.twinsoft.convertigo.beans.extractionrules.siteclipper.IResponseRule;
import com.twinsoft.convertigo.beans.screenclasses.SiteClipperScreenClass;
import com.twinsoft.convertigo.beans.transactions.SiteClipperTransaction;
import com.twinsoft.convertigo.engine.CertificateManager;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineEvent;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EngineStatistics;
import com.twinsoft.convertigo.engine.LogParameters;
import com.twinsoft.convertigo.engine.MySSLSocketFactory;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.HtmlLocation;
import com.twinsoft.convertigo.engine.helpers.DomainsFilterHelper;
import com.twinsoft.convertigo.engine.helpers.ScreenClassHelper;
import com.twinsoft.convertigo.engine.parsers.XulRecorder;
import com.twinsoft.convertigo.engine.siteclipper.clientinstruction.IClientInstruction;
import com.twinsoft.convertigo.engine.util.CaseInsensitiveLinkedMap;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.HttpUtils;
import com.twinsoft.convertigo.engine.util.Log4jHelper;
import com.twinsoft.convertigo.engine.util.RegexpUtils;
import com.twinsoft.convertigo.engine.util.RhinoUtils;
import com.twinsoft.convertigo.engine.util.URLUtils;

public class SiteClipperConnector extends Connector implements IScreenClassContainer<SiteClipperScreenClass>, IDomainsFilterContainer {
	private static final long serialVersionUID = -1802468058387036775L;
	
	// Shuttle works on one request
	public static class Shuttle {
		
		// ENUM only used by Shuttle
		private enum QueryKey {
			connector,
			context;
			
			String value(Map<String, String> parameters) {
				return parameters.get(name());
			}
		}
		
		public enum HttpMethodType {
			GET,
			POST;
		}
		
		public enum DynamicVariable {
			convertigo_path,
			project_path,
			siteclipper_path,
			host_path,
			tail_path;
			
			public String value(Shuttle shuttle) {
				return shuttle.getRequest(this);
			}
		}
		
		public enum ContentEncoding {
			none,
			gzip,
			deflate;
		}
		
		// PATTERN only used by Shuttle
		private final static Pattern variables_pattern = Pattern.compile("\\$(" + StringUtils.join(DynamicVariable.values(), '|') + ")\\$");		
		private final static Pattern charset_pattern = Pattern.compile("(.*?)( ?; ?charset=(.*)|$)", Pattern.CASE_INSENSITIVE);
		private final static Pattern and_pattern = Pattern.compile(",");
		private final static Pattern equal_pattern = Pattern.compile("=");
		
		private final Map<String, String> requestCustomHeaders = new CaseInsensitiveLinkedMap<String>();
		private final Map<String, String> responseCustomHeaders = new CaseInsensitiveLinkedMap<String>();

		private HttpServletRequest request;
		private HttpServletResponse response;
		
		private Matcher url_matcher;
		private Map<String, String> parameters;
		
		private HttpMethod httpMethod;
		
		private ProcessState processState = ProcessState.request;
		
		private String defaultResponseCharset = null;
		private String responseCharset = null;
		private String responseMimeType = "";
		private byte[] responseAsByte = null;
		private String responseAsString = null;
		private String responseAsStringOriginal = null;
		private ContentEncoding responseContentEncoding = null;
		private String statisticsTaskID = "";
		private long score = 0L;
		private Scriptable sharedScope = null;
		private Scriptable clonedScope = null;
		private List<IClientInstruction> postInstructions = null;
		
		private Shuttle(HttpServletRequest request, HttpServletResponse response, Matcher url_matcher) {
			this.request = request;
			this.response = response;
			this.url_matcher = url_matcher;
		}
		
		private Context context = null;
		
		public Context getContext() {
			return context;
		}
		
		public HttpSession getSession() {
			return request.getSession();
		}
		
		private void process() throws ServletException {
			try {
				LogParameters logParameters = new LogParameters();
				Log4jHelper.mdcSet(logParameters);
				
				long uniqueRequestID = System.currentTimeMillis() + (long) (Math.random() * 1261440000000L);
				logParameters.put("UID", Long.toHexString(uniqueRequestID));
				logParameters.put("ClientIP", request.getRemoteAddr());
				
				String sessionID = request.getSession().getId();
				Engine.logSiteClipper.debug("(SiteClipperConnector) find sessionID : " + sessionID);
				
				String projectName = getRequest(QueryPart.projectName);
				Engine.logSiteClipper.debug("(SiteClipperConnector) find projectName : " + projectName);
				
				String siteclipperQuery = getRequest(QueryPart.siteclipperQuery);
				Engine.logSiteClipper.trace("(SiteClipperConnector) find siteclipperQuery : " + siteclipperQuery);
				
				parameters = Collections.unmodifiableMap(GenericUtils.uniqueMap(URLUtils.queryToMap(siteclipperQuery, and_pattern, equal_pattern)));
				
				String connectorName = getRequest(QueryKey.connector);
				Engine.logSiteClipper.debug("(SiteClipperConnector) find connectorName : " + connectorName);
				
				String contextName = getRequest(QueryKey.context);
				Engine.logSiteClipper.debug("(SiteClipperConnector) find contextName : " + contextName);
				
				try {
					context = Engine.theApp.contextManager.get(null, contextName, sessionID, null, projectName, connectorName, null);
					Engine.logSiteClipper.trace("(SiteClipperConnector) context id retrieved : " + context.contextID);
					contextName = context.name;
					
					if (context.project == null) {
						Engine.theApp.contextManager.remove(context);
						throw new ServletException("(SiteClipperConnector) the context " + context.name + " isn't initialized (no project loaded or expired)");
					}
					context.lastAccessTime = System.currentTimeMillis();
					logParameters.put("ContextID", context.contextID);
					logParameters.put("Project", context.projectName);
					
					// if no connector provided, search other SiteClipperConnector of the project
					// and choose the default if exists
					if (connectorName == null) {
						Connector connector = context.project.getDefaultConnector();
						if (connector instanceof SiteClipperConnector) {
							connectorName = connector.getName();
						} else {
							for (Connector cn : context.project.getConnectorsList()) {
								if (cn instanceof SiteClipperConnector) {
									if (connectorName == null) {
										connectorName = cn.getName();
									} else {
										throw new ServletException("(SiteClipperConnector) too more connector, no default suitable SiteClipperConnector in the project " + projectName);
									}
								}
							}
						}
					}
					
					Connector connector = context.loadConnector(connectorName);
					logParameters.put("Connector", connectorName);
					
					if (connector instanceof SiteClipperConnector) {
						SiteClipperConnector siteClipperConnector = (SiteClipperConnector) connector;
						
						siteClipperConnector.siteClipperRequestObjectsPerThread.put(Thread.currentThread(), this);
						try {
							defaultResponseCharset = siteClipperConnector.getDefaultResponseCharset();
							siteClipperConnector.processRequest(this);
						} finally {
							siteClipperConnector.siteClipperRequestObjectsPerThread.remove(Thread.currentThread());
							context.lastAccessTime = System.currentTimeMillis();
						}
					} else {
						throw new ServletException("(SiteClipperConnector) this isn't a SiteClipperConnector : " + connectorName);
					}
					
				} catch (Exception e) {
					Engine.logSiteClipper.info("(SiteClipperConnector) failed to process the request because of\n[" + e.getClass().getSimpleName() + "] " + e.getMessage());
					throw new ServletException("(SiteClipperConnector) failed to process the request", e);
				}
			} finally {
				Log4jHelper.mdcClear();
				request = null;
				response = null;
			}
		}
		
		public String getRequest(QueryPart queryPart) {
			String part = queryPart.value(url_matcher);
			if (part == null) {
				part = "";
			} else if (queryPart.equals(QueryPart.port)) {
				part = part.replace(',', ':');
			}
			return part;
		}
		
		public String getRequest(QueryKey queryKey) {
			return queryKey.value(parameters);
		}
		
		public String getRequestParameter(String parameterName) {
			return request.getParameter(parameterName);
		}
		
		public HttpMethodType getRequestHttpMethodType() {
			return HttpMethodType.valueOf(request.getMethod());
		}
		
		public Scheme getRequestScheme() {
			return Scheme.valueOf(getRequest(QueryPart.scheme));
		}
		
		public String getRequest(DynamicVariable dynamiqueVariable) {
			switch(dynamiqueVariable) {
			case convertigo_path : return getRequest(QueryPart.full_convertigo_path);
			case project_path : return getRequest(QueryPart.full_project_path);
			case siteclipper_path : return getRequest(QueryPart.full_siteclipper_path);
			case host_path : return getRequest(QueryPart.full_host_path);
			case tail_path :
				String currentURI = url_matcher.group(0);
				Matcher matcher_tail = url_tail.matcher(currentURI);
				if (matcher_tail.matches()) {
					currentURI = matcher_tail.group(1);
				}
				return currentURI;
			}
			return null;
		}
		
		public int getRequestPort() {
			String s = getRequest(QueryPart.port_num);
			if (s.length() == 0) {
				switch (getRequestScheme()) {
				case https: return 443;
				case http:
				default: return 80;
				}
			} else {
				return Integer.parseInt(s);
			}
		}
		
		public ProcessState getProcessState() {
			return processState;
		}
		
		public String getCustomHeader(HeaderName name) {
			return getCustomHeader(name.value());
		}
		
		public String getCustomHeader(String name) {
			switch(processState) {
			case request : return getRequestCustomHeader(name);
			case response : return getResponseCustomHeader(name);
			}
			return null;
		}
		
		public void setCustomHeader(HeaderName name, String value) {
			setCustomHeader(name.value(), value);
		}
		
		public void setCustomHeader(String name, String value) {
			switch(processState) {
			case request :
				setRequestCustomHeader(name, value);
				break;
			case response :
				setResponseCustomHeader(name, value);
				break;
			}
		}
		
		public String getRequestHeader(String name) {
			return request.getHeader(name);
		}

		public String getRequestCustomHeader(String name) {
			return requestCustomHeaders.get(name);
		}
		
		public void setRequestCustomHeader(String name, String value) {
			if (value == null) {
				requestCustomHeaders.remove(name);
			} else {
				requestCustomHeaders.put(name, value);
			}
		}
		
		public String getRequestUrl() {
			return getRequest(QueryPart.scheme) + "://" + getRequest(QueryPart.host) + getRequest(QueryPart.port) + getRequest(QueryPart.uri); 
		}
		
		public String getRequestUrlAndQuery() {
			String query = request.getQueryString();
			if (query != null) {
				return getRequestUrl() + "?" + query;
			} else {
				return getRequestUrl();
			}
		}
		
		public String getResponseHeader(String name) {
			Header header = httpMethod.getResponseHeader(name);
			return header != null ? header.getValue() : null;
		}
		
		public String getResponseCustomHeader(String name) {
			return responseCustomHeaders.get(name);
		}
		
		public void setResponseCustomHeader(String name, String value) {
			if (value == null) {
				responseCustomHeaders.remove(name);
			} else {
				responseCustomHeaders.put(name, value);
			}
		}
		
		public int getResponseStatusCode() {
			return httpMethod.getStatusCode();
		}
		
		public String getResponseContentType() {
			Header contentType = httpMethod.getResponseHeader(HeaderName.ContentType.value());
			return contentType != null ? contentType.getValue() : "";
		}
		
		public ContentEncoding getResponseContentEncoding() {
			if (responseContentEncoding == null) {
				Header contentEncoding = httpMethod.getResponseHeader(HeaderName.ContentEncoding.value());
				responseContentEncoding = (contentEncoding != null) ? ContentEncoding.valueOf(contentEncoding.getValue()) : ContentEncoding.none;
			}
			return responseContentEncoding;
		}
		
		public String getResponseCharset() {
			if (responseCharset == null) {
				Matcher charset_matcher = charset_pattern.matcher(getResponseContentType());
				if (charset_matcher.matches()) {
					responseMimeType = charset_matcher.group(1);
					responseCharset = charset_matcher.group(3);
				}
				if (responseCharset == null || responseCharset.length() == 0) {
					responseCharset = defaultResponseCharset;
				}
				
			}
			return responseCharset;
		}
		
		public String getResponseMimeType() {
			if (responseCharset == null) {
				getResponseCharset();
			}
			return responseMimeType;
		}
		
		private byte[] getResponseAsBytes() throws IOException {
			if (responseAsByte == null) {
				InputStream responseBodyInputStream = httpMethod.getResponseBodyAsStream();
				if (responseBodyInputStream == null) {
					responseAsByte = new byte[0];
				} else {
					responseAsByte = IOUtils.toByteArray(responseBodyInputStream);
				}
			}
			return responseAsByte;
		}
		
		public String getResponseAsString() throws UnsupportedEncodingException, IOException {
			if (responseAsString == null) {
				InputStream stream = new ByteArrayInputStream(getResponseAsBytes());
				if (stream != null) {
					switch(getResponseContentEncoding()) {
					case gzip:
						stream = new GZIPInputStream(stream);
						break;
					case deflate:
						stream = new InflaterInputStream(stream, new Inflater(true));
						break;
					default:
						break;
					}
					String charset = getResponseCharset();
					responseAsString = IOUtils.toString(stream, charset);
				}
				responseAsStringOriginal = responseAsString;
			}
			return responseAsString;
		}
		
		public void setResponseAsString(String content) {
			responseAsString = content;
		}
		
		public String makeAbsoluteURL(String url) {
			return makeAbsoluteURL(url, false);
		}
		
		public String makeAbsoluteURL(String url, boolean withHost) {
			if (url != null && url.length() != 0) {
				Matcher scheme_host_matcher = scheme_host_pattern.matcher(url);
				if (scheme_host_matcher.find()) {
					// url of type scheme://url is replaced by ...siteclipper/scheme/url
					url = getRequest(QueryPart.full_siteclipper_path) + '/' + SchemeHost.convert(scheme_host_matcher);
				} else if (url.startsWith("/")) {
					// url of type /uri is replaced by ...siteclipper/current_scheme/current_host/uri
					url = getRequest(DynamicVariable.host_path) + url;
				} else {
					// if url is 'a/b' and current uri is 'y/x/z', the new one will be 'y/x/a/b'
					url = getRequest(DynamicVariable.tail_path) + '/' + url;
				}
			} else {
				url = getRequest(DynamicVariable.host_path);
			}
			
			if (withHost) {
				String schemeAndHost = URLUtils.getSchemeAndHost(HttpUtils.originalRequestURL(request));
				if (schemeAndHost != null) {
					url = schemeAndHost + url;
				}
			}
			return url;
		}
		
		public String resolveVariables(String string_with_variables) {
			Matcher m = variables_pattern.matcher(string_with_variables);
			StringBuffer sb = new StringBuffer();
			while(m.find()) {
				m.appendReplacement(sb, DynamicVariable.valueOf(m.group(1)).value(this));
			}
			m.appendTail(sb);
			return sb.toString();
		}
		
		public Object evalJavascript(String expression) {
			return evalJavascript(expression, new HashMap<String, Object>());
		}
		
		public Object evalJavascript(String expression, Map<String, Object> objectsToAddInScope) {
			if (sharedScope != null) {
				try {
					org.mozilla.javascript.Context ctx = org.mozilla.javascript.Context.enter();
					if (clonedScope == null) {
						clonedScope = RhinoUtils.copyScope(ctx, sharedScope);
					}
					
					for (String objectName : objectsToAddInScope.keySet()) {
						clonedScope.put(objectName, clonedScope, objectsToAddInScope.get(objectName));
					}
					
					Object res = ctx.evaluateString(clonedScope, expression, "", 0, null);
					return res;
				} finally {
					org.mozilla.javascript.Context.exit();
				}
			}
			return null;
		}
		
		public void addPostInstruction(IClientInstruction postInstruction) {
			if (postInstructions == null) {
				postInstructions = new LinkedList<IClientInstruction>();
			}
			postInstructions.add(postInstruction);
		}
	}
	
	// END OF Shuttle START OF SiteClipperConnector
	
	/**
	 * /convertigo/projects/my_project/connector=my_connector&context=my_context.siteclipper/http/remote_host,18080/remote_path/remote_resource
	 * 1 : /convertigo/projects/my_project/connector=my_connector&context=my_context.siteclipper/http/remote_host,18080
	 * 2 : /convertigo/projects/my_project/connector=my_connector&context=my_context.siteclipper | 3 : /convertigo/projects/my_project | 4 : /convertigo
	 * 5 : my_project   | 6 : connector=my_connector&context=my_context   | 7 : http   | 8 : remote_host   | 9 : ,18080   | 10 : 18080   | 11 : /remote_path/remote_resource
	*/
	public enum QueryPart {
		full_host_path(1),
		full_siteclipper_path(2),
		full_project_path(3),
		full_convertigo_path(4),
		projectName(5),
		siteclipperQuery(6),
		scheme(7),
		host(8),
		port(9),
		port_num(10),
		uri(11);
		
		int order;
		
		QueryPart(int order) {
			this.order = order;
		}
		
		String value(Matcher url_matcher) {
			return url_matcher.group(order);
		}
	}
	
	private enum SchemeHost {
		scheme(1),
		host(2),
		port(3),
		uri(4);
		
		int order;
		
		SchemeHost(int order) {
			this.order = order;
		}
		
		String value(Matcher scheme_host_matcher) {
			String value = scheme_host_matcher.group(order); 
			return value == null ? "" : value;
		}

		static String convert(Matcher scheme_host_matcher) {
			String scheme = SchemeHost.scheme.value(scheme_host_matcher);
			String host = SchemeHost.host.value(scheme_host_matcher);
			String port = SchemeHost.port.value(scheme_host_matcher);
			String uri = SchemeHost.uri.value(scheme_host_matcher);
			if (port.length() != 0) {
				port = "," + port;
			}
			
			return scheme + "/" + host + port + uri;
		}
		
		static String convert(String targetURL) {
			Matcher scheme_host_matcher = scheme_host_pattern.matcher(targetURL);
			if (scheme_host_matcher.matches()) {
				return convert(scheme_host_matcher);
			}
			return targetURL;
		}
	}

	public enum Scheme {
		http,
		https;
	}

	public enum ProcessState {
		request,
		response;
	}
	
	private final static Pattern url_pattern = Pattern.compile("((((.*?)/projects/(.*?))/(.*?)\\.siteclipper)/(.*?)/(.*?)(,([\\d]*))?)($|/.*)");
	private final static Pattern scheme_host_pattern = Pattern.compile("(.*?)://(.*?)(?::([\\d]*))?(/.*|$)");
	private final static Pattern url_tail = Pattern.compile("(.*)/.*?$");

	// Use of HashSet to speedup Collection.contains because hash checking seams speeder than list walking
	private final static Collection<HeaderName> requestHeadersToIgnore = Collections.unmodifiableCollection(new HashSet<HeaderName>(Arrays.asList(
		HeaderName.Cookie, HeaderName.ContentLength, HeaderName.XConvertigoHttpsState, HeaderName.XConvertigoRequestHost, HeaderName.XConvertigoRequestURI
	)));
	private final static Collection<HeaderName> responseHeadersToIgnore = Collections.unmodifiableCollection(new HashSet<HeaderName>(Arrays.asList(
		HeaderName.SetCookie, HeaderName.ContentLength, HeaderName.TransferEncoding
	)));
	
	private String defaultResponseCharset = "UTF-8";
	private XMLVector<XMLVector<String>> domainsListing = new XMLVector<XMLVector<String>>();
	private boolean trustAllServerCertificates;
	
	transient final private Map<Thread, Shuttle> siteClipperRequestObjectsPerThread = Collections.synchronizedMap(new HashMap<Thread, Shuttle>());
	transient final private CertificateManager certificateManager = new CertificateManager();
	
	transient private ScreenClassHelper<SiteClipperScreenClass> screenClassHelper = new ScreenClassHelper<SiteClipperScreenClass>(this);
	transient private DomainsFilterHelper domainsFilter = new DomainsFilterHelper(this);
	transient private HostConfiguration hostConfiguration = null;
	
	public static boolean handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		String requestURI = HttpUtils.originalRequestURI(request);
		Engine.logSiteClipper.trace("(SiteClipperConnector) requestURI : " + requestURI);
		
		Matcher url_matcher = url_pattern.matcher(requestURI);
		if (!url_matcher.matches()) {
			Engine.logSiteClipper.trace("(SiteClipperConnector) doesn't match, release the request handler");
			return false;
		}
		
		new Shuttle(request, response, url_matcher).process();
		
		return true;
	}
	
	public static String constructSiteLocation(Context context, Connector connector, String targetURL) {	
		Map<String, String[]> query = new HashMap<String, String[]>();
		
		query.put("context", new String[]{ context.name });
		Engine.logSiteClipper.trace("(SiteClipperConnector) Retrieve context name : " + context.name);
		
		query.put("connector", new String[]{ connector.getName() });
		Engine.logSiteClipper.trace("(SiteClipperConnector) Retrieve connector name : " + connector.getName());
		
		String requestURL = HttpUtils.originalRequestURL(context.httpServletRequest);
		Engine.logSiteClipper.debug("(SiteClipperConnector) Retrieve requestURL to modify : " + requestURL);
		
		targetURL = SchemeHost.convert(targetURL);
		
		String tail_url = URLUtils.mapToQuery(query, ",", "=") + ".siteclipper/" + targetURL;
		Matcher matcher_tail = url_tail.matcher(requestURL);
		if (matcher_tail.matches()) {
			requestURL = matcher_tail.group(1);
		}
		String location = requestURL + '/' + tail_url;
		
		Engine.logSiteClipper.debug("(SiteClipperConnector) Computed location for SiteClipper : " + location);		
		return location;
	}

	private void processRequest(Shuttle shuttle) throws IOException, ServletException, EngineException {
		if (isDebugging()) {
			synchronized (this) {
				doProcessRequest(shuttle);
			}
		} else {
			doProcessRequest(shuttle);
		}
	}
	
	private void doProcessRequest(Shuttle shuttle) throws IOException, ServletException, EngineException {
		shuttle.statisticsTaskID = context.statistics.start(EngineStatistics.GET_DOCUMENT);
		try {
			shuttle.sharedScope = context.getSharedScope();

			String domain = shuttle.getRequest(QueryPart.host) + shuttle.getRequest(QueryPart.port);
			Engine.logSiteClipper.trace("(SiteClipperConnector) Prepare the request for the domain " + domain);
			if (!shouldRewrite(domain)) {
				Engine.logSiteClipper.info("(SiteClipperConnector) The domain " + domain + " is not allowed with this connector");
				shuttle.response.sendError(HttpServletResponse.SC_FORBIDDEN, "The domain " + domain + " is not allowed with this connector");
				return;
			}

			String uri = shuttle.getRequest(QueryPart.uri);

			Engine.logSiteClipper.info("Preparing " + shuttle.request.getMethod() + " "
					+ shuttle.getRequestUrl());

			HttpMethod httpMethod = null;
			XulRecorder xulRecorder = context.getXulRecorder();
			if (xulRecorder != null) {
				httpMethod = shuttle.httpMethod = xulRecorder.getRecord(shuttle.getRequestUrlAndQuery());
			}
			if (httpMethod == null) {
				try {
					switch (shuttle.getRequestHttpMethodType()) {
					case GET :
						httpMethod = new GetMethod(uri);
						break;
					case POST :
						httpMethod = new PostMethod(uri);
						((PostMethod) httpMethod).setRequestEntity(new InputStreamRequestEntity(shuttle.request.getInputStream()));
						break;
					default :
						throw new ServletException("(SiteClipperConnector) unknown http method " + shuttle.request.getMethod());
					}
					httpMethod.setFollowRedirects(false);
				} catch (Exception e) {
					throw new ServletException("(SiteClipperConnector) unexpected exception will building the http method : " + e.getMessage());
				}
				shuttle.httpMethod = httpMethod;

				SiteClipperScreenClass screenClass = getCurrentScreenClass();
				Engine.logSiteClipper.info("Request screen class: " + screenClass.getName());

				for (String name : Collections.list(GenericUtils.<Enumeration<String>>cast(shuttle.request.getHeaderNames()))) {
					if (requestHeadersToIgnore.contains(HeaderName.parse(name))) {
						Engine.logSiteClipper.trace("(SiteClipperConnector) Ignoring request header " + name);
					} else {
						String value = shuttle.request.getHeader(name);
						Engine.logSiteClipper.trace("(SiteClipperConnector) Copying request header " + name + "=" + value);
						shuttle.setRequestCustomHeader(name, value);
					}
				}

				Engine.logSiteClipper.debug("(SiteClipperConnector) applying request rules for the screenclass " + screenClass.getName());
				for (IRequestRule rule : screenClass.getRequestRules()) {
					if (rule.isEnabled()) {
						Engine.logSiteClipper.trace("(SiteClipperConnector) applying request rule " + rule.getName());
						rule.fireEvents();
						boolean done = rule.applyOnRequest(shuttle);
						Engine.logSiteClipper.debug("(SiteClipperConnector) the request rule " + rule.getName() + " is " + (done ? "well" : "not") + " applied");
					} else {
						Engine.logSiteClipper.trace("(SiteClipperConnector) skip the disabled request rule " + rule.getName());
					}
				}

				for (Entry<String, String> header : shuttle.requestCustomHeaders.entrySet()) {
					Engine.logSiteClipper.trace("(SiteClipperConnector) Push request header " + header.getKey() + "=" + header.getValue());
					httpMethod.addRequestHeader(header.getKey(), header.getValue());
				}

				String queryString = shuttle.request.getQueryString();

				if (queryString != null) {
					try {
						// Fake test in order to check query string validity
						new URI("http://localhost/index?" + queryString, true, httpMethod.getParams().getUriCharset());
					} catch (URIException e) {
						// Bugfix #2103
						StringBuffer newQuery = new StringBuffer();
						for (String part : RegexpUtils.pattern_and.split(queryString)) {
							String[] pair = RegexpUtils.pattern_equals.split(part, 2);
							try {
								newQuery.append('&').append(URLEncoder.encode(URLDecoder.decode(pair[0], "UTF-8"), "UTF-8"));
								if (pair.length > 1) {
									newQuery.append('=').append(URLEncoder.encode(URLDecoder.decode(pair[1], "UTF-8"), "UTF-8"));
								}
							} catch (UnsupportedEncodingException ee) {
								Engine.logSiteClipper.trace("(SiteClipperConnector) failed to encore query part : " + part);
							}
						}

						queryString = newQuery.length() > 0 ? newQuery.substring(1) : newQuery.toString();
						Engine.logSiteClipper.trace("(SiteClipperConnector) re-encode query : " + queryString);
					}
				}

				Engine.logSiteClipper.debug("(SiteClipperConnector) Copying the query string : " + queryString);
				httpMethod.setQueryString(queryString);
				
				if (context.httpState == null) {
					Engine.logSiteClipper.debug("(SiteClipperConnector) Creating new HttpState for context id " + context.contextID);
					context.httpState = new HttpState();
				} else {
					Engine.logSiteClipper.debug("(SiteClipperConnector) Using HttpState of context id " + context.contextID);
				}

				HostConfiguration hostConfiguration = getHostConfiguration(shuttle);
				
				httpMethod.getParams().setParameter("http.connection.stalecheck", new Boolean(true));

				Engine.logSiteClipper.info("Requesting " + httpMethod.getName() + " "
						+ hostConfiguration.getHostURL()
						+ httpMethod.getURI().toString() + (queryString == null ? "" : "?" + queryString));

				Engine.theApp.httpClient.executeMethod(hostConfiguration, httpMethod, context.httpState);
			} else {
				Engine.logSiteClipper.info("Retrieve recorded response from Context");
			}

			int status = httpMethod.getStatusCode();

			shuttle.processState = ProcessState.response;

			Engine.logSiteClipper.info("Request terminated with status " + status);
			shuttle.response.setStatus(status);

			if (Engine.isStudioMode() && status == HttpServletResponse.SC_OK && shuttle.getResponseMimeType().startsWith("text/")) {
				fireDataChanged(new ConnectorEvent(this, shuttle.getResponseAsString()));
			}

			SiteClipperScreenClass screenClass = getCurrentScreenClass();
			Engine.logSiteClipper.info("Response screen class: " + screenClass.getName());

			if (Engine.isStudioMode()) {
				Engine.theApp.fireObjectDetected(new EngineEvent(screenClass));
			}

			for (Header header : httpMethod.getResponseHeaders()) {
				String name = header.getName();
				if (responseHeadersToIgnore.contains(HeaderName.parse(name))) {
					Engine.logSiteClipper.trace("(SiteClipperConnector) Ignoring response header " + name);
				} else {
					String value = header.getValue();
					Engine.logSiteClipper.trace("(SiteClipperConnector) Copying response header " + name + "=" + value);
					shuttle.responseCustomHeaders.put(name, value);
				}
			}

			Engine.logSiteClipper.debug("(SiteClipperConnector) applying response rules for the screenclass " + screenClass.getName());
			for (IResponseRule rule : screenClass.getResponseRules()) {
				if (rule.isEnabled()) {
					Engine.logSiteClipper.trace("(SiteClipperConnector) applying response rule " + rule.getName());
					rule.fireEvents();
					boolean done = rule.applyOnResponse(shuttle);
					Engine.logSiteClipper.debug("(SiteClipperConnector) the response rule " + rule.getName() + " is " + (done ? "well" : "not") + " applied");
				} else {
					Engine.logSiteClipper.trace("(SiteClipperConnector) skip the disabled response rule " + rule.getName());
				}
			}

			for (Entry<String, String> header : shuttle.responseCustomHeaders.entrySet()) {
				Engine.logSiteClipper.trace("(SiteClipperConnector) Push request header " + header.getKey() + "=" + header.getValue());
				shuttle.response.addHeader(header.getKey(), header.getValue());
			}

			if (shuttle.postInstructions != null) {
				JSONArray instructions = new JSONArray();
				for (IClientInstruction instruction : shuttle.postInstructions) {
					try {
						instructions.put(instruction.getInstruction());
					} catch (JSONException e) {
						Engine.logSiteClipper.error("(SiteClipperConnector) Failed to add a post instruction due to a JSONException", e);
					}
				}
				String codeToInject = "<script>C8O_postInstructions = " + instructions.toString() + "</script>\n"
				+ "<script src=\"" + shuttle.getRequest(QueryPart.full_convertigo_path) + "/scripts/jquery.min.js\"></script>\n"
				+ "<script src=\"" + shuttle.getRequest(QueryPart.full_convertigo_path) + "/scripts/siteclipper.js\"></script>\n";

				String content = shuttle.getResponseAsString();
				Matcher matcher = HtmlLocation.head_top.matcher(content);
				String newContent = RegexpUtils.inject(matcher, codeToInject);
				if (newContent == null) {
					matcher = HtmlLocation.body_top.matcher(content);
					newContent = RegexpUtils.inject(matcher, codeToInject);
				}
				if (newContent != null) {
					shuttle.setResponseAsString(newContent);
				} else {
					Engine.logSiteClipper.info("(SiteClipperConnector) Failed to find a head or body tag in the response content");
					Engine.logSiteClipper.trace("(SiteClipperConnector) Response content : \"" + content + "\"");
				}
			}

			long nbBytes = 0L;
			if (shuttle.responseAsString != null && shuttle.responseAsString.hashCode() != shuttle.responseAsStringOriginal.hashCode()) {
				OutputStream os = shuttle.response.getOutputStream();
				switch(shuttle.getResponseContentEncoding()) {
				case gzip:
					os = new GZIPOutputStream(os);
					break;
				case deflate:
					os = new DeflaterOutputStream(os, new Deflater(Deflater.DEFAULT_COMPRESSION|Deflater.DEFAULT_STRATEGY, true));
					break;
				default:
					break;
				}
				nbBytes = shuttle.responseAsByte.length;
				IOUtils.write(shuttle.responseAsString, os, shuttle.getResponseCharset());
				os.close();
			} else {
				InputStream is = (shuttle.responseAsByte == null)? httpMethod.getResponseBodyAsStream() : new ByteArrayInputStream(shuttle.responseAsByte);
				if (is != null) {
					nbBytes = IOUtils.copyLarge(is, shuttle.response.getOutputStream());
					Engine.logSiteClipper.trace("(SiteClipperConnector) Response body copyied (" + nbBytes + " bytes)");
				}
			}
			shuttle.response.getOutputStream().close();
			
			shuttle.score = getScore(nbBytes);
			Engine.logSiteClipper.debug("(SiteClipperConnector) Request terminated with a score of " + shuttle.score);
		} finally {
			long duration = context.statistics.stop(shuttle.statisticsTaskID);
			if (context.requestedObject != null) {
				try {
					Engine.theApp.billingManager.insertBilling(context, Long.valueOf(duration), Long.valueOf(shuttle.score));
				}
				catch (Exception e) {
					Engine.logContext.warn("Unable to insert billing ticket (the billing is thus ignored): [" + e.getClass().getName() + "] " + e.getMessage());
				}
			}
		}
	}
	
	private static long getScore(long nb) {
		long score = nb / 10000; // score is incremented by one per 10K of read bytes
		return score;
	}
	
	private boolean isCompatibleConfiguration(boolean testEquals){
		if (!testEquals) {
			Engine.logSiteClipper.debug("(SiteClipperConnector) Incompatible HostConfiguration, remove existing one");
			hostConfiguration = null;			
		}
		return testEquals;
	}
	
	private synchronized HostConfiguration getHostConfiguration(Shuttle shuttle) throws EngineException, MalformedURLException {
		if (hostConfiguration != null) {
			String host = hostConfiguration.getHost();
			if (isCompatibleConfiguration(host != null && host.equals(shuttle.getRequest(QueryPart.host)))) {
				if (isCompatibleConfiguration(hostConfiguration.getPort() == shuttle.getRequestPort())){
					Protocol protocol = hostConfiguration.getProtocol();
					String scheme = (protocol == null) ? "" : protocol.getScheme();
					if (isCompatibleConfiguration(scheme != null && scheme.equals(shuttle.getRequest(QueryPart.scheme)))) {
						if (Engine.theApp.proxyManager.isEnabled()) {
							String proxyHost = hostConfiguration.getProxyHost();
							if (proxyHost == null){
								proxyHost = "";
							}
							if (isCompatibleConfiguration(proxyHost.equals(Engine.theApp.proxyManager.getProxyServer()))){
								//don't test the proxy port if the proxy host is null
								isCompatibleConfiguration(hostConfiguration.getProxyPort() == Engine.theApp.proxyManager.getProxyPort());
								isCompatibleConfiguration(hostConfiguration.getParams().getParameter("hostConfId").equals(Engine.theApp.proxyManager.getHostConfId()));
							}	
						}
					}
				}
			}		
		}
		
		if (hostConfiguration == null) {
			Engine.logSiteClipper.debug("(SiteClipperConnector) create a new HostConfiguration");
			hostConfiguration = new HostConfiguration();
			String host = shuttle.getRequest(QueryPart.host);
			if (shuttle.getRequestScheme() == Scheme.https) {
				Engine.logSiteClipper.debug("(SiteClipperConnector) Setting up SSL properties");
				
				certificateManager.collectStoreInformation(context);
				
				Engine.logSiteClipper.debug("(SiteClipperConnector) CertificateManager has changed: " + certificateManager.hasChanged);
				Engine.logSiteClipper.debug("(SiteClipperConnector) Using MySSLSocketFactory for creating the SSL socket");
				Protocol myhttps = new Protocol("https", (ProtocolSocketFactory) new MySSLSocketFactory(
					certificateManager.keyStore, certificateManager.keyStorePassword,
					certificateManager.trustStore, certificateManager.trustStorePassword,
					trustAllServerCertificates), shuttle.getRequestPort());

				hostConfiguration.setHost(host, shuttle.getRequestPort(), myhttps);
			} else {
				hostConfiguration.setHost(host, shuttle.getRequestPort());
			}
			
			URL requestUrl = new URL(shuttle.getRequestUrl());
			
			Engine.theApp.proxyManager.setProxy(hostConfiguration, context.httpState, requestUrl);
		}
		return hostConfiguration;
	}
	
	@Override
	public SiteClipperConnector clone() throws CloneNotSupportedException {
		SiteClipperConnector siteClipperConnector = (SiteClipperConnector) super.clone();
		siteClipperConnector.screenClassHelper = new ScreenClassHelper<SiteClipperScreenClass>(siteClipperConnector);
		siteClipperConnector.domainsFilter = new DomainsFilterHelper(siteClipperConnector);
		return siteClipperConnector;
	}
	
	public Shuttle getShuttle() {
		return siteClipperRequestObjectsPerThread.get(Thread.currentThread());
	}
	
	@Override
	public void prepareForTransaction(Context context) throws EngineException {
	}
	
	@Override
	public void add(DatabaseObject databaseObject) throws EngineException {
		if (!screenClassHelper.add(databaseObject)) {
			super.add(databaseObject);
		}
	}

	public List<SiteClipperScreenClass> getAllScreenClasses() {
		return sort(screenClassHelper.getAllScreenClasses());
	}

	public SiteClipperScreenClass getCurrentScreenClass() {
		return screenClassHelper.getCurrentScreenClass();
	}

	public SiteClipperScreenClass getDefaultScreenClass() {
		return screenClassHelper.getDefaultScreenClass();
	}

	public SiteClipperScreenClass getScreenClassByName(String screenClassName) {
		return screenClassHelper.getScreenClassByName(screenClassName);
	}

	public void setDefaultScreenClass(ScreenClass defaultScreenClass) throws EngineException {
		screenClassHelper.setDefaultScreenClass(defaultScreenClass);
	}

	public void setDefaultResponseCharset(String defaultResponseCharset) {
		this.defaultResponseCharset = defaultResponseCharset;
	}

	public String getDefaultResponseCharset() {
		return defaultResponseCharset;
	}
		
	@Override
	public List<DatabaseObject> getAllChildren() {
		List<DatabaseObject> childrens = super.getAllChildren();
		childrens.add(0, getDefaultScreenClass());
		return childrens;
	}
	
	public XMLVector<XMLVector<String>> getDomainsListing() {
		return domainsListing;
	}

	public void setDomainsListing(XMLVector<XMLVector<String>> domainsListing) {
		this.domainsListing = domainsListing;
		domainsFilter.reset();
	}
	
	public boolean shouldRewrite(String url) {
		return domainsFilter.shouldRewrite(url);
	}

	public boolean isTrustAllServerCertificates() {
		return trustAllServerCertificates;
	}

	public void setTrustAllServerCertificates(boolean trustAllServerCertificates) {
		this.trustAllServerCertificates = trustAllServerCertificates;
	}
	
	@Override
	public SiteClipperTransaction newTransaction() {
		return new SiteClipperTransaction();
	}
	
	public SiteClipperScreenClass newScreenClass() {
		return new SiteClipperScreenClass();
	}
}