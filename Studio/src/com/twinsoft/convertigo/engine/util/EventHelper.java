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
