/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.engine.sqlcrud;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.twinsoft.convertigo.beans.connectors.SqlConnector;

/**
 * The metadata of a table read like the CRUD transaction generator reads it ({@link SqlConnector#createSqlTransaction}),
 * with everything the generated user interface needs: column families, keys, defaults and what cannot be edited.
 */
public final class SqlTableInfo {
	private final String tableName;
	private final String driverClassName;
	private final List<SqlColumnInfo> columns;
	private final List<SqlColumnInfo> primaryKeys;
	private final List<String> warnings = new ArrayList<>();
	private final List<String> readOnlyReasons = new ArrayList<>();
	private final List<String> updateBlockedReasons = new ArrayList<>();
	private final List<String> createBlockedReasons = new ArrayList<>();

	SqlTableInfo(String tableName, String driverClassName, List<SqlColumnInfo> columns, List<String> keyNames) {
		this.tableName = tableName;
		this.driverClassName = driverClassName == null ? "" : driverClassName;
		this.columns = Collections.unmodifiableList(columns);
		var keys = new ArrayList<SqlColumnInfo>();
		for (var key : keyNames) {
			for (var column : columns) {
				if (column.getName().equals(key)) {
					column.setPrimaryKey(true);
					keys.add(column);
				}
			}
		}
		this.primaryKeys = Collections.unmodifiableList(keys);
		analyse();
	}

	/** Reads the metadata of a table through the connection of a SQL connector, opened then closed. */
	public static SqlTableInfo read(SqlConnector connector, String tableName) throws Exception {
		connector.open();
		try {
			return read(connector.connection, connector.getJdbcDriverClassName(), tableName);
		} finally {
			connector.close();
		}
	}

	/** Reads the metadata of a table, with the same calls as the CRUD transaction generator. */
	public static SqlTableInfo read(Connection connection, String driverClassName, String tableName) throws SQLException {
		DatabaseMetaData dmd = connection.getMetaData();
		var catalog = connection.getCatalog();
		var columns = new ArrayList<SqlColumnInfo>();
		try (var rs = dmd.getColumns(catalog, null, tableName, "%")) {
			while (rs.next()) {
				var defaultValue = string(rs, "COLUMN_DEF");
				columns.add(new SqlColumnInfo(
						rs.getString("COLUMN_NAME"),
						rs.getInt("DATA_TYPE"),
						rs.getString("TYPE_NAME"),
						rs.getInt("COLUMN_SIZE"),
						rs.getInt("DECIMAL_DIGITS"),
						rs.getInt("NULLABLE") == DatabaseMetaData.columnNoNulls,
						defaultValue,
						"YES".equalsIgnoreCase(string(rs, "IS_AUTOINCREMENT")),
						"YES".equalsIgnoreCase(string(rs, "IS_GENERATEDCOLUMN")),
						string(rs, "REMARKS")));
			}
		}
		var keys = new TreeMap<Integer, String>();
		try (var rs = dmd.getPrimaryKeys(catalog, null, tableName)) {
			while (rs.next()) {
				keys.put(rs.getInt("KEY_SEQ"), rs.getString("COLUMN_NAME"));
			}
		}
		return new SqlTableInfo(tableName, driverClassName, columns, new ArrayList<>(keys.values()));
	}

	private static String string(ResultSet rs, String column) {
		try {
			return rs.getString(column);
		} catch (SQLException e) {
			// older drivers do not have every column of getColumns (IS_GENERATEDCOLUMN is JDBC 4.1)
			return null;
		}
	}

	private void analyse() {
		if (columns.isEmpty()) {
			readOnlyReasons.add("no column found for the table " + tableName);
			return;
		}
		if (primaryKeys.isEmpty() && CrudUiDefaults.KEYLESS_TABLE_READ_ONLY) {
			readOnlyReasons.add("the table has no primary key: an update or a delete could change several rows");
		}
		for (var column : columns) {
			if (!column.isVariableName()) {
				readOnlyReasons.add("the column '" + column.getName() + "' cannot be a variable of the generated transactions");
			}
		}
		var binaries = columns.stream().filter(c -> c.getKind() == SqlFieldKind.BINARY).toList();
		if (!binaries.isEmpty()) {
			var names = String.join(", ", binaries.stream().map(SqlColumnInfo::getName).toList());
			warnings.add("binary columns are not displayed: " + names);
			if (CrudUiDefaults.BINARY_COLUMN_BLOCKS_UPDATE) {
				updateBlockedReasons.add("an update would erase the binary columns " + names);
			}
			for (var binary : binaries) {
				if (binary.isNotNull() && !binary.isAutoIncrement()) {
					createBlockedReasons.add("the binary column '" + binary.getName() + "' is required");
				}
			}
		}
		for (var column : columns) {
			if (column.isComputed() && !column.isAutoIncrement() && column.isDisplayed()) {
				warnings.add("the generated INSERT sends the column '" + column.getName() + "', its database default does not apply");
			}
		}
		if (!hasListPagination()) {
			warnings.add("the list of this database is not paginated: the grid loads every row at once");
		}
		// PostgreSQL needs no warning for the values sent as strings: the connector adds stringtype=unspecified
	}

	public String getTableName() {
		return tableName;
	}

	public String getDriverClassName() {
		return driverClassName;
	}

	public List<SqlColumnInfo> getColumns() {
		return columns;
	}

	/** Columns shown by the grid and the form. */
	public List<SqlColumnInfo> getDisplayedColumns() {
		return columns.stream().filter(SqlColumnInfo::isDisplayed).toList();
	}

	/** The real primary key, empty when the table has none. */
	public List<SqlColumnInfo> getPrimaryKeys() {
		return primaryKeys;
	}

	/**
	 * The columns that the generated _UPDATE, _DELETE and _SELECT use in their WHERE clause: the primary key, or the
	 * first column when there is none (as {@link SqlConnector#createSqlTransaction} does).
	 */
	public List<SqlColumnInfo> getKeyColumns() {
		if (!primaryKeys.isEmpty() || columns.isEmpty()) {
			return primaryKeys;
		}
		return List.of(columns.get(0));
	}

	/** Reasons for a list without creation, edition nor deletion (empty when the table can be edited). */
	public List<String> getReadOnlyReasons() {
		return Collections.unmodifiableList(readOnlyReasons);
	}

	public boolean isReadOnly() {
		return !readOnlyReasons.isEmpty();
	}

	public boolean canCreate() {
		return !isReadOnly() && createBlockedReasons.isEmpty();
	}

	public boolean canUpdate() {
		return !isReadOnly() && updateBlockedReasons.isEmpty();
	}

	public boolean canDelete() {
		return !isReadOnly();
	}

	/** Everything the developer should know about the generated user interface of this table. */
	public List<String> getWarnings() {
		var all = new ArrayList<String>();
		readOnlyReasons.forEach(r -> all.add("read-only list: " + r));
		createBlockedReasons.forEach(r -> all.add("no creation: " + r));
		updateBlockedReasons.forEach(r -> all.add("no edition: " + r));
		all.addAll(warnings);
		return all;
	}

	/** True when the generated _LIST sequence pages with limit and offset. */
	public boolean hasListPagination() {
		return SqlConnector.hasListPagination(driverClassName);
	}

	/** The ORDER BY expression of a column for the _LIST sequence: quoted like the generated SELECT quotes it. */
	public String getSortKey(SqlColumnInfo column) {
		return SqlConnector.quoteColumnName(driverClassName, column.getName());
	}

	/** The default ORDER BY of the grid: the first key column, which keeps the pages stable. */
	public String getDefaultSortKey() {
		var keys = getKeyColumns();
		return keys.isEmpty() ? "" : getSortKey(keys.get(0));
	}

	/** Columns by name, for tests and lookups. */
	public Map<String, SqlColumnInfo> getColumnsByName() {
		var map = new TreeMap<String, SqlColumnInfo>();
		columns.forEach(c -> map.put(c.getName(), c));
		return map;
	}
}
