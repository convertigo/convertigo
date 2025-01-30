/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.swt.SWT;

public class ConvertigoPluginPreferenceInitializer extends AbstractPreferenceInitializer {
	
	@Override
	public void initializeDefaultPreferences() {
		IEclipsePreferences node = DefaultScope.INSTANCE.getNode("com.twinsoft.convertigo.studio");
		node.put(ConvertigoPlugin.PREFERENCE_LOG_LEVEL, "3");
		node.put(ConvertigoPlugin.PREFERENCE_OPENED_CONSOLES, "");
		node.put(ConvertigoPlugin.PREFERENCE_TRACEPLAYER_PORT, "2323");
		node.put(ConvertigoPlugin.PREFERENCE_TREE_HIGHLIGHT_DETECTED, "false");
		node.put(ConvertigoPlugin.PREFERENCE_IGNORE_NEWS, "false");
		node.put(ConvertigoPlugin.PREFERENCE_LOCAL_BUILD_FOLDER, SWT.getPlatform().startsWith("win") ? "C:\\TMP\\C8O_build" : "/tmp/C8O_build");
		node.put(ConvertigoPlugin.PREFERENCE_BROWSER_OFFSCREEN, "false");
	}
}
