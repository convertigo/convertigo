/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details: <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.sqlcrud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.Types;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/** Metadata and type mapping of the SQL CRUD user interface generator, on an in-memory HSQLDB database. */
public class SqlTableInfoTest {
	static final String HSQLDB = "org.hsqldb.jdbc.JDBCDriver";
	private static Connection connection;

	@BeforeClass
	public static void createTables() throws Exception {
		connection = MatrixTables.open("sqltableinfo");
	}

	@AfterClass
	public static void close() throws Exception {
		if (connection != null) {
			connection.close();
		}
	}

	private static SqlTableInfo read(String table) throws Exception {
		return SqlTableInfo.read(connection, HSQLDB, table);
	}

	@Test
	public void columnFamilies() throws Exception {
		var c = read("MATRIX").getColumnsByName();
		assertEquals(SqlFieldKind.INTEGER, c.get("ID").getKind());
		assertEquals(SqlFieldKind.TEXT, c.get("FirstName").getKind());
		assertEquals(SqlFieldKind.LONG_TEXT, c.get("NOTES").getKind());
		assertEquals(SqlFieldKind.LONG_TEXT, c.get("LONG_LABEL").getKind());
		assertEquals(SqlFieldKind.BIGINT, c.get("BIG").getKind());
		assertEquals(SqlFieldKind.DECIMAL, c.get("PRICE").getKind());
		assertEquals(SqlFieldKind.FLOAT, c.get("RATIO").getKind());
		assertEquals(SqlFieldKind.BOOLEAN, c.get("ACTIVE").getKind());
		assertEquals(SqlFieldKind.DATE, c.get("BORN").getKind());
		assertEquals(SqlFieldKind.TIMESTAMP, c.get("CREATED_AT").getKind());
		assertEquals(SqlFieldKind.TIME, c.get("START_TIME").getKind());
		assertEquals(SqlFieldKind.BINARY, c.get("PHOTO").getKind());
		assertEquals(SqlFieldKind.TEXT, c.get("CODE").getKind());
		assertEquals(SqlFieldKind.OTHER, c.get("UID").getKind());
		assertEquals("0.01", c.get("PRICE").getStep());
		assertEquals("1", c.get("ID").getStep());
		assertEquals("any", c.get("RATIO").getStep());
		assertEquals(50, c.get("FirstName").getMaxLength());
		assertEquals(0, c.get("NOTES").getMaxLength());
	}

	@Test
	public void jdbcTypesWithoutDatabase() {
		assertEquals(SqlFieldKind.BOOLEAN, SqlFieldKind.of(Types.BIT, "bit", 1));
		assertEquals(SqlFieldKind.BOOLEAN, SqlFieldKind.of(Types.BIT, "TINYINT", 3));
		assertEquals(SqlFieldKind.OTHER, SqlFieldKind.of(Types.BIT, "bit", 8));
		assertEquals(SqlFieldKind.LONG_TEXT, SqlFieldKind.of(Types.VARCHAR, "text", Integer.MAX_VALUE));
		assertEquals(SqlFieldKind.LONG_TEXT, SqlFieldKind.of(Types.NVARCHAR, "nvarchar", 1073741823));
		assertEquals(SqlFieldKind.TEXT, SqlFieldKind.of(Types.CHAR, "uniqueidentifier", 36));
		assertEquals(SqlFieldKind.OTHER, SqlFieldKind.of(Types.OTHER, "uuid", 2147483647));
		assertEquals(SqlFieldKind.OTHER, SqlFieldKind.of(Types.OTHER, "jsonb", 2147483647));
		assertEquals(SqlFieldKind.BINARY, SqlFieldKind.of(Types.BINARY, "bytea", 2147483647));
		assertEquals(SqlFieldKind.TIMESTAMP, SqlFieldKind.of(Types.TIMESTAMP_WITH_TIMEZONE, "timestamptz", 35));
	}

	@Test
	public void keysDefaultsAndRequired() throws Exception {
		var table = read("MATRIX");
		var c = table.getColumnsByName();
		assertEquals(List.of("ID"), table.getPrimaryKeys().stream().map(SqlColumnInfo::getName).toList());
		assertTrue(c.get("ID").isPrimaryKey());
		assertTrue(c.get("ID").isAutoIncrement());
		assertTrue(c.get("ID").isHiddenOnCreate());
		assertTrue(c.get("ID").isReadOnlyOnEdit());
		assertFalse(c.get("ID").isRequired());

		// NOT NULL without default: required
		assertTrue(c.get("FirstName").isRequired());
		assertTrue(c.get("ACTIVE").isRequired());
		// NOT NULL with a constant default: not required, the creation form starts with the default
		assertFalse(c.get("PRICE").isRequired());
		assertEquals("0.00", c.get("PRICE").getCreateDefault());
		assertEquals("draft", c.get("STATUS").getCreateDefault());
		// NOT NULL with a computed default: the INSERT sends it, so it is shown and required
		assertTrue(c.get("CREATED_AT").isComputed());
		assertFalse(c.get("CREATED_AT").isHiddenOnCreate());
		assertTrue(c.get("CREATED_AT").isRequired());
		assertNull(c.get("CREATED_AT").getCreateDefault());
		// nullable with a computed default: hidden in creation
		assertTrue(c.get("UPDATED_AT").isHiddenOnCreate());
		assertFalse(c.get("UPDATED_AT").isRequired());
		// nullable: never required
		assertFalse(c.get("NOTES").isRequired());
		assertEquals("Free notes", c.get("NOTES").getRemarks());
	}

	@Test
	public void editableTable() throws Exception {
		var table = read("PAIRS");
		assertEquals(List.of("A", "B"), table.getKeyColumns().stream().map(SqlColumnInfo::getName).toList());
		assertFalse(table.isReadOnly());
		assertTrue(table.canCreate());
		assertTrue(table.canUpdate());
		assertTrue(table.canDelete());
		assertTrue(table.hasListPagination());
		assertEquals("\"A\"", table.getDefaultSortKey());
		assertTrue(table.getWarnings().isEmpty());
	}

	@Test
	public void binaryColumnsAreNotShownAndBlockTheUpdate() throws Exception {
		var table = read("MATRIX");
		assertFalse(table.getDisplayedColumns().stream().anyMatch(c -> c.getName().equals("PHOTO")));
		assertFalse(table.isReadOnly());
		assertTrue(table.canCreate());
		assertFalse(table.canUpdate());
		assertTrue(table.canDelete());
		assertTrue(table.getWarnings().stream().anyMatch(w -> w.contains("PHOTO")));
		assertTrue(table.getWarnings().stream().anyMatch(w -> w.contains("CREATED_AT")));
	}

	@Test
	public void tableWithoutKeyIsReadOnly() throws Exception {
		var table = read("LOGS");
		assertTrue(table.getPrimaryKeys().isEmpty());
		assertEquals(List.of("MSG"), table.getKeyColumns().stream().map(SqlColumnInfo::getName).toList());
		assertTrue(table.isReadOnly());
		assertFalse(table.canCreate());
		assertFalse(table.canUpdate());
		assertFalse(table.canDelete());
		assertEquals("\"MSG\"", table.getDefaultSortKey());
	}

	@Test
	public void columnNameOutsideVariablesIsReadOnly() throws Exception {
		var table = read("ODD");
		assertFalse(table.getColumnsByName().get("first name").isVariableName());
		assertTrue(table.isReadOnly());
		assertTrue(table.getWarnings().get(0).contains("first name"));
	}

	@Test
	public void sortKeysAreQuotedLikeTheGeneratedSelect() throws Exception {
		var table = read("MATRIX");
		var c = table.getColumnsByName();
		assertEquals("\"FirstName\"", table.getSortKey(c.get("FirstName")));
		var mysql = new SqlTableInfo("t", "com.mysql.cj.jdbc.Driver", List.of(), List.of());
		assertTrue(mysql.hasListPagination());
		var sqlServer = new SqlTableInfo("t", "com.microsoft.sqlserver.jdbc.SQLServerDriver", List.of(), List.of());
		assertEquals("[First Name]", sqlServer.getSortKey(new SqlColumnInfo("First Name", Types.VARCHAR, "varchar", 10, 0, false, null, false, false, null)));
		var db2 = new SqlTableInfo("t", "com.ibm.db2.jcc.DB2Driver", List.of(), List.of());
		assertFalse(db2.hasListPagination());
	}

	@Test
	public void defaultsOfOtherDatabases() {
		assertEquals("0", column("((0))").getCreateDefault());
		assertEquals("abc", column("('abc')").getCreateDefault());
		assertEquals("it's", column("'it''s'::character varying").getCreateDefault());
		assertEquals("true", column("b'1'").getCreateDefault());
		assertTrue(column("(getdate())").isComputed());
		assertTrue(column("nextval('t_id_seq'::regclass)").isComputed());
		assertTrue(column("CURRENT_TIMESTAMP(6)").isComputed());
		assertFalse(column("((0))").isComputed());
		assertNull(column("now()").getCreateDefault());
	}

	@Test
	public void emptyValueExpressions() {
		var pg = "org.postgresql.Driver";
		assertEquals("CAST(NULLIF({qty}, '') AS int4)", CrudSqlValue.expression(pg, "qty", Types.INTEGER, "int4", true));
		assertEquals("CAST(NULLIF({code}, '') AS uuid)", CrudSqlValue.expression(pg, "code", Types.OTHER, "uuid", true));
		assertEquals("CAST(NULLIF({n}, '') AS integer)", CrudSqlValue.expression(pg, "n", Types.INTEGER, "serial", true));
		assertEquals("CAST(NULLIF({at}, '') AS timestamptz)", CrudSqlValue.expression(pg, "at", Types.TIMESTAMP, "timestamptz", true));
		assertEquals("NULLIF({e}, '')", CrudSqlValue.expression(pg, "e", Types.VARCHAR, "\"MyEnum\"", true));
		assertEquals("NULLIF({note}, '')", CrudSqlValue.expression(pg, "note", Types.VARCHAR, "varchar", true));
		assertEquals("{label}", CrudSqlValue.expression(pg, "label", Types.VARCHAR, "varchar", false));
		assertEquals("NULLIF(CAST({QTY} AS LONGVARCHAR), '')", CrudSqlValue.expression(HSQLDB, "QTY", Types.INTEGER, "INTEGER", true));
		assertEquals("NULLIF({qty}, '')", CrudSqlValue.expression("org.mariadb.jdbc.Driver", "qty", Types.INTEGER, "INT", true));
		assertEquals("NULLIF({qty}, '')", CrudSqlValue.expression("com.microsoft.sqlserver.jdbc.SQLServerDriver", "qty", Types.INTEGER, "int", true));
		assertEquals("{QTY}", CrudSqlValue.expression("oracle.jdbc.OracleDriver", "QTY", Types.NUMERIC, "NUMBER", true));
		assertEquals("{QTY}", CrudSqlValue.expression("com.ibm.db2.jcc.DB2Driver", "QTY", Types.INTEGER, "INTEGER", true));
	}

	@Test
	public void labels() {
		assertEquals("First Name", SqlColumnInfo.prettify("first_name"));
		assertEquals("First Name", SqlColumnInfo.prettify("FirstName"));
		assertEquals("Created At", SqlColumnInfo.prettify("CREATED_AT"));
		assertEquals("Id", SqlColumnInfo.prettify("ID"));
		assertEquals("Http Url", SqlColumnInfo.prettify("HTTPUrl"));
	}

	private static SqlColumnInfo column(String defaultValue) {
		return new SqlColumnInfo("c", Types.INTEGER, "int", 10, 0, true, defaultValue, false, false, null);
	}
}
