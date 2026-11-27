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

import java.util.Locale;
import java.util.regex.Pattern;

import com.twinsoft.convertigo.engine.util.StringUtils;

/** One column of a table, as the JDBC metadata describes it, with what the generated user interface does of it. */
public final class SqlColumnInfo {
	/** Names that the engine recognizes as a {variable} of a SQL query. */
	private static final Pattern VARIABLE_NAME = Pattern.compile("[a-zA-Z0-9_]+");
	/**
	 * Default values computed by the database: sequence, current date, generated uuid or any function call. Parentheses
	 * alone are not a call: SQL Server writes its constant defaults as ((0)).
	 */
	private static final Pattern COMPUTED_DEFAULT = Pattern.compile(
			"(?is).*\\b(nextval|now|current_\\w*|localtime\\w*|sysdate|systimestamp|sysdatetime|getdate|getutcdate|newid|newsequentialid|gen_random_uuid|uuid\\w*|identity)\\b.*"
			+ "|.*[a-zA-Z_]\\w*\\s*\\(.*");

	private final String name;
	private final int jdbcType;
	private final String typeName;
	private final int size;
	private final int scale;
	private final boolean notNull;
	private final String defaultValue;
	private final boolean autoIncrement;
	private final boolean generated;
	private final String remarks;
	private final SqlFieldKind kind;
	private boolean primaryKey;

	public SqlColumnInfo(String name, int jdbcType, String typeName, int size, int scale, boolean notNull,
			String defaultValue, boolean autoIncrement, boolean generated, String remarks) {
		this.name = name;
		this.jdbcType = jdbcType;
		this.typeName = typeName == null ? "" : typeName;
		this.size = size;
		this.scale = scale;
		this.notNull = notNull;
		this.defaultValue = defaultValue == null || defaultValue.isBlank() ? null : defaultValue.trim();
		this.autoIncrement = autoIncrement;
		this.generated = generated;
		this.remarks = remarks == null ? "" : remarks.trim();
		this.kind = SqlFieldKind.of(jdbcType, typeName, size);
	}

	void setPrimaryKey(boolean primaryKey) {
		this.primaryKey = primaryKey;
	}

	public String getName() {
		return name;
	}

	public int getJdbcType() {
		return jdbcType;
	}

	public String getTypeName() {
		return typeName;
	}

	public int getSize() {
		return size;
	}

	public int getScale() {
		return scale;
	}

	public boolean isNotNull() {
		return notNull;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public boolean isAutoIncrement() {
		return autoIncrement;
	}

	public boolean isGenerated() {
		return generated;
	}

	public String getRemarks() {
		return remarks;
	}

	public SqlFieldKind getKind() {
		return kind;
	}

	/** A column of the real primary key of the table (not the first column used when the table has none). */
	public boolean isPrimaryKey() {
		return primaryKey;
	}

	/** The engine can bind this column as a {variable}: its name only has letters, digits and underscores. */
	public boolean isVariableName() {
		return VARIABLE_NAME.matcher(name).matches() && StringUtils.isNormalized(name);
	}

	/** The value comes from the database: auto increment, generated column, or a computed default value. */
	public boolean isComputed() {
		return autoIncrement || generated || (defaultValue != null && COMPUTED_DEFAULT.matcher(defaultValue).matches());
	}

	/** Shown by the grid and the form (binary columns are neither shown nor sent). */
	public boolean isDisplayed() {
		return kind != SqlFieldKind.BINARY;
	}

	/** Not part of the creation form. */
	public boolean isHiddenOnCreate() {
		if (!isComputed()) {
			return false;
		}
		// the generated INSERT only leaves the auto increment columns out: the others are sent, NULL when empty
		if (CrudUiDefaults.SHOW_NOT_NULL_COMPUTED_DEFAULT_ON_CREATE && notNull && !autoIncrement && !generated) {
			return false;
		}
		return true;
	}

	/** Read-only in the edition form: keys and values computed by the database. */
	public boolean isReadOnlyOnEdit() {
		return primaryKey || autoIncrement || generated || isReadOnlyType();
	}

	/** Types that the user interface shows but never writes (Oracle ROWID, XML). */
	public boolean isReadOnlyType() {
		var type = typeName.toLowerCase(Locale.ROOT);
		return jdbcType == java.sql.Types.ROWID || jdbcType == java.sql.Types.SQLXML || type.equals("rowid") || type.equals("xmltype");
	}

	/** The form asks for a value. */
	public boolean isRequired() {
		if (!CrudUiDefaults.REQUIRED_WHEN_NOT_NULL_WITHOUT_DEFAULT || !notNull || autoIncrement || generated) {
			return false;
		}
		return defaultValue == null || (isComputed() && CrudUiDefaults.SHOW_NOT_NULL_COMPUTED_DEFAULT_ON_CREATE);
	}

	/**
	 * Initial value of the creation form for a constant default value ("0", "'draft'::character varying", "((1))"),
	 * null for none or a computed one. The generated INSERT sends every column it lists, so the database default
	 * would never apply: the form starts with it instead.
	 */
	public String getCreateDefault() {
		if (defaultValue == null || isComputed()) {
			return null;
		}
		var value = defaultValue.replaceAll("::[\\w\\s\\[\\]\"]+$", "").trim();
		while (value.length() > 1 && value.startsWith("(") && value.endsWith(")")) {
			value = value.substring(1, value.length() - 1).trim();
		}
		if (value.length() > 1 && value.startsWith("'") && value.endsWith("'")) {
			return value.substring(1, value.length() - 1).replace("''", "'");
		}
		if (value.matches("(?i)[+-]?\\d+(\\.\\d+)?|true|false|b'[01]'")) {
			return value.toLowerCase(Locale.ROOT).replace("b'1'", "true").replace("b'0'", "false");
		}
		return null;
	}

	/** Maximum length of a text input, 0 when not limited. */
	public int getMaxLength() {
		return kind == SqlFieldKind.TEXT && size > 0 ? size : 0;
	}

	/** Step of a number input: "1" for integers, "0.01" for a DECIMAL(n,2), "any" otherwise. */
	public String getStep() {
		return switch (kind) {
		case INTEGER, BIGINT -> "1";
		case DECIMAL -> scale > 0 && scale < 20 ? "0." + "0".repeat(scale - 1) + "1" : scale == 0 ? "1" : "any";
		case FLOAT -> "any";
		default -> "";
		};
	}

	/** Label of the column: "first_name" and "firstName" give "First Name". */
	public String getLabel() {
		return prettify(name);
	}

	static String prettify(String name) {
		var words = name.trim()
				.replaceAll("([a-z0-9])([A-Z])", "$1 $2")
				.replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2")
				.split("[\\s_\\-.]+");
		var sb = new StringBuilder();
		for (var word : words) {
			if (word.isEmpty()) {
				continue;
			}
			if (sb.length() > 0) {
				sb.append(' ');
			}
			var lower = word.equals(word.toUpperCase(Locale.ROOT)) && word.length() > 1 ? word.toLowerCase(Locale.ROOT) : word;
			sb.append(Character.toUpperCase(lower.charAt(0))).append(lower.substring(1));
		}
		return sb.length() == 0 ? name : sb.toString();
	}

	@Override
	public String toString() {
		return name + " " + typeName + "(" + size + (scale > 0 ? "," + scale : "") + ") " + kind
				+ (primaryKey ? " PK" : "") + (notNull ? " NOT NULL" : "")
				+ (defaultValue != null ? " DEFAULT " + defaultValue : "") + (autoIncrement ? " AUTO" : "");
	}
}
