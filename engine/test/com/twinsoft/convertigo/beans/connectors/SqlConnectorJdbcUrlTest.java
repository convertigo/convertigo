/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.beans.connectors;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SqlConnectorJdbcUrlTest {
	private static final String URL = "jdbc:mariadb://localhost/db";

	@Test
	public void overrideWithoutOptionsIsUnchanged() {
		var connector = new SqlConnector();
		connector.setOverJdbcURL(URL);
		assertEquals(URL, connector.getConnectionJdbcURL());
	}

	@Test
	public void overrideCredentialsRemainSeparateFromDriverOptions() {
		var connector = new SqlConnector();
		connector.setOverJdbcURL(URL + "?user=demo&password=example");
		assertEquals(URL, connector.getConnectionJdbcURL());
		assertEquals("demo", connector.getRealJdbcUserName());
		assertEquals("example", connector.getRealJdbcUserPassword());
	}

	@Test
	public void overridePreservesExplicitTlsAndOtherDriverOptions() {
		var connector = new SqlConnector();
		connector.setOverJdbcURL(URL + "?user=demo&SsLmOdE=verify-full&password=example&connectTimeout=5000");
		assertEquals(URL + "?SsLmOdE=verify-full&connectTimeout=5000", connector.getConnectionJdbcURL());
	}

	@Test
	public void overridePreservesLegacyTlsAlias() {
		var connector = new SqlConnector();
		connector.setOverJdbcURL(URL + "?user=demo&useSSL=true");
		assertEquals(URL + "?useSSL=true", connector.getConnectionJdbcURL());
	}

	@Test
	public void blankOverrideUsesTheSavedUrlWithoutRewritingIt() {
		var connector = new SqlConnector();
		connector.setJdbcURL(URL + "?sslMode=verify-full");
		connector.setOverJdbcURL(" ");
		assertEquals(URL + "?sslMode=verify-full", connector.getConnectionJdbcURL());
		assertEquals(URL + "?sslMode=verify-full", connector.getJdbcURL());
	}
}
