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

import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;



public class GenericUtils {
	@SuppressWarnings("unchecked")
	public static <E> E clone(E object) {
		Class<?> c = object.getClass();
		try {
			return (E) c.getMethod("clone").invoke(object);
		} catch (Exception e) {
			// null if failed
		}
		return null;
	}
	
	public static List<String> toString(Collection<?> col) {
		List<String> res = new ArrayList<String>(col.size());
		for (Object o : col) {
			res.add(o != null ? o.toString() : "");
		}
		return res;
	}
		
	@SuppressWarnings("unchecked")
	public static <E> E cast(Object o) {
		return (E) o;
	}
	
	public static void removeWeak(Collection<? extends WeakReference<? extends Object>> collection) {
		Collection<WeakReference<? extends Object>> toRemove = new LinkedList<WeakReference<? extends Object>>();
		for (WeakReference<? extends Object> wo : collection) {
			if (wo.get() == null) {
				toRemove.add(wo);
			}
		}
		for (WeakReference<? extends Object> wo : toRemove) {
			collection.remove(wo);
		}
	}
	
	 public static void setListSize(List<?> list, int size){
		int ls = list.size();
		if (ls < size) {
			for (int i=ls; i < size; i++) {
				list.add(null);
			}
		} else if (ls>size) {
			for (int i = ls-1; i >= size; i--) {
				list.remove(i);
			}
		}
	}
	
	public static <E> List<E> asList(Object... o) {
		return cast(Arrays.asList(o));
	}
	
	public static <E> E[] copyOf(E[] original, int newLength) {
		if (original.length != newLength) {
			E[] res = cast(Array.newInstance(original.getClass().getComponentType(), newLength));
			for (int i=0; i < newLength && i < original.length; i++) {
				res[i] = original[i];
			}
			return res;
		} else {
			return original.clone();
		}
	}
	
	public static <E extends Annotation> E getAnnotation(Class<E> at, Enum<?> enu) {
		try {
			return enu.getClass().getField(enu.name()).getAnnotation(at);
		} catch (Exception e) {
			// null if failed
		}
		return null;
	}
	
	public static <K, V> Map<K, V> uniqueMap(final Map<K, V[]> map) {
		return new Map<K, V>() {
			public void clear() {
				map.clear();
			}

			public boolean containsKey(Object key) {
				return map.containsKey(key);
			}

			public boolean containsValue(Object value) {
				return false;
			}

			public Set<java.util.Map.Entry<K, V>> entrySet() {
				Set<K> keys = map.keySet();
				Set<Entry<K, V>> res = new HashSet<Entry<K,V>>(keys.size());
				for (final K key : keys) {
					res.add(new Entry<K, V>() {
						public K getKey() {
							return key;
						}
						public V getValue() {
							return get(getKey());
						}
						public V setValue(V value) {
							return put(getKey(), value);
						}
					});
				}
				return res;
			}

			public V get(Object key) {
				return getFirst(map.get(key));
			}

			public boolean isEmpty() {
				return map.isEmpty();
			}

			public Set<K> keySet() {
				return map.keySet();
			}

			public V put(K key, V value) {
				if (value == null) {
					return getFirst(map.put(key, null));
				} else {
					V[] values = cast(Array.newInstance(value.getClass(), 1));
					values[0] = value;
					return getFirst(map.put(key, values));
				}
			}

			public void putAll(Map<? extends K, ? extends V> m) {
				for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
					put(entry.getKey(), entry.getValue());
				}
			}

			public V remove(Object key) {
				return getFirst(map.remove(key));
			}

			public int size() {
				return map.size();
			}

			public Collection<V> values() {
				Collection<V[]> values = map.values();
				Collection<V> res = new ArrayList<V>(values.size());
				for (V[] vals : values) {
					res.add(getFirst(vals));
				}
				return res;
			}
			
			private V getFirst(V[] array) {
				if (array == null || array.length == 0) {
					return null;
				} else {
					return array[0];
				}
			}
		};
	}
	
	public static <E> boolean contains(Collection<WeakReference<E>> collection, E object) {
		for (Iterator<WeakReference<E>> i = collection.iterator(); i.hasNext();) {
			E item = i.next().get();
			if (item == null) {
				i.remove();
			} else if (item.equals(object)) {
				return true;
			}
		}
		return false;
	}
	
	public static <E> void remove(Collection<WeakReference<E>> collection, E object, boolean removeAll) {
		for (Iterator<WeakReference<E>> i = collection.iterator(); i.hasNext();) {
			E item = i.next().get();
			if (item == null) {
				i.remove();
			} else if (item.equals(object)) {
				i.remove();
				if (!removeAll) {
					return;
				}
			}
		}
	}
	
	public static <E> Collection<E> unWeak(Collection<WeakReference<E>> collection) {
		Collection<E> result = new ArrayList<E>(collection.size());
		for (Iterator<WeakReference<E>> i = collection.iterator(); i.hasNext();) {
			E item = i.next().get();
			if (item == null) {
				i.remove();
			} else {
				result.add(item);
			}
		}
		return result;
	}
	
	public static <E> E nextOrNull(Iterator<E> iterator) {
		return iterator.hasNext() ? iterator.next() : null;
	}
	
	public static <E> void merge(List<E> first, List<E> second, List<E> result, List<Boolean> minor, Comparator<E> comparator) {
		int iFirst = 0;
		int iSecond = 0;
		int sFirst = first.size();
		int sSecond = second.size();
		
		while (iFirst < sFirst && iSecond < sSecond) {
			E finded = null;
			boolean search = true;
			for (int dec = 0 ; finded == null && search; dec++) {
				search = false;
				int iEnd = iFirst + dec;
				if (iEnd < sFirst) {
					search = true;
					E item1 = first.get(iEnd);
					E item2 = second.get(iSecond);
					if (comparator.compare(item1, item2) == 0) {
						for (; iFirst < iEnd ; iFirst++) {
							result.add(first.get(iFirst));
							minor.add(true);
						}
						finded = item1;
					}
				}
				if (finded == null && (iEnd = iSecond + dec) < sSecond) {
					search = true;
					E item1 = first.get(iFirst);
					E item2 = second.get(iEnd);
					if (comparator.compare(item1, item2) == 0) {
						for (; iSecond < iEnd ; iSecond++) {
							result.add(second.get(iSecond));
							minor.add(true);
						}
						finded = item1;
					}
				}
			}
			
			if (search) {
				result.add(finded);
				minor.add(false);
				iFirst++;
				iSecond++;
			} else {
				result.add(first.get(iFirst++));
				minor.add(true);
				result.add(second.get(iSecond++));
				minor.add(true);
			}
		}
		
		for (;iFirst < sFirst; iFirst++) {
			result.add(first.get(iFirst));
			minor.add(true);
		}
		
		for (;iSecond < sSecond; iSecond++) {
			result.add(second.get(iSecond));
			minor.add(true);
		}
	}

	public static void main(String args[]) {
//		List<Character> a1 = Arrays.asList('A', 'B', 'C', 'D', 'E', 'F');
//		List<Character> b1 = Arrays.asList('A', 'B', 'C', 'D', 'E', 'F');
		
//		List<Character> a1 = Arrays.asList('A', 'C', 'D');
//		List<Character> b1 = Arrays.asList('A', 'B', 'C');

//		List<Character> a1 = Arrays.asList('A', 'B', 'C', 'D', 'E', 'F');
//		List<Character> b1 = Arrays.asList('A', 'B', 'D', 'E', 'F');
		
		List<Character> a1 = Arrays.asList('A', 'B', 'C', 'E', 'D', 'F');
		List<Character> b1 = Arrays.asList('B', 'B', 'C', 'A', 'F', 'D', 'F');

//		List<Character> a1 = Arrays.asList('E', 'D');
//		List<Character> b1 = Arrays.asList('A', 'F', 'D');
		
		List<Character> result = new ArrayList<Character>(a1.size() + b1.size());
		List<Boolean> minor = new ArrayList<Boolean>(a1.size() + b1.size());
		
		merge(a1, b1, result, minor, new Comparator<Character>() {
			public int compare(Character o1, Character o2) {
				return o1.compareTo(o2);
			}
		});
		
		System.out.println(a1);
		System.out.println(b1);
		System.out.println(result);
		System.out.println(minor);
	}
}
