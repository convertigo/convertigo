package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.flow.Flow;
import com.twinsoft.convertigo.beans.flow.FlowEngine;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.EngineException;

public class FlowPropertyValueBridgeTest {
	@Test public void transportsProviderValuesWithoutHostCoercionForBothOwners() throws Exception {
		for (DatabaseObject owner : new DatabaseObject[] { new Flow(), new FlowEngine() }) {
			var definition = new JSONObject().put("kind", "projectSpecificType").put("type", "number");
			for (Object value : new Object[] { 7, false, "{{ input.n }}", JSONObject.NULL, new JSONObject().put("x", 1) }) {
				var bridge = new FlowEngineBridge() {
					@Override JSONObject invoke(String engine, String method, JSONObject request, Context context,
							org.mozilla.javascript.Context js, Scriptable scope) {
						assertEquals("propertyValue", method);
						assertSame(definition, request.optJSONObject("propertyDefinition"));
						assertEquals("user text", request.optString("text"));
						try { return new JSONObject().put("ok", true).put("value", value); }
						catch (Exception e) { throw new AssertionError(e); }
					}
				};
				assertSame(value, bridge.propertyValue(owner, definition, "user text"));
			}
		}
	}

	@Test public void failedConversionCannotProduceAMutationValue() throws Exception {
		var bridge = new FlowEngineBridge() {
			@Override JSONObject invoke(String engine, String method, JSONObject request, Context context,
					org.mozilla.javascript.Context js, Scriptable scope) {
				try { return new JSONObject().put("ok", false).put("error", "Invalid value"); }
				catch (Exception e) { throw new AssertionError(e); }
			}
		};
		assertThrows(EngineException.class, () -> bridge.propertyValue(new Flow(), new JSONObject(), "bad"));
	}
}
