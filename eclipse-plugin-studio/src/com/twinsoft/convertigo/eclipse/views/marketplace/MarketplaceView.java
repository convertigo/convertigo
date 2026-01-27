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

package com.twinsoft.convertigo.eclipse.views.marketplace;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.part.ViewPart;

import com.teamdev.jxbrowser.dom.Element;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.actions.OpenTutorialView;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowserPostMessageHelper;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.ProductVersion;
import com.twinsoft.convertigo.engine.util.ProjectUrlParser;

public class MarketplaceView extends ViewPart {

	public static final String ID = "com.twinsoft.convertigo.eclipse.views.marketplace.MarketplaceView";
	public static final String STARTUP_URL = "https://backend-apps.convertigo.net/convertigo/projects/marketplace/DisplayObjects/mobile/";

	private C8oBrowser browser = null;
	
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
		
		browser.addToolItemOpenExternal(tb);
		
		new ToolItem(tb, SWT.SEPARATOR);

		String[] url = {STARTUP_URL};
		try {
			var u = ConvertigoPlugin.getProperty(ConvertigoPlugin.PREFERENCE_MARKETPLACE_URL);
			if (StringUtils.isNotBlank(u)) {
				url[0] = u;
			}
		} catch (Exception e) {
		}
		
		var ti = new ToolItem(tb, SWT.NONE);
		try {
			ti.setImage(ConvertigoPlugin.getDefault().getStudioIcon("icons/studio/retail_store_color_16x16.png"));
		} catch (IOException e1) {
		}
		ti.setText("Home");
		ti.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				browser.setUrl(url[0]);
			}
			
		});
		
		browser.setLayoutData(new GridData(GridData.FILL_BOTH));
		browser.setUseExternalBrowser(false);
		Engine.logStudio.debug("Marketplace debug : "+ browser.getDebugUrl());
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
		
		var handler = new C8oBrowserPostMessageHelper(browser);
		handler.onMessage(json -> {
			Engine.logStudio.debug("Marketplace onMessage: " + json);
			try {
				if ("install".equals(json.getString("type"))) {
					var importUrl = json.getString("url");
					var parser = new ProjectUrlParser(importUrl);
					if (parser.isValid()) {
						Job.create("Import project " + parser.getProjectName(), (mon) -> {
							try {
								mon.beginTask("Loading " + parser.getProjectName(), IProgressMonitor.UNKNOWN);
								var project = Engine.theApp.referencedProjectManager.importProject(parser, true);
								try {
									var msg = new JSONObject();
									msg.put("type", "postInstall");
									msg.put("installed", project != null);
									if (project != null) {
										msg.put("project", project.getName());
										msg.put("version", project.getVersion());
									}
									handler.postMessage(msg);
								} catch (Exception e1) {
									e1.printStackTrace();
								}
								Thread.sleep(1000);
								ConvertigoPlugin.asyncExec(() -> {
									try {
										var pev = ConvertigoPlugin.getDefault().getProjectExplorerView();
										pev.objectSelected(new CompositeEvent(project));
									} catch (Exception e) {
									}
								});
							} catch (Exception e) {
								Engine.logStudio.debug("Loading from remote URL failed", e);
							}
							mon.done();
						}).schedule();
					}
				} else if ("get".equals(json.getString("type"))) {
					var projectName = json.getString("project");
					var project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
					
					try {
						var msg = new JSONObject();
						msg.put("type", "postGet");
						msg.put("project", projectName);
						msg.put("installed", project != null);
						if (project != null) {
							msg.put("project", project.getName());
							msg.put("version", project.getVersion());
						}
						handler.postMessage(msg);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});
		handler.onLoad(event -> {
			try {
				var json = new JSONObject();
				json.put("type", "init");
				json.put("version", ProductVersion.productVersion);
				handler.postMessage(json);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});

		browser.setUrl(url[0]);
	}

	@Override
	public void setFocus() {
	}

}
