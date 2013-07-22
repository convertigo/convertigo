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

import com.twinsoft.convertigo.eclipse.views.projectexplorer.DatabaseObjectTreeObject;

public class TransactionSheetLocationEditor extends PropertyWithTagsEditor {

    public static final String NONE = "None";
    public static final String FROM_TRANSACTION = "From transaction";
    public static final String FROM_LAST_DETECTED_SCREENCLASS = "From last detected screen class";
    
    public static String[] getTags(DatabaseObjectTreeObject databaseObjectTreeObject) {
    	return new String[] { NONE, FROM_TRANSACTION, FROM_LAST_DETECTED_SCREENCLASS };
    }

}