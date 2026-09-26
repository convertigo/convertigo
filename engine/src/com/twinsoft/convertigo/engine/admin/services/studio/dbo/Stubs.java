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

import java.io.File;
import java.util.TreeSet;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

/**
 * The stubs of a sequence or a transaction, the files of the stubs folder of its project recorded for it,
 * which the Eclipse Studio offers to execute from.
 * <ul>
 * <li>id: the tree id of the sequence or the transaction</li>
 * </ul>
 */
@ServiceDefinition(name = "Stubs", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_VIEW }, parameters = {}, returnValue = "")
public class Stubs extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var id = request.getParameter("id");
		if (id == null || id.isBlank()) {
			throw new ServiceException("missing id parameter");
		}
		if (!(Utils.getDbo(id) instanceof RequestableObject requestable)) {
			throw new ServiceException("Only a sequence or a transaction has stubs.");
		}
		var names = new TreeSet<String>();
		var files = new File(requestable.getProject().getDirPath(), "stubs").listFiles(
				(dir, name) -> name.endsWith(".xml"));
		for (var file : files == null ? new File[0] : files) {
			if (isStubOf(file, requestable)) {
				names.add(file.getName());
			}
		}
		var stubs = new JSONArray();
		for (var name : names) {
			stubs.put(name);
		}
		response.put("stubs", stubs);
		response.put("defaultStub", requestable.getDefaultStubFileName());
	}

	/**
	 * @return whether the root of the XML file names the requestable, as a stub written for it
	 */
	private static boolean isStubOf(File file, RequestableObject requestable) {
		try {
			var root = XMLUtils.parseDOM(file).getDocumentElement();
			if (requestable instanceof Sequence) {
				return requestable.getName().equals(root.getAttribute("sequence"));
			}
			return requestable instanceof Transaction
					&& requestable.getName().equals(root.getAttribute("transaction"))
					&& requestable.getParent().getName().equals(root.getAttribute("connector"));
		} catch (Exception e) {
			return false;
		}
	}
}
