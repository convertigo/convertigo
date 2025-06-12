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

import org.codehaus.jettison.json.JSONObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.part.ViewPart;

import com.teamdev.jxbrowser.engine.Theme;
import com.twinsoft.convertigo.beans.common.FormatedContent;
import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIComponent;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponent;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
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

public class AssistantView extends ViewPart {

	public static final String ID = "com.twinsoft.convertigo.eclipse.views.assistant.AssistantView";
	private static final String STARTUP_URL = "https://beta.convertigo.net/convertigo/projects/ConvertigoAssistant/DisplayObjects/mobile/";

	private C8oBrowser browser = null;
	private C8oBrowserPostMessageHelper handler = null;
	
	@Override
	public void dispose() {
		if (browser != null) {
			browser.dispose();
		}
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
		browser.setUseExternalBrowser(false);
		Engine.logStudio.debug("[Assistant] debug : "+ browser.getDebugUrl());
		
		String url = STARTUP_URL;
		//url = "http://localhost:49906/path-to-xfirst";
		//url = "http://localhost:28080/convertigo/projects/ConvertigoAssistant/DisplayObjects/mobile/";
		
		handler = new C8oBrowserPostMessageHelper(browser);
		handler.onMessage(json -> {
			Engine.logStudio.debug("[Assistant] onMessage: " + json);
			try {
				var sXml = json.getString("clipboard");
				var threadid = json.getString("threadid");
				
				if ("create".equals(json.getString("type"))) {
					Engine.logStudio.debug("[Assistant] received clipboard: " + sXml);
					ConvertigoPlugin.asyncExec(() -> {
						try {
							ProjectExplorerView pev = ConvertigoPlugin.getDefault().getProjectExplorerView();
							if (pev != null) {
								DatabaseObjectTreeObject doto = pev.getFirstSelectedDatabaseObjectTreeObject();
								if (doto != null) {
									ApplicationComponent app = null;
									try {
										app = (ApplicationComponent) doto.getObject().getProject().getMobileApplication().getApplicationComponent();
									} catch (Exception e) {}
									if (app != null) {
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
					});
				}
				else if ("edit".equals(json.getString("type"))) {
					ConvertigoPlugin.asyncExec(() -> {
						try {
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
											if (found.getComment().equals(uisc.getComment())) {
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
									Engine.logStudio.warn("[Assistant] component with threadid '"+threadid+"' not found");
								}
							}
						} catch (Exception e) {
							Engine.logStudio.error("[Assistant] unable to edit component", e);
						}
					});
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});
		handler.onLoad(event -> {
			try {
				var json = new JSONObject();
				json.put("type", "init");
				handler.postMessage(json);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});

		browser.setUrl(url);
	}

	@Override
	public void setFocus() {
	}

	public void changeThread(String threadId) {
		if (threadId != null && !threadId.isBlank()) {
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
