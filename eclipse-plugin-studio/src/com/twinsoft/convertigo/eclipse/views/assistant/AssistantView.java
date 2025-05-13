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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.part.ViewPart;

import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowserPostMessageHelper;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.ProjectUrlParser;

public class AssistantView extends ViewPart {

	public static final String ID = "com.twinsoft.convertigo.eclipse.views.assistant.AssistantView";
	private static final String STARTUP_URL = "https://beta.convertigo.net/convertigo/projects/ConvertigoAssistant/DisplayObjects/mobile/";

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
		
		browser.addToolItemNavigation(tb);
		new ToolItem(tb, SWT.SEPARATOR);
		browser.addToolItemOpenExternal(tb);
		
		browser.setLayoutData(new GridData(GridData.FILL_BOTH));
		browser.setUseExternalBrowser(false);
		Engine.logStudio.debug("Assistant debug : "+ browser.getDebugUrl());
		
		String url = STARTUP_URL;
		
		var handler = new C8oBrowserPostMessageHelper(browser);
		handler.onMessage(json -> {
			Engine.logStudio.debug("Assistant onMessage: " + json);
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
									json.put("type", "postInstall");
									json.put("installed", project != null);
									if (project != null) {
										json.put("project", project.getName());
										json.put("version", project.getVersion());
									}
									handler.postMessage(msg);
								} catch (Exception e1) {
									e1.printStackTrace();
								}
							} catch (Exception e) {
								Engine.logStudio.debug("Loading from remote URL failed", e);
							}
							mon.done();
						}).schedule();
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

}
