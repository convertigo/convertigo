/*
 * Copyright (c) 2001-2019 Convertigo SA.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAll;
import org.apache.ws.commons.schema.XmlSchemaAny;
import org.apache.ws.commons.schema.XmlSchemaAnyAttribute;
import org.apache.ws.commons.schema.XmlSchemaAppInfo;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaAttributeGroup;
import org.apache.ws.commons.schema.XmlSchemaAttributeGroupRef;
import org.apache.ws.commons.schema.XmlSchemaChoice;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexContentRestriction;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaContent;
import org.apache.ws.commons.schema.XmlSchemaDocumentation;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaEnumerationFacet;
import org.apache.ws.commons.schema.XmlSchemaFacet;
import org.apache.ws.commons.schema.XmlSchemaGroup;
import org.apache.ws.commons.schema.XmlSchemaGroupRef;
import org.apache.ws.commons.schema.XmlSchemaImport;
import org.apache.ws.commons.schema.XmlSchemaInclude;
import org.apache.ws.commons.schema.XmlSchemaLengthFacet;
import org.apache.ws.commons.schema.XmlSchemaMaxExclusiveFacet;
import org.apache.ws.commons.schema.XmlSchemaMaxInclusiveFacet;
import org.apache.ws.commons.schema.XmlSchemaMaxLengthFacet;
import org.apache.ws.commons.schema.XmlSchemaMinExclusiveFacet;
import org.apache.ws.commons.schema.XmlSchemaMinInclusiveFacet;
import org.apache.ws.commons.schema.XmlSchemaMinLengthFacet;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaObjectCollection;
import org.apache.ws.commons.schema.XmlSchemaPatternFacet;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSimpleContent;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentExtension;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentRestriction;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeList;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeUnion;
import org.apache.ws.commons.schema.XmlSchemaTotalDigitsFacet;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.utils.NamespaceMap;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.engine.Engine;

public class JsonSchemaUtils {

	//https://github.com/OAI/OpenAPI-Specification/issues/1532
	//https://github.com/swagger-api/swagger-ui/issues/3803
	//https://github.com/swagger-api/swagger-ui/issues/4643 : Oas3 Form parameters not rendered as expected. Fixed in 3.19.0
	//swagger-ui issue : model with self recursion

	protected static JSONObject getJsonSchema(XmlSchemaCollection xmlSchemaCollection, XmlSchema xmlSchema, String oasDirUrl, boolean isOas2) {
		final NamespaceMap nsMap = (NamespaceMap) xmlSchemaCollection.getNamespaceContext();
		final JSONObject jsonSchema = new JSONObject();
		try {
			String prefix = nsMap.getPrefix(xmlSchema.getTargetNamespace());
			
			jsonSchema.put("id", prefix + ".jsonschema#");
			jsonSchema.put("ns", xmlSchema.getTargetNamespace());
			jsonSchema.put("definitions", new JSONObject());
			
			new XmlSchemaWalker(false, false) {
				final JSONObject definitions = jsonSchema.getJSONObject("definitions");
				JSONObject parent = definitions;
				
				private boolean isGlobal(JSONObject jParent) {
					if (jParent != null) {
						return jParent.equals(definitions);
					}
					return false;
				}
				
				private void addGlobalObject(JSONObject jParent, JSONObject value, String key) throws JSONException {
					if (jParent != null) {
						jParent.put(key, value);
					}
				}
				
				private String getDefinitionRef(QName rname) {
					if (rname != null) {
						String local = rname.getLocalPart();
						String ns = rname.getNamespaceURI();
						// Made a fix for oas2 because swagger-parser v1.0.39 resolver is buggy
						return (isOas2 ? oasDirUrl:"") + nsMap.getPrefix(ns)+ ".jsonschema#/definitions/"+local;
					}
					return null;
				}
				
				private void addChild(JSONObject jParent, JSONObject jElement) throws JSONException {
					if (!jParent.has("children")) {
						jParent.put("children", new JSONArray());
					}
					
					jParent.getJSONArray("children").put(jElement);
				}
				
				private void handle(JSONObject jParent) {
					try {
						handle(jParent, false);
						
						if (jParent.has("value")) {
							JSONObject value = jParent.getJSONObject("value");
							if (value.has("allOf")) {
								jParent.put("allOf", value.getJSONArray("allOf"));
							} else if (value.has("oneOf")) {
								jParent.put("oneOf", value.getJSONArray("oneOf"));
							} else {
								jParent.put("allOf", new JSONArray().put(value));
							}
						}
						
						jParent.remove("objType");
						jParent.remove("QName");
						jParent.remove("children");
						jParent.remove("name");
						jParent.remove("value");
						
					} catch (JSONException e) {
						e.printStackTrace();
						Engine.logEngine.warn("(JSonSchemaUtils) Unexpected exception while generating jsonchema models", e);
					}
				}
				
				private void handle(JSONObject jParent, boolean forceArray) throws JSONException {
					if (jParent.has("objType")) {
						if (jParent.has("children")) {
							long minOccurs = jParent.has("minOccurs") ? jParent.getLong("minOccurs"):0;
							long maxOccurs = jParent.has("maxOccurs") ? jParent.getLong("maxOccurs"):1;
							boolean asArray = forceArray || maxOccurs > 1;
							boolean asOneOf = "choiceType".equals(jParent.get("objType")) ||
												"simpleUnionType".equals(jParent.get("objType"));
							
							List<JSONObject> list = new ArrayList<JSONObject>();
							
							String lastObjType = null;
							JSONObject lastJsonOb = null;
							JSONArray children = jParent.getJSONArray("children");
							for (int i=0; i<children.length(); i++) {
								JSONObject jChild = children.getJSONObject(i);
								if (jChild.has("objType")) {
									String objType = (String) jChild.get("objType");
									if (lastObjType == null || 
											!lastObjType.equals(objType) || asOneOf ||
												(!"elementType".equals(objType) && !"attributeType".equals(objType))) {
										lastObjType = objType;
										lastJsonOb = handleChild(null, jChild, asArray, minOccurs);
										if (lastJsonOb.length() > 0) {
											list.add(lastJsonOb);
										}
									} else {
										handleChild(lastJsonOb, jChild, asArray, minOccurs);
									}
								} else {
									list.add(jChild);
								}
							}
							
							if (list.size() == 0) {
								Engine.logEngine.debug("(JSonSchemaUtils) Warn: empty "+ jParent.get("objType") + " " + jParent.toString());
								list.add(new JSONObject().put("type", "string"));
							} else {
								int size = list.size();
								if (size == 1) {
									if (!jParent.has("value")) {
										jParent.put("value", list.get(0));
									}
								} else if (size > 1) {
									if (!jParent.has("value")) {
										jParent.put("value", new JSONObject());
									}
									
									// oas2 does not support oneOf, anyOf, not
									JSONArray xxxOf = new JSONArray();
									jParent.getJSONObject("value").put(asOneOf && !isOas2 ? "oneOf":"allOf", xxxOf);
									for (JSONObject ob : list) {
										xxxOf.put(ob);
										//if (isOas2 && isOneOf) break; 
									}
								}
							}
						}
					}
				}
				
				private JSONObject handleChild(JSONObject jsonOb, JSONObject jChild, boolean asArray, long min) throws JSONException {
					if (jsonOb == null) {
						jsonOb = new JSONObject();
					}

					if (jChild.has("objType")) {
						String objType = (String) jChild.get("objType");
						
						boolean force = "elementType".equals(objType) || 
											"attributeType".equals(objType) ? false : asArray;
						
						handle(jChild, force);
						
						if ("attributeType".equals(objType)) {
							handleChildAttribute(jsonOb, jChild);
						} else if ("elementType".equals(objType)) {
							handleChildElement(jsonOb, jChild, asArray, min);
						} else if (jChild.has("value")) {
							jsonOb = jChild.getJSONObject("value");
						}
					}
					
					return jsonOb;
				}
				
				private boolean has(JSONArray array, Object ob) throws JSONException {
					for (int i=0; i<array.length(); i++) {
						if (ob != null && ob.equals(array.get(i))) {
							return true;
						}
					}
					return false;
				}
				
				private void handleChildAttribute(JSONObject jsonOb, JSONObject jChild) throws JSONException {
					if (!jsonOb.has("properties")) {
						jsonOb.put("required", new JSONArray())
								.put("properties", new JSONObject());
					}
					JSONObject jProperties = jsonOb.getJSONObject("properties");
					if (!jProperties.has("attr")) {
						jProperties.put("attr", new JSONObject().put("required", new JSONArray())
																	.put("properties", new JSONObject()));
					}
					JSONArray jAttrReqs = jProperties.getJSONObject("attr").getJSONArray("required");
					JSONObject jAttrProps = jProperties.getJSONObject("attr").getJSONObject("properties");
					
					long minOccurs = jChild.getLong("minOccurs");
					String name = jChild.getString("name");
					if (jChild.has("value")) {
						if (!"any".equals(name) && !has(jAttrReqs, name) && minOccurs > 0) {
							jAttrReqs.put(name);
							if (!has(jsonOb.getJSONArray("required"), "attr")) {
								jsonOb.getJSONArray("required").put("attr");
							}
						}
						if ("any".equals(name)) {
							jProperties.getJSONObject("attr").put("additionalProperties", true);
						}
						jAttrProps.put(name, jChild.get("value"));
					}
				}
				
				private void handleChildElement(JSONObject jsonOb, JSONObject jChild, boolean forceArray, long min) throws JSONException {
					if (!jsonOb.has("properties")) {
						jsonOb.put("required", new JSONArray())
								.put("properties", new JSONObject());
					}
					JSONArray jRequired = jsonOb.getJSONArray("required");
					JSONObject jProperties = jsonOb.getJSONObject("properties");
					
					Object jProperty = null;
					String name = jChild.getString("name");
					long minOccurs = jChild.getLong("minOccurs");
					long maxOccurs = jChild.getLong("maxOccurs");
					
					if (jProperties.has(name) || maxOccurs > 1 || forceArray) {
						jProperty = new JSONObject().put("type", "array")
													.put("items", jChild.get("value"))
													.put("minItems", minOccurs);
						if (maxOccurs != Long.MAX_VALUE) {
							((JSONObject)jProperty).put("maxOccurs", maxOccurs);
						}
					} else {
						jProperty = jChild.get("value");
					}
					
					jProperties.put(name, jProperty);
					if (!"any".equals(name) && !has(jRequired, name) && minOccurs > 0 && !(min==0)) {
						jRequired.put(name);
					}
					
					if (jChild.has("children")) { // special case
						JSONArray children = jChild.getJSONArray("children");
						for (int i=0; i<children.length(); i++) {
							JSONObject jOb = children.getJSONObject(i);
							if (jOb.has("objType")) {
								if ("attributeType".equals(jOb.get("objType"))) {
									handleChildAttribute(jsonOb, jOb);
								}
							}
						}
					}
				}
				
				@Override
				protected void walkChoice(XmlSchema xmlSchema, XmlSchemaChoice obj) {
					JSONObject jParent = parent;
					
					JSONObject jElement = new JSONObject();
					try {
						jElement.put("objType", "choiceType")
								.put("minOccurs", obj.getMinOccurs())
								.put("maxOccurs", obj.getMaxOccurs());				
						
						addChild(jParent, jElement);
						parent = jElement;
						
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkChoice(xmlSchema, obj);
					
					parent = jParent;
				}

				@Override
				protected void walkGroup(XmlSchema xmlSchema, XmlSchemaGroup obj) {
					JSONObject jParent = parent;
					
					QName qname = obj.getName();
					
					JSONObject jElement = new JSONObject();
					try {
						if (isGlobal(jParent)) {
							if (qname != null) {
								jElement.put("objType", "groupType");
								
								jElement.put("QName",new JSONObject()
										.put("localPart", qname.getLocalPart())
										.put("namespaceURI", qname.getNamespaceURI()));
								
								addGlobalObject(jParent, jElement, qname.getLocalPart());
								parent = jElement;
							}
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkGroup(xmlSchema, obj);
					
					if (isGlobal(jParent)) {
						handle(jElement);
					}
					
					parent = jParent;
				}
				
				@Override
				protected void walkGroupRef(XmlSchema xmlSchema, XmlSchemaGroupRef obj) {
					JSONObject jParent = parent;
					
					QName refName = obj.getRefName();
					
					long minOccurs = obj.getMinOccurs();
					long maxOccurs = obj.getMaxOccurs();
					
					JSONObject jElement = new JSONObject();
	        		try {
						if (refName != null) {
			        		String ref = getDefinitionRef(refName);
			        		if (ref != null) {
			        			JSONObject jRef = null;
								if (maxOccurs > 1) {
									jRef = new JSONObject()
													.put("type", "array")
													.put("items", new JSONObject().put("$ref", ref))
													.put("minItems", minOccurs);
								} else {
									jRef = new JSONObject().put("$ref", ref);
								}
			        			
								jElement.put("objType", "groupType")
										.put("minOccurs", minOccurs)
										.put("maxOccurs", maxOccurs)
										.put("name", refName.getLocalPart());
								
								addChild(jElement, jRef);
								
								addChild(jParent, jElement);
								parent = jElement;
			        		}
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkGroupRef(xmlSchema, obj);
					
					parent = jParent;
				}
				
				
				@Override
				protected void walkAll(XmlSchema xmlSchema, XmlSchemaAll obj) {
					JSONObject jParent = parent;

					JSONObject jElement = new JSONObject();
					try {
						jElement.put("objType", "allType")
								.put("minOccurs", obj.getMinOccurs())
								.put("maxOccurs", obj.getMaxOccurs());
						
						addChild(jParent, jElement);
						parent = jElement;
						
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkAll(xmlSchema, obj);
					
					parent = jParent;
				}

				@Override
				protected void walkSequence(XmlSchema xmlSchema, XmlSchemaSequence obj) {
					JSONObject jParent = parent;

					JSONObject jElement = new JSONObject();
					try {
						jElement.put("objType", "sequenceType")
								.put("minOccurs", obj.getMinOccurs())
								.put("maxOccurs", obj.getMaxOccurs());
						
						addChild(jParent, jElement);
						parent = jElement;
						
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkSequence(xmlSchema, obj);

					parent = jParent;
				}

				@Override
				protected void walkElement(XmlSchema xmlSchema, XmlSchemaElement obj) {
					JSONObject jParent = parent;
					
					String name = obj.getName();
					
					QName qname = obj.getQName();
					QName refName = obj.getRefName();
					QName typeName = obj.getSchemaTypeName();
					XmlSchemaType xmlSchemaType = obj.getSchemaType();
					
					long minOccurs = obj.getMinOccurs();
					long maxOccurs = obj.getMaxOccurs();
					
					JSONObject jElement = new JSONObject();
					try {
						if (isGlobal(jParent)) {
							jElement.put("objType", "elementType");
						} else {
							jElement.put("objType", "elementType")
									.put("minOccurs", minOccurs)
									.put("maxOccurs", maxOccurs);
						}
						
						
						if (refName == null && typeName == null) {
							// pass though
						} else {
							QName rname = refName != null ? refName : 
								(typeName != null ? typeName: 
								(xmlSchemaType != null ? xmlSchemaType.getQName() : new QName("")));
					
							String ref = getDefinitionRef(rname);
							if (ref != null) {
								if (name.isEmpty()) {
									name = rname.getLocalPart();
								}
								addChild(jElement, new JSONObject().put("$ref", ref));
							}
						}
						
						jElement.put("name", name);
						
						if (isGlobal(jParent)) {
							if (qname != null) {
								jElement.put("QName",new JSONObject()
										.put("localPart", qname.getLocalPart())
										.put("namespaceURI", qname.getNamespaceURI()));
							}
							
							addGlobalObject(jParent, jElement, name);
							parent = jElement;
						} else {
							addChild(jParent, jElement);
							parent = jElement;
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkElement(xmlSchema, obj);
					
					if (isGlobal(jParent)) {
						handle(jElement);
					}
					
					parent = jParent;
				}

				@Override
				protected void walkAny(XmlSchema xmlSchema, XmlSchemaAny obj) {
					JSONObject jParent = parent;
					
					JSONObject jElement = new JSONObject();
					try {
						JSONObject value = new JSONObject()
											.put("type", "string")
											.put("description", "any element");
						
						jElement.put("objType", "elementType")
								.put("minOccurs", 1)
								.put("maxOccurs", 1)
								.put("name", "any")
								.put("value", value);
						
						jParent.put("additionalProperties", true);
						
						addChild(jParent, jElement);
						parent = jElement;								
						
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkAny(xmlSchema, obj);
					
					parent = jParent;
				}
				
				@Override
				protected void walkAnyAttribute(XmlSchema xmlSchema, XmlSchemaAnyAttribute obj) {
					JSONObject jParent = parent;
					
					JSONObject jElement = new JSONObject();
					try {
						JSONObject value = new JSONObject()
								.put("type", "string")
								.put("xml", new JSONObject().put("attribute", true))
								.put("description", "any attribute");
			
						jElement.put("objType", "attributeType")
								.put("minOccurs", 0)
								.put("name", "any")
								.put("value", value);
						
						addChild(jParent, jElement);
						parent = jElement;								
						
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkAnyAttribute(xmlSchema, obj);
					
					parent = jParent;
				}
				
				@Override
				protected void walkAppInfo(XmlSchema xmlSchema, XmlSchemaAppInfo item) {
					JSONObject jParent = parent;
					
					try {
						String description = "";
						NodeList nodeList = item.getMarkup();
						for (int i=0; i<nodeList.getLength(); i++) {
							Node node = nodeList.item(i);
							if (node.getNodeType() == Node.TEXT_NODE) {
								description += node.getNodeValue();
							}
						}
						
						if (!description.isEmpty()) {
							addChild(jParent, new JSONObject().put("title", description));
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkAppInfo(xmlSchema, item);
					
					parent = jParent;
				}
				
				@Override
				protected void walkDocumentation(XmlSchema xmlSchema, XmlSchemaDocumentation item) {
					JSONObject jParent = parent;
					
					try {
						String description = "";
						NodeList nodeList = item.getMarkup();
						for (int i=0; i<nodeList.getLength(); i++) {
							Node node = nodeList.item(i);
							if (node.getNodeType() == Node.TEXT_NODE) {
								description += node.getNodeValue();
							}
						}
						
						if (!description.isEmpty()) {
							addChild(jParent, new JSONObject().put("description", description));
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkDocumentation(xmlSchema, item);
					
					parent = jParent;
				}
				
				@Override
				protected void walkAttribute(XmlSchema xmlSchema, XmlSchemaAttribute obj) {
					JSONObject jParent = parent;
					
					String name = obj.getName();
					
					QName qname = obj.getQName();
					QName refName = obj.getRefName();
					QName typeName = obj.getSchemaTypeName();
					XmlSchemaSimpleType xmlSchemaSimpleType = obj.getSchemaType();
					
					boolean isRequired = obj.getUse().equals(XmlSchemaUtils.attributeUseRequired);
					
					JSONObject jElement = new JSONObject();
					try {
						jElement.put("objType", "attributeType")
								.put("minOccurs", isRequired ? 1:0);
						
						if (refName == null && typeName == null) {
							// pass through
						} else {
							QName rname = refName != null ? refName : 
								(typeName != null ? typeName: 
								(xmlSchemaSimpleType != null ? xmlSchemaSimpleType.getQName() : new QName("")));
					
							String ref = getDefinitionRef(rname);
							if (ref != null) {
								if (name.isEmpty()) {
									name = rname.getLocalPart();
								}
								JSONObject value = new JSONObject();
									value.put("allOf", new JSONArray()
											.put(new JSONObject().put("xml", new JSONObject().put("attribute", true)))
											.put(new JSONObject().put("$ref", ref)));
								
								addChild(jElement, value);
							}
						}
						
						jElement.put("name", name);
						
						if (isGlobal(jParent)) {
							if (qname != null) {
								jElement.put("QName",new JSONObject()
										.put("localPart", qname.getLocalPart())
										.put("namespaceURI", qname.getNamespaceURI()));
							}
							
							addGlobalObject(jParent, jElement, name);
							parent = jElement;
						} else {
							addChild(jParent, jElement);
							parent = jElement;
						}
						
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkAttribute(xmlSchema, obj);

					if (isGlobal(jParent)) {
						handle(jElement);
					}
					
					parent = jParent;
				}
				
				@Override
				protected void walkAttributeGroup(XmlSchema xmlSchema, XmlSchemaAttributeGroup obj) {
					JSONObject jParent = parent;
					
					QName qname = obj.getName();
					
					JSONObject jElement = new JSONObject();
					try {
						if (isGlobal(jParent)) {
							if (qname != null) {
								jElement.put("objType", "attributeGroupType");
								
								jElement.put("QName",new JSONObject()
										.put("localPart", qname.getLocalPart())
										.put("namespaceURI", qname.getNamespaceURI()));
								
								addGlobalObject(jParent, jElement, qname.getLocalPart());
								parent = jElement;
							}
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkAttributeGroup(xmlSchema, obj);

					if (isGlobal(jParent)) {
						handle(jElement);
					}
					
					parent = jParent;
				}

				@Override
				protected void walkAttributeGroupRef(XmlSchema xmlSchema, XmlSchemaAttributeGroupRef obj) {
					JSONObject jParent = parent;
					
					QName refName = obj.getRefName();
					
					JSONObject jElement = new JSONObject();
	        		try {
						if (refName != null) {
			        		String ref = getDefinitionRef(refName);
			        		if (ref != null) {
								jElement.put("objType", "attributeGroupType")
										.put("name", refName.getLocalPart());
			        			
								addChild(jElement, new JSONObject().put("$ref", ref));
								
								addChild(jParent, jElement);
								parent = jElement;
			        		}
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkAttributeGroupRef(xmlSchema, obj);
					
					parent = jParent;
				}
				
				@Override
				protected void walkSimpleContent(XmlSchema xmlSchema, XmlSchemaSimpleContent obj) {
					JSONObject jParent = parent;
					
					QName qname = null;
					XmlSchemaContent xmlSchemaContent = obj.getContent();
			        if (xmlSchemaContent instanceof XmlSchemaSimpleContentRestriction) {
			        	qname = ((XmlSchemaSimpleContentRestriction)xmlSchemaContent).getBaseTypeName();
			        } else if (xmlSchemaContent instanceof XmlSchemaSimpleContentExtension) {
			        	qname = ((XmlSchemaSimpleContentExtension)xmlSchemaContent).getBaseTypeName();
			        }
			        
					JSONObject jElement = new JSONObject();
					try {
						if (qname != null) {
							String ref = getDefinitionRef(qname);
							if (ref != null) {
								jElement.put("objType", "elementType")
										.put("minOccurs", 1)
										.put("maxOccurs", 1)
										.put("name", "text")
										.put("value", new JSONObject().put("$ref", ref));
																
								addChild(jParent, jElement);
								parent = jElement;								
							}
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkSimpleContent(xmlSchema, obj);
					
					parent = jParent;
				}
				
				@Override
				protected void walkSimpleType(XmlSchema xmlSchema, XmlSchemaSimpleType obj) {
					JSONObject jParent = parent;
					
					QName qname = obj.getQName();
					QName bname = obj.getBaseSchemaTypeName();
					
					JSONObject jElement = new JSONObject();
					try {
						jElement.put("objType", "simpleType");
						
						if (bname != null) {
							String ref = getDefinitionRef(bname);
							if (ref != null) {
								jElement.put("value", new JSONObject().put("$ref", ref));
							}
						}
						
						if (isGlobal(jParent)) {
							if (qname != null) {
								jElement.put("QName",new JSONObject()
										.put("localPart", qname.getLocalPart())
										.put("namespaceURI", qname.getNamespaceURI()));
							}
							
							addGlobalObject(jParent, jElement, obj.getName());
							parent = jElement;
						} else {
							addChild(jParent, jElement);
							parent = jElement;
						}
						
					} catch (JSONException e1) {
						e1.printStackTrace();
					}
					
					super.walkSimpleType(xmlSchema, obj);
					
					if (isGlobal(jParent)) {
						handle(jElement);
					}
					
					parent = jParent;
				}
				
				@Override
				protected void walkSimpleTypeRestriction(XmlSchema xmlSchema, XmlSchemaSimpleTypeRestriction obj) {
					JSONObject jParent = parent;
					
					QName qname = obj.getBaseTypeName();
					
					JSONObject jElement = new JSONObject();
		        	try {
						if (qname != null) {
				        	String ref = getDefinitionRef(qname);
				        	if (ref != null) {
								jElement.put("objType", "simpleRestrictionType");
								addChild(jElement, new JSONObject().put("$ref", ref));
								
								addChild(jParent, jElement);
								parent = jElement;								
				        	}
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkSimpleTypeRestriction(xmlSchema, obj);
					
					parent = jParent;
				}
				
				@Override
				protected void walkSimpleTypeUnion(XmlSchema xmlSchema, XmlSchemaSimpleTypeUnion obj) {
					JSONObject jParent = parent;
					
					JSONObject jElement = new JSONObject();
					try {
						jElement.put("objType", "simpleUnionType");

						QName[] members = obj.getMemberTypesQNames();
						if (members != null) {
							for (QName qname : members) {
				        		String ref = getDefinitionRef(qname);
			        			addChild(jElement, new JSONObject().put("$ref", ref));
							}
						}
						
						addChild(jParent, jElement);						
						parent = jElement;
						
					} catch (JSONException e) {
						e.printStackTrace();
					}
													
					super.walkSimpleTypeUnion(xmlSchema, obj);
					
					parent = jParent;
				}
				
				@Override
				protected void walkFacets(XmlSchema xmlSchema, XmlSchemaObjectCollection facets) {
					JSONObject jParent = parent;
					
					JSONArray array = new JSONArray();
					for (int i = 0; i < facets.getCount(); i++) {
			        	XmlSchemaFacet facet = (XmlSchemaFacet) facets.getItem(i);
			        	Object value = facet.getValue();
			        	try {
			        		JSONObject jFacet = new JSONObject();
			        		
			        		if (facet instanceof XmlSchemaEnumerationFacet) {
			        			array.put(value);
			        			if (i < facets.getCount() - 1) {
			        				continue;
			        			}
							} else if (facet instanceof XmlSchemaPatternFacet) {
								jFacet.put("pattern", value);
							} else if (facet instanceof XmlSchemaLengthFacet) {
								jFacet.put("length", value);
							} else if (facet instanceof XmlSchemaMinLengthFacet) {
								jFacet.put("minLength", value);
							} else if (facet instanceof XmlSchemaMaxLengthFacet) {
								jFacet.put("maxLength", value);
							} else if (facet instanceof XmlSchemaMaxExclusiveFacet) {
								jFacet.put("maximum", value).put("exclusiveMaximum", true);
							} else if (facet instanceof XmlSchemaMaxInclusiveFacet) {
								jFacet.put("maximum", value).put("exclusiveMaximum", false);
							} else if (facet instanceof XmlSchemaMinExclusiveFacet) {
								jFacet.put("minimum", value).put("exclusiveMinimum", true);
							} else if (facet instanceof XmlSchemaMinInclusiveFacet) {
								jFacet.put("minimum", value).put("exclusiveMinimum", false);
							} else if (facet instanceof XmlSchemaTotalDigitsFacet) {
								jFacet.put("maxLength", value);
							}

			        		if (facet instanceof XmlSchemaEnumerationFacet) {
			        			jFacet.put("enum", array);
			        		}
			        		
				        	addChild(jParent, jFacet);
				        	parent = jFacet;
				        	
				        	walkFacet(xmlSchema, facet); // <-- walk here
				        	
				        	//parent = jParent;
				        	
						} catch (JSONException e) {
							e.printStackTrace();
						}
			        }
					
					//super.walFacets();
					
					parent = jParent;
				}
				
				@Override
				protected void walkSimpleTypeList(XmlSchema xmlSchema, XmlSchemaSimpleTypeList obj) {
					JSONObject jParent = parent;
					
					QName qname = obj.getItemTypeName();
					
					JSONObject jElement = new JSONObject();
		        	try {
						if (qname != null) {
				        	String ref = getDefinitionRef(qname);
				        	if (ref != null) {
								JSONObject value = new JSONObject()
															.put("type", "array")
															.put("items", new JSONObject().put("$ref", ref));
								
								jElement.put("objType", "simpleListType")
										.put("value", value);
								
								addChild(jParent, jElement);
								parent = jElement;								
				        	}
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkSimpleTypeList(xmlSchema, obj);
					
					parent = jParent;
				}
				
				@Override
				protected void walkComplexContentExtension(XmlSchema xmlSchema, XmlSchemaComplexContentExtension obj) {
					JSONObject jParent = parent;
					
					QName baseTypeName = obj.getBaseTypeName();
					
					JSONObject jElement = new JSONObject();
	        		try {
						if (baseTypeName != null) {
			        		String ref = getDefinitionRef(baseTypeName);
			        		if (ref != null) {
								jElement.put("objType", "complexContentExtensionType");
								addChild(jElement, new JSONObject().put("$ref", ref));
								
								addChild(jParent, jElement);
								parent = jElement;								
			        		}
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkComplexContentExtension(xmlSchema, obj);
					
					parent = jParent;
				}
				
				@Override
				protected void walkComplexContentRestriction(XmlSchema xmlSchema, XmlSchemaComplexContentRestriction obj) {
					JSONObject jParent = parent;
					
					QName baseTypeName = obj.getBaseTypeName();
					
					JSONObject jElement = new JSONObject();
	        		try {
						if (baseTypeName != null) {
			        		String ref = getDefinitionRef(baseTypeName);
			        		if (ref != null) {
								jElement.put("objType", "complexContentRestrictionType");
								addChild(jElement, new JSONObject().put("$ref", ref));
								
								addChild(jParent, jElement);
								parent = jElement;								
			        		}
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkComplexContentRestriction(xmlSchema, obj);
					
					parent = jParent;
				}
				
				@Override
				protected void walkComplexType(XmlSchema xmlSchema, XmlSchemaComplexType obj) {
					JSONObject jParent = parent;
					
					QName qname = obj.getQName();
					
					JSONObject jElement = new JSONObject();
					try {
						jElement.put("objType", "complexType");
						
						if (obj.isMixed()) {
							JSONObject jText = new JSONObject();
							jText.put("objType", "elementType")
									.put("name", "text")
									.put("minOccurs", 0)
									.put("maxOccurs", 1)
									.put("value", new JSONObject()
													.put("description", "the mixed content string")
													.put("type", "string"));
							
							addChild(jElement, jText);
						}
						
						if (isGlobal(jParent)) {
							if (qname != null) {
								jElement.put("QName",new JSONObject()
										.put("localPart", qname.getLocalPart())
										.put("namespaceURI", qname.getNamespaceURI()));
							}
							
							addGlobalObject(jParent, jElement, obj.getName());
							parent = jElement;
						} else {
							addChild(jParent, jElement);
							parent = jElement;
						}
						
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkComplexType(xmlSchema, obj);
					
					if (isGlobal(jParent)) {
						handle(jElement);
					}
					
					parent = jParent;
				}

				
			}.walk(xmlSchema);
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
				
		return jsonSchema;
	}
	
	protected static JSONObject getOasSchema(XmlSchemaCollection xmlSchemaCollection, XmlSchema xmlSchema, String oasDirUrl, boolean isOas2) {
		final NamespaceMap nsMap = (NamespaceMap) xmlSchemaCollection.getNamespaceContext();
		final JSONObject jsonSchema = new JSONObject();
		try {
			String prefix = nsMap.getPrefix(xmlSchema.getTargetNamespace());
			
			jsonSchema.put("id", oasDirUrl + prefix + ".jsonschema#");
			jsonSchema.put("ns", xmlSchema.getTargetNamespace());
			jsonSchema.put("definitions", new JSONObject());
			
			new XmlSchemaWalker(false, false) {
				final Map<String, JSONObject> refs = new HashMap<String, JSONObject>(50);
				final JSONObject definitions = jsonSchema.getJSONObject("definitions");
				JSONObject parent = definitions;
				
				private boolean isGlobal(JSONObject jParent) {
					if (jParent != null) {
						return jParent.equals(definitions);
					}
					return false;
				}
				
				private JSONObject getRefObject(String ref) throws JSONException {
					JSONObject refObject =  refs.get(ref);
//					if (refObject != null && refObject.getString("objType").equals("simpleType")) {
//						if (refObject.has("value")) {
//							JSONObject ob = new JSONObject();
//							copyOKeys(refObject.getJSONObject("value"), ob);
//							return ob;
//						}
//					}
					if (refObject != null) {
						if (!refObject.has("value")) {
							handle(refObject);
						}
						if (refObject.has("value")) {
							JSONObject value = refObject.getJSONObject("value");
							if (value.has("type") && !value.getString("type").equals("object")) {
								JSONObject ob = new JSONObject();
								copyOKeys(value, ob);
								return ob;
							}
						}
					}
					return new JSONObject().put("$ref", ref);
				}
				
				private String normalize(String key) {
					return key.replaceAll("[^a-zA-Z0-9]", "x");
				}
				
				private void addGlobalObject(JSONObject jParent, JSONObject jElement, String key) throws JSONException {
					if (jParent != null) {
						jParent.put(normalize(key), jElement);
						refs.put(jElement.getString("objKey"), jElement);
					}
				}
				
				private String toOasType(String baseType) {
					String oasType = baseType;
					switch (baseType) {
						case "NMTOKEN":
						case "token":
						case "IDREF":
						case "ID":
							oasType = "string";
							break;
							
						case "decimal":
							oasType = "number";
							break;
							
						case "int":
						case "nonNegativeInteger":
						case "positiveInteger":
							oasType = "integer";
							break;
					}
					return oasType;
				}
				
				private String getDefinitionRef(QName rname) {
					if (rname != null) {
						String local = rname.getLocalPart();
						String ns = rname.getNamespaceURI();
						return oasDirUrl + nsMap.getPrefix(ns)+ ".jsonschema#/definitions/"+ normalize(local);
					}
					return null;
				}
				
				private void addChild(JSONObject jParent, JSONObject jElement) throws JSONException {
					if (!jParent.has("children")) {
						jParent.put("children", new JSONArray());
					}
					
					jParent.getJSONArray("children").put(jElement);
				}
				
				private void handleElement(JSONObject jsonOb, JSONObject jsonChild, long minItems, long maxItems) throws JSONException {
					if (!jsonOb.has("type")) {
						jsonOb.put("type", "object");
					}
					if (!jsonOb.has("required")) {
						jsonOb.put("required", new JSONArray());
					}
					if (!jsonOb.has("properties")) {
						jsonOb.put("properties", new JSONObject());
					}
					JSONArray required = jsonOb.getJSONArray("required");
					JSONObject properties = jsonOb.getJSONObject("properties");
					
					String propertyName = jsonChild.getString("name");
					long minOccurs = jsonChild.has("minOccurs") ? jsonChild.getLong("minOccurs") : 0;
					long maxOccurs = jsonChild.has("maxOccurs") ? jsonChild.getLong("maxOccurs") : minOccurs;
					boolean isRequired = Math.min(minOccurs, minItems) > 0;
					boolean isArray = maxOccurs > 1 || maxItems > 1;
					
					JSONObject property = new JSONObject();
					
					// fill required
					if (isRequired) {
						if (required.join(",").indexOf(propertyName) == -1) {
							required.put(propertyName);
						}
					} else if (!isArray) {
						property.put("nullable",true);
					}
					
					// fill properties
					if (isArray) {
						property.put("type","array");
						property.put("minItems", minOccurs);
						property.put("maxItems", Math.max(maxOccurs, maxItems));
						property.put("items", new JSONObject());
					}
					JSONObject prop = isArray ? property.getJSONObject("items") : property;
					if (jsonChild.has("value")) {
						JSONObject value = jsonChild.getJSONObject("value");
						@SuppressWarnings("unchecked")
						Iterator<String> it = value.keys();
						while (it.hasNext()) {
							String pkey = it.next();
							if (!pkey.isEmpty()) {
								prop.put(pkey, value.get(pkey));
							}
						}
					} else if (jsonChild.has("children")) {
						JSONArray children = jsonChild.getJSONArray("children");
						for (int i = 0; i <children.length(); i++) {
							JSONObject child = children.getJSONObject(i);
							if (!child.has("objType")) {
								@SuppressWarnings("unchecked")
								Iterator<String> it = child.keys();
								while (it.hasNext()) {
									String pkey = it.next();
									if (!pkey.isEmpty()) {
										prop.put(pkey, child.get(pkey));
									}
								}
							}
						}
					}
					
					if (!isRequired && prop.has("$ref")) {
						JSONArray allOf = new JSONArray().put(new JSONObject().put("$ref", prop.remove("$ref")));
						prop.put("nullable", true).put("allOf", allOf);
					}
					
					properties.put(propertyName, property);
					
					// handle children
					if (jsonChild.has("children")) {
						JSONArray children = jsonChild.getJSONArray("children");
						for (int i = 0; i <children.length(); i++) {
							JSONObject child = children.getJSONObject(i);
							if (child.has("objType")) {
								handleChild(prop.has("type") || prop.has("$ref") ? jsonOb : prop, child);
							}
						}
					}
				}
				
				private void handleAttribute(JSONObject jsonOb, JSONObject jsonChild, long minItems, long maxItems) throws JSONException {
					if (!jsonOb.has("required")) {
						jsonOb.put("required", new JSONArray());
					}
					if (!jsonOb.has("properties")) {
						jsonOb.put("properties", new JSONObject());
					}
					
					JSONObject attrObject = null;
					if (jsonOb.getJSONObject("properties").has("attr")) {
						attrObject = jsonOb.getJSONObject("properties").getJSONObject("attr");
					} else {
						attrObject = new JSONObject()
										.put("type", "object")
										.put("required", new JSONArray())
										.put("properties", new JSONObject());
						jsonOb.getJSONObject("properties").put("attr", attrObject);
					}
					
					JSONArray required = attrObject.getJSONArray("required");
					JSONObject properties = attrObject.getJSONObject("properties");
					
					String attrName = jsonChild.getString("name");
					long minOccurs = jsonChild.has("minOccurs") ? jsonChild.getLong("minOccurs") : 0;
					long maxOccurs = jsonChild.has("maxOccurs") ? jsonChild.getLong("maxOccurs") : minOccurs;
					boolean isRequired = minOccurs > 0;
					boolean isArray = maxOccurs > 1;
					
					// fill required
					if (isRequired) {
						if (required.join(",").indexOf(attrName) == -1) {
							required.put(attrName);
							// if attribute is required then attr object is required also
							if (jsonOb.getJSONArray("required").join(",").indexOf("attr") == -1) {
								jsonOb.getJSONArray("required").put("attr");
							}
						}
					}
					
					// fill properties
					JSONObject attribute = new JSONObject();
					if (isArray) {
						attribute.put("type","array");
						attribute.put("minItems", minOccurs);
						attribute.put("maxItems", maxOccurs);
						attribute.put("items", new JSONObject());
					}
					JSONObject attr = isArray ? attribute.getJSONObject("items") : attribute;
					if (jsonChild.has("value")) {
						JSONObject value = jsonChild.getJSONObject("value");
						copyOKeys(value, attr);
					} else if (jsonChild.has("children")) {
						JSONArray children = jsonChild.getJSONArray("children");
						for (int i = 0; i <children.length(); i++) {
							JSONObject child = children.getJSONObject(i);
							if (!child.has("objType")) {
								copyOKeys(child, attr);
							}
						}
					}
					properties.put(attrName, attribute);

					// handle children
					if (jsonChild.has("children")) {
						JSONArray children = jsonChild.getJSONArray("children");
						for (int i = 0; i <children.length(); i++) {
							JSONObject child = children.getJSONObject(i);
							if (child.has("objType")) {
								handleChild(attr, child);
							}
						}
					}
				}
				
				private void handleChoice(JSONObject jsonOb, JSONObject jsonChild, long minItems, long maxItems) throws JSONException {
					//note: oneOf with complex inline model is baddly supported: minTems forced to 0L
					
					// handle children
					if (jsonChild.has("children")) {
						JSONArray children = jsonChild.getJSONArray("children");
						for (int i = 0; i <children.length(); i++) {
							JSONObject child = children.getJSONObject(i);
							handleChild(jsonOb, child, 0L, maxItems); // minTems forced to 0L
						}
						
					}
				}
				
				private void handleUnion(JSONObject jsonOb, JSONObject jsonChild, long minItems, long maxItems) throws JSONException {
					if (jsonChild.has("children")) {
						JSONArray children = jsonChild.getJSONArray("children");
						for (int i = 0; i <children.length(); i++) {
							JSONObject child = children.getJSONObject(i);
							handleChild(jsonOb, child, minItems, maxItems);
						}
					} 
				}
				
				private void handleExtension(JSONObject jsonOb, JSONObject jsonChild, long minItems, long maxItems) throws JSONException {
					boolean hasAllOf = jsonOb.has("allOf");
					if (!hasAllOf) {
						jsonOb.put("allOf", new JSONArray());
					}
					JSONArray allOf = jsonOb.getJSONArray("allOf");
					
					// handle children
					if (jsonChild.has("children")) {
						JSONArray children = jsonChild.getJSONArray("children");
						for (int i = 0; i <children.length(); i++) {
							JSONObject child = children.getJSONObject(i);
							JSONObject ob = new JSONObject();
							handleChild(ob, child);
							merge(ob);
							allOf.put(ob);
						}
					}
					
					merge(jsonOb);
				}
				
				private void handleSequence(JSONObject jsonOb, JSONObject jsonChild, long minItems, long maxItems) throws JSONException {
					long minOccurs = jsonChild.has("minOccurs") ? jsonChild.getLong("minOccurs") : 0;
					long maxOccurs = jsonChild.has("maxOccurs") ? jsonChild.getLong("maxOccurs") : minOccurs;
					
					boolean hasAllOf = jsonOb.has("allOf");
					if (!hasAllOf) {
						jsonOb.put("allOf", new JSONArray());
					}
					JSONArray allOf = jsonOb.getJSONArray("allOf");
					
					// handle children
					if (jsonChild.has("children")) {
						JSONArray children = jsonChild.getJSONArray("children");
						for (int i = 0; i <children.length(); i++) {
							JSONObject child = children.getJSONObject(i);
							JSONObject ob = new JSONObject();
							handleChild(ob, child, Math.min(minOccurs, minItems), Math.max(maxOccurs, maxItems));
							merge(ob);
							allOf.put(ob);
						}
						
						merge(jsonOb);
					}					
				}
				
				private void merge(JSONObject jsonOb) throws JSONException {
					if (jsonOb.has("allOf")) {
						JSONArray allOf = jsonOb.getJSONArray("allOf");
						int len = allOf.length();
						if (len > 0) {
							JSONObject merge = new JSONObject()
												.put("type", "object")
												.put("required", new JSONArray())
												.put("properties", new JSONObject());
							
							List<JSONObject> merged = new ArrayList<JSONObject>();
							List<JSONObject> others = new ArrayList<JSONObject>();
							for (int i = 0; i < len; i++) {
								JSONObject ob = allOf.getJSONObject(i);
								if (ob.has("description") && ob.length() == 1) {
									merge.put("description", ob.getString("description"));
									merged.add(ob);
								}
								if (ob.has("properties")) {
									copyOKeys(ob.getJSONObject("properties"), merge.getJSONObject("properties"));
								}
								if (ob.has("required")) {
									copyAKeys(ob.getJSONArray("required"), merge.getJSONArray("required"));
								}
								
								if (!ob.has("properties") &&  !ob.has("required")) {
									others.add(ob);
								} else {
									merged.add(ob);
								}
							}
							
							for (JSONObject ob: merged) {
								allOf.remove(ob);
							}
							if (merged.size() > 0) {
								allOf.put(merge);
							}
							
							// merge in parent
							if (allOf.length() == 1) {
								JSONObject ob = allOf.getJSONObject(0);
								if (jsonOb.has("properties")) {
									if (ob.has("description") && ob.length() == 1) {
										jsonOb.put("description", ob.getString("description"));
									}
									if (ob.has("properties")) {
										copyOKeys(ob.getJSONObject("properties"), jsonOb.getJSONObject("properties"));
									}
									if (ob.has("required")) {
										copyAKeys(ob.getJSONArray("required"), jsonOb.getJSONArray("required"));
									}
									jsonOb.remove("allOf");
								} else {
									copyOKeys(ob, jsonOb);
									jsonOb.remove("allOf");
								}
							} else {
								;
							}
						}
					}
				}
				
				private void handleChild(JSONObject jsonOb, JSONObject jsonChild) throws JSONException {
					handleChild(jsonOb, jsonChild, 1L, 1L);
				}
				
				private void handleChild(JSONObject jsonOb, JSONObject jsonChild, long minItems, long maxItems) throws JSONException {
					if (jsonChild.has("objType")) {
						String objType = jsonChild.getString("objType");
						switch (objType) {
							case "elementType":
								handleElement(jsonOb, jsonChild, minItems, maxItems);
								break;
								
							case "attributeType":
								handleAttribute(jsonOb, jsonChild, minItems, maxItems);
								break;
							
							case "choiceType":
								handleChoice(jsonOb, jsonChild, minItems, maxItems);
								break;
								
							case "sequenceType":
								handleSequence(jsonOb, jsonChild, minItems, maxItems);
								break;
								
							case "simpleUnionType":
								handleUnion(jsonOb, jsonChild, minItems, maxItems);
								break;
								
							case "complexContentExtensionType":
								handleExtension(jsonOb, jsonChild, minItems, maxItems);
								break;
							
							case "allType":
							case "groupType":
							case "complexType":
							case "simpleRestrictionType":
							case "simpleType":
								if (jsonChild.has("children")) {
									JSONArray children = jsonChild.getJSONArray("children");
									for (int i = 0; i <children.length(); i++) {
										handleChild(jsonOb, (JSONObject)children.get(i), minItems, maxItems);
									}
								} else {
									System.out.println("ObjType: " + objType + " has no children");
								}
								break;
							default:
								System.out.println("Unhandled objType: " + objType);
								break;
						}
					} else {
						copyOKeys(jsonChild, jsonOb);
					}
					
					// remove pattern for integer (not supported)
					if (jsonOb.has("type") && jsonOb.has("pattern")) {
						if (!jsonOb.getString("type").equals("string")) {
							jsonOb.remove("pattern");
						}
					}
					
				}
				
				private void copyOKeys(JSONObject from, JSONObject to) throws JSONException {
					@SuppressWarnings("unchecked")
					Iterator<String> it = from.keys();
					while (it.hasNext()) {
						String pkey = it.next();
						if (!pkey.isEmpty()) {
							to.put(pkey, from.get(pkey));
						}
					}
				}
				
				private void copyAKeys(JSONArray from, JSONArray to) throws JSONException {
					for (int i = 0; i < from.length(); i++) {
						to.put(from.get(i));
					}
				}
				
				private void handleRefs(Object ob) {
					try {
						if (ob instanceof JSONObject) {
							JSONObject jsonOb = (JSONObject)ob;
							if (jsonOb.has("$ref")) {
								String ref = jsonOb.getString("$ref");
								JSONObject refObject = getRefObject(ref);
								if (!refObject.has("$ref")) {
									jsonOb.remove("$ref");
									copyOKeys(refObject, jsonOb);
								}
							}
							
							@SuppressWarnings("unchecked")
							Iterator<String> it = jsonOb.keys();
							while (it.hasNext()) {
								String pkey = it.next();
								handleRefs(jsonOb.get(pkey));
							}
							
							if (jsonOb.has("allOf")) {
								merge(jsonOb);
							}
							
						} else if (ob instanceof JSONArray) {
							JSONArray jsonArray = (JSONArray)ob;
							for (int i = 0; i < jsonArray.length(); i++) {
								handleRefs(jsonArray.get(i));
							}
						}
						
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
				
				private void handle(JSONObject jParent) {
					try {
						if (jParent != null) {
							if (!jParent.has("value")) {
								JSONObject value = new JSONObject();
								jParent.put("value", value);
								
								JSONObject jsonOb = jParent.getJSONObject("value");
								if (jParent.has("children")) {
									JSONArray children = jParent.getJSONArray("children");
									int len = children.length();
									for (int i = 0; i <len; i++) {
										handleChild(jsonOb, (JSONObject)children.get(i));
										merge(jsonOb);
									}
								} else {
									//
								}
							}
						}
					} catch (JSONException e) {
						e.printStackTrace();
						Engine.logEngine.warn("(JSonSchemaUtils) Unexpected exception while generating jsonchema models", e);
					}
				}
				
				@Override
				protected void walkChoice(XmlSchema xmlSchema, XmlSchemaChoice obj) {
					JSONObject jParent = parent;
					
					JSONObject jElement = new JSONObject();
					try {
						jElement.put("objType", "choiceType")
								.put("minOccurs", obj.getMinOccurs())
								.put("maxOccurs", obj.getMaxOccurs());				
						
						addChild(jParent, jElement);
						parent = jElement;
						
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkChoice(xmlSchema, obj);
					
					parent = jParent;
				}

				@Override
				protected void walkGroup(XmlSchema xmlSchema, XmlSchemaGroup obj) {
					JSONObject jParent = parent;
					
					QName qname = obj.getName();
					
					JSONObject jElement = new JSONObject();
					try {
						if (isGlobal(jParent)) {
							if (qname != null) {
								jElement.put("objType", "groupType");
								
								String id = jsonSchema.getString("id");
								jElement.put("objKey", id + "/definitions/" + normalize(qname.getLocalPart()));
								
								jElement.put("QName",new JSONObject()
										.put("localPart", qname.getLocalPart())
										.put("namespaceURI", qname.getNamespaceURI()));
								
								addGlobalObject(jParent, jElement, qname.getLocalPart());
								parent = jElement;
							}
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkGroup(xmlSchema, obj);
					
					if (isGlobal(jParent)) {
						//handle(jElement);
					}
					
					parent = jParent;
				}
				
				@Override
				protected void walkGroupRef(XmlSchema xmlSchema, XmlSchemaGroupRef obj) {
					JSONObject jParent = parent;
					
					QName refName = obj.getRefName();
					
					long minOccurs = obj.getMinOccurs();
					long maxOccurs = obj.getMaxOccurs();
					
					JSONObject jElement = new JSONObject();
	        		try {
						if (refName != null) {
			        		String ref = getDefinitionRef(refName);
			        		if (ref != null) {
			        			JSONObject jRef = null;
								if (maxOccurs > 1) {
									if (ref.indexOf("xsd.jsonschema") != -1) {
										jRef = new JSONObject()
												.put("type", "array")
												.put("items", new JSONObject().put("type", toOasType(refName.getLocalPart())))
												.put("minItems", minOccurs);
									} else {
										JSONObject refObject = getRefObject(ref);
										jRef = new JSONObject()
												.put("type", "array")
												.put("items", refObject)
												.put("minItems", minOccurs);
									}
								} else {
									if (ref.indexOf("xsd.jsonschema") != -1) {
										jRef = new JSONObject().put("type", toOasType(refName.getLocalPart()));
										if (minOccurs == 0) {
											jRef.put("nullable", "true");
										}
									} else {
										JSONObject refObject = getRefObject(ref);
										if (minOccurs == 0) {
											jRef = new JSONObject().put("nullable", "true")
													.put("allOf", new JSONArray().put(refObject));
										} else {
											jRef = refObject;
										}
									}
								}
			        			
								jElement.put("objType", "groupType")
										.put("minOccurs", minOccurs)
										.put("maxOccurs", maxOccurs)
										.put("name", refName.getLocalPart());
								
								addChild(jElement, jRef);
								
								addChild(jParent, jElement);
								parent = jElement;
			        		}
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkGroupRef(xmlSchema, obj);
					
					parent = jParent;
				}
				
				
				@Override
				protected void walkAll(XmlSchema xmlSchema, XmlSchemaAll obj) {
					JSONObject jParent = parent;

					JSONObject jElement = new JSONObject();
					try {
						jElement.put("objType", "allType")
								.put("minOccurs", obj.getMinOccurs())
								.put("maxOccurs", obj.getMaxOccurs());
						
						addChild(jParent, jElement);
						parent = jElement;
						
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkAll(xmlSchema, obj);
					
					parent = jParent;
				}

				@Override
				protected void walkSequence(XmlSchema xmlSchema, XmlSchemaSequence obj) {
					JSONObject jParent = parent;

					JSONObject jElement = new JSONObject();
					try {
						jElement.put("objType", "sequenceType")
								.put("minOccurs", obj.getMinOccurs())
								.put("maxOccurs", obj.getMaxOccurs());
						
						addChild(jParent, jElement);
						parent = jElement;
						
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkSequence(xmlSchema, obj);

					parent = jParent;
				}

				@Override
				protected void walkElement(XmlSchema xmlSchema, XmlSchemaElement obj) {
					JSONObject jParent = parent;
					
					String name = obj.getName();
					
					QName qname = obj.getQName();
					QName refName = obj.getRefName();
					QName typeName = obj.getSchemaTypeName();
					XmlSchemaType xmlSchemaType = obj.getSchemaType();
					
					long minOccurs = obj.getMinOccurs();
					long maxOccurs = obj.getMaxOccurs();
					
					JSONObject jElement = new JSONObject();
					try {
						if (isGlobal(jParent)) {
							jElement.put("objType", "elementType");
						} else {
							jElement.put("objType", "elementType")
									.put("minOccurs", minOccurs)
									.put("maxOccurs", maxOccurs);
						}
						
						
						if (refName == null && typeName == null) {
							// pass though
						} else {
							QName rname = refName != null ? refName : 
								(typeName != null ? typeName: 
								(xmlSchemaType != null ? xmlSchemaType.getQName() : new QName("")));
					
							String ref = getDefinitionRef(rname);
							if (ref != null) {
								if (name.isEmpty()) {
									name = rname.getLocalPart();
								}
								if (ref.indexOf("xsd.jsonschema") != -1) {
									addChild(jElement, new JSONObject().put("type", toOasType(rname.getLocalPart())));
								} else {
									JSONObject refObject = getRefObject(ref);
									addChild(jElement, refObject);
								}
							}
						}
						
						jElement.put("name", name);
						
						if (isGlobal(jParent)) {
							if (qname != null) {
								String id = jsonSchema.getString("id");
								jElement.put("objKey", id + "/definitions/" + normalize(qname.getLocalPart()));
								
								jElement.put("QName",new JSONObject()
										.put("localPart", qname.getLocalPart())
										.put("namespaceURI", qname.getNamespaceURI()));
							}
							
							addGlobalObject(jParent, jElement, name);
							parent = jElement;
						} else {
							addChild(jParent, jElement);
							parent = jElement;
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkElement(xmlSchema, obj);
					
					if (isGlobal(jParent)) {
						//handle(jElement);
					}
					
					parent = jParent;
				}

				@Override
				protected void walkAny(XmlSchema xmlSchema, XmlSchemaAny obj) {
					JSONObject jParent = parent;
					
					JSONObject jElement = new JSONObject();
					try {
						JSONObject value = new JSONObject()
											.put("type", "string")
											.put("description", "any element");
						
						jElement.put("objType", "elementType")
								.put("minOccurs", 1)
								.put("maxOccurs", 1)
								.put("name", "any")
								.put("value", value);
						
						jParent.put("additionalProperties", true);
						
						addChild(jParent, jElement);
						parent = jElement;								
						
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkAny(xmlSchema, obj);
					
					parent = jParent;
				}
				
				@Override
				protected void walkAnyAttribute(XmlSchema xmlSchema, XmlSchemaAnyAttribute obj) {
					JSONObject jParent = parent;
					
					JSONObject jElement = new JSONObject();
					try {
						JSONObject value = new JSONObject()
								.put("type", "string")
								.put("xml", new JSONObject().put("attribute", true))
								.put("description", "any attribute");
			
						jElement.put("objType", "attributeType")
								.put("minOccurs", 0)
								.put("name", "any")
								.put("value", value);
						
						addChild(jParent, jElement);
						parent = jElement;								
						
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkAnyAttribute(xmlSchema, obj);
					
					parent = jParent;
				}
				
				@Override
				protected void walkAppInfo(XmlSchema xmlSchema, XmlSchemaAppInfo item) {
					JSONObject jParent = parent;
					
					try {
						String description = "";
						NodeList nodeList = item.getMarkup();
						for (int i=0; i<nodeList.getLength(); i++) {
							Node node = nodeList.item(i);
							if (node.getNodeType() == Node.TEXT_NODE) {
								description += node.getNodeValue();
							}
						}
						
						if (!description.isEmpty()) {
							addChild(jParent, new JSONObject().put("title", description));
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkAppInfo(xmlSchema, item);
					
					parent = jParent;
				}
				
				@Override
				protected void walkDocumentation(XmlSchema xmlSchema, XmlSchemaDocumentation item) {
					JSONObject jParent = parent;
					
					try {
						String description = "";
						NodeList nodeList = item.getMarkup();
						for (int i=0; i<nodeList.getLength(); i++) {
							Node node = nodeList.item(i);
							if (node.getNodeType() == Node.TEXT_NODE) {
								description += node.getNodeValue();
							}
						}
						
						if (!description.isEmpty()) {
							addChild(jParent, new JSONObject().put("description", description));
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkDocumentation(xmlSchema, item);
					
					parent = jParent;
				}
				
				@Override
				protected void walkAttribute(XmlSchema xmlSchema, XmlSchemaAttribute obj) {
					JSONObject jParent = parent;
					
					String name = obj.getName();
					
					QName qname = obj.getQName();
					QName refName = obj.getRefName();
					QName typeName = obj.getSchemaTypeName();
					XmlSchemaSimpleType xmlSchemaSimpleType = obj.getSchemaType();
					
					boolean isRequired = obj.getUse().equals(XmlSchemaUtils.attributeUseRequired);
					
					JSONObject jElement = new JSONObject();
					try {
						jElement.put("objType", "attributeType")
								.put("minOccurs", isRequired ? 1:0);
						
						if (refName == null && typeName == null) {
							// pass through
						} else {
							QName rname = refName != null ? refName : 
								(typeName != null ? typeName: 
								(xmlSchemaSimpleType != null ? xmlSchemaSimpleType.getQName() : new QName("")));
					
							String ref = getDefinitionRef(rname);
							if (ref != null) {
								if (name.isEmpty()) {
									name = rname.getLocalPart();
								}
								
								JSONObject value = new JSONObject();
								if (ref.indexOf("xsd.jsonschema") != -1) {
									value.put("type", toOasType(rname.getLocalPart()));
									value.put("xml", new JSONObject().put("attribute", true));
								} else {
									JSONObject refObject = getRefObject(ref);
									copyOKeys(refObject, value);
									value.put("xml", new JSONObject().put("attribute", true));
								}
								
								addChild(jElement, value);
							}
						}
						
						jElement.put("name", name);
						
						if (isGlobal(jParent)) {
							if (qname != null) {
								String id = jsonSchema.getString("id");
								jElement.put("objKey", id + "/definitions/" + normalize(qname.getLocalPart()));
								
								jElement.put("QName",new JSONObject()
										.put("localPart", qname.getLocalPart())
										.put("namespaceURI", qname.getNamespaceURI()));
							}
							
							addGlobalObject(jParent, jElement, name);
							parent = jElement;
						} else {
							addChild(jParent, jElement);
							parent = jElement;
						}
						
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkAttribute(xmlSchema, obj);

					if (isGlobal(jParent)) {
						//handle(jElement);
					}
					
					parent = jParent;
				}
				
				@Override
				protected void walkAttributeGroup(XmlSchema xmlSchema, XmlSchemaAttributeGroup obj) {
					JSONObject jParent = parent;
					
					QName qname = obj.getName();
					
					JSONObject jElement = new JSONObject();
					try {
						if (isGlobal(jParent)) {
							if (qname != null) {
								jElement.put("objType", "attributeGroupType");
								
								String id = jsonSchema.getString("id");
								jElement.put("objKey", id + "/definitions/" + normalize(qname.getLocalPart()));
								
								jElement.put("QName",new JSONObject()
										.put("localPart", qname.getLocalPart())
										.put("namespaceURI", qname.getNamespaceURI()));
								
								addGlobalObject(jParent, jElement, qname.getLocalPart());
								parent = jElement;
							}
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkAttributeGroup(xmlSchema, obj);

					if (isGlobal(jParent)) {
						//handle(jElement);
					}
					
					parent = jParent;
				}

				@Override
				protected void walkAttributeGroupRef(XmlSchema xmlSchema, XmlSchemaAttributeGroupRef obj) {
					JSONObject jParent = parent;
					
					QName refName = obj.getRefName();
					
					JSONObject jElement = new JSONObject();
	        		try {
						if (refName != null) {
			        		String ref = getDefinitionRef(refName);
			        		if (ref != null) {
								jElement.put("objType", "attributeGroupType")
										.put("name", refName.getLocalPart());
			        			
								addChild(jElement, new JSONObject().put("$ref", ref));
								
								addChild(jParent, jElement);
								parent = jElement;
			        		}
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkAttributeGroupRef(xmlSchema, obj);
					
					parent = jParent;
				}
				
				@Override
				protected void walkSimpleContent(XmlSchema xmlSchema, XmlSchemaSimpleContent obj) {
					JSONObject jParent = parent;
					
					QName qname = null;
					XmlSchemaContent xmlSchemaContent = obj.getContent();
			        if (xmlSchemaContent instanceof XmlSchemaSimpleContentRestriction) {
			        	qname = ((XmlSchemaSimpleContentRestriction)xmlSchemaContent).getBaseTypeName();
			        } else if (xmlSchemaContent instanceof XmlSchemaSimpleContentExtension) {
			        	qname = ((XmlSchemaSimpleContentExtension)xmlSchemaContent).getBaseTypeName();
			        }
			        
					JSONObject jElement = new JSONObject();
					try {
						if (qname != null) {
							String ref = getDefinitionRef(qname);
							if (ref != null) {
								if (ref.indexOf("xsd.jsonschema") != -1) {
									jElement.put("objType", "elementType")
											.put("minOccurs", 1)
											.put("maxOccurs", 1)
											.put("name", "text")
											.put("value", new JSONObject().put("type", toOasType(qname.getLocalPart())));
								} else {
									JSONObject refObject = getRefObject(ref);
									jElement.put("objType", "elementType")
											.put("minOccurs", 1)
											.put("maxOccurs", 1)
											.put("name", "text")
											.put("value", refObject);
								}
								
								addChild(jParent, jElement);
								parent = jElement;								
							}
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkSimpleContent(xmlSchema, obj);
					
					parent = jParent;
				}
				
				@Override
				protected void walkSimpleType(XmlSchema xmlSchema, XmlSchemaSimpleType obj) {
					JSONObject jParent = parent;
					
					QName qname = obj.getQName();
					QName bname = obj.getBaseSchemaTypeName();
					
					JSONObject jElement = new JSONObject();
					try {
						jElement.put("objType", "simpleType");
						
						if (bname != null) {
							String ref = getDefinitionRef(bname);
							if (ref != null) {
								if (ref.indexOf("xsd.jsonschema") != -1) {
									jElement.put("value", new JSONObject().put("type", toOasType(bname.getLocalPart())));
								} else {
									JSONObject refObject = getRefObject(ref);
									jElement.put("value", refObject);
								}
							}
						}
						
						if (isGlobal(jParent)) {
							if (qname != null) {
								String id = jsonSchema.getString("id");
								jElement.put("objKey", id + "/definitions/" + normalize(qname.getLocalPart()));
								
								jElement.put("QName",new JSONObject()
										.put("localPart", qname.getLocalPart())
										.put("namespaceURI", qname.getNamespaceURI()));
							}
							
							addGlobalObject(jParent, jElement, obj.getName());
							parent = jElement;
						} else {
							addChild(jParent, jElement);
							parent = jElement;
						}
						
					} catch (JSONException e1) {
						e1.printStackTrace();
					}
					
					super.walkSimpleType(xmlSchema, obj);
					
					if (isGlobal(jParent)) {
						//handle(jElement);
					}
					
					parent = jParent;
				}
				
				@Override
				protected void walkSimpleTypeRestriction(XmlSchema xmlSchema, XmlSchemaSimpleTypeRestriction obj) {
					JSONObject jParent = parent;
					
					QName qname = obj.getBaseTypeName();
					
					JSONObject jElement = new JSONObject();
		        	try {
						if (qname != null) {
				        	String ref = getDefinitionRef(qname);
				        	if (ref != null) {
								jElement.put("objType", "simpleRestrictionType");
								
								if (ref.indexOf("xsd.jsonschema") != -1) {
									addChild(jElement, new JSONObject().put("type", toOasType(qname.getLocalPart())));
								} else {
									JSONObject refObject = getRefObject(ref);
									addChild(jElement, refObject);
								}
								
								addChild(jParent, jElement);
								parent = jElement;								
				        	}
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkSimpleTypeRestriction(xmlSchema, obj);
					
					parent = jParent;
				}
				
				@Override
				protected void walkSimpleTypeUnion(XmlSchema xmlSchema, XmlSchemaSimpleTypeUnion obj) {
					JSONObject jParent = parent;
					
					JSONObject jElement = new JSONObject();
					try {
						jElement.put("objType", "simpleUnionType");

						QName[] members = obj.getMemberTypesQNames();
						if (members != null) {
							for (QName qname : members) {
				        		String ref = getDefinitionRef(qname);
				        		if (ref.indexOf("xsd.jsonschema") != -1) {
				        			addChild(jElement, new JSONObject().put("type", toOasType(qname.getLocalPart())));
				        		} else {
				        			parent = jElement;
				        			walkByTypeName(xmlSchema, qname);
				        		}
							}
						} else {
							parent = jElement;
							super.walkSimpleTypeUnion(xmlSchema, obj);
						}
						
						addChild(jParent, jElement);						
						
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					parent = jParent;
				}
				
				@Override
				protected void walkFacets(XmlSchema xmlSchema, XmlSchemaObjectCollection facets) {
					JSONObject jParent = parent;
					
					JSONArray array = new JSONArray();
	        		boolean arrayWithDuplicates = false;
					for (int i = 0; i < facets.getCount(); i++) {
			        	XmlSchemaFacet facet = (XmlSchemaFacet) facets.getItem(i);
			        	Object value = facet.getValue();
			        	try {
			        		JSONObject jFacet = new JSONObject();
			        		
			        		if (facet instanceof XmlSchemaEnumerationFacet) {
			        			if ("".equals(value)) {
			        				jFacet.put("minLength", 0);
			        				array.put(value);
			        				array.put(JSONObject.NULL);
			        			} else {
			        				if (array.join(",").toLowerCase().indexOf(String.valueOf(value).toLowerCase()) != -1) {
			        					arrayWithDuplicates = true;
			        				}
			        				array.put(value);
			        			}
			        			if (i < facets.getCount() - 1) {
			        				continue;
			        			}
							} else if (facet instanceof XmlSchemaPatternFacet) {
								jFacet.put("pattern", value);
							} else if (facet instanceof XmlSchemaLengthFacet) {
								jFacet.put("length", Integer.valueOf(value.toString(), 10));
							} else if (facet instanceof XmlSchemaMinLengthFacet) {
								jFacet.put("minLength", Integer.valueOf(value.toString(), 10));
							} else if (facet instanceof XmlSchemaMaxLengthFacet) {
								jFacet.put("maxLength", Integer.valueOf(value.toString(), 10));
							} else if (facet instanceof XmlSchemaMaxExclusiveFacet) {
								jFacet.put("maximum", Integer.valueOf(value.toString(), 10)).put("exclusiveMaximum", true);
							} else if (facet instanceof XmlSchemaMaxInclusiveFacet) {
								jFacet.put("maximum", Integer.valueOf(value.toString(), 10)).put("exclusiveMaximum", false);
							} else if (facet instanceof XmlSchemaMinExclusiveFacet) {
								jFacet.put("minimum", Integer.valueOf(value.toString(), 10)).put("exclusiveMinimum", true);
							} else if (facet instanceof XmlSchemaMinInclusiveFacet) {
								jFacet.put("minimum", Integer.valueOf(value.toString(), 10)).put("exclusiveMinimum", false);
							} else if (facet instanceof XmlSchemaTotalDigitsFacet) {
								jFacet.put("maxLength", Integer.valueOf(value.toString(), 10));
							}

			        		if (facet instanceof XmlSchemaEnumerationFacet) {
			        			if (array.length() > 0) {
			        				jFacet.put("nullable", true);
			        				if (facets.getCount() > 1) {
			        					if (arrayWithDuplicates) {
			        						jFacet.put("pattern", array.join("|"));
			        					} else {
			        						jFacet.put("enum", array);
			        					}
			        				}
			        			}
			        		}
			        		
			        		if (jFacet.length() > 0) {
			        			addChild(jParent, jFacet);
			        		}
			        		
				        	parent = jFacet;
				        	
						} catch (JSONException e) {
							e.printStackTrace();
						}
			        }
					
					parent = jParent;
				}
				
				@Override
				protected void walkSimpleTypeList(XmlSchema xmlSchema, XmlSchemaSimpleTypeList obj) {
					JSONObject jParent = parent;
					
					QName qname = obj.getItemTypeName();
					
					JSONObject jElement = new JSONObject();
		        	try {
						if (qname != null) {
				        	String ref = getDefinitionRef(qname);
				        	if (ref != null) {
								JSONObject value = new JSONObject();
								
								if (ref.indexOf("xsd.jsonschema") != -1) {
									value.put("type", "array")
									.put("items", new JSONObject().put("type", toOasType(qname.getLocalPart())));
								} else {
									JSONObject refObject = getRefObject(ref);
									value.put("type", "array")
											.put("items", refObject);
								}
								
								jElement.put("objType", "simpleListType")
										.put("value", value);
								
								addChild(jParent, jElement);
								parent = jElement;								
				        	}
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkSimpleTypeList(xmlSchema, obj);
					
					parent = jParent;
				}
				
				@Override
				protected void walkComplexContentExtension(XmlSchema xmlSchema, XmlSchemaComplexContentExtension obj) {
					JSONObject jParent = parent;
					
					QName baseTypeName = obj.getBaseTypeName();
					
					JSONObject jElement = new JSONObject();
	        		try {
						if (baseTypeName != null) {
			        		String ref = getDefinitionRef(baseTypeName);
			        		if (ref != null) {
								jElement.put("objType", "complexContentExtensionType");
								addChild(jElement, new JSONObject().put("$ref", ref));
								
								addChild(jParent, jElement);
								parent = jElement;								
			        		}
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkComplexContentExtension(xmlSchema, obj);
					
					parent = jParent;
				}
				
				@Override
				protected void walkComplexContentRestriction(XmlSchema xmlSchema, XmlSchemaComplexContentRestriction obj) {
					JSONObject jParent = parent;
					
					QName baseTypeName = obj.getBaseTypeName();
					
					JSONObject jElement = new JSONObject();
	        		try {
						if (baseTypeName != null) {
			        		String ref = getDefinitionRef(baseTypeName);
			        		if (ref != null) {
								jElement.put("objType", "complexContentRestrictionType");
								addChild(jElement, new JSONObject().put("$ref", ref));
								
								addChild(jParent, jElement);
								parent = jElement;								
			        		}
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkComplexContentRestriction(xmlSchema, obj);
					
					parent = jParent;
				}
				
				@Override
				protected void walkComplexType(XmlSchema xmlSchema, XmlSchemaComplexType obj) {
					JSONObject jParent = parent;
					
					QName qname = obj.getQName();
					
					JSONObject jElement = new JSONObject();
					try {
						jElement.put("objType", "complexType");
						
						if (obj.isMixed()) {
							JSONObject jText = new JSONObject();
							jText.put("objType", "elementType")
									.put("name", "text")
									.put("minOccurs", 0)
									.put("maxOccurs", 1)
									.put("value", new JSONObject()
													.put("description", "the mixed content string")
													.put("type", "string"));
							
							addChild(jElement, jText);
						}
						
						if (isGlobal(jParent)) {
							if (qname != null) {
								String id = jsonSchema.getString("id");
								jElement.put("objKey", id + "/definitions/" + normalize(qname.getLocalPart()));
								
								jElement.put("QName",new JSONObject()
										.put("localPart", qname.getLocalPart())
										.put("namespaceURI", qname.getNamespaceURI()));
							}
							
							addGlobalObject(jParent, jElement, obj.getName());
							parent = jElement;
						} else {
							addChild(jParent, jElement);
							parent = jElement;
						}
						
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					super.walkComplexType(xmlSchema, obj);
					
					if (isGlobal(jParent)) {
						//handle(jElement);
					}
					
					parent = jParent;
				}

				@Override
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
					
					// walk simple types first
					for (Iterator<XmlSchemaType> i = GenericUtils.cast(xmlSchema.getSchemaTypes().getValues()); i.hasNext(); ) {
						XmlSchemaObject obj = i.next();
						if (obj instanceof XmlSchemaSimpleType) {
							walk(xmlSchema, obj);
						}
					}
					
					// walk others
					for (Iterator<XmlSchemaType> i = GenericUtils.cast(xmlSchema.getSchemaTypes().getValues()); i.hasNext(); ) {
						XmlSchemaObject obj = i.next();
						if (!(obj instanceof XmlSchemaSimpleType)) {
							walk(xmlSchema, obj);
						}
					}
					for (Iterator<XmlSchemaObject> i = GenericUtils.cast(items.getIterator()); i.hasNext(); ) {
						XmlSchemaObject obj = i.next();
						if (!(obj instanceof XmlSchemaInclude || obj instanceof XmlSchemaImport || obj instanceof XmlSchemaType)) {
							walk(xmlSchema, obj);
						}
					}
					
					List<String> toRemove = new ArrayList<String>();
					@SuppressWarnings("unchecked")
					Iterator<String> it = definitions.keys();
					while (it.hasNext()) {
						String pkey = it.next();
						try {
							JSONObject jsonOb = definitions.getJSONObject(pkey);
							handle(jsonOb);
							
							JSONObject value = jsonOb.getJSONObject("value");
							if (value.has("properties") || value.has("allOf") || value.has("$ref")) {
								jsonOb.put("type", "object");
								handleRefs(value);
							}
							copyOKeys(value, jsonOb);
							
							if (jsonOb.has("type")) {
								if (!jsonOb.getString("type").equals("object")) {
									toRemove.add(pkey);
								}
							}
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
					for (String pkey: toRemove) {
						definitions.remove(pkey);
					}
					
					boolean debug = false;
					if (!debug) {
						try {
							@SuppressWarnings("unchecked")
							Iterator<String> ita = definitions.keys();
							while (ita.hasNext()) {
								JSONObject jsonOb = definitions.getJSONObject(ita.next());
								jsonOb.remove("objKey");
								jsonOb.remove("objType");
								jsonOb.remove("QName");
								jsonOb.remove("children");
								jsonOb.remove("name");
								jsonOb.remove("maxOccurs");
								jsonOb.remove("minOccurs");
								jsonOb.remove("value");
							}
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
					
					refs.clear();
				}
			}.walk(xmlSchema);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
				
		return jsonSchema;
	}
	
}
