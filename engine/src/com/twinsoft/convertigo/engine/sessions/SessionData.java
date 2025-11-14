/*
 * Copyright (c) 2001-2025 Convertigo SA.
 *
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the  Free Software Foundation;  either
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

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SessionData implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	private final String id;
	private final Map<String, Object> attributes = new ConcurrentHashMap<>();
	private long creationTime;
	private long lastAccessedTime;
	private int maxInactiveInterval;
	private boolean isNew = true;

	private SessionData(String id) {
		this.id = id;
		var now = System.currentTimeMillis();
		this.creationTime = now;
		this.lastAccessedTime = now;
	}

	public static SessionData newSession(String id, int defaultMaxInactiveInterval) {
		var data = new SessionData(id);
		data.maxInactiveInterval = defaultMaxInactiveInterval;
		return data;
	}

	public String getId() {
		return id;
	}

	public long getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(long creationTime) {
		this.creationTime = creationTime;
	}

	public long getLastAccessedTime() {
		return lastAccessedTime;
	}

	public void setLastAccessedTime(long lastAccessedTime) {
		this.lastAccessedTime = lastAccessedTime;
	}

	public int getMaxInactiveInterval() {
		return maxInactiveInterval;
	}

	public void setMaxInactiveInterval(int maxInactiveInterval) {
		this.maxInactiveInterval = maxInactiveInterval;
	}

	public boolean isNew() {
		return isNew;
	}

	public void setNew(boolean aNew) {
		isNew = aNew;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public Object getAttribute(String name) {
		return attributes.get(name);
	}

	public void setAttribute(String name, Object value) {
		if (value == null) {
			attributes.remove(name);
		} else {
			attributes.put(name, value);
		}
	}

	public void removeAttribute(String name) {
		attributes.remove(name);
	}

	public Enumeration<String> getAttributeNames() {
		Set<String> names = attributes.keySet();
		return Collections.enumeration(names);
	}

	public void touch() {
		lastAccessedTime = System.currentTimeMillis();
		isNew = false;
	}

	static SessionData copyOf(SessionData source) {
		var copy = new SessionData(source.id);
		copy.creationTime = source.creationTime;
		copy.lastAccessedTime = source.lastAccessedTime;
		copy.maxInactiveInterval = source.maxInactiveInterval;
		copy.isNew = source.isNew;
		copy.attributes.putAll(source.attributes);
		return copy;
	}
}
