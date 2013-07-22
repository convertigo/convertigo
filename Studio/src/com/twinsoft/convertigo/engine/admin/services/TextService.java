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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/branches/6.1.3/Studio/src/com/twinsoft/convertigo/engine/admin/services/JSonService.java $
 * $Author: jibrilk $
 * $Revision: 29362 $
 * $Date: 2011-12-22 12:36:08 +0100 (jeu., 22 d√©c. 2011) $
 */

package com.twinsoft.convertigo.engine.admin.services;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;

public abstract class TextService implements Service {

	public void run(String serviceName, HttpServletRequest request, HttpServletResponse response) throws ServiceException {
        try {
        	response.setContentType("text/plain");
        	response.setCharacterEncoding("UTF-8");

            String sResponse = getServiceResult(request);

            response.getWriter().write(sResponse);
            
			Engine.logAdmin.debug("Generated string response:\n" + sResponse);
        } catch (Throwable t) {
			ServiceUtils.handleError(t, response);
		}
	}

	protected abstract String getServiceResult(HttpServletRequest request) throws Exception;
}
