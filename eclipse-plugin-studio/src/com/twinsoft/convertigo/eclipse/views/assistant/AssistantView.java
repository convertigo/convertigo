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
import java.nio.charset.Charset;
import java.util.Base64;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeSelection;
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
import com.twinsoft.convertigo.eclipse.editors.ngx.ApplicationComponentEditor;
import com.twinsoft.convertigo.eclipse.editors.ngx.ApplicationComponentEditorInput;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowserPostMessageHelper;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.Clipboard;
import com.twinsoft.convertigo.engine.util.FileUtils;

public class AssistantView extends ViewPart {

	public static final String ID = "com.twinsoft.convertigo.eclipse.views.assistant.AssistantView";
	public static final String STARTUP_URL = "https://assistant.convertigo.com/";
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

		String[] url = {STARTUP_URL};
		try {
			var u = ConvertigoPlugin.getProperty(ConvertigoPlugin.PREFERENCE_ASSISTANT_URL);
			if (StringUtils.isNotBlank(u)) {
				url[0] = u;
			}
		} catch (Exception e) {
		}
		url[0] += "?dark-theme=" + SwtUtils.isDark();

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
				browser.setUrl(url[0]);
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
			setToolbarEnabled(tb, true);
			browser.setUrl(url[0]);
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
						app = (ApplicationComponent) p.getMobileApplication().getApplicationComponent();
					} catch (Exception ex) {
						p = null;
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
			ConvertigoPlugin.logStudioInfo("[Assistant] set json message: " + jsonMessage.toString());
		} catch (Exception e) {
			ConvertigoPlugin.logStudioWarn("[Assistant] could not set json message: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	protected void setSelectMessage(String qname) {
		try {
			jsonMessage.put("type", "select");
			jsonMessage.put("threadQname", qname);
			jsonMessage.put("projectName", qname.substring(0, qname.indexOf('.')));
			ConvertigoPlugin.logStudioInfo("[Assistant] set json message: " + jsonMessage.toString());
		} catch (Exception e) {
			ConvertigoPlugin.logStudioWarn("[Assistant] could not set json message: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void changeThread(String qname, String threadId) {
		if (qname != null && threadId != null && !threadId.isBlank()) {
			// set select message
			setSelectMessage(qname);
			
			// set url
			String burl = browser.getURL();
			int idx = burl.indexOf("/DisplayObjects/mobile");
			if (idx != -1) {
				burl = burl.substring(0, idx) + "/DisplayObjects/mobile";
			}
			int idy = burl.indexOf("/path-to-xfirst");
			if (idy != -1) {
				burl = burl.substring(0, idy);
			}
			String url = burl + "/path-to-xfirst/" + threadId;
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
