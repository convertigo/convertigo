/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details: <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.sessions;

import com.twinsoft.convertigo.engine.EnginePropertiesManager.ComboEnum;

/** What a Redis-mode instance does when Redis cannot be reached at startup. */
public enum RedisStartupFallback implements ComboEnum {
	/** Keep retrying in background, stay not ready and refuse session requests until Redis is reachable. */
	none,
	/** Use local sessions for the whole life of the instance, reported as degraded. */
	tomcat;

	@Override
	public String getDisplay() {
		return name();
	}

	@Override
	public String getValue() {
		return name();
	}

	static RedisStartupFallback fromProperty(String raw) {
		if (raw != null) {
			for (var value : values()) {
				if (value.name().equalsIgnoreCase(raw.trim())) {
					return value;
				}
			}
		}
		return none;
	}
}
