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

package com.twinsoft.convertigo.eclipse.views.references.model;

import com.twinsoft.convertigo.beans.core.DatabaseObject;

public class DboNode extends AbstractParentNode {
	
	public DboNode(
			AbstractParentNode parent, String name,
			DatabaseObject target) {
		super(parent, name);
		setTarget(target);
	}

	private DatabaseObject target;
	private DatabaseObject source;

	public DatabaseObject getTarget() {
		return target;
	}

	public void setTarget(DatabaseObject target) {
		this.target = target;
	}

	public DatabaseObject getSource() {
		return source;
	}

	public void setSource(DatabaseObject source) {
		this.source = source;
	}

	@Override
	public String getName() {
		/*
		 * treat speacial case, see ViewRefLabelProvider.java
		 * public Image getImage(Object element)
		 */
		if (target == null) {
			if (name.contains("entry") || name.contains("exit"))
				return name;
		}

		return (target == null ? name + " (broken reference)" : name);
	}
}
