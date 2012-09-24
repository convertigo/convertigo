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

package com.twinsoft.convertigo.eclipse.views.schema;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.twinsoft.convertigo.eclipse.views.schema.model.TreeNode;

public class SchemaViewContentProvider implements ITreeContentProvider {

	public SchemaViewContentProvider() {
		
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	public Object[] getElements(Object object) {
		return getChildren(object);
	}

	public Object[] getChildren(Object object) {
		return ((TreeNode) object).getChildren().toArray();
	}

	public Object getParent(Object object) {
		return ((TreeNode) object).getParent();
	}

	public boolean hasChildren(Object object) {
		return ((TreeNode) object).hasChildren();
	}

}
