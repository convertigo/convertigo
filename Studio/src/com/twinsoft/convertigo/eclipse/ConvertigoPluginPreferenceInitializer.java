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

public class ConvertigoPluginPreferenceInitializer extends AbstractPreferenceInitializer {
	
	@Override
	public void initializeDefaultPreferences() {
		IEclipsePreferences node = DefaultScope.INSTANCE.getNode("com.twinsoft.convertigo.studio");
		node.put(ConvertigoPlugin.PREFERENCE_LOG_LEVEL, "3");
		node.put(ConvertigoPlugin.PREFERENCE_OPENED_CONSOLES, "");
		node.put(ConvertigoPlugin.PREFERENCE_TRACEPLAYER_PORT, "2323");
		node.put(ConvertigoPlugin.PREFERENCE_TREE_HIGHLIGHT_DETECTED, "true");
		node.put(ConvertigoPlugin.PREFERENCE_IGNORE_NEWS, "false");
	}
}
