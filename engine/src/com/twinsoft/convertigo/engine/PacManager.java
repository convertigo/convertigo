/*
 * Copyright (c) 2001-2026 Convertigo SA.
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
import java.net.Proxy;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.engine.util.RhinoUtils;

public class PacManager {
	private static final Pattern PROXY_ADDRESS = Pattern.compile(
			"(\\[[^\\s\\[\\]]+\\]|[^\\s:/\\\\?#@\\[\\]]+)\\s*:\\s*([0-9]+)");

	private final String scriptUrl;
	private volatile Scriptable scope;
	private final AtomicBoolean unavailableWarningLogged = new AtomicBoolean();

	PacManager(String url) {
		this.scriptUrl = url;
	}

	void start() {
		scope = null;
		unavailableWarningLogged.set(false);
		try {
			Context ctx = Context.enter();
			Scriptable loaded = ctx.initStandardObjects();
			loaded.put("pacMethods", loaded, new PacScriptMethods());
			RhinoUtils.evalInterpretedJavascript(ctx, loaded, getScriptContent(), "pac", 0, null);
			for (PacScriptMethods.jsFunctions function : PacScriptMethods.jsFunctions.values()) {
				RhinoUtils.evalInterpretedJavascript(ctx, loaded,
						"function " + function.name() + " () {" + "return pacMethods." + function.name() + ".apply(pacMethods, arguments); }",
						function.name(), 0, null);
			}
			if (!(loaded.get("FindProxyForURL", loaded) instanceof Function)) {
				throw new IOException("PAC script has no FindProxyForURL function");
			}
			// Publish only a fully loaded script, including when settings are applied at runtime.
			scope = loaded;
			Engine.logProxyManager.debug("(PacManager) PAC script loaded");
		} catch (IOException | RuntimeException e) {
			warnUnavailable();
			Engine.logProxyManager.debug("(PacManager) PAC loading failure", e);
		} finally {
			Context.exit();
		}
	}

	private void warnUnavailable() {
		if (unavailableWarningLogged.compareAndSet(false, true)) {
			Engine.logProxyManager.warn("(PacManager) Automatic proxy mode: PAC is unavailable or could not be loaded; "
					+ "requests will use a direct connection. Check the PAC location and cems.ProxyManager DEBUG logs.");
		}
	}

	public String getScriptContent() throws IOException {
		try {
			if (scriptUrl == null || scriptUrl.isBlank()) {
				throw new IOException("PAC location is empty");
			}
			String scriptContent = (this.scriptUrl.startsWith("file:/") || this.scriptUrl.indexOf(":/") == -1) ?
					readPacFileContent(this.scriptUrl) : downloadPacContent(this.scriptUrl);
			Engine.logProxyManager.debug("(PacManager) .pac content : \n" + scriptContent);
			return scriptContent;
		} catch (IOException e) {
			Engine.logProxyManager.info("(PacManager) Loading script failed", e);
			throw e;
		}
	}

	private String readPacFileContent(String scriptUrl) throws IOException {
		try {
			File file = (scriptUrl.indexOf(":/") == -1) ? new File(scriptUrl) : new File(new URI(scriptUrl));

			try (var input = new FileInputStream(file)) {
				return IOUtils.toString(input, "UTF-8");
			}
		} catch (Exception e) {
			throw new IOException("Cannot read PAC file", e);
		}
	}

	private String downloadPacContent(String url) throws IOException {
		if (url == null) {
			Engine.logProxyManager.debug("(PacManager) Invalid PAC script URL: null");
			throw new IOException("Invalid PAC script URL: null");
		}

		HttpClient client = new HttpClient();
		HttpMethod method = new GetMethod(url);

		try {
			int statusCode = client.executeMethod(method);
			if (statusCode != HttpStatus.SC_OK) {
				throw new IOException("PAC download returned HTTP " + statusCode);
			}
			return IOUtils.toString(method.getResponseBodyAsStream(), "UTF-8");
		} finally {
			method.releaseConnection();
		}
	}

	private String evaluate(String url, String host) {
		Object result = "";
		try {
			Context ctx = Context.enter();
			result = RhinoUtils.evalInterpretedJavascript(ctx, scope, "FindProxyForURL (\"" + url + "\",\"" + host + "\")", "check", 0, null);
		} catch (Exception e) {
			Engine.logProxyManager.warn("(PacManager) PAC evaluation failed; using a direct connection. Check cems.ProxyManager DEBUG logs.");
			Engine.logProxyManager.debug("(PacManager) PAC evaluation failure", e);
			return null;
		} finally {
			Context.exit();	
		}
		if (result instanceof NativeJavaObject) {
			result = ((NativeJavaObject) result).unwrap();
		}

		Engine.logProxyManager.debug("(PacManager) evaluate " + url + " from " + host + " : " + result);

		return result == null ? "" : result.toString();
	}

	PacInfos getPacInfos(String url, String host) {
		return firstProxy(getPacProxies(url, host));
	}

	List<Proxy> getPacProxies(String url, String host) {
		if (scope == null) {
			warnUnavailable();
			return List.of(Proxy.NO_PROXY);
		}
		return parsePacProxies(evaluate(url, host));
	}

	PacInfos parsePacResult(String result) {
		return firstProxy(parsePacProxies(result));
	}

	private PacInfos firstProxy(List<Proxy> proxies) {
		Proxy proxy = proxies.get(0);
		if (proxy.type() == Proxy.Type.DIRECT) return null;
		var address = (InetSocketAddress) proxy.address();
		var info = new PacInfos();
		info.pacServer = address.getHostString();
		info.pacPort = address.getPort();
		return info;
	}

	List<Proxy> parsePacProxies(String result) {
		var proxies = new ArrayList<Proxy>();
		if (result != null) for (String entry : result.split(";")) {
			entry = entry.trim();
			if (entry.isEmpty()) continue;
			String[] directive = entry.split("\\s+", 2);
			if ("DIRECT".equalsIgnoreCase(directive[0]) && directive.length == 1) {
				proxies.add(Proxy.NO_PROXY);
				continue;
			}
			if (!"PROXY".equalsIgnoreCase(directive[0])) {
				Engine.logProxyManager.warn("(PacManager) Skipping unsupported PAC directive: " + directive[0]);
				continue;
			}
			if (directive.length == 2) {
				var address = PROXY_ADDRESS.matcher(directive[1].trim());
				if (address.matches()) {
					try {
						int port = Integer.parseInt(address.group(2));
						if (port > 0 && port <= 65535) {
							String server = address.group(1);
							if (server.startsWith("[")) server = server.substring(1, server.length() - 1);
							proxies.add(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(server, port)));
							continue;
						}
					} catch (NumberFormatException invalidPort) {
						// An invalid alternative must not prevent trying the next entry.
					}
				}
			}
			Engine.logProxyManager.warn("(PacManager) Skipping invalid PROXY entry in PAC response");
		}
		return proxies.isEmpty() ? List.of(Proxy.NO_PROXY) : List.copyOf(proxies);
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
