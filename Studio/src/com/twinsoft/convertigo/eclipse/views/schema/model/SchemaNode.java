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

import org.w3c.dom.Element;

public class SchemaNode extends XsdNode {

	private DirectivesFolder directives;
	private AttributesFolder attributes;
	private ElementsFolder elements;
	private GroupsFolder groups;
	private TypesFolder types;
	
	public SchemaNode(TreeParentNode parent, Element object) {
		super(parent, object);
		
		// Add folders
		addChild(directives = new DirectivesFolder(this, "Directives"));
		addChild(attributes = new AttributesFolder(this, "Attributes"));
		addChild(elements = new ElementsFolder(this, "Elements"));
		addChild(groups = new GroupsFolder(this, "Groups"));
		addChild(types = new TypesFolder(this, "Types"));
		
		// Add children
		handleChidren();
	}

	
	@Override
	public String getName() {
		return getObject().getAttribute("targetNamespace");
	}

	@Override
	public void addChild(TreeNode child) {
		if (child instanceof ImportNode) {
			directives.addChild(child);
		}
		else if (child instanceof IncludeNode) {
			directives.addChild(child);
		}
		else if (child instanceof AttributeNode) {
			attributes.addChild(child);
		}
		else if (child instanceof ElementNode) {
			elements.addChild(child);
		}
		else if (child instanceof TypeNode) {
			types.addChild(child);
		}
		else if (child instanceof GroupNode) {
			groups.addChild(child);
		}
		else {
			super.addChild(child);
		}
	}
}
