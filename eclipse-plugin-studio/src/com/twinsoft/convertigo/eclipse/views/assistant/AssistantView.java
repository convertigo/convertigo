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

package com.twinsoft.convertigo.eclipse.views.assistant;

import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Objects;

import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.teamdev.jxbrowser.dom.Element;
import com.teamdev.jxbrowser.engine.Theme;
import com.twinsoft.convertigo.beans.common.FormatedContent;
import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIComponent;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponent;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.actions.OpenTutorialView;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.editors.flow.FlowEngineEditor;
import com.twinsoft.convertigo.eclipse.editors.flow.FlowEngineEditorInput;
import com.twinsoft.convertigo.eclipse.editors.ngx.ApplicationComponentEditor;
import com.twinsoft.convertigo.eclipse.editors.ngx.ApplicationComponentEditorInput;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowserPostMessageHelper;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.eclipse.views.admin.AdminView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.ProductVersion;
import com.twinsoft.convertigo.engine.util.Clipboard;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.GitUtils;
import com.twinsoft.convertigo.engine.util.ProjectUrlParser;

public class AssistantView extends ViewPart {

	public static final String ID = "com.twinsoft.convertigo.eclipse.views.assistant.AssistantView";
	public static final String STARTUP_URL = "https://assistant.convertigo.com/";
	private static final String LOCAL_ASSISTANT_PATH = "/projects/lib_ConvertigoAssistant/DisplayObjects/mobile/";
	private static final String AGENT_ONBOARDING_FEATURE_VERSION = "2026-07-02.agent-onboarding-v1";
	private static final String AGENT_DOWNLOAD_URL = "https://www.convertigo.com/developers/download-low-code-studio";
	private static final String[] LOCAL_AGENT_STACK_PROJECTS = {"lib_ConvertigoAssistant", "lib_ConvertigoMCP", "lib_ConvertigoAgentBridge"};
	private static final long LOCAL_AGENT_STACK_LOADING_RECHECK_MS = 1000L;
	private static final long LOCAL_AGENT_STACK_LOADING_TIMEOUT_MS = 90000L;
	private static final String WAITING_HTML = "<!doctype html><html><head><meta charset=\"utf-8\">"
			+ "<style>"
			+ "html,body{height:100%;margin:0;}"
			+ "body{display:flex;align-items:center;justify-content:center;background:$background$;color:$foreground$;"
			+ "font-family:system-ui,-apple-system,Segoe UI,Roboto,Ubuntu,'Helvetica Neue',Arial,sans-serif;}"
			+ ".c8o-wait{display:flex;flex-direction:column;align-items:center;gap:12px;font-size:14px;}"
			+ ".c8o-spin{width:28px;height:28px;border:3px solid rgba(127,127,127,0.35);border-top-color:$foreground$;"
			+ "border-radius:50%;animation:spin 1s linear infinite;}"
			+ "@keyframes spin{to{transform:rotate(360deg);}}"
			+ "</style></head><body>"
			+ "<div class=\"c8o-wait\"><div class=\"c8o-spin\"></div>"
			+ "<div>Waiting for Convertigo Engine to be ready...</div></div>"
			+ "</body></html>";

	private C8oBrowser browser = null;
	private C8oBrowserPostMessageHelper handler = null;
	private JSONObject jsonMessage = new JSONObject();
	private int counter = 1;
	private String startupUrl = STARTUP_URL;
	private long localAgentStackLoadingStartedAt = 0L;
	private boolean localAgentStackContextRecheckScheduled = false;
	
	@Override
	public void dispose() {
		if (browser != null) {
			browser.dispose();
		}
		jsonMessage = new JSONObject();
		super.dispose();
	}

	@Override
	public void createPartControl(Composite parent) {
		SwtUtils.refreshTheme();
		
		parent.setLayout(new GridLayout(1, true));
		ToolBar tb = new ToolBar(parent, SWT.FLAT | SWT.WRAP | SWT.RIGHT);
		tb.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		browser = new C8oBrowser(parent, SWT.NONE);
		
		browser.getBrowser().engine().setTheme(Theme.LIGHT);

		startupUrl = resolveAssistantStartupUrl();

		browser.addToolItemOpenExternal(tb);
		new ToolItem(tb, SWT.SEPARATOR);

		var ti = new ToolItem(tb, SWT.NONE);
		try {
			ti.setImage(ConvertigoPlugin.getDefault().getStudioIcon("icons/setupwizard_16x16.gif"));
		} catch (Exception e1) {
		}
		ti.setText("Home");
		ti.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				startupUrl = resolveAssistantStartupUrl();
				browser.setUrl(startupUrl);
			}

		});
		
		new ToolItem(tb, SWT.SEPARATOR);
		browser.addToolItemNavigation(tb);
		
		browser.setLayoutData(new GridData(GridData.FILL_BOTH));
		browser.setUseExternalBrowser(true);
		if (!Engine.isStarted) {
			setToolbarEnabled(tb, false);
			browser.setText(WAITING_HTML);
		}
		browser.onClick(ev -> {
			try {
				Element elt = (Element) ev.target().get();
				while (!elt.nodeName().equalsIgnoreCase("a")) {
					elt = (Element) elt.parent().get();
				}
				String href = elt.attributes().get("href");
				if (href.equals("#opentutorialview")) {
					ConvertigoPlugin.asyncExec(() -> {
						new OpenTutorialView().run(null);
					});
					ev.preventDefault();
					return true;
				} else if (href.startsWith("#") || elt.attributes().get("id").startsWith("weglot")) {
					return true;
				}
			} catch (Exception e) {
			}
			return false;
		});
		ConvertigoPlugin.logStudioDebug("[Assistant] debug : " + browser.getDebugUrl());
		
		handler = new C8oBrowserPostMessageHelper(browser);
		handler.onMessage(json -> {
			ConvertigoPlugin.logStudioDebug("[Assistant] onMessage: " + json);
			try {
				if ("create".equals(json.getString("type"))) {
					ConvertigoPlugin.asyncExec(() -> {
						create(json);
					});
				}
				else if ("edit".equals(json.getString("type"))) {
					ConvertigoPlugin.asyncExec(() -> {
						edit(json);
					});
				}
				else if ("capture".equals(json.getString("type"))) {
					ConvertigoPlugin.asyncExec(() -> {
						capture();
					});
				}
				else if ("lib_ConvertigoAssistant.context.request".equals(json.getString("type"))) {
					ConvertigoPlugin.asyncExec(() -> {
						postAssistantContext();
					});
				}
				else if ("lib_ConvertigoAssistant.activateLocalAgent".equals(json.getString("type"))) {
					activateLocalAgentStack(json);
				}
				else if ("lib_ConvertigoAssistant.openExternal".equals(json.getString("type"))) {
					openExternal(json);
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});
		handler.onLoad(event -> {
			// post init message
			try {
				var json = new JSONObject();
				json.put("type", "init");
				handler.postMessage(json);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			
			// post select message
			try {
				if (jsonMessage.has("type") && "select".equals(jsonMessage.getString("type"))) {
					handler.postMessage(jsonMessage);
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			
		});

		ConvertigoPlugin.runAtStartup(() -> {
			if (browser == null || browser.isDisposed()) {
				return;
			}
			startupUrl = resolveAssistantStartupUrl();
			setToolbarEnabled(tb, true);
			browser.setUrl(startupUrl);
		});
		
		Runnable initPev = () -> {
			ProjectExplorerView pev = ConvertigoPlugin.getDefault().getProjectExplorerView();
			if (pev == null) {
				return;
			}
			ISelectionChangedListener selectionListener = new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent e) {
					if (browser != null && browser.isDisposed()) {
						pev.removeSelectionChangedListener(this);
						jsonMessage = new JSONObject();
						return;
					}
					@SuppressWarnings("unused")
					ApplicationComponent app = null;
					Project p = null;
					try {
						TreeSelection selection = (TreeSelection) e.getSelection();
						TreeObject to = (TreeObject) selection.getFirstElement();
						ProjectTreeObject prjtree = to.getProjectTreeObject();
						p = prjtree != null ? prjtree.getObject() : null;
					} catch (Exception ex) {
						p = null;
					}
					try {
						app = p != null ? (ApplicationComponent) p.getMobileApplication().getApplicationComponent() : null;
					} catch (Exception ex) {
						app = null;
					}
					try {
						String pname = p != null ? p.getName() : "";
						String projectName = jsonMessage.has("projectName") ? jsonMessage.getString("projectName") : null;
						if (projectName == null || !projectName.equals(pname)) {
							// set select message
							setSelectMessage(p);
							// post project message
							handler.postMessage(jsonMessage);
						}
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			};
			pev.addSelectionChangedListener(selectionListener);
			selectionListener.selectionChanged(new SelectionChangedEvent(pev.viewer, pev.viewer.getSelection()));
		};
		
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().addPartListener(new IPartListener2() {
			@Override
			public void partOpened(IWorkbenchPartReference partRef) {
				if (browser != null && browser.isDisposed()) {
					partRef.getPage().removePartListener(this);
					return;
				}
				if (partRef.getPart(false) instanceof ProjectExplorerView) {
					ConvertigoPlugin.asyncExec(initPev);
				}
			}
		});
		ConvertigoPlugin.asyncExec(initPev);
		
	}

	private void capture() {
		try {
			String base64 = null;
			IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			if (activePage != null) {
				IEditorReference[] editorRefs = activePage.getEditorReferences();
				for (int i = 0; i < editorRefs.length; i++) {
					IEditorReference editorRef = (IEditorReference) editorRefs[i];
					String project = jsonMessage.has("projectName") ? jsonMessage.getString("projectName") : "";
					IEditorInput editorInput = editorRef.getEditorInput();
					if ((editorInput != null) && (editorInput instanceof ApplicationComponentEditorInput)) {
						if (((ApplicationComponentEditorInput) editorInput).getApplication().getProject().getName().equals(project)) {
							ApplicationComponentEditor editorPart = (ApplicationComponentEditor) editorRef.getEditor(false);
							base64 = editorPart.captureToBase64HtmlString();
							break;
						}
					}
				}
				if (base64 != null) {
					if (browser != null && browser.isDisposed()) {
						return;
					}
					JSONObject jo = new JSONObject();
					jo.put("type", "capture")
					.put("filename", "capture"+ counter++ +".jpg")
					.put("filetype", "image/jpg")
					.put("filedata", base64);
					handler.postMessage(jo);
					ConvertigoPlugin.logStudioInfo("[Assistant] capture component: image succesfully sent");
				} else {
					ConvertigoPlugin.logStudioWarn("[Assistant] unable to make capture: editor not found");
				}
			}
		} catch (Exception e) {
			ConvertigoPlugin.logStudioError("[Assistant] unable to make capture", e);
		}
	}

	private void edit(JSONObject json) {
		try {
			var sXml = json.getString("clipboard");
			var threadid = json.getString("threadid");
			
			ProjectExplorerView pev = ConvertigoPlugin.getDefault().getProjectExplorerView();
			if (pev != null) {
				UISharedComponent found = null;
				for (ProjectTreeObject pto: pev.getOpenedProjects()) {
					if (found != null) break;
					Project project = pto.getObject();
					MobileApplication ma = project.getMobileApplication();
					if (ma != null) {
						ApplicationComponent app = null;
						try {
							app = (ApplicationComponent) ma.getApplicationComponent();
						} catch (Exception e) {}
						if (app != null) {
							for (UISharedComponent uisc: app.getSharedComponentList()) {
								if (uisc.getComment().indexOf(threadid) != -1) {
									found = uisc;
									break;
								}
							}
						}
					}
				}
				
				if (found != null) {
					FormatedContent oldScriptContent = found.getScriptContent();
					FormatedContent newScriptContent = found.getScriptContent();
					Clipboard clipboard = new Clipboard();
					for (Object ob: clipboard.fromXml(sXml)) {
						if (ob instanceof UISharedComponent) {
							UISharedComponent uisc = (UISharedComponent)ob;
							if (uisc.getComment().endsWith(threadid)) {
								newScriptContent = uisc.getScriptContent();
								found.setScriptContent(newScriptContent);
								for (UIComponent uic: found.getUIComponentList()) {
									found.remove(uic);
								}
								for (UIComponent uic: uisc.getUIComponentList()) {
									found.add(uic);
								}
								found.hasChanged = true;
								break;
							}
						}
					}
					TreeObject tto = pev.findTreeObjectByUserObject(found);
					pev.objectChanged(new CompositeEvent(found, tto.getPath()));
					TreeObjectEvent treeObjectEvent = new TreeObjectEvent(tto, "scriptContent", oldScriptContent, newScriptContent);
					pev.fireTreeObjectPropertyChanged(treeObjectEvent);
					ConvertigoPlugin.logStudioInfo("[Assistant] edit component: clipboard succesfully added");
				} else {
					ConvertigoPlugin.logStudioWarn("[Assistant] component with threadid '"+threadid+"' not found, try to create it instead");
					create(json);
				}
			}
		} catch (Exception e) {
			ConvertigoPlugin.logStudioError("[Assistant] unable to edit component", e);
		}
	}

	private void create(JSONObject json) {
		try {			
			var sXml = json.getString("clipboard");
			
			ProjectExplorerView pev = ConvertigoPlugin.getDefault().getProjectExplorerView();
			if (pev != null) {
				DatabaseObjectTreeObject doto = pev.getFirstSelectedDatabaseObjectTreeObject();
				if (doto != null) {
					ApplicationComponent app = null;
					try {
						app = (ApplicationComponent) doto.getObject().getProject().getMobileApplication().getApplicationComponent();
					} catch (Exception e) {}
					if (app != null) {
						// add assets
						if (json.has("assets")) {
							File assetsDir = new File(app.getParent().getResourceFolder(), "assets");
							JSONArray arr = json.getJSONArray("assets");
							for (int i = 0; i < arr.length(); i++) {
								addAsset(assetsDir, arr.getJSONObject(i));
							}
						}
						// paste clipboard
						ConvertigoPlugin.clipboardManagerSystem.paste(sXml, app, true);
						TreeObject tto = pev.findTreeObjectByUserObject(app);
						pev.objectChanged(new CompositeEvent(app, tto.getPath()));
						ConvertigoPlugin.logStudioInfo("[Assistant] create component: clipboard succesfully added");
					} else {
						ConvertigoPlugin.logStudioInfo("[Assistant] unable to create component for non ngx application");
					}
				}
			}
		} catch (Exception e) {
			ConvertigoPlugin.logStudioError("[Assistant] unable to create component from clipboard", e);
		}
	}

	private void addAsset(File assetsDir, JSONObject jsonObject) {
		try {
			String filename = jsonObject.getString("filename");
			String content = jsonObject.getString("content");
			String type = jsonObject.getString("type");
			
			File image = new File(assetsDir, filename);
			if ("image/svg+xml".equals(type)) {
				FileUtils.writeFile(image, content, Charset.forName("UTF-8"));
			} else if ("image/png".equals(type)) {
				byte[] decodedBytes = Base64.getDecoder().decode(content);
				FileUtils.writeByteArrayToFile(image, decodedBytes);
			}
		} catch (Exception e) {
			ConvertigoPlugin.logStudioWarn("[Assistant] could not add asset: " + e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public void setFocus() {
	}

	protected void setSelectMessage(Project p) {
		try {
			String pname = p != null ? p.getName() : "";
			jsonMessage.put("type", "select");
			jsonMessage.put("projectName", pname);
			addAgentProfile(jsonMessage, p);
			addViewerDebugContext(jsonMessage, p);
			ConvertigoPlugin.logStudioInfo("[Assistant] set json message: " + jsonMessage.toString());
		} catch (Exception e) {
			ConvertigoPlugin.logStudioWarn("[Assistant] could not set json message: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void postAssistantContext() {
		try {
			if (browser == null || browser.isDisposed() || handler == null) {
				return;
			}
			String assistantUrl = Objects.toString(browser.getURL(), startupUrl);
			String localConvertigoUrl = getLocalConvertigoUrl();
			boolean assistantLocal = isLocalConvertigoUrl(assistantUrl, localConvertigoUrl);
			LocalAgentStackState stackState = getLocalAgentStackState();
			boolean assistantInstalled = stackState.assistantInstalled;
			boolean mcpInstalled = stackState.mcpInstalled;
			boolean bridgeInstalled = stackState.bridgeInstalled;
			boolean localStackAvailable = assistantInstalled && mcpInstalled && bridgeInstalled && !stackState.loading;
			JSONObject payload = new JSONObject();
			payload.put("assistantSurface", "studio");
			payload.put("assistantContext", "studio");
			payload.put("agentOnboardingFeatureVersion", AGENT_ONBOARDING_FEATURE_VERSION);
			payload.put("studioVersion", ProductVersion.fullProductVersion);
			payload.put("assistantUrl", assistantUrl);
			payload.put("assistantRuntime", assistantLocal ? "local" : "remote");
			payload.put("localConvertigoUrl", localConvertigoUrl);
			payload.put("localAssistantUrl", getLocalAssistantUrl());
			payload.put("downloadUrl", AGENT_DOWNLOAD_URL);
			payload.put("canActivateLocalAgent", true);
			payload.put("localAssistantInstalled", assistantInstalled);
			payload.put("localMcpInstalled", mcpInstalled);
			payload.put("localAgentBridgeInstalled", bridgeInstalled);
			payload.put("localAssistantVersion", stackState.assistantVersion);
			payload.put("localMcpVersion", stackState.mcpVersion);
			payload.put("localAgentBridgeVersion", stackState.bridgeVersion);
			payload.put("localAgentStackUpdateAllowed", stackState.updateAllowed);
			payload.put("localAgentStackAvailable", localStackAvailable);
			payload.put("localAgentStackState", stackState.state);
			payload.put("localAgentStackLoading", stackState.loading);
			payload.put("localAgentBridgeAvailable", assistantLocal && localStackAvailable && !stackState.loading);
			payload.put("agentBridgeAvailable", assistantLocal && localStackAvailable && !stackState.loading);
			try {
				if (jsonMessage.has("projectName")) {
					payload.put("projectName", jsonMessage.getString("projectName"));
					payload.put("defaultProject", jsonMessage.getString("projectName"));
				}
				if (jsonMessage.has("threadQname")) {
					payload.put("threadQname", jsonMessage.getString("threadQname"));
				}
			} catch (Exception e) {
			}
			addAgentProfile(payload, null);
			addViewerDebugContext(payload, null);
			JSONObject message = new JSONObject();
			message.put("type", "lib_ConvertigoAssistant.context");
			message.put("payload", payload);
			handler.postMessage(message);
			ConvertigoPlugin.logStudioDebug("[Assistant] context: " + message.toString());
			scheduleLocalAgentStackContextRecheck(stackState);
		} catch (Exception e) {
			ConvertigoPlugin.logStudioWarn("[Assistant] could not post context: " + e.getMessage());
		}
	}

	private static String addDarkThemeParameter(String url) {
		url = StringUtils.defaultIfBlank(url, STARTUP_URL);
		return url + (url.contains("?") ? "&" : "?") + "dark-theme=" + SwtUtils.isDark();
	}

	private static String resolveAssistantStartupUrl() {
		String url = STARTUP_URL;
		try {
			var u = ConvertigoPlugin.getProperty(ConvertigoPlugin.PREFERENCE_ASSISTANT_URL);
			if (StringUtils.isNotBlank(u)) {
				url = ConvertigoPlugin.resolveStudioUrl(u);
			}
		} catch (Exception e) {
		}
		url = addDarkThemeParameter(removeAgentProfileParameters(url));
		if (!isLocalConvertigoUrl(url, getLocalConvertigoUrl())) {
			return url;
		}
		try {
			URI uri = new URI(url);
			URI localUri = new URI(getLocalConvertigoUrl());
			String path = Objects.toString(uri.getRawPath(), "/");
			String localPath = Objects.toString(localUri.getRawPath(), "").replaceFirst("/+$", "");
			if (StringUtils.isNotBlank(localPath) && path.startsWith(localPath + "/")) {
				path = path.substring(localPath.length());
			} else if (path.equals(localPath)) {
				path = "/";
			}
			if (StringUtils.isNotBlank(uri.getRawQuery())) {
				path += "?" + uri.getRawQuery();
			}
			return AdminView.getAuthenticatedUrl(path);
		} catch (Exception e) {
			ConvertigoPlugin.logStudioWarn("[Assistant] unable to create authenticated local URL: " + e.getMessage());
			return url;
		}
	}

	private static String getUrlQuery(String url) {
		try {
			return StringUtils.defaultString(new URI(Objects.toString(url, "")).getRawQuery());
		} catch (Exception e) {
			String value = Objects.toString(url, "");
			int queryStart = value.indexOf('?');
			if (queryStart == -1) {
				return "";
			}
			int fragmentStart = value.indexOf('#', queryStart);
			return value.substring(queryStart + 1, fragmentStart == -1 ? value.length() : fragmentStart);
		}
	}

	private static String removeAgentProfileParameters(String url) {
		try {
			String value = Objects.toString(url, "");
			int fragmentStart = value.indexOf('#');
			String fragment = fragmentStart == -1 ? "" : value.substring(fragmentStart);
			String withoutFragment = fragmentStart == -1 ? value : value.substring(0, fragmentStart);
			int queryStart = withoutFragment.indexOf('?');
			if (queryStart == -1) {
				return value;
			}
			StringBuilder query = new StringBuilder();
			for (String part : withoutFragment.substring(queryStart + 1).split("&")) {
				String key = part.contains("=") ? part.substring(0, part.indexOf('=')) : part;
				if (!part.isBlank() && !"agentProfile".equals(key) && !"skillProfile".equals(key)) {
					if (query.length() > 0) {
						query.append('&');
					}
					query.append(part);
				}
			}
			return withoutFragment.substring(0, queryStart)
					+ (query.length() == 0 ? "" : "?" + query)
					+ fragment;
		} catch (Exception e) {
			return Objects.toString(url, "")
					.replaceAll("([?&])(?:agentProfile|skillProfile)=[^&#]*&?", "$1")
					.replace("?&", "?")
					.replaceAll("[?&]$", "");
		}
	}

	private static String getLocalConvertigoUrl() {
		try {
			return Strings.CS.removeEnd(EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL), "/");
		} catch (Exception e) {
			return "http://localhost:18080/convertigo";
		}
	}

	private static String getLocalAssistantUrl() {
		return ConvertigoPlugin.resolveStudioUrl(LOCAL_ASSISTANT_PATH);
	}

	private static boolean isProjectInstalled(String projectName) {
		try {
			return Engine.theApp != null
					&& Engine.theApp.databaseObjectsManager != null
					&& Engine.theApp.databaseObjectsManager.existsProject(projectName);
		} catch (Exception e) {
			return false;
		}
	}

	private static Project getInstalledProject(String projectName) {
		try {
			if (Engine.theApp != null && Engine.theApp.databaseObjectsManager != null) {
				return Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName, false);
			}
		} catch (Exception e) {
		}
		return null;
	}

	private static String getInstalledProjectVersion(String projectName) {
		Project project = getInstalledProject(projectName);
		return project == null ? "" : Objects.toString(project.getVersion(), "");
	}

	private static boolean isReleaseManagedProject(String projectName) {
		Project project = getInstalledProject(projectName);
		return project == null || GitUtils.getWorkingDir(project.getDirFile()) == null;
	}

	private LocalAgentStackState getLocalAgentStackState() {
		LocalAgentStackState state = new LocalAgentStackState();
		state.assistantInstalled = isProjectInstalled("lib_ConvertigoAssistant");
		state.mcpInstalled = isProjectInstalled("lib_ConvertigoMCP");
		state.bridgeInstalled = isProjectInstalled("lib_ConvertigoAgentBridge");
		state.assistantVersion = getInstalledProjectVersion("lib_ConvertigoAssistant");
		state.mcpVersion = getInstalledProjectVersion("lib_ConvertigoMCP");
		state.bridgeVersion = getInstalledProjectVersion("lib_ConvertigoAgentBridge");
		state.updateAllowed = isReleaseManagedProject("lib_ConvertigoAssistant")
				&& isReleaseManagedProject("lib_ConvertigoMCP")
				&& isReleaseManagedProject("lib_ConvertigoAgentBridge");
		boolean allInstalled = state.assistantInstalled && state.mcpInstalled && state.bridgeInstalled;
		boolean opening = false;
		boolean presentButNotInstalled = false;
		for (String projectName : LOCAL_AGENT_STACK_PROJECTS) {
			opening |= isProjectOpening(projectName);
			presentButNotInstalled |= !isProjectInstalled(projectName) && isProjectPresentInWorkspace(projectName);
		}
		boolean shouldWait = opening || presentButNotInstalled;
		if (allInstalled && !opening) {
			localAgentStackLoadingStartedAt = 0L;
			state.state = "ready";
			state.loading = false;
			return state;
		}
		if (shouldWait) {
			long now = System.currentTimeMillis();
			if (localAgentStackLoadingStartedAt == 0L) {
				localAgentStackLoadingStartedAt = now;
			}
			if (now - localAgentStackLoadingStartedAt < LOCAL_AGENT_STACK_LOADING_TIMEOUT_MS) {
				state.state = "loading";
				state.loading = true;
				return state;
			}
		}
		localAgentStackLoadingStartedAt = 0L;
		state.state = "missing";
		state.loading = false;
		return state;
	}

	private void scheduleLocalAgentStackContextRecheck(LocalAgentStackState state) {
		if (state == null || !state.loading || localAgentStackContextRecheckScheduled) {
			return;
		}
		localAgentStackContextRecheckScheduled = true;
		Job.create("Recheck local Assistant agent stack", monitor -> {
			try {
				Thread.sleep(LOCAL_AGENT_STACK_LOADING_RECHECK_MS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			ConvertigoPlugin.asyncExec(() -> {
				localAgentStackContextRecheckScheduled = false;
				postAssistantContext();
			});
		}).schedule();
	}

	private static boolean isProjectOpening(String projectName) {
		String jobName = "Opening project " + projectName;
		for (Job job : Job.getJobManager().find(null)) {
			try {
				if (jobName.equals(job.getName()) && job.getState() != Job.NONE) {
					return true;
				}
			} catch (Exception e) {
			}
		}
		return false;
	}

	private static boolean isProjectPresentInWorkspace(String projectName) {
		try {
			File projectFile = ConvertigoPlugin.getDefault().getProject(projectName);
			if (projectFile != null && projectFile.exists()) {
				return true;
			}
		} catch (Exception e) {
		}
		try {
			File projectDir = new File(Engine.PROJECTS_PATH, projectName);
			return new File(projectDir, "c8oProject.yaml").exists()
					|| new File(projectDir, projectName + ".xml").exists();
		} catch (Exception e) {
			return false;
		}
	}

	private static class LocalAgentStackState {
		boolean assistantInstalled;
		boolean mcpInstalled;
		boolean bridgeInstalled;
		boolean loading;
		boolean updateAllowed;
		String assistantVersion = "";
		String mcpVersion = "";
		String bridgeVersion = "";
		String state = "missing";
	}

	private static boolean isLocalHost(String host) {
		host = Objects.toString(host, "").toLowerCase();
		return "localhost".equals(host) || "127.0.0.1".equals(host) || "::1".equals(host) || "[::1]".equals(host);
	}

	private static boolean isLocalConvertigoUrl(String candidateUrl, String localConvertigoUrl) {
		try {
			URI candidate = new URI(Objects.toString(candidateUrl, ""));
			URI local = new URI(Objects.toString(localConvertigoUrl, ""));
			if (candidate.getHost() == null || local.getHost() == null) {
				return false;
			}
			boolean sameHost = candidate.getHost().equalsIgnoreCase(local.getHost())
					|| (isLocalHost(candidate.getHost()) && isLocalHost(local.getHost()));
			return sameHost && candidate.getPort() == local.getPort()
					&& Objects.toString(candidate.getPath(), "").startsWith(Objects.toString(local.getPath(), ""));
		} catch (Exception e) {
			return false;
		}
	}

	private void activateLocalAgentStack(JSONObject message) {
		JSONObject payload = new JSONObject();
		try {
			if (message.has("payload")) {
				payload = message.getJSONObject("payload");
			}
		} catch (Exception e) {
		}
		final JSONObject activationPayload = payload;
		Job.create("Activate local Convertigo Assistant agent", monitor -> {
			monitor.beginTask("Activating local Assistant agent", IProgressMonitor.UNKNOWN);
			try {
				boolean forceUpdate = getPayloadBoolean(activationPayload, "forceUpdate");
				postActivationStatus("running", forceUpdate ? "Mise à jour de la stack Agent locale..." : "Activation de l'assistant local...", "", false);
				JSONArray projects = getActivationProjects(activationPayload);
				if (projects.length() == 0) {
					throw new IllegalArgumentException("No project import URL provided by the Assistant");
				}
				for (int i = 0; i < projects.length(); i++) {
					JSONObject item = projects.getJSONObject(i);
					String projectName = item.getString("name");
					String importUrl = item.getString("importUrl");
					boolean updateProject = forceUpdate || getPayloadBoolean(item, "forceUpdate");
					if (isProjectInstalled(projectName) && !updateProject) {
						postActivationStatus("running", projectName + " est déjà installé.", projectName, false);
						continue;
					}
					postActivationStatus("running", (updateProject ? "Mise à jour de " : "Import de ") + projectName + "...", projectName, false);
					ProjectUrlParser parser = new ProjectUrlParser(importUrl);
					var project = Engine.theApp.referencedProjectManager.importProject(parser, true);
					if (project == null) {
						throw new IllegalStateException("Unable to import " + projectName);
					}
				}
				String preferenceUrl = getActivationAssistantPreferenceUrl(activationPayload);
				ConvertigoPlugin.setProperty(ConvertigoPlugin.PREFERENCE_ASSISTANT_URL, preferenceUrl);
				startupUrl = resolveAssistantStartupUrl();
				postActivationStatus("success", forceUpdate ? "Stack Agent locale mise à jour." : "Assistant local activé.", "", true);
				ConvertigoPlugin.asyncExec(() -> {
					try {
						if (browser != null && !browser.isDisposed()) {
							browser.setUrl(startupUrl);
						}
					} catch (Exception e) {
						ConvertigoPlugin.logStudioWarn("[Assistant] unable to reload local Assistant: " + e.getMessage());
					}
				});
			} catch (Exception e) {
				ConvertigoPlugin.logStudioError("[Assistant] unable to activate local Assistant agent", e);
				postActivationStatus("error", "Activation impossible : " + e.getMessage(), "", true);
			} finally {
				monitor.done();
			}
		}).schedule();
	}

	private void openExternal(JSONObject message) {
		try {
			String url = "";
			if (message.has("url")) {
				url = message.getString("url");
			} else if (message.has("payload")) {
				JSONObject payload = message.getJSONObject("payload");
				if (payload.has("url")) {
					url = payload.getString("url");
				}
			}
			final String target = StringUtils.trimToEmpty(url);
			if (target.startsWith("http://") || target.startsWith("https://")) {
				ConvertigoPlugin.asyncExec(() -> Program.launch(target));
			}
		} catch (Exception e) {
			ConvertigoPlugin.logStudioWarn("[Assistant] unable to open external URL: " + e.getMessage());
		}
	}

	private JSONArray getActivationProjects(JSONObject payload) {
		try {
			if (payload.has("projects")) {
				return payload.getJSONArray("projects");
			}
		} catch (Exception e) {
		}
		JSONArray projects = new JSONArray();
		return projects;
	}

	private static String getActivationAssistantPreferenceUrl(JSONObject payload) {
		String value = getPayloadString(payload, "assistantUrl");
		if (StringUtils.isBlank(value)) {
			value = getPayloadString(payload, "assistantPreferenceUrl");
		}
		if (StringUtils.isBlank(value)) {
			value = getPayloadString(payload, "localAssistantPath");
		}
		return StringUtils.defaultIfBlank(value, LOCAL_ASSISTANT_PATH);
	}

	private static String getPayloadString(JSONObject payload, String key) {
		try {
			if (payload != null && payload.has(key)) {
				return StringUtils.trimToEmpty(payload.getString(key));
			}
		} catch (Exception e) {
		}
		return "";
	}

	private static boolean getPayloadBoolean(JSONObject payload, String key) {
		try {
			return payload != null && payload.has(key) && payload.getBoolean(key);
		} catch (Exception e) {
			return false;
		}
	}

	private void postActivationStatus(String status, String message, String projectName, boolean done) {
		ConvertigoPlugin.asyncExec(() -> {
			try {
				if (handler == null || browser == null || browser.isDisposed()) {
					return;
				}
				JSONObject payload = new JSONObject();
				payload.put("status", status);
				payload.put("message", message);
				payload.put("projectName", Objects.toString(projectName, ""));
				payload.put("done", done);
				payload.put("localAssistantUrl", getLocalAssistantUrl());
				JSONObject msg = new JSONObject();
				msg.put("type", "lib_ConvertigoAssistant.activateLocalAgent.status");
				msg.put("payload", payload);
				handler.postMessage(msg);
			} catch (Exception e) {
				ConvertigoPlugin.logStudioWarn("[Assistant] unable to post activation status: " + e.getMessage());
			}
		});
	}
	
	protected void setSelectMessage(String qname) {
		try {
			jsonMessage.put("type", "select");
			jsonMessage.put("threadQname", qname);
			jsonMessage.put("projectName", qname.substring(0, qname.indexOf('.')));
			addAgentProfile(jsonMessage, null);
			addViewerDebugContext(jsonMessage, null);
			ConvertigoPlugin.logStudioInfo("[Assistant] set json message: " + jsonMessage.toString());
		} catch (Exception e) {
			ConvertigoPlugin.logStudioWarn("[Assistant] could not set json message: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void addAgentProfile(JSONObject json, Project project) {
		String profile = "generalist";
		try {
			String projectName = project != null ? project.getName() : "";
			if (StringUtils.isBlank(projectName) && json.has("projectName")) {
				projectName = json.getString("projectName");
			}
			if (project == null && StringUtils.isNotBlank(projectName) && Engine.theApp != null) {
				project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName, false);
			}
			profile = project != null && project.getFlowEngine() != null ? "flow" : "generalist";
			json.put("agentProfile", profile);
			json.put("skillProfile", profile);
		} catch (Exception e) {
			try {
				json.put("agentProfile", profile);
				json.put("skillProfile", profile);
			} catch (Exception ignored) {
			}
		}
	}

	private void addViewerDebugContext(JSONObject json, Project project) {
		try {
			String projectName = project != null ? project.getName() : "";
			try {
				if (StringUtils.isBlank(projectName) && json.has("projectName")) {
					projectName = json.getString("projectName");
				}
			} catch (Exception e) {
			}
			String debugUrl = getViewerDebugUrl(project, projectName);
			if (StringUtils.isNotBlank(debugUrl)) {
				json.put("browserDebugUrl", debugUrl);
				json.put("browserDevToolsJsonUrl", debugUrl + "/json");
				json.put("playwrightCdpEndpoint", debugUrl);
				json.put("viewerCdpEndpoint", debugUrl);
			} else {
				json.put("browserDebugUrl", "");
				json.put("browserDevToolsJsonUrl", "");
				json.put("playwrightCdpEndpoint", "");
				json.put("viewerCdpEndpoint", "");
			}
		} catch (Exception e) {
			ConvertigoPlugin.logStudioWarn("[Assistant] could not add viewer debug context: " + e.getMessage());
		}
	}

	private String getViewerDebugUrl(Project project, String projectName) {
		projectName = Objects.toString(projectName, "");
		try {
			IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			if (activePage == null) {
				return "";
			}
			String activeDebugUrl = getViewerDebugUrl(activePage.getActiveEditor(), projectName);
			if (StringUtils.isNotBlank(activeDebugUrl)) {
				return activeDebugUrl;
			}
			for (IEditorReference editorRef: activePage.getEditorReferences()) {
				try {
					IEditorInput editorInput = editorRef.getEditorInput();
					if (editorInput instanceof ApplicationComponentEditorInput) {
						ApplicationComponentEditorInput input = (ApplicationComponentEditorInput) editorInput;
						if (StringUtils.isBlank(projectName) || input.getApplication().getProject().getName().equals(projectName)) {
							IEditorPart editorPart = editorRef.getEditor(false);
							String debugUrl = getViewerDebugUrl(editorPart, projectName);
							if (StringUtils.isNotBlank(debugUrl)) {
								return debugUrl;
							}
						}
					} else if (editorInput instanceof com.twinsoft.convertigo.eclipse.editors.mobile.ApplicationComponentEditorInput) {
						com.twinsoft.convertigo.eclipse.editors.mobile.ApplicationComponentEditorInput input =
								(com.twinsoft.convertigo.eclipse.editors.mobile.ApplicationComponentEditorInput) editorInput;
						if (StringUtils.isBlank(projectName) || input.getApplication().getProject().getName().equals(projectName)) {
							IEditorPart editorPart = editorRef.getEditor(false);
							String debugUrl = getViewerDebugUrl(editorPart, projectName);
							if (StringUtils.isNotBlank(debugUrl)) {
								return debugUrl;
							}
						}
					} else if (editorInput instanceof FlowEngineEditorInput) {
						FlowEngineEditorInput input = (FlowEngineEditorInput) editorInput;
						if (StringUtils.isBlank(projectName) || input.getProjectName().equals(projectName)) {
							IEditorPart editorPart = editorRef.getEditor(false);
							String debugUrl = getViewerDebugUrl(editorPart, projectName);
							if (StringUtils.isNotBlank(debugUrl)) {
								return debugUrl;
							}
						}
					}
				} catch (Exception e) {
				}
			}
		} catch (Exception e) {
		}
		return "";
	}

	private String getViewerDebugUrl(IEditorPart editorPart, String projectName) {
		try {
			if (editorPart instanceof ApplicationComponentEditor) {
				return ((ApplicationComponentEditor) editorPart).getDebugUrl();
			} else if (editorPart instanceof com.twinsoft.convertigo.eclipse.editors.mobile.ApplicationComponentEditor) {
				return ((com.twinsoft.convertigo.eclipse.editors.mobile.ApplicationComponentEditor) editorPart).getDebugUrl();
			} else if (editorPart instanceof FlowEngineEditor) {
				return ((FlowEngineEditor) editorPart).getDebugUrl();
			}
		} catch (Exception e) {
		}
		return "";
	}
	
	public void changeThread(String qname, String threadId) {
		if (qname != null && threadId != null && !threadId.isBlank()) {
			// set select message
			setSelectMessage(qname);
			
			// set url
			String burl = browser.getURL();
			String query = getUrlQuery(burl);
			if (StringUtils.isBlank(query)) {
				query = getUrlQuery(startupUrl);
			}
			int idx = burl.indexOf("/DisplayObjects/mobile");
			if (idx != -1) {
				burl = burl.substring(0, idx) + "/DisplayObjects/mobile";
			}
			int idy = burl.indexOf("/path-to-xfirst");
			if (idy != -1) {
				burl = burl.substring(0, idy);
			}
			String url = burl + "/path-to-xfirst/" + threadId
					+ (StringUtils.isBlank(query) ? "" : "?" + query);
			ConvertigoPlugin.logStudioInfo("[Assistant] url: " + url);
			browser.setUrl(url);			
		}
	}

	private static void setToolbarEnabled(ToolBar toolbar, boolean enabled) {
		if (toolbar == null || toolbar.isDisposed()) {
			return;
		}
		toolbar.setEnabled(enabled);
	}

}
