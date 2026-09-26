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

import java.io.StringReader;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.transactions.CicsTransaction;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;
import com.twinsoft.convertigo.engine.util.Copybook;

/**
 * Imports a COBOL copybook into the input or the output map of a CICS transaction, as the "Import copybook"
 * action of the Eclipse Studio.
 * <ul>
 * <li>id: the tree id of the CICS transaction</li>
 * <li>map: input or output</li>
 * <li>copybook: the text of the copybook</li>
 * </ul>
 */
@ServiceDefinition(name = "ImportCopybook", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
public class ImportCopybook extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var id = request.getParameter("id");
		if (id == null || id.isBlank()) {
			throw new ServiceException("missing id parameter");
		}
		if (!(Utils.getDbo(id) instanceof CicsTransaction transaction)) {
			throw new ServiceException("Only a CICS transaction imports a copybook.");
		}
		var copybook = request.getParameter("copybook");
		if (copybook == null || copybook.isBlank()) {
			throw new ServiceException("The copybook is empty.");
		}
		// a copybook without field would empty the map
		var fields = new Copybook().importFromFile2(new StringReader(copybook)).size();
		if (fields == 0) {
			throw new ServiceException("The copybook has no field to import.");
		}
		var input = !"output".equals(request.getParameter("map"));
		transaction.importCopyBook(input, new StringReader(copybook));
		response.put("done", true);
		response.put("fields", fields);
		response.put("message", "The copybook is imported into the " + (input ? "input" : "output") + " map of "
				+ transaction.getName() + ".");
	}
}
