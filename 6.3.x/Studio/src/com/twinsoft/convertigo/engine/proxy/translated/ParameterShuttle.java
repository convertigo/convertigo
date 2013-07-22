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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.twinsoft.convertigo.engine.Context;

public class ParameterShuttle {
	// parameters from user/browser
	public String sessionID;
	public String userID;
	public String userIP;
	
	public Context context;

	public boolean postFromUser;
	public String userPostData;
	public String userContentType;
	public int userContentLength;

	public List<String> userHeaderNames = new ArrayList<String>();
	public List<String> userHeaderValues = new ArrayList<String>();

	public String userGoto;
	public String userThen;
	public boolean postToSite;

	public URL prevSiteURL;
	public URL siteURL;

	// parameters from remote site
	public int httpCode;

	public List<String> siteHeaderNames = new ArrayList<String>();
	public List<String> siteHeaderValues = new ArrayList<String>();

	public String siteContentType;
	public int siteContentSize;
	public boolean siteContentHTML;

	public InputStream siteInputStream;
	public OutputStream siteOutputStream;

	/*public void closeSiteInputStream() {
		if (siteInputStream != null) {
			try {
				siteInputStream.close();
			}
			catch (Exception e) {
				Engine.logEngine.error("Unexpected exception", e);
			}
			finally {
				siteInputStream = null;
			}
		}
	}*/

	public void clear() {
		sessionID = userID = userIP = null;
		userHeaderNames.clear();
		userHeaderValues.clear();
		userContentType = userGoto = userThen = null;
		prevSiteURL = siteURL = null;

		siteContentType = null;
		siteHeaderNames.clear();
		siteHeaderValues.clear();
		//closeSiteInputStream();
	}
	// static parameters about HttpBridge itself
	// constants about context and servlets
	//static public final String bridgeGotoTag = "(goto)";
	//static public final String bridgeThenTag = "(then)";
	//static public final String bridgePostTag = "(post)";

	// SelfURL
	static private String bridgeScheme;
	static private String bridgeName;
	static private int bridgePort;
	static private String bridgePath;

	static private String bridgeURLStr;

	static public String getSelfURL(
		String scheme,
		String name,
		int port,
		String path) {
		if (!scheme.equals(bridgeScheme) || !name.equals(bridgeName) || port != bridgePort || !path.equals(bridgePath)) {
			bridgeScheme = scheme;
			bridgeName = name;
			bridgePort = port;
			bridgePath = path;

			bridgeURLStr = bridgeScheme + "://" + bridgeName +
				((bridgePort == 443 && bridgeScheme.equals("https")) || (bridgePort == 80 && bridgeScheme.equals("http")) ? "" : ":" + bridgePort) +
				bridgePath;
		}

		return bridgeURLStr;
	};

	static public String getSelfURL() {
		return bridgeURLStr;
	}
}