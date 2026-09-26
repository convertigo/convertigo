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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.connectors.SqlData;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.ConnectorEvent;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

/**
 * The data a connector got last, as the connector editors of the Eclipse Studio show it: the text of an HTTP
 * or CouchDB response, the rows of an SQL query. The Studio watches a connector before it runs one of its
 * transactions, then reads its data.
 * <ul>
 * <li>project, connector: the connector</li>
 * <li>action: watch to forget the last data and keep the next one, none to read it</li>
 * </ul>
 */
@ServiceDefinition(name = "ConnectorData", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
public class ConnectorData extends JSonService {
	/** the text an HTTP response keeps, as the Eclipse Studio cuts it */
	private static final int MAX_TEXT = 100000;
	/** the rows an SQL query keeps */
	private static final int MAX_ROWS = 1000;
	/** the last data of the watched connectors, by their qname */
	private static final Map<String, Object> DATA = new ConcurrentHashMap<>();
	private static final Object NONE = new Object();

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(request.getParameter("project"));
		var connector = project == null ? null : project.getConnectorByName(request.getParameter("connector"));
		if (connector == null) {
			throw new ServiceException("The connector " + request.getParameter("connector") + " does not exist.");
		}
		var qname = connector.getQName();
		if ("watch".equals(request.getParameter("action"))) {
			synchronized (DATA) {
				if (Connector.dataWatcher == null) {
					Connector.dataWatcher = ConnectorData::dataChanged;
				}
			}
			DATA.put(qname, NONE);
			response.put("done", true);
			return;
		}
		var data = DATA.get(qname);
		if (data instanceof SqlData sql) {
			var rows = new JSONArray();
			var list = sql.data == null ? List.<List<String>>of() : sql.data;
			for (var row : list.subList(0, Math.min(list.size(), MAX_ROWS))) {
				rows.put(new JSONArray(row));
			}
			response.put("kind", "table");
			response.put("headers", new JSONArray(sql.columnHeaders == null ? List.of() : sql.columnHeaders));
			response.put("rows", rows);
			response.put("total", list.size());
		} else if (data != null && data != NONE) {
			var text = String.valueOf(data);
			response.put("kind", "text");
			response.put("text", text.length() > MAX_TEXT ? text.substring(0, MAX_TEXT) + "..." : text);
		} else {
			response.put("kind", "none");
		}
	}

	private static void dataChanged(ConnectorEvent event) {
		if (event.getSource() instanceof Connector connector) {
			var qname = connector.getQName();
			// only the connectors the Studio watches keep their data
			if (DATA.containsKey(qname)) {
				DATA.put(qname, event.data == null ? NONE : event.data);
			}
		}
	}
}
