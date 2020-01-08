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

package com.twinsoft.convertigo.engine.studio.responses;


import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.Engine;

public class SetPropertyResponse extends AbstractResponse {

	private String propertyName;

	public SetPropertyResponse(String propertyName) {
		super();
		this.propertyName = propertyName;
	}

	@Override
	public Element toXml(Document document, String qname) throws Exception {
		// Create property
		DatabaseObject dbo = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
		Element property = dbo.toXml(document, propertyName);

		// If we update the source definition, the display name of the dbo will change so we need it
		if (propertyName == "sourceDefinition") {
		    property.setAttribute("name", dbo.toString());
		}

		// Create response
		Element response = super.toXml(document, qname);
		response.setAttribute("message", "Property \"" + propertyName + "\" has been successfully updated.");
		response.appendChild(property);

		return response;
	}
}
