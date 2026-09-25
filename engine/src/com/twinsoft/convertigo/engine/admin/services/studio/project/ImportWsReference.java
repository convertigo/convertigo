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

package com.twinsoft.convertigo.engine.admin.services.studio.project;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.references.RemoteFileReference;
import com.twinsoft.convertigo.beans.references.RestServiceReference;
import com.twinsoft.convertigo.beans.references.WebServiceReference;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.enums.Accessibility;
import com.twinsoft.convertigo.engine.util.WsReference;

/**
 * Imports a remote web service into a project, as the web service reference wizards of the Eclipse
 * Studio: a reference to its REST (Swagger or OpenAPI) or SOAP (WSDL) definition is added to the project,
 * with an HTTP connector and a transaction per operation, and a sequence per transaction if asked.
 * <ul>
 * <li>projectName: the project that receives the web service</li>
 * <li>type: rest or soap</li>
 * <li>url: the URL of the definition</li>
 * <li>user, password: the credentials to read the definition, if it needs some</li>
 * <li>sequences: Public, Hidden or Private to create a sequence per transaction with this accessibility,
 * none otherwise; sequencesAuthenticated: true for sequences requiring an authenticated context</li>
 * </ul>
 */
@ServiceDefinition(name = "ImportWsReference", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
public class ImportWsReference extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var projectName = request.getParameter("projectName");
		var url = request.getParameter("url");
		if (projectName == null || url == null || url.isBlank()) {
			throw new ServiceException("missing projectName or url parameter");
		}
		var project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
		if (project == null) {
			throw new ServiceException("The project " + projectName + " does not exist.");
		}
		RemoteFileReference reference = "soap".equals(request.getParameter("type"))
				? new WebServiceReference()
				: new RestServiceReference();
		reference.bNew = true;
		reference.setUrlpath(url.trim());
		var user = request.getParameter("user");
		if (user != null && !user.isBlank()) {
			reference.setNeedAuthentication(true);
			reference.setAuthUser(user);
			reference.setAuthPassword(request.getParameter("password") == null ? "" : request.getParameter("password"));
		}
		Accessibility sequences = null;
		try {
			sequences = Accessibility.valueOf(request.getParameter("sequences"));
		} catch (Exception e) {
			// no sequence
		}
		var connector = importInto(project, reference, sequences == null ? null
				: new WsReference.CreateSequenceOptions(sequences, "true".equals(request.getParameter("sequencesAuthenticated"))));
		response.put("done", connector != null);
		if (connector != null) {
			response.put("id", connector.getQName(true));
			response.put("transactions", connector.getTransactionsList().size());
		}
	}

	/**
	 * Imports or updates a reference in its project.
	 *
	 * @return the HTTP connector of the web service
	 */
	public static com.twinsoft.convertigo.beans.connectors.HttpConnector importInto(Project project,
			RemoteFileReference reference, WsReference.CreateSequenceOptions sequences) throws Exception {
		synchronized (WsReference.class) {
			// the sequences to create are an option the import reads when it is created
			WsReference.nextCreateSequences = sequences;
			try {
				var importer = reference instanceof WebServiceReference soap
						? new com.twinsoft.convertigo.engine.util.ImportWsReference(soap)
						: new com.twinsoft.convertigo.engine.util.ImportWsReference((RestServiceReference) reference);
				var connector = importer.importInto(project);
				Engine.theApp.schemaManager.clearCache(project.getName());
				return connector;
			} finally {
				WsReference.nextCreateSequences = null;
			}
		}
	}
}
