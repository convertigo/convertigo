/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.services.database_objects;

import java.beans.BeanInfo;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.DownloadService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;
import com.twinsoft.convertigo.engine.util.GenericUtils;

@ServiceDefinition(
	name = "GetIcon",
	roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG, Role.PROJECT_DBO_VIEW },
	parameters = {},
	returnValue = ""
)
public class GetIcon extends DownloadService {

	@Override
	protected void writeResponseResult(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServiceException {

		String className = request.getParameter("className");
		String large = request.getParameter("large");

		if (className == null || !className.startsWith("com.twinsoft.convertigo.beans"))
			throw new ServiceException("Must provide className parameter", null);

		try {
			BeanInfo bi = CachedIntrospector.getBeanInfo(GenericUtils.<Class<? extends DatabaseObject>>cast(Class.forName(className)));
			int iconType = large != null && large.equals("true") ? BeanInfo.ICON_COLOR_32x32 : BeanInfo.ICON_COLOR_16x16;
			IOUtils.copy(bi.getBeanDescriptor().getBeanClass().getResourceAsStream(MySimpleBeanInfo.getIconName(bi, iconType)), response.getOutputStream());
		} catch (Exception e) {
			throw new ServiceException("Icon unreachable", e);
		}

		Engine.logAdmin.info("The image has been exported");
	}

}
