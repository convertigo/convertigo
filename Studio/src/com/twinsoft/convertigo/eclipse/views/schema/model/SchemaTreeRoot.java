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

public class SchemaTreeRoot extends TreeRootNode {

	public SchemaTreeRoot(TreeParentNode parent, String name) {
		super(parent, name);
	}

	public SchemaNode getSchemaNode(String name) {
		return (SchemaNode) findChild(name);
	}
	
	public AttributeNode findAttribute(String namespaceURI, String attributeName) {
		SchemaNode schemaNode = getSchemaNode(namespaceURI);
		if (schemaNode != null) {
			return schemaNode.findAttribute(attributeName);
		}
		return null;
	}

	public TypeNode findType(String namespaceURI, String typeName) {
		SchemaNode schemaNode = getSchemaNode(namespaceURI);
		if (schemaNode != null) {
			return schemaNode.findType(typeName);
		}
		return null;
	}
	
	public TypeNode findElement(String namespaceURI, String elementName) {
		SchemaNode schemaNode = getSchemaNode(namespaceURI);
		if (schemaNode != null) {
			return schemaNode.findElement(elementName);
		}
		return null;
	}
	
	public GroupNode findGroup(String namespaceURI, String groupName) {
		SchemaNode schemaNode = getSchemaNode(namespaceURI);
		if (schemaNode != null) {
			return schemaNode.findGroup(groupName);
		}
		return null;
	}
}
