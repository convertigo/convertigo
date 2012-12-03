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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/branches/6.1.3/Studio/src/com/twinsoft/convertigo/engine/RsaManager.java $
 * $Author: fabienb $
 * $Revision: 28379 $
 * $Date: 2011-09-27 11:38:59 +0200 (mar., 27 sept. 2011) $
 */

package com.twinsoft.convertigo.engine;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

public class SecurityTokenManager implements AbstractManager {

	private Map<String, SecurityToken> tokens;
	private SecureRandom random;
	
	public void destroy() throws EngineException {
		tokens = null;
	}

	public void init() throws EngineException {
		tokens = new HashMap<String, SecurityToken>();
		random = new SecureRandom();
	}

	public synchronized SecurityToken consumeToken(String tokenID) throws NoSuchSecurityTokenException, ExpiredSecurityTokenException {
		SecurityToken token = tokens.get(tokenID);
		
		if (token == null) throw new NoSuchSecurityTokenException(tokenID);
		
		tokens.remove(tokenID);

		if (token.isExpired()) throw new ExpiredSecurityTokenException(tokenID);
		
		return token;
	}
	
	public SecurityToken generateToken(String userID) {
		String tokenID = new BigInteger(130, random).toString(32);
		
		long now = System.currentTimeMillis();
		long tokenLifeTime = Long.parseLong(EnginePropertiesManager.getProperty(PropertyName.SECURITY_TOKEN_LIFE_TIME));
		SecurityToken token = new SecurityToken(tokenID, userID, now + tokenLifeTime * 1000);
		
		tokens.put(tokenID, token);
		
		return token;
	}
	
}