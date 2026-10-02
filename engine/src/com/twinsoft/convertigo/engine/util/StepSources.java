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

package com.twinsoft.convertigo.engine.util;

import java.util.Set;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.Step;

/**
 * The properties of the steps the Eclipse Studio edits with its step source editor: a source, the priority
 * of a step and an xpath, as the source of a step, the connection string of a transaction step, the tokens
 * of a push notification or the dates of a dates generation.
 */
public final class StepSources {
	private static final Set<String> OTHERS = Set.of("connectionStringDefinition", "token", "startDefinition",
			"stopDefinition", "daysDefinition");

	private StepSources() {
	}

	/**
	 * @return whether the property of the object is a step source
	 */
	public static boolean handles(DatabaseObject dbo, String name) {
		if ("sourceDefinition".equals(name)) {
			return dbo instanceof IStepSourceContainer;
		}
		return dbo instanceof Step && OTHERS.contains(name);
	}
}
