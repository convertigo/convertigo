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

package com.twinsoft.convertigo.engine.process;

import java.io.File;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * What {@link ManagedProcesses#start(ManagedProcessSpec)} launches: a function, which names the process in
 * the registry and the list of the processes, and a {@link ProcessBuilder}, built here or given by the
 * launcher (e.g. ProcessUtils.getNpmProcessBuilder).
 *
 * <pre>
 * ManagedProcess p = ManagedProcesses.get().start(ManagedProcessSpec.of("ngx-serve:" + project)
 * 		.builder(ProcessUtils.getNpmProcessBuilder(path, "npm", "run", "ionic:serve"))
 * 		.directory(ionicDir)
 * 		.nodePreload(true));
 * </pre>
 */
public final class ManagedProcessSpec {
	final String function;
	ProcessBuilder builder;
	File owner;
	boolean ownerSet = false;
	boolean record = true;
	boolean nodePreload = false;
	Consumer<String> output = null;
	int outputLines = 20;
	Charset charset = null;
	Duration stopGrace = Duration.ofSeconds(5);
	final Map<String, String> properties = new LinkedHashMap<>();

	private ManagedProcessSpec(String function) {
		if (function == null || function.isBlank()) {
			throw new IllegalArgumentException("A managed process needs a function");
		}
		this.function = function;
	}

	/**
	 * @param function what the process does, e.g. pouchdb-server or ngx-serve:&lt;project&gt;; the processes
	 *                 of a function can be stopped together
	 */
	public static ManagedProcessSpec of(String function) {
		return new ManagedProcessSpec(function);
	}

	/**
	 * The process builder to start; its command, directory, environment and redirections are kept. The stdin
	 * must stay a pipe for the Node.js preload.
	 */
	public ManagedProcessSpec builder(ProcessBuilder builder) {
		this.builder = builder;
		return this;
	}

	public ManagedProcessSpec command(List<String> command) {
		builder().command(new ArrayList<>(command));
		return this;
	}

	public ManagedProcessSpec command(String... command) {
		return command(Arrays.asList(command));
	}

	public ManagedProcessSpec directory(File directory) {
		builder().directory(directory);
		return this;
	}

	public ManagedProcessSpec environment(Map<String, String> environment) {
		builder().environment().putAll(environment);
		return this;
	}

	public ManagedProcessSpec redirectErrorStream(boolean redirect) {
		builder().redirectErrorStream(redirect);
		return this;
	}

	/**
	 * @param owner the workspace whose registry (&lt;owner&gt;/processes) records the process, to stop it
	 *              if the engine dies; null for no registry; by default, the workspace of the engine
	 */
	public ManagedProcessSpec owner(File owner) {
		this.owner = owner;
		this.ownerSet = true;
		return this;
	}

	/**
	 * @param record false for a short process that needs no registry (e.g. node --version)
	 */
	public ManagedProcessSpec record(boolean record) {
		this.record = record;
		return this;
	}

	/**
	 * @param nodePreload whether the Node.js processes stop when the engine is gone: "--require &lt;preload&gt;"
	 *                    after the node executable, else NODE_OPTIONS for a tree of Node.js processes (npm)
	 */
	public ManagedProcessSpec nodePreload(boolean nodePreload) {
		this.nodePreload = nodePreload;
		return this;
	}

	/**
	 * The output (stdout, stderr too when redirected to it) is read continuously and given line by line to
	 * output; the last lines are kept for diagnostic. Without it, the launcher reads the streams of
	 * {@link ManagedProcess#getProcess()} itself.
	 */
	public ManagedProcessSpec output(Consumer<String> output, int lines) {
		this.output = output;
		this.outputLines = Math.max(0, lines);
		return this;
	}

	/**
	 * @param charset the charset of the output read by an output consumer, UTF-8 by default
	 */
	public ManagedProcessSpec charset(Charset charset) {
		this.charset = charset;
		return this;
	}

	/**
	 * @param grace the delay a stopped process has to exit before it is destroyed forcibly
	 */
	public ManagedProcessSpec stopGrace(Duration grace) {
		this.stopGrace = grace;
		return this;
	}

	/**
	 * A free field recorded with the process, such as port or dir.
	 */
	public ManagedProcessSpec property(String name, String value) {
		if (value != null) {
			properties.put(name, value);
		}
		return this;
	}

	public String getFunction() {
		return function;
	}

	ProcessBuilder builder() {
		if (builder == null) {
			builder = new ProcessBuilder();
		}
		return builder;
	}
}
