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

package com.convertigo.jdbc;

import java.io.File;

public class CommonDriver {

	public static void init(String classPath, String jarName, String howToGetIt) {
		if (howToGetIt == null || howToGetIt == "") {
			howToGetIt = "ask to your database administrator to have one.";
		}
		
		String message = "(" + classPath + ") This is not the true " + jarName + ", due to license issue we cannot provide it.\n\n"
				+ "If you do not have an official " + jarName + ", " + howToGetIt + "\n\n";
		try {
			Class<?> engine = Class.forName("com.twinsoft.convertigo.engine.Engine");
			String workspacePath = "" + engine.getField("USER_WORKSPACE_PATH").get(null);
			String webappPath = "" + engine.getField("WEBAPP_PATH").get(null);
			boolean isContainer = (Boolean) engine.getMethod("isContainer").invoke(null);
			String libsPath = new File(workspacePath + "/libs/").getCanonicalPath();
			String webappLibPath = new File(webappPath + "/WEB-INF/lib/").getCanonicalPath();

			message += "Where to install the official " + jarName + " depends on what uses it:\n"
					+ "- SQL connectors only: put it in the {project}/libs directory of the project, or in " + libsPath + " for all projects;\n"
					+ "- database cache or analytics database: it must replace the placeholder in " + webappLibPath;
			if (isContainer) {
				// the official Docker image copies <workspace>/lib into WEB-INF/lib at each start
				String libPath = new File(workspacePath + "/lib/").getCanonicalPath();
				message += "\n  (in this container, put it in the persistent folder " + libPath
						+ ": it is copied into the web application at each start, then restart the container).";
			} else {
				message += ".";
			}
			message += "\nA README.md file in each of these workspace folders gives the details.";
			Object logger = engine.getField("logEngine").get(null);
			Class<?> c_logger = logger.getClass();
			c_logger.getMethod("error", Object.class).invoke(logger, message);
		} catch(Exception e) {
			message += "Sorry, we can not determine where your Convertigo instance is due to " + e.getClass().getName() + " : " + e.getMessage();
		}
		message = "============================================\n\n" + message
				+ "\n\n============================================";
		System.err.println(message);
		throw new FakeJarException(message);
	}
	
}
