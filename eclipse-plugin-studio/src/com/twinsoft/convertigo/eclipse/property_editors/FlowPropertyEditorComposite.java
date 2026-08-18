/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU Affero General Public
 * License  as published by  the Free Software Foundation; either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.eclipse.property_editors;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.teamdev.jxbrowser.js.JsAccessible;
import com.teamdev.jxbrowser.js.JsObject;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.flow.Flow;
import com.twinsoft.convertigo.beans.flow.FlowEngine;
import com.twinsoft.convertigo.beans.flow.FlowVirtualObject;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.FlowVirtualObjectTreeObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.flow.FlowEngineBridge;

public class FlowPropertyEditorComposite extends Composite {

	private final FlowVirtualObjectTreeObject treeObject;
	private final String propertyName;
	private final JSONObject propertyDefinition;
	private final String initialValue;
	private C8oBrowser browser;
	private String value;
	private final JSONObject values = new JSONObject();

	private record EditorTarget(Flow flow, FlowEngine flowEngine, String qName) {
	}

	private record EditorPayload(String html, JSONObject state, long editorPreparationTime, long statePreparationTime) {
	}

	public FlowPropertyEditorComposite(Composite parent, int style, FlowVirtualObjectTreeObject treeObject,
			String propertyName, JSONObject propertyDefinition, String initialValue) {
		super(parent, style);
		this.treeObject = treeObject;
		this.propertyName = propertyName;
		this.propertyDefinition = propertyDefinition == null ? new JSONObject() : propertyDefinition;
		this.initialValue = initialValue == null ? "" : initialValue;
		this.value = this.initialValue;
		setLayout(new FillLayout());
		createBrowser();
	}

	public String getValue() {
		return value == null ? "" : value;
	}

	public void applyAdditionalValues(String currentPropertyName) {
		for (var keys = values.keys(); keys.hasNext();) {
			var key = String.valueOf(keys.next());
			if (key.isBlank() || key.equals(currentPropertyName)) {
				continue;
			}
			try {
				treeObject.setFlowPropertyValue(key, values.get(key));
			} catch (Exception e) {
				ConvertigoPlugin.logException(e, "Unable to apply Flow editor property \"" + key + "\".");
			}
		}
	}

	private void createBrowser() {
		var loading = new Label(this, SWT.CENTER);
		loading.setText("Loading Flow property editor...");
		var display = getDisplay();
		var object = treeObject.getObject();
		var target = findTarget(object);
		if (target == null) {
			initializeBrowser(loading,
					new EditorPayload(fallbackHtml("Unable to resolve parent Flow or FlowEngine."), null, 0, 0));
			return;
		}
		Engine.execute(() -> {
			EditorPayload payload;
			try {
				var editorStarted = System.currentTimeMillis();
				var response = propertyEditor(target);
				var editorPreparationTime = System.currentTimeMillis() - editorStarted;
				var html = response.optString("html", "");
				var stateStarted = System.currentTimeMillis();
				var editorState = state(target, object);
				payload = new EditorPayload(
						html.isBlank() ? fallbackHtml("Flow property editor is not available.") : html,
						editorState, editorPreparationTime, System.currentTimeMillis() - stateStarted);
			} catch (Exception e) {
				ConvertigoPlugin.logException(e, "Unable to prepare Flow property editor.");
				payload = new EditorPayload(fallbackHtml(e.getMessage()), null, 0, 0);
			}
			var prepared = payload;
			if (!display.isDisposed()) {
				display.asyncExec(() -> initializeBrowser(loading, prepared));
			}
		});
	}

	private void initializeBrowser(Label loading, EditorPayload payload) {
		if (isDisposed()) {
			return;
		}
		var started = System.currentTimeMillis();
		try {
			if (!loading.isDisposed()) {
				loading.dispose();
			}
			browser = new C8oBrowser(this, SWT.NONE);
			browser.setUseExternalBrowser(true);
			browser.setText(payload.html());
			if (payload.state() != null) {
				installBridge();
				post(payload.state());
				loadBindingSourcesAsync();
			}
			layout(true, true);
			ConvertigoPlugin.logStudioDebug("Flow property editor ready for " + propertyName
					+ " (editor " + payload.editorPreparationTime() + " ms, state "
					+ payload.statePreparationTime() + " ms, browser "
					+ (System.currentTimeMillis() - started) + " ms) : " + browser.getDebugUrl());
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Unable to open Flow property editor.");
		}
	}

	private void installBridge() {
		try {
			var bridge = new BrowserBridge();
			JsObject window = browser.getBrowser().mainFrame().get().executeJavaScript("window");
			window.putProperty("flowEditor", bridge);
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Unable to install Flow property editor bridge.");
		}
	}

	private JSONObject propertyEditor(EditorTarget target) throws Exception {
		if (target.flow() != null) {
			return new FlowEngineBridge().propertyEditor(target.flow());
		}
		return new FlowEngineBridge().propertyEditor(target.flowEngine());
	}

	private JSONObject state(EditorTarget target, FlowVirtualObject object) throws Exception {
		var state = new JSONObject()
				.put("mode", "property")
				.put("property", propertyName)
				.put("value", initialValue)
				.put("propertyDefinition", propertyDefinition)
				.put("virtualKind", object.getVirtualKind())
				.put("virtualType", object.getVirtualType())
				.put("virtualPath", object.getVirtualPath())
				.put("summary", object.getSummary())
				.put("flowQName", target.qName())
				.put("definition", valueOrObject(object.getDefinitionObject()))
				.put("info", valueOrObject(object.getVirtualInfoObject()));
		if (target.flow() != null && "node".equals(object.getVirtualKind())) {
			state.put("context", context(target.flow(), object));
		}
		if (target.flow() == null && isBindingProperty()) {
			state.put("bindingSources", new JSONArray());
			state.put("bindingSourcesLoading", true);
		}
		if (isRequestableProperty()) {
			state.put("requestables", requestables(target));
		}
		return state;
	}

	private boolean isBindingProperty() {
		return "binding".equals(propertyDefinition.optString("kind", ""))
				|| "binding".equals(propertyDefinition.optString("type", ""));
	}

	private boolean isRequestableProperty() {
		return "requestable".equals(propertyDefinition.optString("kind", ""))
				|| "requestable".equals(propertyDefinition.optString("type", ""));
	}

	private JSONObject context(Flow flow, FlowVirtualObject object) throws Exception {
		return context(flow, object, null);
	}

	private JSONObject context(Flow flow, FlowVirtualObject object, JSONObject options) throws Exception {
		var include = new JSONArray()
				.put("input")
				.put("config")
				.put("local")
				.put("current")
				.put("result");
		var request = new JSONObject()
				.put("path", object.getVirtualPath())
				.put("property", propertyName)
				.put("include", include)
				.put("position", "before")
				.put("detail", "normal");
		merge(request, options);
		return new FlowEngineBridge().context(flow, request);
	}

	private JSONObject requestables(EditorTarget target) throws Exception {
		if (target.flow() != null) {
			return new FlowEngineBridge().requestables(target.flow());
		}
		return new FlowEngineBridge().requestables(target.flowEngine());
	}

	private JSONObject icons(EditorTarget target, JSONObject options) throws Exception {
		if (target.flow() != null) {
			return new FlowEngineBridge().icons(target.flow(), options);
		}
		return new FlowEngineBridge().icons(target.flowEngine(), options);
	}

	private void post(JSONObject message) {
		try {
			browser.getBrowser().mainFrame()
					.ifPresent(frame -> frame.executeJavaScript("window.receiveFromJava(" + message + ");"));
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Unable to update Flow property editor.");
		}
	}

	private void postData(JSONObject message) {
		try {
			if (browser == null || browser.isDisposed()) {
				return;
			}
			browser.getBrowser().mainFrame()
					.ifPresent(frame -> frame.executeJavaScript("window.receiveFlowData(" + message + ");"));
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Unable to update Flow property editor data.");
		}
	}

	private void loadBindingSourcesAsync() {
		if (!isBindingProperty()) {
			return;
		}
		var display = getDisplay();
		Engine.execute(() -> {
			var update = new JSONObject();
			try {
				var object = treeObject.getObject();
				var target = findTarget(object);
				var response = target == null
						? new JSONObject().put("bindingSources", new JSONArray())
						: bindingSources(target, object, new JSONObject().put("property", propertyName));
				update.put("bindingSources", response.optJSONArray("bindingSources"));
				update.put("bindingSourcesLoading", false);
				if (!response.optBoolean("ok", true)) {
					update.put("bindingSourcesError", "Unable to load available values.");
				}
			} catch (Exception e) {
				ConvertigoPlugin.logException(e, "Unable to load Flow property editor values.");
				try {
					update.put("bindingSources", new JSONArray());
					update.put("bindingSourcesLoading", false);
					update.put("bindingSourcesError", e.getMessage());
				} catch (Exception jsonError) {
				}
			}
			if (!display.isDisposed()) {
				display.asyncExec(() -> {
					if (!isDisposed()) {
						postData(update);
					}
				});
			}
		});
	}

	private static EditorTarget findTarget(DatabaseObject object) {
		for (var current = object; current != null; current = current.getParent()) {
			if (current instanceof Flow flow) {
				var project = flow.getProject();
				return new EditorTarget(flow, project == null ? null : project.getFlowEngine(), flow.getQName());
			}
			if (current instanceof FlowEngine flowEngine) {
				return new EditorTarget(null, flowEngine, flowEngine.getQName());
			}
		}
		return null;
	}

	private static JSONObject valueOrObject(JSONObject object) {
		return object == null ? new JSONObject() : object;
	}

	private static void merge(JSONObject target, JSONObject source) throws Exception {
		if (source == null) {
			return;
		}
		for (var keys = source.keys(); keys.hasNext();) {
			var key = String.valueOf(keys.next());
			target.put(key, source.opt(key));
		}
	}

	private static String fallbackHtml(String message) {
		return """
				<!doctype html>
				<html><body style="margin:0;padding:16px;font:12px sans-serif;background:#1f2327;color:#e9eef2">
				<div id="app"></div>
				<script>document.getElementById('app').textContent=%s;</script>
				</body></html>
				""".formatted(JSONObject.quote(message == null ? "Flow property editor is not available." : message));
	}

	public class BrowserBridge {
		@JsAccessible
		public void receive(String message) {
			try {
				var json = new JSONObject(message);
				if ("value".equals(json.optString("type", ""))) {
					value = json.optString("value", "");
					values.put(propertyName, value);
				}
				if ("values".equals(json.optString("type", ""))) {
					var properties = json.optJSONObject("values");
					if (properties != null) {
						for (var keys = properties.keys(); keys.hasNext();) {
							var key = String.valueOf(keys.next());
							values.put(key, properties.get(key));
						}
						if (properties.has(propertyName)) {
							value = properties.optString(propertyName, value);
						}
					}
					if (json.has("value")) {
						value = json.optString("value", value);
						values.put(propertyName, value);
					}
				}
				if ("openExternal".equals(json.optString("type", ""))) {
					var url = json.optString("url", "");
					if (url.matches("(?i)^https?://.*")) {
						getDisplay().asyncExec(() -> Program.launch(url));
					}
				}
			} catch (Exception e) {
				ConvertigoPlugin.logException(e, "Unable to process Flow property editor message.");
			}
		}

		@JsAccessible
		public String request(String message) {
			try {
				var json = new JSONObject(message == null ? "{}" : message);
				var name = json.optString("name", "");
				var object = treeObject.getObject();
				var target = findTarget(object);
				if (target == null) {
					return new JSONObject()
							.put("ok", false)
							.put("error", "Unable to resolve parent Flow or FlowEngine.")
							.toString();
				}
				return switch (name) {
				case "requestables" -> new JSONObject()
						.put("ok", true)
						.put("requestables", requestables(target))
						.toString();
				case "bindingSources" -> bindingSources(target, object, json.optJSONObject("payload"))
						.toString();
				case "context" -> new JSONObject()
						.put("ok", true)
						.put("context", target.flow() == null
								? new JSONObject()
								: context(target.flow(), object, json.optJSONObject("payload")))
						.toString();
				case "icons" -> icons(target, json.optJSONObject("payload"))
						.toString();
				default -> new JSONObject()
						.put("ok", false)
						.put("error", "Unsupported Flow editor request: " + name)
						.toString();
				};
			} catch (Exception e) {
				ConvertigoPlugin.logException(e, "Unable to process Flow property editor request.");
				try {
					return new JSONObject()
							.put("ok", false)
							.put("error", e.getMessage())
							.toString();
				} catch (Exception jsonError) {
					return "{\"ok\":false}";
				}
			}
		}
	}

	private JSONObject bindingSources(EditorTarget target, FlowVirtualObject object, JSONObject payload) throws Exception {
		if (target.flowEngine() == null || object.getVirtualPath().isBlank()) {
			return new JSONObject().put("ok", true).put("bindingSources", new JSONArray());
		}
		var property = payload == null ? propertyName : payload.optString("property", propertyName);
		var response = new FlowEngineBridge().authoringTree(target.flowEngine(), new JSONObject()
				.put("surface", "frontend")
				.put("focusPath", object.getVirtualPath())
				.put("bindingTargetPath", object.getSourceMutationPath())
				.put("bindingTargetSource", object.getSourcePath())
				.put("detail", "full")
				.put("includeBindings", true)
				.put("includeFrontendCatalog", false)
				.put("includeFlowCatalog", false)
				.put("property", property));
		var children = response.optJSONArray("children");
		var node = children == null || children.length() == 0 ? null : children.optJSONObject(0);
		var info = node == null ? null : jsonObject(node.opt("info"));
		var definitions = info == null ? null : info.optJSONObject("propertyDefinitions");
		var definition = definitions == null ? null : definitions.optJSONObject(property);
		var sources = definition == null ? null : definition.optJSONArray("bindingSources");
		return new JSONObject()
				.put("ok", response.optBoolean("ok", true))
				.put("bindingSources", sources == null ? new JSONArray() : sources);
	}

	private static JSONObject jsonObject(Object value) {
		if (value instanceof JSONObject object) {
			return object;
		}
		if (value instanceof String text && !text.isBlank()) {
			try {
				return new JSONObject(text);
			} catch (Exception e) {
			}
		}
		return null;
	}
}
