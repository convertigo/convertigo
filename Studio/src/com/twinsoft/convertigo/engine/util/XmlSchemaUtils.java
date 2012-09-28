package com.twinsoft.convertigo.engine.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaObjectCollection;
import org.apache.ws.commons.schema.XmlSchemaUse;
import org.apache.ws.commons.schema.constants.Constants;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;

public class XmlSchemaUtils {
	public static class XmlSchemaObjectCollectionList<E extends XmlSchemaObject> implements List<E> {
		private XmlSchemaObjectCollection collection;
		
		public XmlSchemaObjectCollectionList(XmlSchemaObjectCollection collection) {
			this.collection = collection;
		}

		public int size() {
			return collection.getCount();
		}

		public boolean isEmpty() {
			return collection.getCount() == 0;
		}

		public boolean contains(Object o) {
			if (o instanceof XmlSchemaObject) {
				return collection.indexOf((XmlSchemaObject) o) != -1;
			} else {
				return false;
			}
		}

		public Iterator<E> iterator() {
			return GenericUtils.cast(collection.getIterator());
		}

		public Object[] toArray() {
			return null;
		}

		public <T> T[] toArray(T[] a) {
			return null;
		}

		public boolean add(XmlSchemaObject o) {
			collection.add(o);
			return true;
		}

		public boolean remove(Object o) {
			if (contains(o)) {
				collection.remove((XmlSchemaObject) o);
				return true;
			} else {
				return false;
			}
		}

		public boolean containsAll(Collection<?> c) {
			return false;
		}

		public boolean addAll(Collection<? extends E> c) {
			return false;
		}

		public boolean addAll(int index, Collection<? extends E> c) {
			return false;
		}

		public boolean removeAll(Collection<?> c) {
			return false;
		}

		public boolean retainAll(Collection<?> c) {
			return false;
		}

		public void clear() {
			XmlSchemaUtils.clear(collection);
		}

		public E get(int index) {
			return GenericUtils.cast(collection.getItem(index));
		}

		public E set(int index, XmlSchemaObject element) {
			E o = get(index);
			collection.setItem(index, element);
			return o;
		}

		public void add(int index, XmlSchemaObject element) {
			
		}

		public E remove(int index) {
			E o = get(index);
			collection.removeAt(index);
			return o;
		}

		public int indexOf(Object o) {
			if (o instanceof XmlSchemaObject) {
				return collection.indexOf((XmlSchemaObject) o);
			} else {
				return -1;
			}
		}

		public int lastIndexOf(Object o) {
			return -1;
		}

		public ListIterator<E> listIterator() {
			return null;
		}

		public ListIterator<E> listIterator(int index) {
			return null;
		}

		public List<E> subList(int fromIndex, int toIndex) {
			return null;
		}
		
	}
	
	final public static XmlSchemaUse attributeUseRequired = new XmlSchemaUse(Constants.BlockConstants.REQUIRED);
	final public static XmlSchemaUse attributeUseOptional = new XmlSchemaUse(Constants.BlockConstants.OPTIONAL);
	
	final public static Comparator<XmlSchemaAttribute> attributeNameComparator = new Comparator<XmlSchemaAttribute>() {
		public int compare(XmlSchemaAttribute o1, XmlSchemaAttribute o2) {
			return o1.getName().compareTo(o2.getName());
		}
	};
	
	public static SortedSet<XmlSchemaAttribute> attributesToSortedSet(XmlSchemaObjectCollection attrs) {
		SortedSet<XmlSchemaAttribute> result = new TreeSet<XmlSchemaAttribute>(XmlSchemaUtils.attributeNameComparator);
		for (Iterator<XmlSchemaAttribute> i = GenericUtils.cast(attrs.getIterator()); i.hasNext();) {
			result.add(i.next());
		}
		return result;
	}
	
	public static void clear(XmlSchemaObjectCollection collection) {
		int count = collection.getCount();
		while (count > 0) {
			collection.removeAt(--count);
		}
	}
	
	public static <E extends XmlSchemaObject> E makeDynamic(DatabaseObject databaseObject, E xso) {
		SchemaMeta.getReferencedDatabaseObjects(xso).add(databaseObject);
		SchemaMeta.setDynamic(xso);
		return xso;
	}
	
	public static <E extends XmlSchemaObject> E makeDynamic(Collection<DatabaseObject> databaseObjects, E xso) {
		SchemaMeta.getReferencedDatabaseObjects(xso).addAll(databaseObjects);
		SchemaMeta.setDynamic(xso);
		return xso;
	}
}
