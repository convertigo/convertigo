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

package com.twinsoft.convertigo.engine.admin.services.studio.ngxbuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Test;

/** The state of a development build the agents of the Assistant read through the MCP. */
public class WsBuilderDevBuildStateTest {
	private static final String PROJECT = "StateApp";

	@After
	public void forget() {
		synchronized (WsBuilder.builds) {
			WsBuilder.builds.remove(WsBuilder.key(PROJECT, true));
			WsBuilder.builds.remove(WsBuilder.key(PROJECT, false));
		}
	}

	private static WsBuilder.Build register(String mode) {
		var build = new WsBuilder.Build(PROJECT, "http://localhost:28080/convertigo", mode, "");
		synchronized (WsBuilder.builds) {
			WsBuilder.builds.put(WsBuilder.key(PROJECT, "dev".equals(mode)), build);
		}
		return build;
	}

	@Test
	public void noDevelopmentBuildHasNoState() {
		assertNull(WsBuilder.devBuildState(PROJECT));
		register("prod");
		assertNull("a local build is not the development build", WsBuilder.devBuildState(PROJECT));
	}

	@Test
	public void theStateFollowsTheBuildUntilTheApplicationIsServed() throws Exception {
		var build = register("dev");
		build.state = "serving";
		build.phase = "installing";
		var installing = new JSONObject(WsBuilder.devBuildState(PROJECT));
		assertEquals("serving", installing.getString("state"));
		assertEquals("installing", installing.getString("phase"));
		assertEquals("", installing.getString("compiled"));
		assertEquals("", installing.getString("url"));
		assertEquals("", installing.getString("localUrl"));

		build.phase = "";
		build.compiled = "success";
		build.portNode = 48046;
		build.baseUrl = "/convertigo/projects/StateApp/DisplayObjects/dev48046/";
		var served = new JSONObject(WsBuilder.devBuildState(PROJECT));
		assertEquals("success", served.getString("compiled"));
		assertEquals("", served.getString("phase"));
		assertEquals(48046, served.getInt("port"));
		assertEquals("http://localhost:48046/convertigo/projects/StateApp/DisplayObjects/dev48046/", served.getString("localUrl"));

		build.stopped = true;
		assertNull("a stopped build serves nothing", WsBuilder.devBuildState(PROJECT));
	}
}
