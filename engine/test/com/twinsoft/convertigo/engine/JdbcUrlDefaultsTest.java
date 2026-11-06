/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class JdbcUrlDefaultsTest {
	private static final String MARIA = "org.mariadb.jdbc.Driver";
	private static final String POSTGRES = "org.postgresql.Driver";
	private static final String URL = "jdbc:mariadb://localhost/db";

	@Parameters(name = "{index}: {0}")
	public static Object[][] urls() {
		return new Object[][] {
			{"no query", MARIA, URL, URL + "?sslMode=disable"},
			{"existing query", MARIA, URL + "?connectTimeout=5000", URL + "?connectTimeout=5000&sslMode=disable"},
			{"several parameters", MARIA, URL + "?user=demo&connectTimeout=5000", URL + "?user=demo&connectTimeout=5000&sslMode=disable"},
			{"empty query", MARIA, URL + "?", URL + "?sslMode=disable"},
			{"trailing separator", MARIA, URL + "?connectTimeout=5000&", URL + "?connectTimeout=5000&sslMode=disable"},
			{"explicit TLS", MARIA, URL + "?sslMode=verify-full", URL + "?sslMode=verify-full"},
			{"explicit plaintext", MARIA, URL + "?sslMode=disable", URL + "?sslMode=disable"},
			{"mixed case key and value", MARIA, URL + "?SsLmOdE=VeRiFy-FuLl", URL + "?SsLmOdE=VeRiFy-FuLl"},
			{"explicit mode after another parameter", MARIA, URL + "?user=demo&SSLMODE=trust&connectTimeout=5000", URL + "?user=demo&SSLMODE=trust&connectTimeout=5000"},
			{"empty explicit mode", MARIA, URL + "?sslMode=", URL + "?sslMode="},
			{"explicit key without value", MARIA, URL + "?sslMode", URL + "?sslMode"},
			{"unknown explicit mode", MARIA, URL + "?sslMode=not-a-mode", URL + "?sslMode=not-a-mode"},
			{"legacy useSsl", MARIA, URL + "?useSsl=true", URL + "?useSsl=true"},
			{"legacy useSSL", MARIA, URL + "?useSSL=false", URL + "?useSSL=false"},
			{"mixed case legacy alias", MARIA, URL + "?UsEsSl=true", URL + "?UsEsSl=true"},
			{"legacy certificate choice", MARIA, URL + "?trustServerCertificate=true", URL + "?trustServerCertificate=true"},
			{"legacy hostname choice", MARIA, URL + "?disableSslHostnameVerification=false", URL + "?disableSslHostnameVerification=false"},
			{"explicit server certificate", MARIA, URL + "?serverSslCert=ca.pem", URL + "?serverSslCert=ca.pem"},
			{"explicit trust store", MARIA, URL + "?trustStore=trust.p12", URL + "?trustStore=trust.p12"},
			{"explicit key store", MARIA, URL + "?keyStore=client.p12", URL + "?keyStore=client.p12"},
			{"mode text in value is not a key", MARIA, URL + "?sessionVariables=sslMode=verify-full", URL + "?sessionVariables=sslMode=verify-full&sslMode=disable"},
			{"similarly named key", MARIA, URL + "?notsslMode=verify-full", URL + "?notsslMode=verify-full&sslMode=disable"},
			{"multi-host", MARIA, "jdbc:mariadb:sequential://db1:3306,db2:3307/db", "jdbc:mariadb:sequential://db1:3306,db2:3307/db?sslMode=disable"},
			{"multi-host explicit TLS", MARIA, "jdbc:mariadb:loadbalance://db1,db2/db?sslMode=verify-full", "jdbc:mariadb:loadbalance://db1,db2/db?sslMode=verify-full"},
			{"per-host explicit TLS", MARIA, "jdbc:mariadb://address=(host=db1)(sslMode=verify-full),address=(host=db2)/db", "jdbc:mariadb://address=(host=db1)(sslMode=verify-full),address=(host=db2)/db"},
			{"mixed case per-host TLS", MARIA, "jdbc:mariadb://address=(host=db1)(SSLMODE=trust)/db", "jdbc:mariadb://address=(host=db1)(SSLMODE=trust)/db"},
			{"MySQL URL through MariaDB", MARIA, "jdbc:mysql://localhost/db", URL + "?sslMode=disable"},
			{"MySQL URL and parameters", MARIA, "jdbc:mysql://localhost/db?user=demo", URL + "?user=demo&sslMode=disable"},
			{"MySQL URL with explicit TLS", MARIA, "jdbc:mysql://localhost/db?sslMode=verify-full", URL + "?sslMode=verify-full"},
			{"MySQL URL with legacy alias", MARIA, "jdbc:mysql://localhost/db?useSSL=true", URL + "?useSSL=true"},
			{"real MySQL driver unchanged", "com.mysql.cj.jdbc.Driver", "jdbc:mysql://localhost/db", "jdbc:mysql://localhost/db"},
			{"non-MariaDB URL unchanged", MARIA, "jdbc:other://localhost/db", "jdbc:other://localhost/db"},
			{"other driver unchanged", "org.hsqldb.jdbcDriver", "jdbc:hsqldb:file:db;shutdown=true", "jdbc:hsqldb:file:db;shutdown=true"},
			{"PostgreSQL existing default", POSTGRES, "jdbc:postgresql://localhost/db", "jdbc:postgresql://localhost/db?stringtype=unspecified"},
			{"PostgreSQL existing query", POSTGRES, "jdbc:postgresql://localhost/db?user=demo", "jdbc:postgresql://localhost/db?user=demo&stringtype=unspecified"},
			{"PostgreSQL explicit choice", POSTGRES, "jdbc:postgresql://localhost/db?stringtype=varchar", "jdbc:postgresql://localhost/db?stringtype=varchar"},
		};
	}

	private final String driver;
	private final String input;
	private final String expected;

	public JdbcUrlDefaultsTest(String name, String driver, String input, String expected) {
		this.driver = driver;
		this.input = input;
		this.expected = expected;
	}

	@Test
	public void appliesOnlyMissingDefaults() {
		assertEquals(expected, JdbcConnectionManager.prepareJdbcUrl(input, driver));
	}

	@Test
	public void repeatedPreparationDoesNotChangeTheUrlAgain() {
		String prepared = JdbcConnectionManager.prepareJdbcUrl(input, driver);
		assertEquals(prepared, JdbcConnectionManager.prepareJdbcUrl(prepared, driver));
	}
}
