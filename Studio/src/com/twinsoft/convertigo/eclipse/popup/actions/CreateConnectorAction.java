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

package com.twinsoft.convertigo.eclipse.popup.actions;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

public class CreateConnectorAction extends DatabaseObjectCreateAction {

	public CreateConnectorAction() {
		super("com.twinsoft.convertigo.beans.core.Connector");
	}

	public void postCreate(TreeObject parentTreeObject, DatabaseObject createdDatabaseObject) throws Exception {
		super.postCreate(parentTreeObject, createdDatabaseObject);
		
		ProjectTreeObject projectTreeObject = (ProjectTreeObject) parentTreeObject;
		projectTreeObject.openConnectorEditor((Connector) createdDatabaseObject);
	}
	
}
