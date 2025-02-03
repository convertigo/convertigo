/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.engine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.twinsoft.convertigo.engine.events.BaseEvent;
import com.twinsoft.convertigo.engine.events.BaseEventListener;
import com.twinsoft.convertigo.engine.util.EventHelper;

public class EventManager implements AbstractManager {
	private final Map<Class<?>, EventHelper<?, ?>> eventHelpers = new ConcurrentHashMap<>();

	public <T extends BaseEventListener<E>, E extends BaseEvent> void addListener(T listener, Class<T> listenerClass) {
		EventHelper<T, E> helper = getOrCreateHelper(listenerClass);
		helper.addListener(listener);
	}

	public <T extends BaseEventListener<E>, E extends BaseEvent> void removeListener(T listener, Class<T> listenerClass) {
		EventHelper<T, E> helper = getHelper(listenerClass);
		if (helper != null) {
			helper.removeListener(listener);
		}
	}

	public <T extends BaseEventListener<E>, E extends BaseEvent> void dispatchEvent(E event, Class<T> listenerClass) {
		EventHelper<T, E> helper = getHelper(listenerClass);
		if (helper != null) {
			helper.fireEvent(event);
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends BaseEventListener<E>, E extends BaseEvent> EventHelper<T, E> getHelper(Class<T> listenerClass) {
		return (EventHelper<T, E>) eventHelpers.get(listenerClass);
	}

	@SuppressWarnings("unchecked")
	private <T extends BaseEventListener<E>, E extends BaseEvent> EventHelper<T, E> getOrCreateHelper(Class<T> listenerClass) {
		return (EventHelper<T, E>) eventHelpers.computeIfAbsent(listenerClass, k -> new EventHelper<T, E>());
	}

	synchronized public void destroy() throws EngineException {
		eventHelpers.clear();
	}

	synchronized public void init() throws EngineException {
	}
}