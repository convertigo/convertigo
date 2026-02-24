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
import java.net.URI;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.part.ViewPart;

import com.teamdev.jxbrowser.dom.Element;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowserPostMessageHelper;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.ProductVersion;
import com.twinsoft.convertigo.engine.util.ProjectUrlParser;

public class MarketplaceView extends ViewPart {

	public static final String ID = "com.twinsoft.convertigo.eclipse.views.marketplace.MarketplaceView";
	public static final String STARTUP_URL = "https://marketplace.convertigo.com/";
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
	private String startup_url = STARTUP_URL;

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

		try {
			var u = ConvertigoPlugin.getProperty(ConvertigoPlugin.PREFERENCE_MARKETPLACE_URL);
			if (StringUtils.isNotBlank(u)) {
				startup_url = u;
			}
		} catch (Exception e) {
		}

		browser.addToolItemOpenExternal(tb);
		new ToolItem(tb, SWT.SEPARATOR);

		var ti = new ToolItem(tb, SWT.NONE);
		ti.setText("Home");
		try {
			ti.setImage(ConvertigoPlugin.getDefault().getStudioIcon("icons/studio/retail_store_color_16x16.png"));
		} catch (IOException e1) {
		}
		ti.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				browser.setUrl(startup_url);
			}

		});
		
		new ToolItem(tb, SWT.SEPARATOR);
		browser.addToolItemNavigation(tb);

		browser.setLayoutData(new GridData(GridData.FILL_BOTH));
		browser.setUseExternalBrowser(false);
		if (!Engine.isStarted) {
			setToolbarEnabled(tb, false);
			browser.setText(WAITING_HTML);
		}
		ConvertigoPlugin.logStudioDebug("Marketplace debug : " + browser.getDebugUrl());
		browser.onClick(ev -> {
			try {
				Element elt = (Element) ev.target().get();
				while (!elt.nodeName().equalsIgnoreCase("a")) {
					elt = (Element) elt.parent().get();
				}
				String href = elt.attributes().get("href");
				String id = elt.attributes().get("id");
				if ((href != null && href.startsWith("#")) || (id != null && id.startsWith("weglot"))) {
					return true;
				}
				if (StringUtils.isNotBlank(href)) {
					var marketplaceHost = new URI(startup_url).getHost();
					var baseUri = StringUtils.defaultIfBlank(elt.document().baseUri(), startup_url);
					var target = new URI(baseUri).resolve(href.trim());
					var scheme = target.getScheme();
					var targetHost = target.getHost();
					boolean nonHttpScheme = scheme != null && !"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme);
					boolean externalHost = targetHost != null && marketplaceHost != null && !targetHost.equalsIgnoreCase(marketplaceHost);
					if (nonHttpScheme || externalHost) {
						Program.launch(target.toString());
						ev.preventDefault();
						return true;
					}
				}
			} catch (Exception e) {
			}
			return false;
		});

		var handler = new C8oBrowserPostMessageHelper(browser);
		handler.onMessage(json -> {
			ConvertigoPlugin.logStudioDebug("Marketplace onMessage: " + json);
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
								ConvertigoPlugin.logStudioDebug("Loading from remote URL failed", e);
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

		ConvertigoPlugin.runAtStartup(() -> {
			if (browser == null || browser.isDisposed()) {
				return;
			}
			setToolbarEnabled(tb, true);
			browser.setUrl(startup_url);
		});
	}

	@Override
	public void setFocus() {
	}

	public void openTag(String tag) {
		if (StringUtils.isBlank(tag)) {
			return;
		}
		browser.setUrl(startup_url + "?topics=" + tag + "#results");
	}

	private static void setToolbarEnabled(ToolBar toolbar, boolean enabled) {
		if (toolbar == null || toolbar.isDisposed()) {
			return;
		}
		toolbar.setEnabled(enabled);
	}

}
