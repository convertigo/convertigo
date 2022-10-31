/*
 * Copyright (c) 2001-2019 Convertigo SA.
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

package com.twinsoft.convertigo.beans.connectors;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;
import javax.swing.event.EventListenerList;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.OptionsMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.TraceMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.ConnectorEvent;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.transactions.AbstractHttpTransaction;
import com.twinsoft.convertigo.beans.transactions.HttpTransaction;
import com.twinsoft.convertigo.beans.variables.RequestableHttpVariable;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.CertificateManager;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.HttpStateEvent;
import com.twinsoft.convertigo.engine.HttpStateListener;
import com.twinsoft.convertigo.engine.MySSLSocketFactory;
import com.twinsoft.convertigo.engine.Version;
import com.twinsoft.convertigo.engine.enums.AuthenticationMode;
import com.twinsoft.convertigo.engine.enums.DoFileUploadMode;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.HttpMethodType;
import com.twinsoft.convertigo.engine.enums.HttpPool;
import com.twinsoft.convertigo.engine.enums.MimeType;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.oauth.HttpOAuthConsumer;
import com.twinsoft.convertigo.engine.plugins.VicApi;
import com.twinsoft.convertigo.engine.util.BigMimeMultipart;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.HttpUtils;
import com.twinsoft.convertigo.engine.util.ParameterUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.URLUtils;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.util.StringEx;

import oauth.signpost.exception.OAuthException;

/**
 * The Connector class is the base class for all connectors.
 */
public class HttpConnector extends Connector {

	private static final long serialVersionUID = -3169027624556390926L;

	public static String DYNAMIC_HEADER_PREFIX = "(dynamic)-";

	public interface HeaderForwarder {
		public void add(String name, String value, String forwardPolicy);
	}

	private XMLVector<XMLVector<String>> httpHeaderForward = new XMLVector<XMLVector<String>>();
	
	transient public CertificateManager certificateManager = null;
	transient public HostConfiguration hostConfiguration = null;
	transient public HttpState httpState = null;
	transient public boolean handleCookie = true;
	transient public XMLVector<XMLVector<String>> httpParameters = new XMLVector<XMLVector<String>>();
	transient public String postQuery = "";
	transient public String sUrl = "";
	transient public URL url;
	transient private String referer = "";
	transient private String charset = null;
	transient public Element httpInfoElement;
	
	//oAuth Signature
	transient public HttpOAuthConsumer oAuthConsumer = null;
	transient String oAuthKey = null;
	transient String oAuthSecret = null;
	transient String oAuthToken = null;
	transient String oAuthTokenSecret = null;	
	
	public static final String HTTP_HEADER_FORWARD_POLICY_REPLACE = "Replace";
	public static final String HTTP_HEADER_FORWARD_POLICY_IGNORE = "Ignore";
	public static final String HTTP_HEADER_FORWARD_POLICY_MERGE = "Merge";
	transient private Map<String, String> httpHeaderForwardMap = null;
	
	private String urlEncodingCharset = "UTF-8";

	transient private boolean doMultipartFormData;
	transient private String contentType;
	
	public HttpConnector() {
		super();

		certificateManager = new CertificateManager();
				
		hostConfiguration = new HostConfiguration();
	}

    @Override
	public void preconfigure(Element element) throws Exception {
		super.preconfigure(element);

		String version = element.getAttribute("version");

		if (VersionUtils.compare(version, "5.3.3") < 0) {
			NodeList properties = element.getElementsByTagName("property");

			Element propName = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "name");
			String objectName = (String) XMLUtils.readObjectFromXml((Element) XMLUtils.findChildNode(propName,
					Node.ELEMENT_NODE));

			Element propHttpHeaderForward = (Element) XMLUtils.findNodeByAttributeValue(properties, "name",
					"httpHeaderForward");
			if (propHttpHeaderForward != null) {
				NodeList values = propHttpHeaderForward.getElementsByTagName("java.lang.String");
				
				int len = values.getLength();
				for (int i = 0; i < len; i++) {
					Element httpHeaderForwardPolicy = (Element) values.item(i);

					String value = httpHeaderForwardPolicy.getAttribute("value");
					if ("true".equals(value)) {
						httpHeaderForwardPolicy.setAttribute("value", HttpConnector.HTTP_HEADER_FORWARD_POLICY_MERGE);
						hasChanged = true;
					}
					else if ("false".equals(value)) {
						httpHeaderForwardPolicy.setAttribute("value", HttpConnector.HTTP_HEADER_FORWARD_POLICY_REPLACE);
						hasChanged = true;
					}
				}
				
				if (hasChanged) {
					Engine.logBeans.warn("[Sequence] The object \"" + objectName
							+ "\" has been updated to version 5.3.3 (property \"httpHeaderForward\" update)");
				}
			}
		}
	}

	@Override
	public HttpConnector clone() throws CloneNotSupportedException {
		HttpConnector clonedObject = (HttpConnector) super.clone();
		clonedObject.httpStateListeners = new EventListenerList();
		clonedObject.sUrl = "";
		clonedObject.handleCookie = true;
		clonedObject.httpParameters = new XMLVector<XMLVector<String>>();
		clonedObject.postQuery = "";

		clonedObject.certificateManager = new CertificateManager();

		clonedObject.hostConfiguration = new HostConfiguration();
		clonedObject.givenAuthPassword = null;
		clonedObject.givenAuthUser = null;

		return clonedObject;
	}

	public void setBaseUrl(String httpUrl) {
		if (org.apache.commons.lang3.StringUtils.isNotBlank(httpUrl)) {
			sUrl = httpUrl;
		} else {
			setBaseUrl();
	}
	}

	public void setBaseUrl() {
		sUrl = "http";

		if (https) {
			sUrl += "s";
		}

		sUrl += "://" + server;
		
		if ((https && (port != 443)) || (!https && (port != 80))) {
			sUrl += ":" + port;
		}
		sUrl += baseDir;
	}

	public String getBaseUrl() {
		setBaseUrl();
		return sUrl;
	}

	public String getReferer() {
		return referer;
	}

	public String getCharset() {
		return charset;
	}

	private String appendToQuery(String query, String key, Object value) {
		if (!query.isEmpty()) {
			query += "&";
		}
		return query + URLUtils.encodePart(key, ParameterUtils.toString(value));
	}
	
	private String appendToQuery(String queryString, boolean isMultiValued, String httpVariable, Object httpObjectVariableValue) {
		if (isMultiValued) {
			if (httpObjectVariableValue instanceof Collection<?>) {
				for (Object httpVariableValue : (Collection<?>) httpObjectVariableValue) {
					queryString = appendToQuery(queryString, httpVariable, httpVariableValue);
				}
			} else if (httpObjectVariableValue.getClass().isArray()) {
				for (Object httpVariableValue : (Object[]) httpObjectVariableValue) {
					queryString = appendToQuery(queryString, httpVariable, httpVariableValue);
				}
			}
		}
		// standard case
		else {
			queryString = appendToQuery(queryString, httpVariable, httpObjectVariableValue);
		}
		return queryString;
	}
	
	@Override
	public void prepareForTransaction(Context context) throws EngineException {	
		Engine.logBeans.debug("(HttpConnector) Preparing for transaction");

		if (Boolean.parseBoolean(EnginePropertiesManager.getProperty(PropertyName.SSL_DEBUG))) {
			System.setProperty("javax.net.debug", "all");
			Engine.logBeans.trace("(HttpConnector) Enabling SSL debug mode");
		} else {
			System.setProperty("javax.net.debug", "");
			Engine.logBeans.debug("(HttpConnector) Disabling SSL debug mode");
		}

		Engine.logBeans.debug("(HttpConnector) Initializing...");

		if (context.isRequestFromVic) {
			// Check the VIC authorizations only if this is a non trusted
			// request, i.e. from a request not triggered from VIC (for
			// instance, from a web service call).
			if (!context.isTrustedRequest) {
				try {
					VicApi vicApi = new VicApi();
					if (!vicApi.isServiceAuthorized(context.tasUserName, context.tasVirtualServerName,
							context.tasServiceCode)) {
						throw new EngineException("The service '" + context.tasServiceCode
								+ "' is not authorized for the user '" + context.tasUserName + "'");
					}
				} catch (IOException e) {
					throw new EngineException("Unable to retrieve authorization from the VIC database.", e);
				}
			}
		}

		AbstractHttpTransaction httpTransaction = null;
		try {
			httpTransaction = (AbstractHttpTransaction) context.requestedObject;
		} catch (ClassCastException e) {
			throw new EngineException("Requested object is not a transaction", e);
		}

		handleCookie = httpTransaction.isHandleCookie();
		if (!handleCookie && httpState != null) {
			httpState.clearCookies(); // remove cookies from previous transaction
		}

		httpParameters = httpTransaction.getCurrentHttpParameters();
		
		contentType = MimeType.WwwForm.value();
		
		for (List<String> httpParameter : httpParameters) {
			String headerName = httpParameter.get(0);
			String value = httpParameter.get(1);
			
			// Content-Type
			if (HeaderName.ContentType.is(headerName)) {
				contentType = value;
			}
			
			// oAuth Parameters are passed as standard Headers
			if (HeaderName.OAuthKey.is(headerName)) {
				oAuthKey = value;
			}
			if (HeaderName.OAuthSecret.is(headerName)) {
				oAuthSecret = value;
			}
			if (HeaderName.OAuthToken.is(headerName)) {
				oAuthToken = value;
			}
			if (HeaderName.OAuthTokenSecret.is(headerName)) {
				oAuthTokenSecret = value;
			}
		}
		
		{
			String overrideContentType = ParameterUtils.toString(httpTransaction.getParameterValue(Parameter.HttpContentType.getName()));
			if (overrideContentType != null) {
				contentType = overrideContentType;
			}
		}

		int len = httpTransaction.numberOfVariables();
		boolean isFormUrlEncoded = MimeType.WwwForm.is(contentType);
		
		doMultipartFormData = false;
		for (int i = 0; i < len; i++) {
			RequestableHttpVariable trVariable = (RequestableHttpVariable) httpTransaction.getVariable(i);
			if (trVariable.getDoFileUploadMode() == DoFileUploadMode.multipartFormData) {
				doMultipartFormData = true;
				isFormUrlEncoded = true;
			}
		}
		
		// Retrieve request template file if necessary
		File requestTemplateFile = null;
		if (!isFormUrlEncoded) {
			String requestTemplateUrl = httpTransaction.getRequestTemplate();
			if (!requestTemplateUrl.equals("")) {
				String projectDirectoryName = context.project.getName();
				String absoluteRequestTemplateUrl = Engine.projectDir(projectDirectoryName) + "/"
						+ (context.subPath.length() > 0 ? context.subPath + "/" : "") + requestTemplateUrl;
				Engine.logBeans.debug("(HttpConnector) Request template Url: " + absoluteRequestTemplateUrl);
				requestTemplateFile = new File(absoluteRequestTemplateUrl);
				if (!requestTemplateFile.exists()) {
					Engine.logBeans.debug("(HttpConnector) The local request template file (\""
							+ absoluteRequestTemplateUrl
							+ "\") does not exist. Trying search in Convertigo TEMPLATES directory...");
					absoluteRequestTemplateUrl = Engine.TEMPLATES_PATH + "/" + requestTemplateUrl;
					Engine.logBeans.debug("(HttpConnector) Request template Url: "
							+ absoluteRequestTemplateUrl);
					requestTemplateFile = new File(absoluteRequestTemplateUrl);
					if (!requestTemplateFile.exists()) {
						Engine.logBeans
								.debug("(HttpConnector) The common request template file (\""
										+ absoluteRequestTemplateUrl
										+ "\") does not exist. Trying absolute search...");
						absoluteRequestTemplateUrl = requestTemplateUrl;
						Engine.logBeans.debug("(HttpConnector) Request template Url: "
								+ absoluteRequestTemplateUrl);
						requestTemplateFile = new File(absoluteRequestTemplateUrl);
						if (!requestTemplateFile.exists()) {
							throw new EngineException("Could not find any request template file \""
									+ requestTemplateUrl + "\" for transaction \"" + httpTransaction.getName()
									+ "\".");
						}
					}
				}
			}
		}

		// Sets or overwrites server url
		String httpUrl = httpTransaction.getParameterStringValue(Parameter.ConnectorConnectionString.getName());
		if (org.apache.commons.lang3.StringUtils.isNotBlank(httpUrl)) {
			setBaseUrl(httpUrl);
		} else {
			setBaseUrl();

		String transactionBaseDir = httpTransaction.getCurrentSubDir();
		if (transactionBaseDir.startsWith("http")) {
			sUrl = transactionBaseDir;
			/*
			 * if (transactionBaseDir.startsWith("https")) setHttps(true);
			 */
			} else {
			sUrl += transactionBaseDir;
			}
		}

		// Setup the SSL properties if needed
		if (https) {
			Engine.logBeans.debug("(HttpConnector) Setting up SSL properties");
			certificateManager.collectStoreInformation(context);
		}

		String variable, method, httpVariable, queryString = "";
		Object httpObjectVariableValue;
		boolean isMultiValued = false;
		boolean bIgnoreVariable = false;
		
		String urlEncodingCharset = httpTransaction.getComputedUrlEncodingCharset();

		// Replace variables in URL
		List<String> urlPathVariableList = AbstractHttpTransaction.getPathVariableList(sUrl);
		if (!urlPathVariableList.isEmpty()) {
			Engine.logBeans.debug("(HttpConnector) Defined URL: " + sUrl);
			for (String varName : urlPathVariableList) {
				RequestableHttpVariable rVariable = (RequestableHttpVariable) httpTransaction.getVariable(varName);
				httpObjectVariableValue = rVariable == null ? "":httpTransaction.getParameterValue(varName);
				httpVariable = rVariable == null ? "null":varName;
				method = rVariable == null ? "NULL":rVariable.getHttpMethod();
				
				Engine.logBeans.trace("(HttpConnector) Path variable: " + varName + " => (" + method + ") " + httpVariable);
				
				sUrl = sUrl.replaceAll("\\{" + varName + "\\}", ParameterUtils.toString(httpObjectVariableValue));
			}
		}
		
		// Build query string
		for (int i = 0; i < len; i++) {
			RequestableHttpVariable rVariable = (RequestableHttpVariable) httpTransaction.getVariable(i);
			variable = rVariable.getName();
			isMultiValued = rVariable.isMultiValued();
			method = rVariable.getHttpMethod();
			httpVariable = rVariable.getHttpName();
			httpObjectVariableValue = httpTransaction.getParameterValue(variable);

			bIgnoreVariable = urlPathVariableList.contains(variable) ||
								httpObjectVariableValue == null ||
								httpVariable.isEmpty() ||
								!method.equals("GET");
			
			if (!bIgnoreVariable) {
				Engine.logBeans.trace("(HttpConnector) Query variable: " + variable + " => (" + method + ") " + httpVariable);
				queryString = appendToQuery(queryString, isMultiValued, httpVariable, httpObjectVariableValue);
			}
		}

		// Encodes URL if it contains special characters
		sUrl = URLUtils.encodeAbsoluteURL(sUrl, urlEncodingCharset);

		if (queryString.length() != 0) {
			if (sUrl.indexOf('?') == -1) {
				sUrl += "?" + queryString;
			} else {
				sUrl += "&" + queryString;
			}
		}

		Engine.logBeans.debug("(HttpConnector) URL: " + sUrl);

		if (Engine.logBeans.isDebugEnabled()) {
			Engine.logBeans.debug("(HttpConnector) GET query: " + Visibility.Logs.replaceVariables(httpTransaction.getVariablesList(), queryString));
		}
		
		if (doMultipartFormData) {
			Engine.logBeans.debug("(HttpConnector) Skip postQuery computing and do a multipart/formData content");
			return;
		}

		// Build body for POST/PUT
		postQuery = "";

		// Load request template in postQuery if necessary
		if (!isFormUrlEncoded) {

			// Ticket #1040
			// A request template may be an XML document; in this case, the
			// "standard"
			// process apply: tokens are replaced by their respective value.
			// But a request template may also be an XSL document. In this case,
			// an XML
			// document giving all transaction variables should be built and
			// applied to
			// the XSL in order to produce a real XML request template.
			if (requestTemplateFile != null) {
				try {
					FileInputStream fis = new FileInputStream(requestTemplateFile);
					Document requestTemplate = XMLUtils.parseDOM(fis);

					Element documentElement = requestTemplate.getDocumentElement();
					// XSL document
					if (documentElement.getNodeName().equalsIgnoreCase("xsl:stylesheet")) {
						// Build the variables XML document
						Document variablesDocument = XMLUtils.createDom("java");
						Element variablesElement = variablesDocument.createElement("variables");
						variablesDocument.appendChild(variablesElement);

						for (RequestableVariable requestableVariable : httpTransaction.getVariablesList()) {
							RequestableHttpVariable trVariable = (RequestableHttpVariable) requestableVariable;
							variable = trVariable.getName();
							isMultiValued = trVariable.isMultiValued();
							httpVariable = trVariable.getHttpName();

							Element variableElement = variablesDocument.createElement("variable");
							variablesElement.appendChild(variableElement);
							variableElement.setAttribute("name", variable);

							httpObjectVariableValue = httpTransaction.getParameterValue(variable);
							if (httpObjectVariableValue != null) {
								if (isMultiValued) {
									variableElement.setAttribute("multi", "true");
									if (httpObjectVariableValue instanceof Collection<?>) {
										for (Object httpVariableValue : (Collection<?>) httpObjectVariableValue) {
											Element valueElement = variablesDocument.createElement("value");
											variableElement.appendChild(valueElement);
											Text valueText = variablesDocument
													.createTextNode(getStringValue(trVariable, httpVariableValue));
											valueElement.appendChild(valueText);
										}
									}
								} else {
									Element valueElement = variablesDocument.createElement("value");
									variableElement.appendChild(valueElement);
									Text valueText = variablesDocument
											.createTextNode(getStringValue(trVariable, httpObjectVariableValue));
									valueElement.appendChild(valueText);
								}
							}
						}

						if (Engine.logBeans.isDebugEnabled()) {
							String sVariablesDocument = XMLUtils.prettyPrintDOM((Document)Visibility.Logs.replaceVariables(httpTransaction.getVariablesList(), variablesDocument));
							Engine.logBeans.debug("Build variables XML document:\n" + sVariablesDocument);
						}

						// Apply XSL
						TransformerFactory tFactory = TransformerFactory.newInstance();
						StreamSource streamSource = new StreamSource(new FileInputStream(requestTemplateFile));
						Transformer transformer = tFactory.newTransformer(streamSource);
						StringWriter sw = new StringWriter();
						transformer.transform(new DOMSource(variablesElement), new StreamResult(sw));

						postQuery = sw.getBuffer().toString();
					}
					// XML document
					else {
						// Template has been parsed from file, retrieve its declared encoding char set
						// If not found use "UTF-8" according to HTTP POST for text/xml (see getData)
						String xmlEncoding = requestTemplate.getXmlEncoding();
						xmlEncoding = (xmlEncoding == null) ? "UTF-8" : xmlEncoding;
						
						postQuery = XMLUtils.prettyPrintDOMWithEncoding(requestTemplate, xmlEncoding);
					}
				} catch (Exception e) {
					Engine.logBeans
							.warn("Unable to parse the request template file as a valid XML/XSL document");
					throw new EngineException(
							"An unexpected error occured while retrieving the request template file for transaction \""
									+ httpTransaction.getName() + "\".", e);
				}
			}
		}
		
		RequestableHttpVariable body = (RequestableHttpVariable) httpTransaction.getVariable(Parameter.HttpBody.getName());
		if (body != null) {
			method = body.getHttpMethod();
			httpObjectVariableValue = httpTransaction.getParameterValue(Parameter.HttpBody.getName());
			if (method.equals("POST") && httpObjectVariableValue != null) {
				postQuery = ParameterUtils.toString(httpObjectVariableValue);
				isFormUrlEncoded = false;
			}
		}
		
		// Add all input variables marked as POST
		boolean isLogHidden = false;
		List<String> logHiddenValues = new ArrayList<String>();
		
		for (int i = 0; i < len; i++) {
			bIgnoreVariable = false;
			RequestableHttpVariable trVariable = (RequestableHttpVariable) httpTransaction.getVariable(i);
			variable = trVariable.getName();
			isMultiValued = trVariable.isMultiValued();
			method = trVariable.getHttpMethod();
			httpVariable = trVariable.getHttpName();
			isLogHidden = Visibility.Logs.isMasked(trVariable.getVisibility());
			
			// do not add variable to query if empty name
			if (httpVariable.equals(""))
				bIgnoreVariable = true;

			// Retrieves variable value
			httpObjectVariableValue = httpTransaction.getParameterValue(variable);
			
			if (method.equals("POST")) {
				// variable must be sent as an HTTP parameter
				if (!bIgnoreVariable) {
					Engine.logBeans.trace("(HttpConnector) Parameter variable: " + variable + " => (" + method + ") " + httpVariable);
					// Content-Type is 'application/x-www-form-urlencoded'
					if (isFormUrlEncoded) {
						// Replace variable value in postQuery
						if (httpObjectVariableValue != null) {
							// handle multivalued variable
							postQuery = appendToQuery(postQuery, isMultiValued, httpVariable, httpObjectVariableValue);
						}
					}
					// Content-Type is 'text/xml'
					else {
						// Replace variable value in postQuery
						if (httpObjectVariableValue != null) {
							// Handle multivalued variable
							if (isMultiValued) {
								String varPattern = "$(" + httpVariable + ")";
								int varPatternIndex, indexAfterPattern, beginTagIndex, endTagIndex;
								if (httpObjectVariableValue instanceof Collection<?>) {
									// while postQuery contains the variable
									// pattern
									while (postQuery.indexOf(varPattern) != -1) {
										varPatternIndex = postQuery.indexOf(varPattern);
										indexAfterPattern = varPatternIndex + varPattern.length();
										if (postQuery.substring(indexAfterPattern).startsWith("concat")) {
											// concat every value from the
											// vector
											// to replace the occurrence in the
											// template
											// by the concatenation of the
											// multiple values
											String httpVariableValue = "";
											for (Object var : (Collection<?>) httpObjectVariableValue)
												httpVariableValue += getStringValue(trVariable, var);
											if (isLogHidden) logHiddenValues.add(httpVariableValue);
											postQuery = postQuery.substring(0, varPatternIndex)
													+ httpVariableValue
													+ postQuery.substring(indexAfterPattern
													+ "concat".length());
										} else {
											// duplicate the tag surrounding the
											// occurrence in the template
											// for each value from the vector
											beginTagIndex = postQuery.substring(0, varPatternIndex)
													.lastIndexOf('<');
											endTagIndex = indexAfterPattern
													+ postQuery.substring(indexAfterPattern).indexOf('>');
											String tmpPostQuery = postQuery.substring(0, beginTagIndex);
											for (Object httpVariableValue : (Collection<?>) httpObjectVariableValue) {
												String stringValue = getStringValue(trVariable, httpVariableValue);
												if (isLogHidden) {
													logHiddenValues.add(stringValue);
												}
												tmpPostQuery += (postQuery.substring(beginTagIndex,
														varPatternIndex)
														+ stringValue + postQuery.substring(
														indexAfterPattern, endTagIndex + 1));
											}
											tmpPostQuery += postQuery.substring(endTagIndex + 1);
											postQuery = tmpPostQuery;
										}
									}
								} else {
									String stringValue = getStringValue(trVariable, httpObjectVariableValue);
									if (isLogHidden) {
										logHiddenValues.add(stringValue);
									}
									StringEx sx = new StringEx(postQuery);
									sx.replaceAll("$(" + httpVariable + ")concat", stringValue);
									postQuery = sx.toString();
								}
							}
							// Handle single valued variable
							else {
								String stringValue = getStringValue(trVariable, httpObjectVariableValue);
								
								if (isLogHidden) {
									logHiddenValues.add(stringValue);
								}
								StringEx sx = new StringEx(postQuery);
								sx.replaceAll("$(" + httpVariable + ")noE", stringValue);
								sx.replaceAll("$(" + httpVariable + ")", stringValue);
								postQuery = sx.toString();
							}
						}
						// Remove variable from postQuery
						else {
							String varPattern = "$(" + httpVariable + ")";
							int varPatternIndex, beginTagIndex, endTagIndex;
							// while postQuery contains the variable pattern
							while (postQuery.indexOf(varPattern) != -1) {
								varPatternIndex = postQuery.indexOf(varPattern);
								beginTagIndex = postQuery.substring(0, varPatternIndex).lastIndexOf('<');
								endTagIndex = postQuery.indexOf('>', varPatternIndex);
								postQuery = postQuery.substring(0, beginTagIndex)
										+ postQuery.substring(endTagIndex + 1);
							}
						}
					}
				}
			} else if (method.equals("")) {
				// Replace variable value in postQuery
				if (httpObjectVariableValue != null) {
					if (!isFormUrlEncoded && (!(httpVariable.equals("")))) {// used
																			// to
																			// replace
																			// empty
																			// element
						String stringValue = getStringValue(trVariable, httpObjectVariableValue);
						if (isLogHidden) {
							logHiddenValues.add(stringValue);
						}
						StringEx sx = new StringEx(postQuery);
						sx.replaceAll(httpVariable, stringValue);
						postQuery = sx.toString();
					}
				}
			}
		}
		
		if (Engine.logBeans.isDebugEnabled()) {
			Engine.logBeans.debug("(HttpConnector) POST query: " + (isFormUrlEncoded ? "" : "\n") 
					+ (isFormUrlEncoded ? 
							Visibility.Logs.replaceVariables(httpTransaction.getVariablesList(), postQuery):
								Visibility.Logs.replaceValues(logHiddenValues, postQuery)));
		}
		Engine.logBeans.debug("(HttpConnector) Connector successfully prepared for transaction");
	}

	protected static final int BUFFER_SIZE = 8192;
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.twinsoft.convertigo.beans.core.Connector#addTransaction(com.twinsoft
	 * .convertigo.beans.core.Transaction)
	 */
	@Override
	protected void addTransaction(Transaction transaction) throws EngineException {
		if (!(transaction instanceof AbstractHttpTransaction))
			throw new EngineException("You cannot add to a HTTP connector a database object of type "
					+ transaction.getClass().getName());
		super.addTransaction(transaction);
	}
	
	private void getHttpState(Context context) {
		if (authenticationPropertiesHasChanged) {
			if (context.httpState != null) {
				context.httpState.clearCredentials();
				context.httpState.clearProxyCredentials();
			}
			authenticationPropertiesHasChanged = false;			
		}
		
		boolean stateChanged = false;
		if (context.httpState == null) {
			Engine.logBeans.debug("(HttpConnector) Creating new HttpState for context id " + context.contextID);
			context.httpState = new HttpState();
			stateChanged = true;
		} else {
			Engine.logBeans.debug("(HttpConnector) Using HttpState of context id " + context.contextID);
		}
		
		String realm = null;
		if (!authUser.equals("") || !authPassword.equals("") || (givenAuthUser != null) || (givenAuthPassword != null)) {
			String user = ((givenAuthUser == null) ? authUser : givenAuthUser);
			String password = ((givenAuthPassword == null) ? authPassword : givenAuthPassword);
			String host = hostConfiguration.getHost() == null ? server : hostConfiguration.getHost();
			String domain = NTLMAuthenticationDomain;
			
			if (authenticationType.setCredentials(context.httpState, user, password, host, domain)) {
				stateChanged = true;
			}
		}

		httpState = context.httpState;
		
		if (stateChanged) {
			fireStateChanged(new HttpStateEvent(this, context, realm, server, httpState));
		}
	}

	public void resetHttpState(Context context) {
		context.httpState = null;
		getHttpState(context);
	}

	public byte[] getData(Context context) throws IOException, EngineException {
		return getData(context, sUrl);
	}
	
	public byte[] getData(Context context, String sUrl) throws IOException, EngineException {
		HttpMethod method = null;

		// Fire event for plugins
		long t0 = System.currentTimeMillis();
		Engine.theApp.pluginsManager.fireHttpConnectorGetDataStart(context, t0);

		try {
			Engine.logBeans.trace("(HttpConnector) Retrieving data as a bytes array...");
			Engine.logBeans.debug("(HttpConnector) Connecting to: " + sUrl);

			// Setting the referer
			referer = sUrl;

			Engine.logBeans.debug("(HttpConnector) Https: " + https);
			
			URL url = new URL(sUrl);
			String host = url.getHost();
			int port = url.getPort();
			
			if (sUrl.toLowerCase().startsWith("https:")) {
				if (!https) {
					Engine.logBeans.debug("(HttpConnector) Setting up SSL properties");
					certificateManager.collectStoreInformation(context);
				}
				
				if (port == -1)
					port = 443;

				Engine.logBeans.debug("(HttpConnector) Host: " + host + ":" + port);

				Engine.logBeans.debug("(HttpConnector) CertificateManager has changed: "
						+ certificateManager.hasChanged);
				if (certificateManager.hasChanged || (!host.equalsIgnoreCase(hostConfiguration.getHost()))
						|| (hostConfiguration.getPort() != port)) {
					Engine.logBeans
							.debug("(HttpConnector) Using MySSLSocketFactory for creating the SSL socket");
					Protocol myhttps = new Protocol("https", MySSLSocketFactory.getSSLSocketFactory(
							certificateManager.keyStore, certificateManager.keyStorePassword,
							certificateManager.trustStore, certificateManager.trustStorePassword,
							this.trustAllServerCertificates), port);

					hostConfiguration.setHost(host, port, myhttps);
				}
				
				sUrl = url.getFile();
				Engine.logBeans.debug("(HttpConnector) Updated URL for SSL purposes: " + sUrl);
			} else {
				Engine.logBeans.debug("(HttpConnector) Host: " + host + ":" + port);
				hostConfiguration.setHost(host, port);
			}
			
			// Retrieving httpState
			getHttpState(context);
			
			// Proxy configuration
			Engine.theApp.proxyManager.setProxy(hostConfiguration, httpState, url);
			
			AbstractHttpTransaction httpTransaction = (AbstractHttpTransaction) context.transaction;
			
			// Retrieve HTTP method
			HttpMethodType httpVerb = httpTransaction.getHttpVerb();
			String sHttpVerb = httpVerb.name();
			final String sCustomHttpVerb = httpTransaction.getCustomHttpVerb();
			
			if (sCustomHttpVerb.length() > 0) {
				Engine.logBeans.debug("(HttpConnector) HTTP verb: " + sHttpVerb + " overridden to '" + sCustomHttpVerb + "'");
				
				switch (httpVerb) {
				case GET:
					method = new GetMethod(sUrl) {
						@Override
						public String getName() {
							return sCustomHttpVerb;
						}
					};
					break;
				case POST:
					method = new PostMethod(sUrl) {
						@Override
						public String getName() {
							return sCustomHttpVerb;
						}
					};
					break;
				case PUT:
					method = new PutMethod(sUrl) {
						@Override
						public String getName() {
							return sCustomHttpVerb;
						}
					};
					break;
				case DELETE:
					method = new DeleteMethod(sUrl) {
						@Override
						public String getName() {
							return sCustomHttpVerb;
						}
					};
					break;
				case HEAD:
					method = new HeadMethod(sUrl) {
						@Override
						public String getName() {
							return sCustomHttpVerb;
						}
					};
					break;
				case OPTIONS:
					method = new OptionsMethod(sUrl) {
						@Override
						public String getName() {
							return sCustomHttpVerb;
						}
					};
					break;
				case TRACE:
					method = new TraceMethod(sUrl) {
						@Override
						public String getName() {
							return sCustomHttpVerb;
						}
					};
					break;
				}
			} else {
				Engine.logBeans.debug("(HttpConnector) HTTP verb: " + sHttpVerb);
				
				switch (httpVerb) {
				case GET: method = new GetMethod(sUrl); break;
				case POST: method = new PostMethod(sUrl); break;
				case PUT: method = new PutMethod(sUrl); break;
				case DELETE: method = new DeleteMethod(sUrl); break;
				case HEAD: method = new HeadMethod(sUrl); break;
				case OPTIONS: method = new OptionsMethod(sUrl); break;
				case TRACE: method = new TraceMethod(sUrl); break;
				}
			}
			
			// Setting HTTP parameters
			boolean hasUserAgent = false;

			for (List<String> httpParameter : httpParameters) {
				String key = httpParameter.get(0);
				String value = httpParameter.get(1);
				if (key.equalsIgnoreCase("host") && !value.equals(host)) {
					value = host;
				}

				if (!key.startsWith(DYNAMIC_HEADER_PREFIX)) {
					method.setRequestHeader(key, value);
				}
				if (HeaderName.UserAgent.is(key)) {
					hasUserAgent = true;
				}
			}

			// set user-agent header if not found
			if (!hasUserAgent) {
				HeaderName.UserAgent.setRequestHeader(method, getUserAgent(context));
			}

			// Setting POST or PUT parameters if any
			Engine.logBeans.debug("(HttpConnector) Setting " + httpVerb + " data");
			if (method instanceof EntityEnclosingMethod) {
				EntityEnclosingMethod entityEnclosingMethod = (EntityEnclosingMethod) method;
				AbstractHttpTransaction transaction = (AbstractHttpTransaction) context.requestedObject;
				
				if (doMultipartFormData) {
					RequestableHttpVariable body = (RequestableHttpVariable) httpTransaction.getVariable(Parameter.HttpBody.getName());
					if (body != null && body.getDoFileUploadMode() == DoFileUploadMode.multipartFormData) {
						String stringValue = httpTransaction.getParameterStringValue(Parameter.HttpBody.getName());
						String filepath = Engine.theApp.filePropertyManager.getFilepathFromProperty(stringValue, getProject().getName());
						File file = new File(filepath);
						if (file.exists()) {
							HeaderName.ContentType.setRequestHeader(method, contentType);
							entityEnclosingMethod.setRequestEntity(new FileRequestEntity(file, contentType));
						} else {
							throw new FileNotFoundException(file.getAbsolutePath());
						}
					} else {
						List<Part> parts = new LinkedList<Part>();
						
						for (RequestableVariable variable : transaction.getVariablesList()) {
							if (variable instanceof RequestableHttpVariable) {
								RequestableHttpVariable httpVariable = (RequestableHttpVariable) variable;
								
								if ("POST".equals(httpVariable.getHttpMethod())) {
									Object httpObjectVariableValue = transaction.getVariableValue(httpVariable.getName());
									
									if (httpVariable.isMultiValued()) {
										if (httpObjectVariableValue instanceof Collection<?>) {
											for (Object httpVariableValue : (Collection<?>) httpObjectVariableValue) {
												addFormDataPart(parts, httpVariable, httpVariableValue);
											}
										}
									} else {
										addFormDataPart(parts, httpVariable, httpObjectVariableValue);
									}
								}
							}
						}
						MultipartRequestEntity mre = new MultipartRequestEntity(parts.toArray(new Part[parts.size()]), entityEnclosingMethod.getParams());
						HeaderName.ContentType.setRequestHeader(method, mre.getContentType());
						entityEnclosingMethod.setRequestEntity(mre);
					}
				} else if (MimeType.TextXml.is(contentType)) {
					final MimeMultipart[] mp = {null};
					
					for (RequestableVariable variable : transaction.getVariablesList()) {
						if (variable instanceof RequestableHttpVariable) {
							RequestableHttpVariable httpVariable = (RequestableHttpVariable) variable;
							
							if (httpVariable.getDoFileUploadMode() == DoFileUploadMode.MTOM) {
								Engine.logBeans.trace("(HttpConnector) Variable " + httpVariable.getName() + " detected as MTOM");
								
								MimeMultipart mimeMultipart = mp[0];
								try {
									if (mimeMultipart == null) {
										Engine.logBeans.debug("(HttpConnector) Preparing the MTOM request");
										
										mimeMultipart = new MimeMultipart("related; type=\"application/xop+xml\"");
										MimeBodyPart bp = new MimeBodyPart();
										bp.setText(postQuery, "UTF-8");
										bp.setHeader(HeaderName.ContentType.value(), contentType);
										mimeMultipart.addBodyPart(bp);
									}
									
									Object httpObjectVariableValue = transaction.getVariableValue(httpVariable.getName());
									
									if (httpVariable.isMultiValued()) {
										if (httpObjectVariableValue instanceof Collection<?>) {
											for (Object httpVariableValue : (Collection<?>) httpObjectVariableValue) {
												addMtomPart(mimeMultipart, httpVariable, httpVariableValue);
											}
										}
									} else {
										addMtomPart(mimeMultipart, httpVariable, httpObjectVariableValue);
									}
									mp[0] = mimeMultipart;
								} catch (Exception e) {
									Engine.logBeans.warn("(HttpConnector) Failed to add MTOM part for " + httpVariable.getName(), e);
								}
							}
						}
					}
					
					if (mp[0] == null) {
						entityEnclosingMethod.setRequestEntity(new StringRequestEntity(postQuery, MimeType.TextXml.value(), "UTF-8"));
					} else {
						Engine.logBeans.debug("(HttpConnector) Commit the MTOM request with the ContentType: " + mp[0].getContentType());
						
						HeaderName.ContentType.setRequestHeader(method, mp[0].getContentType());
						entityEnclosingMethod.setRequestEntity(new RequestEntity() {
							
							@Override
							public void writeRequest(OutputStream outputStream) throws IOException {
								try {
									mp[0].writeTo(outputStream);
								} catch (MessagingException e) {
									new IOException(e);
								}
							}
							
							@Override
							public boolean isRepeatable() {
								return true;
							}
							
							@Override
							public String getContentType() {
								return mp[0].getContentType();
							}
							
							@Override
							public long getContentLength() {
								return -1;
							}
						});
					}
				} else {
					String charset = httpTransaction.getComputedUrlEncodingCharset();
					HeaderName.ContentType.setRequestHeader(method, contentType);
					entityEnclosingMethod.setRequestEntity(new StringRequestEntity(postQuery, contentType, charset));
				}
			}

			// Getting the result
			Engine.logBeans.debug("(HttpConnector) HttpClient: getting response body");
			byte[] result = executeMethod(method, context);
			Engine.logBeans.debug("(HttpConnector) Total read bytes: "
					+ ((result != null) ? result.length : 0));

			StringBuilder sb = new StringBuilder();
			sb.append("HTTP result {ContentType: " + context.contentType + ", Length: " + (result != null ? result.length : 0) + "}\n\n");
			
			if (result != null && context.contentType != null
					&& ( context.contentType.startsWith("text/")
					|| context.contentType.startsWith("application/xml")
					|| context.contentType.startsWith("application/json"))) {
				sb.append(new String(result, "UTF-8"));
			}

			fireDataChanged(new ConnectorEvent(this, sb.toString()));

			return result;
		} finally {
			// Fire event for plugins
			long t1 = System.currentTimeMillis();
			Engine.theApp.pluginsManager.fireHttpConnectorGetDataEnd(context, t0, t1);
			
			if (method != null)
				method.releaseConnection();
		}
	}
	
	protected String getUserAgent(Context context) throws ConnectionException {
		return "Mozilla/5.0 ConvertigoEMS/" + Version.fullProductVersion;
	}

	transient private EventListenerList httpStateListeners = new EventListenerList();

	public void addHttpStateListener(HttpStateListener httpStateListener) {
		httpStateListeners.add(HttpStateListener.class, httpStateListener);
	}

	public void removeHttpStateListener(HttpStateListener httpStateListener) {
		httpStateListeners.remove(HttpStateListener.class, httpStateListener);
	}

	public void fireStateChanged(HttpStateEvent httpStateEvent) {
		// Guaranteed to return a non-null array
		Object[] listeners = httpStateListeners.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == HttpStateListener.class) {
				try {
					((HttpStateListener) listeners[i + 1]).stateChanged(httpStateEvent);
				} catch (Exception e) {
					;
				}
			}
		}
	}

	private byte[] executeMethod(HttpMethod method, final Context context) throws IOException, URIException,
			MalformedURLException, EngineException {
		Header[] requestHeaders, responseHeaders = null;
		byte[] result = null;
		String contents = null;
		int statuscode = -1;
		final String referer = this.referer;
		
		if (!context.requestedObject.runningThread.bContinue)
			return null;

		Engine.logBeans.debug("(HttpConnector) Executing method - " + method.getName() + "("
				+ method.getPath() + ")");

		try {
			AbstractHttpTransaction transaction = (AbstractHttpTransaction) context.requestedObject;
			
			requestHeaders = method.getRequestHeaders();
			if (Engine.logBeans.isTraceEnabled())
				Engine.logBeans.trace("(HttpConnector) Request headers :\n"
						+ Arrays.asList(requestHeaders).toString());
			
			statuscode = doExecuteMethod(method, context);

			Engine.logBeans.debug("(HttpConnector) Status: " + method.getStatusLine().toString());

			responseHeaders = method.getResponseHeaders();
			context.setResponseHeaders(responseHeaders);
			if (Engine.logBeans.isTraceEnabled())
				Engine.logBeans.trace("(HttpConnector) Response headers:\n"
						+ Arrays.asList(responseHeaders).toString());

			if (statuscode != -1) {
				InputStream in = method.getResponseBodyAsStream();
				if (in != null) {

					/**
					 * Retrieve response charset if available in responseHeaders
					 */
					charset = null;
					boolean checkGZip = false; // add GZip support #320

					for (int i = 0; i < responseHeaders.length && (charset == null || !checkGZip); i++) {
						Header head = responseHeaders[i];
						if (HeaderName.ContentType.is(head)) {
							context.contentType = head.getValue();
							HeaderElement[] els = head.getElements();
							for (int j = 0; j < els.length && charset == null; j++) {
								NameValuePair nvp = els[j].getParameterByName("charset");
								if (nvp != null)
									charset = nvp.getValue();
							}
						} else if (HeaderName.ContentEncoding.is(head)) {
							checkGZip = true;
							HeaderElement[] els = head.getElements();
							for (int j = 0; j < els.length; j++)
								if ("gzip".equals(els[j].getName())) {
									Engine.logBeans.debug("(HttpConnector) Decode GZip stream");
									in = new GZIPInputStream(in);
								}
						}
					}
					
					if (context.contentType != null && context.contentType.startsWith("multipart/") && context.requestedObject instanceof AbstractHttpTransaction) {
						Engine.logBeans.debug("(HttpConnector) Decoding multipart contentType");
						
						try {														
							BigMimeMultipart mp = new BigMimeMultipart(new BufferedInputStream(in), context.contentType);
							
							ByteArrayOutputStream bos = new ByteArrayOutputStream();
							mp.nextPart(bos);
							result = bos.toByteArray();
							
							if (transaction.getAllowDownloadAttachment()) {
								Document doc = context.outputDocument;
								Element attInfo = null;
								
								File file = File.createTempFile("c8o_", ".part");
								
								for (MimePart bp = mp.nextPart(file); bp != null; bp = mp.nextPart(file)) {
									try {
										file.deleteOnExit();
										
										if (attInfo == null) {
											Engine.logBeans.debug("(HttpConnector) Saving attachment(s)");
											
											attInfo = doc.createElement("AttachmentInfo");
											doc.getDocumentElement().appendChild(attInfo);
										}
										
										Element att = doc.createElement("attachment");
										attInfo.appendChild(att);

										String cid = bp.getContentID();
										
										if (cid != null) {
											cid = cid.replaceFirst("^<?(.*?)>?$", "$1");
											att.setAttribute("cid", "cid:" + cid);
										}
										
										Engine.logBeans.debug("(HttpConnector) Saving the attachment cid: " + cid + " in file: " + file.getAbsolutePath());

										att.setAttribute("filepath", file.getAbsolutePath());

										Enumeration<javax.mail.Header> headers = GenericUtils.cast(bp.getAllHeaders());
										while (headers.hasMoreElements()) {
											javax.mail.Header header = headers.nextElement();
											Element eHeader = doc.createElement("header");
											att.appendChild(eHeader);

											eHeader.setAttribute("name", header.getName());
											eHeader.setAttribute("value", header.getValue());
										}
									} catch (Exception e1) {
										Engine.logBeans.error("(HttpConnector) Failed to retrieve the attachment in " + file.getAbsolutePath(), e1);
									}
									
									file = File.createTempFile("c8o_", ".part");
								}
								
								file.delete();
								in.close();
							}
						} catch (Exception e) {
							Engine.logBeans.error("(HttpConnector) Failed to retrieve attachments", e);
						}
					} else {
						result = IOUtils.toByteArray(in);
						in.close();						
					}
				}

				if (Engine.logBeans.isTraceEnabled()) {
					contents = new String((result != null) ? result : new byte[] {});
					Engine.logBeans.trace("(HttpConnector) Response content:\n" + contents);
				}

				String redirectUrl, newuri;

				// Handles REDIRECTION through Location header
				if (transaction.isFollowRedirect() &&
						(  statuscode == HttpStatus.SC_MOVED_TEMPORARILY
						|| statuscode == HttpStatus.SC_MOVED_PERMANENTLY
						|| statuscode == HttpStatus.SC_SEE_OTHER
						|| statuscode == HttpStatus.SC_TEMPORARY_REDIRECT)) {
					Header location = method.getResponseHeader("Location");
					if (location != null) {
						newuri = location.getValue();
						if ((newuri == null) || (newuri.equals(""))) {
							newuri = "/";
						}

						// ignore any data after the ";" character
						int split = newuri.indexOf(';');
						if (split != -1) {
							newuri = newuri.substring(0, split);
						}

						redirectUrl = getAbsoluteUrl(method, newuri);
						Engine.logBeans.debug("(HttpConnector) Redirecting to : " + redirectUrl);
						result = getData(context, redirectUrl);
					} else {
						Engine.logBeans.debug("(HttpConnector) Invalid redirect!");
					}
				}
			}
			//Added by julienda - #3433 - 04/03/2013
			AbstractHttpTransaction abstractHttpTransaction = (AbstractHttpTransaction) context.transaction;
			
			if (abstractHttpTransaction.getHttpInfo()) {
				Document doc = context.outputDocument;
				
				//Parent Element
				httpInfoElement = doc.createElement(abstractHttpTransaction.getHttpInfoTagName());
				
				//Add requested URL
				Element urlElement = doc.createElement("url");
				urlElement.setTextContent(referer);
				httpInfoElement.appendChild(urlElement);

				//Add status code
				Element httpStatusElement = doc.createElement("status");
					
				httpStatusElement.setAttribute("code", Integer.toString(statuscode));
				httpStatusElement.setAttribute("text", method.getStatusText());
				httpInfoElement.appendChild(httpStatusElement);

				//We add headers informations

				List<Header> headers = Arrays.asList(requestHeaders);
				if (!headers.isEmpty()) {
					Element httpHeadersElement = doc.createElement("headers");

					for (int i = 0; i < headers.size(); i++){
						Element elt = doc.createElement("header");
						elt.setAttribute("name", headers.get(i).getName());
						elt.setAttribute("value", headers.get(i).getValue());
						httpHeadersElement.appendChild(elt);
					}				
					httpInfoElement.appendChild(httpHeadersElement);
				}
				
				// we add response header information
				if (responseHeaders.length != 0) {
					Element httpHeadersElement = doc.createElement("responseHeaders");

					for (int i = 0; i < responseHeaders.length; i++){
						Element elt = doc.createElement("header");
						elt.setAttribute("name", responseHeaders[i].getName());
						elt.setAttribute("value", responseHeaders[i].getValue());
						httpHeadersElement.appendChild(elt);
					}				
					httpInfoElement.appendChild(httpHeadersElement);
				}	
				
				doc.getDocumentElement().appendChild(httpInfoElement);
			}
		} finally {
			method.releaseConnection();
		}

		return result;
	}

	protected int doExecuteMethod(final HttpMethod method, Context context) throws ConnectionException, URIException, MalformedURLException {
		int statuscode = -1;

		// Tells the method to automatically handle authentication.
		method.setDoAuthentication(true);

		// Tells the method to automatically handle redirection.
		method.setFollowRedirects(false);
		
		HttpPool httpPool = ((AbstractHttpTransaction) context.transaction).getHttpPool();
		HttpClient httpClient = context.getHttpClient3(httpPool);
		
		try {
			// Display the cookies
			if (handleCookie) {
				Cookie[] cookies = httpState.getCookies();
				if (Engine.logBeans.isTraceEnabled())
					Engine.logBeans.trace("(HttpConnector) HttpClient request cookies:"
							+ Arrays.asList(cookies).toString());
			}

			forwardHeader(new HeaderForwarder() {
				public void add(String name, String value, String forwardPolicy) {
					if (HttpConnector.HTTP_HEADER_FORWARD_POLICY_IGNORE.equals(forwardPolicy)) {
						Header exHeader = method.getRequestHeader(name);
						if (exHeader != null) {
							// Nothing to do
							Engine.logEngine.debug("(WebViewer) Forwarding header '" + name
									+ "' has been ignored due to forward policy");
						}
						else {
							method.setRequestHeader(name, value);
							Engine.logEngine.debug("(WebViewer) Header forwarded and added: " + name
									+ "=" + value);
						}
					} else if (HttpConnector.HTTP_HEADER_FORWARD_POLICY_REPLACE.equals(forwardPolicy)) {
						method.setRequestHeader(name, value);
						Engine.logEngine.debug("(WebViewer) Header forwarded and replaced: " + name
								+ "=" + value);
					} else if (HttpConnector.HTTP_HEADER_FORWARD_POLICY_MERGE.equals(forwardPolicy)) {
						Header exHeader = method.getRequestHeader(name);
						if (exHeader != null)
							value = exHeader.getValue() + ", " + value;

						method.setRequestHeader(name, value);
						Engine.logEngine.debug("(WebViewer) Header forwarded and merged: " + name
								+ "=" + value);
					}
				}
			});

			// handle oAuthSignatures if any
			if (oAuthKey != null && oAuthSecret != null && oAuthToken != null && oAuthTokenSecret != null) {
				oAuthConsumer = new HttpOAuthConsumer(oAuthKey, oAuthSecret, hostConfiguration);
				oAuthConsumer.setTokenWithSecret(oAuthToken, oAuthTokenSecret);
				oAuthConsumer.sign(method);

				oAuthConsumer = null;
			}
			
			HttpUtils.logCurrentHttpConnection(httpClient, hostConfiguration, httpPool);
			
			hostConfiguration.getParams().setIntParameter(HttpConnectionParams.SO_TIMEOUT, (int) context.requestedObject.getResponseTimeout() * 1000);
			hostConfiguration.getParams().setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, (int) context.requestedObject.getResponseTimeout() * 1000);
			
			Engine.logBeans.debug("(HttpConnector) HttpClient: executing method...");
			statuscode = httpClient.executeMethod(hostConfiguration, method, httpState);
			Engine.logBeans.debug("(HttpConnector) HttpClient: end of method successfull");

			// Display the cookies
			if (handleCookie) {
				Cookie[] cookies = httpState.getCookies();
				if (Engine.logBeans.isTraceEnabled())
					Engine.logBeans.trace("(HttpConnector) HttpClient response cookies:"
							+ Arrays.asList(cookies).toString());
			}
		} catch (SocketTimeoutException e) {
			throw new ConnectionException("Timeout reached (" + context.requestedObject.getResponseTimeout() + " sec)");
		} catch (IOException e) {
			if (!context.requestedObject.runningThread.bContinue)
				return statuscode;

			try {
				HttpUtils.logCurrentHttpConnection(httpClient, hostConfiguration, httpPool);
				Engine.logBeans.warn("(HttpConnector) HttpClient: connection error to " + sUrl + ": "
						+ e.getMessage() + "; retrying method");
				statuscode = httpClient.executeMethod(hostConfiguration, method, httpState);
				Engine.logBeans.debug("(HttpConnector) HttpClient: end of method successfull");
			} catch (IOException ee) {
				throw new ConnectionException("Connection error to " + sUrl, ee);
			}
		} catch (OAuthException eee) {
			throw new ConnectionException("OAuth Connection error to " + sUrl, eee);
		}
		return statuscode;
	}

	public void forwardHeader(HeaderForwarder hf) {
		if (!getHttpHeaderForwardMap().isEmpty() && !context.getRequestHeaders().isEmpty()) {
			Map<String, List<String>> requestHeaders = context.getRequestHeaders();
			for (Map.Entry<String, String> entry : getHttpHeaderForwardMap().entrySet())
				if (requestHeaders.containsKey(entry.getKey()))
					// Uppercase the first letter of each word (can be an issue if all letters
					// are in lowercase for some HTTP servers)
					for (String value : requestHeaders.get(entry.getKey())) {
						String[] split = entry.getKey().split("-");
						for (int i = 0; i < split.length; i++)
							split[i] = split[i].substring(0, 1).toUpperCase() + split[i].substring(1);
						hf.add(StringUtils.join(split, "-"), value, entry.getValue());
					}
		}
	}

	private String getAbsoluteUrl(HttpMethod method, String givenUrl) throws URIException,
			MalformedURLException, EngineException {
		String absoluteUrl = givenUrl;
		if (method != null) {
			if (givenUrl != null) {
				// givenUrl is already absolute
				if (givenUrl.startsWith("http")) {
					absoluteUrl = givenUrl;

					URL url = null;
					String host = "";
					int port = -1;
					if (absoluteUrl.toLowerCase().startsWith("https:")) {
						if (!https) {
							Engine.logBeans.debug("(HttpConnector) Setting up SSL properties");
							certificateManager.collectStoreInformation(context);
						}

						url = new URL(absoluteUrl);
						host = url.getHost();
						port = url.getPort();
						if (port == -1)
							port = 443;

						Engine.logBeans.debug("(HttpConnector) Host: " + host + ":" + port);

						Engine.logBeans.debug("(HttpConnector) CertificateManager has changed: "
								+ certificateManager.hasChanged);
						if (certificateManager.hasChanged
								|| (!host.equalsIgnoreCase(hostConfiguration.getHost()))
								|| (hostConfiguration.getPort() != port)) {
							Engine.logBeans
									.debug("(HttpConnector) Using MySSLSocketFactory for creating the SSL socket");
							Protocol myhttps = new Protocol("https", MySSLSocketFactory.getSSLSocketFactory(
											certificateManager.keyStore, certificateManager.keyStorePassword,
											certificateManager.trustStore,
											certificateManager.trustStorePassword,
											this.trustAllServerCertificates), port);

							hostConfiguration.setHost(host, port, myhttps);
						}

						// absoluteUrl = url.getFile();
						Engine.logBeans.debug("(HttpConnector) Updated URL for SSL purposes: " + absoluteUrl);
					} else {
						url = new URL(absoluteUrl);
						host = url.getHost();
						port = url.getPort();

						Engine.logBeans.debug("(HttpConnector) Host: " + host + ":" + port);
						hostConfiguration.setHost(host, port);
					}
				}
				// givenUrl is relative to method uri ones
				else {
					String methodProtocol;
					String methodHost;
					int methodPort;
					
					URI uri = method.getURI();

					if (hostConfiguration.getProtocol().isSecure()) {
						methodProtocol = hostConfiguration.getProtocol().getScheme();
						methodHost = hostConfiguration.getHost();
						methodPort = hostConfiguration.getPort();
					} else {
						methodProtocol = uri.getScheme();
						methodHost = uri.getHost();
						methodPort = uri.getPort();						
					}
/*
					if (hostConfiguration.getProtocol().isSecure()) {
						return givenUrl.startsWith("/") ? givenUrl : ('/' + givenUrl);
					}
*/
					String path = uri.getCurrentHierPath();
					path = ((path.equals("/") ? "" : path));

					absoluteUrl = methodProtocol + "://" + methodHost;
					if (methodPort != -1) {
						absoluteUrl += ":" + methodPort;
					}

					if (!givenUrl.startsWith("/") && (path.length() == 0 || !givenUrl.contains(path + "/"))) {
						absoluteUrl += path + "/" + givenUrl;
					} else {
						absoluteUrl += givenUrl;
					}
				}
			}
		}
		return absoluteUrl;
	}
	
	public String getStringValue(RequestableHttpVariable variable, Object value) {
		String stringValue = ParameterUtils.toString(value);
		
		DoFileUploadMode doFileUploadmode = variable.getDoFileUploadMode();
		if (doFileUploadmode == DoFileUploadMode.base64) {
			try {
				String filepath = Engine.theApp.filePropertyManager.getFilepathFromProperty(stringValue, getProject().getName());
				stringValue = Base64.encodeBase64String(IOUtils.toByteArray(new FileInputStream(filepath)));
			} catch (Exception e) {
				Engine.logBeans.warn("(HttpConnector) Failed to read the file for base64 encoding: " + stringValue, e);
			}
		} else if (doFileUploadmode == DoFileUploadMode.MTOM) {
			stringValue = "<xop:Include xmlns:xop=\"http://www.w3.org/2004/08/xop/include\" href=\"cid:" + variable.getMtomCid(stringValue) + "\"/>";
		}
		
		return stringValue;
	}
	
	private void addMtomPart(MimeMultipart mp, RequestableHttpVariable variable, Object httpVariableValue) throws IOException, MessagingException {
		String stringValue = ParameterUtils.toString(httpVariableValue);
		String filepath = Engine.theApp.filePropertyManager.getFilepathFromProperty(stringValue, getProject().getName());
		String cid = variable.getMtomCid(stringValue);
		
		Engine.logBeans.debug("(HttpConnector) Prepare the MTOM attachment with cid: " + cid + ". Converting the path '" + stringValue + "' to '" + filepath + "'");
		
		MimeBodyPart bp = new MimeBodyPart();
		bp.attachFile(filepath);
		bp.setContentID(cid);
		mp.addBodyPart(bp);
	}
		
	private void addFormDataPart(List<Part> parts, RequestableHttpVariable httpVariable, Object httpVariableValue) throws FileNotFoundException {
		String stringValue = ParameterUtils.toString(httpVariableValue);
		if (httpVariable.getDoFileUploadMode() == DoFileUploadMode.multipartFormData) {
			String filepath = Engine.theApp.filePropertyManager.getFilepathFromProperty(stringValue, getProject().getName());
			File file = new File(filepath);
			if (file.exists()) {
				String charset = httpVariable.getDoFileUploadCharset();
				String type = httpVariable.getDoFileUploadContentType();
				if (org.apache.commons.lang3.StringUtils.isBlank(type)) {
					type = Engine.getServletContext().getMimeType(file.getName());
				}
				parts.add(new FilePart(httpVariable.getHttpName(), file.getName(), file, type, charset));
			} else {
				throw new FileNotFoundException(filepath);
			}
		} else {
			parts.add(new StringPart(httpVariable.getHttpName(), stringValue));
		}
	}
	
	transient private boolean authenticationPropertiesHasChanged = false;
	
	/** Holds value of property trustAllServerCertificates. */
	private boolean trustAllServerCertificates = true;

	/** Holds value of property https. */
	private boolean https = false;

	/** Holds value of property baseDir. */
	private String baseDir = "/";

	/** Holds value of property server. */
	private String server = "";

	/**
	 * Getter for property server.
	 * 
	 * @return Value of property server.
	 */
	public String getServer() {
		return this.server;
	}

	/**
	 * Setter for property server.
	 * 
	 * @param server
	 *            New value of property server.
	 */
	public void setServer(String server) {
		if (!this.server.equals(server)) {
			this.server = server;
			setAuthenticationPropertiesChanged();
		}
	}

	/** Holds value of property authUser. */
	private String authUser = "";

	/**
	 * Getter for property authUser.
	 * 
	 * @return the authUser
	 */
	public String getAuthUser() {
		return authUser;
	}

	/**
	 * Setter for property authUser.
	 * 
	 * @param authUser
	 *            the authUser to set
	 */
	public void setAuthUser(String authUser) {
		if (!this.authUser.equals(authUser)) {
			this.authUser = authUser;
			setAuthenticationPropertiesChanged();
		}
	}

	/** Holds value of property authPassword. */
	private String authPassword = "";

	/**
	 * Getter for property authPassword.
	 * 
	 * @return the authPassword
	 */
	public String getAuthPassword() {
		return authPassword;
	}

	/**
	 * Setter for property authPassword.
	 * 
	 * @param authPassword
	 *            the authPassword to set
	 */
	public void setAuthPassword(String authPassword) {
		if (!this.authPassword.equals(authPassword)) {
			this.authPassword = authPassword;
			setAuthenticationPropertiesChanged();
		}
	}
	
	/**
	 * Holds value of property authenticationType.
	 */
	private AuthenticationMode authenticationType = AuthenticationMode.None;
	
	/**
	 * Getter for property authenticationType.
	 * @return the authenticationType
	 */
	public AuthenticationMode getAuthenticationType() {
		return authenticationType;
	}
	
	/**
	 * Setter for property authenticationType.
	 * @param authenticationType
	 */
	public void setAuthenticationType(AuthenticationMode authenticationType) {
		if (!this.authenticationType.equals(authenticationType)) {
			this.authenticationType = authenticationType;
			setAuthenticationPropertiesChanged();
		}
	}
	
	/**
	 * Holds value of property NTLMAuthenticationDomain.
	 */
	private String NTLMAuthenticationDomain = "";
	
	/**
	 * Getter for property NTLMAuthenticationDomain.
	 * @return the NTLMAuthenticationDomain
	 */
	public String getNTLMAuthenticationDomain() {
		return NTLMAuthenticationDomain;
	}
	
	/**
	 * Setter for property NTLMAuthenticationDomain.
	 * @param NTLMAuthenticationDomain
	 */
	public void setNTLMAuthenticationDomain(String NTLMAuthenticationDomain) {
		if (!this.NTLMAuthenticationDomain.equals(NTLMAuthenticationDomain)) {
			this.NTLMAuthenticationDomain = NTLMAuthenticationDomain;
			setAuthenticationPropertiesChanged();
		}
	}
	
	/** Holds value of givenAuthUser. */
	transient private String givenAuthUser = null;

	public String getGivenAuthUser() {
		return givenAuthUser;
	}

	public void setGivenAuthUser(String givenAuthUser) {
		this.givenAuthUser = givenAuthUser;
	}

	transient private String givenAuthPassword = null;

	public String getGivenAuthPassword() {
		return givenAuthPassword;
	}

	public void setGivenAuthPassword(String givenAuthPassword) {
		this.givenAuthPassword = givenAuthPassword;
	}

	/**
	 * Getter for property https.
	 * 
	 * @return Value of property https.
	 */
	public boolean isHttps() {
		return this.https;
	}

	/**
	 * Setter for property https.
	 * 
	 * @param https
	 *            New value of property https.
	 */
	public void setHttps(boolean https) {
		this.https = https;
	}

	/** Holds value of property port. */
	private int port = 80;

	/**
	 * Getter for property port.
	 * 
	 * @return Value of property port.
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * Setter for property port.
	 * 
	 * @param port
	 *            New value of property port.
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Getter for property baseDir.
	 * 
	 * @return Value of property baseDir.
	 */
	public String getBaseDir() {
		return this.baseDir;
	}

	/**
	 * Setter for property baseDir.
	 * 
	 * @param baseDir
	 *            New value of property baseDir.
	 */
	public void setBaseDir(String baseDir) {
		this.baseDir = baseDir;
	}

	/**
	 * @return the trustAllServerCertificates
	 */
	public boolean isTrustAllServerCertificates() {
		return trustAllServerCertificates;
	}

	/**
	 * @param trustAllServerCertificates
	 *            the trustAllServerCertificates to set
	 */
	public void setTrustAllServerCertificates(boolean trustAllServerCertificates) {
		this.trustAllServerCertificates = trustAllServerCertificates;
	}

	public XMLVector<XMLVector<String>> getHttpHeaderForward() {
		return httpHeaderForward;
	}

	public void setHttpHeaderForward(XMLVector<XMLVector<String>> httpHeaderForward) {
		this.httpHeaderForward = httpHeaderForward;
		httpHeaderForwardMap = null;
	}

	public Map<String, String> getHttpHeaderForwardMap() {
		if (httpHeaderForwardMap == null) {
			httpHeaderForwardMap = new HashMap<String, String>();
			for (XMLVector<String> v : httpHeaderForward)
				if (v.size() > 1)
					httpHeaderForwardMap.put(v.get(0).toLowerCase(), v.get(1));
			httpHeaderForwardMap = Collections.unmodifiableMap(httpHeaderForwardMap);
		}
		return httpHeaderForwardMap;
	}

	@Override
	public AbstractHttpTransaction newTransaction() {
		return new HttpTransaction();
	}
	
	@Override
	public boolean isMaskedProperty(Visibility target, String propertyName) {
		if ("authPassword".equals(propertyName)) {
			return true;
		}
		return super.isMaskedProperty(target, propertyName);
	}

	@Override
	public boolean isCipheredProperty(String propertyName) {
		if ("authPassword".equals(propertyName)) {
			return true;
		}
		return super.isCipheredProperty(propertyName);
	}

	public String getUrlEncodingCharset() {
		return urlEncodingCharset;
	}

	public void setUrlEncodingCharset(String urlEncodingCharset) {
		this.urlEncodingCharset = urlEncodingCharset;
	}
	
	private void setAuthenticationPropertiesChanged() {
		if (!isOriginal()) {
			authenticationPropertiesHasChanged = true;
		}
	}
}