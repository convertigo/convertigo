/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.services.studio.treeview;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import javax.xml.namespace.QName;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.SchemaUtils;
import com.twinsoft.convertigo.engine.util.StepUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

/**
 * Creates the steps building the XML structure of an element of a schema in a sequence or a step, as the
 * "Create steps structure from XSD schema" action of the Eclipse Studio.
 * <ul>
 * <li>id: the sequence or the step receiving the steps</li>
 * <li>path: a schema file of the project, its includes found beside it; or xsd: the content of a schema</li>
 * <li>element: the qualified name of the element to build, as {namespace}name; missing to list the elements
 * of the schema</li>
 * </ul>
 */
@ServiceDefinition(name = "StepsFromXsd", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
public class StepsFromXsd extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var id = request.getParameter("id");
		var dbo = id == null ? null : Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(id);
		if (!StepsFromXml.handles(dbo)) {
			throw new ServiceException("The object " + id + " cannot hold steps.");
		}
		File temporary = null;
		try {
			File file;
			var path = request.getParameter("path");
			if (path != null && !path.isBlank()) {
				var dir = dbo.getProject().getDirFile().getCanonicalFile();
				file = new File(dir, path).getCanonicalFile();
				if (!file.toPath().startsWith(dir.toPath()) || !file.isFile()) {
					throw new ServiceException("The project has no schema " + path + ".");
				}
			} else {
				var xsd = request.getParameter("xsd");
				if (xsd == null || xsd.isBlank()) {
					throw new ServiceException("missing path or xsd parameter");
				}
				file = temporary = File.createTempFile("c8o-steps-", ".xsd");
				FileUtils.writeStringToFile(file, xsd, StandardCharsets.UTF_8);
			}
			var collection = new XmlSchemaCollection();
			collection.setBaseUri(file.getAbsolutePath());
			var schema = SchemaUtils.loadSchema(file, collection);
			if (schema == null) {
				throw new ServiceException("The schema cannot be read.");
			}
			SchemaMeta.setCollection(schema, collection);
			var element = request.getParameter("element");
			if (element == null || element.isBlank()) {
				response.put("elements", elements(schema));
				return;
			}
			var xso = collection.getElementByQName(QName.valueOf(element));
			if (xso == null) {
				throw new ServiceException("The schema has no element " + element + ".");
			}
			SchemaMeta.setSchema(xso, schema);
			var document = XmlSchemaUtils.getDomInstance(xso);
			var first = document == null ? null : document.getDocumentElement().getFirstChild();
			var sequence = dbo instanceof Sequence main ? main : ((StepWithExpressions) dbo).getSequence();
			var step = first instanceof Element root ? StepUtils.createStepFromSchemaDomModel(sequence, root) : null;
			if (step == null) {
				throw new ServiceException("The element " + element + " makes no step.");
			}
			if (dbo instanceof Sequence main) {
				main.addStep(step);
			} else {
				((StepWithExpressions) dbo).addStep(step);
			}
			sequence.hasChanged = true;
			response.put("done", true);
			response.put("id", step.getFullQName());
		} finally {
			if (temporary != null) {
				FileUtils.deleteQuietly(temporary);
			}
		}
	}

	private static JSONArray elements(XmlSchema schema) throws Exception {
		var names = new ArrayList<String>();
		for (Iterator<QName> iterator = GenericUtils.cast(schema.getElements().getNames()); iterator.hasNext();) {
			names.add(iterator.next().toString());
		}
		Collections.sort(names);
		return new JSONArray(names);
	}
}
