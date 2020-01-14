/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

package com.twinsoft.convertigo.beans.couchdb;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.connectors.CouchDbConnector;
import com.twinsoft.convertigo.beans.core.Index;

public class JsonIndex extends Index {

	private static final long serialVersionUID = -1523783503757936791L;
	
	private XMLVector<XMLVector<String>> fields = new XMLVector<XMLVector<String>>();
	private boolean ascending = true;
	
	public JsonIndex() {
		super();
	}

	@Override
	public JsonIndex clone() throws CloneNotSupportedException {
		JsonIndex clonedObject =  (JsonIndex) super.clone();
		return clonedObject;
	}
	
	@Override
	public CouchDbConnector getConnector() {
		return (CouchDbConnector) super.getConnector();
	}

	public XMLVector<XMLVector<String>> getFields() {
		return fields;
	}

	public void setFields(XMLVector<XMLVector<String>> fields) {
		this.fields = fields;
	}

	public boolean getAscending() {
		return ascending;
	}

	public void setAscending(boolean ascending) {
		this.ascending = ascending;
	}
}
