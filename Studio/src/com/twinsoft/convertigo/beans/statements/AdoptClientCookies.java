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

package com.twinsoft.convertigo.beans.statements;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HttpState;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.CookiesUtils;

public class AdoptClientCookies extends Statement {
	private static final long serialVersionUID = -8541325182087905783L;

	private static final Pattern regexpHost = Pattern.compile(".*(\\..*?\\..*?)");

	public AdoptClientCookies() {
		super();
	}

	@Override
	public String toString() {
		String text = this.getComment();
		return "Adopt client cookies" + (!text.equals("") ? " // " + text : "");
	}

	@Override
	public boolean execute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable) {
			HttpConnector connector = this.getConnector();
			com.twinsoft.convertigo.engine.Context context = this.getParentTransaction().context;
			if (connector.handleCookie) {
				HttpState httpState = context.httpState;
				if (httpState == null) {
					connector.resetHttpState(context);
					httpState = context.httpState;
				}

				Map<String, List<String>> requestHeaders = context.getRequestHeaders();
				List<String> lCookies = requestHeaders.get("cookie");
				if (lCookies != null) {
					for (String sCookies : lCookies) {
						Engine.logBeans.debug("(AdoptClientCookies) adding cookies: " + sCookies);

						String[] cookies = sCookies.split(";");
						for (String cookie : cookies) {
							String[] cookieParams = cookie.split("=");
							String name = cookieParams[0].trim();
							String value = (cookieParams.length > 1 ? cookieParams[1] : "");
							String domain;
							try {
								String url = context.httpServletRequest.getRequestURL().toString();
								Engine.logBeans.debug("(AdoptClientCookies) requested url: " + url);
								domain = new URL(url).getHost();
								Matcher matcher = regexpHost.matcher(domain);
								if (matcher.matches()) {
									domain = matcher.group(1);
									Engine.logBeans.debug("(AdoptClientCookies) sub domain match: " + domain);
								} else {
									Engine.logBeans.debug("(AdoptClientCookies) using default domain: "
											+ domain);
								}

								CookiesUtils.addCookie(httpState, domain, name, value, "/", new Date(
										Long.MAX_VALUE), false);
							} catch (MalformedURLException e) {
								Engine.logBeans.error("(AdoptClientCookies) unable to add cookie: " + cookie, e);
							}
						}
					}
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public String toJsString() {
		return "";
	}
}