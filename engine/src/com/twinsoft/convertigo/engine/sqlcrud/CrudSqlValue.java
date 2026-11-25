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

import java.sql.Types;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The value of a column in the INSERT and the UPDATE generated for a table. A variable that is not sent, or sent
 * empty, is bound as an empty string: for a nullable column it becomes NULL with NULLIF, followed on PostgreSQL by a
 * CAST to the type of the column (the parameter has no type there), and around a CAST to text on HSQLDB.
 */
public final class CrudSqlValue {
	/** Type names that can follow AS without quotes. */
	private static final Pattern SIMPLE_TYPE = Pattern.compile("[a-z_][a-z0-9_]*( [a-z]+)*");
	/** PostgreSQL pseudo types of the metadata, and the type to cast to. */
	private static final Map<String, String> PG_TYPES = Map.of("serial", "integer", "bigserial", "bigint", "smallserial", "smallint");

	private CrudSqlValue() {
	}

	/** {column}, NULLIF({column}, '') or CAST(NULLIF({column}, '') AS type). */
	public static String expression(String driverClassName, String column, int jdbcType, String typeName, boolean nullable) {
		var variable = "{" + column + "}";
		if (!CrudUiDefaults.EMPTY_VALUE_IS_NULL_IN_TRANSACTIONS || !nullable || driverClassName == null) {
			return variable;
		}
		if (isText(jdbcType) && !CrudUiDefaults.EMPTY_FIELD_IS_NULL) {
			return variable;
		}
		var driver = driverClassName.toLowerCase(Locale.ROOT);
		var nullIf = "NULLIF(" + variable + ", '')";
		if (driver.contains("postgresql")) {
			var type = typeName == null ? "" : typeName.toLowerCase(Locale.ROOT);
			type = PG_TYPES.getOrDefault(type, type);
			return isText(jdbcType) || !SIMPLE_TYPE.matcher(type).matches() ? nullIf : "CAST(" + nullIf + " AS " + type + ")";
		}
		if (driver.contains("hsqldb")) {
			// HSQLDB needs a type for a parameter in NULLIF; the string is then converted to the column type
			return "NULLIF(CAST(" + variable + " AS LONGVARCHAR), '')";
		}
		if (driver.contains("mysql") || driver.contains("mariadb")
				|| driver.contains("sqlserver") || driver.contains("jtds")) {
			return nullIf;
		}
		// Oracle already stores an empty string as NULL; other databases are left as they were
		return variable;
	}

	private static boolean isText(int jdbcType) {
		return switch (jdbcType) {
		case Types.CHAR, Types.VARCHAR, Types.NCHAR, Types.NVARCHAR, Types.LONGVARCHAR, Types.LONGNVARCHAR, Types.CLOB, Types.NCLOB -> true;
		default -> false;
		};
	}
}
