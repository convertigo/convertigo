/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.beans.steps;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.constants.Constants;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.core.IStepSmartTypeContainer;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.steps.SmartType.Mode;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.JsonOutput;
import com.twinsoft.convertigo.engine.enums.JsonOutput.JsonRoot;
import com.twinsoft.convertigo.engine.json.JsonPath;
import com.twinsoft.convertigo.engine.json.JsonStreamOutput;
import com.twinsoft.convertigo.engine.json.NativeStepOutput;
import com.twinsoft.convertigo.engine.requesters.InternalJsonRequester;
import com.twinsoft.convertigo.engine.requesters.JsonServletRequester;
import com.twinsoft.convertigo.engine.util.JsonNativeOutput;
import com.twinsoft.convertigo.engine.util.RhinoUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class JsonToXmlStep extends Step implements IStepSmartTypeContainer {

	private static final long serialVersionUID = 4426288799938289068L;

	private SmartType key = new SmartType();
	private SmartType jsonObject = new SmartType();
	private String jsonSample = "";
	private String arrayChildrenTag = "item";
	transient protected String jsonSource;
	/** in a native sequence, the evaluated data whose JSON text is built only when something asks for it */
	transient private Object jsonData = NO_DATA;
	private static final Object NO_DATA = new Object();

	/** the JSON text of the step, built from the data the first time it is asked in a native sequence */
	protected String jsonSource() {
		if (jsonSource == null && jsonData != NO_DATA) {
			jsonSource = RhinoUtils.jsonStringifyData(jsonData);
		}
		return jsonSource;
	}

	/** '{' or '[' for an object or an array, from the data without writing its JSON when it is plain data */
	private char jsonKind() {
		if (jsonSource == null && jsonData != NO_DATA) {
			Object data = jsonData instanceof org.mozilla.javascript.NativeJavaObject ? ((org.mozilla.javascript.NativeJavaObject) jsonData).unwrap() : jsonData;
			if (RhinoUtils.isJsonData(data)) {
				if (data != null && (data.getClass() == org.mozilla.javascript.NativeObject.class || (data instanceof java.util.Map && !(data instanceof Scriptable)))) {
					return '{';
				}
				if (data != null && (data.getClass() == org.mozilla.javascript.NativeArray.class || (data instanceof java.util.List && !(data instanceof Scriptable)))) {
					return '[';
				}
				return ' ';
			}
		}
		String source = jsonSource();
		return source == null || source.isEmpty() ? ' ' : source.charAt(0);
	}
	transient private Object jsonValue;

	public JsonToXmlStep() {
		super();
		setOutput(true);
		this.xml = true;
		key.setExpression(getName());
	}

	@Override
	public JsonToXmlStep clone() throws CloneNotSupportedException {
		JsonToXmlStep clonedObject = (JsonToXmlStep) super.clone();
		clonedObject.key = key.clone();
		clonedObject.jsonObject = jsonObject.clone();
		return clonedObject;
	}

	@Override
	public JsonToXmlStep copy() throws CloneNotSupportedException {
		JsonToXmlStep copiedObject = (JsonToXmlStep) super.copy();
		return copiedObject;
	}

	@Override
	public String toString() {
		String name;
		switch (key.getMode()) {
		case JS: name = key.getExpression(); break;
		case PLAIN: name = "\"" + key.getExpression() + "\""; break;
		case SOURCE: name = key.toString(this); break;
		default: name = "(" + getName() + ")" ; break;
		}
		return name + " : " + jsonObject.toString();
	}

	@Override
	public String getStepNodeName() {
		return getName();
	}

	public SmartType getKey() {
		return key;
	}

	public void setKey(SmartType key) {
		this.key = key;
	}

	public SmartType getJsonObject() {
		return jsonObject;
	}

	public void setJsonObject(SmartType jsonObject) {
		this.jsonObject = jsonObject;
	}

	public String getJsonSample() {
		return jsonSample;
	}

	public void setJsonSample(String jsonSample) {
		this.jsonSample = jsonSample;
	}

	public String getArrayChildrenTag() {
		return arrayChildrenTag;
	}

	public void setArrayChildrenTag(String arrayChildrenTag) {
		this.arrayChildrenTag = arrayChildrenTag;
	}

	@Override
	protected Node createStepNode() throws EngineException {
		if (keepJsonNative()) {
			return null;
		}
		Document doc = getOutputDocument();
		Element elt = doc.createElement("e");

		try {
			Object o;
			String jsonSource = jsonSource();
			if (jsonSource.startsWith("{")) {
				o = new JSONObject(jsonSource);
			} else if (jsonSource.startsWith("[")) {
				o = new JSONArray(jsonSource);
			} else {
				o = RhinoUtils.jsonParse(jsonSource);
			}

			XMLUtils.jsonToXml(o, getStepNodeName(), elt, true, false, arrayChildrenTag);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		elt = (Element) elt.getFirstChild();
		elt.setAttribute("originalKeyName", key.getSingleString(this));
		return elt;
	}

	/**
	 * With Sequence.jsonNativeOutput, an output step directly under a sequence requested as JSON keeps its
	 * JSON as text for the response (JsonServletRequester) instead of building its XML. Only for a JS object,
	 * whose text is valid JSON, and for the default JSON output of the project (types, document child nodes).
	 */
	protected boolean isJsonNativeCandidate() {
		if (!isOutput() || jsonObject.getMode() != Mode.JS || !(getParent() instanceof Sequence)
				|| sequence == null || !sequence.isJsonNativeOutput() || sequence.context == null
				|| !(sequence.getRequester() instanceof JsonServletRequester || sequence.getRequester() instanceof InternalJsonRequester)) {
			return false;
		}
		Project project = sequence.context.project;
		return project != null && project.getJsonOutput() == JsonOutput.useType && project.getJsonRoot() == JsonRoot.docChildNodes;
	}

	private boolean keepJsonNative() throws EngineException {
		if (sequence != null && sequence.context != null && sequence.context.nativeExecution && isOutput() && (jsonSource != null || jsonData != NO_DATA)) {
			// a native sequence builds no XML for its outputs: the ones that were XML (not directly under the sequence, a
			// value that is not an object nor an array) keep their place and form in the answer (JsonNativeOutput.legacy)
			String keyName = key.getSingleString(this);
			char kind = jsonKind();
			boolean legacy = !isJsonNativeCandidate() || !(kind == '{' || kind == '[');
			// its JSON is written from the data in the answer, its text built only when asked (legacy, native call)
			JsonNativeOutput nativeOutput = (jsonSource == null && jsonData != NO_DATA
					? JsonNativeOutput.ofData(keyName, jsonData, getStepNodeName(), arrayChildrenTag)
					: new JsonNativeOutput(keyName, jsonSource, getStepNodeName(), arrayChildrenTag)).legacy(legacy);
			if (jsonObject.getMode() == Mode.JS) {
				nativeOutput.value(jsonValue);
			}
			if (keyName != null && registerJsonNativeOutput(nativeOutput)) {
				return true;
			}
		}
		String jsonSource = jsonSource();
		if (!isJsonNativeCandidate() || jsonSource == null || !(jsonSource.startsWith("{") || jsonSource.startsWith("["))) {
			return false;
		}
		String keyName = key.getSingleString(this);
		return keyName != null && registerJsonNativeOutput(new JsonNativeOutput(keyName, jsonSource, getStepNodeName(), arrayChildrenTag));
	}

	/** the JSON path sources read the JSON of the step (also when it is kept native), read in stream from its text */
	@Override
	public Object selectJson(JsonPath path) throws EngineException {
		String jsonSource = jsonSource();
		if (jsonSource == null || !(jsonSource.startsWith("{") || jsonSource.startsWith("["))) {
			return JsonPath.MISSING;
		}
		try {
			return path.selectValue(jsonSource);
		} catch (java.io.IOException e) {
			throw new EngineException("Unable to read the JSON of the step '" + getName() + "' at " + path, e);
		}
	}

	/** puts a JSON kept as is in the response of the sequence, false when its key is already there */
	protected boolean registerJsonNativeOutput(JsonNativeOutput nativeOutput) {
		com.twinsoft.convertigo.engine.Context context = sequence.context;
		synchronized (context) {
			if (context.jsonNativeOutputs == null) {
				context.jsonNativeOutputs = new LinkedHashMap<String, JsonNativeOutput>();
			}
			if (context.jsonNativeOutputs.containsKey(nativeOutput.getKey())) {
				return false;
			}
			context.jsonNativeOutputs.put(nativeOutput.getKey(), nativeOutput);
			// the XML output lacks this JSON: it must not be cached
			context.isCacheEnabled = false;
		}
		if (Engine.isStudioMode()) {
			boolean stream = nativeOutput instanceof JsonStreamOutput;
			NativeStepOutput.fire(this, sequence.loadedSteps.get(priority), executeTimeID, nativeOutput.getKey(),
					stream ? null : nativeOutput.toJSONString(), 0, context.contextID, stream ? "stream" : "output");
		}
		return true;
	}

	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnabled()) {
			evaluate(javascriptContext, scope, "key", key);
			evaluate(javascriptContext, scope, "jsonObject", jsonObject);

			if (jsonObject.getMode() == Mode.JS) {
				// the JSON kept as is for the response is written directly from the data
				jsonData = NO_DATA;
				if (sequence.context != null && sequence.context.nativeExecution) {
					// a native sequence: the JSON is written from the data in the answer, its text built only if asked
					jsonSource = null;
					jsonData = jsonObject.getEvaluated();
				} else {
					jsonSource = isJsonNativeCandidate()
							? RhinoUtils.jsonStringifyData(jsonObject.getEvaluated())
							: RhinoUtils.jsonStringify(jsonObject.getEvaluated());
				}
				jsonValue = jsonObject.getEvaluated();
			} else {
				jsonData = NO_DATA;
				jsonSource = jsonObject.getSingleString(this).trim();
			}

			return super.stepExecute(javascriptContext, scope);
		}
		return false;
	}

	@Override
	public XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaElement element = (XmlSchemaElement) super.getXmlSchemaObject(collection, schema);
		try {
			String s = getJsonSample().trim();
			if (s.isEmpty() && jsonObject.getMode() == Mode.PLAIN) {
				s = jsonObject.getExpression().trim();
			}
			if (!s.startsWith("{") && !s.startsWith("[")) {
				return element;
			}
			Object json = s.startsWith("[") ? new JSONArray(s) : new JSONObject(s);
			Document doc = XMLUtils.getDefaultDocumentBuilder().newDocument();
			Element root = doc.createElement("root");
			doc.appendChild(root);
			XMLUtils.jsonToXml(json, null, root, true, true, false, arrayChildrenTag);
			XmlSchemaUtils.handleXsdElement(this, element, root, schema);

			XmlSchemaComplexType cType = (XmlSchemaComplexType) element.getSchemaType();
			XmlSchemaAttribute attribute = XmlSchemaUtils.makeDynamic(this, new XmlSchemaAttribute());
			attribute.setName("originalKeyName");
			attribute.setSchemaTypeName(Constants.XSD_STRING);
			if (key.getMode().equals(SmartType.Mode.PLAIN)) {
				attribute.setDefaultValue(key.getExpression());
			}
			attribute.setUse(XmlSchemaUtils.attributeUseOptional);
			cType.getAttributes().add(attribute);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return element;
	}

	private transient Set<SmartType> smartTypes = null;

	@Override
	public Set<SmartType> getSmartTypes() {
		if (smartTypes != null) {
			if  (!hasChanged)
				return smartTypes;
			else
				smartTypes.clear();
		}
		else {
			smartTypes = new HashSet<SmartType>();
		}
		smartTypes.add(key);
		smartTypes.add(jsonObject);
		return smartTypes;
	}

	@Override
	public String toJsString() {
		return null;
	}

	@Override
	protected void onBeanNameChanged(String oldName, String newName) {
		if (key != null && key.getMode() == Mode.PLAIN
				&& oldName.startsWith(StringUtils.normalize(key.getExpression()))) {
			key.setExpression(newName);
			hasChanged = true;
		}
	}

	@Override
	protected String defaultBeanName(String displayName) {
		return "object";
	}
}
