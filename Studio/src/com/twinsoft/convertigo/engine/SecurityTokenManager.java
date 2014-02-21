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

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.SecurityTokenMode;
import com.twinsoft.convertigo.engine.events.PropertyChangeEvent;
import com.twinsoft.convertigo.engine.events.PropertyChangeEventListener;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class SecurityTokenManager implements AbstractManager, PropertyChangeEventListener {

	private Map<String, SecurityToken> tokens;
	private SecureRandom random;
	private SessionFactory sessionFactory;
	private long nextExpireCheck;
	
	public void destroy() throws EngineException {
		tokens = null;
		random = null;
		if (sessionFactory != null) {
			sessionFactory.close();
			sessionFactory = null;
		}
		Engine.theApp.eventManager.removeListener(this, PropertyChangeEventListener.class);
	}

	public void init() throws EngineException {
		random = new SecureRandom();
		nextCheck();
		Engine.theApp.eventManager.addListener(this, PropertyChangeEventListener.class);
		configureStorage();
	}

	public synchronized SecurityToken consumeToken(String tokenID) throws NoSuchSecurityTokenException, ExpiredSecurityTokenException {
		SecurityToken token = null;
		
		Engine.logEngine.trace("(SecurityTokenManager) Try to consume tokenID: " + tokenID);
		
		removeExpired();
		
		if (tokens != null) {
			token = tokens.get(tokenID);
			if (Engine.logEngine.isTraceEnabled()) {
				Engine.logEngine.trace("(SecurityTokenManager) Memory tokens manager retrieves: " + token);
			}
		}
		if (sessionFactory != null) {
			StatelessSession session = sessionFactory.openStatelessSession();
			try {
				token = (SecurityToken) session.createCriteria(SecurityToken.class).add(Restrictions.eq("tokenID", tokenID)).uniqueResult();
			} finally {
				session.close();
			}
			if (Engine.logEngine.isTraceEnabled()) {
				Engine.logEngine.trace("(SecurityTokenManager) Database tokens manager retrieves: " + token);
			}
		}
		
		if (token == null) throw new NoSuchSecurityTokenException(tokenID);

		if (tokens != null) {
			tokens.remove(tokenID);	
		}
		if (sessionFactory != null) {
			StatelessSession session = sessionFactory.openStatelessSession();
			try {
				session.delete(token);
			} finally {
				session.close();
			}
		}

		if (token.isExpired()) throw new ExpiredSecurityTokenException(tokenID);
		
		return token;
	}
	
	public SecurityToken generateToken(String userID, Map<String, String> data) {
		String tokenID = new BigInteger(130, random).toString(32);
		
		Engine.logEngine.trace("(SecurityTokenManager) Generate a new tokenID: " + tokenID);
		
		removeExpired();
		
		long now = System.currentTimeMillis();
		long tokenLifeTime = Long.parseLong(EnginePropertiesManager.getProperty(PropertyName.SECURITY_TOKEN_LIFE_TIME));
		SecurityToken token = new SecurityToken(tokenID, userID, now + tokenLifeTime * 1000, data);
		
		if (tokens != null) {
			tokens.put(tokenID, token);
		}
		if (sessionFactory != null) {
			StatelessSession session = sessionFactory.openStatelessSession();
			try {
				session.insert(token);
			} finally {
				session.close();
			}
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
			if (sessionFactory != null) {
				sessionFactory.close();
				sessionFactory = null;
			}
			break;
		case database:
			if (tokens != null) {
				tokens = null;
			}
			if (sessionFactory == null) {
				Document doc = XMLUtils.getDefaultDocumentBuilder().newDocument();
				Element elt = doc.createElement("session-factory");
				addProperty(elt, "hibernate.connection.driver_class", EnginePropertiesManager.getProperty(PropertyName.SECURITY_TOKEN_PERSISTENCE_JDBC_DRIVER));
				addProperty(elt, "hibernate.connection.url", EnginePropertiesManager.getProperty(PropertyName.SECURITY_TOKEN_PERSISTENCE_JDBC_URL));
				addProperty(elt, "hibernate.connection.username", EnginePropertiesManager.getProperty(PropertyName.SECURITY_TOKEN_PERSISTENCE_JDBC_USERNAME));
				addProperty(elt, "hibernate.connection.password", EnginePropertiesManager.getProperty(PropertyName.SECURITY_TOKEN_PERSISTENCE_JDBC_PASSWORD));
				addProperty(elt, "hibernate.dialect", EnginePropertiesManager.getProperty(PropertyName.SECURITY_TOKEN_PERSISTENCE_DIALECT));
				addProperty(elt, "hibernate.hbm2ddl.auto", "update");
				addProperty(elt, "hibernate.connection.autocommit", "true");
				addProperty(elt, "hibernate.jdbc.batch_size", "1");
				doc.appendChild(doc.createElement("hibernate-configuration")).appendChild(elt);

				Configuration cfg = new Configuration();
				cfg.addAnnotatedClass(com.twinsoft.convertigo.engine.SecurityToken.class);
				cfg.configure(doc);
				
				try {
					sessionFactory = cfg.buildSessionFactory();
				} catch (HibernateException e) {
					Engine.logEngine.error("(SecurityTokenManager) Hibernate session factory creation failed", e);
				}
			}
			break;
		default:
			break;
		}
	}
	
	private void addProperty(Element element, String name, String value) {
		Element property = element.getOwnerDocument().createElement("property");
		property.setAttribute("name", name);
		property.setTextContent(value);
		element.appendChild(property);
	}
	
	public void onEvent(PropertyChangeEvent event) {
		PropertyName name = event.getKey();
		switch (name) {
			case SECURITY_TOKEN_PERSISTENCE_DIALECT:
			case SECURITY_TOKEN_PERSISTENCE_JDBC_DRIVER:
			case SECURITY_TOKEN_PERSISTENCE_JDBC_PASSWORD:
			case SECURITY_TOKEN_PERSISTENCE_JDBC_URL:
			case SECURITY_TOKEN_PERSISTENCE_JDBC_USERNAME:
				if (sessionFactory != null) {
					sessionFactory.close();
					sessionFactory = null;
				}
			case SECURITY_TOKEN_MODE:
				configureStorage();
		default:
			break;
		}
	}
	
	private void nextCheck() {
		nextExpireCheck = System.currentTimeMillis() + (3600 * 1000);
	}
	
	private void removeExpired() {
		if (System.currentTimeMillis() >= nextExpireCheck) {
			Engine.logEngine.debug("(SecurityTokenManager) Remove all expired tokens …");
			
			if (tokens != null) {
				int removeCpt = 0;
				for (Iterator<SecurityToken> i = tokens.values().iterator(); i.hasNext();) {
					SecurityToken token = i.next();
					if (token.isExpired()) {
						i.remove();
						removeCpt++;
					}
				}
				Engine.logEngine.info("(SecurityTokenManager) Memory tokens manager removes: " + removeCpt + " token(s)");		
			}
			
			if (sessionFactory != null) {
				int removeCpt = 0;
				StatelessSession session = sessionFactory.openStatelessSession();
				try {
					 removeCpt = session.createQuery("delete from " + SecurityToken.class.getSimpleName() + " where expiryDate < " + System.currentTimeMillis()).executeUpdate();
				} finally {
					session.close();
				}
				Engine.logEngine.info("(SecurityTokenManager) Database tokens manager removes: " + removeCpt + " token(s)");
			}
			
			nextCheck();
		}
	}
}