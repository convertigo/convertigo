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

package com.twinsoft.convertigo.engine.proxy.translated;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;

import com.twinsoft.convertigo.beans.connectors.ProxyHttpConnector;
import com.twinsoft.convertigo.beans.connectors.ProxyHttpConnector.Replacements;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EngineStatistics;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.plugins.AbstractBiller;
import com.twinsoft.convertigo.engine.proxy.cache.CacheEntry;
import com.twinsoft.convertigo.engine.proxy.cache.FileCacheManager;
import com.twinsoft.convertigo.engine.requesters.ServletRequester;
import com.twinsoft.convertigo.engine.translators.NullTranslator;
import com.twinsoft.convertigo.engine.translators.Translator;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.util.StringEx;

public class ProxyServletRequester extends ServletRequester {

	protected static FileCacheManager proxyCacheManager;

	static {
		try {
			proxyCacheManager = new FileCacheManager();
			proxyCacheManager.init();
		}
		catch(Exception e) {
			Engine.logEngine.error("Unexpected exception", e);
		}
	}
	
	static public ThreadLocal<ParameterShuttle> threadParameterShuttle = new ThreadLocal<ParameterShuttle>() {
		protected ParameterShuttle initialValue() {
			return new ParameterShuttle();
		}
	};
    
	static private ThreadLocal<HttpClient> threadHttpClient = new ThreadLocal<HttpClient>() {
		protected HttpClient initialValue() {
			return new HttpClient();
		}
	};

    public String getName() {
        return "ProxyServletRequester";
    }

	public Context getContext() throws Exception {
		ParameterShuttle infoShuttle = threadParameterShuttle.get();
		infoShuttle.context = super.getContext();
		return infoShuttle.context;
	}

	public Translator getTranslator() {
		return new NullTranslator();
	}

    protected Object coreProcessRequest() throws Exception {
		// The proxy converts HTML data on the fly
		ParameterShuttle infoShuttle = threadParameterShuttle.get();
		HttpClient httpClient = threadHttpClient.get();
		HttpServletRequest request = (HttpServletRequest) inputData;

		InputStream siteIn = null;

		try {
			try {
				getUserRequest(infoShuttle, context, request);

				// Loading project
				if (context.projectName == null) throw new EngineException("The project name has not been specified!");

	            Project currentProject;
				if (Engine.isStudioMode()) {
					currentProject = Engine.objectsProvider.getProject(context.projectName);
					if (currentProject == null) {
						throw new EngineException("No project has been opened in the Studio. A project should be opened in the Studio in order that the Convertigo engine can work.");
					}
					else if (!currentProject.getName().equalsIgnoreCase(context.projectName)) {
						throw new EngineException("The requested project (\"" + context.projectName + "\") does not match with the opened project (\"" + currentProject.getName() + "\") in the Studio.\nYou cannot make a request on a different project than the one opened in the Studio.");
					}
					Engine.logEngine.debug("Using project from Studio");
					context.project = currentProject;
				}
				else {
					if ((context.project == null) || (context.isNewSession)) {
						Engine.logEngine.debug("New project requested: '" + context.projectName + "'");
						context.project = Engine.theApp.databaseObjectsManager.getProjectByName(context.projectName);
						Engine.logEngine.debug("Project loaded: " + context.project.getName());
					}
				}
	
				// Loading sequence
				if (context.sequenceName != null) {
					context.requestedObject = context.project.getSequenceByName(context.sequenceName);
					Engine.logEngine.debug("Loaded sequence: " + context.requestedObject.getName());
				}
				
				// Loading connector
				context.loadConnector();
				
				if (context.requestedObject != null)
					context.requestedObject.context = context;
				
				if (context.getConnector() != null)
					context.getConnector().context = context;
			
				if (Boolean.parseBoolean(EnginePropertiesManager.getProperty(PropertyName.SSL_DEBUG))) {
					System.setProperty("javax.net.debug", "all");
					Engine.logEngine.trace("(ProxyServletRequester) Enabling SSL debug mode");
				}
				else {
					System.setProperty("javax.net.debug", "");
					Engine.logEngine.debug("(ProxyServletRequester) Disabling SSL debug mode");
				}

				if (context.getConnector().isTasAuthenticationRequired() && (context.tasSessionKey == null)) {
	            	throw new EngineException("A Carioca authentication is required in order to process the transaction.");
	            }

				infoShuttle.userID = context.tasUserName;
				infoShuttle.userIP = context.remoteAddr;
	
				// gather user id, parameters, headers from request
				gatherRequestInfo(infoShuttle, request);
	
				String t = context.statistics.start(EngineStatistics.APPLY_USER_REQUEST);
				
				try {
					// get connected
					httpClient.connect(infoShuttle);
				}
				finally {
					context.statistics.stop(t);
				}

				if (infoShuttle.siteInputStream == null) {
					Engine.logEngine.debug("(ProxyServletRequester) No input stream!");
					return null;
				}

				siteIn = infoShuttle.siteInputStream;

				Engine.logEngine.debug("(ProxyServletRequester) Start of document retransmission");
				Object result;
				
				ProxyHttpConnector proxyHttpConnector = (ProxyHttpConnector) infoShuttle.context.getConnector();
				
				String host = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
				String newBaseUrl = host + request.getRequestURI() + "?"+Parameter.Connector.getName()+"=" + proxyHttpConnector.getName() + "&"+Parameter.ProxyGoto.getName()+"=";
				String newBaseUrlThen = newBaseUrl + URLEncoder.encode(infoShuttle.siteURL.getPath(), "UTF-8") + "&"+Parameter.ProxyThen.getName()+"=";
				
				if (isDynamicContent(infoShuttle.siteURL.getPath(), proxyHttpConnector.getDynamicContentFiles())) {
					Engine.logEngine.debug("(ProxyServletRequester) Dynamic content");
					String sResponse;
					StringBuffer sbResponse = new StringBuffer("");
					
					t = context.statistics.start(EngineStatistics.APPLY_USER_REQUEST);
					
					try {
						int c = siteIn.read();
						char cc;
						while (c > -1) {
							cc = (char) c;
							sbResponse.append(cc);
							c = siteIn.read();
						}
					}
					finally {
						context.statistics.stop(t, true);
					}

					sResponse = sbResponse.toString();
					result = sResponse;
					
					if (infoShuttle.siteContentType == null) {
						Engine.logEngine.warn("(ProxyServletRequester) Aborting string replacements because of null mimetype! Resource: " + infoShuttle.siteURL);
					}
					else {
						Engine.logEngine.debug("(ProxyServletRequester) String replacements");
						
						// Text/html replacements
						Engine.logEngine.debug("(ProxyServletRequester) Replacements for mime type '" + infoShuttle.siteContentType + "'");
						Replacements replacements = proxyHttpConnector.getReplacementsForMimeType(infoShuttle.siteContentType);

						if (!replacements.isEmpty()) {
							StringEx sxResponse = new StringEx(sResponse);

							StringEx sx;
							String strSearched, strReplacing;
							for (int i = 0; i < replacements.strReplacing.length; i++) {
								strSearched = replacements.strSearched[i];
								sx = new StringEx(strSearched);
								sx.replaceAll("{tab}", "\t");
								sx.replaceAll("{apos0x92}", "" + (char) 146);
								sx.replaceAll("{newBaseUrl}", newBaseUrl);
								sx.replaceAll("{newBaseUrlThen}", newBaseUrlThen);
								strSearched = sx.toString();
								replacements.strSearched[i] = strSearched;
								Engine.logEngine.debug("(ProxyServletRequester) Replacing: " + strSearched);

								strReplacing = replacements.strReplacing[i];
								sx = new StringEx(strReplacing);
								sx.replaceAll("{newBaseUrl}", newBaseUrl);
								sx.replaceAll("{newBaseUrlThen}", newBaseUrlThen);
								strReplacing = sx.toString();
								replacements.strReplacing[i] = strReplacing;
								Engine.logEngine.debug("(ProxyServletRequester) By: " + strReplacing);
							}
		
							Engine.logEngine.debug("(ProxyServletRequester) Replacements in progress");
							sxResponse.replaceAll(replacements.strSearched, replacements.strReplacing);
							Engine.logEngine.debug("(ProxyServletRequester) Replacements done!");
		
							if (Engine.isStudioMode()) {
								sxResponse.replaceAll("?"+Parameter.Connector.getName()+"=","?"+Parameter.Context.getName()+"="+ infoShuttle.context.name +"&"+Parameter.Connector.getName()+"=");
							}
							
							result = sxResponse.toString();
						}
					}

					infoShuttle.siteContentSize = ((String) result).length();
					Engine.logEngine.debug("(ProxyServletRequester) HTML data retrieved!");

		    		String billingClassName = context.getConnector().getBillingClassName();
		    		if (billingClassName != null) {
						try {
							Engine.logContext.debug("Billing class name required: " + billingClassName);
							AbstractBiller biller = (AbstractBiller) Class.forName(billingClassName).newInstance();
							Engine.logContext.debug("Executing the biller");
							biller.insertBilling(context);
						}
						catch(Throwable e) {
							Engine.logContext.warn("Unable to execute the biller (the billing is thus ignored): [" + e.getClass().getName() + "] " + e.getMessage());
						}
		    		}
				}
				else {
					Engine.logEngine.debug("(ProxyServletRequester) Static content: " + infoShuttle.siteContentType);
					
					ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
					
					t = context.statistics.start(EngineStatistics.APPLY_USER_REQUEST);
					
					try {
						// Read either from the cache, either from the remote server
						int c = siteIn.read();
						while (c > -1) {
							baos.write(c);
							c = siteIn.read();
						}
					}
					finally {
						context.statistics.stop(t, true);
					}
					
					result = baos.toByteArray();
					Engine.logEngine.debug("(ProxyServletRequester) Static data retrieved!");
					
					// Determine if the resource has already been cached or not
					String resourceUrl = infoShuttle.siteURL.toString();
					CacheEntry cacheEntry  = ProxyServletRequester.proxyCacheManager.getCacheEntry(resourceUrl);
					if (cacheEntry == null) {
						// Managing text replacements
						Engine.logEngine.debug("(ProxyServletRequester) Replacements for mime type '" + infoShuttle.siteContentType + "'");

						Replacements replacements = proxyHttpConnector.getReplacementsForMimeType(infoShuttle.siteContentType);
						
						if (!replacements.isEmpty()) {
							String sResult = new String((byte[]) result);
							StringEx sxResponse = new StringEx(sResult);

							StringEx sx;
							String strSearched, strReplacing;
							for (int i = 0; i < replacements.strReplacing.length; i++) {
								strSearched = replacements.strSearched[i];
								sx = new StringEx(strSearched);
								sx.replaceAll("{tab}", "\t");
								sx.replaceAll("{newBaseUrl}", newBaseUrl);
								sx.replaceAll("{newBaseUrlThen}", newBaseUrlThen);
								strSearched = sx.toString();
								replacements.strSearched[i] = strSearched;
								Engine.logEngine.debug("(ProxyServletRequester) Replacing: " + strSearched);

								strReplacing = replacements.strReplacing[i];
								sx = new StringEx(strReplacing);
								sx.replaceAll("{newBaseUrl}", newBaseUrl);
								sx.replaceAll("{newBaseUrlThen}", newBaseUrlThen);
								strReplacing = sx.toString();
								replacements.strReplacing[i] = strReplacing;
								Engine.logEngine.debug("(ProxyServletRequester) By: " + strReplacing);
							}

							Engine.logEngine.debug("(ProxyServletRequester) Replacements in progress");
							sxResponse.replaceAll(replacements.strSearched, replacements.strReplacing);
							Engine.logEngine.debug("(ProxyServletRequester) Replacements done!");

							result = sxResponse.toString().getBytes();
						}
						
						if (infoShuttle.httpCode == 200) {
							Engine.logEngine.debug("(ProxyServletRequester) Resource stored: " + resourceUrl);
							cacheEntry = proxyCacheManager.storeResponse(resourceUrl, (byte[]) result);
							cacheEntry.contentLength = ((byte[]) result).length;
							infoShuttle.siteContentSize = cacheEntry.contentLength;
							cacheEntry.contentType = infoShuttle.siteContentType;
						}
					}
					else {
						infoShuttle.httpCode = 200;
						infoShuttle.siteContentSize = cacheEntry.contentLength;
						infoShuttle.siteContentType = cacheEntry.contentType;
					}
					
					baos.close();
				}

				Engine.logEngine.debug("(ProxyServletRequester) End of document retransmission");
				
				return result;
			}
			finally {
				if (siteIn != null) {
					try {
						siteIn.close();
					}
					catch (Exception e) {
					}
				}
			}
		}
		finally {
            context.contentType = infoShuttle.siteContentType;
			httpClient.disconnect();
		}
    }
	
    private boolean isDynamicContent(String path, String dynamicContentFiles) {
    	StringTokenizer stringTokenizer = new StringTokenizer(dynamicContentFiles, " ");
    	String filePattern;
    	while (stringTokenizer.hasMoreTokens()) {
    		filePattern = stringTokenizer.nextToken();
    		if (path.endsWith(filePattern)) return true;
    	}

		return false;
    }
    
	private void getUserRequest(ParameterShuttle infoShuttle, Context context, HttpServletRequest request) {
		context.requestedObject = new ProxyTransaction();
		
		// For compatibility with older projects, set the transaction context property
		context.transaction = (ProxyTransaction)context.requestedObject;
		
		Object module = context.get("module");
		if (module != null) context.transactionName = module.toString();
		
		if ((context.transactionName == null) || (context.transactionName.equals(""))) {
			context.transactionName = "(proxy)";
		}

		// We transform the HTTP post data into XML data.
		Enumeration<String> parameterNames = GenericUtils.cast(request.getParameterNames());
		String parameterName, parameterValue;

		while (parameterNames.hasMoreElements()) {
			parameterName = (String) parameterNames.nextElement();
			parameterValue = (String) request.getParameter(parameterName);
            
			// Context variables
			if (!parameterName.startsWith("__")) {
				context.set(parameterName, parameterValue);
				Engine.logEngine.debug("Added context variable '" + parameterName + "' = '" + parameterValue + "'");
				continue;
			}
		}
	}
    
	private void gatherRequestInfo(ParameterShuttle infoShuttle, HttpServletRequest request) throws IOException {
		infoShuttle.sessionID = request.getSession().getId();

		infoShuttle.postFromUser = request.getMethod().equals("POST");
		if (infoShuttle.postFromUser) {
			infoShuttle.userContentType = request.getContentType();
			infoShuttle.userContentLength = request.getContentLength();
		}

/*		String query = request.getQueryString();
		if (query != null) {
			int pos = 0;

			infoShuttle.postToSite = query.startsWith(ParameterShuttle.bridgePostTag);
			if (infoShuttle.postToSite)
				pos = ParameterShuttle.bridgePostTag.length();

			if (query.startsWith(ParameterShuttle.bridgeGotoTag, pos)) {
				int p2 = query.indexOf(ParameterShuttle.bridgeThenTag, pos);

				if (p2 > -1) {
					infoShuttle.userGoto = query.substring(pos + ParameterShuttle.bridgeGotoTag.length(), p2);
					infoShuttle.userThen = query.substring(p2 + ParameterShuttle.bridgeThenTag.length());
				}
				else {
					infoShuttle.userGoto = query.substring(pos + ParameterShuttle.bridgeGotoTag.length());
				}
			}
			else if (query.startsWith(ParameterShuttle.bridgeThenTag, pos)) {
				infoShuttle.userThen = query.substring(pos + ParameterShuttle.bridgeThenTag.length());
			}
		}*/
		infoShuttle.postToSite = infoShuttle.postFromUser;//request.getParameter(Parameter.ProxyPost.getName()) != null;
		infoShuttle.userGoto = request.getParameter(Parameter.ProxyGoto.getName());
		infoShuttle.userThen = request.getParameter(Parameter.ProxyThen.getName());

		Enumeration<String> allHeaderNames = GenericUtils.cast(request.getHeaderNames());
		while (allHeaderNames.hasMoreElements()) {
			String name = (String) allHeaderNames.nextElement();
			infoShuttle.userHeaderNames.add(name.toLowerCase());
			infoShuttle.userHeaderValues.add(request.getHeader(name));
		}

		ParameterShuttle.getSelfURL(
			request.getScheme(),
			request.getServerName(),
			request.getServerPort(),
			request.getRequestURI()
		);
	}

	protected void findStyleSheet(String browser) throws EngineException {
		// Nothing to do
	}

	public void setStyleSheet(Document document) {
		// Nothing to do
	}
	
	protected Object addStatisticsAsData(Object result) {
		return result;
	}
	protected Object addStatisticsAsText(String stats, Object result) throws UnsupportedEncodingException{
		return result;
	}
}