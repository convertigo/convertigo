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

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractParentNode extends AbstractNode {
	
	protected AbstractParentNode(AbstractParentNode parent, String name) {
		super(parent, name);
		children = new ArrayList<AbstractNode>();
	}

	private List<AbstractNode> children;
	
	public void addChild(AbstractNode child) {
		children.add(child);
		child.setParent(this);
	}

	public List<AbstractNode> getChildren() {
		return children;
	}
	
	public boolean hasChildren() {
		return children.size()>0;
	}

}
