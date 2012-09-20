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

import java.util.LinkedHashMap;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAll;
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

public class XmlSchemaWalker {
	
	public static XmlSchemaWalker newDependencyWalker(LinkedHashMap<QName, XmlSchemaObject> linkedMap) {
		final LinkedHashMap<QName, XmlSchemaObject> map = linkedMap;
		return new XmlSchemaWalker() {
			private LinkedHashMap<QName, XmlSchemaObject> linkedMap = map;
			
			@Override
			protected void walkByTypeName(XmlSchema xmlSchema, QName qname, boolean deep) {
				XmlSchemaType obj = xmlSchema.getTypeByName(qname);
				if (obj != null) {
					if (!linkedMap.containsKey(qname)) {
						linkedMap.put(qname, obj);
						super.walkByTypeName(xmlSchema, qname, deep);
					}
				}
			}

			@Override
			protected void walkByElementRef(XmlSchema xmlSchema, QName qname, boolean deep) {
				XmlSchemaElement obj = xmlSchema.getElementByName(qname);
				if (obj != null) {
					if (!linkedMap.containsKey(qname)) {
						linkedMap.put(qname, obj);
						super.walkByElementRef(xmlSchema, qname, deep);
					}
				}
			}

			@Override
			protected void walkByAttributeGroupRef(XmlSchema xmlSchema, QName qname, boolean deep) {
				XmlSchemaAttributeGroup obj = (XmlSchemaAttributeGroup) xmlSchema.getAttributeGroups().getItem(qname);
				if (obj != null) {
					if (!linkedMap.containsKey(qname)) {
						linkedMap.put(qname, obj);
						super.walkByAttributeGroupRef(xmlSchema, qname, deep);
					}
				}
			}

			@Override
			protected void walkByAttributeRef(XmlSchema xmlSchema, QName qname, boolean deep) {
				XmlSchemaAttribute obj = xmlSchema.getAttributeByName(qname);
				if (obj != null) {
					if (!linkedMap.containsKey(qname)) {
						linkedMap.put(qname, obj);
						super.walkByAttributeRef(xmlSchema, qname, deep);
					}
				}
			}

			@Override
			protected void walkByGroupRef(XmlSchema xmlSchema, QName qname, boolean deep) {
				XmlSchemaGroup obj = (XmlSchemaGroup) xmlSchema.getGroups().getItem(qname);
				if (obj != null) {
					if (!linkedMap.containsKey(qname)) {
						linkedMap.put(qname, obj);
						super.walkByGroupRef(xmlSchema, qname, deep);
					}
				}
			}
		};
	}
	
	protected void walk(XmlSchema xmlSchema, boolean deep) {
		XmlSchemaObjectCollection items = xmlSchema.getItems();
		for (int i=0; i < items.getCount(); i++) {
			XmlSchemaObject obj = items.getItem(i);
			if (obj instanceof XmlSchemaInclude) {
				walkInclude(xmlSchema, (XmlSchemaInclude)obj, deep);
			}
			else if (obj instanceof XmlSchemaImport) {
				walkImport(xmlSchema, (XmlSchemaImport)obj, deep);
			}
		}
		
		for (int i=0; i < items.getCount(); i++) {
			walk(xmlSchema, items.getItem(i), deep);
		}
	}
	
	protected void walkByTypeName(XmlSchema xmlSchema, QName qname, boolean deep) {
		XmlSchemaType obj = xmlSchema.getTypeByName(qname);
		if (obj != null) {
			walk(xmlSchema, obj, deep);
		}
	}
	protected void walkByElementRef(XmlSchema xmlSchema, QName qname, boolean deep) {
		XmlSchemaElement obj = xmlSchema.getElementByName(qname);
		if (obj != null) {
			walk(xmlSchema, obj, deep);
		}
	}
	protected void walkByAttributeRef(XmlSchema xmlSchema, QName qname, boolean deep) {
		XmlSchemaAttribute obj = xmlSchema.getAttributeByName(qname);
		if (obj != null) {
			walk(xmlSchema, obj, deep);
		}
	}
	protected void walkByAttributeGroupRef(XmlSchema xmlSchema, QName qname, boolean deep) {
		XmlSchemaAttributeGroup obj = (XmlSchemaAttributeGroup)xmlSchema.getAttributeGroups().getItem(qname);
		if (obj != null) {
			walk(xmlSchema, obj, deep);
		}
	}
	protected void walkByGroupRef(XmlSchema xmlSchema, QName qname, boolean deep) {
		XmlSchemaGroup obj = (XmlSchemaGroup)xmlSchema.getGroups().getItem(qname);
		if (obj != null) {
			walk(xmlSchema, obj, deep);
		}
	}
	
	protected void walk(XmlSchema xmlSchema, XmlSchemaObject obj, boolean deep) {
        if (obj instanceof XmlSchemaElement) {
        	walkElement(xmlSchema, (XmlSchemaElement)obj, deep);
        } else if (obj instanceof XmlSchemaSimpleType) {
        	walkSimpleType(xmlSchema, (XmlSchemaSimpleType)obj, deep);
        } else if (obj instanceof XmlSchemaComplexType) {
        	walkComplexType(xmlSchema, (XmlSchemaComplexType)obj, deep);
        } else if (obj instanceof XmlSchemaGroup) {
        	walkGroup(xmlSchema, (XmlSchemaGroup)obj, deep);
        } else if (obj instanceof XmlSchemaAttributeGroup) {
        	walkAttributeGroup(xmlSchema, (XmlSchemaAttributeGroup)obj, deep);
        } else if (obj instanceof XmlSchemaAttribute) {
        	walkAttribute(xmlSchema, (XmlSchemaAttribute)obj, deep);
        } else if (obj instanceof XmlSchemaRedefine) {
        	walkRedefine(xmlSchema, (XmlSchemaRedefine)obj, deep);
        }
	}
	
	protected void walkElement(XmlSchema xmlSchema, XmlSchemaElement obj, boolean deep) {
		XmlSchemaElement xmlSchemaElement = (XmlSchemaElement)obj;
		QName refName = xmlSchemaElement.getRefName();
		QName typeName = xmlSchemaElement.getSchemaTypeName();
		XmlSchemaType xmlSchemaType = xmlSchemaElement.getSchemaType();
		if ((refName != null) && (deep)) {
			walkByElementRef(xmlSchema, refName, deep);
		} else if ((typeName != null) && (deep)) {
			walkByTypeName(xmlSchema, typeName, deep);
		} else if (xmlSchemaType != null) {
            if (xmlSchemaType instanceof XmlSchemaComplexType) {
            	walkComplexType(xmlSchema, (XmlSchemaComplexType)xmlSchemaType, deep);
            } else if (xmlSchemaType instanceof XmlSchemaSimpleType) {
            	walkSimpleType(xmlSchema, (XmlSchemaSimpleType)xmlSchemaType, deep);
            }
		}
	}
	
	protected void walkSimpleType(XmlSchema xmlSchema, XmlSchemaSimpleType obj, boolean deep) {
		XmlSchemaSimpleTypeContent simpleTypeContent = obj.getContent();
		if (simpleTypeContent != null) {
	        if (simpleTypeContent instanceof XmlSchemaSimpleTypeRestriction) {
	        	walkSimpleTypeRestriction(xmlSchema, (XmlSchemaSimpleTypeRestriction)simpleTypeContent, deep);
	        } else if (simpleTypeContent instanceof XmlSchemaSimpleTypeList) {
	        	walkSimpleTypeList(xmlSchema, (XmlSchemaSimpleTypeList)simpleTypeContent, deep);
	        } else if (simpleTypeContent instanceof XmlSchemaSimpleTypeUnion) {
	        	walkSimpleTypeUnion(xmlSchema, (XmlSchemaSimpleTypeUnion)simpleTypeContent, deep);
	        }
		}
	}
	protected void walkSimpleTypeRestriction(XmlSchema xmlSchema, XmlSchemaSimpleTypeRestriction obj, boolean deep) {
		QName qname = obj.getBaseTypeName();
		if ((qname != null) && (deep))
			walkByTypeName(xmlSchema, qname, deep);
		else
			walkSimpleType(xmlSchema, obj.getBaseType(), deep);
	}
	protected void walkSimpleTypeList(XmlSchema xmlSchema, XmlSchemaSimpleTypeList obj, boolean deep) {
		
	}
	protected void walkSimpleTypeUnion(XmlSchema xmlSchema, XmlSchemaSimpleTypeUnion obj, boolean deep) {
		
	}
	
	protected void walkComplexType(XmlSchema xmlSchema, XmlSchemaComplexType obj, boolean deep) {
		XmlSchemaContentModel xmlSchemaContentModel  = obj.getContentModel();
		XmlSchemaParticle xmlSchemaParticle = obj.getParticle();
		if (xmlSchemaContentModel != null) {
	        if (xmlSchemaContentModel instanceof XmlSchemaSimpleContent) {
	        	walkSimpleContent(xmlSchema, (XmlSchemaSimpleContent)xmlSchemaContentModel, deep);
	        } else if (xmlSchemaContentModel instanceof XmlSchemaComplexContent) {
	        	walkComplexContent(xmlSchema, (XmlSchemaComplexContent)xmlSchemaContentModel, deep);
	        }
		} else if (xmlSchemaParticle != null) {
	        if (xmlSchemaParticle instanceof XmlSchemaSequence) {
	        	walkSequence(xmlSchema, (XmlSchemaSequence)xmlSchemaParticle, deep);
	        } else if (xmlSchemaParticle instanceof XmlSchemaChoice) {
	        	walkChoice(xmlSchema, (XmlSchemaChoice)xmlSchemaParticle, deep);
	        } else if (xmlSchemaParticle instanceof XmlSchemaAll) {
	        	walkAll(xmlSchema, (XmlSchemaAll)xmlSchemaParticle, deep);
	        } else if (xmlSchemaParticle instanceof XmlSchemaGroupRef) {
	        	walkGroupRef(xmlSchema, (XmlSchemaGroupRef)xmlSchemaParticle, deep);
	        }
		}
	}
	protected void walkGroupRef(XmlSchema xmlSchema, XmlSchemaGroupRef obj, boolean deep) {
		QName refName = obj.getRefName();
		if ((refName != null) && deep) {
			walkByGroupRef(xmlSchema, refName, deep);
		}
		
		XmlSchemaGroupBase xmlSchemaGroupBase = obj.getParticle();
		if (xmlSchemaGroupBase != null) {
            if (xmlSchemaGroupBase instanceof XmlSchemaChoice) {
                walkChoice(xmlSchema, (XmlSchemaChoice)xmlSchemaGroupBase, deep);
            } else if (xmlSchemaGroupBase instanceof XmlSchemaSequence) {
                walkSequence(xmlSchema, (XmlSchemaSequence)xmlSchemaGroupBase, deep);
            } else if (xmlSchemaGroupBase instanceof XmlSchemaAll) {
                walkAll(xmlSchema, (XmlSchemaAll)xmlSchemaGroupBase, deep);
            }
		}
	}

	protected void walkIdentityConstraint(XmlSchema xmlSchema, XmlSchemaIdentityConstraint obj, boolean deep) {
		
	}
	
	protected void walkImport(XmlSchema xmlSchema, XmlSchemaImport obj, boolean deep) {
		XmlSchema imported = obj.getSchema();
		if ((imported != null) && deep) {
			walk(imported, deep);
		}
	}
	
	protected void walkInclude(XmlSchema xmlSchema, XmlSchemaInclude obj, boolean deep) {
		XmlSchema included = obj.getSchema();
		if ((included != null) && deep) {
			walk(included, deep);
		}
	}

	protected void walkAll(XmlSchema xmlSchema, XmlSchemaAll obj, boolean deep) {
        XmlSchemaObjectCollection children = obj.getItems();
        for (int i = 0; i < children.getCount(); i++) {
            XmlSchemaObject child = children.getItem(i);
            if (child instanceof XmlSchemaElement) {
            	walkElement(xmlSchema, (XmlSchemaElement)child, deep);
            }
        }
	}

	protected void walkAnnotation(XmlSchema xmlSchema, XmlSchemaAnnotation obj, boolean deep) {
        XmlSchemaObjectCollection contents = obj.getItems();
        for (int i = 0; i < contents.getCount(); i++) {
            XmlSchemaObject item = contents.getItem(i);
            if (item instanceof XmlSchemaAppInfo) {
            	walkAppInfo(xmlSchema, (XmlSchemaAppInfo)item, deep);
            } else if (item instanceof XmlSchemaDocumentation) {
            	walkDocumentation(xmlSchema, (XmlSchemaDocumentation)item, deep);
            }
        }
	}
	
	protected void walkDocumentation(XmlSchema xmlSchema, XmlSchemaDocumentation item, boolean deep) {
		// TODO Auto-generated method stub
		
	}

	protected void walkAppInfo(XmlSchema xmlSchema, XmlSchemaAppInfo item, boolean deep) {
		// TODO Auto-generated method stub
		
	}

	protected void walkChoice(XmlSchema xmlSchema, XmlSchemaChoice obj, boolean deep) {
        XmlSchemaObjectCollection children = obj.getItems();
        for (int i = 0; i < children.getCount(); i++) {
            XmlSchemaObject child = children.getItem(i);
            if (child instanceof XmlSchemaElement) {
            	walkElement(xmlSchema, (XmlSchemaElement)child, deep);
            } else if (child instanceof XmlSchemaGroupRef) {
            	walkGroupRef(xmlSchema, (XmlSchemaGroupRef)child, deep);
            } else if (child instanceof XmlSchemaChoice) {
            	walkChoice(xmlSchema, (XmlSchemaChoice)child, deep);
            } else if (child instanceof XmlSchemaSequence) {
            	walkSequence(xmlSchema, (XmlSchemaSequence)child, deep);
            } else if (child instanceof XmlSchemaAny) {
            	walkAny(xmlSchema, (XmlSchemaAny)child, deep);
            }
        }
	}

	protected void walkSequence(XmlSchema xmlSchema, XmlSchemaSequence obj, boolean deep) {
        XmlSchemaObjectCollection children = obj.getItems();
        for (int i = 0; i < children.getCount(); i++) {
            XmlSchemaObject child = children.getItem(i);
            if (child instanceof XmlSchemaElement) {
            	walkElement(xmlSchema, (XmlSchemaElement)child, deep);
            } else if (child instanceof XmlSchemaGroupRef) {
            	walkGroupRef(xmlSchema, (XmlSchemaGroupRef)child, deep);
            } else if (child instanceof XmlSchemaChoice) {
            	walkChoice(xmlSchema, (XmlSchemaChoice)child, deep);
            } else if (child instanceof XmlSchemaSequence) {
            	walkSequence(xmlSchema, (XmlSchemaSequence)child, deep);
            } else if (child instanceof XmlSchemaAny) {
            	walkAny(xmlSchema, (XmlSchemaAny)child, deep);
            }
        }
	}

	protected void walkAny(XmlSchema xmlSchema, XmlSchemaAny obj, boolean deep) {
		// TODO Auto-generated method stub
		
	}

	protected void walkSimpleContent(XmlSchema xmlSchema, XmlSchemaSimpleContent obj, boolean deep) {
		XmlSchemaContent xmlSchemaContent = obj.getContent();
        if (xmlSchemaContent instanceof XmlSchemaSimpleContentRestriction) {
        	walkSimpleContentRestriction(xmlSchema, (XmlSchemaSimpleContentRestriction)xmlSchemaContent, deep);
        } else if (xmlSchemaContent instanceof XmlSchemaSimpleContentExtension) {
        	walkSimpleContentExtension(xmlSchema, (XmlSchemaSimpleContentExtension)xmlSchemaContent, deep);
        }
	}
	protected void walkSimpleContentRestriction(XmlSchema xmlSchema, XmlSchemaSimpleContentRestriction obj, boolean deep) {
		QName baseTypeName = obj.getBaseTypeName();
		if ((baseTypeName != null) && deep) {
			walkByTypeName(xmlSchema, baseTypeName, deep);
		}
		
		XmlSchemaObjectCollection attributes = obj.getAttributes();
        for (int i = 0; i < attributes.getCount(); i++) {
            XmlSchemaObject attribute = attributes.getItem(i);
            if (attribute instanceof XmlSchemaAttribute) {
            	walkAttribute(xmlSchema, (XmlSchemaAttribute)attribute, deep);
            } else if (attribute instanceof XmlSchemaAttributeGroupRef) {
            	walkAttributeGroupRef(xmlSchema, (XmlSchemaAttributeGroupRef)attribute, deep);
            }
        }
		XmlSchemaSimpleType xmlSchemaSimpleType = obj.getBaseType();
		if (xmlSchemaSimpleType != null) {
			walkSimpleType(xmlSchema, xmlSchemaSimpleType, deep);
		}
		XmlSchemaAnyAttribute xmlSchemaAnyAttribute = obj.getAnyAttribute();
		if (xmlSchemaAnyAttribute != null) {
			walkAnyAttribute(xmlSchema, xmlSchemaAnyAttribute, deep);
		}
        XmlSchemaObjectCollection facets = obj.getFacets();
        for (int i = 0; i < facets.getCount(); i++) {
        	XmlSchemaObject facet = facets.getItem(i);
        	walkFacet(xmlSchema, (XmlSchemaFacet)facet, deep);
        }
	}
	protected void walkFacet(XmlSchema xmlSchema, XmlSchemaFacet obj, boolean deep) {
		// TODO Auto-generated method stub
		
	}

	protected void walkField(XmlSchema xmlSchema, XmlSchemaXPath obj, boolean deep) {
		
	}
	
	protected void walkAttributeGroupRef(XmlSchema xmlSchema, XmlSchemaAttributeGroupRef obj, boolean deep) {
		QName refName = obj.getRefName();
		if ((refName != null) && deep) {
			walkByGroupRef(xmlSchema, refName, deep);
		}
	}

	protected void walkSimpleContentExtension(XmlSchema xmlSchema, XmlSchemaSimpleContentExtension obj, boolean deep) {
		QName baseTypeName = obj.getBaseTypeName();
		if ((baseTypeName != null) && deep) {
			walkByTypeName(xmlSchema, baseTypeName, deep);
		}
		
		XmlSchemaObjectCollection attributes = obj.getAttributes();
        for (int i = 0; i < attributes.getCount(); i++) {
            XmlSchemaObject attribute = attributes.getItem(i);
            if (attribute instanceof XmlSchemaAttribute) {
            	walkAttribute(xmlSchema, (XmlSchemaAttribute)attribute, deep);
            } else if (attribute instanceof XmlSchemaAttributeGroupRef) {
            	walkAttributeGroupRef(xmlSchema, (XmlSchemaAttributeGroupRef)attribute, deep);
            }
        }
		XmlSchemaAnyAttribute xmlSchemaAnyAttribute = obj.getAnyAttribute();
		if (xmlSchemaAnyAttribute != null) {
			walkAnyAttribute(xmlSchema, xmlSchemaAnyAttribute, deep);
		}
	}
	
	protected void walkAnyAttribute(XmlSchema xmlSchema, XmlSchemaAnyAttribute obj, boolean deep) {
		// TODO Auto-generated method stub
		
	}

	protected void walkComplexContent(XmlSchema xmlSchema, XmlSchemaComplexContent obj, boolean deep) {
		XmlSchemaContent xmlSchemaContent = obj.getContent();
        if (xmlSchemaContent instanceof XmlSchemaComplexContentRestriction) {
        	walkComplexContentRestriction(xmlSchema, (XmlSchemaComplexContentRestriction)xmlSchemaContent, deep);
        } else if (xmlSchemaContent instanceof XmlSchemaComplexContentExtension) {
        	walkComplexContentExtension(xmlSchema, (XmlSchemaComplexContentExtension)xmlSchemaContent, deep);
        }
	}
	
	protected void walkComplexContentExtension(XmlSchema xmlSchema, XmlSchemaComplexContentExtension obj, boolean deep) {
		QName baseTypeName = obj.getBaseTypeName();
		if ((baseTypeName != null) && deep) {
			walkByTypeName(xmlSchema, baseTypeName, deep);
		}
		
		XmlSchemaParticle xmlSchemaParticle = obj.getParticle();
		if (xmlSchemaParticle != null) {
	        if (xmlSchemaParticle instanceof XmlSchemaSequence) {
	        	walkSequence(xmlSchema, (XmlSchemaSequence)xmlSchemaParticle, deep);
	        } else if (xmlSchemaParticle instanceof XmlSchemaChoice) {
	        	walkChoice(xmlSchema, (XmlSchemaChoice)xmlSchemaParticle, deep);
	        } else if (xmlSchemaParticle instanceof XmlSchemaAll) {
	        	walkAll(xmlSchema, (XmlSchemaAll)xmlSchemaParticle, deep);
	        } else if (xmlSchemaParticle instanceof XmlSchemaGroupRef) {
	        	walkGroupRef(xmlSchema, (XmlSchemaGroupRef)xmlSchemaParticle, deep);
	        }
		}
		
		XmlSchemaObjectCollection attributes = obj.getAttributes();
        for (int i = 0; i < attributes.getCount(); i++) {
            XmlSchemaObject attribute = attributes.getItem(i);
            if (attribute instanceof XmlSchemaAttribute) {
            	walkAttribute(xmlSchema, (XmlSchemaAttribute)attribute, deep);
            } else if (attribute instanceof XmlSchemaAttributeGroupRef) {
            	walkAttributeGroupRef(xmlSchema, (XmlSchemaAttributeGroupRef)attribute, deep);
            }
        }
		XmlSchemaAnyAttribute xmlSchemaAnyAttribute = obj.getAnyAttribute();
		if (xmlSchemaAnyAttribute != null) {
			walkAnyAttribute(xmlSchema, xmlSchemaAnyAttribute, deep);
		}
	}

	protected void walkComplexContentRestriction(XmlSchema xmlSchema, XmlSchemaComplexContentRestriction obj, boolean deep) {
		QName baseTypeName = obj.getBaseTypeName();
		if ((baseTypeName != null) && deep) {
			walkByTypeName(xmlSchema, baseTypeName, deep);
		}
		
		XmlSchemaParticle xmlSchemaParticle = obj.getParticle();
		if (xmlSchemaParticle != null) {
	        if (xmlSchemaParticle instanceof XmlSchemaSequence) {
	        	walkSequence(xmlSchema, (XmlSchemaSequence)xmlSchemaParticle, deep);
	        } else if (xmlSchemaParticle instanceof XmlSchemaChoice) {
	        	walkChoice(xmlSchema, (XmlSchemaChoice)xmlSchemaParticle, deep);
	        } else if (xmlSchemaParticle instanceof XmlSchemaAll) {
	        	walkAll(xmlSchema, (XmlSchemaAll)xmlSchemaParticle, deep);
	        } else if (xmlSchemaParticle instanceof XmlSchemaGroupRef) {
	        	walkGroupRef(xmlSchema, (XmlSchemaGroupRef)xmlSchemaParticle, deep);
	        }
		}
		
		XmlSchemaObjectCollection attributes = obj.getAttributes();
        for (int i = 0; i < attributes.getCount(); i++) {
            XmlSchemaObject attribute = attributes.getItem(i);
            if (attribute instanceof XmlSchemaAttribute) {
            	walkAttribute(xmlSchema, (XmlSchemaAttribute)attribute, deep);
            } else if (attribute instanceof XmlSchemaAttributeGroupRef) {
            	walkAttributeGroupRef(xmlSchema, (XmlSchemaAttributeGroupRef)attribute, deep);
            }
        }
		XmlSchemaAnyAttribute xmlSchemaAnyAttribute = obj.getAnyAttribute();
		if (xmlSchemaAnyAttribute != null) {
			walkAnyAttribute(xmlSchema, xmlSchemaAnyAttribute, deep);
		}
	}

	protected void walkGroup(XmlSchema xmlSchema, XmlSchemaGroup obj, boolean deep) {
		XmlSchemaGroupBase xmlSchemaGroupBase = obj.getParticle();
        if (xmlSchemaGroupBase instanceof XmlSchemaSequence) {
        	walkSequence(xmlSchema, (XmlSchemaSequence)xmlSchemaGroupBase, deep);
        } else if (xmlSchemaGroupBase instanceof XmlSchemaChoice) {
        	walkChoice(xmlSchema, (XmlSchemaChoice)xmlSchemaGroupBase, deep);
        } else if (xmlSchemaGroupBase instanceof XmlSchemaAll) {
        	walkAll(xmlSchema, (XmlSchemaAll)xmlSchemaGroupBase, deep);
        }
	}
	protected void walkAttributeGroup(XmlSchema xmlSchema, XmlSchemaAttributeGroup obj, boolean deep) {
		XmlSchemaObjectCollection  attributes = obj.getAttributes();
        for (int i = 0; i < attributes.getCount(); i++) {
            XmlSchemaObject attribute = attributes.getItem(i);
            if (attribute instanceof XmlSchemaAttribute) {
            	walkAttribute(xmlSchema, (XmlSchemaAttribute)attribute, deep);
            } else if (attribute instanceof XmlSchemaAttributeGroupRef) {
            	walkAttributeGroupRef(xmlSchema, (XmlSchemaAttributeGroupRef)attribute, deep);
            }
        }
        XmlSchemaAnyAttribute xmlSchemaAnyAttribute = obj.getAnyAttribute();
        if (xmlSchemaAnyAttribute != null) {
        	walkAnyAttribute(xmlSchema, xmlSchemaAnyAttribute, deep);
        }
	}
	protected void walkAttribute(XmlSchema xmlSchema, XmlSchemaAttribute obj, boolean deep) {
		QName refName = obj.getRefName();
		QName typeName = obj.getSchemaTypeName();
		XmlSchemaSimpleType xmlSchemaSimpleType = obj.getSchemaType();
		
		if ((refName != null) && deep) {
			walkByAttributeRef(xmlSchema, refName, deep);
		} else if (typeName != null) {
			walkByTypeName(xmlSchema, typeName, deep);
		} else if (xmlSchemaSimpleType != null) {
			walkSimpleType(xmlSchema, xmlSchemaSimpleType, deep);
		}
		
	}
	protected void walkRedefine(XmlSchema xmlSchema, XmlSchemaRedefine obj, boolean deep) {
		XmlSchemaObjectCollection items = obj.getItems();
        for (int i = 0; i < items.getCount(); i++) {
            XmlSchemaObject item = items.getItem(i);
            if (item instanceof XmlSchemaSimpleType) {
            	walkSimpleType(xmlSchema, (XmlSchemaSimpleType)item, deep);
            } else if (item instanceof XmlSchemaComplexType) {
            	walkComplexType(xmlSchema, (XmlSchemaComplexType)item, deep);
            } else if (item instanceof XmlSchemaGroupRef) {
            	walkGroupRef(xmlSchema, (XmlSchemaGroupRef)item, deep);
            } else if (item instanceof XmlSchemaGroup) {
            	walkGroup(xmlSchema, (XmlSchemaGroup)item, deep);
            } else if (item instanceof XmlSchemaAttributeGroup) {
            	walkAttributeGroup(xmlSchema, (XmlSchemaAttributeGroup)item, deep);
            } else if (item instanceof XmlSchemaAttributeGroupRef) {
            	walkAttributeGroupRef(xmlSchema, (XmlSchemaAttributeGroupRef)item, deep);
            }
        }
	}
}
