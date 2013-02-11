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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/branches/6.2.x/Studio/src/com/twinsoft/convertigo/eclipse/property_editors/HttpVerbEditor.java $
 * $Author: fabienb $
 * $Revision: 33430 $
 * $Date: 2013-01-29 17:54:06 +0100 (mar., 29 janv. 2013) $
 */

package com.twinsoft.convertigo.eclipse.property_editors;

import com.twinsoft.convertigo.beans.transactions.JsonHttpTransaction;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;

public class JsonArrayTranslationPolicyEditor extends PropertyWithTagsEditor {

    public static String[] getTags(DatabaseObjectTreeObject databaseObjectTreeObject) {
        return JsonHttpTransaction.JSON_ARRAY_TRANSLATION_POLICY;
    }
}
