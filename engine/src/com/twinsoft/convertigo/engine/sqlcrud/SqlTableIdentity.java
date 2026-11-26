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

/** The exact table selected from JDBC metadata. */
public record SqlTableIdentity(String catalog, String schema, String name) {

	public static SqlTableIdentity from(ResultSet rows) throws SQLException {
		return new SqlTableIdentity(rows.getString("TABLE_CAT"), rows.getString("TABLE_SCHEM"), rows.getString("TABLE_NAME"));
	}

	static SqlTableIdentity find(Connection connection, String name) throws SQLException {
		var metadata = connection.getMetaData();
		SqlTableIdentity match = null;
		try (var rows = metadata.getTables(connection.getCatalog(), null, literalPattern(metadata, name), null)) {
			while (rows.next()) {
				var table = from(rows);
				if (name.equals(table.name())) {
					if (match != null && !match.equals(table)) {
						throw new SQLException("Several tables are named " + name + "; select a catalog and schema.");
					}
					match = table;
				}
			}
		}
		if (match == null) {
			throw new SQLException("Table not found: " + name);
		}
		return match;
	}

	public static String literalPattern(DatabaseMetaData metadata, String identifier) throws SQLException {
		if (identifier == null) {
			return null;
		}
		var escape = metadata.getSearchStringEscape();
		if (escape == null || escape.isEmpty()) {
			// The callers check the exact identity of each result.
			return identifier;
		}
		var pattern = new StringBuilder();
		for (int i = 0; i < identifier.length();) {
			if (identifier.startsWith(escape, i)) {
				pattern.append(escape).append(escape);
				i += escape.length();
			} else {
				char c = identifier.charAt(i++);
				if (c == '%' || c == '_') {
					pattern.append(escape);
				}
				pattern.append(c);
			}
		}
		return pattern.toString();
	}

	static String quote(String quote, String identifier) {
		return quote + identifier.replace(quote, quote + quote) + quote;
	}

	String qualifiedName(DatabaseMetaData metadata, String quote) throws SQLException {
		var qualified = quote(quote, name);
		if (schema != null && !schema.isEmpty() && metadata.supportsSchemasInDataManipulation()) {
			qualified = quote(quote, schema) + "." + qualified;
		}
		if (catalog != null && !catalog.isEmpty() && metadata.supportsCatalogsInDataManipulation()) {
			var separator = metadata.getCatalogSeparator();
			var catalogName = quote(quote, catalog);
			qualified = metadata.isCatalogAtStart() ? catalogName + separator + qualified : qualified + separator + catalogName;
		}
		return qualified;
	}

	public String displayName() {
		return (catalog == null || catalog.isEmpty() ? "" : catalog + ".")
				+ (schema == null || schema.isEmpty() ? "" : schema + ".") + name;
	}
}
