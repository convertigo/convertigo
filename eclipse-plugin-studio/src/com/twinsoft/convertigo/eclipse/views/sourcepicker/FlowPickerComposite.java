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

package com.twinsoft.convertigo.eclipse.views.sourcepicker;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.teamdev.jxbrowser.js.JsAccessible;
import com.teamdev.jxbrowser.js.JsObject;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.flow.Flow;
import com.twinsoft.convertigo.beans.flow.FlowEngine;
import com.twinsoft.convertigo.beans.flow.FlowVirtualObject;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.FlowVirtualObjectTreeObject;
import com.twinsoft.convertigo.engine.flow.FlowEngineBridge;

class FlowPickerComposite extends Composite {

	private final ToolItem tiLink;
	private final C8oBrowser browser;
	private int editorHtmlHash;
	private FlowVirtualObjectTreeObject currentTreeObject;

	private record PickerTarget(Flow flow, FlowEngine flowEngine, String qName) {
	}

	FlowPickerComposite(Composite parent, int style) {
		super(parent, style);
		var layout = new GridLayout(1, false);
		layout.marginHeight = layout.marginWidth = 0;
		setLayout(layout);

		var toolbar = new ToolBar(this, SWT.FLAT);
		toolbar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		tiLink = new ToolItem(toolbar, SWT.CHECK);

		browser = new C8oBrowser(this, SWT.NONE);
		browser.setUseExternalBrowser(true);
		ConvertigoPlugin.logStudioDebug("Flow picker debug : " + browser.getDebugUrl());
		browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}

	ToolItem getTiLink() {
		return tiLink;
	}

	void setCurrentInput(Object selected) {
		if (!(selected instanceof FlowVirtualObjectTreeObject treeObject)) {
			currentTreeObject = null;
			post(errorState("Select a Flow object."));
			return;
		}
		try {
			var target = findTarget(treeObject.getObject());
			if (target == null) {
				currentTreeObject = null;
				post(errorState("Unable to resolve parent Flow or FlowEngine."));
				return;
			}
			currentTreeObject = treeObject;
			loadEditor(target);
			var object = treeObject.getObject();
			post(state(target, object));
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Unable to update Flow picker.");
			post(errorState(e.getMessage()));
		}
	}

	private void loadEditor(PickerTarget target) throws Exception {
		var response = target.flow() != null
				? new FlowEngineBridge().propertyEditor(target.flow())
				: new FlowEngineBridge().propertyEditor(target.flowEngine());
		var html = response.optString("html", "");
		html = html.isBlank() ? fallbackHtml("Flow picker is not available.") : html;
		var hash = html.hashCode();
		if (hash == editorHtmlHash) {
			return;
		}
		browser.setText(html);
		installBridge();
		editorHtmlHash = hash;
	}

	private void installBridge() {
		try {
			var bridge = new BrowserBridge();
			JsObject window = browser.getBrowser().mainFrame().get().executeJavaScript("window");
			window.putProperty("flowEditor", bridge);
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Unable to install Flow picker bridge.");
		}
	}

	private JSONObject state(PickerTarget target, FlowVirtualObject object) throws Exception {
		var state = new JSONObject()
				.put("mode", "picker")
				.put("virtualKind", object.getVirtualKind())
				.put("virtualType", object.getVirtualType())
				.put("virtualPath", object.getVirtualPath())
				.put("summary", object.getSummary())
				.put("flowQName", target.qName())
				.put("definition", valueOrObject(object.getDefinitionObject()))
				.put("info", valueOrObject(object.getVirtualInfoObject()));
		if (canProvideContext(target, object)) {
			state.put("context", context(target, object));
		}
		return state;
	}

	private JSONObject context(PickerTarget target, FlowVirtualObject object) throws Exception {
		return context(target, object, null);
	}

	private JSONObject context(PickerTarget target, FlowVirtualObject object, JSONObject options) throws Exception {
		var include = new JSONArray()
				.put("input")
				.put("config")
				.put("local")
				.put("current")
				.put("result");
		var request = new JSONObject()
				.put("include", include)
				.put("detail", "normal")
				.put("position", "before");
		if (target.flow() != null && !isFlowImplementationSource(object)) {
			request.put("path", object.getVirtualPath());
			merge(request, options);
			return new FlowEngineBridge().context(target.flow(), request);
		}

		var info = object.getVirtualInfoObject();
		var sourcePath = infoString(info, "sourcePath");
		var sourceMutationPath = infoString(info, "sourceMutationPath");
		if (sourcePath.isBlank() || sourceMutationPath.isBlank() || target.flowEngine() == null) {
			return new JSONObject();
		}
		request.put("flowSource", java.nio.file.Files.readString(java.nio.file.Path.of(sourcePath)));
		request.put("path", sourceMutationPath);
		var sourceBlockName = infoString(info, "sourceBlockName");
		if (!sourceBlockName.isBlank()) {
			request.put("sourceBlockName", sourceBlockName);
		}
		merge(request, options);
		return new FlowEngineBridge().context(target.flowEngine(), request);
	}

	private static boolean canProvideContext(PickerTarget target, FlowVirtualObject object) {
		if (!"node".equals(object.getVirtualKind())) {
			return false;
		}
		return target.flow() != null || isFlowImplementationSource(object);
	}

	private static boolean isFlowImplementationSource(FlowVirtualObject object) {
		var info = object.getVirtualInfoObject();
		return info != null && info.optBoolean("flowImplementation", false)
				&& !info.optString("sourcePath", "").isBlank()
				&& !info.optString("sourceMutationPath", "").isBlank();
	}

	private static String infoString(JSONObject info, String key) {
		return info == null ? "" : info.optString(key, "");
	}

	private void post(JSONObject message) {
		try {
			browser.getBrowser().mainFrame()
					.ifPresent(frame -> frame.executeJavaScript("window.receiveFromJava(" + message + ");"));
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Unable to post Flow picker state.");
		}
	}

	private static JSONObject errorState(String message) {
		try {
			return new JSONObject().put("error", message == null ? "Flow picker error." : message);
		} catch (Exception e) {
			return new JSONObject();
		}
	}

	private static PickerTarget findTarget(DatabaseObject object) {
		for (var current = object; current != null; current = current.getParent()) {
			if (current instanceof Flow flow) {
				var project = flow.getProject();
				return new PickerTarget(flow, project == null ? null : project.getFlowEngine(), flow.getQName());
			}
			if (current instanceof FlowEngine flowEngine) {
				return new PickerTarget(null, flowEngine, flowEngine.getQName());
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
				""".formatted(JSONObject.quote(message == null ? "Flow picker is not available." : message));
	}

	public class BrowserBridge {
		@JsAccessible
		public void receive(String message) {
			try {
				var json = new JSONObject(message);
				if ("copy".equals(json.optString("type", ""))) {
					var value = json.optString("value", "");
					getDisplay().asyncExec(() -> {
						var clipboard = new Clipboard(getDisplay());
						try {
							clipboard.setContents(new Object[] { value }, new Transfer[] { TextTransfer.getInstance() });
						} finally {
							clipboard.dispose();
						}
					});
					return;
				}
				if ("setProperty".equals(json.optString("type", ""))) {
					var property = json.optString("property", "");
					var value = json.optString("value", "");
					getDisplay().asyncExec(() -> applyProperty(property, value));
				}
				if ("openExternal".equals(json.optString("type", ""))) {
					var url = json.optString("url", "");
					if (url.matches("(?i)^https?://.*")) {
						getDisplay().asyncExec(() -> Program.launch(url));
					}
				}
			} catch (Exception e) {
				ConvertigoPlugin.logException(e, "Unable to process Flow picker message.");
			}
		}

		@JsAccessible
		public String request(String message) {
			try {
				var json = new JSONObject(message == null ? "{}" : message);
				var name = json.optString("name", "");
				var treeObject = currentTreeObject;
				if (treeObject == null) {
					return new JSONObject()
							.put("ok", false)
							.put("error", "No Flow object selected.")
							.toString();
				}
				var target = findTarget(treeObject.getObject());
				if (target == null) {
					return new JSONObject()
							.put("ok", false)
							.put("error", "Unable to resolve parent Flow or FlowEngine.")
							.toString();
				}
				return switch (name) {
				case "requestables" -> new JSONObject()
						.put("ok", true)
						.put("requestables", target.flow() == null
								? new FlowEngineBridge().requestables(target.flowEngine())
								: new FlowEngineBridge().requestables(target.flow()))
						.toString();
				case "context" -> new JSONObject()
						.put("ok", true)
						.put("context", canProvideContext(target, treeObject.getObject())
								? context(target, treeObject.getObject(), json.optJSONObject("payload"))
								: new JSONObject())
						.toString();
				case "icons" -> (target.flow() == null
						? new FlowEngineBridge().icons(target.flowEngine(), json.optJSONObject("payload"))
						: new FlowEngineBridge().icons(target.flow(), json.optJSONObject("payload")))
						.toString();
				default -> new JSONObject()
						.put("ok", false)
						.put("error", "Unsupported Flow picker request: " + name)
						.toString();
				};
			} catch (Exception e) {
				ConvertigoPlugin.logException(e, "Unable to process Flow picker request.");
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

	private void applyProperty(String property, String value) {
		if (currentTreeObject == null) {
			post(errorState("No Flow object selected."));
			return;
		}
		if (property == null || property.isBlank()) {
			post(errorState("No Flow property selected."));
			return;
		}
		try {
			currentTreeObject.setFlowPropertyValue(property, value);
			var target = findTarget(currentTreeObject.getObject());
			if (target != null) {
				var nextState = state(target, currentTreeObject.getObject())
						.put("applied", new JSONObject()
								.put("property", property)
								.put("value", value));
				post(nextState);
			}
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Unable to apply Flow picker value.");
			post(errorState(e.getMessage()));
		}
	}
}
