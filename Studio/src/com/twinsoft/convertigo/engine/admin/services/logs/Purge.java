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
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.engine.admin.services.logs;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.logmanager.LogManager;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

@ServiceDefinition(
		name = "Purge",
		roles = { Role.WEB_ADMIN, Role.LOGS_CONFIG },
		parameters = {},
		returnValue = ""
	)
public class Purge extends XmlService {
	enum Action {
		list_files,
		delete_files;
		
		static String parameterName() {
			return Action.class.getSimpleName().toLowerCase();
		}
	}

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		Action action;
		try {
			action = Action.valueOf(request.getParameter(Action.parameterName()));
		} catch (Exception e) {
			throw new InvalidParameterException("Need '" + Action.parameterName() + "' parameter in " + Arrays.toString(Action.values()));
		}
		
		Element e_dates = document.createElement("dates");
		
		LogManager lm = new LogManager();
		Map<Date, File> files = lm.getTimedFiles();
		lm.close();
		
		switch (action) {
		case list_files :
			for (Date date : files.keySet()) {
				addDate(e_dates, date);
			}
			break;
		case delete_files :
			try {
				Date end_date = LogManager.date_format.parse(request.getParameter("date"));
				for (Entry<Date, File> entry : files.entrySet()) {
					if (entry.getKey().compareTo(end_date) <= 0) {
						if (entry.getValue().delete()) {
							addDate(e_dates, entry.getKey());
						}
					}
				}
			} catch (Exception e) {
				throw new InvalidParameterException("Need parsable 'date' parameter");
			}
			break;
		}
		
		document.getDocumentElement().appendChild(document.createElement(Action.parameterName())).setTextContent(action.name());
		document.getDocumentElement().appendChild(e_dates);
	}
	
	private void addDate(Element e_dates, Date date) {
		Element e_date = e_dates.getOwnerDocument().createElement("date");
		e_date.setTextContent(LogManager.date_format.format(date)); 
		e_dates.appendChild(e_date);
	}
}