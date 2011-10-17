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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.httpclient.Header;
import org.w3c.dom.Document;

import com.twinsoft.api.Session;
import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.proxy.cache.CacheEntry;
import com.twinsoft.convertigo.engine.proxy.cache.FileCacheManager;
import com.twinsoft.convertigo.engine.translators.DefaultServletTranslator;
import com.twinsoft.convertigo.engine.translators.Translator;

public class WebClippingServletRequester extends GenericRequester {
	private final  static Pattern parseURI = Pattern.compile(".*/webclipper/(.*?)/(.*?)/(.*?)/(.*)");
	private static FileCacheManager cache = null;
	private String contextName = null;
	private boolean bCache = true;
	private boolean bDirty = false;
	
	public WebClippingServletRequester(){
		createFileCacheManager();
	}
	
	public WebClippingServletRequester(HttpServletResponse response){
		createFileCacheManager();
	}
	
	private void createFileCacheManager() {
		if (cache == null) {
			cache = new FileCacheManager();
			try {
				cache.init();
			} catch (EngineException e) {
				Engine.logEngine.error("Error when create cache", e);
			}
		}
	}
	
	public static void storeResponse(String resourceUrl, byte[] data) throws EngineException {
		cache.storeResponse(resourceUrl, data);
	}

    @Override
    public String getName() {
        return "WebClippingServletRequester";
    }

    @Override
	public void setStyleSheet(Document document) {
        // No sheet should be inserted
        return;
	}

	@Override
	protected Object coreProcessRequest() throws Exception {
		try {
			if (context.getConnector() != null) {
				if (context.getConnector() instanceof HtmlConnector) {
					HtmlConnector connector = (HtmlConnector) context.getConnector();
					if (bDirty) {
						return Boolean.toString(connector.getHtmlParser().getDomDirty(context)).getBytes();
					} else {
						HttpServletRequest request = (HttpServletRequest) inputData;
						String webURL = request.getQueryString();
						String contentType = null;
						if (webURL == null) {
							Engine.logEngine.debug("(WebClippingServletRequester) no webURL !!! return empty result");
							return new byte[0];
						}
						
						webURL = webURL.replace('\\', '/'); //TODO: q&d fix #419 [Invalid URI error if ressource image to get contains "\"] ; may be include it in httpClient ?
						
						Engine.logEngine.trace("(WebClippingServletRequester) weburl is " + webURL);
						
						InputStream is = bCache ? cache.getResource(webURL) : null;
						if (is == null) {
							String charset = null;
							byte[]data = null;
							
							synchronized (context) {
								connector.sUrl = webURL;
								if (context.requestedObject == null) {
									context.requestedObject = connector.getDefaultTransaction();
									context.transaction = (HtmlTransaction)context.requestedObject;
									context.requestedObject.createRequestableThread();
								}
								//save old context headers
								String exContentType = context.contentType;
								Header[] exHeaders = context.getResponseHeaders();
								
								data = connector.getData(context);
								
								contentType = context.contentType;
								charset = connector.getCharset();
								
								//restore old context headers
								context.contentType = exContentType;
								context.setResponseHeaders(exHeaders);
								
								Engine.logEngine.trace("(WebClippingServletRequester) contentType is " + contentType);
								Engine.logEngine.trace("(WebClippingServletRequester) charset is " + charset);
							}
							request.setAttribute("convertigo.charset", charset);

							data = rewriteCSS(data, webURL, contentType, charset);

							is = new ByteArrayInputStream(data);
							if (bCache) {
								CacheEntry c = cache.storeResponse(webURL, data);
								c.contentLength = data.length;
								c.contentType = contentType;								
							}
						} else {
							contentType = cache.getCacheEntry(webURL).contentType;
						}
						
						request.setAttribute("convertigo.contentType", contentType);

						byte[] data = new byte[is.available()];

						is.read(data);
						is.close();
						return data;
					}
				} else if (context.getConnector() instanceof JavelinConnector && bDirty) {
					Session session = Engine.theApp.sessionManager.getSession(context.contextID);
					if (session != null) {
						return Boolean.toString(session.isSomethingChange());
					}
				}
			}
		} catch (EngineException e) {
			Engine.logEngine.error("Error", e);
		}
		return null;
	}

    @Override
	public Context getContext() throws Exception {
		HttpServletRequest request = (HttpServletRequest) inputData;

		initInternalVariables();
				
		HttpSession httpSession = request.getSession();
		String sessionID = httpSession.getId();
		
		context = Engine.isEngineMode() ?
				Engine.theApp.contextManager.get(this, contextName, sessionID, poolName, projectName, connectorName, sequenceName) :
				Engine.theApp.contextManager.get(this, connectorName, sessionID, poolName, projectName, connectorName, sequenceName);

		return context;
	}

    @Override
	protected void initInternalVariables() throws EngineException {
		HttpServletRequest request = (HttpServletRequest) inputData;
		
		Matcher uri = parseURI.matcher(request.getRequestURI());
		
		if (uri.matches()) {
			projectName = uri.group(1);
			connectorName = uri.group(2);
			if (connectorName.equals("$")) {
				connectorName = null;
			}
			contextName = uri.group(3);
			if (contextName.equals("$")) {
				contextName = "default";
			}
			
			/**
			 * mHttptunnelOnCache -> c
			 * mHttptunnelOnNoCache -> n 
			 * doDirty -> d
			 * */
			String mode = uri.group(4);
			bCache = mode.equals("c");
			bDirty = mode.equals("d");
			
			Engine.logEngine.debug("(ServletRequester) requested execution context : " + contextName);
		} else {
			throw new EngineException("(WebClippingServletRequester) invalide uri : " + uri.group());
		}
	}

    @Override
	public void preGetDocument() throws Exception {
	}

    @Override
	public Translator getTranslator() {
		return new DefaultServletTranslator();
	}
	
	public byte[] rewriteCSS(byte[] data, String webURL, String contentType, String charset) {
		if (context.urlRewriter != null &&
				(contentType != null && contentType.startsWith("text/css") || webURL.endsWith(".css"))) {
			String txt = null;				

			List<String> al = new LinkedList<String>(Arrays.asList(new String[]{"ASCII", "ISO-8859-1", "UTF-8"}));
			if (charset != null) {
				al.add(0, charset); 
			}
			for (String cs : al) {
				try {
					txt = new String(data, charset=cs);
					break;
				} catch (UnsupportedEncodingException e) {
					
				}
			}
			if (txt != null) {
				//txt = context.urlRewriter.rewriteStyle(txt);
				txt = context.urlRewriter.rewriteStyle(txt, webURL); // Report from 4.5 #419
				try {
					data = txt.getBytes(charset);
				} catch (UnsupportedEncodingException e) {
					data = txt.getBytes();
				}
			}
		}
		return data;
	}
}