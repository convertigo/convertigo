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

package com.twinsoft.convertigo.beans.core;

import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.DatabaseObject.DboCategoryInfo;
import com.twinsoft.convertigo.beans.core.DatabaseObject.DboFolderType;
import com.twinsoft.convertigo.engine.enums.FolderType;

@DboCategoryInfo(
		getCategoryId = "Index",
		getCategoryName = "Index",
		getIconClassCSS = "convertigo-action-newIndex"
	)
@DboFolderType(type = FolderType.INDEX)
public abstract class Index extends DatabaseObject {

	private static final long serialVersionUID = 9150595374464464408L;
	
	public Index() {
		super();
		databaseType = "Index";
	}

	@Override
	public void preconfigure(Element element) throws Exception {
		super.preconfigure(element);
	}
	
	@Override
	public Index clone() throws CloneNotSupportedException {
		Index clonedObject =  (Index) super.clone();
		return clonedObject;
	}
	
	public String getRenderer() {
		return "IndexTreeObject";
	}
}
