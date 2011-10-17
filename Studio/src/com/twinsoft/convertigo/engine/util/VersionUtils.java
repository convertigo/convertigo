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

package com.twinsoft.convertigo.engine.util;

import java.util.regex.Pattern;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;

public class VersionUtils {
    
    public static int compareProductVersion(String version1, String version2) {
		boolean bProductVersionCheck = new Boolean(EnginePropertiesManager.getProperty(EnginePropertiesManager.PropertyName.CONVERTIGO_PRODUCT_VERSION_CHECK)).booleanValue();
    	
		if (!bProductVersionCheck) {
			Engine.logEngine.warn("The product version check has been ignored!");
			return 0;
		}
		
		version1 = version1.substring(0, version1.indexOf('.', version1.indexOf('.') + 1));
		version2 = version2.substring(0, version2.indexOf('.', version2.indexOf('.') + 1));
		
		return VersionUtils.compare(version1, version2);
	}
    
	public static int compare(String v1, String v2) {
		String s1 = normalizeVersionString(v1);
		String s2 = normalizeVersionString(v2);
		int cmp = s1.compareTo(s2);
		return cmp;
	}

	public static String normalizeVersionString(String version) {
		return VersionUtils.normalizeVersionString(version, ".", 4);
	}

	public static String normalizeVersionString(String version, String sep, int maxWidth) {
		String[] split = Pattern.compile(sep, Pattern.LITERAL).split(version);
		StringBuilder sb = new StringBuilder();
		for (String s : split) {
			sb.append(String.format("%" + maxWidth + 's', s));
		}
		return sb.toString();
	}

	public static int compareMigrationVersion(String v1, String v2) {
		int i1 = v1.indexOf(".m");
		if (i1 == -1) v1 = "000";
		else v1 = v1.substring(i1 + 2);

		int i2 = v2.indexOf(".m");
		if (i2 == -1) v2 = "000";
		else v2 = v2.substring(i2 +2);

		int cmp = v1.compareTo(v2);
		return cmp;
	}

}
