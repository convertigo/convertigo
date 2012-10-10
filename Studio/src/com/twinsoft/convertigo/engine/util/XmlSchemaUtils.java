package com.twinsoft.convertigo.engine.util;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaObjectCollection;
import org.apache.ws.commons.schema.XmlSchemaSerializer.XmlSchemaSerializerException;
import org.apache.ws.commons.schema.XmlSchemaUse;
import org.apache.ws.commons.schema.constants.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

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
	
	final private static SchemaFactory factory = SchemaFactory.newInstance(Constants.URI_2001_SCHEMA_XSD);
	final private static Source emptySource = new DOMSource(XMLUtils.getDefaultDocumentBuilder().newDocument());
	
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
	
	public static Document getDomInstance(XmlSchemaObject object) {
		return getDomInstance(object, null);
	}
	
	public static Document getDomInstance(XmlSchemaObject object, final Map<Node, XmlSchemaObject> references) {
		final Document doc = XMLUtils.getDefaultDocumentBuilder().newDocument();
		final Element root = doc.createElement("document");
		doc.appendChild(root);
		
		new XmlSchemaWalker() {
			Node parent = root;
			int maxDepth = 50;
			
			@Override
			protected void walkElement(XmlSchema xmlSchema, XmlSchemaElement obj) {
				Node myParent = parent;
				XmlSchemaElement element = (XmlSchemaElement) obj;
				Element xElement = doc.createElement(element.getName());
				if (references != null) {
					references.put(xElement, element);
				}
				myParent.appendChild(xElement);
				parent = xElement;
				if (--maxDepth > 0) {
					super.walkElement(xmlSchema, obj);
				}
				parent = myParent;
			}

			@Override
			protected void walkAttribute(XmlSchema xmlSchema, XmlSchemaAttribute obj) {
				if (parent instanceof Element) {
					Element xParent = (Element) parent;
					String name = obj.getName();
					xParent.setAttribute(name, "");
					if (references != null) {
						references.put(xParent.getAttributeNode(name), obj);
					}
				}
			}
			
		}.walk(SchemaMeta.getSchema(object), object);
		
		return doc;
	}

	public static void validate(XmlSchema schema) throws SAXException {
		validate(schema, emptySource);
	}
	
	public static void validate(XmlSchema schema, Document document) throws SAXException {
		validate(schema, new DOMSource(document));
	}
	
	private static void validate(XmlSchema schema, Source source) throws SAXException {
		try {
		      Schema vSchema = factory.newSchema(new DOMSource(schema.getSchemaDocument()));
		      Validator validator = vSchema.newValidator();
		      validator.validate(source);
			} catch (XmlSchemaSerializerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		      // at last perform validation:
	}
}
