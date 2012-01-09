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

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.proxy.util.IntQueue;

public class UrlBuilder {
	// base URL in two parts of ROOT and FILE
//	private String baseScheme;
//	private String baseHost;
	private String basePath;

	public void setBaseURL(URL url) {
//		baseScheme = url.getProtocol();
//		baseHost = baseScheme + "://" + url.getHost() + (url.getPort() == -1 ? "" : ":" + url.getPort());
		basePath = (url.getPath().length() == 0 ? "/" : url.getPath()) + (url.getQuery() == null ? "" : "?" + url.getQuery());
		try {
			basePath = URLEncoder.encode(basePath, "UTF-8");
		} catch (UnsupportedEncodingException e) {
		}
	}

	static private final int SCHEME_HTTP = 1; // http:(*) or https:(*)
	static private final int SCHEME_OTHER = 2; // mailto: or ftp:// or others
	static private final int ABSOLUTE_HOST = 3; // //****
	static private final int ABSOLUTE_PATH = 4; // /****
	static private final int OTHER = 5;

	private StringBuffer buf = new StringBuffer(512);

	// return null means no need to change the URI in the htmlQueue
	// this method does not update htmlQueue
	public StringBuffer composeURLString(ParameterShuttle infoShuttle, IntQueue htmlQueue, int pos, int len, boolean addPost) {
		// trim spaces
		while (htmlQueue.getChar(pos) == ' ' && len > 0) {
			pos++;
			len--;
		}
		while (htmlQueue.getChar(pos + len - 1) == ' ' && len > 0) {
			len--;
		}
		if (len <= 0)
			return null;

		// logic starts
		char ch = htmlQueue.getChar(pos);
		if (ch == '#')
			return null;

		// determine the type of incoming URI
		int type = OTHER;

		if (ch == '/') {
			if (len > 1 && htmlQueue.getChar(pos + 1) == '/')
				type = ABSOLUTE_HOST;
			else
				type = ABSOLUTE_PATH;
		}
		else {
			boolean colonFound = false;
			boolean allGoodChars = true;
			int schemeLen = 0;

			for (int p = pos;
				allGoodChars && !colonFound && schemeLen < len;
				schemeLen++, p++) {
				ch = htmlQueue.getChar(p);
				colonFound = ch == ':';
				if (!colonFound)
					allGoodChars = (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z');
			}

			if (colonFound && allGoodChars) {
				type = SCHEME_OTHER;
				if (schemeLen == 5) {
					if (htmlQueue.getLowerCase(pos) == 'h'
						&& htmlQueue.getLowerCase(pos + 1) == 't'
						&& htmlQueue.getLowerCase(pos + 2) == 't'
						&& htmlQueue.getLowerCase(pos + 3) == 'p')
						type = SCHEME_HTTP;
				}
				else if (schemeLen == 6) {
					if (htmlQueue.getLowerCase(pos) == 'h'
						&& htmlQueue.getLowerCase(pos + 1) == 't'
						&& htmlQueue.getLowerCase(pos + 2) == 't'
						&& htmlQueue.getLowerCase(pos + 3) == 'p'
						&& htmlQueue.getLowerCase(pos + 4) == 's')
						type = SCHEME_HTTP;
				}
			}
		}

		if (type == SCHEME_OTHER)
			return null;

		// compose new URI in buf
		buf.setLength(0);

		buf.append(ParameterShuttle.getSelfURL()).append('?');

		if (addPost)
			buf.append(Parameter.ProxyPost.getName()+"=true&");
		
		buf.append(Parameter.Connector.getName()+"=" + infoShuttle.context.getConnector().getName() + "&"+Parameter.ProxyGoto.getName()+"=");

		switch (type) {
			case ABSOLUTE_HOST :
				//buf.append(baseScheme).append(':');
				break;

			case ABSOLUTE_PATH :
				//buf.append(baseHost);
				break;

			case OTHER :
				//buf.append(baseHost).append(basePath).append("&"+Parameter.ProxyThen.getName()+"=");
				buf.append(basePath);
				buf.append("&"+Parameter.ProxyThen.getName()+"=");
				break;
		}

		String endOfHtmlQueue = "";
		for (int p = pos, psize = pos + len; p < psize; p++) {
			endOfHtmlQueue += htmlQueue.getChar(p);
		}
		try {
			buf.append(URLEncoder.encode(endOfHtmlQueue, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
		}

		return buf;
	}
}
