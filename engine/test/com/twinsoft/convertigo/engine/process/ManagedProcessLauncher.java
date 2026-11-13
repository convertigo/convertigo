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

/**
 * Stands for the engine in the tests: starts managed processes in the workspace args[0], prints their
 * PIDs, then waits to be killed, or stops normally when its stdin ends.
 * <p>
 * Arguments: workspace, reaper mode (on, off), then the kinds of processes: shell (a tree of sleep), node
 * (node with the preload), npm (a shell running node, the preload in NODE_OPTIONS); the node executable
 * is the system property nodeExecutable.
 */
public class ManagedProcessLauncher {

	public static void main(String[] args) throws Exception {
		File workspace = new File(args[0]);
		ManagedProcesses service = ManagedProcesses.get();
		service.setReaperMode(args[1]);
		service.open(workspace);
		String node = System.getProperty("nodeExecutable", "node");
		for (int i = 2; i < args.length; i++) {
			ManagedProcessSpec spec = ManagedProcessSpec.of("test-" + args[i]);
			switch (args[i]) {
			case "shell" -> spec.command("sh", "-c", "sleep 600 & sleep 600 & wait");
			case "node" -> spec.command(node, "-e", "setInterval(function () {}, 1000)").nodePreload(true);
			// a shell that waits for its node child, as npm runs a script
			case "npm" -> spec.command("sh", "-c", "'" + node + "' -e 'setInterval(function () {}, 1000)'; exit $?").nodePreload(true);
			default -> throw new IllegalArgumentException(args[i]);
			}
			spec.builder().redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD);
			ManagedProcess process = service.start(spec);
			System.out.println(args[i] + " " + process.pid());
		}
		// the identity of a launcher script is refreshed after 500 ms
		Thread.sleep(700);
		System.out.println("reaper " + service.getReaperPid());
		System.out.println("ready " + ProcessHandle.current().pid());
		System.out.flush();
		// a normal stop: the end of stdin
		while (System.in.read() >= 0) {
		}
		System.exit(0);
	}
}
