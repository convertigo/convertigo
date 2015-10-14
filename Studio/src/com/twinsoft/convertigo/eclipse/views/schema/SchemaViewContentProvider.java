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

package com.twinsoft.convertigo.eclipse.views.schema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaExternal;
import org.apache.ws.commons.schema.XmlSchemaGroup;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.constants.Constants;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.twinsoft.convertigo.engine.enums.SchemaMeta;
import com.twinsoft.convertigo.engine.util.XmlSchemaWalker;

public class SchemaViewContentProvider implements ITreeContentProvider {
	static public final DecoratingLabelProvider decoratingLabelProvider = new DecoratingLabelProvider(new SchemaViewLabelProvider(), new SchemaViewLabelDecorator());
	
	static public class Root {
		private Object root;
		
		private Root(Object root) {
			this.root = root;
		}
		
		public Object get() {
			return root;
		}
	}
	
	static public Root newRoot(Object root) {
		return new Root(root);
	}
	
	static public class NamedList {
		List<?> list;
		String name;
		
		public NamedList(String name, List<?> list) {
			this.name = name;
			this.list = list;
		}
		
		public String getName() {
			return name;
		}
		
		public List<?> getList() {
			return list;
		}
	}
	
	XmlSchemaCollection collection = null;
	private int maxDepth = -1;
	private Map<Object, Integer> depths = null;
	private Map<Object, Object[]> childrenCache = new HashMap<Object, Object[]>();
	private Map<Object, Object> parents = new HashMap<Object, Object>();

	public SchemaViewContentProvider() {
	}
	
	public SchemaViewContentProvider(int maxDepth) {
		if (maxDepth > 0) {
			this.maxDepth = maxDepth;
			depths = new HashMap<Object, Integer>();
		}
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput != null) {
			if (newInput instanceof XmlSchemaCollection) {
				childrenCache.clear();
				collection = (XmlSchemaCollection) newInput;
			}
			if (maxDepth > 0) {
				depths.clear();
				depths.put(newInput, 0);
			}
		}
	}

	public Object[] getElements(Object object) {
		return getChildren(object);
	}

	public Object[] getChildren(final Object object) {
		Object[] res = childrenCache.get(object);
		if (res == null) {
			if (object instanceof Root) {
				res = new Object[]{ ((Root) object).get()};
			} else if (object instanceof XmlSchemaCollection) {
				XmlSchemaCollection collection = (XmlSchemaCollection) object;
				XmlSchema[] schemas = collection.getXmlSchemas();
				
				// sort the array to set our dynamic schema at the first position, and XSD schema at the end
				Arrays.sort(schemas, new Comparator<XmlSchema>() {
					public int compare(XmlSchema o1, XmlSchema o2) {
						if (Constants.URI_2001_SCHEMA_XSD.equals(o1.getTargetNamespace())) {
							return 1;
						} else if (Constants.URI_2001_SCHEMA_XSD.equals(o2.getTargetNamespace())) {
							return -1;
						} else {
							return SchemaMeta.isDynamic(o1) ? -1 : SchemaMeta.isDynamic(o2) ? 1 :
									o1.getTargetNamespace().compareTo(o2.getTargetNamespace());
						}
					}
				});
				
				res = schemas;
			} else if (object instanceof XmlSchemaObject) {
				final List<XmlSchemaObject> children = new LinkedList<XmlSchemaObject>();
				final XmlSchemaObject xso = (XmlSchemaObject) object;
				try {
	
					new XmlSchemaWalker.XmlSchemaWalkerWatcher() {
						@Override
						protected boolean on(XmlSchemaObject subObject) {
							if (object == subObject) {
								return true;
							} else if (subObject != null) {
								filter(xso, children, subObject);
							}
							return false;
						}
					}.init(xso, true, true);
	
				} catch (Exception e) {
					e.printStackTrace();
				}
	
				if (object instanceof XmlSchema) {
					List<XmlSchemaExternal> directives = new LinkedList<XmlSchemaExternal>();
					List<XmlSchemaAttribute> attributes = new LinkedList<XmlSchemaAttribute>();
					List<XmlSchemaElement> elements = new LinkedList<XmlSchemaElement>();
					List<XmlSchemaGroup> groups = new LinkedList<XmlSchemaGroup>();
					List<XmlSchemaType> types = new LinkedList<XmlSchemaType>();
					
					for (XmlSchemaObject child : children) {
						if (child instanceof XmlSchemaExternal) {
							directives.add((XmlSchemaExternal) child);
						} else if (child instanceof XmlSchemaAttribute) {
							attributes.add((XmlSchemaAttribute) child);
						} else if (child instanceof XmlSchemaElement) {
							elements.add((XmlSchemaElement) child);
						} else if (child instanceof XmlSchemaGroup) {
							groups.add((XmlSchemaGroup) child);
						} else if (child instanceof XmlSchemaType) {
							types.add((XmlSchemaType) child);
						}
					}
	
					List<NamedList> folders = new ArrayList<NamedList>(5);
					if (directives.size() > 0) {
						folders.add(new NamedList("Directives", directives));
					}
					if (attributes.size() > 0) {
						folders.add(new NamedList("Attributes", attributes));
					}
					if (elements.size() > 0) {
						folders.add(new NamedList("Elements", elements));
					}
					if (groups.size() > 0) {
						folders.add(new NamedList("Groups", groups));
					}
					if (types.size() > 0) {
						Collections.sort(types, new Comparator<XmlSchemaType>() {
							public int compare(XmlSchemaType o1, XmlSchemaType o2) {
								return o1.getName().compareTo(o2.getName());
							}
						});
						folders.add(new NamedList("Types", types));
					}
					res = folders.toArray();
				} else {
					res = children.toArray();
				}
			} else if (object instanceof NamedList) {
				res = ((NamedList) object).getList().toArray();
			} else {
				res = new Object[0];
			}
			if (depths != null) {
				for (Object child : res) {
					depths.put(child, depths.get(object) + 1);
				}
			}
			childrenCache.put(object, res);
		}
		for (Object o : res) {
			parents.put(o, object);
		}
		return res;
	}

	public Object getParent(Object object) {
		return parents.get(object);
	}

	public boolean hasChildren(final Object object) {
		boolean ret = depths == null || depths.get(object) < maxDepth;
		if (ret) {
			ret = getChildren(object).length > 0;
		}
		return ret;
	}
	
	protected void filter(XmlSchemaObject xso, List<XmlSchemaObject> children, XmlSchemaObject subObject) {
		if (xso instanceof XmlSchema || (!(subObject instanceof XmlSchemaSimpleType) || ((XmlSchemaSimpleType) subObject).getName() == null)) {
			children.add(subObject);
		}
	}
}
