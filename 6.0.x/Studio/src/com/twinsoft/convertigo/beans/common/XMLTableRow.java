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

package com.twinsoft.convertigo.beans.common;

import org.w3c.dom.Node;

import com.twinsoft.convertigo.engine.util.GenericUtils;

public class XMLTableRow extends XMLVector<Object> {

	private static final long serialVersionUID = -1740199960104952346L;

	private transient XMLVector<XMLVector<Object>> columns = null;

	private transient String name = null;
	
	private transient String xpath = null;
	
	public static XMLTableRow create() {
		XMLVector<Object> row = new XMLVector<Object>();
		row.add("row");
		row.add(".//TR");
		row.add(new XMLVector<XMLVector<Object>>());
		return new XMLTableRow(row);
	}

	public XMLTableRow() {
		super();
	}
	
	public XMLTableRow(XMLVector<Object> v) {
		super(v);
		name = (String)get(0);
		xpath = (String)get(1);
		columns = GenericUtils.cast(get(2));
	}
	
	@Override
	public void readXml(Node node) throws Exception {
		super.readXml(node);
		name = (String)get(0);
		xpath = (String)get(1);
		columns = GenericUtils.cast(get(2));
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		if ((name != null) && (!name.equals(""))) {
			this.name = name;
			set(0, name);
		}
	}

	public String getXpath() {
		return xpath;
	}

	public void setXpath(String xpath) {
		if ((xpath != null) && (!xpath.equals(""))) {
			this.xpath = xpath;
			set(1, xpath);
		}
	}

	public XMLVector<XMLVector<Object>> getColumns() {
		return columns;
	}
	
	public void setColumns(XMLVector<XMLVector<Object>> v) {
		if (v != null) {
			this.columns = v;
			set(2, v);
		}
	}
	
	public XMLVector<Object> addColumn(String colName, String colXpath, boolean bExtractChilds) {
		XMLVector<Object> v = new XMLVector<Object>();
		v.add(colName);
		v.add(colXpath);
		v.add(new Boolean(bExtractChilds));
		columns.add(v);
		return v;
	}
	
	public XMLVector<Object> getColumn(int index) {
		return columns.get(index);
	}
	
	public String getColumnName(int index) {
		return getColumn(index).get(0).toString();
	}
	
	public void setColumnName(int index, String columnName) {
		getColumn(index).set(0, columnName);
	}
	
	public String getColumnXPath(int index) {
		return getColumn(index).get(1).toString();
	}
	
	public void setColumnXPath(int index, String xpath) {
		getColumn(index).set(1, xpath);
	}
	
	public boolean hasToExtract(int index) {
		return ((Boolean)getColumn(index).get(2)).booleanValue();
	}
	
	public XMLVector<Object> toXMLVector() {
		XMLVector<Object> xmlv = new XMLVector<Object>();
		xmlv.add(name);
		xmlv.add(xpath);
		xmlv.add(columns);
		return xmlv;
	}	
}
