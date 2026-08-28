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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.eclipse.editors.flow;

import org.codehaus.jettison.json.JSONObject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import com.teamdev.jxbrowser.browser.callback.InjectJsCallback;
import com.teamdev.jxbrowser.browser.callback.InjectJsCallback.Response;
import com.teamdev.jxbrowser.js.JsAccessible;
import com.teamdev.jxbrowser.js.JsObject;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.eclipse.views.mobile.MobileDebugView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ViewImageProvider;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.flow.FlowStudioSupport;

public class FlowEngineEditor extends EditorPart {

	public static final String ID = "com.twinsoft.convertigo.eclipse.editors.flow.FlowEngineEditor";

	private FlowEngineEditorInput input;
	private C8oBrowser browser;
	private boolean authoringBridgeInstalled;

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if (!(input instanceof FlowEngineEditorInput flowInput)) {
			throw new PartInitException("Invalid Flow browser editor input.");
		}
		this.input = flowInput;
		setSite(site);
		setInput(input);
		setPartName(flowInput.getName());
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void dispose() {
		if (browser != null && !browser.isDisposed()) {
			browser.dispose();
		}
		super.dispose();
	}

	@Override
	public void createPartControl(Composite parent) {
		SwtUtils.refreshTheme();
		parent.setLayout(new GridLayout(1, false));

		var toolbar = new ToolBar(parent, SWT.FLAT | SWT.WRAP | SWT.RIGHT);
		toolbar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		var project = getProject();
		browser = project == null ? new C8oBrowser(parent, SWT.NONE) : new C8oBrowser(parent, SWT.NONE, project);
		browser.setLayoutData(new GridData(GridData.FILL_BOTH));
		browser.setUseExternalBrowser(false);

		browser.addToolItemNavigation(toolbar);
		browser.addToolItemOpenExternal(toolbar);
		addToolItemFrontendDebug(toolbar);
		installAuthoringBridge();
		browser.setUrl(input.getUrl());
	}

	private void installAuthoringBridge() {
		if (authoringBridgeInstalled || browser == null || browser.isDisposed() || input == null || !input.supportsAuthoring()) {
			return;
		}
		authoringBridgeInstalled = true;
		browser.getBrowser().set(InjectJsCallback.class, event -> {
			var frame = event.frame();
			JsObject window = frame.executeJavaScript("window");
			window.putProperty("__c8oStudioAuthoring", new AuthoringBridge());
			frame.executeJavaScript("""
					window.addEventListener("convertigo-flow-authoring", event => {
						const message = event.detail;
						if (message?.protocol === "convertigo.flow.authoring.v1"
								&& ["authoring.select", "authoring.reveal", "authoring.move", "authoring.drop"].includes(message.type)
								&& message.reference) {
							window.__c8oStudioAuthoring?.receive(JSON.stringify(message));
						}
					});
					""");
			return Response.proceed();
		});
	}

	public class AuthoringBridge {
		@JsAccessible
		public void receive(String serializedMessage) {
			try {
				var message = new JSONObject(serializedMessage);
				if (!"convertigo.flow.authoring.v1".equals(message.optString("protocol", ""))) {
					return;
				}
				var type = message.optString("type", "");
				var reference = message.optJSONObject("reference");
				if (reference == null) {
					return;
				}
				if ("authoring.select".equals(type) || "authoring.reveal".equals(type)) {
					browser.getDisplay().asyncExec(() -> revealAuthoringReference(reference));
				} else if ("authoring.move".equals(type) || "authoring.drop".equals(type)) {
					browser.getDisplay().asyncExec(() -> runAuthoringMutation(message));
				}
			} catch (Exception e) {
				Engine.logStudio.debug("Unable to process Flow authoring message.", e);
			}
		}
	}

	private void runAuthoringMutation(JSONObject message) {
		var explorer = ConvertigoPlugin.getDefault().getProjectExplorerView();
		if (explorer == null || browser == null || browser.isDisposed()) {
			return;
		}
		try {
			var type = message.optString("type", "");
			var position = message.optString("position", "");
			if (!"before".equals(position) && !"inside".equals(position) && !"after".equals(position)) {
				throw new IllegalArgumentException("Unsupported Flow authoring position: " + position);
			}
			var target = FlowStudioSupport.resolveAuthoringReference(getProjectName(), message.getJSONObject("reference"));
			var source = "authoring.move".equals(type)
					? FlowStudioSupport.resolveAuthoringReference(getProjectName(), message.getJSONObject("source"))
					: null;
			if (target == null || ("authoring.move".equals(type) && source == null)) {
				throw new IllegalArgumentException("Unable to resolve the Flow authoring source or target.");
			}
			var targetTreeObject = explorer.findTreeObjectByUserObject(target);
			var selectedTreeObject = explorer.findTreeObjectByUserObject(source == null ? target : source);
			var selectionReference = new JSONObject((source == null
					? message.getJSONObject("reference") : message.getJSONObject("source")).toString());
			Engine.execute(() -> executeAuthoringMutation(message, type, position, target, source,
					targetTreeObject, selectedTreeObject, selectionReference));
		} catch (Exception e) {
			showAuthoringMutationError(e);
		}
	}

	private void executeAuthoringMutation(JSONObject message, String type, String position, DatabaseObject target,
			DatabaseObject source, TreeObject targetTreeObject, TreeObject selectedTreeObject,
			JSONObject selectionReference) {
		JSONObject response = null;
		Exception failure = null;
		try {
			if ("authoring.move".equals(type)) {
				response = FlowStudioSupport.moveNode(source, target, position);
			} else {
				var payload = message.getJSONObject("payload");
				if (!FlowStudioSupport.isFlowPaletteData(payload)) {
					throw new IllegalArgumentException("Unsupported Flow authoring palette payload.");
				}
				response = FlowStudioSupport.addFromPalette(target, position, payload);
			}
		} catch (Exception e) {
			failure = e;
		}
		var effectiveResponse = response;
		var effectiveFailure = failure;
		ConvertigoPlugin.asyncExec(() -> handleAuthoringMutationResult(target, targetTreeObject,
				selectedTreeObject, selectionReference, effectiveResponse, effectiveFailure));
	}

	private void handleAuthoringMutationResult(DatabaseObject target, TreeObject targetTreeObject,
			TreeObject selectedTreeObject, JSONObject selectionReference, JSONObject response, Exception failure) {
		if (failure != null) {
			showAuthoringMutationError(failure);
			return;
		}
		if (response == null || !response.optBoolean("done", false)) {
			var error = response == null ? null : response.opt("error");
			showAuthoringMutationError(new IllegalStateException(error == null || error == JSONObject.NULL
					? "The Flow visual authoring mutation was rejected." : error.toString()));
			return;
		}
		var explorer = ConvertigoPlugin.getDefault().getProjectExplorerView();
		if (explorer != null) {
			explorer.reconcileFlowAuthoringMutation(targetTreeObject, selectedTreeObject, target,
					updatedSelectionReference(selectionReference, response), response);
		}
		clearAuthoringHighlight();
	}

	private static JSONObject updatedSelectionReference(JSONObject reference, JSONObject response) {
		try {
			var updated = reference == null ? new JSONObject() : new JSONObject(reference.toString());
			var mutationPath = response == null ? "" : response.optString("selectionMutationPath", "");
			var selectionId = response == null ? "" : response.optString("selectionId", "");
			if (!mutationPath.isBlank()) {
				updated.put("sourceMutationPath", mutationPath);
			}
			if (!selectionId.isBlank()) {
				updated.put("nodeId", selectionId);
			}
			return updated;
		} catch (Exception e) {
			return reference;
		}
	}

	private void clearAuthoringHighlight() {
		if (browser == null || browser.isDisposed()) {
			return;
		}
		C8oBrowser.run(() -> {
			try {
				browser.executeJavaScriptAndReturnValue("window.__c8oFlowAuthoring?.clear()");
			} catch (Exception e) {
				Engine.logStudio.debug("Unable to clear the Flow authoring highlight.", e);
			}
		});
	}

	private void showAuthoringMutationError(Exception error) {
		Engine.logStudio.warn("Unable to apply Flow visual authoring mutation.", error);
		clearAuthoringHighlight();
		if (browser != null && !browser.isDisposed()) {
			var message = error.getMessage();
			MessageDialog.openError(browser.getShell(), "Flow",
					message == null || message.isBlank() ? error.toString() : message);
		}
	}

	private void revealAuthoringReference(JSONObject reference) {
		var explorer = ConvertigoPlugin.getDefault().getProjectExplorerView();
		var sourceProject = reference.optString("sourceProject", getProjectName());
		if (explorer != null && !explorer.selectFlowAuthoringReference(sourceProject, reference)) {
			Engine.logStudio.warn("Unable to find the Flow authoring object in the project tree: " + reference);
		}
	}

	private void highlightAuthoringReference(JSONObject reference) {
		if (reference == null || browser == null || browser.isDisposed()) {
			return;
		}
		C8oBrowser.run(() -> {
			try {
				browser.executeJavaScriptAndReturnValue(
						"window.__c8oFlowAuthoring?.highlight(" + reference + ") ?? 0");
			} catch (Exception e) {
				Engine.logStudio.debug("Unable to highlight Flow authoring object.", e);
			}
		});
	}

	public static boolean highlightAuthoringObject(String projectName, JSONObject reference) {
		if (projectName == null || projectName.isBlank() || reference == null) {
			return false;
		}
		try {
			var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			for (IEditorReference editorReference : page.getEditorReferences()) {
				var editorInput = editorReference.getEditorInput();
				if (!(editorInput instanceof FlowEngineEditorInput flowInput)
						|| !flowInput.supportsAuthoring()
						|| !projectName.equals(flowInput.getProjectName())) {
					continue;
				}
				var editor = editorReference.getEditor(false);
				if (editor instanceof FlowEngineEditor flowEditor) {
					page.bringToTop(flowEditor);
					flowEditor.highlightAuthoringReference(reference);
					return true;
				}
			}
		} catch (Exception e) {
			Engine.logStudio.debug("Unable to find the Flow authoring viewer.", e);
		}
		return false;
	}

	public static boolean open(JSONObject browser, String fallbackUrl, String fallbackTitle, String fallbackProject) {
		var url = browser == null ? fallbackUrl : browser.optString("url", fallbackUrl);
		if (url == null || url.isBlank()) {
			return false;
		}
		try {
			var title = browser == null ? fallbackTitle : browser.optString("title", fallbackTitle);
			var id = browser == null ? "" : browser.optString("id", "");
			var projectName = browser == null ? fallbackProject : browser.optString("project", fallbackProject);
			var debugPort = browser == null ? 0
					: browser.optInt("debugPort", browser.optInt("browserDebugPort", 0));
			if (debugPort >= 1024 && debugPort <= 65535 && Engine.theApp != null
					&& projectName != null && !projectName.isBlank()) {
				var project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName, false);
				if (project != null) {
					C8oBrowser.setPreferredDebugPort(project, debugPort);
				}
			}
			var tooltip = browser == null ? url : browser.optString("tooltip", url);
			var authoring = browser == null ? null : browser.optJSONObject("authoring");
			var authoringProtocol = authoring == null ? "" : authoring.optString("protocol", "");
			var input = new FlowEngineEditorInput(id, title, url, projectName, tooltip, authoringProtocol);
			var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			var editor = page.openEditor(input, ID);
			if (editor instanceof FlowEngineEditor flowEditor) {
				if (debugPort >= 1024 && debugPort <= 65535 && flowEditor.browser != null
						&& !flowEditor.browser.isDisposed()) {
					flowEditor.browser.setDebugPort(debugPort);
				}
				if (flowEditor.getEditorInput() != input) {
					flowEditor.updateInput(input);
				}
			}
			return true;
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Unable to open Flow browser editor.");
			return false;
		}
	}

	private Project getProject() {
		try {
			var projectName = input == null ? "" : input.getProjectName();
			if (projectName != null && !projectName.isBlank()) {
				return Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName, false);
			}
		} catch (Exception e) {
		}
		return null;
	}

	private void addToolItemFrontendDebug(ToolBar toolbar) {
		new ToolItem(toolbar, SWT.SEPARATOR);
		var item = new ToolItem(toolbar, SWT.NONE);
		item.setImage(ViewImageProvider.getImageFromCache("/com/twinsoft/convertigo/eclipse/editors/images/debug.png"));
		item.setToolTipText("Open FrontEnd Debug");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				MobileDebugView view = ConvertigoPlugin.getDefault().getMobileDebugView(true);
				if (view != null) {
					view.onActivated(FlowEngineEditor.this);
				}
			}
		});
	}

	public String getDebugUrl() {
		return browser == null || browser.isDisposed() ? "" : browser.getDebugUrl();
	}

	public String getProjectName() {
		return input == null ? "" : input.getProjectName();
	}

	public void updateInput(FlowEngineEditorInput input) {
		var targetUrl = input.getUrl();
		if (browser != null && !browser.isDisposed() && this.input != null) {
			var previousBaseUrl = this.input.getUrl();
			var currentUrl = browser.getURL();
			if (!previousBaseUrl.isBlank() && currentUrl != null && currentUrl.startsWith(previousBaseUrl)) {
				targetUrl += currentUrl.substring(previousBaseUrl.length());
			}
		}
		this.input = input;
		setInput(input);
		setPartName(input.getName());
		if (browser != null && !browser.isDisposed()) {
			installAuthoringBridge();
			browser.setUrl(targetUrl);
		}
	}

	@Override
	public void setFocus() {
		if (browser != null && !browser.isDisposed()) {
			browser.setFocus();
		}
	}
}
