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

package com.twinsoft.convertigo.engine.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

/**
 * What the No Code agents of an engine (Tigo for a No Code Studio user, through lib_ConvertigoAgentBridge) may do
 * besides calling the Convertigo MCP. An agent process runs as the engine's user: without a limit, its shell and file
 * tools read every project, account, token and secret of the engine, the folders of the other users' agents included,
 * and on the Docker image they become root with the passwordless sudo of that user. The agent of the Studio's own
 * developer (the user studio) is not limited by it.
 * <p>
 * The policy is the engine property agents.tool_policy:
 * <ul>
 * <li>mcp-only: no tool of the agent's own runtime;</li>
 * <li>files: files in the session's own folders, no shell; the agent's runtime enforces it (its configuration);</li>
 * <li>sandboxed-shell: files and a shell, every process of the agent confined by the kernel (Landlock) to the folders
 * the Bridge names, without new privileges (no sudo, no setuid). It needs Linux with Landlock and the helper of the
 * Docker image ({@value #DEFAULT_HELPER}); without them the effective policy is files;</li>
 * <li>full: every tool, as the engine's user;</li>
 * <li>auto, the default: files.</li>
 * </ul>
 * Landlock limits the file system only: a confined process still reaches the network. It sees no other process: of
 * /proc it reads its own entries and the system-wide files only.
 */
public final class AgentSandbox {
	/** the path of the helper that confines a command (the system property, else {@value #DEFAULT_HELPER}) */
	public static final String HELPER_SYSTEM_PROPERTY = "convertigo.agent.sandbox.helper";
	public static final String DEFAULT_HELPER = "/usr/local/bin/c8o-agent-sandbox";

	/**
	 * what every confined process reads and runs: the system, neither /usr/local (Tomcat, the engine) nor /workspace.
	 * Of /proc, the confined command's own entries (/proc/self, resolved by the helper to its pid, which the command
	 * keeps) and the system-wide files only: with the whole of /proc, a confined shell read the command line of the
	 * engine and the passwords it holds (test-nocode). The processes it starts cannot read their own entries. A process
	 * space of its own is not an option there: the container refuses to mount another /proc.
	 */
	static final List<String> SYSTEM_READ_ONLY = List.of("/bin", "/sbin", "/lib", "/lib32", "/lib64", "/usr/bin",
			"/usr/sbin", "/usr/lib", "/usr/lib32", "/usr/lib64", "/usr/libexec", "/usr/share", "/etc", "/sys", "/dev",
			"/proc/self", "/proc/thread-self", "/proc/cpuinfo", "/proc/meminfo", "/proc/stat", "/proc/loadavg",
			"/proc/uptime", "/proc/version", "/proc/filesystems", "/proc/sys");
	/** what every confined process writes besides its folders */
	static final List<String> SYSTEM_READ_WRITE = List.of("/dev/null");

	private static final long PROBE_TIMEOUT_MS = 5000;

	private static String probedHelper;
	private static int probedAbi;

	public enum ToolPolicy {
		MCP_ONLY("mcp-only"),
		FILES("files"),
		SANDBOXED_SHELL("sandboxed-shell"),
		FULL("full");

		private final String value;

		ToolPolicy(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		/** the agent reads and writes files in its session's folders */
		public boolean allowsFiles() {
			return this != MCP_ONLY;
		}

		/** the agent runs commands */
		public boolean allowsShell() {
			return this == SANDBOXED_SHELL || this == FULL;
		}

		/** every process of the agent runs through {@link AgentSandbox#wrap} */
		public boolean isConfined() {
			return this == SANDBOXED_SHELL;
		}

		static ToolPolicy of(String value) {
			for (ToolPolicy policy : values()) {
				if (policy.value.equals(value)) {
					return policy;
				}
			}
			return null;
		}
	}

	private AgentSandbox() {
	}

	/** the policy agents.tool_policy names; auto, or a value no policy has, is files */
	static ToolPolicy resolve(String value) {
		String name = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
		ToolPolicy policy = ToolPolicy.of(name);
		if (policy == null && !name.isEmpty() && !"auto".equals(name) && Engine.logEngine != null) {
			Engine.logEngine.warn("(AgentSandbox) agents.tool_policy=" + value + " names no policy: auto is used");
		}
		return policy != null ? policy : ToolPolicy.FILES;
	}

	/** what a policy gives on an engine whose kernel confines at this Landlock version (0: it cannot) */
	static ToolPolicy effective(ToolPolicy policy, int landlockAbi) {
		return policy.isConfined() && landlockAbi < 1 ? ToolPolicy.FILES : policy;
	}

	/** the policy of this engine, as configured */
	public static ToolPolicy getPolicy() {
		return resolve(EnginePropertiesManager.getProperty(PropertyName.AGENTS_TOOL_POLICY));
	}

	/** the policy the agents get: sandboxed-shell is files where the commands cannot be confined */
	public static ToolPolicy getEffectivePolicy() {
		return effective(getPolicy(), getLandlockAbi());
	}

	public static String getHelperPath() {
		return System.getProperty(HELPER_SYSTEM_PROPERTY, DEFAULT_HELPER);
	}

	/** the Landlock version the helper finds in the kernel, probed once per helper path; 0 off Linux or without it */
	public static synchronized int getLandlockAbi() {
		if (!Engine.isLinux()) {
			return 0;
		}
		String helper = getHelperPath();
		if (!helper.equals(probedHelper)) {
			probedAbi = probe(helper, PROBE_TIMEOUT_MS);
			probedHelper = helper;
		}
		return probedAbi;
	}

	/** what `helper --probe` prints: the Landlock version, on success only */
	static int probe(String helper, long timeoutMs) {
		File file = new File(helper);
		if (!file.isFile() || !file.canExecute()) {
			return 0;
		}
		Process process = null;
		try {
			process = new ProcessBuilder(helper, "--probe").redirectErrorStream(true).start();
			String line;
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
				if (!process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
					return 0;
				}
				line = reader.readLine();
			}
			if (process.exitValue() != 0 || line == null) {
				return 0;
			}
			int abi = Integer.parseInt(line.trim());
			return abi > 0 ? abi : 0;
		} catch (Exception e) {
			return 0;
		} finally {
			if (process != null && process.isAlive()) {
				process.destroyForcibly();
			}
		}
	}

	/**
	 * The command run confined to the system (read only), the readOnly folders and the readWrite folders: the helper
	 * applies Landlock and no_new_privs, then runs the command, and every process it starts inherits both. A path
	 * that does not exist is left out by the helper.
	 *
	 * @throws IllegalStateException where the kernel cannot confine it: the command is never run unconfined
	 */
	public static List<String> wrap(List<String> command, Collection<String> readOnly, Collection<String> readWrite) {
		if (getLandlockAbi() < 1) {
			throw new IllegalStateException("The agent's command cannot be confined here: " + getHelperPath() + " or Landlock is missing");
		}
		return build(getHelperPath(), command, readOnly, readWrite);
	}

	static List<String> build(String helper, List<String> command, Collection<String> readOnly, Collection<String> readWrite) {
		if (command == null || command.isEmpty()) {
			throw new IllegalArgumentException("No command to confine");
		}
		Set<String> ro = paths(SYSTEM_READ_ONLY, readOnly);
		Set<String> rw = paths(SYSTEM_READ_WRITE, readWrite);
		List<String> args = new ArrayList<>(2 * (ro.size() + rw.size()) + command.size() + 2);
		args.add(helper);
		for (String path : ro) {
			args.add("--ro");
			args.add(path);
		}
		for (String path : rw) {
			args.add("--rw");
			args.add(path);
		}
		args.add("--");
		args.addAll(command);
		return args;
	}

	private static Set<String> paths(List<String> system, Collection<String> given) {
		Set<String> paths = new LinkedHashSet<>(system);
		for (String path : given == null ? Collections.<String>emptyList() : given) {
			if (path == null || !new File(path).isAbsolute() || path.indexOf('\0') >= 0) {
				throw new IllegalArgumentException("Not an absolute path: " + path);
			}
			paths.add(path);
		}
		return paths;
	}

	/** {"policy", "effective", "landlockAbi", "helper"}: what the Bridge reads in one call */
	public static String describe() {
		try {
			return new JSONObject()
					.put("policy", getPolicy().getValue())
					.put("effective", getEffectivePolicy().getValue())
					.put("landlockAbi", getLandlockAbi())
					.put("helper", getHelperPath())
					.toString();
		} catch (JSONException e) {
			throw new IllegalStateException(e);
		}
	}
}
