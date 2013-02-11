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

package com.twinsoft.convertigo.eclipse.views.projectexplorer.model;

import org.eclipse.jface.viewers.Viewer;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.PropertyData;
import com.twinsoft.convertigo.engine.EngineException;

public class XMLRecordDescriptionTreeObject extends PropertyTableTreeObject {

	/**
	 * @param viewer
	 * @param propertyName
	 * @param data
	 * @param databaseObjectTreeObject
	 */
	public XMLRecordDescriptionTreeObject(Viewer viewer, String propertyName, XMLVector<XMLVector<Object>> data, DatabaseObjectTreeObject databaseObjectTreeObject) {
		super(viewer, propertyName, data, databaseObjectTreeObject);
	}
	
	public static Object read(Node node) throws EngineException {
		PropertyData pd = (PropertyData)PropertyTableTreeObject.read(node);
		if (pd != null)
			return new PropertyData(XMLRecordDescriptionTreeObject.class,pd.getData());
		return null;
	}

	@Override
	protected Class<? extends PropertyTableRowTreeObject> getRowClass() {
		return XMLRecordDescriptionRowTreeObject.class;
	}

	@Override
	protected XMLVector<Object> createRow(String name) {
		XMLVector<Object> row = new XMLVector<Object>();
		row.add(name);
		row.add(".//TR");
		row.add(Boolean.FALSE);
		return row;
	}

	@Override
	protected PropertyTableRowTreeObject newRow(XMLVector<Object> row) {
		return new XMLRecordDescriptionRowTreeObject(viewer, row);
	}

	@Override
	public String getRowDefaultLabel() {
		return "Data";
	}

	@Override
	public boolean testAttribute(Object target, String name, String value) {
		if (name.equals("acceptColumns")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.FALSE);
		}
		return super.testAttribute(target, name, value);
	}
}
