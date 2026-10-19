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

package com.twinsoft.convertigo.beans.steps;

/** The values the Studio proposes for the properties of the native JSON steps (ITagsProperty). */
class JsonStepTags {
	static final String[] FORMATS = { "baserow" };
	static final String[] BOOLEANS = { "true", "false" };
	static final String[] TIMEZONES = { "\"UTC\"", "\"Europe/Paris\"", "\"Europe/London\"", "\"America/New_York\"", "\"Asia/Tokyo\"" };

	private JsonStepTags() {
	}

	static String[] get(String propertyName) {
		switch (propertyName) {
		case "format": return FORMATS;
		case "valuesOnly":
		case "streamMode": return BOOLEANS;
		case "defaultTimezone": return TIMEZONES;
		default: return new String[0];
		}
	}

	/** JavaScript expressions (the Studio colors them), the format is a name */
	static boolean isScriptable(String propertyName) {
		return !"format".equals(propertyName);
	}
}
