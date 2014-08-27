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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.proxy.util.ClientCookie;
import com.twinsoft.convertigo.engine.proxy.util.CookieManager;
import com.twinsoft.convertigo.engine.proxy.util.IntQueue;
import com.twinsoft.util.StringEx;

public class HttpBridge {
	static private CookieManager cookieMgr = CookieManager.getDefaultInstance();

	// general headers
	static private int HH_CACHE_CONTROL = 0;
	static private int HH_CONNECTION = 1;
	static private int HH_DATE = 2;
	static private int HH_PRAGMA = 3;
	static private int HH_TRAILER = 4;
	static private int HH_TRANSFER_ENCODING = 5;
	static private int HH_UPGRADE = 6;
	static private int HH_VIA = 7;
	static private int HH_WARNING = 8;
	// entity headers
	static private int HH_ALLOW = 9;
	static private int HH_CONTENT_LOCATION = 10;
	static private int HH_CONTENT_TYPE = 11;
	static private int HH_CONTENT_LENGTH = 12;
	static private int HH_CONTENT_LANGUAGE = 13;
	static private int HH_CONTENT_RANGE = 14;
	static private int HH_CONTENT_ENCODING = 15;
	static private int HH_CONTENT_MD5 = 16;
	static private int HH_EXPIRES = 17;
	static private int HH_LAST_MODIFIED = 18;
	// request headers
	static private int HH_TE = 19;
	static private int HH_ACCEPT = 20;
	static private int HH_ACCEPT_CHARSET = 21;
	static private int HH_ACCEPT_LANGUAGE = 22;
	static private int HH_ACCEPT_ENCODING = 23;
	static private int HH_AUTHORIZATION = 24;
	static private int HH_EXPECT = 25;
	static private int HH_RANGE = 26;
	static private int HH_IF_MATCH = 27;
	static private int HH_IF_NONE_MATCH = 28;
	static private int HH_IF_MODIFIED_SINCE = 29;
	static private int HH_IF_UNMODIFIED_SINCE = 30;
	static private int HH_IF_RANGE = 31;
	static private int HH_MAX_FORWARDS = 32;
	static private int HH_PROXY_AUTHORIZATION = 33;
	static private int HH_REFERER = 34;
	static private int HH_FROM = 35;
	static private int HH_HOST = 36;
	static private int HH_USER_AGENT = 37;
	// response headers
	static private int HH_LOCATION = 38;
	static private int HH_AUTHENTICATE = 39;
	static private int HH_ACCEPT_RANGES = 40;
	static private int HH_AGE = 41;
	static private int HH_ETAG = 42;
	static private int HH_VARY = 43;
	static private int HH_RETRY_AFTER = 44;
	static private int HH_SERVER = 45;
	static private int HH_PROXY_AUTHENTICATE = 46;
	// cookie headers
	static private int HH_COOKIE = 47;
	static private int HH_SET_COOKIE = 48;
	// count
	static private int HH_TOTAL_COUNT = 49;

	static private String[] ALL_HEADERS;
	static private boolean[] AllowedToRequest;
	static private boolean[] AllowedToResponse;
	static {
		ALL_HEADERS = new String[HH_TOTAL_COUNT];
		AllowedToRequest = new boolean[HH_TOTAL_COUNT];
		AllowedToResponse = new boolean[HH_TOTAL_COUNT];

		// general headers

		ALL_HEADERS[HH_CACHE_CONTROL] = "cache-control";
		AllowedToRequest[HH_CACHE_CONTROL] = true;
		AllowedToResponse[HH_CACHE_CONTROL] = true;

		ALL_HEADERS[HH_CONNECTION] = "connection";
		AllowedToRequest[HH_CONNECTION] = true;
		AllowedToResponse[HH_CONNECTION] = true;

		ALL_HEADERS[HH_DATE] = "date";
		AllowedToRequest[HH_DATE] = true;
		AllowedToResponse[HH_DATE] = true;

		ALL_HEADERS[HH_PRAGMA] = "pragma";
		AllowedToRequest[HH_PRAGMA] = true;
		AllowedToResponse[HH_PRAGMA] = true;

		ALL_HEADERS[HH_TRAILER] = "trailer";
		AllowedToRequest[HH_TRAILER] = false;
		AllowedToResponse[HH_TRAILER] = false;

		ALL_HEADERS[HH_TRANSFER_ENCODING] = "transfer-encoding";
		AllowedToRequest[HH_TRANSFER_ENCODING] = false;
		AllowedToResponse[HH_TRANSFER_ENCODING] = false;

		ALL_HEADERS[HH_UPGRADE] = "upgrade";
		AllowedToRequest[HH_UPGRADE] = false;
		AllowedToResponse[HH_UPGRADE] = false;

		ALL_HEADERS[HH_VIA] = "via";
		AllowedToRequest[HH_VIA] = false;
		AllowedToResponse[HH_VIA] = false;

		ALL_HEADERS[HH_WARNING] = "warning";
		AllowedToRequest[HH_WARNING] = false;
		AllowedToResponse[HH_WARNING] = false;

		// entity headers

		ALL_HEADERS[HH_ALLOW] = "allow";
		AllowedToRequest[HH_ALLOW] = true;
		AllowedToResponse[HH_ALLOW] = true;

		ALL_HEADERS[HH_CONTENT_LOCATION] = "content-location";
		AllowedToRequest[HH_CONTENT_LOCATION] = false;
		AllowedToResponse[HH_CONTENT_LOCATION] = true;

		ALL_HEADERS[HH_CONTENT_TYPE] = "content-type";
		AllowedToRequest[HH_CONTENT_TYPE] = true;
		AllowedToResponse[HH_CONTENT_TYPE] = true;

		ALL_HEADERS[HH_CONTENT_LENGTH] = "content-length";
		AllowedToRequest[HH_CONTENT_LENGTH] = false;
		AllowedToResponse[HH_CONTENT_LENGTH] = false;

		ALL_HEADERS[HH_CONTENT_LANGUAGE] = "content-language";
		AllowedToRequest[HH_CONTENT_LANGUAGE] = false;
		AllowedToResponse[HH_CONTENT_LANGUAGE] = true;

		ALL_HEADERS[HH_CONTENT_RANGE] = "content-range";
		AllowedToRequest[HH_CONTENT_RANGE] = false;
		AllowedToResponse[HH_CONTENT_RANGE] = true;

		ALL_HEADERS[HH_CONTENT_ENCODING] = "content-encoding";
		AllowedToRequest[HH_CONTENT_ENCODING] = false;
		AllowedToResponse[HH_CONTENT_ENCODING] = true;

		ALL_HEADERS[HH_CONTENT_MD5] = "content-md5";
		AllowedToRequest[HH_CONTENT_MD5] = false;
		AllowedToResponse[HH_CONTENT_MD5] = true;

		ALL_HEADERS[HH_EXPIRES] = "expires";
		AllowedToRequest[HH_EXPIRES] = false;
		AllowedToResponse[HH_EXPIRES] = true;

		ALL_HEADERS[HH_LAST_MODIFIED] = "last-modified";
		AllowedToRequest[HH_LAST_MODIFIED] = false;
		AllowedToResponse[HH_LAST_MODIFIED] = true;

		// request headers

		ALL_HEADERS[HH_TE] = "te";
		AllowedToRequest[HH_TE] = false;
		AllowedToResponse[HH_TE] = false;

		ALL_HEADERS[HH_ACCEPT] = "accept";
		AllowedToRequest[HH_ACCEPT] = true;
		AllowedToResponse[HH_ACCEPT] = false;

		ALL_HEADERS[HH_ACCEPT_CHARSET] = "accept-charset";
		AllowedToRequest[HH_ACCEPT_CHARSET] = true;
		AllowedToResponse[HH_ACCEPT_CHARSET] = false;

		ALL_HEADERS[HH_ACCEPT_LANGUAGE] = "accept-language";
		AllowedToRequest[HH_ACCEPT_LANGUAGE] = true;
		AllowedToResponse[HH_ACCEPT_LANGUAGE] = false;

		ALL_HEADERS[HH_ACCEPT_ENCODING] = "accept-encoding";
		AllowedToRequest[HH_ACCEPT_ENCODING] = false;
		AllowedToResponse[HH_ACCEPT_ENCODING] = false;

		ALL_HEADERS[HH_AUTHORIZATION] = "authorization";
		AllowedToRequest[HH_AUTHORIZATION] = true;
		AllowedToResponse[HH_AUTHORIZATION] = false;

		ALL_HEADERS[HH_EXPECT] = "expect";
		AllowedToRequest[HH_EXPECT] = true;
		AllowedToResponse[HH_EXPECT] = false;

		ALL_HEADERS[HH_RANGE] = "range";
		AllowedToRequest[HH_RANGE] = true;
		AllowedToResponse[HH_RANGE] = false;

		ALL_HEADERS[HH_IF_MATCH] = "if-match";
		AllowedToRequest[HH_IF_MATCH] = true;
		AllowedToResponse[HH_IF_MATCH] = false;

		ALL_HEADERS[HH_IF_NONE_MATCH] = "if-none-match";
		AllowedToRequest[HH_IF_NONE_MATCH] = true;
		AllowedToResponse[HH_IF_NONE_MATCH] = false;

		ALL_HEADERS[HH_IF_MODIFIED_SINCE] = "if-modified-since";
		AllowedToRequest[HH_IF_MODIFIED_SINCE] = true;
		AllowedToResponse[HH_IF_MODIFIED_SINCE] = false;

		ALL_HEADERS[HH_IF_UNMODIFIED_SINCE] = "if-unmodified-since";
		AllowedToRequest[HH_IF_UNMODIFIED_SINCE] = true;
		AllowedToResponse[HH_IF_UNMODIFIED_SINCE] = false;

		ALL_HEADERS[HH_IF_RANGE] = "if-range";
		AllowedToRequest[HH_IF_RANGE] = true;
		AllowedToResponse[HH_IF_RANGE] = false;

		ALL_HEADERS[HH_MAX_FORWARDS] = "max-forwards";
		AllowedToRequest[HH_MAX_FORWARDS] = false;
		AllowedToResponse[HH_MAX_FORWARDS] = false;

		ALL_HEADERS[HH_PROXY_AUTHORIZATION] = "proxy-authorization";
		AllowedToRequest[HH_PROXY_AUTHORIZATION] = false;
		AllowedToResponse[HH_PROXY_AUTHORIZATION] = false;

		ALL_HEADERS[HH_REFERER] = "referer";
		AllowedToRequest[HH_REFERER] = false;
		AllowedToResponse[HH_REFERER] = false;

		ALL_HEADERS[HH_FROM] = "from";
		AllowedToRequest[HH_FROM] = false;
		AllowedToResponse[HH_FROM] = false;

		ALL_HEADERS[HH_HOST] = "host";
		AllowedToRequest[HH_HOST] = false;
		AllowedToResponse[HH_HOST] = false;

		ALL_HEADERS[HH_USER_AGENT] = "user-agent";
		AllowedToRequest[HH_USER_AGENT] = true;
		AllowedToResponse[HH_USER_AGENT] = false;

		// response headers

		ALL_HEADERS[HH_LOCATION] = "location";
		AllowedToRequest[HH_LOCATION] = false;
		AllowedToResponse[HH_LOCATION] = true;

		ALL_HEADERS[HH_AUTHENTICATE] = "authenticate";
		AllowedToRequest[HH_AUTHENTICATE] = false;
		AllowedToResponse[HH_AUTHENTICATE] = true;

		ALL_HEADERS[HH_ACCEPT_RANGES] = "accept-ranges";
		AllowedToRequest[HH_ACCEPT_RANGES] = false;
		AllowedToResponse[HH_ACCEPT_RANGES] = true;

		ALL_HEADERS[HH_AGE] = "age";
		AllowedToRequest[HH_AGE] = false;
		AllowedToResponse[HH_AGE] = true;

		ALL_HEADERS[HH_ETAG] = "etag";
		AllowedToRequest[HH_ETAG] = false;
		AllowedToResponse[HH_ETAG] = true;

		ALL_HEADERS[HH_VARY] = "vary";
		AllowedToRequest[HH_VARY] = false;
		AllowedToResponse[HH_VARY] = true;

		ALL_HEADERS[HH_RETRY_AFTER] = "retry-after";
		AllowedToRequest[HH_RETRY_AFTER] = false;
		AllowedToResponse[HH_RETRY_AFTER] = true;

		ALL_HEADERS[HH_SERVER] = "server";
		AllowedToRequest[HH_SERVER] = false;
		AllowedToResponse[HH_SERVER] = false;

		ALL_HEADERS[HH_PROXY_AUTHENTICATE] = "proxy-authenticate";
		AllowedToRequest[HH_PROXY_AUTHENTICATE] = false;
		AllowedToResponse[HH_PROXY_AUTHENTICATE] = false;

		ALL_HEADERS[HH_COOKIE] = "cookie";
		AllowedToRequest[HH_COOKIE] = false;
		AllowedToResponse[HH_COOKIE] = false;

		ALL_HEADERS[HH_SET_COOKIE] = "set-cookie";
		AllowedToRequest[HH_SET_COOKIE] = false;
		AllowedToResponse[HH_SET_COOKIE] = true;
	};

	static private int findString(
		String target,
		String[] candList,
		boolean[] candFlags,
		boolean matchFlag) {
		int k = 0, kSize = candList.length;
		for (; k < kSize; k++)
			if (candFlags[k] == matchFlag && candList[k].equals(target))
				break;

		return k >= kSize ? -1 : k;
	}

	static public void convertIncomingRequest(ParameterShuttle infoShuttle) {
		HttpConnector connector = (HttpConnector) infoShuttle.context.getConnector();
		connector.setBaseUrl(); 
		
		// compose target siteURL by Http Bridge parameters
		
		String bridgeGoto = infoShuttle.userGoto;
		
		// BUGFIX: if the url contains blank spaces, replace them by "%20"
		if (bridgeGoto != null) {
			StringEx sx = new StringEx(bridgeGoto);
			sx.replaceAll(" ", "%20");
			bridgeGoto = sx.toString();
		}

		String bridgeThen = infoShuttle.userThen;

		// BUGFIX: if the url contains blank spaces, replace them by "%20"
		if (bridgeThen != null) {
			StringEx sx = new StringEx(bridgeThen);
			sx.replaceAll(" ", "%20");
			bridgeThen = sx.toString();
		}
		
		if (bridgeGoto == null) {
			bridgeGoto = connector.sUrl;
		}
		else if (!bridgeGoto.startsWith(connector.sUrl)) {
			bridgeGoto = connector.sUrl + bridgeGoto;
		}
		
		Engine.logEngine.debug("(Proxy) goto: " + bridgeGoto);
		Engine.logEngine.debug("(Proxy) then: " + bridgeThen);

		if (bridgeGoto == null)
			bridgeGoto = infoShuttle.prevSiteURL.toString();

		URI gotoURI = null, thenURI = null;

		if (bridgeThen != null) {
			try {
				thenURI = new URI(bridgeThen);
			}
			catch (URISyntaxException e) {
				throw new IllegalArgumentException("2nd target address in wrong syntax: " + bridgeThen);
			}

			if (thenURI.isAbsolute()) {
				gotoURI = thenURI;
				thenURI = null;
				bridgeGoto = bridgeThen;
				bridgeThen = null;
			}
		}

		if (gotoURI == null) {
			try {
				if (bridgeGoto.charAt(0) == '/' && infoShuttle.siteURL != null) {
					if (bridgeGoto.startsWith("//"))
						gotoURI = new URI(infoShuttle.siteURL.getProtocol() + ":" + bridgeGoto);
					else
						gotoURI = new URI(infoShuttle.siteURL.getProtocol() + "://" +
							infoShuttle.siteURL.getHost() +
							(infoShuttle.siteURL.getPort() == -1 ? "" : ":" + infoShuttle.siteURL.getPort()) + bridgeGoto);
				}
				else if (bridgeGoto.indexOf("://") < 0)
					gotoURI = new URI("http://" + bridgeGoto);
				else
					gotoURI = new URI(bridgeGoto);
			}
			catch (URISyntaxException e) {
				throw new IllegalArgumentException("main target address in wrong syntax: " + bridgeGoto);
			}
		}

		if (gotoURI.getScheme() == null || !gotoURI.getScheme().startsWith("http"))
			throw new IllegalArgumentException("missing or unsupported protocol in address: " + bridgeGoto);

		if (gotoURI.getHost() == null)
			throw new IllegalArgumentException("missing server in address: " + bridgeGoto);

		if (thenURI != null)
			gotoURI = gotoURI.resolve(thenURI);

		if (infoShuttle.postFromUser && !infoShuttle.postToSite) {
			StringBuffer sb = new StringBuffer(128 + infoShuttle.userContentLength);

			sb.append(gotoURI.getScheme()).append("://").append(gotoURI.getHost());
			if (gotoURI.getPort() != -1)
				sb.append(":").append(gotoURI.getPort());
			sb.append(gotoURI.getPath()).append("?");

			try {
				sb.append(infoShuttle.userPostData);
				infoShuttle.siteURL = new URL(sb.toString());
			}
			catch (MalformedURLException e) {
				throw new IllegalArgumentException("Cannot compose an URL (A): " + sb.toString());
			}
		}
		else {
			try {
				infoShuttle.siteURL = gotoURI.toURL();
			}
			catch (MalformedURLException e) {
				throw new IllegalArgumentException("Cannot compose an URL (B): " + gotoURI.toString());
			}
		}

		// filter http headers
		int index = 0;
		while (index < infoShuttle.userHeaderNames.size()) {
			String name = (String) infoShuttle.userHeaderNames.get(index);

			int pos = findString(name, ALL_HEADERS, AllowedToRequest, true);
			if (pos == -1 || (pos == HH_CONTENT_TYPE && !infoShuttle.postToSite)) {
				infoShuttle.userHeaderNames.remove(index);
				infoShuttle.userHeaderValues.remove(index);
				continue;
			}

			index++;
		}
		
		// always set Accept-Encoding to empty
		infoShuttle.userHeaderNames.add(ALL_HEADERS[HH_ACCEPT_ENCODING]);
		infoShuttle.userHeaderValues.add("");

		// compose cookie for target site
		String serverCookieStr = cookieMgr.getServerCookieString(infoShuttle.sessionID, infoShuttle.siteURL.getHost(), infoShuttle.siteURL.getPath());
		
		if (serverCookieStr != null) {
			infoShuttle.userHeaderNames.add("cookie");
			infoShuttle.userHeaderValues.add(serverCookieStr);
		}
	}

	static public void convertOutgoingHeaders(ParameterShuttle infoShuttle, boolean keepEncoding) {
		int index = 0;
		while (index < infoShuttle.siteHeaderNames.size()) {
			String name = (String) infoShuttle.siteHeaderNames.get(index);
			int headerIndex =
				findString(name, ALL_HEADERS, AllowedToResponse, true);

			if (headerIndex == -1 || (!keepEncoding	&& (headerIndex == HH_CONTENT_ENCODING || headerIndex == HH_CONTENT_MD5))) {
				infoShuttle.siteHeaderNames.remove(index);
				infoShuttle.siteHeaderValues.remove(index);
				continue;
			}

			String value = (String) infoShuttle.siteHeaderValues.get(index);

			if (headerIndex == HH_SET_COOKIE) {
				cookieMgr.addCookie(
					infoShuttle.sessionID,
					infoShuttle.userIP + "-" + infoShuttle.userID,
					new ClientCookie(
						value,
						infoShuttle.siteURL.getHost(),
						infoShuttle.siteURL.getPath()));

				infoShuttle.siteHeaderNames.remove(index);
				infoShuttle.siteHeaderValues.remove(index);
				continue;
			}
			else if (headerIndex == HH_LOCATION || headerIndex == HH_CONTENT_LOCATION) {
				IntQueue htmlQ = new IntQueue(value.length() + 10, 10);
				htmlQ.push(value);

				UrlBuilder urlBuilder = new UrlBuilder();
				urlBuilder.setBaseURL(infoShuttle.siteURL);

				StringBuffer sbuf = urlBuilder.composeURLString(infoShuttle, htmlQ, 0, value.length(), false);
				if (sbuf != null)
					infoShuttle.siteHeaderValues.set(index, sbuf.toString());
			}

			index++;
		}
	}
	
	// static varibles for HTML conversion service
	static private final int TAG_HEAD = 100;
	static private final int TAG_NAME = TAG_HEAD + 1;
	static private final int AFTER_TAG_NAME = TAG_HEAD + 2;
	static private final int ATTR = TAG_HEAD + 3;
	static private final int EQUAL = TAG_HEAD + 4;
	static private final int LEFT_QUOTE = TAG_HEAD + 5;
	static private final int VALUE = TAG_HEAD + 6;
	static private final int RIGHT_QUOTE = TAG_HEAD + 7;
	static private final int AFTER_PAIR = TAG_HEAD + 8;
	static private final int TAG_TAIL = TAG_HEAD + 9;

	static public final int OUTPUT = 10;
	static private final int DISCARD = OUTPUT + 1;
	static private final int DIRECT_URI = OUTPUT + 2;
	static private final int META_URI = OUTPUT + 3;
	static private final int BASE_URI = OUTPUT + 4;
	static private final int FORM_GET_URI = OUTPUT + 5;
	static private final int FORM_POST_URI = OUTPUT + 6;
	static private final int METHOD_POST = OUTPUT + 7;

	// class varibles for HTML conversion service
	// refer to queues in HtmlInputStream
	private IntQueue htmlQueue;
	private IntQueue typeQueue;
	private IntQueue sizeQueue;

	// base UrlBuilder
	private UrlBuilder baseUrlBuilder = new UrlBuilder();
	
	private ParameterShuttle infoShuttle;

	// HTML conversion service for HtmlInputStream
	// init() -- get connected to a HtmlInputStream
	public void init(ParameterShuttle infoShuttle, URL baseURL, IntQueue htmlQueue, IntQueue typeQueue, IntQueue sizeQueue) {
		baseUrlBuilder.setBaseURL(baseURL);
		
		this.infoShuttle = infoShuttle;
		this.htmlQueue = htmlQueue;
		this.typeQueue = typeQueue;
		this.sizeQueue = sizeQueue;
	}

	public void convertHtmlTag() {
		switch (typeQueue.get()) {
			case FORM_GET_URI :
			case DIRECT_URI :
			case META_URI :
				convertDirectURI(false);
				break;

			case FORM_POST_URI :
				convertDirectURI(true);
				break;

			case METHOD_POST :
				insertContent(" method=\"post\"");
				break;

			case DISCARD :
				htmlQueue.pop(sizeQueue.pop());
				typeQueue.pop();
				break;

			case BASE_URI :
				convertBaseURI();
				break;

			default :
				typeQueue.set(OUTPUT);
				break;
		}
	}

	private void convertDirectURI(boolean addPost) {
		int uriLen = sizeQueue.get();
		StringBuffer newUrlBuf = baseUrlBuilder.composeURLString(infoShuttle, htmlQueue, 0, uriLen, addPost);

		if (newUrlBuf == null) {
			typeQueue.set(OUTPUT);
			return;
		}

		htmlQueue.pop(uriLen);
		htmlQueue.push(newUrlBuf);

		typeQueue.set(OUTPUT);
		sizeQueue.set(newUrlBuf.length());
	}

	private void convertBaseURI() {
		try {
			URL baseURL = new URL(htmlQueue.getString(sizeQueue.get()).trim());
			baseUrlBuilder.setBaseURL(baseURL);
		}
		catch (Exception e) {
		}

		typeQueue.set(DISCARD);
	}

	private void insertContent(String content) {
		htmlQueue.push(content);
		typeQueue.set(OUTPUT);
		sizeQueue.set(sizeQueue.get() + content.length());
	}

	public void markHtmlTag() {
		// form URI
		if (FORM_URI_TAG_LEN == sizeQueue.get(1) && htmlQueue.compareString(FORM_URI_TAG, sizeQueue.get(0), sizeQueue.get(1)) == 0) {
			markFormURITag();
			return;
		}

		// direct URIes
		int tagIndex =
			findString(DIRECT_URI_TAGS, sizeQueue.get(0), sizeQueue.get(1));
		if (tagIndex > -1) {
			markDirectURITag(tagIndex);
			return;
		}

		// base URI
		if (BASE_URI_TAG_LEN == sizeQueue.get(1) && htmlQueue.compareString(BASE_URI_TAG, sizeQueue.get(0), sizeQueue.get(1)) == 0) {
			markBaseURITag();
			return;
		}

		// meta URI
		if (META_URI_TAG_LEN == sizeQueue.get(1) && htmlQueue.compareString(META_URI_TAG, sizeQueue.get(0), sizeQueue.get(1)) == 0) {
			markMetaURITag();
			return;
		}

		// default: all other tags
		typeQueue.clear();
		sizeQueue.clear();

		typeQueue.append(OUTPUT);
		sizeQueue.append(htmlQueue.size());

		return;
	}

	// predefined TAGs and ATTRIBUTIONs
	static private final String[] DIRECT_URI_TAGS = {
		"a",
		"applet",
		"area",
		"blockquote",
		"body",
		"del",
		"embed",
		"frame",
		"head",
		"iframe",
		"img",
		"input",
		"ins",
		"item",
		"link",
		"object",
		"q",
		"script",
		"table",
		"td"
	};

	static private final String[][] DIRECT_URI_ATTRS = {
		{ "href" },
		{ "codebase" },
		{ "href" },
		{ "cite" },
		{ "background" },
		{ "cite" },
		{ "pluginspage", "src" },
		{ "longdesc", "src" },
		{ "profile" },
		{ "longdesc", "src" },
		{ "longdesc", "src", "usemap" },
		{ "src", "usemap" },
		{ "cite" },
		{ "href" },
		{ "href" },
		{ "classid", "codebase", "data", "usemap" },
		{ "cite" },
		{ "src" },
		{ "background" },
		{ "background" }
	};

	private void markDirectURITag(int tagIndex) {
		int bufSize = 0;
		int totSize = 0;

		int segSize = 0;
		int segType = TAG_HEAD;

		int attrCount = 0;
		int valueTrigger = 0;

		while (segType != TAG_TAIL) {
			segType = typeQueue.pop();
			segSize = sizeQueue.pop();

			if (segType == ATTR) {
				attrCount++;
				if (findString(DIRECT_URI_ATTRS[tagIndex], totSize, segSize) >= 0)
					valueTrigger = attrCount;

				bufSize += segSize;
			}
			else if (segType == VALUE && valueTrigger == attrCount) {
				typeQueue.append(OUTPUT);
				sizeQueue.append(bufSize);

				typeQueue.append(DIRECT_URI);
				sizeQueue.append(segSize);

				bufSize = 0;
			}
			else {
				bufSize += segSize;
			}

			totSize += segSize;
		}

		typeQueue.append(OUTPUT);
		sizeQueue.append(bufSize);
	}

	static private final String BASE_URI_TAG = "base";
	static private final int BASE_URI_TAG_LEN = 4;
	static private final String BASE_URI_ATTR = "href";

	private void markBaseURITag() {
		int bufSize = 0;
		int totSize = 0;

		int segSize = 0;
		int segType = TAG_HEAD;

		int attrCount = 0;
		int valueTrigger = 0;

		while (segType != TAG_TAIL) {
			segType = typeQueue.pop();
			segSize = sizeQueue.pop();

			if (segType == ATTR) {
				attrCount++;
				if (segSize == BASE_URI_ATTR.length()
					&& htmlQueue.compareString(BASE_URI_ATTR, totSize, segSize)
						== 0)
					valueTrigger = attrCount;

				bufSize += segSize;
			}
			else if (segType == VALUE && valueTrigger == attrCount) {
				typeQueue.append(DISCARD);
				sizeQueue.append(bufSize);

				typeQueue.append(BASE_URI);
				sizeQueue.append(segSize);

				bufSize = 0;
			}
			else {
				bufSize += segSize;
			}

			totSize += segSize;
		}

		typeQueue.append(DISCARD);
		sizeQueue.append(bufSize);
	}

	static private final String META_URI_TAG = "meta";
	static private final int META_URI_TAG_LEN = 4;
	static private final String META_FLAG_ATTR = "http-equiv";
	static private final String META_CONTENT_ATTR = "content";

	private void markMetaURITag() {
		int bufSize = 0;
		int contentSegIndex = 0;
		int contentSegSize = 0;

		// identify whether both HTTP-EQUIV and CONTENT fields appear together
		boolean equivAttrFound = false;
		boolean contentAttrFound = false;
		for (int k = 0, pos = 0;
			k < sizeQueue.size();
			pos += sizeQueue.get(k++)) {
			if (typeQueue.get(k) != ATTR)
				continue;

			if (!equivAttrFound && htmlQueue.compareString(META_FLAG_ATTR, pos, sizeQueue.get(k)) == 0) {
				equivAttrFound = true;
				if (contentAttrFound)
					break;
			}

			if (!contentAttrFound && htmlQueue.compareString(META_CONTENT_ATTR, pos, sizeQueue.get(k)) == 0) {
				bufSize = pos + sizeQueue.get(k);
				contentSegIndex = k + 1;
				if (typeQueue.get(contentSegIndex) != EQUAL)
					break;

				bufSize += sizeQueue.get(contentSegIndex++);
				if (typeQueue.get(contentSegIndex) == LEFT_QUOTE)
					bufSize += sizeQueue.get(contentSegIndex++);

				if (typeQueue.get(contentSegIndex) != VALUE)
					break;

				contentSegSize = sizeQueue.get(contentSegIndex);

				contentAttrFound = true;
				if (equivAttrFound)
					break;
			}
		}

		if (!equivAttrFound || !contentAttrFound) {
			typeQueue.clear();
			sizeQueue.clear();
			typeQueue.append(OUTPUT);
			sizeQueue.append(htmlQueue.size());
			return;
		}

		// discard all segments that are before the VALUE of the CONTENT
		typeQueue.pop(contentSegIndex + 1);
		sizeQueue.pop(contentSegIndex + 1);

		// analyse the VALUE of the CONTENT
		int totSize = bufSize;
		int restLen = contentSegSize;
		int pos = 0;

		while (restLen > 0) {
			if ((pos = htmlQueue.indexOf("url=", totSize, restLen)) > -1)
				pos += 4;
			else
				pos = htmlQueue.indexOf("http://", totSize, restLen);

			if (pos == -1) {
				bufSize += restLen;
				break;
			}

			int gapLen = pos - totSize;
			bufSize += gapLen;

			int uriLen = restLen - gapLen;
			for (int k = pos, kEnd = totSize + restLen; k < kEnd; k++) {
				char ch = htmlQueue.getChar(k);
				if (ch == '\''
					|| ch == '\"'
					|| Character.isWhitespace(ch)
					|| ch == '&') {
					uriLen = k - pos;
					break;
				}
			}

			if (uriLen > 0) {
				// mark OUTPUT
				typeQueue.append(OUTPUT);
				sizeQueue.append(bufSize);

				// mark META_URI
				typeQueue.append(META_URI);
				sizeQueue.append(uriLen);

				// adjust pointers
				bufSize = 0;
			}

			totSize += gapLen + uriLen;
			restLen -= gapLen + uriLen;
		}

		// merge all segments that are behind the VALUE of the CONTENT to OUTPUT
		do {
			bufSize += sizeQueue.pop();
		} while (typeQueue.pop() != TAG_TAIL);

		typeQueue.append(OUTPUT);
		sizeQueue.append(bufSize);
	}

	static private final String FORM_URI_TAG = "form";
	static private final int FORM_URI_TAG_LEN = 4;
	static private final String FORM_ACTION_ATTR = "action";
	static private final String FORM_METHOD_ATTR = "method";

	private void markFormURITag() {
		boolean isPost = false;

		// first scan to find out whether ACTION and METHOD exist
		boolean actionAttrFound = false;

		for (int k = 0, kSize = typeQueue.size(), totSize = 0;
			k < kSize;
			totSize += sizeQueue.get(k++)) {
			if (typeQueue.get(k) == ATTR
				&& typeQueue.get(k + 1) == EQUAL
				&& (typeQueue.get(k + 2) == VALUE
					|| (typeQueue.get(k + 2) == LEFT_QUOTE
						&& typeQueue.get(k + 3) == VALUE))) {
				if (htmlQueue.compareString(FORM_ACTION_ATTR, totSize, sizeQueue.get(k)) == 0) {
					actionAttrFound = true;
					if (isPost)
						break;
				} else if (
					htmlQueue.compareString(
						FORM_METHOD_ATTR,
						totSize,
						sizeQueue.get(k))
						== 0) {
					int startPos =
						totSize + sizeQueue.get(k) + sizeQueue.get(k + 1);
					int endPos = startPos + sizeQueue.get(k + 2);
					if (typeQueue.get(k + 2) == LEFT_QUOTE) {
						startPos += sizeQueue.get(k + 2);
						endPos += sizeQueue.get(k + 3);
					}

					if (htmlQueue.indexOf("post", startPos, endPos - startPos)
						> -1) {
						isPost = true;
						if (actionAttrFound)
							break;
					}
				}
			}
		}

		if (!actionAttrFound) {
			typeQueue.clear();
			sizeQueue.clear();
			typeQueue.append(OUTPUT);
			sizeQueue.append(htmlQueue.size());
			return;
		}

		// second scan to mark tags
		int totSize = 0;
		int bufSize = 0;

		int segType = 0;
		int segSize = 0;

		do {
			segType = typeQueue.pop();
			segSize = sizeQueue.pop();

			if (segType == ATTR) {
				// convert ATTRs
				if (htmlQueue.compareString(FORM_ACTION_ATTR, totSize, segSize)
					== 0) {
					// merge ATTR, EQUAL, LEFT_QUOTE
					do {
						bufSize += segSize;
						totSize += segSize;

						segType = typeQueue.pop();
						segSize = sizeQueue.pop();
					} while (segType != VALUE);

					// output all segments that are before VALUE
					typeQueue.append(OUTPUT);
					sizeQueue.append(bufSize);

					bufSize = 0;

					// value of action
					typeQueue.append(isPost ? FORM_POST_URI : FORM_GET_URI);
					sizeQueue.append(segSize);

					totSize += segSize;
				}
				else if (
					!isPost
						&& htmlQueue.compareString(
							FORM_METHOD_ATTR,
							totSize,
							segSize)
							== 0) {
					// output all segments that are before ATTR of method
					typeQueue.append(OUTPUT);
					sizeQueue.append(bufSize);

					// merge ATTR
					bufSize = segSize;
					totSize += segSize;

					while (typeQueue.get() != TAG_TAIL
						&& typeQueue.get() != ATTR) {
						segType = typeQueue.pop();
						segSize = sizeQueue.pop();

						bufSize += segSize;
						totSize += segSize;
					}

					// DISCARD ATTR, EQUAL and VALUE of method
					typeQueue.append(DISCARD);
					sizeQueue.append(bufSize);

					// reset bufSize for rest of segments
					bufSize = 0;
				} else {
					totSize += segSize;
					bufSize += segSize;
				}
			}
			else if (segType == TAG_TAIL) {
				// convert TAG_TAIL
				if (!isPost) {
					if (bufSize > 0) {
						typeQueue.append(OUTPUT);
						sizeQueue.append(bufSize);

						bufSize = 0;
					}

					typeQueue.append(METHOD_POST);
					sizeQueue.append(0);
				}

				typeQueue.append(OUTPUT);
				sizeQueue.append(bufSize + segSize);
			}
			else {
				// convert segments that are other than ATTRs and TAG_TAIL
				totSize += segSize;
				bufSize += segSize;
			}
		}
		while (segType != TAG_TAIL);
	}

	private int findString(String[] tags, int pos, int len) {
		char firstChar = htmlQueue.getLowerCase(pos);

		boolean found = false;
		int k = 0;
		for (; k < tags.length && tags[k].charAt(0) < firstChar; k++)
			continue;

		for (; k < tags.length && firstChar == tags[k].charAt(0); k++) {
			if (len != tags[k].length())
				continue;

			int diff = htmlQueue.compareString(tags[k], pos, len);
			if (diff == 0) {
				found = true;
				break;
			} else if (diff < 0)
				break;
		}

		return found ? k : -1;
	}

	static private final int TAG_START_CHAR = (int) '<';

	public int fillBuffer(InputStream in) throws IOException {
		int rt = in.read();
		if (rt != TAG_START_CHAR)
			return rt;

		htmlQueue.append(rt);
		rt = parseTag(in);

		switch (rt) {
			case START_TAG :
				markHtmlTag();
				break;

			case END_TAG :
				typeQueue.clear();
				sizeQueue.clear();

				typeQueue.append(OUTPUT);
				sizeQueue.append(htmlQueue.size());
				break;

			case INVALID_TAG :
			case NOT_A_TAG :
			default :
				typeQueue.set(OUTPUT);
				break;
		}

		return -2;
	}

	static private final int NOT_A_TAG = -10;
	static private final int INVALID_TAG = -11;
	static private final int START_TAG = -98;
	static private final int END_TAG = -99;

	static private final int BEFORE_EQUAL = 500;
	static private final int AFTER_EQUAL = 501;

	static private final int NO_QUOTE = 600;
	static private final int DOUBLE_QUOTE = 601; //    "
	static private final int DQ_BS = 602; //    \"
	static private final int DQ_DEC = 603; //    &#34;
	static private final int DQ_HEX = 604; //    &#x22;
	static private final int DQ_WORD = 605; //    &quot;
	static private final int SINGLE_QUOTE = 606; //    '
	static private final int SQ_BS = 607; //    \'
	static private final int SQ_DEC = 608; //    &#39;
	static private final int SQ_HEX = 609; //    &#x27;

	// private int parseTag(InputStream in)
	//    return values:
	//        NOT_A_TAG:        all chars are stored in htmlQueue, and one segment is stored in segQueues
	//        INVALID_TAG:    all chars are stored in htmlQueue, and one segment is stored in segQueues
	//        START_TAG:        all chars are stored in htmlQueue, and ALL segments is stored in segQueues
	//        END_TAG:        all chars are stored in htmlQueue, and ALL segments is stored in segQueues
	private int parseTag(InputStream in) throws IOException {
		// handle segType == TAG_HEAD, to parse start delimiter: < or </
		int goodTagType = START_TAG;
		int htmlOrigSize = htmlQueue.size() - 1;

		int segSize = 1;
		int segType = TAG_HEAD;

		int ci = in.read();
		char ch = (char) ci;

		if (ch == '/') {
			goodTagType = END_TAG;
			htmlQueue.append(ci);
			segSize++;

			ci = in.read();
			ch = (char) ci;
		}

		if (!Character.isLetter(ch) || ci == -1) {
			if (ci > -1) {
				htmlQueue.append(ci);
				segSize++;
			}

			typeQueue.append(NOT_A_TAG);
			sizeQueue.append(segSize);
			return NOT_A_TAG;
		}

		int segOrigSize = typeQueue.size();
		typeQueue.append(TAG_HEAD);
		sizeQueue.append(segSize);

		segSize = 0;
		segType = TAG_NAME;

		// handle segType == TAG_NAME, to parse tag name
		while (segType == TAG_NAME) {
			htmlQueue.append(ci);
			segSize++;

			ci = in.read();
			if (ci == -1) {
				segType = INVALID_TAG;
				break;
			}

			ch = (char) ci;
			if (!Character.isLetterOrDigit(ch)) {
				typeQueue.append(TAG_NAME);
				sizeQueue.append(segSize);

				segSize = 0;
				segType = AFTER_TAG_NAME;
			}
		}

		// handle segType == AFTER_TAG_NAME, to parse WHITESPACES after TAG NAME
		if (segType == AFTER_TAG_NAME) {
			if (Character.isWhitespace(ch))
				segType = AFTER_TAG_NAME;
			else if (ch == '>' || ch == '/')
				segType = TAG_TAIL;
			else
				segType = INVALID_TAG;
		}

		while (segType == AFTER_TAG_NAME) {
			htmlQueue.append(ci);
			segSize++;

			ci = in.read();
			if (ci == -1) {
				segType = INVALID_TAG;
				break;
			}

			ch = (char) ci;
			if (!Character.isWhitespace(ch)) {
				typeQueue.append(AFTER_TAG_NAME);
				sizeQueue.append(segSize);

				segSize = 0;
				segType = ATTR;
			}
		}

		// handle segType == ATTR, to parse ATTR/VALUE pairs
		while (segType == ATTR) {
			// TAG_TAIL? jump out
			if (ch == '>' || ch == '/') {
				segType = TAG_TAIL;
				break;
			}

			// handle segType == ATTR, to parse ATTR
			if (!Character.isLetter(ch)) {
				segType = INVALID_TAG;
				break;
			}

			while (segType == ATTR) {
				htmlQueue.append(ci);
				segSize++;

				ci = in.read();
				if (ci == -1) {
					segType = INVALID_TAG;
					break;
				}

				ch = (char) ci;
				if (ch == '='
					|| ch == '>'
					|| ch == '/'
					|| Character.isWhitespace(ch)) {
					typeQueue.append(ATTR);
					sizeQueue.append(segSize);

					segSize = 0;
					if (ch == '>' || ch == '/')
						segType = TAG_TAIL;
					else
						segType = EQUAL;
				}
			}

			// parse equal sign and whitspaces around it
			if (segType != EQUAL)
				continue;

			// following three cases are allowed:
			// 1: ... ATTR ... (no equal sign nor value)
			// 2: ... ATTR = ... (with equal sign, but no value)
			// 3: ... ATTR = VALUE ... (with both equal sign and value)
			int spacePosition = ch == '=' ? AFTER_EQUAL : BEFORE_EQUAL;

			while (segType == EQUAL) {
				htmlQueue.append(ci);
				segSize++;

				ci = in.read();
				if (ci == -1) {
					segType = INVALID_TAG;
					break;
				}

				ch = (char) ci;
				if (ch == '=' && spacePosition == BEFORE_EQUAL) {
					spacePosition = AFTER_EQUAL;
				} else if (!Character.isWhitespace(ch)) {
					if (spacePosition == BEFORE_EQUAL) {
						typeQueue.append(AFTER_PAIR);
						sizeQueue.append(segSize);

						segSize = 0;
						segType = ATTR;
					} else {
						typeQueue.append(EQUAL);
						sizeQueue.append(segSize);

						segSize = 0;
						if (ch == '>')
							segType = TAG_TAIL;
						else
							segType = VALUE;
					}
				}
			}

			// parse attribute QUOTED value
			if (segType != VALUE)
				continue;

			// read first part of value into htmlQueue, stopped by '>', '/', or whitespaces
			int segPos = htmlQueue.size();
			while (segType == VALUE) {
				htmlQueue.append(ci);
				segSize++;

				ci = in.read();
				ch = (char) ci;
				if (ci == -1)
					segType = INVALID_TAG;
				else if (ch == '/' || ch == '>' || Character.isWhitespace(ch))
					break;
			}
			if (segType == INVALID_TAG)
				break;

			// try to find left quote in htmlQueue
			int leftQuoteType = NO_QUOTE;
			int quoteSize = 0;

			char htmlCh = htmlQueue.getChar(segPos);

			if (htmlCh == '\"') {
				leftQuoteType = DOUBLE_QUOTE;
				quoteSize = 1;
			} else if (htmlCh == '\'') {
				leftQuoteType = SINGLE_QUOTE;
				quoteSize = 1;
			} else if (htmlCh == '\\' && segSize > 1) {
				htmlCh = htmlQueue.getChar(segPos + 1);
				if (htmlCh == '\"') {
					leftQuoteType = DQ_BS;
					quoteSize = 2;
				} else if (htmlCh == '\'') {
					leftQuoteType = SQ_BS;
					quoteSize = 2;
				}
			} else if (htmlCh == '&' && segSize > 3) {
				if (htmlQueue.compareString("&#34", segPos) == 0) {
					leftQuoteType = DQ_DEC;
					if (segSize > 4 && htmlQueue.getChar(segPos + 4) == ';')
						quoteSize = 5;
					else
						quoteSize = 4;
				} else if (htmlQueue.compareString("&#x22", segPos) == 0) {
					leftQuoteType = DQ_HEX;
					if (segSize > 5 && htmlQueue.getChar(segPos + 5) == ';')
						quoteSize = 6;
					else
						quoteSize = 5;
				} else if (htmlQueue.compareString("&quot", segPos) == 0) {
					leftQuoteType = DQ_WORD;
					if (segSize > 5 && htmlQueue.getChar(segPos + 5) == ';')
						quoteSize = 6;
					else
						quoteSize = 5;
				} else if (htmlQueue.compareString("&#39", segPos) == 0) {
					leftQuoteType = SQ_DEC;
					if (segSize > 4 && htmlQueue.getChar(segPos + 4) == ';')
						quoteSize = 5;
					else
						quoteSize = 4;
				} else if (htmlQueue.compareString("&#x27", segPos) == 0) {
					leftQuoteType = SQ_HEX;
					if (segSize > 5 && htmlQueue.getChar(segPos + 5) == ';')
						quoteSize = 6;
					else
						quoteSize = 5;
				}
			}

			if (leftQuoteType == NO_QUOTE) {
				// LEFT QUOTE not found, read rest of VALUE until a '>' or a whitespace
				while (ch != '>' && !Character.isWhitespace(ch)) {
					htmlQueue.append(ci);
					segSize++;

					ci = in.read();
					ch = (char) ci;
					if (ci == -1) {
						segType = INVALID_TAG;
						break;
					}
				}
				if (segType == INVALID_TAG)
					break;

				typeQueue.append(VALUE);
				sizeQueue.append(segSize);

				segSize = 0;
				if (ch == '>') {
					segType = TAG_TAIL;
					break;
				} else
					segType = AFTER_PAIR;
			} else {
				// save LEFT_QUOTE and read next part if no other chars follow the LEFT_QUOTE
				typeQueue.append(LEFT_QUOTE);
				sizeQueue.append(quoteSize);

				segSize -= quoteSize;

				if (segSize == 0) {
					if (ch == '>') {
						segType = TAG_TAIL;
						break;
					}

					while (true) {
						htmlQueue.append(ci);
						segSize++;

						ci = in.read();
						if (ci == -1) {
							segType = INVALID_TAG;
							break;
						}

						ch = (char) ci;
						if (ch == '/'
							|| ch == '>'
							|| Character.isWhitespace(ch))
							break;
					}
					if (segType == INVALID_TAG)
						break;
				}
			}

			// LEFT QUOTE presented: parse VALUE after left quote
			while (segType == VALUE) {
				// check whether right quote presents
				segPos = htmlQueue.size() - 1;
				htmlCh = htmlQueue.getLowerCase(segPos);
				quoteSize = 0;

				if (leftQuoteType == DOUBLE_QUOTE) {
					if (htmlCh == '\"')
						quoteSize = 1;
				} else if (leftQuoteType == SINGLE_QUOTE) {
					if (htmlCh == '\'')
						quoteSize = 1;
				} else if (leftQuoteType == DQ_BS) {
					if (segSize > 1
						&& htmlQueue.getChar(segPos - 1) == '\\'
						&& htmlCh == '\"')
						quoteSize = 2;
				} else if (leftQuoteType == SQ_BS) {
					if (segSize > 1
						&& htmlQueue.getChar(segPos - 1) == '\\'
						&& htmlCh == '\'')
						quoteSize = 2;
				} else if (leftQuoteType == DQ_DEC) {
					if (htmlCh == ';') {
						if (segSize > 4
							&& htmlQueue.compareString("&#34", segPos - 4) == 0)
							quoteSize = 5;
					} else if (htmlCh == '4') {
						if (segSize > 3
							&& htmlQueue.compareString("&#34", segPos - 3) == 0)
							quoteSize = 4;
					}
				} else if (leftQuoteType == SQ_DEC) {
					if (htmlCh == ';') {
						if (segSize > 4
							&& htmlQueue.compareString("&#39", segPos - 4) == 0)
							quoteSize = 5;
					} else if (htmlCh == '9') {
						if (segSize > 3
							&& htmlQueue.compareString("&#39", segPos - 3) == 0)
							quoteSize = 4;
					}
				} else if (leftQuoteType == DQ_HEX) {
					if (htmlCh == ';') {
						if (segSize > 5
							&& htmlQueue.compareString("&#x22", segPos - 5) == 0)
							quoteSize = 6;
					} else if (htmlCh == '2') {
						if (segSize > 4
							&& htmlQueue.compareString("&#x22", segPos - 4) == 0)
							quoteSize = 5;
					}
				} else if (leftQuoteType == SQ_HEX) {
					if (htmlCh == ';') {
						if (segSize > 5
							&& htmlQueue.compareString("&#x27", segPos - 5) == 0)
							quoteSize = 6;
					} else if (htmlCh == '7') {
						if (segSize > 4
							&& htmlQueue.compareString("&#x27", segPos - 4) == 0)
							quoteSize = 5;
					}
				} else if (leftQuoteType == DQ_WORD) {
					if (htmlCh == ';') {
						if (segSize > 5
							&& htmlQueue.compareString("&quot", segPos - 5) == 0)
							quoteSize = 6;
					} else if (htmlCh == 't') {
						if (segSize > 4
							&& htmlQueue.compareString("&quot", segPos - 4) == 0)
							quoteSize = 5;
					}
				}

				// yes, RIGHT QUOTE found, save both VALUE and RIGHT QUOTE
				if (quoteSize > 0) {
					segSize -= quoteSize;
					if (segSize > 0) {
						typeQueue.append(VALUE);
						sizeQueue.append(segSize);
					}

					typeQueue.append(RIGHT_QUOTE);
					sizeQueue.append(quoteSize);

					segSize = 0;
					segType = AFTER_PAIR;
				}

				// no, RIGHT QUOTE not found, read next part and loop
				while (segType == VALUE) {
					htmlQueue.append(ci);
					segSize++;

					ci = in.read();
					ch = (char) ci;
					if (ci == -1)
						segType = INVALID_TAG;
					else if (
						ch == '/' || ch == '>' || Character.isWhitespace(ch))
						break;
				}

				if (segSize > 1024)
					segType = INVALID_TAG;
			}

			// parse whitespaces after a pair of attribute name and value
			if (segType == AFTER_PAIR && !Character.isWhitespace(ch)) {
				segType = ATTR;
				continue;
			}

			while (segType == AFTER_PAIR) {
				htmlQueue.append(ci);
				segSize++;

				ci = in.read();
				ch = (char) ci;
				if (ci == -1)
					segType = INVALID_TAG;
				else if (!Character.isWhitespace(ch)) {
					typeQueue.append(AFTER_PAIR);
					sizeQueue.append(segSize);

					segSize = 0;
					segType = ATTR;
				}
			}
		}

		// handle segType == TAG_TAIL, to parse end delimiter: '>' or '/>'
		if (segType == TAG_TAIL) {
			if (ch == '/') {
				htmlQueue.append(ci);
				segSize++;

				ci = in.read();
				ch = (char) ci;
			}

			if (ch == '>') {
				htmlQueue.append(ci);
				segSize++;

				typeQueue.append(TAG_TAIL);
				sizeQueue.append(segSize);

				segSize = 0;
				segType = goodTagType;
			} else
				segType = INVALID_TAG;
		}

		// handle segType == INVALID_TAG
		if (segType == INVALID_TAG) {
			if (ci > -1)
				htmlQueue.append(ci);

			int segSizeDiff = typeQueue.size() - segOrigSize;
			if (segSizeDiff > 0) {
				typeQueue.popLast(segSizeDiff);
				sizeQueue.popLast(segSizeDiff);
			}

			typeQueue.append(INVALID_TAG);
			sizeQueue.append(htmlQueue.size() - htmlOrigSize);
		}

		return segType;
	}
}