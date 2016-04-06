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

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaImport;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.SchemaManager.Option;

public class ProjectSchemaReference extends ImportXsdSchemaReference {
	private static final long serialVersionUID = 6345829826119228229L;
	
	private String projectName = "";

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	
	@Override
	public XmlSchemaImport getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaImport schemaImport = new XmlSchemaImport();
		try {
			String pname = getProjectName();
			if (pname.equals("")) {
				throw new EngineException("Incorrect schema import: referenced Convertigo project name is empty");
			}
			if (pname.equals(getProject().getName())) {
				throw new EngineException("Incorrect schema import: a project cannot reference itself");
			}
			if (!Engine.theApp.databaseObjectsManager.existsProject(pname)) {
				throw new EngineException("Incorrect schema import: the project "+ pname +" does not exist");
			}
			
			// load schema
			XmlSchemaCollection xmlSchemaCollection = Engine.theApp.schemaManager.getSchemasForProject(pname, Option.noCache);
			for (XmlSchema xmlSchema : xmlSchemaCollection.getXmlSchemas()) {
				if (collection.schemaForNamespace(xmlSchema.getTargetNamespace()) != null) continue;
				collection.read(xmlSchema.getSchemaDocument(), null);
			}
			
			String tns = Project.getProjectTargetNamespace(pname);
			XmlSchema importedSchema = collection.schemaForNamespace(tns);
			if (importedSchema != null) {
				schemaImport.setNamespace(importedSchema.getTargetNamespace());
				schemaImport.setSchema(importedSchema);
			}
		} catch (Exception e) {
			if (e instanceof EngineException)
				Engine.logBeans.error(e.getMessage());
			e.printStackTrace();
		}
		return schemaImport;
		
	}
	
	@Override
	public String toString() {
		String label = "";
		try {
			// Check for project
			Project p = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(getProjectName());
			if (p == null) {
				label = "! broken project !";
			}
		}
		catch (Exception e) {
			label = "! broken project !";
		}
		
		if (label.isEmpty()) {
			return getProjectName();
		} else {
			return getProjectName() + " " + label;
		}
	}
}
