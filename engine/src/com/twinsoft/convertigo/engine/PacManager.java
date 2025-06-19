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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.engine.util.RhinoUtils;

public class PacManager {

	private final String scriptUrl;
	private Scriptable scope;

	PacManager(String url) {
		this.scriptUrl = url;
	}

	void start() {
		try {
			Context ctx = Context.enter();
			scope = ctx.initStandardObjects();
			scope.put("pacMethods", scope, new PacScriptMethods());
			RhinoUtils.evalInterpretedJavascript(ctx, scope, getScriptContent(), "pac", 0, null);
			for (PacScriptMethods.jsFunctions function : PacScriptMethods.jsFunctions.values()) {
				RhinoUtils.evalInterpretedJavascript(ctx, scope,
						"function " + function.name() + " () {" + "return pacMethods." + function.name() + ".apply(pacMethods, arguments); }",
						function.name(), 0, null);
			}
		} catch (IOException e) {
			Engine.logProxyManager.error("(PacManager) Failed to declare PacScriptMethods wrapper", e);
		} finally {
			Context.exit();	
		}
	}

	public String getScriptContent() throws IOException {
		try {
			String scriptContent = (this.scriptUrl.startsWith("file:/") || this.scriptUrl.indexOf(":/") == -1) ?
					readPacFileContent(this.scriptUrl) : downloadPacContent(this.scriptUrl);
			Engine.logProxyManager.debug("(PacManager) .pac content : \n" + scriptContent);
			return scriptContent;
		} catch (IOException e) {
			Engine.logProxyManager.info("(PacManager) Loading script failed", e);
			throw e;
		}
	}

	private String readPacFileContent(String scriptUrl) {
		try {
			File file = (scriptUrl.indexOf(":/") == -1) ? new File(scriptUrl) : new File(new URI(scriptUrl));

			return IOUtils.toString(new FileInputStream(file), "UTF-8");
		} catch (Exception e) {
			Engine.logProxyManager.error("(PacManager) Error reading proxy auto config file" + e);
			return "";
		}
	}

	private String downloadPacContent(String url) throws IOException {
		if (url == null) {
			Engine.logProxyManager.debug("(PacManager) Invalid PAC script URL: null");
			throw new IOException("Invalid PAC script URL: null");
		}

		HttpClient client = new HttpClient();
		HttpMethod method = new GetMethod(url);

		int statusCode = client.executeMethod(method);

		if (statusCode != HttpStatus.SC_OK) {
			throw new IOException("(PacManager) Method failed: " + method.getStatusLine());
		}

		return IOUtils.toString(method.getResponseBodyAsStream(), "UTF-8");
	}

	private String evaluate(String url, String host) {
		Object result = "";
		try {
			Context ctx = Context.enter();
			result = RhinoUtils.evalInterpretedJavascript(ctx, scope, "FindProxyForURL (\"" + url + "\",\"" + host + "\")", "check", 0, null);
		} catch (Exception e) {
			Engine.logProxyManager.error("(PacManager) Failed to evaluate .pac for " + url + " from " + host, e);
		} finally {
			Context.exit();	
		}
		if (result instanceof NativeJavaObject) {
			result = ((NativeJavaObject) result).unwrap();
		}

		Engine.logProxyManager.debug("(PacManager) evaluate " + url + " from " + host + " : " + result);

		return result.toString();
	}

	PacInfos getPacInfos(String url, String host) {
		PacInfos pacInfos = null;
		String result = evaluate(url, host);
		if (result.startsWith("PROXY")) {
			result = result.replaceAll("PROXY\\s*", "");
			pacInfos = new PacInfos();
			pacInfos.pacServer = result.split(":")[0];
			pacInfos.pacPort = Integer.parseInt(result.split(":")[1]);
		}
		return pacInfos;
	}

	public class PacInfos {
		private String pacServer;
		private int pacPort;

		protected PacInfos() {
		}

		public String getServer() {
			return pacServer;
		}

		public int getPort() {
			return pacPort;
		}
	}
}
