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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.swing.event.EventListenerList;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.OptionsMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.TraceMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
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
import com.twinsoft.convertigo.beans.transactions.XmlHttpTransaction;
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
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.plugins.VicApi;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.URLUtils;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.util.StringEx;

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
	
	public static final String HTTP_HEADER_FORWARD_POLICY_REPLACE = "Replace";
	public static final String HTTP_HEADER_FORWARD_POLICY_IGNORE = "Ignore";
	public static final String HTTP_HEADER_FORWARD_POLICY_MERGE = "Merge";
	transient private Map<String, String> httpHeaderForwardMap = null;

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

		return clonedObject;
	}

	public void setBaseUrl(String httpUrl) {
		if ((httpUrl != null) && (!httpUrl.equals(""))) {
			sUrl = httpUrl;
		} else
			setBaseUrl();
	}

	public void setBaseUrl() {
		sUrl = "http";

		if (https)
			sUrl += "s";

		sUrl += "://" + server;
		if ((https && (port != 443)) || (!https && (port != 80)))
			sUrl += ":" + port;
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
		if (!handleCookie && httpState != null)
			httpState.clearCookies(); // remove cookies from previous
										// transaction

		// Looks for content-type
		boolean isFormUrlEncoded = true;

		httpParameters = httpTransaction.getHttpParameters();

		for (List<String> httpParameter : httpTransaction.getHttpParameters()) {
			String key = httpParameter.get(0);
			String value = httpParameter.get(1);
			if (key.equalsIgnoreCase("Content-Type")) {
				if (!value.equalsIgnoreCase("application/x-www-form-urlencoded")) {
					isFormUrlEncoded = false;
					break;
				}
			}
		}

		// Retrieve request template file if necessary
		File requestTemplateFile = null;
		if (!isFormUrlEncoded) {
			String requestTemplateUrl = httpTransaction.getRequestTemplate();
			if (!requestTemplateUrl.equals("")) {
				String projectDirectoryName = context.project.getName();
				String absoluteRequestTemplateUrl = Engine.PROJECTS_PATH + "/" + projectDirectoryName + "/"
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
		String httpUrl = (String) getVariableValue(httpTransaction, Parameter.ConnectorConnectionString.getName());
		if (httpUrl != null)
			setBaseUrl(httpUrl);
		else
			setBaseUrl();

		String transactionBaseDir = httpTransaction.getSubDir();
		if (transactionBaseDir.startsWith("http")) {
			sUrl = transactionBaseDir;
			/*
			 * if (transactionBaseDir.startsWith("https")) setHttps(true);
			 */
		} else
			sUrl += transactionBaseDir;

		// Setup the SSL properties if needed
		if (https) {
			Engine.logBeans.debug("(HttpConnector) Setting up SSL properties");
			certificateManager.collectStoreInformation(context);
		}

		// Getting all input variables marked as GET
		Engine.logBeans.trace("(HttpConnector) Loading all GET input variables");
		String variable, method, httpVariable, queryString = "";
		Object httpObjectVariableValue;
		boolean isMultiValued = false;
		boolean bIgnoreVariable = false;
		
		// int len = httpTransaction.getVariablesDefinitionSize();
		int len = httpTransaction.numberOfVariables();

		for (int i = 0; i < len; i++) {
			bIgnoreVariable = false;
			RequestableHttpVariable rVariable = (RequestableHttpVariable) httpTransaction.getVariable(i);
			variable = rVariable.getName();
			isMultiValued = rVariable.isMultiValued();
			method = rVariable.getHttpMethod();
			httpVariable = rVariable.getHttpName();

			// do not add variable to query if emty name
			if (httpVariable.equals(""))
				bIgnoreVariable = true;

			Engine.logBeans.trace("(HttpConnector) Variable: " + variable + " => (" + method + ") "
					+ httpVariable);
			if (method.equals("GET")) {

				// Retrieves variable value
				httpObjectVariableValue = getVariableValue(httpTransaction, variable);
				if (httpObjectVariableValue != null) {
					// variable must be sent as an HTTP parameter
					if (!bIgnoreVariable) {
						try {
							// handle multivalued variable
							if (isMultiValued) {
								if (httpObjectVariableValue instanceof Collection<?>)
									for (Object httpVariableValue : (Collection<?>) httpObjectVariableValue) {
										queryString += ((queryString.length() != 0) ? "&" : "");
										queryString += httpVariable
												+ "="
												+ URLEncoder.encode(httpVariableValue.toString(),
														getRequestEncoding(httpTransaction));
									}
								else if (httpObjectVariableValue.getClass().isArray())
									for (Object item : (Object[]) httpObjectVariableValue) {
										queryString += ((queryString.length() != 0) ? "&" : "");
										queryString += httpVariable
												+ "="
												+ URLEncoder.encode(item.toString(),
														getRequestEncoding(httpTransaction));
									}
							}
							// standard case
							else {
								queryString += ((queryString.length() != 0) ? "&" : "");
								queryString += httpVariable
										+ "="
										+ URLEncoder.encode(httpObjectVariableValue.toString(),
												getRequestEncoding(httpTransaction));
							}
						} catch (UnsupportedEncodingException e) {
							throw new EngineException("UTF-8 encoding is not supported.", e);
						}
					}
				}
			}
		}

		// encodes URL if it contains special characters
		sUrl = URLUtils.encodeAbsoluteURL(sUrl);

		if (queryString.length() != 0) {
			if (sUrl.indexOf('?') == -1) {
				sUrl += "?" + queryString;
			} else {
				sUrl += "&" + queryString;
			}
		}

		Engine.logBeans.debug("(HttpConnector) URL: " + sUrl);

		if (Engine.logBeans.isDebugEnabled())
			Engine.logBeans.debug("(HttpConnector) GET query: " + Visibility.Logs.replaceVariables(httpTransaction.getVariablesList(), queryString));

		// Posting all input variables marked as POST
		Engine.logBeans.trace("(HttpConnector) Posting all POST input variables");
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

							httpObjectVariableValue = getVariableValue(httpTransaction, variable);
							if (httpObjectVariableValue != null) {
								if (isMultiValued) {
									variableElement.setAttribute("multi", "true");
									if (httpObjectVariableValue instanceof Collection<?>) {
										for (Object httpVariableValue : (Collection<?>) httpObjectVariableValue) {
											Element valueElement = variablesDocument.createElement("value");
											variableElement.appendChild(valueElement);
											Text valueText = variablesDocument
													.createTextNode(httpVariableValue.toString());
											valueElement.appendChild(valueText);
										}
									} else if (httpObjectVariableValue.getClass().isArray()) {
										for (Object httpVariableValue : (Object[]) httpObjectVariableValue) {
											Element valueElement = variablesDocument.createElement("value");
											variableElement.appendChild(valueElement);
											Text valueText = variablesDocument
													.createTextNode(httpVariableValue.toString());
											valueElement.appendChild(valueText);
										}
									}
								} else {
									Element valueElement = variablesDocument.createElement("value");
									variableElement.appendChild(valueElement);
									Text valueText = variablesDocument.createTextNode(httpObjectVariableValue
											.toString());
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
						xmlEncoding = (xmlEncoding == null) ? "UTF-8":xmlEncoding;
						
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
			httpObjectVariableValue = getVariableValue(httpTransaction, variable);
			if (method.equals("POST")) {
				// variable must be sent as an HTTP parameter
				if (!bIgnoreVariable) {
					// Content-Type is 'application/x-www-form-urlencoded'
					if (isFormUrlEncoded) {
						// Replace variable value in postQuery
						if (httpObjectVariableValue != null) {
							// handle multivalued variable
							if (isMultiValued) {
								if (httpObjectVariableValue instanceof Collection<?>)
									for (Object httpVariableValue : (Collection<?>) httpObjectVariableValue) {
										postQuery += ((postQuery.length() != 0) ? "&" : "");
										postQuery += httpVariable + "=" + httpVariableValue;
									}
								else if (httpObjectVariableValue.getClass().isArray())
									for (Object httpVariableValue : (Object[]) httpObjectVariableValue) {
										postQuery += ((postQuery.length() != 0) ? "&" : "");
										postQuery += httpVariable + "=" + httpVariableValue.toString();
									}
							}
							// standard case
							else {
								postQuery += ((postQuery.length() != 0) ? "&" : "");
								postQuery += httpVariable + "=" + httpObjectVariableValue.toString();
							}
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
												httpVariableValue += var.toString();
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
												if (isLogHidden) logHiddenValues.add(httpVariableValue.toString());
												tmpPostQuery += (postQuery.substring(beginTagIndex,
														varPatternIndex)
														+ httpVariableValue.toString() + postQuery.substring(
														indexAfterPattern, endTagIndex + 1));
											}
											tmpPostQuery += postQuery.substring(endTagIndex + 1);
											postQuery = tmpPostQuery;
										}
									}
								} else if (httpObjectVariableValue.getClass().isArray()) {
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
											for (Object item : (Object[]) httpObjectVariableValue)
												httpVariableValue += item.toString();
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
											for (Object item : (Object[]) httpObjectVariableValue) {
												if (isLogHidden) logHiddenValues.add(item.toString());
												tmpPostQuery += (postQuery.substring(beginTagIndex,
														varPatternIndex)
														+ item.toString() + postQuery.substring(
														indexAfterPattern, endTagIndex + 1));
											}
											tmpPostQuery += postQuery.substring(endTagIndex + 1);
											postQuery = tmpPostQuery;
										}
									}
								} else if (httpObjectVariableValue instanceof String) {
									if (isLogHidden) logHiddenValues.add(httpObjectVariableValue.toString());
									StringEx sx = new StringEx(postQuery);
									sx.replaceAll("$(" + httpVariable + ")concat", httpObjectVariableValue
											.toString());
									postQuery = sx.toString();
								}
							}
							// Handle single valued variable
							else {
								if (httpObjectVariableValue instanceof String) {
									if (isLogHidden) logHiddenValues.add(httpObjectVariableValue.toString());
									StringEx sx = new StringEx(postQuery);
									sx.replaceAll("$(" + httpVariable + ")noE", httpObjectVariableValue
											.toString());
									sx.replaceAll("$(" + httpVariable + ")", httpObjectVariableValue
											.toString());
									postQuery = sx.toString();
								}
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
					if (httpObjectVariableValue instanceof String) {
						if (!isFormUrlEncoded && (!(httpVariable.equals("")))) {// used
																				// to
																				// replace
																				// empty
																				// element
							if (isLogHidden) logHiddenValues.add(httpObjectVariableValue.toString());
							StringEx sx = new StringEx(postQuery);
							sx.replaceAll(httpVariable, httpObjectVariableValue.toString());
							postQuery = sx.toString();
						}
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

	protected Object getVariableValue(AbstractHttpTransaction httpTransaction, String variableName) {
		Object variableValue = null;
		
		int variableVisibility = httpTransaction.getVariableVisibility(variableName);
		
		// Transaction parameter
		variableValue = httpTransaction.variables.get(variableName);
		if (variableValue != null)
			Engine.logBeans.trace("(HttpConnector) parameter value: " + Visibility.Logs.printValue(variableVisibility,variableValue));

		// Otherwise context parameter
		if (variableValue == null) {
			variableValue = (context.get(variableName) == null ? null : context.get(variableName));
			if (variableValue != null)
				Engine.logBeans.trace("(HttpConnector) context value: " + Visibility.Logs.printValue(variableVisibility,variableValue));
		}

		// Otherwise default transaction parameter value
		if (variableValue == null) {
			variableValue = httpTransaction.getVariableValue(variableName);
			if (variableValue != null)
				Engine.logBeans.trace("(HttpConnector) default value: " + Visibility.Logs.printValue(variableVisibility,variableValue));
		}

		if (variableValue == null)
			Engine.logBeans.trace("(HttpConnector) none value found");

		return variableValue;
	}

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
		if (context.httpState == null) {
			Engine.logBeans
					.debug("(HttpConnector) Creating new HttpState for context id " + context.contextID);
			httpState = new HttpState();

			// Basic authentication configuration
			String realm = null;
			if (!basicUser.equals("") || !basicPassword.equals("") || (givenBasicUser != null) || (givenBasicPassword != null)) {
				String userName = ((givenBasicUser == null) ? basicUser : givenBasicUser);
				String userPassword = ((givenBasicPassword == null) ? basicPassword : givenBasicPassword);
				// httpState.setCredentials(realm, server, new
				// UsernamePasswordCredentials(userName, userPassword));
				httpState.setCredentials(new AuthScope(server, AuthScope.ANY_PORT, AuthScope.ANY_REALM),
						new UsernamePasswordCredentials(userName, userPassword));
				Engine.logBeans.debug("(HttpConnector) Credentials: " + userName + ": ******");
			}

			context.httpState = httpState;
			fireStateChanged(new HttpStateEvent(this, context, realm, server, httpState));
		} else {
			Engine.logBeans.debug("(HttpConnector) Using HttpState of context id " + context.contextID);
			httpState = context.httpState;
		}
	}

	public void resetHttpState(Context context) {
		context.httpState = null;
		getHttpState(context);
	}

	public byte[] getData(Context context) throws IOException, EngineException {
		HttpMethod method = null;

		try {
			// Retrieving httpState
			getHttpState(context);

			Engine.logBeans.trace("(HttpConnector) Retrieving data as a bytes array...");
			Engine.logBeans.debug("(HttpConnector) Connecting to: " + sUrl);

			// Setting the referer
			referer = sUrl;
			
			URL url = null;
			url = new URL(sUrl);
			
			// Proxy configuration
			Engine.theApp.proxyManager.setProxy(hostConfiguration, httpState, url);

			Engine.logBeans.debug("(HttpConnector) Https: " + https);

			String host = "";
			int port = -1;
			if (sUrl.toLowerCase().startsWith("https:")) {
				if (!https) {
					Engine.logBeans.debug("(HttpConnector) Setting up SSL properties");
					certificateManager.collectStoreInformation(context);
				}

				url = new URL(sUrl);
				host = url.getHost();
				port = url.getPort();
				if (port == -1)
					port = 443;

				Engine.logBeans.debug("(HttpConnector) Host: " + host + ":" + port);

				Engine.logBeans.debug("(HttpConnector) CertificateManager has changed: "
						+ certificateManager.hasChanged);
				if (certificateManager.hasChanged || (!host.equalsIgnoreCase(hostConfiguration.getHost()))
						|| (hostConfiguration.getPort() != port)) {
					Engine.logBeans
							.debug("(HttpConnector) Using MySSLSocketFactory for creating the SSL socket");
					Protocol myhttps = new Protocol("https", (ProtocolSocketFactory) new MySSLSocketFactory(
							certificateManager.keyStore, certificateManager.keyStorePassword,
							certificateManager.trustStore, certificateManager.trustStorePassword,
							this.trustAllServerCertificates), port);

					hostConfiguration.setHost(host, port, myhttps);
				}

				sUrl = url.getFile();
				Engine.logBeans.debug("(HttpConnector) Updated URL for SSL purposes: " + sUrl);
			} else {
				url = new URL(sUrl);
				host = url.getHost();
				port = url.getPort();

				Engine.logBeans.debug("(HttpConnector) Host: " + host + ":" + port);
				hostConfiguration.setHost(host, port);
			}

			// Retrieve HTTP method
			int httpVerb = ((AbstractHttpTransaction) context.transaction).getHttpVerb();
			String sHttpVerb = AbstractHttpTransaction.HTTP_VERBS[httpVerb];
			Engine.logBeans.debug("(HttpConnector) HTTP verb: " + sHttpVerb);
			if (httpVerb == AbstractHttpTransaction.HTTP_VERB_GET) {
				method = new GetMethod(sUrl);
			} else if (httpVerb == AbstractHttpTransaction.HTTP_VERB_POST) {
				method = new PostMethod(sUrl);
			} else if (httpVerb == AbstractHttpTransaction.HTTP_VERB_PUT) {
				method = new PutMethod(sUrl);
			} else if (httpVerb == AbstractHttpTransaction.HTTP_VERB_DELETE) {
				method = new DeleteMethod(sUrl);
			} else if (httpVerb == AbstractHttpTransaction.HTTP_VERB_HEAD) {
				method = new HeadMethod(sUrl);
			} else if (httpVerb == AbstractHttpTransaction.HTTP_VERB_OPTIONS) {
				method = new OptionsMethod(sUrl);
			} else if (httpVerb == AbstractHttpTransaction.HTTP_VERB_TRACE) {
				method = new TraceMethod(sUrl);
			}

			// Setting HTTP parameters
			boolean hasUserAgent = false;
			String content_type = "application/x-www-form-urlencoded";

			for (List<String> httpParameter : httpParameters) {
				String key = httpParameter.get(0);
				String value = httpParameter.get(1);
				if (key.equalsIgnoreCase("host") && !value.equals(host))
					value = host;

				if (!key.startsWith(DYNAMIC_HEADER_PREFIX))
					method.setRequestHeader(key, value);
				if (key.equalsIgnoreCase("User-Agent"))
					hasUserAgent = true;
				if (key.equalsIgnoreCase("Content-Type"))
					content_type = value;
			}

			// set user-agent header if not found
			if (!hasUserAgent)
				method.setRequestHeader("User-Agent", getUserAgent(context));

			// Setting POST parameters if any
			Engine.logBeans.debug("(HttpConnector) Setting post data");
			if (httpVerb == AbstractHttpTransaction.HTTP_VERB_POST) {
				PostMethod postMethod = (PostMethod) method;
				if (content_type.equalsIgnoreCase("text/xml")) {
					postMethod.setRequestEntity(new StringRequestEntity(postQuery, "text/xml", "UTF-8"));
				}
				else {
					String charset = getCharset();
					if (charset == null) charset = "UTF-8";
					postMethod.setRequestEntity(new StringRequestEntity(postQuery, content_type, charset));
				}
			}

			// Getting the result
			Engine.logBeans.debug("(HttpConnector) HttpClient: getting response body");
			byte[] result = executeMethod(method, context);
			Engine.logBeans.debug("(HttpConnector) Total read bytes: "
					+ ((result != null) ? result.length : 0));

			fireDataChanged(new ConnectorEvent(this, result));

			return result;
		} finally {
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

	private byte[] executeMethod(HttpMethod method, Context context) throws IOException, URIException,
			MalformedURLException, EngineException {
		Header[] requestHeaders, responseHeaders = null;
		byte[] result = null;
		String contents = null;
		int statuscode = -1;

		if (!context.requestedObject.runningThread.bContinue)
			return null;

		Engine.logBeans.debug("(HttpConnector) Executing method - " + method.getName() + "("
				+ method.getPath() + ")");

		try {
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
						if (head.getName().equalsIgnoreCase("Content-Type")) {
							context.contentType = head.getValue();
							HeaderElement[] els = head.getElements();
							for (int j = 0; j < els.length && charset == null; j++) {
								NameValuePair nvp = els[j].getParameterByName("charset");
								if (nvp != null)
									charset = nvp.getValue();
							}
						} else if (head.getName().equalsIgnoreCase("Content-Encoding")) {
							checkGZip = true;
							HeaderElement[] els = head.getElements();
							for (int j = 0; j < els.length; j++)
								if ("gzip".equals(els[j].getName())) {
									Engine.logBeans.debug("(HttpConnector) Decode GZip stream");
									in = new GZIPInputStream(in);
								}
						}
					}

					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					byte[] buf = new byte[1024];
					int len;
					while ((len = in.read(buf)) > 0) {
						bos.write(buf, 0, len);
					}
					result = bos.toByteArray();
					in.close();
					bos.close();
				}

				if (Engine.logBeans.isTraceEnabled()) {
					contents = new String((result != null) ? result : new byte[] {});
					Engine.logBeans.trace("(HttpConnector) Response content:\n" + contents);
				}

				String redirectUrl, newuri;
				GetMethod redirectMethod = null;

				// Handles REDIRECTION through Location header
				if ((statuscode == HttpStatus.SC_MOVED_TEMPORARILY)
						|| (statuscode == HttpStatus.SC_MOVED_PERMANENTLY)
						|| (statuscode == HttpStatus.SC_SEE_OTHER)
						|| (statuscode == HttpStatus.SC_TEMPORARY_REDIRECT)) {

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
						redirectMethod = new GetMethod(redirectUrl);

						// set headers
						for (int i = 0; i < requestHeaders.length; i++)
							redirectMethod.setRequestHeader(requestHeaders[i]);

						referer = redirectUrl.startsWith("http") ? redirectUrl : (hostConfiguration
								.getHostURL() + redirectUrl);

						result = executeMethod(redirectMethod, context); // recurse
					} else {
						Engine.logBeans.debug("(HttpConnector) Invalid redirect!");
					}
				} else {
					/*
					 * String lwContents = contents.toLowerCase(); int index, i,
					 * j, k, z; // Handles REDIRECTION through META Refresh if
					 * (((index = lwContents.indexOf("http-equiv='refresh'")) !=
					 * -1) || ((index =
					 * lwContents.indexOf("http-equiv=\"refresh\"")) != -1)) {
					 * if ((i = lwContents.indexOf("content=", index + 20)) !=
					 * -1) { char c = lwContents.charAt(i+8); if ((j =
					 * lwContents.indexOf("url=", i)) != -1) { if ((k =
					 * lwContents.indexOf(c, j + 1)) != -1) { newuri =
					 * lwContents.substring(j+4, k); redirectUrl =
					 * getAbsoluteUrl(method,newuri);
					 * Engine.logBeans.debug("(HttpConnector) Redirecting to : "
					 * + redirectUrl); redirectMethod = new
					 * GetMethod(redirectUrl);
					 * 
					 * // set headers for (z=0; z<requestHeaders.length; z++)
					 * redirectMethod.setRequestHeader(requestHeaders[z]);
					 * 
					 * referer = redirectUrl; result =
					 * executeMethod(redirectMethod, context); // recurse } } }
					 * } // Handles FRAMESET else if
					 * (lwContents.indexOf("frameset") != -1) {
					 * Engine.logBeans.debug
					 * ("(HttpConnector) Analyzing frameset...");
					 * StringTokenizer st = new StringTokenizer(lwContents);
					 * StringEx newcontents = new StringEx(lwContents); while
					 * (st.hasMoreTokens()) { String token = st.nextToken();
					 * String uri; if (token.startsWith("src=")) { if
					 * ((token.indexOf("\"") != -1) || (token.indexOf("'") !=
					 * -1)) { token = token.substring(5); uri =
					 * token.substring(0,token.length()-1); newuri =
					 * getAbsoluteUrl(method,uri);
					 * Engine.logBeans.trace("(HttpConnector) Replaced uri ("+
					 * uri +") with newuri("+ newuri +")");
					 * 
					 * newcontents.replaceAll(token,newuri); } } }
					 * Engine.logBeans
					 * .trace("(HttpConnector) New response content:\n"+
					 * newcontents); result = newcontents.toString().getBytes();
					 * }
					 */
				}
			}
			//Added by julienda - #3433 - 04/03/2013
			AbstractHttpTransaction abstractHttpTransaction = (AbstractHttpTransaction) context.transaction;
			
			if (abstractHttpTransaction.getHttpInfo()) {
				Document doc = context.outputDocument;
				
				//Remove the node HTTPInfo if we have a redirect
				NodeList nodeList = XMLUtils.findElements(context.outputDocument.getDocumentElement(), abstractHttpTransaction.getHttpInfoTagName());
				if (nodeList != null) {
					XMLUtils.removeNodeListContent(nodeList);
				}
				
				//Parent Element
				httpInfoElement = doc.createElement(abstractHttpTransaction.getHttpInfoTagName());
				
				//Add requested URL
				Element urlElement = doc.createElement("url");
				urlElement.setTextContent(method.getURI().toString());
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
						elt.setAttribute("name", headers.get(i).toString().substring( 0, headers.get(i).toString().indexOf(":") ) );
						elt.setAttribute("value", headers.get(i).toString().substring( headers.get(i).toString().indexOf(":")+2 ) );
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

			Engine.logBeans.debug("(HttpConnector) HttpClient: executing method...");
			statuscode = Engine.theApp.httpClient.executeMethod(hostConfiguration, method, httpState);
			Engine.logBeans.debug("(HttpConnector) HttpClient: end of method successfull");

			// Display the cookies
			if (handleCookie) {
				Cookie[] cookies = httpState.getCookies();
				if (Engine.logBeans.isTraceEnabled())
					Engine.logBeans.trace("(HttpConnector) HttpClient response cookies:"
							+ Arrays.asList(cookies).toString());
			}
		} catch (IOException e) {
			try {
				Engine.logBeans.warn("(HttpConnector) HttpClient: connection error to " + sUrl + ": "
						+ e.getMessage() + "; retrying method");
				statuscode = Engine.theApp.httpClient.executeMethod(hostConfiguration, method, httpState);
				Engine.logBeans.debug("(HttpConnector) HttpClient: end of method successfull");
			} catch (IOException ee) {
				throw new ConnectionException("Connection error to " + sUrl, ee);
			}
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

	private String getRequestEncoding(AbstractHttpTransaction httpTransaction) {
		if (httpTransaction instanceof XmlHttpTransaction) {
			return (((XmlHttpTransaction) httpTransaction).getXmlEncoding());
		}
		return "UTF-8";
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
							Protocol myhttps = new Protocol("https",
									(ProtocolSocketFactory) new MySSLSocketFactory(
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
					URI uri = method.getURI();
					String methodProtocol = uri.getScheme();
					String methodHost = uri.getHost();

					if (hostConfiguration.getProtocol().isSecure()) {
						return givenUrl.startsWith("/") ? givenUrl : ('/' + givenUrl);
					}

					int methodPort = uri.getPort();
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
		this.server = server;
	}

	/** Holds value of property basicUser. */
	private String basicUser = "";

	/**
	 * Getter for property basicUser.
	 * 
	 * @return the basicUser
	 */
	public String getBasicUser() {
		return basicUser;
	}

	/**
	 * Setter for property basicUser.
	 * 
	 * @param basicUser
	 *            the basicUser to set
	 */
	public void setBasicUser(String basicUser) {
		this.basicUser = basicUser;
	}

	/** Holds value of property basicPassword. */
	private String basicPassword = "";

	/**
	 * Getter for property basicPassword.
	 * 
	 * @return the basicPassword
	 */
	public String getBasicPassword() {
		return basicPassword;
	}

	/**
	 * Setter for property basicPassword.
	 * 
	 * @param basicPassword
	 *            the basicPassword to set
	 */
	public void setBasicPassword(String basicPassword) {
		this.basicPassword = basicPassword;
	}

	/** Holds value of givenBasicUser. */
	transient private String givenBasicUser = null;

	public String getGivenBasicUser() {
		return givenBasicUser;
	}

	public void setGivenBasicUser(String givenBasicUser) {
		this.givenBasicUser = givenBasicUser;
	}

	transient private String givenBasicPassword = null;

	public String getGivenBasicPassword() {
		return givenBasicPassword;
	}

	public void setGivenBasicPassword(String givenBasicPassword) {
		this.givenBasicPassword = givenBasicPassword;
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
		if ("basicPassword".equals(propertyName)) {
			return true;
		}
		return super.isMaskedProperty(target, propertyName);
	}

	@Override
	public boolean isCipheredProperty(String propertyName) {
		if ("basicPassword".equals(propertyName)) {
			return true;
		}
		return super.isCipheredProperty(propertyName);
	}
}