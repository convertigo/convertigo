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
import java.util.Iterator;
import java.util.Map;

import org.hibernate.StatelessSession;
import org.hibernate.criterion.Restrictions;

import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.SecurityTokenMode;
import com.twinsoft.convertigo.engine.events.PropertyChangeEvent;
import com.twinsoft.convertigo.engine.events.PropertyChangeEventListener;
import com.twinsoft.convertigo.engine.helpers.HibernateHelper;

public class SecurityTokenManager implements AbstractManager, PropertyChangeEventListener {

	private Map<String, SecurityToken> tokens;
	private SecureRandom random;
	private HibernateHelper hibernateHelper;
	private long nextExpireCheck;
	
	public void destroy() throws EngineException {
		tokens = null;
		random = null;
		if (hibernateHelper != null) {
			hibernateHelper.destroy();
			hibernateHelper = null;
		}
		Engine.theApp.eventManager.removeListener(this, PropertyChangeEventListener.class);
	}

	public void init() throws EngineException {
		random = new SecureRandom();
		nextCheck();
		Engine.theApp.eventManager.addListener(this, PropertyChangeEventListener.class);
		configureStorage();
	}

	public synchronized SecurityToken consumeToken(final String tokenID) throws NoSuchSecurityTokenException, ExpiredSecurityTokenException {
		final SecurityToken[] token = {null};
		
		Engine.logSecurityTokenManager.debug("(SecurityTokenManager) Try to consume tokenID: '" + tokenID + "'");
		
		removeExpired();
		
		if (tokens != null) {
			token[0] = tokens.get(tokenID);
			if (Engine.logSecurityTokenManager.isDebugEnabled()) {
				Engine.logSecurityTokenManager.debug("(SecurityTokenManager) Memory tokens manager retrieves: " + token[0]);
			}
		}
		
		if (hibernateHelper != null) {
			hibernateHelper.retry(new Runnable() {
				public void run() {
					StatelessSession session = hibernateHelper.getSession();
					try {
						token[0] = (SecurityToken) session.createCriteria(SecurityToken.class).add(Restrictions.eq("tokenID", tokenID)).uniqueResult();
					} finally {
						session.close();
					}
				}
			});
			
			if (Engine.logSecurityTokenManager.isDebugEnabled()) {
				Engine.logSecurityTokenManager.debug("(SecurityTokenManager) Database tokens manager retrieves: " + token[0]);
			}
		}
		
		if (token[0] == null) {
			Engine.logSecurityTokenManager.debug("(SecurityTokenManager) Not found tokenID: '" + tokenID + "'");
			throw new NoSuchSecurityTokenException(tokenID);
		}

		if (tokens != null) {
			tokens.remove(tokenID);	
		}
		
		if (hibernateHelper != null) {
			hibernateHelper.delete(token[0]);
		}

		if (token[0].isExpired()) {
			Engine.logSecurityTokenManager.debug("(SecurityTokenManager) Expired tokenID: '" + tokenID + "'");
			throw new ExpiredSecurityTokenException(tokenID);
		}
		
		Engine.logSecurityTokenManager.debug("(SecurityTokenManager) The security token is: '" + token[0] + "'");
		return token[0];
	}
	
	public SecurityToken generateToken(String userID, Map<String, String> data) {
		String tokenID = new BigInteger(130, random).toString(32);
		
		Engine.logSecurityTokenManager.debug("(SecurityTokenManager) Generate a new tokenID: '" + tokenID + "' for userID: '" + userID);
		
		removeExpired();
		
		long now = System.currentTimeMillis();
		long tokenLifeTime = Long.parseLong(EnginePropertiesManager.getProperty(PropertyName.SECURITY_TOKEN_LIFE_TIME));
		SecurityToken token = new SecurityToken(tokenID, userID, now + tokenLifeTime * 1000, data);
		
		if (tokens != null) {
			tokens.put(tokenID, token);
		}
		if (hibernateHelper != null) {
			hibernateHelper.insert(token);
		}
		
		return token;
	}
	
	private void configureStorage() {
		SecurityTokenMode mode = EnginePropertiesManager.getPropertyAsEnum(PropertyName.SECURITY_TOKEN_MODE);
		switch (mode) {
		case memory:
			if (tokens == null) {
				tokens = new HashMap<String, SecurityToken>();
			}
			if (hibernateHelper != null) {
				hibernateHelper.destroy();
				hibernateHelper = null;
			}
			break;
		case database:
			if (tokens != null) {
				tokens = null;
			}
			if (hibernateHelper == null) {
				try {
					hibernateHelper = new HibernateHelper(
						Engine.logSecurityTokenManager,
						EnginePropertiesManager.getProperty(PropertyName.SECURITY_TOKEN_PERSISTENCE_DIALECT),
						EnginePropertiesManager.getProperty(PropertyName.SECURITY_TOKEN_PERSISTENCE_JDBC_DRIVER),
						EnginePropertiesManager.getProperty(PropertyName.SECURITY_TOKEN_PERSISTENCE_JDBC_URL),
						EnginePropertiesManager.getProperty(PropertyName.SECURITY_TOKEN_PERSISTENCE_JDBC_USERNAME),
						EnginePropertiesManager.getProperty(PropertyName.SECURITY_TOKEN_PERSISTENCE_JDBC_PASSWORD),
						EnginePropertiesManager.getPropertyAsLong(PropertyName.SECURITY_TOKEN_PERSISTENCE_MAX_RETRY),
						com.twinsoft.convertigo.engine.SecurityToken.class
					);
				} catch (EngineException e) {
					hibernateHelper = null;
					Engine.logSecurityTokenManager.error("(SecurityTokenManager) Failed to create hibernate helper", e);
				}
			}
			break;
		default:
			break;
		}
	}
	
	public void onEvent(PropertyChangeEvent event) {
		PropertyName name = event.getKey();
		switch (name) {
			case SECURITY_TOKEN_PERSISTENCE_DIALECT:
			case SECURITY_TOKEN_PERSISTENCE_JDBC_DRIVER:
			case SECURITY_TOKEN_PERSISTENCE_JDBC_PASSWORD:
			case SECURITY_TOKEN_PERSISTENCE_JDBC_URL:
			case SECURITY_TOKEN_PERSISTENCE_JDBC_USERNAME:
			case SECURITY_TOKEN_PERSISTENCE_MAX_RETRY:
				if (hibernateHelper != null) {
					hibernateHelper.destroy();
					hibernateHelper = null;
				}
			case SECURITY_TOKEN_MODE:
				configureStorage();
		default:
			break;
		}
	}
	
	private void nextCheck() {
		nextExpireCheck = System.currentTimeMillis() + (5 * 60 * 1000);
	}
	
	private void removeExpired() {
		if (System.currentTimeMillis() >= nextExpireCheck) {
			Engine.logSecurityTokenManager.debug("(SecurityTokenManager) Remove all expired tokens …");
			
			if (tokens != null) {
				int removeCpt = 0;
				for (Iterator<SecurityToken> i = tokens.values().iterator(); i.hasNext();) {
					SecurityToken token = i.next();
					if (token.isExpired()) {
						i.remove();
						removeCpt++;
					}
				}
				Engine.logSecurityTokenManager.info("(SecurityTokenManager) Memory tokens manager removes: " + removeCpt + " token(s)");		
			}
			
			if (hibernateHelper != null) {
				int removeCpt = hibernateHelper.update("delete from " + SecurityToken.class.getSimpleName() + " where expiryDate < " + System.currentTimeMillis());
				Engine.logSecurityTokenManager.info("(SecurityTokenManager) Database tokens manager removes: " + removeCpt + " token(s)");
			}
			
			nextCheck();
		}
	}
}