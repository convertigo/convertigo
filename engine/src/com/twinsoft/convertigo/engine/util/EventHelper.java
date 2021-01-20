/*
 * Copyright (c) 2001-2021 Convertigo SA.
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

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;


public class EventHelper {
	private Map<Class<?>, Collection<WeakReference<?>>> listeners = new HashMap<Class<?>, Collection<WeakReference<?>>>();
	
	public synchronized <E> void addListener(Class<E> key, E listener) {
		Collection<WeakReference<E>> collection = GenericUtils.cast(listeners.get(key));
		if (collection == null) {
			collection = new LinkedList<WeakReference<E>>();
			listeners.put(key, GenericUtils.<Collection<WeakReference<?>>>cast(collection));
		}
		if (!GenericUtils.contains(collection, listener)) {
			collection.add(new WeakReference<E>(listener));	
		}
	}
	
	public synchronized <E> void removeListener(Class<E> key, E listener) {
		Collection<WeakReference<E>> collection = GenericUtils.cast(listeners.get(key));
		if (collection != null) {
			GenericUtils.remove(collection, listener, false);
		}
	}
	
	public synchronized <E> Collection<E> getListeners(Class<E> key) {
		Collection<E> result = Collections.emptyList();
		Collection<WeakReference<E>> collection = GenericUtils.cast(listeners.get(key));
		if (collection != null) {
			result = GenericUtils.unWeak(collection);
		}
		return result;
	}
}
