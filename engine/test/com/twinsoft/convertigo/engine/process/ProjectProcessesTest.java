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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.time.Duration;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * The development server of an application (npm run, then node) and the engine killed abruptly: before the
 * managed processes, the server outlives the engine and keeps its port; managed, it stops.
 */
public class ProjectProcessesTest {
	@Rule
	public TemporaryFolder temporary = new TemporaryFolder();

	private final ProcessTestSupport support = new ProcessTestSupport();
	private File workspace;
	private int port;

	@Before
	public void setUp() throws Exception {
		ProcessTestSupport.assumeUnix();
		File node = ProcessTestSupport.node();
		org.junit.Assume.assumeTrue("npm next to node", new File(node.getParentFile(), "npm").exists());
		workspace = temporary.newFolder("workspace");
		File app = new File(workspace, "app");
		app.mkdirs();
		try (ServerSocket socket = new ServerSocket(0)) {
			port = socket.getLocalPort();
		}
		Files.writeString(new File(app, "package.json").toPath(), "{\"name\":\"demo\",\"version\":\"1.0.0\",\"scripts\":{\"serve\":"
				+ "\"node -e \\\"require('http').createServer(function (q, s) { s.end('ok'); }).listen(" + port + ")\\\"\"}}");
	}

	@After
	public void cleanup() throws Exception {
		support.cleanup();
	}

	private static boolean listens(int port) {
		try (Socket socket = new Socket("127.0.0.1", port)) {
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	private long serve(String mode) throws Exception {
		Process engine = support.java(ManagedProcessLauncher.class, workspace.getAbsolutePath(), "off", mode);
		BufferedReader reader = ProcessTestSupport.reader(engine);
		long npm = Long.parseLong(ProcessTestSupport.readLine(reader, mode + " ", Duration.ofSeconds(20)));
		support.own(npm);
		assertNotNull(ProcessTestSupport.readLine(reader, "ready ", Duration.ofSeconds(10)));
		assertTrue("the server listens", ProcessTestSupport.waitFor(() -> listens(port), Duration.ofSeconds(20)));
		engine.destroyForcibly().waitFor();
		return npm;
	}

	/** As before: started with ProcessBuilder.start, the server outlives the killed engine and keeps its port. */
	@Test
	public void beforeTheServerOutlivesAKilledEngine() throws Exception {
		serve("legacy-serve");
		Thread.sleep(3000);
		assertTrue("the orphan keeps its port", listens(port));
	}

	/** Managed: the server stops with the killed engine, its port is free. */
	@Test
	public void theManagedServerStopsWithAKilledEngine() throws Exception {
		long npm = serve("app-serve");
		assertTrue("the port is free", ProcessTestSupport.waitFor(() -> !listens(port), Duration.ofSeconds(3)));
		assertTrue(ProcessTestSupport.waitDead(npm, Duration.ofSeconds(3)));
		assertFalse(listens(port));
	}
}
