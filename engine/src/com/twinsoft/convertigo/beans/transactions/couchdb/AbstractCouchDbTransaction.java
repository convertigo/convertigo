/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.beans.transactions.couchdb;

import java.io.File;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.constants.Constants;
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
import com.twinsoft.convertigo.beans.core.IComplexTypeAffectation;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.variables.RequestableMultiValuedVariable;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EngineStatistics;
import com.twinsoft.convertigo.engine.enums.CouchParam;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchClient;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.RhinoUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public abstract class AbstractCouchDbTransaction extends TransactionWithVariables implements IComplexTypeAffectation {

	private static final long serialVersionUID = 8218411805775719448L;
	private static final String defaultSchemaMark = "<default/>";
	
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
	
	public void createVariables(List<CouchVariable> selectedParameters) {
		try {
			if (selectedParameters != null) {
				for (CouchVariable variable : selectedParameters) {
					String varName = variable.getName();
					String varNameNew = (varName.startsWith("p_") || varName.startsWith("q_") ? CouchParam.prefix + varName.substring(2) : varName );
					String varDesc = variable.getDescription();
					if (getVariable(varNameNew) == null) {
						RequestableVariable requestableVariable = null;
						if (variable.isMultiValued()) {
							requestableVariable = new RequestableMultiValuedVariable();

						} else {
							requestableVariable = new RequestableVariable();
						}
						requestableVariable.setName(varNameNew);
						requestableVariable.setDescription(varDesc);
						
						addVariable(requestableVariable);

						hasChanged = true;
					}
				}
			}
		}
		catch (Exception e) {
			Engine.logBeans.error("Unable to add needed variable(s) for CouchDbTransaction", e);
		}
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
//			Element couchdb_output = doc.createElement("couchdb_output");
			XMLUtils.jsonToXml(jsonResult, "couchdb_output", root, getConnector().isJsonUseType(), false, "item");
//			XMLUtils.JsonToXml(jsonResult, couchdb_output);
//			root.appendChild(couchdb_output);
		}
		
		Engine.logBeans.debug("(CouchDBTransaction) Document generated!");
	}

	public Class<?> getParameterDataTypeClass(String parameterName) {
		try {
			QName typeQName = ((RequestableVariable) getVariable(parameterName)).getTypeAffectation();
			if (typeQName.getNamespaceURI().equals(Constants.URI_2001_SCHEMA_XSD)) {
				if (typeQName.equals(Constants.XSD_BOOLEAN)) {
					return Boolean.class;
				}
				else if (typeQName.equals(Constants.XSD_BYTE)) {
					return Byte.class;
				}
				else if (typeQName.equals(Constants.XSD_SHORT) ||
						typeQName.equals(Constants.XSD_UNSIGNEDBYTE)) {
					return Short.class;
				}
				else if (typeQName.equals(Constants.XSD_INT) ||
						typeQName.equals(Constants.XSD_UNSIGNEDSHORT)) {
					return Integer.class;
				}
				else if (typeQName.equals(Constants.XSD_LONG) ||
						typeQName.equals(Constants.XSD_UNSIGNEDINT)) {
					return Long.class;
				}
				else if (typeQName.equals(Constants.XSD_DOUBLE)) {
					return Double.class;
				}
				else if (typeQName.equals(Constants.XSD_FLOAT)) {
					return Float.class;
				}
				else if (typeQName.equals(Constants.XSD_INTEGER) || 
						typeQName.equals(Constants.XSD_POSITIVEINTEGER) ||
						typeQName.equals(Constants.XSD_NEGATIVEINTEGER) ||
						typeQName.equals(Constants.XSD_NONNEGATIVEINTEGER) ||
						typeQName.equals(Constants.XSD_NONPOSITIVEINTEGER)) {
					return BigInteger.class;
				}
				else if (typeQName.equals(Constants.XSD_DECIMAL) ||
						typeQName.equals(Constants.XSD_UNSIGNEDLONG)) {
					return BigDecimal.class;
				}
				else if (typeQName.equals(Constants.XSD_STRING)) {
					return String.class;
				}
			}
		}
		catch (Exception e) {}
		return null;
	}
	
	public Object getEvaluatedParam(org.mozilla.javascript.Context cx, CouchParam param) throws Exception {
		String str = getParameterStringValue(param);
		if (str.isEmpty()) {
			throw new EngineException("empty parameter");
		}
		try {
			Object o = RhinoUtils.evalCachedJavascript(this, cx, scope, "(" + str + ")", param.name(), 1, null);
			o = toJson(o);
			return o;
		} catch (Exception e) {}
		return str;
	}
	
	public void putEvaluatedParam(org.mozilla.javascript.Context cx, JSONObject json, CouchParam param) {
		try {
			Object o = getEvaluatedParam(cx, param);
			json.put(param.name(), o);
		} catch (Exception e) { }
	}
	
	public void putBooleanParam(JSONObject json, CouchParam param) {
		try {
			String str = getParameterStringValue(param);
			if (!str.isEmpty()) {
				json.put(param.name(), Boolean.parseBoolean(str));
			}
		} catch (Exception e) {
		}
	}
	
	public void putLongParam(JSONObject json, CouchParam param) {
		try {
			String str = getParameterStringValue(param);
			if (!str.isEmpty()) {
				json.put(param.name(), Long.parseLong(str));
			}
		} catch (Exception e) {
		}
	}
	
	public void putStringParam(JSONObject json, CouchParam param) {
		try {
			String str = getParameterStringValue(param);
			if (!str.isEmpty()) {
				json.put(param.name(), str);
			}
		} catch (Exception e) {
		}
	}
	
	public String getParameterStringValue(CouchParam param) throws EngineException {
		String value = null;
		Variable variable = getVariable(param.param());
		
		if (variable != null) {
			value = getParameterStringValue(param.param());
		}
		
		if (value == null) {
			try {
				value = (String) getClass().getMethod("getP_" + param.name()).invoke(this);
			} catch (Exception e) {
				//TODO: handle error
				e.printStackTrace();
			}
		}
		return value;
	}
	
	public boolean getParameterBooleanValue(CouchParam param, boolean def) throws EngineException {
		String value = getParameterStringValue(param);
		if ("true".equalsIgnoreCase(value)) {
			return true;
		} else if ("false".equalsIgnoreCase(value)) {
			return false;
		} else {
			return def;
		}
	}
	
	public Map<String, String> getQueryVariableValues() {
		Map<String, String> map = new HashMap<String, String>();
		
		try {
			for (Method method: getClass().getMethods()) {
				if (method.getName().startsWith("getQ_")) {
					String name = method.getName().substring(5);
					String value = (String) method.invoke(this);
					
					if (value != null && !value.isEmpty()) {
						map.put(name, value);
					}
				}
			}
			
			for (RequestableVariable variable: getAllVariables()) {
				if (variable.getName().startsWith(CouchParam.prefix)) {
					String name = variable.getName().substring(CouchParam.prefix.length());
					try {
						getClass().getMethod("getP_" + name);
					} catch (Throwable t) {
						String value;
						if (variable.isMultiValued()) {
							Object o = getParameterValue(variable.getName());
							if (o instanceof Collection) {
								JSONArray array = new JSONArray();
								for (Object i : ((Collection<?>) o)) {
									array.put(i);
								}
								value = array.toString();
							} else {
								value = o == null ? "" : o.toString();
							}
						} else {
							value = getParameterStringValue(variable.getName());
						}
						
						if (value != null && !value.isEmpty()) {
							map.put(name, value);
						}
					}
				}
			}
		} catch (Exception e) {
			//TODO: handle error
			e.printStackTrace();
		}
		
		return map;
	}
	
	public JSONObject getJsonBody(JSONObject jsonDocument) throws JSONException, EngineException {
		// add document members from variables
		for (RequestableVariable variable: getAllVariables()) {
			String variableName = variable.getName();
			if (!variableName.startsWith("__") && !variableName.startsWith(CouchParam.prefix)) {
				Object value = toJson(getParameterValue(variableName));
				addJson(jsonDocument, variableName, value, getParameterDataTypeClass(variableName));
			}
		}

		return jsonDocument;
	}
	
	protected static void addJson(JSONObject jsonObject, String propertyName, Object propertyValue, Class<?> propertyType) {
		if (propertyValue != null) {
			if (propertyValue instanceof JSONObject) { // comes from a complex variable
				JSONObject json = (JSONObject) propertyValue;
				for (Iterator<String> i = GenericUtils.cast(json.keys()); i.hasNext(); ) {
					String key = i.next();
					try {
						//jsonObject.put(key, json.get(key));
						jsonObject.put(key, valueOf(propertyType, json.get(key)));
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} else { // comes from a simple variable
				try {
					//jsonObject.put(propertyName, propertyValue);
					jsonObject.put(propertyName, valueOf(propertyType, propertyValue));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	protected static Object valueOf(Class<?> type, Object value) {
		if (type != null && value != null && value instanceof String) {
			try {
				Object args[] = { value };
				return type.getConstructor(String.class).newInstance(args);
			}
			catch (Throwable t) {
				// TODO Auto-generated catch block
				t.printStackTrace();
			}
		}
		return value;
	}
	
	protected Object toJson(Object object) throws JSONException {
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
	
	private JSONObject toJson(Element element) throws JSONException {
		JSONObject jsonXml = new JSONObject();
		XMLUtils.handleElement(element, jsonXml, true, getConnector().isJsonUseType());
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
		if (object instanceof List) {
			Vector<Object> v = GenericUtils.cast(object);
			return isInputDomVariable(v.isEmpty() ? false : v.firstElement());
		}
		if (object instanceof NodeList) {
			NodeList l = (NodeList)object;
			return isInputDomVariable(l.getLength() == 0 ? false : l.item(0));
		}
		return false;
	}
	
	@Override
	public void writeSchemaToFile(String xsdTypes) {
		if (defaultSchemaMark.equals(xsdTypes)) {
			new File(getSchemaFilePath()).delete();
		} else {
			super.writeSchemaToFile(xsdTypes);
		}
	}

	@Override
    public String generateXsdTypes(Document document, boolean extract) throws Exception {
    	if (extract) {
    		return super.generateXsdTypes(document, extract);
    	} else {
    		return defaultSchemaMark;
    	}
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
