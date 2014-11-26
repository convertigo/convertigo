package com.twinsoft.convertigo.engine.helpers;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.cfg.Configuration;
import org.hibernate.exception.JDBCConnectionException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.ThreadUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class HibernateHelper {
	private SessionFactory sessionFactory;
	private Configuration configuration;
	
	private Logger log;
	private long retry;
	
	public HibernateHelper(Logger log, String dialect, String driver_class, String url, String username, String password, long retry, Class<?>... annotatedClasses) throws EngineException {
		this.log = log;
		this.retry = retry + 1;
		
		Document doc = XMLUtils.getDefaultDocumentBuilder().newDocument();
		Element elt = doc.createElement("session-factory");
		addProperty(elt, "hibernate.connection.driver_class", driver_class);
		addProperty(elt, "hibernate.connection.url", url);
		addProperty(elt, "hibernate.connection.username", username);
		addProperty(elt, "hibernate.connection.password", password);
		addProperty(elt, "hibernate.dialect", dialect);
		addProperty(elt, "hibernate.hbm2ddl.auto", "update");
		addProperty(elt, "hibernate.connection.autocommit", "true");
		addProperty(elt, "hibernate.jdbc.batch_size", "1");
		addProperty(elt, "hibernate.show_sql", "true");
		doc.appendChild(doc.createElement("hibernate-configuration")).appendChild(elt);
		
		configuration = new Configuration();
		for (Class<?> annotatedClass: annotatedClasses) {
			configuration.addAnnotatedClass(annotatedClass);
		}
		configuration.configure(doc);
	}
	
	private void addProperty(Element element, String name, String value) {
		Element property = element.getOwnerDocument().createElement("property");
		property.setAttribute("name", name);
		property.setTextContent(value);
		element.appendChild(property);
	}
	
	public void insert(final Object obj) {
		retry(new Runnable() {
			public void run() {
				StatelessSession session = getSession();
				try {
					session.insert(obj);
				} finally {
					session.close();
				}
			}
		});
	}
	
	public void delete(final Object obj) {
		retry(new Runnable() {
			public void run() {
				StatelessSession session = getSession();
				try {
					session.delete(obj);
				} finally {
					session.close();
				}
			}
		});
	}
	
	public int update(final String query) {
		final int[] updated = {0};
		
		retry(new Runnable() {
			public void run() {
				StatelessSession session = getSession();
				try {
					updated[0] = session.createQuery(query).executeUpdate();
				} finally {
					session.close();
				}
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
				sessionFactory = configuration.buildSessionFactory();
			} catch (HibernateException e) {
				log.warn("(HibernateHelper) Hibernate session factory creation failed", e);
				throw new RuntimeException("(HibernateHelper) Hibernate session factory creation failed", e);
			}
		}
		return sessionFactory.openStatelessSession();
	}
}
