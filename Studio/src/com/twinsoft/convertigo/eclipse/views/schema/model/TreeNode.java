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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.eclipse.views.schema.model;

import java.util.List;

import org.w3c.dom.Element;

public abstract class TreeNode {
	
	private Element element;
	private TreeParentNode parent;
	private String name;
	
	protected TreeNode(TreeParentNode parent, String name, Element element) {
		this.parent = parent;
		this.name = name;
		this.element = element;
	}
	
	public Element getObject() {
		return element;
	}

	public String getName() {
		return name;
	}

	public void setParent(TreeParentNode parent) {
		this.parent = parent;
	}

	public TreeParentNode getParent() {
		return parent;
	}

	public abstract List<? extends TreeNode> getChildren();	

	public abstract boolean hasChildren();
}
