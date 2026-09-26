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

package com.twinsoft.convertigo.engine.admin.services.studio.dbo;

import java.util.HashMap;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.connectors.CouchDbConnector;
import com.twinsoft.convertigo.beans.couchdb.DesignDocument;
import com.twinsoft.convertigo.beans.couchdb.DesignDocumentView;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchDbManager;

/**
 * Runs a view of a design document of a CouchDB connector, as the Execute actions of a view in the
 * Eclipse Studio: with its reduce function, or without it on its first 50 rows.
 * <ul>
 * <li>id: the view</li>
 * <li>reduce: true to reduce the rows, when the view has a reduce function</li>
 * </ul>
 */
@ServiceDefinition(name = "CouchView", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
public class CouchView extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		if (!(Utils.getDbo(request.getParameter("id")) instanceof DesignDocumentView view)
				|| !(view.getParent() instanceof DesignDocument document)
				|| !(document.getParent() instanceof CouchDbConnector connector)) {
			throw new ServiceException("Only a view of a design document of a CouchDB connector runs.");
		}
		var reduce = view.hasReduce() && "true".equals(request.getParameter("reduce"));
		var query = new HashMap<String, String>();
		query.put("reduce", Boolean.toString(reduce));
		if (!reduce) {
			query.put("limit", "50");
		}
		// the database has the design document as the project defines it
		CouchDbManager.syncDocument(connector);
		var result = connector.getCouchClient().getView(connector.getDatabaseName(), document.getName(), view.getName(), query);
		// the rows, without the details of the HTTP exchange
		result.remove("_c8oMeta");
		response.put("database", connector.getDatabaseName());
		response.put("view", document.getName() + "/" + view.getName());
		response.put("reduce", reduce);
		response.put("result", result);
	}
}
