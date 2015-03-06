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
 * $URL: svn://devus.twinsoft.fr/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/engine/admin/services/scheduler/CreateScheduledElements.java $
 * $Author: julienda $
 * $Revision: 39299 $
 * $Date: 2015-03-05 16:00:44 +0100 (jeu., 05 mars 2015) $
 */

package com.twinsoft.convertigo.engine.admin.services.scheduler;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.quartz.CronExpression;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

@ServiceDefinition(name = "CronCalculator", roles = { Role.WEB_ADMIN }, parameters = {}, returnValue = "")
public class CronCalculator extends XmlService {
	
	@Override
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		if (request != null) {
			Element rootElement = document.getDocumentElement();
			Element cronsElement = document.createElement("crons");
			
			int iteration = Integer.parseInt(request.getParameter("iteration"));
					
			String cronExpression = request.getParameter("input");
			cronExpression = cronExpression.substring(cronExpression.indexOf("[") + 1, cronExpression.indexOf("]"));
			long start = new Date().getTime();
			boolean bContinue = true;
			while (iteration-- > 0 && bContinue) {
				Date nextTime;
				String nDate;
				
				try {
					CronExpression exp = new CronExpression(cronExpression);
					nextTime = exp.getNextValidTimeAfter(new Date(start));
					start = nextTime.getTime() + 1;
					nDate = (new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(start))) + "";

					Element cronElement = document.createElement("nextTime");
					cronElement.setTextContent(nDate);

					cronsElement.appendChild(cronElement);
				} catch (Exception e) {
					Element errorElement = document.createElement("error");
					errorElement.setTextContent(e.getMessage());

					cronsElement.appendChild(errorElement);

					nDate = "";
					bContinue = false;
				}
			}
			
			rootElement.appendChild(cronsElement);
		}
	}
}