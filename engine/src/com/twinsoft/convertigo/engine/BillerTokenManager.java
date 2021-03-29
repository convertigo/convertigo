/*
 * Copyright (c) 2001-2021 Convertigo SA.
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

package com.twinsoft.convertigo.engine;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

public class BillerTokenManager implements AbstractManager {

	private Map<String, BillerToken> tokens;
	private SecureRandom random;
	
	@Override
	public void init() throws EngineException {
		random = new SecureRandom();
		tokens = new HashMap<String, BillerToken>();
	}

	@Override
	public void destroy() throws EngineException {
		if (tokens != null) {
			tokens.clear();
		}
		tokens = null;
		random = null;
	}

	protected BillerToken generateToken(String project, String user, Map<String, String> data) {
		String tokenID = new BigInteger(130, random).toString(32);
		BillerToken token = new BillerToken(tokenID, project, user, data == null ? new HashMap<String, String>():data);
		return token;
	}
	
	public BillerToken addToken(String project, String user, Map<String, String> data) {
		BillerToken token = generateToken(project, user, data);
		synchronized (tokens) {
			tokens.put(token.getTokenID(), token);
		}
		return token;
	}
	
	public BillerToken getToken(String tokenID) {
		BillerToken token = null;
		synchronized (tokens) {
			token = tokens.get(tokenID);
		}
		return token;
	}
	
	public void removeToken(String tokenID) {
		synchronized (tokens) {
			tokens.remove(tokenID);
		}
	}
}
