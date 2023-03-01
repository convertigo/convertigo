/*
 * Copyright (c) 2001-2022 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.views.baserow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import com.teamdev.jxbrowser.browser.callback.InjectJsCallback;
import com.teamdev.jxbrowser.browser.callback.InjectJsCallback.Response;
import com.teamdev.jxbrowser.cookie.Cookie;
import com.teamdev.jxbrowser.cookie.CookieStore;
import com.teamdev.jxbrowser.dom.Document;
import com.teamdev.jxbrowser.dom.Element;
import com.teamdev.jxbrowser.frame.Frame;
import com.teamdev.jxbrowser.js.JsAccessible;
import com.teamdev.jxbrowser.js.JsObject;
import com.teamdev.jxbrowser.navigation.event.FrameDocumentLoadFinished;
import com.teamdev.jxbrowser.navigation.event.NavigationStarted;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin.PscException;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UnloadedProjectTreeObject;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.ProjectUrlParser;

public class BaserowView extends ViewPart {
	private Composite main;
	private C8oBrowser browser;
	private String email;
	private String secret;
	
	public class StudioAPI {
		
		@JsAccessible
		public String email() {
			return email;
		}
		
		@JsAccessible
		public String secret() {
			return secret;
		}
		
		@JsAccessible
		public String postMessage(String message) {
			return BaserowView.this.postMessage(message);
		}
	}
	
	public BaserowView() {
		Properties pscProps;
		try {
			pscProps = ConvertigoPlugin.decodePsc();
		} catch (PscException e1) {
			return;
		}
		email = pscProps.getProperty("owner.email");
		secret = pscProps.getProperty("studio.secret");
	}

	@Override
	public void dispose() {
		browser.dispose();
		main.dispose();
		super.dispose();
	}

	@Override
	public void createPartControl(Composite parent) {
		GridLayout gl;
		main = new Composite(parent, SWT.NONE);
		main.setLayout(gl = new GridLayout(1, true));
		gl.marginHeight = gl.marginWidth = 0;
		
		Composite bar = new Composite(main, SWT.NONE);
		bar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
		bar.setLayout(new GridLayout(99, false));
		
		DragSourceAdapter dragListener = new DragSourceAdapter() {
			@Override
			public void dragStart(DragSourceEvent event) {
				try {
					event.doit = true;
				} catch (Exception e) {
					ConvertigoPlugin.logException(e, "Cannot drag");
				}
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				event.data = ((DragSource) event.widget).getControl().getData("dnd");
			}
		};
		
		List<Button> drags = new ArrayList<Button>(2);
		for (Pair<String, String> p: Arrays.asList(
				Pair.of("table_id", "table/(\\w+)"),
				Pair.of("view_id", "table/\\w+/(\\w+)"))) {
			Button drag = new Button(bar, SWT.FLAT);
			drag.setLayoutData(new GridData());
			drag.setToolTipText("Please drag and drop me on a " + p.getLeft() + " variable");
			drag.setData("txt", p.getLeft());
			drag.setData("matcher", Pattern.compile(p.getRight()).matcher(""));
			drag.setVisible(false);
			drags.add(drag);
			
			DragSource source = new DragSource(drag, DND.DROP_COPY | DND.DROP_MOVE);
			source.setTransfer(new Transfer[] { TextTransfer.getInstance() });
			source.addDragListener(dragListener);
		}
		ConvertigoPlugin.asyncExec(() -> {
			browser = new C8oBrowser(main, SWT.NONE);
			browser.setLayoutData(new GridData(GridData.FILL_BOTH));
			
			String url = "https://c8ocloud.convertigo.net/convertigo/projects/C8oCloudBaserow/DisplayObjects/mobile/";
			
			browser.getBrowser().set(InjectJsCallback.class, params -> {
				try {
					if (params.frame().browser().url().contains(url)) {
						JsObject window = params.frame().executeJavaScript("window"); 
						window.putProperty("studio", new StudioAPI());
					}
				} catch (Exception e) {
					Engine.logStudio.info("failure", e);
				}
				return Response.proceed();
			});
			
			browser.getBrowser().navigation().on(FrameDocumentLoadFinished.class, event -> {
				try {
					Frame frame = event.frame();
					Document doc = frame.document().get();
					Element style = doc.createElement("style");
					style.innerText(".dashboard__help, .sidebar__logo { display: none}");
					doc.findElementByTagName("head").get().appendChild(style);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			browser.getBrowser().navigation().on(NavigationStarted.class, event -> {
				try {
					String u = event.url();
					Engine.logStudio.warn("NavigationStarted " + u);
					ConvertigoPlugin.asyncExec(() -> {
						for (Button drag: drags) {
							Matcher m = (Matcher) drag.getData("matcher");
							m.reset(u);
							if (m.find()) {
								drag.setText(drag.getData("txt") + ": " + m.group(1));
								drag.setData("dnd", m.group(1));
								drag.setVisible(true);
							} else {
								drag.setVisible(false);
							}
						}
						drags.get(0).getParent().layout(true);
						main.layout(true);
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			
			browser.setUrl(url);
			Engine.logStudio.debug("Debug the NoCodeDB view: " + browser.getDebugUrl() + "/json");
			main.layout(true);
		});
	}
	
	private String postMessage(String message) {
		try {
			JSONObject obj = new JSONObject(message);
			if (obj.has("key")) {
				String key = obj.getString("key");
				Engine.execute(() -> {
					DatabaseObjectsManager dbom = Engine.theApp.databaseObjectsManager;
					String val = dbom.symbolsGetValue("lib_baserow.apikey.secret");
					if (StringUtils.isBlank(val)) {
						dbom.symbolsAdd("lib_baserow.apikey.secret", key);
//						dbom.symbolsAdd("lib_baserow.port", "443");
//						dbom.symbolsAdd("lib_baserow.server", "baserow-backend.convertigo.net");
//						dbom.symbolsAdd("lib_baserow.https", "true");
					}
					ConvertigoPlugin.asyncExec(() -> {
						try {
							ProjectUrlParser parser = new ProjectUrlParser("lib_BaseRow=https://github.com/convertigo/c8oprj-lib-baserow/archive/master.zip");
							String projectName = parser.getProjectName();
							Engine.logStudio.warn("exist project " + projectName + " ? " + Engine.theApp.databaseObjectsManager.existsProject(projectName));
							ProjectExplorerView pew = ConvertigoPlugin.getDefault().getProjectExplorerView();
							if (Engine.theApp.databaseObjectsManager.existsProject(projectName)) {
								if (!ConvertigoPlugin.getDefault().isProjectOpened(projectName)) {
									TreeObject root = pew.getProjectRootObject(projectName);
									if (root == null) {
										pew.importProjectTreeObject(projectName);
									} else if (root instanceof UnloadedProjectTreeObject) {
										pew.loadProject((UnloadedProjectTreeObject) root);
									}
								}
							} else {
								Project project = Engine.theApp.referencedProjectManager.importProject(parser, true); 
								if (project != null) {
									TreeObject tree = pew.getProjectRootObject(project.getName());
									if (tree != null) {
										pew.reloadProject(tree);
									}
									pew.refreshProjects();
								}
							}
							Engine.logStudio.debug("Debug the NoCodeDB view: " + browser.getDebugUrl() + "/json");
						} catch (Exception e) {
							Engine.logStudio.warn("failure", e);
						}
					});
				});
				
			}
			if (obj.has("token")) {
				String token = obj.getString("token");
				CookieStore cookieStore = browser.getBrowser().engine().cookieStore();
				cookieStore.set(Cookie.newBuilder("baserow.convertigo.net")
					.name("jwt_token").value(token)
					.path("/").secure(true).build());
				cookieStore.persist();
				browser.setUrl("https://baserow.convertigo.net/dashboard");
			} else {
				Engine.logStudio.debug("(NoCode Databases) page response: " + message);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	@Override
	public void setFocus() {
		main.setFocus();
	}
}
