/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.services.studio.ngxbuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.websocket.Session;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.WebSocketService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;
import com.twinsoft.convertigo.engine.util.NetworkUtils;
import com.twinsoft.convertigo.engine.util.ProcessUtils;

@ServiceDefinition(name = "WsBuilder", roles = { Role.WEB_ADMIN }, parameters = {}, returnValue = "")
public class WsBuilder extends WebSocketService {
	static class Build implements Runnable {
		private static final Set<Integer> usedPort = new HashSet<>();
		private static Pattern pRemoveEchap = Pattern.compile("\\x1b\\[\\d+m");
		private static Pattern pIsBrowserOpenable = Pattern.compile(".*?open your browser on (http\\S*).*");

		String projectName;
		String projectEndpoint;
		Project project;
		String url;
		Thread thread;
		int portNode;
		String baseUrl;
		private Collection<Process> processes = new LinkedList<>();

		Set<WsBuilder> clients = new HashSet<>();

		Build(String projectName, String endpoint) {
			this.projectName = projectName;
			projectEndpoint = endpoint + "/projects/" + projectName + "/";
		}

		public void start() {
			if (thread != null) {
				return;
			}
			thread = new Thread(this);
			thread.setName("Build of " + projectName);
			thread.setDaemon(true);
			thread.start();
		}

		@Override
		public void run() {
			log("build thread started");
			var mutex = new Object();
			MobileBuilder mb = null;
			try {
				project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
				mb = project.getMobileBuilder();
				if (!mb.isInitialized()) {
					throw new Exception("Mobile Builder not initialized");
				}
				mb.setBuildMutex(mutex);
				mb.startBuild();

				var displayObjectsMobile = new File(project.getDirPath(), "DisplayObjects/mobile");
				displayObjectsMobile.mkdirs();

				var ionicDir = new File(project.getDirPath(), "_private/ionic");
//				var nodeModules = new File(ionicDir, "node_modules");

				var nodeVersion = ProcessUtils.getNodeVersion(project);
				var nodeDir = ProcessUtils.getDefaultNodeDir();
				try {
					nodeDir = ProcessUtils.getNodeDir(nodeVersion, (r, t, x) -> {
						appendOutput("Downloading nodejs " + nodeVersion + ": " + Math.round((r * 100f) / t) + "%");
					});
				} catch (Exception e1) {
				}

				{
					String versions = "Will use nodejs " + ProcessUtils.getNodeVersion(nodeDir) + " and npm "
							+ ProcessUtils.getNpmVersion(nodeDir);
					appendOutput(versions);
					Engine.logStudio.info(versions);
				}

				String path = nodeDir.getAbsolutePath();

				terminateNode(false);

				var pb = ProcessUtils.getNpmProcessBuilder(path, "npm", "run", "ionic:serve");

				List<String> cmd = pb.command();
				synchronized (usedPort) {
					int port = (Math.abs(ionicDir.getAbsolutePath().hashCode()) % 10000) + 40000;
					cmd.add("--");
					usedPort.clear();
					portNode = NetworkUtils.nextAvailable(port, usedPort);
					cmd.add("--port=" + portNode);
					cmd.add("--host=0.0.0.0");
					cmd.add("--disable-host-check=true");
				}

				// #183 add useless option to help terminateNode method to find the current path
				cmd.add("--ssl-key=" + new File(project.getDirFile(), "DisplayObjects/mobile").getAbsolutePath());

				pb.redirectErrorStream(true);
				pb.directory(ionicDir);
				String angular_json = FileUtils.readFileToString(new File(ionicDir, "angular.json"), "UTF-8");
				angular_json = angular_json.replaceFirst("(\"serve\":\s*\\{).*",
						"$1 \"baseHref\":\"" + projectEndpoint + "DisplayObjects/dev" + portNode + "/\",");
				FileUtils.write(new File(ionicDir, "angular.json"), angular_json, "UTF-8");
				Process p = pb.start();
				processes.add(p);

				var matcher = Pattern.compile("(\\d+)% (.*)").matcher("");
				var br = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line;

				StringBuilder sb = null;
				String lastLine = null;
				while ((line = br.readLine()) != null) {
					line = pRemoveEchap.matcher(line).replaceAll("");
					if (StringUtils.isNotBlank(line) && !line.equals(lastLine)) {
						lastLine = line;
						Engine.logStudio.info(line);
						if (line.startsWith("Error: ")) {
							sb = new StringBuilder();
						}
						if (sb != null) {
							if (line.contains("Failed to compile.")) {
								sb.append(line);
								error(sb.toString());
								sb = null;
							} else {
								sb.append(line + "\n");
							}
						}

						matcher.reset(line);
						if (matcher.find()) {
							progress(Integer.parseInt(matcher.group(1)));
							appendOutput(matcher.group(2));
						} else {
							appendOutput(line);
						}
						if (line.matches(".*Compiled .*successfully.*")) {
							progress(100);
							error(null);
							synchronized (mutex) {
								mutex.notify();
							}
							mb.buildFinished();
						}

						Matcher m = pIsBrowserOpenable.matcher(line);
						if (m.matches()) {
							String sGroup = m.group(1);
							baseUrl = sGroup.replaceFirst(".*?://.*?/", "/").replaceFirst("([^/])$", "$1/"); // sGroup.substring(0,
																												// sGroup.lastIndexOf("/"));
							doLoad();
						}
					}
				}

//				if (buildCount == this.buildCount) {
//					appendOutput("\\o/");
//				} else {
//					appendOutput("previous build canceled !");
//				}
			} catch (Exception e) {
				log("Exception: " + e);
			} finally {
				synchronized (mutex) {
					mutex.notify();
				}
				mb.setBuildMutex(null);
				if (mb != null) {
					mb.buildFinished();
				}
			}
			synchronized (clients) {
				clients.forEach(c -> remove(c));
			}
		}

		private void doLoad() {
			send("load", baseUrl);
		}

		private void progress(int parseInt) {
			send("progress", "" + parseInt);
		}

		private void error(String string) {
			if (string != null) {
				log("error: " + string);
			}
		}

		private void appendOutput(String string) {
			send("output", string);
		}

		private void log(String log) {
			send("log", log);
		}

		private void send(String type, String value) {
			synchronized (clients) {
				for (var client : clients) {
					try {
						WsBuilder.send(client.session, type, value);
					} catch (Exception e) {
					}
				}
			}
		}

		public void add(WsBuilder client) {
			synchronized (clients) {
				client.build = this;
				clients.add(client);
			}
		}

		public void remove(WsBuilder client) {
			synchronized (clients) {
				client.build = null;
				clients.remove(client);
			}
		}

		void terminateNode(boolean prodOnly) {
			baseUrl = null;
			String projectName = new File(project.getDirPath()).getName();
			int retry = 10;
			try {
				while (retry-- > 0) {
					if (Engine.isWindows()) {
						String prod = prodOnly ? " AND CommandLine Like '%--watch%'" : "";
						Process process = new ProcessBuilder("wmic", "PROCESS", "WHERE",
								"Name='node.exe' AND CommandLine Like '%\\\\" + projectName + "\\\\DisplayObjects\\\\%'"
										+ prod,
								"CALL", "TERMINATE").redirectError(Redirect.DISCARD).start();
						String output = IOUtils.toString(process.getInputStream(), Charset.defaultCharset());
						process.waitFor();
						int id = output.indexOf('\n');
						if (id == -1 || output.indexOf('\n', id) == -1) {
							retry = 0;
						}

						process = new ProcessBuilder("wmic", "PROCESS", "WHERE",
								("Name='node.exe' AND CommandLine Like '%\\\\" + projectName
										+ "\\\\DisplayObjects\\\\%'" + prod).replace("\\", "\\\\"),
								"CALL", "TERMINATE").redirectError(Redirect.DISCARD).start();
						output = IOUtils.toString(process.getInputStream(), Charset.defaultCharset());
						process.waitFor();
						id = output.indexOf('\n');
						if (id == -1 || output.indexOf('\n', id) == -1) {
							retry = 0;
						}
					} else {
						String prod = prodOnly ? " | grep -e \"--watch\" -e \":watch\"" : "";
						Process process = new ProcessBuilder("/bin/bash", "-c",
								"ps -e" + (Engine.isLinux() ? "f" : "") + " | grep -v \"sed -n\"" + prod
										+ " | sed -n -E \"s,[^0-9]*([0-9]+).*(node|npm|ng).*/" + projectName
										+ "/DisplayObjects/.*,\\1,p\" | xargs kill")
								.redirectError(Redirect.DISCARD).redirectOutput(Redirect.DISCARD).start();
						int code = process.waitFor();
						if (code == 0) {
							retry = 0;
						}
					}
				}
				synchronized (usedPort) {
					usedPort.remove(portNode);
				}
			} catch (Exception e) {
				Engine.logStudio.warn("Failed to terminate the node server", e);
			}
		}
	}

	static Map<String, Build> builds = new HashMap<>();

	Session session;
	String project;
	Build build;

	@Override
	public void onOpen(Session session) {
		Engine.logAdmin.warn("onOpen");
		this.session = session;
	}

	@Override
	public void onMessage(String message, Session session) {
		Engine.logAdmin.warn("onMessage: " + message);
		try {
			var json = new JSONObject(message);
			project = json.getString("project");
			var action = json.getString("action");
			var params = json.getJSONObject("params");
			switch (action) {
			case "attach":
				onAttach();
				break;
			case "build_dev":
				onBuildDev(params);
				break;
			case "kill":
				onKill();
				break;
			}
		} catch (Exception e) {
			Engine.logAdmin.error("failed to send back message", e);
		}
	}

	@Override
	public void onClose(Session session) {
		Engine.logAdmin.warn("onClose");
		if (build != null) {
			build.remove(this);
		}
	}

	@Override
	public void onError(Throwable throwable, Session session) {
		Engine.logAdmin.warn("onError", throwable);
		if (build != null) {
			build.remove(this);
		}
	}

	void send(String action, String message) throws JSONException, IOException {
		send(session, action, message);
	}

	private void onAttach() throws Exception {
		if (build != null) {
			build.remove(this);
		}
		synchronized (builds) {
			build = builds.get(project);
		}

		if (build == null) {
			send("log", "No builder running for " + project);
			return;
		}

		build.add(this);
		send("log", "Builder attached for " + project);
		if (build.baseUrl != null) {
			send("load", build.baseUrl);
		}
	}

	private void onBuildDev(JSONObject params) throws Exception {
		if (build != null) {
			send("log", "prevents starting a new build of " + project);
//			return;
		}
		synchronized (builds) {
			build = new Build(project, params.getString("endpoint"));
			builds.put(project, build);
		}
		build.add(this);
		build.start();
	}

	private void onKill() {
		if (build != null) {
			build.log("killing builds for " + project);
			build.terminateNode(false);
		}
	}

	static void send(Session session, String action, String message) throws JSONException, IOException {
		var json = new JSONObject();
		json.put("type", action);
		json.put("value", message);
		session.getBasicRemote().sendText(json.toString());
	}
}
