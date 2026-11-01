/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.billing.HibernateTicketManager;
import com.twinsoft.convertigo.engine.billing.Ticket;

/**
 * The JDBC ticket manager, on an in-memory database: the tickets handed together are written in one transaction, all
 * or none of them, and the billing then writes them one by one, so that only a ticket the database refuses is lost.
 */
public class BillingPersistenceTest {
	private static final String URL = "jdbc:hsqldb:mem:billingPersistenceTest";
	private Connection connection;
	private BillingManager billing;

	@BeforeClass
	public static void setUpEngine() throws Exception {
		BillingBench.setUpEngine();
	}

	@AfterClass
	public static void tearDownEngine() throws Exception {
		BillingBench.tearDownEngine();
	}

	@Before
	public void setUp() throws Exception {
		EnginePropertiesManager.setProperty(PropertyName.ANALYTICS_PERSISTENCE_DIALECT, "org.hibernate.dialect.HSQLDialect");
		EnginePropertiesManager.setProperty(PropertyName.ANALYTICS_PERSISTENCE_JDBC_DRIVER, "org.hsqldb.jdbc.JDBCDriver");
		EnginePropertiesManager.setProperty(PropertyName.ANALYTICS_PERSISTENCE_JDBC_URL, URL);
		EnginePropertiesManager.setProperty(PropertyName.ANALYTICS_PERSISTENCE_JDBC_USERNAME, "SA");
		EnginePropertiesManager.setProperty(PropertyName.ANALYTICS_PERSISTENCE_JDBC_PASSWORD, "");
		Class.forName("org.hsqldb.jdbc.JDBCDriver");
		connection = DriverManager.getConnection(URL, "SA", "");
	}

	@After
	public void tearDown() throws Exception {
		if (billing != null) {
			billing.destroy();
			billing = null;
		}
		try (var statement = connection.createStatement()) {
			statement.execute("DROP SCHEMA PUBLIC CASCADE");
		}
		connection.close();
		EnginePropertiesManager.setProperty(PropertyName.ANALYTICS_PERSISTENCE_ENABLED, "false");
	}

	private long rows() throws Exception {
		try (var statement = connection.createStatement();
				var rs = statement.executeQuery("SELECT COUNT(*) FROM Ticket")) {
			rs.next();
			return rs.getLong(1);
		} catch (java.sql.SQLException e) {
			return 0; // not created yet
		}
	}

	private void waitForRows(long count) throws Exception {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
		while (rows() < count && System.nanoTime() < deadline) {
			Thread.sleep(20);
		}
	}

	private static Ticket ticket(String requestable) {
		var ticket = new Ticket();
		ticket.setCreationDate(System.currentTimeMillis());
		ticket.setProjectName("Persistence");
		ticket.setRequestableName(requestable);
		ticket.setSessionID("SESSION");
		return ticket;
	}

	@Test
	public void theTicketsHandedTogetherAreWrittenInOneTransactionAllOrNone() throws Exception {
		var manager = new HibernateTicketManager(Engine.logBillers);
		try {
			var tickets = new ArrayList<Ticket>();
			for (int i = 0; i < 300; i++) {
				tickets.add(ticket("Together" + i));
			}
			assertTrue(manager.addTickets(tickets));
			assertEquals(300, rows());
			var ids = new HashSet<Long>();
			for (var ticket : tickets) {
				ids.add(ticket.getId());
			}
			assertEquals("an identifier each", 300, ids.size());

			var refused = new ArrayList<Ticket>(List.of(ticket("Before"), ticket("x".repeat(300)), ticket("After")));
			assertThrows(RuntimeException.class, () -> manager.addTickets(refused));
			assertEquals("none of them", 300, rows());
			for (var ticket : refused) {
				assertEquals("identifier given back", 0, ticket.getId());
			}
		} finally {
			manager.destroy();
		}
	}

	@Test
	public void onlyTheTicketTheDatabaseRefusesIsLost() throws Exception {
		EnginePropertiesManager.setProperty(PropertyName.ANALYTICS_PERSISTENCE_ENABLED, "true");
		billing = new BillingManager();
		billing.init();
		var good = BillingBench.context(0, BillingBench.sequence("Good"));
		var refused = BillingBench.context(1, BillingBench.sequence("x".repeat(300)));
		for (int i = 0; i < 2000; i++) {
			billing.insertBilling(i == 1000 ? refused : good);
		}
		billing.destroy();
		billing = null;
		waitForRows(1999);
		assertEquals(1999, rows());
		try (var statement = connection.createStatement();
				var rs = statement.executeQuery("SELECT COUNT(DISTINCT id) FROM Ticket WHERE requestableName = 'Good'")) {
			rs.next();
			assertEquals(1999, rs.getLong(1));
		}
	}
}
