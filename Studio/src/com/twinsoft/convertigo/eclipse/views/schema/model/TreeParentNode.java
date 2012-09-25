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

package com.twinsoft.convertigo.eclipse.views.schema.model;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

public abstract class TreeParentNode extends TreeNode {
	
	protected TreeParentNode(TreeParentNode parent, String name, Element element) {
		super(parent, name, element);
		children = new ArrayList<TreeNode>();
	}

	private List<TreeNode> children;
	
	public void addChild(TreeNode child) {
		children.add(child);
		child.setParent(this);
	}

	public List<TreeNode> getChildren() {
		return children;
	}
	
	public boolean hasChildren() {
		return children.size()>0;
	}
	
	public TreeNode findChild(String name) {
		if (name != null) {
			for (TreeNode treeNode: children) {
				if (name.equals(treeNode.getName()))
					return treeNode;
			}
		}
		return null;
	}
}
