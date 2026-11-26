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

/** Family of a SQL column for the generated user interface: it gives the grid column and the form widget. */
public enum SqlFieldKind {
	/** INTEGER, SMALLINT, TINYINT: a number input with a step of 1. */
	INTEGER("number"),
	/** BIGINT: like an integer, sent as a string (JavaScript numbers lose precision beyond 2^53). */
	BIGINT("number"),
	/** DECIMAL, NUMERIC: read and sent as a decimal string, never as a float. */
	DECIMAL("number"),
	/** REAL, FLOAT, DOUBLE. */
	FLOAT("number"),
	/** BIT, BOOLEAN: a check box in the grid, a toggle in the form. */
	BOOLEAN("checkbox"),
	/** Short texts (up to 255 characters). */
	TEXT("text"),
	/** Long texts: CLOB, LONGVARCHAR, PostgreSQL text, texts longer than 255 characters. */
	LONG_TEXT("textarea"),
	/** DATE: sent as YYYY-MM-DD. */
	DATE("date"),
	/** TIMESTAMP: text input to support JDBC fractions and offsets. */
	TIMESTAMP("text"),
	/** TIME: text input to support JDBC fractions and offsets. */
	TIME("text"),
	/** Binary columns: neither displayed nor sent. */
	BINARY(""),
	/** Any other type (uuid, json, enums...): a text input. */
	OTHER("text");

	static final int LONG_TEXT_THRESHOLD = 255;

	private final String inputType;

	SqlFieldKind(String inputType) {
		this.inputType = inputType;
	}

	/** Type of the form input ("number", "text", "date"...), "checkbox" for a toggle, "textarea", or "" when absent. */
	public String inputType() {
		return inputType;
	}

	public boolean isNumber() {
		return "number".equals(inputType);
	}

	/** The family of a column from its JDBC metadata (DATA_TYPE, TYPE_NAME and COLUMN_SIZE). */
	public static SqlFieldKind of(int jdbcType, String typeName, int size) {
		var type = typeName == null ? "" : typeName.toLowerCase(Locale.ROOT);
		if ("uuid".equals(type)) {
			return OTHER; // HSQLDB exposes UUID as JDBC BINARY.
		}
		return switch (jdbcType) {
		case Types.INTEGER, Types.SMALLINT, Types.TINYINT -> INTEGER;
		case Types.BIGINT -> BIGINT;
		case Types.DECIMAL, Types.NUMERIC -> DECIMAL;
		case Types.REAL, Types.FLOAT, Types.DOUBLE -> FLOAT;
		// a PostgreSQL bit(n) with n > 1 is a bit string, not a boolean
		case Types.BIT -> size > 1 && type.startsWith("bit") ? OTHER : BOOLEAN;
		case Types.BOOLEAN -> BOOLEAN;
		case Types.CHAR, Types.VARCHAR, Types.NCHAR, Types.NVARCHAR ->
			size <= 0 || size > LONG_TEXT_THRESHOLD || "text".equals(type) ? LONG_TEXT : TEXT;
		case Types.CLOB, Types.NCLOB, Types.LONGVARCHAR, Types.LONGNVARCHAR -> LONG_TEXT;
		case Types.DATE -> DATE;
		case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> TIMESTAMP;
		case Types.TIME, Types.TIME_WITH_TIMEZONE -> TIME;
		case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB -> BINARY;
		default -> OTHER;
		};
	}
}
