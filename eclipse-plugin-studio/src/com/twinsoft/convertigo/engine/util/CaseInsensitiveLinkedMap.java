/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.engine.util;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class CaseInsensitiveLinkedMap<V> extends AbstractMap<String, V> {
	Set<Entry<String, V>> list = new LinkedHashSet<Entry<String, V>>();

	@Override
	public boolean containsKey(Object key) {
		Iterator<Map.Entry<String, V>> i = entrySet().iterator();
		if (key == null) {
			while (i.hasNext()) {
				Entry<String, V> e = i.next();
				if (e.getKey() == null) {
					return true;
				}
			}
		} else {
			while (i.hasNext()) {
				Entry<String, V> e = i.next();
				if (((String) key).equalsIgnoreCase(e.getKey())) {
					return true;
				}
			}
		}
		return false;
	}

	public Set<Entry<String, V>> entrySet() {
		return list;
	}

	@Override
	public V get(Object key) {
		Iterator<Entry<String, V>> i = entrySet().iterator();
		if (key == null) {
			while (i.hasNext()) {
				Entry<String, V> e = i.next();
				if (e.getKey() == null) {
					return e.getValue();
				}
			}
		} else {
			while (i.hasNext()) {
				Entry<String, V> e = i.next();
				if (((String) key).equalsIgnoreCase(e.getKey())) {
					return e.getValue();
				}
			}
		}
		return null;
	}

	@Override
	public V put(final String key, V value) {
		Iterator<Entry<String, V>> i = entrySet().iterator();
		Entry<String, V> correctEntry = null;
		if (key == null) {
			while (i.hasNext()) {
				Entry<String, V> e = i.next();
				if (e.getKey() == null) {
					correctEntry = e;
				}
			}
		} else {
			while (i.hasNext()) {
				Entry<String, V> e = i.next();
				if (((String) key).equalsIgnoreCase(e.getKey())) {
					correctEntry = e;
				}
			}
		}

		if (correctEntry == null) {
			list.add(correctEntry =  new Entry<String, V>() {
				private V v = null;
				
				public String getKey() {
					return key;
				}

				public V getValue() {
					return v;
				}

				public V setValue(V value) {
					V oldValue = v;
					v = value;
					return oldValue;
				}
			});
		}
		
		return correctEntry.setValue(value);
	}

	@Override
	public V remove(Object key) {
		Iterator<Entry<String, V>> i = entrySet().iterator();
		Entry<String, V> correctEntry = null;
		if (key == null) {
			while (correctEntry == null && i.hasNext()) {
				Entry<String, V> e = i.next();
				if (e.getKey() == null) {
					correctEntry = e;
				}
			}
		} else {
			while (correctEntry == null && i.hasNext()) {
				Entry<String, V> e = i.next();
				if (((String) key).equalsIgnoreCase(e.getKey())) {
					correctEntry = e;
				}
			}
		}

		V oldValue = null;
		if (correctEntry != null) {
			oldValue = correctEntry.getValue();
			i.remove();
		}
		return oldValue;
	}
}