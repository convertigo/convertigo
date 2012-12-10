/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.eclipse;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import com.twinsoft.convertigo.engine.ProductVersion;

public class ConvertigoPluginPreferenceInitializer extends AbstractPreferenceInitializer {
	
	private static final String REMOTE_HELP_HOST = "help.convertigo.com";
	private static final String REMOTE_HELP_PATH = "/" + ProductVersion.helpVersion + "/";
	private static final String REMOTE_HELP_NAME = "Convertigo help " + ProductVersion.helpVersion;
	private static final int REMOTE_HELP_PORT = 80;
	
	@Override
	public void initializeDefaultPreferences() {
		IEclipsePreferences node = new DefaultScope().getNode("com.twinsoft.convertigo.studio");
		node.put(ConvertigoPlugin.PREFERENCE_LOG_LEVEL, "3");
		node.put(ConvertigoPlugin.PREFERENCE_OPENED_CONSOLES, "");
		node.put(ConvertigoPlugin.PREFERENCE_TRACEPLAYER_PORT, "2323");
		node.put(ConvertigoPlugin.PREFERENCE_TREE_HIGHLIGHT_DETECTED, "true");
		node.put(ConvertigoPlugin.PREFERENCE_IGNORE_NEWS, "false");
		
		boolean found = false;
		String [] remoteHelpPaths;

		IEclipsePreferences rootNode = new InstanceScope().getNode("org.eclipse.help.base");
		rootNode.putBoolean("remoteHelpOn", true);
		rootNode.getBoolean("remoteHelpPreferred", false);	
		
		if (rootNode.get("remoteHelpPath", "").equals("")) {
			rootNode.put("remoteHelpName", rootNode.get("remoteHelpName", "") + REMOTE_HELP_NAME);
			rootNode.put("remoteHelpHost", rootNode.get("remoteHelpHost", "") + REMOTE_HELP_HOST);
			rootNode.put("remoteHelpPath", rootNode.get("remoteHelpPath", "") + REMOTE_HELP_PATH);
			rootNode.put("remoteHelpPort", rootNode.get("remoteHelpPort", "") + REMOTE_HELP_PORT);
			rootNode.put("remoteHelpICEnabled", rootNode.get("remoteHelpICEnabled", "") + "true");
		}
		else {
			remoteHelpPaths = rootNode.get("remoteHelpPath", "").split(",");	
			for (String remoteHelpPath: remoteHelpPaths) {
				if (remoteHelpPath.equals(REMOTE_HELP_PATH)) {
					found = true;
				}			
			}
			if (!found) {
				rootNode.put("remoteHelpName", rootNode.get("remoteHelpName", "") + "," + REMOTE_HELP_NAME);
				rootNode.put("remoteHelpHost", rootNode.get("remoteHelpHost", "") + "," + REMOTE_HELP_HOST);
				rootNode.put("remoteHelpPath", rootNode.get("remoteHelpPath", "") + "," + REMOTE_HELP_PATH);
				rootNode.put("remoteHelpPort", rootNode.get("remoteHelpPort", "") + "," + REMOTE_HELP_PORT);
				rootNode.put("remoteHelpICEnabled", rootNode.get("remoteHelpICEnabled", "") + "," +  "true");
			}
		}
	}
}
