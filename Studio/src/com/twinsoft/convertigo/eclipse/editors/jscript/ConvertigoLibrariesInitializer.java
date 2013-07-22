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

package com.twinsoft.convertigo.eclipse.editors.jscript;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer;
import org.eclipse.wst.jsdt.core.compiler.libraries.LibraryLocation;

public class ConvertigoLibrariesInitializer extends JsGlobalScopeContainerInitializer {
	private static final String LIBRARY_ID = "com.twinsoft.convertigo.js_lib_id";

	private static char[][] stringsToCharses(String strs[]) {
		char[][] charses = new char[strs.length][];
		for (int i = 0; i < strs.length; ++i) {
			charses[i] = strs[i].toCharArray();
		}
		return charses;
	}

	private static final String[] LIB_STRS = { "javelin.js" };
	private static final char[][] LIB_CHARSES = stringsToCharses(LIB_STRS);

	public char[][] getLibraryFileNames() {
		return ConvertigoLibrariesInitializer.LIB_CHARSES;
	}

	// Q: When is an IPath not a path?
	public IPath getPath() {
		// A: When it's a library ID.
		return new Path(LIBRARY_ID);
	}

	public LibraryLocation getLibraryLocation() {
		return null;
	}

}
