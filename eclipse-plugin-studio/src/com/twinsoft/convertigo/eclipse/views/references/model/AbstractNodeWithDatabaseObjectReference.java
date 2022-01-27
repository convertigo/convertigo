/*
 * Copyright (c) 2001-2022 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.views.references.model;

import com.twinsoft.convertigo.beans.core.DatabaseObject;

public abstract class AbstractNodeWithDatabaseObjectReference extends AbstractParentNode {
	
	protected AbstractNodeWithDatabaseObjectReference(
			AbstractParentNode parent, String name,
			DatabaseObject refDatabaseObject) {
		super(parent, name);
		setRefDatabaseObject(refDatabaseObject);
	}

	private DatabaseObject refDatabaseObject;

	public DatabaseObject getRefDatabaseObject() {
		return refDatabaseObject;
	}

	public void setRefDatabaseObject(DatabaseObject refDatabaseObject) {
		this.refDatabaseObject = refDatabaseObject;
	}

	@Override
	public String getName() {
		/*
		 * treat speacial case, see ViewRefLabelProvider.java
		 * public Image getImage(Object element)
		 */
		if (refDatabaseObject == null) {
			if (name.contains("entry") || name.contains("exit"))
				return name;
		}

		return (refDatabaseObject == null ? name + " (broken reference)" : name);
	}
}
