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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.engine.util;

import java.util.Iterator;
import java.util.LinkedHashMap;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAll;
import org.apache.ws.commons.schema.XmlSchemaAnnotated;
import org.apache.ws.commons.schema.XmlSchemaAnnotation;
import org.apache.ws.commons.schema.XmlSchemaAny;
import org.apache.ws.commons.schema.XmlSchemaAnyAttribute;
import org.apache.ws.commons.schema.XmlSchemaAppInfo;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaAttributeGroup;
import org.apache.ws.commons.schema.XmlSchemaAttributeGroupRef;
import org.apache.ws.commons.schema.XmlSchemaChoice;
import org.apache.ws.commons.schema.XmlSchemaComplexContent;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexContentRestriction;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaContent;
import org.apache.ws.commons.schema.XmlSchemaContentModel;
import org.apache.ws.commons.schema.XmlSchemaDocumentation;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaFacet;
import org.apache.ws.commons.schema.XmlSchemaGroup;
import org.apache.ws.commons.schema.XmlSchemaGroupBase;
import org.apache.ws.commons.schema.XmlSchemaGroupRef;
import org.apache.ws.commons.schema.XmlSchemaIdentityConstraint;
import org.apache.ws.commons.schema.XmlSchemaImport;
import org.apache.ws.commons.schema.XmlSchemaInclude;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaObjectCollection;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaRedefine;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSimpleContent;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentExtension;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentRestriction;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeContent;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeList;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeUnion;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.XmlSchemaXPath;

import com.twinsoft.convertigo.engine.enums.SchemaMeta;

public class XmlSchemaWalker {
	protected boolean deep = true;
	protected boolean deepExternal = true;
	
	static public class XmlSchemaWalkerWatcher extends XmlSchemaWalker {
		
		public void init(XmlSchemaObject obj) {
			init(obj, false, false);
		}
		
		public void init(XmlSchemaObject obj, boolean deep, boolean deepExternal) {
			this.deep = deep;
			this.deepExternal = deepExternal;
			walk(SchemaMeta.getSchema(obj), obj);
		}
		
		protected boolean on(XmlSchemaObject obj) {
			return true;
		}

		@Override
		protected void walk(XmlSchema xmlSchema) {
			if (on(xmlSchema)) {
				super.walk(xmlSchema);
			}
		}
		
		@Override
		protected void walkElement(XmlSchema xmlSchema, XmlSchemaElement obj) {
			if (on(obj)) {
				super.walkElement(xmlSchema, obj);
			}
		}

		@Override
		protected void walkSimpleType(XmlSchema xmlSchema, XmlSchemaSimpleType obj) {
			if (on(obj)) {
				super.walkSimpleType(xmlSchema, obj);
			}
		}

		@Override
		protected void walkSimpleTypeRestriction(XmlSchema xmlSchema, XmlSchemaSimpleTypeRestriction obj) {
			if (on(obj)) {
				super.walkSimpleTypeRestriction(xmlSchema, obj);
			}
		}

		@Override
		protected void walkSimpleTypeList(XmlSchema xmlSchema, XmlSchemaSimpleTypeList obj) {
			if (on(obj)) {
				super.walkSimpleTypeList(xmlSchema, obj);
			}
		}

		@Override
		protected void walkSimpleTypeUnion(XmlSchema xmlSchema, XmlSchemaSimpleTypeUnion obj) {
			if (on(obj)) {
				super.walkSimpleTypeUnion(xmlSchema, obj);
			}
		}

		@Override
		protected void walkComplexType(XmlSchema xmlSchema, XmlSchemaComplexType obj) {
			if (on(obj)) {
				super.walkComplexType(xmlSchema, obj);
			}
		}

		@Override
		protected void walkGroupRef(XmlSchema xmlSchema, XmlSchemaGroupRef obj) {
			if (on(obj)) {
				super.walkGroupRef(xmlSchema, obj);
			}
		}

		@Override
		protected void walkIdentityConstraint(XmlSchema xmlSchema, XmlSchemaIdentityConstraint obj) {
			if (on(obj)) {
				super.walkIdentityConstraint(xmlSchema, obj);
			}
		}

		@Override
		protected void walkImport(XmlSchema xmlSchema, XmlSchemaImport obj) {
			if (on(obj)) {
				super.walkImport(xmlSchema, obj);
			}
		}

		@Override
		protected void walkInclude(XmlSchema xmlSchema, XmlSchemaInclude obj) {
			if (on(obj)) {
				super.walkInclude(xmlSchema, obj);
			}
		}

		@Override
		protected void walkAll(XmlSchema xmlSchema, XmlSchemaAll obj) {
			if (on(obj)) {
				super.walkAll(xmlSchema, obj);
			}
		}

		@Override
		protected void walkAnnotation(XmlSchema xmlSchema, XmlSchemaAnnotation obj) {
			if (on(obj)) {
				super.walkAnnotation(xmlSchema, obj);
			}
		}

		@Override
		protected void walkDocumentation(XmlSchema xmlSchema, XmlSchemaDocumentation obj) {
			if (on(obj)) {
				super.walkDocumentation(xmlSchema, obj);
			}
		}

		@Override
		protected void walkAppInfo(XmlSchema xmlSchema, XmlSchemaAppInfo obj) {
			if (on(obj)) {
				super.walkAppInfo(xmlSchema, obj);
			}
		}

		@Override
		protected void walkChoice(XmlSchema xmlSchema, XmlSchemaChoice obj) {
			if (on(obj)) {
				super.walkChoice(xmlSchema, obj);
			}
		}

		@Override
		protected void walkSequence(XmlSchema xmlSchema, XmlSchemaSequence obj) {
			if (on(obj)) {
				super.walkSequence(xmlSchema, obj);
			}
		}

		@Override
		protected void walkAny(XmlSchema xmlSchema, XmlSchemaAny obj) {
			if (on(obj)) {
				super.walkAny(xmlSchema, obj);
			}
		}

		@Override
		protected void walkSimpleContent(XmlSchema xmlSchema, XmlSchemaSimpleContent obj) {
			if (on(obj)) {
				super.walkSimpleContent(xmlSchema, obj);
			}
		}

		@Override
		protected void walkSimpleContentRestriction(XmlSchema xmlSchema, XmlSchemaSimpleContentRestriction obj) {
			if (on(obj)) {
				super.walkSimpleContentRestriction(xmlSchema, obj);
			}
		}

		@Override
		protected void walkFacet(XmlSchema xmlSchema, XmlSchemaFacet obj) {
			if (on(obj)) {
				super.walkFacet(xmlSchema, obj);
			}
		}

		@Override
		protected void walkField(XmlSchema xmlSchema, XmlSchemaXPath obj) {
			if (on(obj)) {
				super.walkField(xmlSchema, obj);
			}
		}

		@Override
		protected void walkAttributeGroupRef(XmlSchema xmlSchema, XmlSchemaAttributeGroupRef obj) {
			if (on(obj)) {
				super.walkAttributeGroupRef(xmlSchema, obj);
			}
		}

		@Override
		protected void walkSimpleContentExtension(XmlSchema xmlSchema, XmlSchemaSimpleContentExtension obj) {
			if (on(obj)) {
				super.walkSimpleContentExtension(xmlSchema, obj);
			}
		}

		@Override
		protected void walkAnyAttribute(XmlSchema xmlSchema, XmlSchemaAnyAttribute obj) {
			if (on(obj)) {
				super.walkAnyAttribute(xmlSchema, obj);
			}
		}

		@Override
		protected void walkComplexContent(XmlSchema xmlSchema, XmlSchemaComplexContent obj) {
			if (on(obj)) {
				super.walkComplexContent(xmlSchema, obj);
			}
		}

		@Override
		protected void walkComplexContentExtension(XmlSchema xmlSchema, XmlSchemaComplexContentExtension obj) {
			if (on(obj)) {
				super.walkComplexContentExtension(xmlSchema, obj);
			}
		}

		@Override
		protected void walkComplexContentRestriction(XmlSchema xmlSchema, XmlSchemaComplexContentRestriction obj) {
			if (on(obj)) {
				super.walkComplexContentRestriction(xmlSchema, obj);
			}
		}

		@Override
		protected void walkGroup(XmlSchema xmlSchema, XmlSchemaGroup obj) {
			if (on(obj)) {
				super.walkGroup(xmlSchema, obj);
			}
		}

		@Override
		protected void walkAttributeGroup(XmlSchema xmlSchema,
				XmlSchemaAttributeGroup obj) {
			if (on(obj)) {
				super.walkAttributeGroup(xmlSchema, obj);
			}
		}

		@Override
		protected void walkAttribute(XmlSchema xmlSchema, XmlSchemaAttribute obj) {
			if (on(obj)) {
				super.walkAttribute(xmlSchema, obj);
			}
		}

		@Override
		protected void walkRedefine(XmlSchema xmlSchema, XmlSchemaRedefine obj) {
			if (on(obj)) {
				super.walkRedefine(xmlSchema, obj);
			}
		}

	}
	
	public static XmlSchemaWalker newDependencyWalker(LinkedHashMap<QName, XmlSchemaObject> linkedMap, boolean deep, boolean deepExternal) {
		final LinkedHashMap<QName, XmlSchemaObject> map = linkedMap;
		return new XmlSchemaWalker(deep, deepExternal) {
			private LinkedHashMap<QName, XmlSchemaObject> linkedMap = map;
			
			@Override
			public void walkByTypeName(XmlSchema xmlSchema, QName qname) {
				XmlSchemaType obj = SchemaMeta.getCollection(xmlSchema).getTypeByQName(qname);
				if (obj != null) {
					if (!linkedMap.containsKey(qname)) {
						linkedMap.put(qname, obj);
						super.walkByTypeName(xmlSchema, qname);
					}
				}
			}

			@Override
			public void walkByElementRef(XmlSchema xmlSchema, QName qname) {
				XmlSchemaElement obj = SchemaMeta.getCollection(xmlSchema).getElementByQName(qname);
				if (obj != null) {
					if (!linkedMap.containsKey(qname)) {
						linkedMap.put(qname, obj);
						super.walkByElementRef(xmlSchema, qname);
					}
				}
			}

			@Override
			public void walkByAttributeGroupRef(XmlSchema xmlSchema, QName qname) {
				XmlSchema schema = SchemaMeta.getCollection(xmlSchema).schemaForNamespace(qname.getNamespaceURI());
				XmlSchemaAttributeGroup obj = (XmlSchemaAttributeGroup) schema.getAttributeGroups().getItem(qname);
				if (obj != null) {
					if (!linkedMap.containsKey(qname)) {
						linkedMap.put(qname, obj);
						super.walkByAttributeGroupRef(xmlSchema, qname);
					}
				}
			}

			@Override
			public void walkByAttributeRef(XmlSchema xmlSchema, QName qname) {
				XmlSchemaAttribute obj = SchemaMeta.getCollection(xmlSchema).getAttributeByQName(qname);
				if (obj != null) {
					if (!linkedMap.containsKey(qname)) {
						linkedMap.put(qname, obj);
						super.walkByAttributeRef(xmlSchema, qname);
					}
				}
			}

			@Override
			public void walkByGroupRef(XmlSchema xmlSchema, QName qname) {
				XmlSchema schema = SchemaMeta.getCollection(xmlSchema).schemaForNamespace(qname.getNamespaceURI());
				XmlSchemaGroup obj = (XmlSchemaGroup) schema.getGroups().getItem(qname);
				if (obj != null) {
					if (!linkedMap.containsKey(qname)) {
						linkedMap.put(qname, obj);
						super.walkByGroupRef(xmlSchema, qname);
					}
				}
			}
		};
	}
	
	public XmlSchemaWalker() {
		
	}
	
	public XmlSchemaWalker(boolean deep, boolean deepExternal) {
		this.deep = deep;
		this.deepExternal = deepExternal;
	}
	
	protected void walk(XmlSchema xmlSchema) {
		XmlSchemaObjectCollection items = xmlSchema.getItems();
		for (Iterator<XmlSchemaObject> i = GenericUtils.cast(items.getIterator()); i.hasNext(); ) {
			XmlSchemaObject obj = i.next();
			if (obj instanceof XmlSchemaInclude) {
				walkInclude(xmlSchema, (XmlSchemaInclude) obj);
			} else if (obj instanceof XmlSchemaImport) {
				walkImport(xmlSchema, (XmlSchemaImport) obj);
			}
		}
		
		for (Iterator<XmlSchemaType> i = GenericUtils.cast(xmlSchema.getSchemaTypes().getValues()); i.hasNext(); ) {
			walk(xmlSchema, i.next());
		}
		
		for (Iterator<XmlSchemaObject> i = GenericUtils.cast(items.getIterator()); i.hasNext(); ) {
			XmlSchemaObject obj = i.next();
			if (!(obj instanceof XmlSchemaInclude || obj instanceof XmlSchemaImport || obj instanceof XmlSchemaType)) {
				walk(xmlSchema, obj);
			}
		}
	}
	
	private boolean isExternal(XmlSchema xmlSchema, QName qname) {
		return !(qname.getNamespaceURI().equals(xmlSchema.getTargetNamespace()));
	}
	
	private void doWalk(XmlSchema xmlSchema, QName qname, XmlSchemaObject obj) {
		if (obj != null) {
			boolean isExternal = isExternal(xmlSchema, qname);
			if (!isExternal || (isExternal && deepExternal)) {
				walk(xmlSchema, obj);
			}
		}
	}
	
	public void walkByTypeName(XmlSchema xmlSchema, QName qname) {
		doWalk(xmlSchema, qname, SchemaMeta.getCollection(xmlSchema).getTypeByQName(qname));
	}
	public void walkByElementRef(XmlSchema xmlSchema, QName qname) {
		doWalk(xmlSchema, qname, SchemaMeta.getCollection(xmlSchema).getElementByQName(qname));
	}
	public void walkByAttributeRef(XmlSchema xmlSchema, QName qname) {
		doWalk(xmlSchema, qname, SchemaMeta.getCollection(xmlSchema).getAttributeByQName(qname));
	}
	public void walkByAttributeGroupRef(XmlSchema xmlSchema, QName qname) {
		//doWalk(xmlSchema, qname, (XmlSchemaAttributeGroup)xmlSchema.getAttributeGroups().getItem(qname));
		XmlSchema schema = SchemaMeta.getCollection(xmlSchema).schemaForNamespace(qname.getNamespaceURI());
		doWalk(xmlSchema, qname, (XmlSchemaAttributeGroup)schema.getAttributeGroups().getItem(qname));
	}
	public void walkByGroupRef(XmlSchema xmlSchema, QName qname) {
		//doWalk(xmlSchema, qname, (XmlSchemaGroup)xmlSchema.getGroups().getItem(qname));
		XmlSchema schema = SchemaMeta.getCollection(xmlSchema).schemaForNamespace(qname.getNamespaceURI());
		doWalk(xmlSchema, qname, (XmlSchemaGroup)schema.getGroups().getItem(qname));
	}
	
	protected void walk(XmlSchema xmlSchema, XmlSchemaObject obj) {
        if (obj instanceof XmlSchema) {
        	walk((XmlSchema) obj);
        } else if (obj instanceof XmlSchemaElement) {
        	walkElement(xmlSchema, (XmlSchemaElement) obj);
        } else if (obj instanceof XmlSchemaSimpleType) {
        	walkSimpleType(xmlSchema, (XmlSchemaSimpleType) obj);
        } else if (obj instanceof XmlSchemaSimpleTypeRestriction) {
        	walkSimpleTypeRestriction(xmlSchema, (XmlSchemaSimpleTypeRestriction) obj);
        } else if (obj instanceof XmlSchemaSimpleTypeList) {
        	walkSimpleTypeList(xmlSchema, (XmlSchemaSimpleTypeList) obj);
        } else if (obj instanceof XmlSchemaSimpleTypeUnion) {
        	walkSimpleTypeUnion(xmlSchema, (XmlSchemaSimpleTypeUnion) obj);
        } else if (obj instanceof XmlSchemaComplexType) {
        	walkComplexType(xmlSchema, (XmlSchemaComplexType) obj);
        } else if (obj instanceof XmlSchemaGroupRef) {
        	walkGroupRef(xmlSchema, (XmlSchemaGroupRef) obj);
        } else if (obj instanceof XmlSchemaIdentityConstraint) {
        	walkIdentityConstraint(xmlSchema, (XmlSchemaIdentityConstraint) obj);
        } else if (obj instanceof XmlSchemaImport) {
        	walkImport(xmlSchema, (XmlSchemaImport) obj);
        } else if (obj instanceof XmlSchemaInclude) {
        	walkInclude(xmlSchema, (XmlSchemaInclude) obj);
        } else if (obj instanceof XmlSchemaAll) {
        	walkAll(xmlSchema, (XmlSchemaAll) obj);
        } else if (obj instanceof XmlSchemaAnnotation) {
        	walkAnnotation(xmlSchema, (XmlSchemaAnnotation) obj);
        } else if (obj instanceof XmlSchemaDocumentation) {
        	walkDocumentation(xmlSchema, (XmlSchemaDocumentation) obj);
        } else if (obj instanceof XmlSchemaAppInfo) {
        	walkAppInfo(xmlSchema, (XmlSchemaAppInfo) obj);
        } else if (obj instanceof XmlSchemaChoice) {
        	walkChoice(xmlSchema, (XmlSchemaChoice) obj);
        } else if (obj instanceof XmlSchemaSequence) {
        	walkSequence(xmlSchema, (XmlSchemaSequence) obj);
        } else if (obj instanceof XmlSchemaAny) {
        	walkAny(xmlSchema, (XmlSchemaAny) obj);
        } else if (obj instanceof XmlSchemaSimpleContent) {
        	walkSimpleContent(xmlSchema, (XmlSchemaSimpleContent) obj);
        } else if (obj instanceof XmlSchemaSimpleContentRestriction) {
        	walkSimpleContentRestriction(xmlSchema, (XmlSchemaSimpleContentRestriction) obj);
        } else if (obj instanceof XmlSchemaFacet) {
        	walkFacet(xmlSchema, (XmlSchemaFacet) obj);
        } else if (obj instanceof XmlSchemaXPath) {
        	walkField(xmlSchema, (XmlSchemaXPath) obj);
        } else if (obj instanceof XmlSchemaAttributeGroupRef) {
        	walkAttributeGroupRef(xmlSchema, (XmlSchemaAttributeGroupRef) obj);
        } else if (obj instanceof XmlSchemaSimpleContentExtension) {
        	walkSimpleContentExtension(xmlSchema, (XmlSchemaSimpleContentExtension) obj);
        } else if (obj instanceof XmlSchemaAnyAttribute) {
        	walkAnyAttribute(xmlSchema, (XmlSchemaAnyAttribute) obj);
        } else if (obj instanceof XmlSchemaComplexContent) {
        	walkComplexContent(xmlSchema, (XmlSchemaComplexContent) obj);
        } else if (obj instanceof XmlSchemaComplexContentExtension) {
        	walkComplexContentExtension(xmlSchema, (XmlSchemaComplexContentExtension) obj);
        } else if (obj instanceof XmlSchemaComplexContentRestriction) {
        	walkComplexContentRestriction(xmlSchema, (XmlSchemaComplexContentRestriction) obj);
        } else if (obj instanceof XmlSchemaGroup) {
        	walkGroup(xmlSchema, (XmlSchemaGroup) obj);
        } else if (obj instanceof XmlSchemaAttributeGroup) {
        	walkAttributeGroup(xmlSchema, (XmlSchemaAttributeGroup) obj);
        } else if (obj instanceof XmlSchemaAttribute) {
        	walkAttribute(xmlSchema, (XmlSchemaAttribute) obj);
        } else if (obj instanceof XmlSchemaRedefine) {
        	walkRedefine(xmlSchema, (XmlSchemaRedefine) obj);
        }
	}
	
	protected void walkAnnotated(XmlSchema xmlSchema, XmlSchemaAnnotated obj) {
		XmlSchemaAnnotation annotation = obj.getAnnotation();
		if (annotation != null) {
			walkAnnotation(xmlSchema, annotation);
		}
	}
	
	protected void walkElement(XmlSchema xmlSchema, XmlSchemaElement obj) {
		walkAnnotated(xmlSchema, obj);
		QName refName = obj.getRefName();
		QName typeName = obj.getSchemaTypeName();
		XmlSchemaType xmlSchemaType = obj.getSchemaType();
		if (refName != null) {
			if (deep) {
				walkByElementRef(xmlSchema, refName);
			}
		} else if (typeName != null) {
			if (deep) {
				walkByTypeName(xmlSchema, typeName);
			}
		} else if (xmlSchemaType != null) {
            if (xmlSchemaType instanceof XmlSchemaComplexType) {
            	walkComplexType(xmlSchema, (XmlSchemaComplexType)xmlSchemaType);
            } else if (xmlSchemaType instanceof XmlSchemaSimpleType) {
            	walkSimpleType(xmlSchema, (XmlSchemaSimpleType)xmlSchemaType);
            }
		}
	}
	
	protected void walkSimpleType(XmlSchema xmlSchema, XmlSchemaSimpleType obj) {
		walkAnnotated(xmlSchema, obj);
		XmlSchemaSimpleTypeContent simpleTypeContent = obj.getContent();
		if (simpleTypeContent != null) {
	        if (simpleTypeContent instanceof XmlSchemaSimpleTypeRestriction) {
	        	walkSimpleTypeRestriction(xmlSchema, (XmlSchemaSimpleTypeRestriction)simpleTypeContent);
	        } else if (simpleTypeContent instanceof XmlSchemaSimpleTypeList) {
	        	walkSimpleTypeList(xmlSchema, (XmlSchemaSimpleTypeList)simpleTypeContent);
	        } else if (simpleTypeContent instanceof XmlSchemaSimpleTypeUnion) {
	        	walkSimpleTypeUnion(xmlSchema, (XmlSchemaSimpleTypeUnion)simpleTypeContent);
	        }
		}
	}
	
	protected void walkSimpleTypeRestriction(XmlSchema xmlSchema, XmlSchemaSimpleTypeRestriction obj) {
		walkAnnotated(xmlSchema, obj);
		QName qname = obj.getBaseTypeName();
		if ((qname != null) && (deep)) {
			walkByTypeName(xmlSchema, qname);
		} else {
			XmlSchemaSimpleType type = obj.getBaseType();
			if (type != null) {
				walkSimpleType(xmlSchema, type);
			}
		}
	}
	
	protected void walkSimpleTypeList(XmlSchema xmlSchema, XmlSchemaSimpleTypeList obj) {
		walkAnnotated(xmlSchema, obj);		
	}
	
	protected void walkSimpleTypeUnion(XmlSchema xmlSchema, XmlSchemaSimpleTypeUnion obj) {
		walkAnnotated(xmlSchema, obj);		
	}
	
	protected void walkComplexType(XmlSchema xmlSchema, XmlSchemaComplexType obj) {
		walkAnnotated(xmlSchema, obj);
		XmlSchemaObjectCollection attributes = obj.getAttributes();
        for (int i = 0; i < attributes.getCount(); i++) {
            XmlSchemaObject attribute = attributes.getItem(i);
            if (attribute instanceof XmlSchemaAttribute) {
            	walkAttribute(xmlSchema, (XmlSchemaAttribute) attribute);
            } else if (attribute instanceof XmlSchemaAttributeGroupRef) {
            	walkAttributeGroupRef(xmlSchema, (XmlSchemaAttributeGroupRef) attribute);
            }
        }
		XmlSchemaContentModel xmlSchemaContentModel  = obj.getContentModel();
		XmlSchemaParticle xmlSchemaParticle = obj.getParticle();
		if (xmlSchemaContentModel != null) {
	        if (xmlSchemaContentModel instanceof XmlSchemaSimpleContent) {
	        	walkSimpleContent(xmlSchema, (XmlSchemaSimpleContent)xmlSchemaContentModel);
	        } else if (xmlSchemaContentModel instanceof XmlSchemaComplexContent) {
	        	walkComplexContent(xmlSchema, (XmlSchemaComplexContent)xmlSchemaContentModel);
	        }
		} else if (xmlSchemaParticle != null) {
	        if (xmlSchemaParticle instanceof XmlSchemaSequence) {
	        	walkSequence(xmlSchema, (XmlSchemaSequence)xmlSchemaParticle);
	        } else if (xmlSchemaParticle instanceof XmlSchemaChoice) {
	        	walkChoice(xmlSchema, (XmlSchemaChoice)xmlSchemaParticle);
	        } else if (xmlSchemaParticle instanceof XmlSchemaAll) {
	        	walkAll(xmlSchema, (XmlSchemaAll)xmlSchemaParticle);
	        } else if (xmlSchemaParticle instanceof XmlSchemaGroupRef) {
	        	walkGroupRef(xmlSchema, (XmlSchemaGroupRef)xmlSchemaParticle);
	        }
		}
	}
	
	protected void walkGroupRef(XmlSchema xmlSchema, XmlSchemaGroupRef obj) {
		walkAnnotated(xmlSchema, obj);
		QName refName = obj.getRefName();
		if ((refName != null) && deep) {
			walkByGroupRef(xmlSchema, refName);
		}
		
		XmlSchemaGroupBase xmlSchemaGroupBase = obj.getParticle();
		if (xmlSchemaGroupBase != null) {
            if (xmlSchemaGroupBase instanceof XmlSchemaChoice) {
                walkChoice(xmlSchema, (XmlSchemaChoice)xmlSchemaGroupBase);
            } else if (xmlSchemaGroupBase instanceof XmlSchemaSequence) {
                walkSequence(xmlSchema, (XmlSchemaSequence)xmlSchemaGroupBase);
            } else if (xmlSchemaGroupBase instanceof XmlSchemaAll) {
                walkAll(xmlSchema, (XmlSchemaAll)xmlSchemaGroupBase);
            }
		}
	}

	protected void walkIdentityConstraint(XmlSchema xmlSchema, XmlSchemaIdentityConstraint obj) {
		walkAnnotated(xmlSchema, obj);		
	}
	
	protected void walkImport(XmlSchema xmlSchema, XmlSchemaImport obj) {
		walkAnnotated(xmlSchema, obj);
		XmlSchema imported = obj.getSchema();
		if ((imported != null) && deep) {
			walk(imported);
		}
	}
	
	protected void walkInclude(XmlSchema xmlSchema, XmlSchemaInclude obj) {
		walkAnnotated(xmlSchema, obj);
		XmlSchema included = obj.getSchema();
		if ((included != null) && deep) {
			walk(included);
		}
	}

	protected void walkAll(XmlSchema xmlSchema, XmlSchemaAll obj) {
		walkAnnotated(xmlSchema, obj);
        XmlSchemaObjectCollection children = obj.getItems();
        for (int i = 0; i < children.getCount(); i++) {
            XmlSchemaObject child = children.getItem(i);
            if (child instanceof XmlSchemaElement) {
            	walkElement(xmlSchema, (XmlSchemaElement)child);
            }
        }
	}

	protected void walkAnnotation(XmlSchema xmlSchema, XmlSchemaAnnotation obj) {
        XmlSchemaObjectCollection contents = obj.getItems();
        for (int i = 0; i < contents.getCount(); i++) {
            XmlSchemaObject item = contents.getItem(i);
            if (item instanceof XmlSchemaAppInfo) {
            	walkAppInfo(xmlSchema, (XmlSchemaAppInfo)item);
            } else if (item instanceof XmlSchemaDocumentation) {
            	walkDocumentation(xmlSchema, (XmlSchemaDocumentation)item);
            }
        }
	}
	
	protected void walkDocumentation(XmlSchema xmlSchema, XmlSchemaDocumentation item) {
		// TODO Auto-generated method stub
		
	}

	protected void walkAppInfo(XmlSchema xmlSchema, XmlSchemaAppInfo item) {
		// TODO Auto-generated method stub
		
	}

	protected void walkChoice(XmlSchema xmlSchema, XmlSchemaChoice obj) {
		walkAnnotated(xmlSchema, obj);
        XmlSchemaObjectCollection children = obj.getItems();
        for (int i = 0; i < children.getCount(); i++) {
            XmlSchemaObject child = children.getItem(i);
            if (child instanceof XmlSchemaElement) {
            	walkElement(xmlSchema, (XmlSchemaElement)child);
            } else if (child instanceof XmlSchemaGroupRef) {
            	walkGroupRef(xmlSchema, (XmlSchemaGroupRef)child);
            } else if (child instanceof XmlSchemaChoice) {
            	walkChoice(xmlSchema, (XmlSchemaChoice)child);
            } else if (child instanceof XmlSchemaSequence) {
            	walkSequence(xmlSchema, (XmlSchemaSequence)child);
            } else if (child instanceof XmlSchemaAny) {
            	walkAny(xmlSchema, (XmlSchemaAny)child);
            }
        }
	}

	protected void walkSequence(XmlSchema xmlSchema, XmlSchemaSequence obj) {
		walkAnnotated(xmlSchema, obj);
        XmlSchemaObjectCollection children = obj.getItems();
        for (int i = 0; i < children.getCount(); i++) {
            XmlSchemaObject child = children.getItem(i);
            if (child instanceof XmlSchemaElement) {
            	walkElement(xmlSchema, (XmlSchemaElement)child);
            } else if (child instanceof XmlSchemaGroupRef) {
            	walkGroupRef(xmlSchema, (XmlSchemaGroupRef)child);
            } else if (child instanceof XmlSchemaChoice) {
            	walkChoice(xmlSchema, (XmlSchemaChoice)child);
            } else if (child instanceof XmlSchemaSequence) {
            	walkSequence(xmlSchema, (XmlSchemaSequence)child);
            } else if (child instanceof XmlSchemaAny) {
            	walkAny(xmlSchema, (XmlSchemaAny)child);
            }
        }
	}

	protected void walkAny(XmlSchema xmlSchema, XmlSchemaAny obj) {
		walkAnnotated(xmlSchema, obj);
		// TODO Auto-generated method stub
		
	}

	protected void walkSimpleContent(XmlSchema xmlSchema, XmlSchemaSimpleContent obj) {
		walkAnnotated(xmlSchema, obj);
		XmlSchemaContent xmlSchemaContent = obj.getContent();
        if (xmlSchemaContent instanceof XmlSchemaSimpleContentRestriction) {
        	walkSimpleContentRestriction(xmlSchema, (XmlSchemaSimpleContentRestriction)xmlSchemaContent);
        } else if (xmlSchemaContent instanceof XmlSchemaSimpleContentExtension) {
        	walkSimpleContentExtension(xmlSchema, (XmlSchemaSimpleContentExtension)xmlSchemaContent);
        }
	}
	protected void walkSimpleContentRestriction(XmlSchema xmlSchema, XmlSchemaSimpleContentRestriction obj) {
		walkAnnotated(xmlSchema, obj);
		QName baseTypeName = obj.getBaseTypeName();
		if ((baseTypeName != null) && deep) {
			walkByTypeName(xmlSchema, baseTypeName);
		}
		
		XmlSchemaObjectCollection attributes = obj.getAttributes();
        for (int i = 0; i < attributes.getCount(); i++) {
            XmlSchemaObject attribute = attributes.getItem(i);
            if (attribute instanceof XmlSchemaAttribute) {
            	walkAttribute(xmlSchema, (XmlSchemaAttribute)attribute);
            } else if (attribute instanceof XmlSchemaAttributeGroupRef) {
            	walkAttributeGroupRef(xmlSchema, (XmlSchemaAttributeGroupRef)attribute);
            }
        }
		XmlSchemaSimpleType xmlSchemaSimpleType = obj.getBaseType();
		if (xmlSchemaSimpleType != null) {
			walkSimpleType(xmlSchema, xmlSchemaSimpleType);
		}
		XmlSchemaAnyAttribute xmlSchemaAnyAttribute = obj.getAnyAttribute();
		if (xmlSchemaAnyAttribute != null) {
			walkAnyAttribute(xmlSchema, xmlSchemaAnyAttribute);
		}
        XmlSchemaObjectCollection facets = obj.getFacets();
        for (int i = 0; i < facets.getCount(); i++) {
        	XmlSchemaObject facet = facets.getItem(i);
        	walkFacet(xmlSchema, (XmlSchemaFacet)facet);
        }
	}
	
	protected void walkFacet(XmlSchema xmlSchema, XmlSchemaFacet obj) {
		walkAnnotated(xmlSchema, obj);
		// TODO Auto-generated method stub
		
	}

	protected void walkField(XmlSchema xmlSchema, XmlSchemaXPath obj) {
		walkAnnotated(xmlSchema, obj);
		
	}
	
	protected void walkAttributeGroupRef(XmlSchema xmlSchema, XmlSchemaAttributeGroupRef obj) {
		walkAnnotated(xmlSchema, obj);
		QName refName = obj.getRefName();
		if ((refName != null) && deep) {
			walkByGroupRef(xmlSchema, refName);
		}
	}

	protected void walkSimpleContentExtension(XmlSchema xmlSchema, XmlSchemaSimpleContentExtension obj) {
		walkAnnotated(xmlSchema, obj);
		QName baseTypeName = obj.getBaseTypeName();
		if ((baseTypeName != null) && deep) {
			walkByTypeName(xmlSchema, baseTypeName);
		}
		
		XmlSchemaObjectCollection attributes = obj.getAttributes();
        for (int i = 0; i < attributes.getCount(); i++) {
            XmlSchemaObject attribute = attributes.getItem(i);
            if (attribute instanceof XmlSchemaAttribute) {
            	walkAttribute(xmlSchema, (XmlSchemaAttribute)attribute);
            } else if (attribute instanceof XmlSchemaAttributeGroupRef) {
            	walkAttributeGroupRef(xmlSchema, (XmlSchemaAttributeGroupRef)attribute);
            }
        }
		XmlSchemaAnyAttribute xmlSchemaAnyAttribute = obj.getAnyAttribute();
		if (xmlSchemaAnyAttribute != null) {
			walkAnyAttribute(xmlSchema, xmlSchemaAnyAttribute);
		}
	}
	
	protected void walkAnyAttribute(XmlSchema xmlSchema, XmlSchemaAnyAttribute obj) {
		walkAnnotated(xmlSchema, obj);
		// TODO Auto-generated method stub
		
	}

	protected void walkComplexContent(XmlSchema xmlSchema, XmlSchemaComplexContent obj) {
		walkAnnotated(xmlSchema, obj);
		XmlSchemaContent xmlSchemaContent = obj.getContent();
        if (xmlSchemaContent instanceof XmlSchemaComplexContentRestriction) {
        	walkComplexContentRestriction(xmlSchema, (XmlSchemaComplexContentRestriction)xmlSchemaContent);
        } else if (xmlSchemaContent instanceof XmlSchemaComplexContentExtension) {
        	walkComplexContentExtension(xmlSchema, (XmlSchemaComplexContentExtension)xmlSchemaContent);
        }
	}
	
	protected void walkComplexContentExtension(XmlSchema xmlSchema, XmlSchemaComplexContentExtension obj) {
		walkAnnotated(xmlSchema, obj);
		QName baseTypeName = obj.getBaseTypeName();
		if ((baseTypeName != null) && deep) {
			walkByTypeName(xmlSchema, baseTypeName);
		}
		
		XmlSchemaParticle xmlSchemaParticle = obj.getParticle();
		if (xmlSchemaParticle != null) {
	        if (xmlSchemaParticle instanceof XmlSchemaSequence) {
	        	walkSequence(xmlSchema, (XmlSchemaSequence)xmlSchemaParticle);
	        } else if (xmlSchemaParticle instanceof XmlSchemaChoice) {
	        	walkChoice(xmlSchema, (XmlSchemaChoice)xmlSchemaParticle);
	        } else if (xmlSchemaParticle instanceof XmlSchemaAll) {
	        	walkAll(xmlSchema, (XmlSchemaAll)xmlSchemaParticle);
	        } else if (xmlSchemaParticle instanceof XmlSchemaGroupRef) {
	        	walkGroupRef(xmlSchema, (XmlSchemaGroupRef)xmlSchemaParticle);
	        }
		}
		
		XmlSchemaObjectCollection attributes = obj.getAttributes();
        for (int i = 0; i < attributes.getCount(); i++) {
            XmlSchemaObject attribute = attributes.getItem(i);
            if (attribute instanceof XmlSchemaAttribute) {
            	walkAttribute(xmlSchema, (XmlSchemaAttribute)attribute);
            } else if (attribute instanceof XmlSchemaAttributeGroupRef) {
            	walkAttributeGroupRef(xmlSchema, (XmlSchemaAttributeGroupRef)attribute);
            }
        }
		XmlSchemaAnyAttribute xmlSchemaAnyAttribute = obj.getAnyAttribute();
		if (xmlSchemaAnyAttribute != null) {
			walkAnyAttribute(xmlSchema, xmlSchemaAnyAttribute);
		}
	}

	protected void walkComplexContentRestriction(XmlSchema xmlSchema, XmlSchemaComplexContentRestriction obj) {
		walkAnnotated(xmlSchema, obj);
		QName baseTypeName = obj.getBaseTypeName();
		if ((baseTypeName != null) && deep) {
			walkByTypeName(xmlSchema, baseTypeName);
		}
		
		XmlSchemaParticle xmlSchemaParticle = obj.getParticle();
		if (xmlSchemaParticle != null) {
	        if (xmlSchemaParticle instanceof XmlSchemaSequence) {
	        	walkSequence(xmlSchema, (XmlSchemaSequence)xmlSchemaParticle);
	        } else if (xmlSchemaParticle instanceof XmlSchemaChoice) {
	        	walkChoice(xmlSchema, (XmlSchemaChoice)xmlSchemaParticle);
	        } else if (xmlSchemaParticle instanceof XmlSchemaAll) {
	        	walkAll(xmlSchema, (XmlSchemaAll)xmlSchemaParticle);
	        } else if (xmlSchemaParticle instanceof XmlSchemaGroupRef) {
	        	walkGroupRef(xmlSchema, (XmlSchemaGroupRef)xmlSchemaParticle);
	        }
		}
		
		XmlSchemaObjectCollection attributes = obj.getAttributes();
        for (int i = 0; i < attributes.getCount(); i++) {
            XmlSchemaObject attribute = attributes.getItem(i);
            if (attribute instanceof XmlSchemaAttribute) {
            	walkAttribute(xmlSchema, (XmlSchemaAttribute)attribute);
            } else if (attribute instanceof XmlSchemaAttributeGroupRef) {
            	walkAttributeGroupRef(xmlSchema, (XmlSchemaAttributeGroupRef)attribute);
            }
        }
		XmlSchemaAnyAttribute xmlSchemaAnyAttribute = obj.getAnyAttribute();
		if (xmlSchemaAnyAttribute != null) {
			walkAnyAttribute(xmlSchema, xmlSchemaAnyAttribute);
		}
	}

	protected void walkGroup(XmlSchema xmlSchema, XmlSchemaGroup obj) {
		walkAnnotated(xmlSchema, obj);
		XmlSchemaGroupBase xmlSchemaGroupBase = obj.getParticle();
        if (xmlSchemaGroupBase instanceof XmlSchemaSequence) {
        	walkSequence(xmlSchema, (XmlSchemaSequence)xmlSchemaGroupBase);
        } else if (xmlSchemaGroupBase instanceof XmlSchemaChoice) {
        	walkChoice(xmlSchema, (XmlSchemaChoice)xmlSchemaGroupBase);
        } else if (xmlSchemaGroupBase instanceof XmlSchemaAll) {
        	walkAll(xmlSchema, (XmlSchemaAll)xmlSchemaGroupBase);
        }
	}
	
	protected void walkAttributeGroup(XmlSchema xmlSchema, XmlSchemaAttributeGroup obj) {
		walkAnnotated(xmlSchema, obj);
		XmlSchemaObjectCollection  attributes = obj.getAttributes();
        for (int i = 0; i < attributes.getCount(); i++) {
            XmlSchemaObject attribute = attributes.getItem(i);
            if (attribute instanceof XmlSchemaAttribute) {
            	walkAttribute(xmlSchema, (XmlSchemaAttribute)attribute);
            } else if (attribute instanceof XmlSchemaAttributeGroupRef) {
            	walkAttributeGroupRef(xmlSchema, (XmlSchemaAttributeGroupRef)attribute);
            }
        }
        XmlSchemaAnyAttribute xmlSchemaAnyAttribute = obj.getAnyAttribute();
        if (xmlSchemaAnyAttribute != null) {
        	walkAnyAttribute(xmlSchema, xmlSchemaAnyAttribute);
        }
	}
	
	protected void walkAttribute(XmlSchema xmlSchema, XmlSchemaAttribute obj) {
		walkAnnotated(xmlSchema, obj);
		QName refName = obj.getRefName();
		QName typeName = obj.getSchemaTypeName();
		XmlSchemaSimpleType xmlSchemaSimpleType = obj.getSchemaType();
		
		if ((refName != null) && deep) {
			walkByAttributeRef(xmlSchema, refName);
		} else if (typeName != null) {
			walkByTypeName(xmlSchema, typeName);
		} else if (xmlSchemaSimpleType != null) {
			walkSimpleType(xmlSchema, xmlSchemaSimpleType);
		}
	}
	
	protected void walkRedefine(XmlSchema xmlSchema, XmlSchemaRedefine obj) {
		walkAnnotated(xmlSchema, obj);
		XmlSchemaObjectCollection items = obj.getItems();
        for (int i = 0; i < items.getCount(); i++) {
            XmlSchemaObject item = items.getItem(i);
            if (item instanceof XmlSchemaSimpleType) {
            	walkSimpleType(xmlSchema, (XmlSchemaSimpleType)item);
            } else if (item instanceof XmlSchemaComplexType) {
            	walkComplexType(xmlSchema, (XmlSchemaComplexType)item);
            } else if (item instanceof XmlSchemaGroupRef) {
            	walkGroupRef(xmlSchema, (XmlSchemaGroupRef)item);
            } else if (item instanceof XmlSchemaGroup) {
            	walkGroup(xmlSchema, (XmlSchemaGroup)item);
            } else if (item instanceof XmlSchemaAttributeGroup) {
            	walkAttributeGroup(xmlSchema, (XmlSchemaAttributeGroup)item);
            } else if (item instanceof XmlSchemaAttributeGroupRef) {
            	walkAttributeGroupRef(xmlSchema, (XmlSchemaAttributeGroupRef)item);
            }
        }
	}
}
