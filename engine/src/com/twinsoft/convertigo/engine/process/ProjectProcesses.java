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

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * The Node.js processes of the application of a project (npm install, development server, builds), started
 * by the application editors of the Studio and by the web Studio (WsBuilder) as managed processes, whose
 * function is &lt;kind&gt;:&lt;project&gt;: a launcher stops the processes of the project the other one
 * started, and only them, never on a name or a folder.
 */
public final class ProjectProcesses {
	/** npm install */
	public static final String INSTALL = "app-install";
	/** the development server (ionic:serve) */
	public static final String SERVE = "app-serve";
	/** a build of the application that ends */
	public static final String BUILD = "app-build";
	/** a build that watches the sources */
	public static final String WATCH = "app-watch";

	private static final Set<String> KINDS = Set.of(INSTALL, SERVE, BUILD, WATCH);

	private ProjectProcesses() {
	}

	public static String function(String kind, String project) {
		return kind + ":" + project;
	}

	/**
	 * Starts a Node.js process of the application of project, with the preload that stops it when the
	 * engine is gone.
	 *
	 * @param kind INSTALL, SERVE, BUILD or WATCH
	 */
	public static ManagedProcess start(String kind, String project, ProcessBuilder builder) throws IOException {
		return ManagedProcesses.get().start(ManagedProcessSpec.of(function(kind, project)).builder(builder)
				.nodePreload(true).property("project", project));
	}

	/**
	 * Stops the processes of the application of project, with their trees.
	 *
	 * @param kinds the kinds to stop, all of them when none
	 * @return the processes still alive, normally none
	 */
	public static List<ProcessHandle> stop(String project, String... kinds) {
		Set<String> functions = new java.util.HashSet<>();
		for (String kind : kinds.length == 0 ? KINDS.toArray(new String[0]) : kinds) {
			functions.add(function(kind, project));
		}
		return ManagedProcesses.get().stop(process -> functions.contains(process.getFunction()));
	}

	/**
	 * @return whether a process of the application of project of one of these kinds runs
	 */
	public static boolean isRunning(String project, String kind) {
		return !ManagedProcesses.get().list(function(kind, project)).isEmpty();
	}
}
