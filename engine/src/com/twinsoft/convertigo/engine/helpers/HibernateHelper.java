/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

package com.twinsoft.convertigo.engine.helpers;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.service.ServiceRegistry;

import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.ThreadUtils;

public class HibernateHelper {
	private SessionFactory sessionFactory;
	private Configuration configuration;
	
	private Logger log;
	private long retry;
	
	public HibernateHelper(Logger log, String dialect, String driver_class, String url, String username, String password, long retry, Class<?>... annotatedClasses) throws EngineException {
		this.log = log;
		this.retry = retry + 1;
		
		configuration = new Configuration();
		configuration.setProperty("hibernate.connection.driver_class", driver_class);
		configuration.setProperty("hibernate.connection.url", url);
		configuration.setProperty("hibernate.connection.username", username);
		configuration.setProperty("hibernate.connection.password", password);
		configuration.setProperty("hibernate.dialect", dialect);
		configuration.setProperty("hibernate.hbm2ddl.auto", "update");
		configuration.setProperty("hibernate.connection.autocommit", "true");
		configuration.setProperty("hibernate.jdbc.batch_size", "1");
		configuration.setProperty("hibernate.show_sql", "false");
		for (Class<?> annotatedClass: annotatedClasses) {
			configuration.addAnnotatedClass(annotatedClass);
		}
	}
	
	public void insert(final Object obj) {
		retry(() -> {
			try (StatelessSession session = getSession()) {
				session.insert(obj);
			}
		});
	}
	
	public void delete(final Object obj) {
		retry(() -> {
			try (StatelessSession session = getSession()) {
				session.delete(obj);
			}
		});
	}
	
	public int update(final String query) {
		final int[] updated = {0};
		
		retry(() -> {
			try (StatelessSession session = getSession()) {
				updated[0] = session.createQuery(query).executeUpdate();
			}
		});
		return updated[0];
	}
	
	public void retry(Runnable run) {
		for (long retry = this.retry; retry > 0; retry--) {
			try {
				run.run();
				retry = -1;
			} catch (JDBCConnectionException e) {
				clearSessionFactory();
				if (retry > 0) {							
					log.debug("(SecurityTokenManager) JDBCConnectionException for removeExpired, retry left: " + retry);
					ThreadUtils.sleep(200);
				} else {
					log.warn("(SecurityTokenManager) JDBCConnectionException for removeExpired, no more retry", e);
				}					
			}
		}
	}
	
	public synchronized void destroy() {
		clearSessionFactory();
		configuration = null;
	}
	
	private synchronized void clearSessionFactory() {
		if (sessionFactory != null) {
			sessionFactory.close();
			sessionFactory = null;
		}
	}
	
	public synchronized StatelessSession getSession() {
		if (sessionFactory == null) {
			try {
				ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties()).build();
				sessionFactory = configuration.buildSessionFactory(serviceRegistry);
			} catch (HibernateException e) {
				log.warn("(HibernateHelper) Hibernate session factory creation failed", e);
				throw new RuntimeException("(HibernateHelper) Hibernate session factory creation failed", e);
			}
		}
		return sessionFactory.openStatelessSession();
	}
}
