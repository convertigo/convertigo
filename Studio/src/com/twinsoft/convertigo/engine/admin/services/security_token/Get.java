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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/branches/6.1.3/Studio/src/com/twinsoft/convertigo/engine/admin/services/projects/Get.java $
 * $Author: fabienb $
 * $Revision: 30435 $
 * $Date: 2012-05-11 15:21:46 +0200 (ven., 11 mai 2012) $
 */

package com.twinsoft.convertigo.engine.admin.services.security_token;

import javax.servlet.http.HttpServletRequest;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.SecurityToken;
import com.twinsoft.convertigo.engine.admin.services.TextService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceParameterDefinition;

@ServiceDefinition(
		name = "Get",
		roles = { Role.ANONYMOUS },
		parameters = {
				@ServiceParameterDefinition(
						name = "userID",
						description = "the ID of the portal authenticated user"
					),
				@ServiceParameterDefinition(
						name = "passPhrase",
						description = "the pass phrase required to use this service"
					)
			},
		returnValue = "the token ID"
	)
public class Get extends TextService {	

	protected String getServiceResult(HttpServletRequest request) throws Exception {		
		String passPhrase = request.getParameter("passPhrase");
		
		if (!EnginePropertiesManager.getProperty(PropertyName.SECURITY_TOKEN_PASSPHRASE).equals(passPhrase))
			throw new EngineException("Invalid passphrase");
		
		String userID = request.getParameter("userID");
		if ((userID == null) || (userID.length() == 0)) throw new EngineException("Invalid user ID");
		
		SecurityToken token = Engine.theApp.securityTokenManager.generateToken(userID);
		
		return token.tokenID;
	}
	
}