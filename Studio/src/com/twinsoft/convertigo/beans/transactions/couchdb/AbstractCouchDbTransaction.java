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

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.codehaus.jettison.json.JSONException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.twinsoft.convertigo.beans.common.XmlQName;
import com.twinsoft.convertigo.beans.connectors.CouchDbConnector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IComplexTypeAffectation;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EngineStatistics;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchDbProvider;
import com.twinsoft.convertigo.engine.providers.couchdb.api.Context;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public abstract class AbstractCouchDbTransaction extends TransactionWithVariables implements IComplexTypeAffectation {

	private static final long serialVersionUID = 8218411805775719448L;
	
	public static final String COUCHDB_XSD_NAMESPACE = "http://www.convertigo.com/convertigo/couchdb";
	public static final String COUCHDB_XSD_LOCATION = "http://localhost:18080/convertigo/xsd/couchdb/CouchDb.xsd";
	
	private XmlQName xmlComplexTypeAffectation = new XmlQName();
	
	protected transient CouchDbConnector connector = null;
	
	public AbstractCouchDbTransaction() {
		super();
	}

	@Override
	public AbstractCouchDbTransaction clone() throws CloneNotSupportedException {
		AbstractCouchDbTransaction clonedObject =  (AbstractCouchDbTransaction) super.clone();
		clonedObject.connector = null;
		return clonedObject;
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
			RequestableVariable variable = (RequestableVariable)databaseObject;
			if (CouchDbParameter.contains(getDeclaredParameters(),variable.getName())) {
				throw new EngineException("You are not allowed to remove the variable named '"+variable.getName()+"'");
			}
		}
		super.remove(databaseObject);
	}

	@Override
	public void runCore() throws EngineException {
		connector = (CouchDbConnector)parent;
		Object result = null;
		try {
            String t = context.statistics.start(EngineStatistics.APPLY_USER_REQUEST);
            
            try {
    			Engine.logBeans.debug("(CouchDBTransaction) Retrieving data...");
    			result = invoke();
    			Engine.logBeans.debug("(CouchDBTransaction) Data retrieved!");			
            }
            finally {
                context.statistics.stop(t);
            }

			// Update Studio connector's editor
			if (Engine.isStudioMode()) {
				try {
					connector.setData(result.toString());
				} catch (Exception e) {}
			}
			
			// Applying the underlying process
			makeDocument(result);
			
			score +=1;
			
		}
		catch (Throwable t) {
			connector.setData(null);
			throw new EngineException("An error occured while running the transaction", t);
		}
	}
	
	protected abstract Object invoke() throws Exception;

	protected CouchDbProvider getCouchDbClient() {
		return connector.getCouchDbClient();
	}
	
	protected Gson getGson() {
		return getCouchDbClient().getGson();
	}
	
	protected Context getCouchDbContext() {
		return getCouchDbClient().context();
	}
	
	protected void makeDocument(Object result) throws Exception {
		if (result == null) return;
		Engine.logBeans.debug("(CouchDBTransaction) making document...");
		Document doc = context.outputDocument;
		Element root = doc.getDocumentElement();
		if (result instanceof JsonElement) {
			JsonElement jsonResult = (JsonElement)result;
			Element couchdb_output = doc.createElement("couchdb_output");
			toXml(jsonResult, couchdb_output);
			root.appendChild(couchdb_output);
		}
		Engine.logBeans.debug("(CouchDBTransaction) Document generated!");
	}

	public Object getParameterValue(CouchDbParameter param) {
		return super.getParameterValue(param.variableName());
	}
	
	protected static JsonElement toJson(Gson gson, JsonParser parser, Object object) throws JsonSyntaxException, JSONException {
		if (object == null) return null;
		
		JsonElement jsonElement = null;
		
		if (object instanceof Vector) {
			Vector<Object> v = GenericUtils.cast(object);
			JsonArray jsonArray = new JsonArray();
			for (int i = 0; i< v.size(); i++) {
				jsonArray.add(toJson(gson, parser, v.get(i)));
			}
			jsonElement = jsonArray;
		}
		else if (object instanceof Element) {
			jsonElement = toJson(parser, (Element)object);
		}
		/*else if (object instanceof String) {
			jsonElement = parser.parse(object.toString());
		}*/
		else {
			jsonElement = gson.toJsonTree(object);
		}
		
		return jsonElement;
	}
	
	private static JsonElement toJson(JsonParser parser, Element element) throws JsonSyntaxException, JSONException {
		JsonElement jsonXml = parser.parse(XMLUtils.XmlToJson(element, true));
		if (isInputDomVariable(element)) {
			JsonObject jsonVariable = jsonXml.getAsJsonObject().get("variable").getAsJsonObject();
			JsonObject jsonAttr = jsonVariable.get("attr").getAsJsonObject();
			JsonElement jsonAttrValue = jsonAttr.get("value");
			
			// this is a simple variable
			if (jsonAttrValue != null) {
				return jsonAttrValue;
			}
			// this is a complex variable
			else {
				jsonVariable.remove("attr");
				JsonObject jsonChildren = new JsonObject();
				Set<Entry<String, JsonElement>> set = jsonVariable.entrySet();
				for (Iterator<Entry<String, JsonElement>> it = GenericUtils.cast(set.iterator()); it.hasNext();) {
					Entry<String, JsonElement> entry = it.next();
					jsonChildren.add(entry.getKey(), entry.getValue());
				}
				return jsonChildren;
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
	
	private static void toXml(Entry<String, JsonElement> entry, Element parentElement) {
		String key = entry.getKey();
		if (key == null || "".equals(key)) {
			key = "object";
		}
		
		if ("_attachments".equals(parentElement.getNodeName())) {
			Element att = parentElement.getOwnerDocument().createElement("attachment");
			Element att_name = parentElement.getOwnerDocument().createElement("name");
			Text att_txt = parentElement.getOwnerDocument().createTextNode(key);
			att_name.appendChild(att_txt);
			att.appendChild(att_name);
			parentElement.appendChild(att);
			toXml(entry.getValue(), att);
		}
		else {
			String normalisedKey = StringUtils.normalize(key);
			Element child = parentElement.getOwnerDocument().createElement(normalisedKey);
			parentElement.appendChild(child);
			toXml(entry.getValue(), child);
		}
	}
	
	private static void toXml(JsonElement jsone, Element parentElement) {
		Document doc = parentElement.getOwnerDocument();
		if (jsone.isJsonObject()) {
			JsonObject jsonObject = (JsonObject)jsone;
			Set<Entry<String, JsonElement>> set = jsonObject.entrySet();
			for (Iterator<Entry<String, JsonElement>> it = GenericUtils.cast(set.iterator()); it.hasNext();) {
				toXml(it.next(), parentElement);
			}
		}
		else if (jsone.isJsonArray()) {
			JsonArray jsonArray = (JsonArray)jsone;
			for (Iterator<JsonElement> i = GenericUtils.cast(jsonArray.iterator()); i.hasNext();) {
				Element item = doc.createElement("item");
				parentElement.appendChild(item);
				toXml(i.next(), item);
			}
		}
		else if (jsone.isJsonPrimitive()) {
			JsonPrimitive jsonPrimitive = (JsonPrimitive)jsone;
			String jsonString = jsonPrimitive.getAsString();
			Text text = doc.createTextNode(decode(jsonString));
			parentElement.appendChild(text);
		}
		else if (jsone.isJsonNull()) {
		}
		else {
		}
	}
	
	protected static String encode(String jsonString) {
		try {
			return new String(jsonString.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return jsonString;
	}
	
	protected static String decode(String jsonString) {
		try {
			return new String(jsonString.getBytes(),"UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return jsonString;
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
