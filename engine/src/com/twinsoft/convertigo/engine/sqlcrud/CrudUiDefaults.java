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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The choices of the SQL CRUD user interface generator, kept in one place so that each one can be changed alone.
 * Everything generated from them stays editable in the project afterwards.
 */
public final class CrudUiDefaults {

	private CrudUiDefaults() {
	}

	/** Name of the library project that provides the grid. */
	public static final String LIB_PROJECT_NAME = "lib_ExtendedComponents_ui_ngx";

	/** Version of the library added when the project has no reference to it yet (pinned release archive). */
	public static final String LIB_VERSION = "v8.4.0.49";

	/** The project reference added when it is missing: the release archive of {@link #LIB_VERSION}. */
	public static final String LIB_REFERENCE = LIB_PROJECT_NAME
			+ "=https://github.com/convertigo/c8oprj-lib-extended-components-ui-ngx/releases/download/"
			+ LIB_VERSION + "/" + LIB_PROJECT_NAME + ".car";

	/** An existing reference to the library is kept as it is, whatever its version. */
	public static final boolean KEEP_EXISTING_LIB_REFERENCE = true;

	/** Shared component of the library used for the grid, and the shared action that answers its GetRows event. */
	public static final String LIB_GRID = LIB_PROJECT_NAME + ".MobileApplication.Application.agGrid";
	public static final String LIB_GRID_UPDATE_ROWS = LIB_PROJECT_NAME + ".MobileApplication.Application.agGridUpdateRows";

	/** An emptied field of the form is not sent: the column is saved as NULL (texts included). */
	public static final boolean EMPTY_FIELD_IS_NULL = true;

	/**
	 * The CRUD transactions generated from now on turn an empty value into NULL for the nullable columns (an absent
	 * variable is an empty string): always for the other columns, for the texts when {@link #EMPTY_FIELD_IS_NULL}.
	 * Transactions generated before are not changed. See {@link CrudSqlValue}.
	 */
	public static final boolean EMPTY_VALUE_IS_NULL_IN_TRANSACTIONS = true;

	/** Column filters of the grid: off, the list sequences do not filter on the server side. */
	public static final boolean COLUMN_FILTERS = false;

	/** A NOT NULL column without a default value is a required field. */
	public static final boolean REQUIRED_WHEN_NOT_NULL_WITHOUT_DEFAULT = true;

	/**
	 * Workaround while the generated INSERT keeps the columns with a computed default: a NOT NULL one is shown and
	 * required in the creation form instead of being hidden (hidden, it would insert NULL and break the constraint).
	 */
	public static final boolean SHOW_NOT_NULL_COMPUTED_DEFAULT_ON_CREATE = true;

	/**
	 * Workaround while the generated UPDATE sets every column but the keys: the rows of a table with a binary column
	 * are not editable, an update would erase the binary values that the user interface never shows.
	 */
	public static final boolean BINARY_COLUMN_BLOCKS_UPDATE = true;

	/** An existing shared component or page with the same name is never overwritten: the table is skipped. */
	public static final boolean NEVER_OVERWRITE = true;

	/** Default values of the generated component variables. */
	public static final int PAGE_SIZE = 50;
	public static final boolean PAGINATION = false;
	public static final String HEIGHT = "600px";
	public static final String GRID_THEME = "ag-theme-quartz";
	public static final String GRID_LOCALE = "en";

	/** Texts of the generated user interface, in the labels variable of the component. */
	public static Map<String, String> labels() {
		var labels = new LinkedHashMap<String, String>();
		labels.put("add", "Add");
		labels.put("view", "View");
		labels.put("edit", "Edit");
		labels.put("delete", "Delete");
		labels.put("clearSelection", "Clear selection");
		labels.put("save", "Save");
		labels.put("boolean_unset", "Not set");
		labels.put("boolean_true", "True");
		labels.put("boolean_false", "False");
		labels.put("cancel", "Cancel");
		labels.put("close", "Close");
		labels.put("confirmTitle", "Confirm");
		labels.put("confirmDelete", "Delete {n} row(s)?");
		labels.put("deleted", "{n} row(s) deleted");
		labels.put("deleteFailed", "{m} row(s) not deleted");
		labels.put("saved", "Row saved");
		labels.put("loadFailed", "Unable to load the rows");
		labels.put("createTitle", "New row");
		labels.put("editTitle", "Edit row");
		labels.put("viewTitle", "Row");
		return labels;
	}
}
