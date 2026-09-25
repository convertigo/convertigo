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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.xml.sax.SAXException;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.SchemaManager.Option;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

/**
 * The XML schema of the project of an object, as the Schema view of the Eclipse Studio generates and
 * validates it: one XSD per namespace of the project and of its references.
 * <ul>
 * <li>id: the tree id of an object of the project</li>
 * <li>full: true for the full schema, with the internal types of the transactions</li>
 * <li>refresh: true to generate it again instead of using the cached one</li>
 * </ul>
 */
@ServiceDefinition(name = "Schema", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_VIEW }, parameters = {}, returnValue = "")
public class Schema extends JSonService {
	private static final String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema";

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var id = request.getParameter("id");
		if (id == null || id.isBlank()) {
			throw new ServiceException("missing id parameter");
		}
		var dbo = Utils.getDbo(id);
		if (dbo == null || dbo.getProject() == null) {
			throw new ServiceException("The object " + id + " does not exist.");
		}
		var projectName = dbo.getProject().getName();
		if ("true".equals(request.getParameter("refresh"))) {
			Engine.theApp.schemaManager.clearCache(projectName);
		}
		var collection = "true".equals(request.getParameter("full"))
				? Engine.theApp.schemaManager.getSchemasForProject(projectName, Option.fullSchema)
				: Engine.theApp.schemaManager.getSchemasForProject(projectName);

		var schemas = new JSONArray();
		for (var schema : collection.getXmlSchemas()) {
			var namespace = schema.getTargetNamespace();
			if (XSD_NAMESPACE.equals(namespace)) {
				continue;
			}
			var out = new ByteArrayOutputStream();
			schema.write(out);
			schemas.put(new JSONObject()
					.put("namespace", namespace == null ? "" : namespace)
					.put("xsd", out.toString(StandardCharsets.UTF_8)));
		}

		var valid = true;
		var message = "The " + projectName + " schema is valid.";
		try {
			XmlSchemaUtils.validate(collection);
		} catch (SAXException e) {
			valid = false;
			message = "The " + projectName + " schema is invalid: " + e.getMessage();
		}

		response.put("project", projectName);
		response.put("schemas", schemas);
		response.put("valid", valid);
		response.put("message", message);
	}
}
