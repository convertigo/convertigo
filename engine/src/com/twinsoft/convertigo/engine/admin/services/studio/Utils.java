/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.services.studio;

import java.util.regex.Pattern;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.FolderType;

public class Utils {
	public static final Pattern parseQName = Pattern.compile("(.*?)(?::(\\w+?))?");

	public static DatabaseObject getDbo(String id) throws Exception {
		var reg = parseQName.matcher(id);
		reg.matches();
		var ft = FolderType.parse(reg.group(2));
		var qname = ft == null ? id : reg.group(1);
		return Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
	}

	public static FolderType getFolderType(String id) throws Exception {
		var reg = parseQName.matcher(id);
		reg.matches();
		return FolderType.parse(reg.group(2));
	}
}
