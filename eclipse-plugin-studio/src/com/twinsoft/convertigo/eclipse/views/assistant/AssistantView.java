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

package com.twinsoft.convertigo.eclipse.views.assistant;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Base64;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.SWT;
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
	public static final String STARTUP_URL = "https://beta.convertigo.net/convertigo/projects/ConvertigoAssistant/DisplayObjects/mobile/";

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
		
		browser.addToolItemNavigation(tb);
		new ToolItem(tb, SWT.SEPARATOR);
		browser.addToolItemOpenExternal(tb);
		
		browser.setLayoutData(new GridData(GridData.FILL_BOTH));
		browser.setUseExternalBrowser(true);
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
		Engine.logStudio.debug("[Assistant] debug : "+ browser.getDebugUrl());
		
		String url = STARTUP_URL;
		//url = "http://localhost:47563/path-to-xfirst";
		//url = "http://localhost:28080/convertigo/projects/ConvertigoAssistant/DisplayObjects/mobile/";
		
		handler = new C8oBrowserPostMessageHelper(browser);
		handler.onMessage(json -> {
			Engine.logStudio.debug("[Assistant] onMessage: " + json);
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

		browser.setUrl(url);
		
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
					Engine.logStudio.info("[Assistant] capture component: image succesfully sent");
				} else {
					Engine.logStudio.warn("[Assistant] unable to make capture: editor not found");
				}
			}
		} catch (Exception e) {
			Engine.logStudio.error("[Assistant] unable to make capture", e);
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
					Engine.logStudio.info("[Assistant] edit component: clipboard succesfully added");
				} else {
					Engine.logStudio.warn("[Assistant] component with threadid '"+threadid+"' not found, try to create it instead");
					create(json);
				}
			}
		} catch (Exception e) {
			Engine.logStudio.error("[Assistant] unable to edit component", e);
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
						Engine.logStudio.info("[Assistant] create component: clipboard succesfully added");
					} else {
						Engine.logStudio.info("[Assistant] unable to create component for non ngx application");
					}
				}
			}
		} catch (Exception e) {
			Engine.logStudio.error("[Assistant] unable to create component from clipboard", e);
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
			Engine.logStudio.warn("[Assistant] could not add asset: "+ e.getMessage());
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
			Engine.logStudio.info("[Assistant] set json message: "+ jsonMessage.toString());
		} catch (Exception e) {
			Engine.logStudio.warn("[Assistant] could not set json message: "+ e.getMessage());
			e.printStackTrace();
		}
	}
	
	protected void setSelectMessage(String qname) {
		try {
			jsonMessage.put("type", "select");
			jsonMessage.put("threadQname", qname);
			jsonMessage.put("projectName", qname.substring(0, qname.indexOf('.')));
			Engine.logStudio.info("[Assistant] set json message: "+ jsonMessage.toString());
		} catch (Exception e) {
			Engine.logStudio.warn("[Assistant] could not set json message: "+ e.getMessage());
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
			Engine.logStudio.info("[Assistant] url: "+ url);
			browser.setUrl(url);			
		}
	}
}
