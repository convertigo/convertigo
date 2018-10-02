package com.twinsoft.convertigo.engine.util;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class WeakValueHashMap<K, V> implements Map<K, V> {
	
	Map<K, WeakReference<V>> map;
	
	public WeakValueHashMap() {
		map = new HashMap<>();
	}
	
	public WeakValueHashMap(int initialCapacity) {
		map = new HashMap<>(initialCapacity);
	}
	
	@Override
	public int size() {
		return map.size();
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return false;
	}

	@Override
	public V get(Object key) {
		WeakReference<V> w = map.get(key);
		return w != null ? w.get() : null;
	}

	@Override
	public V put(K key, V value) {
		WeakReference<V> w = map.put(key, new WeakReference<>(value));
		return w != null ? w.get() : null;
	}

	@Override
	public V remove(Object key) {
		WeakReference<V> w = map.remove(key);
		return w != null ? w.get() : null;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for (Entry<? extends K, ? extends V> e: m.entrySet()) {
			map.put(e.getKey(), new WeakReference<>(e.getValue()));
		}
	}

	@Override
	public void clear() {
		map.clear();
	}

	@Override
	public Set<K> keySet() {
		return map.keySet();
	}

	@Override
	public Collection<V> values() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		throw new UnsupportedOperationException();
	}
	
}
