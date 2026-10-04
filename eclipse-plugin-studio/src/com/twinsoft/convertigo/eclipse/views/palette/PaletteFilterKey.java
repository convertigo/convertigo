/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.eclipse.views.palette;

import java.util.List;
import java.util.Set;

/** Immutable snapshot of every user-controlled palette filter. */
record PaletteFilterKey(String query, String targetType, String target, String project,
		Set<String> hiddenCategories, List<String> favorites, List<String> lastUsed,
		boolean builtIn, boolean shared) {
	PaletteFilterKey {
		hiddenCategories = Set.copyOf(hiddenCategories);
		favorites = List.copyOf(favorites);
		lastUsed = List.copyOf(lastUsed);
	}

	static String context(String javaType, String kind, String type) {
		return javaType + "\u0000" + kind + "\u0000" + type;
	}
}
