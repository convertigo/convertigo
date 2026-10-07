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
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;

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
 * Saves the default stub of a sequence or a transaction, which answers its requests run with __stub, as
 * the stub actions of the Eclipse Studio: from an XML response of the requestable, or empty.
 * <ul>
 * <li>id: the tree id of the sequence or the transaction</li>
 * <li>xml: the XML response to save, an empty stub when missing</li>
 * <li>overwrite: true to replace an existing stub</li>
 * </ul>
 */
@ServiceDefinition(name = "CreateStub", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "", available_without_web_studio = true)
public class CreateStub extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var id = request.getParameter("id");
		if (id == null || id.isBlank()) {
			throw new ServiceException("missing id parameter");
		}
		if (!(Utils.getDbo(id) instanceof RequestableObject requestable)) {
			throw new ServiceException("Only a sequence or a transaction has a stub.");
		}
		var xml = request.getParameter("xml");
		var document = xml == null || xml.isBlank() ? emptyStub(requestable) : XMLUtils.parseDOM("java", xml);
		var file = write(requestable, document, "true".equals(request.getParameter("overwrite")));
		response.put("done", file != null);
		response.put("exists", file == null);
		response.put("file", "stubs/" + requestable.getDefaultStubFileName());
	}

	/**
	 * @return the stub file, or null when it exists and must not be replaced
	 */
	public static File write(RequestableObject requestable, Document document, boolean overwrite) throws Exception {
		var stubDir = new File(requestable.getProject().getDirPath(), "stubs");
		var file = new File(stubDir, requestable.getDefaultStubFileName());
		if (file.exists() && !overwrite) {
			return null;
		}
		stubDir.mkdirs();
		try (var out = new FileOutputStream(file)) {
			XMLUtils.prettyPrintDOMWithEncoding(document, "UTF-8", out);
		}
		return file;
	}

	/**
	 * @return the document of an empty response of the requestable
	 */
	public static Document emptyStub(RequestableObject requestable) throws Exception {
		var document = XMLUtils.createDom("java");
		var root = document.createElement("document");
		var project = requestable.getProject().getName();
		var generated = Calendar.getInstance(Locale.getDefault()).getTime().toString();
		root.setAttribute("connector", requestable instanceof Transaction transaction ? transaction.getParentName() : "");
		root.setAttribute("fromcache", "false");
		root.setAttribute("generated", generated);
		root.setAttribute("project", project);
		root.setAttribute("sequence", requestable instanceof Sequence ? requestable.getName() : "");
		root.setAttribute("transaction", requestable instanceof Transaction ? requestable.getName() : "");
		document.appendChild(root);
		return document;
	}
}
