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

package com.twinsoft.convertigo.beans.references;

import java.net.URL;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;

import com.twinsoft.convertigo.beans.core.ISchemaReader;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.SchemaUtils;

public abstract class XsdSchemaReference extends RemoteFileReference implements ISchemaReference, ISchemaReader {

	private static final long serialVersionUID = -7369046768171657947L;

	public XmlSchema readSchema(XmlSchemaCollection collection) {
		try {
			URL xsdURL = getReferenceUrl();
			if (xsdURL != null) {
				// loads schema
				XmlSchema xmlSchema = SchemaUtils.loadSchema(xsdURL, collection);
				
				// overwrites elementFormDefault, attributeFormDefault
				//xmlSchema.setElementFormDefault(new XmlSchemaForm(Project.XSD_FORM_UNQUALIFIED));
				//xmlSchema.setAttributeFormDefault(new XmlSchemaForm(Project.XSD_FORM_UNQUALIFIED));
				return xmlSchema;
			}
		} catch (Exception e) {
			Engine.logBeans.error(e.toString());
		}
		return null;
	}
	
}
