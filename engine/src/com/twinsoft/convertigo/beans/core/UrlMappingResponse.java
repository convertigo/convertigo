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

package com.twinsoft.convertigo.beans.core;

import com.twinsoft.convertigo.beans.core.DatabaseObject.DboCategoryInfo;
import com.twinsoft.convertigo.beans.core.DatabaseObject.DboFolderType;
import com.twinsoft.convertigo.engine.enums.FolderType;

@DboCategoryInfo(
		getCategoryId = "UrlMappingResponse",
		getCategoryName = "Response",
		getIconClassCSS = "convertigo-action-newUrlMappingResponse"
	)
@DboFolderType(type = FolderType.RESPONSE)
public abstract class UrlMappingResponse extends DatabaseObject {

	private static final long serialVersionUID = -8538886126930614080L;

	public UrlMappingResponse() {
		super();
		databaseType = "UrlMappingResponse";
	}
	
	@Override
	public UrlMappingResponse clone() throws CloneNotSupportedException {
		UrlMappingResponse clonedObject = (UrlMappingResponse)super.clone();
		return clonedObject;
	}

	public abstract String getStatusCode();
	public abstract String getStatusText();

	@Override
	public String toString() {
		return getStatusCode()+"-Response";
	}
}
