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
package com.twinsoft.convertigo.beans.transactions.couchdb;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Vector;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XmlQName;
import com.twinsoft.convertigo.beans.connectors.CouchDbConnector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IComplexTypeAffectation;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EngineStatistics;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchClient;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public abstract class AbstractCouchDbTransaction extends TransactionWithVariables implements IComplexTypeAffectation {

	private static final long serialVersionUID = 8218411805775719448L;
	
	public static final String COUCHDB_XSD_NAMESPACE = "http://www.convertigo.com/convertigo/couchdb";
	public static final String COUCHDB_XSD_LOCATION = "http://localhost:18080/convertigo/xsd/couchdb/CouchDb.xsd";
	
	private XmlQName xmlComplexTypeAffectation = new XmlQName();
	
	public AbstractCouchDbTransaction() {
		super();
	}

	@Override
	public AbstractCouchDbTransaction clone() throws CloneNotSupportedException {
		AbstractCouchDbTransaction clonedObject =  (AbstractCouchDbTransaction) super.clone();
		return clonedObject;
	}
	
	@Override
	public CouchDbConnector getConnector() {
		return (CouchDbConnector) super.getConnector();
	}
	
	@Override
	public void setStatisticsOfRequestFromCache() {
		// TODO Auto-generated method stub
	}

	public abstract List<CouchDbParameter> getDeclaredParameters();
	
	public void createVariables() {
		try {
			for (CouchDbParameter param : getDeclaredParameters()) {
				if (getVariable(param.variableName()) == null) {
					addVariable(CouchDbParameter.create(param.name()));
					hasChanged = true;
				}
			}
		}
		catch (Exception e) {
			Engine.logBeans.error("Unable to add needed variable(s) for CouchDbTransaction", e);
		}
	}
	
	@Override
	public void remove(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof RequestableVariable) {
			RequestableVariable variable = (RequestableVariable) databaseObject;
			
			if (CouchDbParameter.contains(getDeclaredParameters(), variable.getName())) {
				throw new EngineException("You are not allowed to remove the variable named '" + variable.getName() + "'");
			}
		}
		super.remove(databaseObject);
	}

	@Override
	public void runCore() throws EngineException {
		Object result = null;
		try {
            String t = context.statistics.start(EngineStatistics.APPLY_USER_REQUEST);
            
            try {
            	getConnector().beforeTransactionInvoke();
            	
    			Engine.logBeans.debug("(CouchDBTransaction) Retrieving data...");
    			result = invoke();
    			Engine.logBeans.debug("(CouchDBTransaction) Data retrieved!");			
            }
            finally {
            	getConnector().afterTransactionInvoke();
            	
                context.statistics.stop(t);
            }

			// Update Studio connector's editor
			if (Engine.isStudioMode()) {
				try {
					getConnector().setData(result.toString());
				} catch (Exception e) {}
			}
			
			// Applying the underlying process
			makeDocument(result);
			
			score +=1;
			
		}
		catch (Throwable t) {
			getConnector().setData(null);
			throw new EngineException("An error occured while running the transaction", t);
		}
	}
	
	protected abstract Object invoke() throws Exception;

	protected CouchClient getCouchClient() {
		return getConnector().getCouchClient();
	}
	
	protected void makeDocument(Object result) throws Exception {
		if (result == null) return;
		Engine.logBeans.debug("(CouchDBTransaction) making document...");
		Document doc = context.outputDocument;
		Element root = doc.getDocumentElement();
		
		if (result instanceof JSONObject) {
			JSONObject jsonResult = (JSONObject) result;
			Element couchdb_output = doc.createElement("couchdb_output");
			toXml(jsonResult, couchdb_output);
			root.appendChild(couchdb_output);
		}
		
		Engine.logBeans.debug("(CouchDBTransaction) Document generated!");
	}

	public Object getParameterValue(CouchDbParameter param) {
		return super.getParameterValue(param.variableName());
	}

	public String getParameterStringValue(CouchDbParameter param) {
		return super.getParameterStringValue(param.variableName());
	}
	
	protected static void addJson(JSONObject jsonObject, String propertyName, Object jsonElement) {
		if (jsonElement != null) {
			if (jsonElement instanceof JSONObject) { // comes from a complex variable
				JSONObject json = (JSONObject) jsonElement;
				for (Iterator<String> i = GenericUtils.cast(json.keys()); i.hasNext(); ) {
					String key = i.next();
					try {
						jsonObject.put(key, json.get(key));
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} else { // comes from a simple variable
				try {
					jsonObject.put(propertyName, jsonElement);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	protected static Object toJson(Object object) throws JSONException {
		if (object == null) return null;
		
		Object jsonElement = null;
		
		if (object instanceof NativeObject) {
			JSONObject jsonChildren = new JSONObject();
			NativeObject nativeObject = (NativeObject) object;
			for (Iterator<Entry<Object, Object>> it = GenericUtils.cast(nativeObject.entrySet().iterator()); it.hasNext();) {
				Entry<Object, Object> entry = it.next();
				jsonChildren.put(entry.getKey().toString(), toJson(entry.getValue()));
			}
			return jsonChildren;
		}
		if (object instanceof NativeJavaObject) {
			NativeJavaObject nativeJavaObject = (NativeJavaObject) object;
			return toJson(nativeJavaObject.unwrap());
		}
		else if (object instanceof NativeJavaArray) {
			Object ob = ((NativeJavaArray) object).unwrap();
			return toJson(Arrays.asList((Object[])ob));
		}
		else if (object instanceof NativeArray) {
			NativeArray array = (NativeArray) object;
			JSONArray jsonArray = new JSONArray();
			for (int j=0; j<array.getLength(); j++) {
				jsonArray.put(toJson(array.get(j,array)));
			}
			jsonElement = jsonArray;
		}
		else if ((object instanceof org.mozilla.javascript.Scriptable)) {
			org.mozilla.javascript.Scriptable jsObj = (org.mozilla.javascript.Scriptable) object;
		    return toJson(String.valueOf(jsObj.toString()));
		} else if (object.getClass().isArray()) {
			return toJson(Arrays.asList((Object[])object));
		}
		else if (object instanceof Collection<?>) {
			JSONArray jsonArray = new JSONArray();
			for (Object o : (Collection<?>) object) {
				jsonArray.put(toJson(o));
			}
			jsonElement = jsonArray;
		}
		else if (object instanceof Element) {
			jsonElement = toJson((Element) object);
		}
		else {
			jsonElement = object;
		}
		
		return jsonElement;
	}
	
	protected List<CouchDbParameter> getDeclaredParameters(CouchDbParameter... parameters) {
		return getConnector().filter(parameters);
	}
	
	private static JSONObject toJson(Element element) throws JSONException {
		JSONObject jsonXml = new JSONObject();
		XMLUtils.handleElement(element, jsonXml, true);
		if (isInputDomVariable(element)) {
			JSONObject jsonVariable = jsonXml.getJSONObject("variable");
			JSONObject jsonAttr = jsonVariable.getJSONObject("attr");
			String jsonAttrName = jsonAttr.getString("name");
			String jsonAttrValue;
			
			// Complex variable will cause exception as they have no value... 
			try {
				jsonAttrValue = jsonAttr.getString("value");
			}  catch (JSONException eValue) {
				jsonAttrValue = null;
			}

			// this is a simple variable
			if (jsonAttrValue != null) {
				JSONObject jsonobject = new JSONObject();
				jsonobject.put(jsonAttrName, jsonAttrValue);
				return jsonobject;
			}
			// this is a complex variable
			else {
				jsonVariable.remove("attr");
				return jsonVariable;
			}
		}
		return jsonXml;
	}
	
	private static boolean isInputDomVariable(Object object) {
		if (object == null) return false;
		
		if (object instanceof Element) {
			Element element = (Element)object;
			return element.getParentNode().getNodeName().equals("transaction-variables");
		}
		if (object instanceof Vector) {
			Vector<Object> v = GenericUtils.cast(object);
			return isInputDomVariable(v.isEmpty() ? false : v.firstElement());
		}
		if (object instanceof NodeList) {
			NodeList l = (NodeList)object;
			return isInputDomVariable(l.getLength() == 0 ? false : l.item(0));
		}
		return false;
	}
	
	public static void toXml(Object object, Element parentElement) {
		Document doc = parentElement.getOwnerDocument();
		if (object instanceof JSONObject) {
			JSONObject jsonObject = (JSONObject) object;
			String[] keys = new String[jsonObject.length()];
			
			int index = 0;
			for (Iterator<String> i = GenericUtils.cast(jsonObject.keys()); i.hasNext();) {
				keys[index++] = i.next();
			}
			
			Arrays.sort(keys);
			
			for (String key: keys) {
				try {
					toXml(key, jsonObject.get(key), parentElement);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		else if (object instanceof JSONArray) {
			JSONArray jsonArray = (JSONArray) object;
			for (int i = 0; i < jsonArray.length(); i++) {
				Element item = doc.createElement("item");
				parentElement.appendChild(item);
				try {
					toXml(jsonArray.get(i), item);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		else if (object != null && object != JSONObject.NULL) {
			parentElement.setTextContent(object.toString());
		}
	}
	
	private static void toXml(String key, Object value, Element parentElement) {
		if (key == null || "".equals(key)) {
			key = "object";
		}
		
		if ("_attachments".equals(parentElement.getNodeName())) {
			Element att = parentElement.getOwnerDocument().createElement("attachment");
			Element att_name = parentElement.getOwnerDocument().createElement("name");
			att_name.setTextContent(key);
			att.appendChild(att_name);
			parentElement.appendChild(att);
			toXml(value, att);
		}
		else {
			String normalisedKey = StringUtils.normalize(key);
			Element child = parentElement.getOwnerDocument().createElement(normalisedKey);
			parentElement.appendChild(child);
			toXml(value, child);
		}
	}
	
	@Override
	protected String extractXsdType(Document document) throws Exception {
		return generateWsdlType(document);
	}

	@Override
	public String generateWsdlType(Document document) throws Exception {
		String xsdType = "<xsd:complexType name=\""+ getXsdResponseElementName() +"\" />\n";
		try {
    		XmlSchema xmlSchema = createSchema();
    		Document doc = xmlSchema.getSchemaDocument();
    		Element elt = XMLUtils.findNodeByAttributeValue(doc.getDocumentElement().getChildNodes(), "name", getXsdResponseTypeName());
   			xsdType = XMLUtils.prettyPrintElement(elt, true, true);
		}
		catch (Exception e) {
			Engine.logBeans.error("Unable to generate schema for CouchDbTransaction named '"+ getName() +"'", e);
		}
		return xsdType;
	}

	@Override
	protected XmlSchemaComplexType addSchemaResponseDataType(XmlSchema xmlSchema) {
		XmlSchemaComplexType xmlSchemaComplexType = super.addSchemaResponseDataType(xmlSchema);
		XmlSchemaSequence xmlSchemaSequence = (XmlSchemaSequence)xmlSchemaComplexType.getParticle();
		if (xmlSchemaSequence == null) xmlSchemaSequence = new XmlSchemaSequence();
		try {
			XmlSchemaElement couchdb_output = new XmlSchemaElement();
			couchdb_output.setName("couchdb_output");
			couchdb_output.setSchemaTypeName(getComplexTypeAffectation());
			xmlSchemaSequence.getItems().add(couchdb_output);
		} catch (Exception e) {
			Engine.logBeans.error("Unable to generate response schema for CouchDbTransaction named '"+ getName() +"'", e);
		}
		xmlSchemaComplexType.setParticle(xmlSchemaSequence);
		return xmlSchemaComplexType;
	}

	public XmlQName getXmlComplexTypeAffectation() {
		return xmlComplexTypeAffectation;
	}

	public void setXmlComplexTypeAffectation(XmlQName xmlComplexTypeAffectation) {
		this.xmlComplexTypeAffectation = xmlComplexTypeAffectation;
	}
	
	public QName getComplexTypeAffectation() {
		return getXmlComplexTypeAffectation().getQName();
	}
	
}
