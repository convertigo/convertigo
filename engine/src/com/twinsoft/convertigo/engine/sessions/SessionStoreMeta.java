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

package com.twinsoft.convertigo.engine.sessions;

import java.util.Map;

/**
 * Metadata of a stored session, with the serialized forms of the attributes read in the same command, by name (a null
 * form for an attribute that is not stored).
 */
public record SessionStoreMeta(long creationTime, long lastAccessedTime, int maxInactiveInterval, Map<String, String> attributes) {
	public SessionStoreMeta(long creationTime, long lastAccessedTime, int maxInactiveInterval) {
		this(creationTime, lastAccessedTime, maxInactiveInterval, Map.of());
	}
}

