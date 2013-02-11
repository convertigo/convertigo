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

package com.twinsoft.convertigo.eclipse.property_editors;

import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;

public class EmulatorTechnologyEditor extends PropertyWithTagsEditor {

    public static final String BULLDKU = "Bull DKU 7107";
    public static final String IBM3270 = "IBM 3270";
    public static final String IBM5250 = "IBM 5250 (AS/400)";
    public static final String VDX = "Videotex (Minitel)";
    public static final String VT = "Unix (VT220)";
    
    public static String[] getTags(DatabaseObjectTreeObject databaseObjectTreeObject) {
        return new String[] { BULLDKU, IBM3270, IBM5250, VDX, VT };
    }
    
    public static String[] getEmulatorClassNames(DatabaseObjectTreeObject databaseObjectTreeObject) {
    	String[] emulatorClassNames = new String[5];
		emulatorClassNames[0] = com.twinsoft.api.Session.DKU;
		emulatorClassNames[1] = com.twinsoft.api.Session.SNA;
		emulatorClassNames[2]  = com.twinsoft.api.Session.AS400;
		emulatorClassNames[3]  = com.twinsoft.api.Session.VDX;
		emulatorClassNames[4]  = com.twinsoft.api.Session.VT;
    	return emulatorClassNames;
    }
}
