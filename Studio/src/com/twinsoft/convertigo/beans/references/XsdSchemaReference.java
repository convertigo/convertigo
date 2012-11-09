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

package com.twinsoft.convertigo.beans.references;

import java.net.URL;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;

import com.twinsoft.convertigo.beans.core.ISchemaReader;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.SchemaUtils;

public abstract class XsdSchemaReference extends RemoteFileReference implements ISchemaReader {

	private static final long serialVersionUID = -7369046768171657947L;

	public XmlSchema readSchema(XmlSchemaCollection collection) {
		try {
			URL xsdURL = getReferenceUrl();
			if (xsdURL != null) {
				return SchemaUtils.loadSchema(xsdURL, collection);
			}
		} catch (Exception e) {
			Engine.logBeans.error(e.getMessage());
		}
		return null;
	}
	
}
