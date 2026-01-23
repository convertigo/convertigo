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

package com.twinsoft.convertigo.eclipse.views.tuto;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.teamdev.jxbrowser.browser.callback.InjectJsCallback;
import com.teamdev.jxbrowser.browser.callback.InjectJsCallback.Response;
import com.teamdev.jxbrowser.frame.Frame;
import com.teamdev.jxbrowser.js.JsAccessible;
import com.teamdev.jxbrowser.js.JsObject;
import com.teamdev.jxbrowser.permission.PermissionType;
import com.teamdev.jxbrowser.permission.callback.RequestPermissionCallback;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.IonBean;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.IonProperty;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.ngx.ApplicationComponentEditor;
import com.twinsoft.convertigo.eclipse.editors.ngx.ApplicationComponentEditorInput;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.events.StudioEvent;
import com.twinsoft.convertigo.engine.events.StudioEventListener;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;

public class TutoView extends ViewPart implements StudioEventListener {
	public class API {

		@JsAccessible
		public void message(JsObject o) {
			String type = o != null ? (String) o.property("type").orElse("") : "";
			if ("imgEnter".equals(type)) {
				onImgEnter((String) o.property("url").get());
			} else if ("control".equals(type)) {
				onControl((String) o.property("json").get());
			}
		}
	};
	
	private API api;
	private Composite main;
	private C8oBrowser browser;
	private Shell dialog;
	private JSONArray controls;
	private String lastDeployment;
	private String lastLink;
	
	public TutoView() {
		api = new API();
		ConvertigoPlugin.runAtStartup(() -> {
			Engine.theApp.eventManager.addListener(this, StudioEventListener.class);
		});
	}

	@Override
	public void dispose() {
		try {
			Engine.theApp.eventManager.removeListener(this, StudioEventListener.class);
		} catch (Exception e) {
		}
		controls = null;
		if (browser != null) {
			browser.dispose();
		}
		main.dispose();
		super.dispose();
	}

	@Override
	public void createPartControl(Composite parent) {
		GridLayout gl;
		main = new Composite(parent, SWT.NONE);
		main.setLayout(gl = new GridLayout(1, true));
		gl.marginHeight = gl.marginWidth = 0;
		Label noEngine = new Label(main, SWT.CENTER);
		noEngine.setLayoutData(new GridData(GridData.FILL_BOTH));
		if (!Engine.isStarted) {
		noEngine.setText("\n"
				+ "Convertigo Studio isn't completely installed,\n"
				+ "you have to complete the registration before\n"
				+ "starting building your projects.");
		}
		ConvertigoPlugin.runAtStartup(() -> ConvertigoPlugin.asyncExec(() -> {
			noEngine.dispose();
			String[] inject = {null};
			try (InputStream is = getClass().getResourceAsStream("inject.js")) {
				inject[0] = IOUtils.toString(is, "UTF-8"); 
			} catch (Exception e2) {
				Engine.logStudio.info("failure", e2);
				inject[0] = "alert('the tutorial is broken, please restart the studio')";
			}
			
			ConvertigoPlugin.asyncExec(() -> {
				browser = new C8oBrowser(main, SWT.NONE);
				browser.setLayoutData(new GridData(GridData.FILL_BOTH));
				browser.getBrowser().profile().permissions().set(RequestPermissionCallback.class, (event, params) -> {
					if (event.permissionType() == PermissionType.CLIPBOARD_READ_WRITE ||
							event.permissionType() == PermissionType.CLIPBOARD_SANITIZED_WRITE) {
						params.grant();
					} else {
						params.deny();
					}
				});
				browser.getBrowser().set(InjectJsCallback.class, params -> {
					try {
						Frame frame = params.frame();
						JsObject window = frame.executeJavaScript("window");
						window.putProperty("IDE", api);
						frame.executeJavaScript(inject[0]);
						Engine.logStudio.debug("(TutoView) inject.js done");
					} catch (Exception e) {
						Engine.logStudio.info("failure", e);
					}
					return Response.proceed();
				});
				browser.setUrl("https://www.convertigo.com/studio-tutorials");
				Engine.logStudio.debug("(TutoView) debug url: " + browser.getDebugUrl() + "/json");
				main.layout(true);
				main.getParent().layout(true);
			});
		}));
	}
	
	private void onImgEnter(String url) {
		ConvertigoPlugin.asyncExec(() -> {
			if (dialog != null && !dialog.isDisposed() && url.equals(dialog.getData("url"))) {
				return;
			}
			
			if (dialog != null && !dialog.isDisposed()) {
				dialog.close();
			}
			dialog = new Shell(main.getShell(), SWT.CLOSE | SWT.TITLE | SWT.RESIZE | SWT.MODELESS | SWT.BORDER);
			dialog.setImages(main.getShell().getImages());
			dialog.setData("url", url);
			
			Rectangle area = null;
			try {
				Matcher m = Pattern.compile(".*?(\\d+).*?(\\d+).*?(\\d+).*?(\\d+).*?").matcher(ConvertigoPlugin.getProperty("tutoview.bound"));
				m.matches();
				area = new Rectangle(
					Integer.parseInt(m.group(1)),
					Integer.parseInt(m.group(2)),
					Integer.parseInt(m.group(3)),
					Integer.parseInt(m.group(4))
				);
			} catch (Exception e) {
				area = main.getShell().getBounds();
				area.x += Math.round(area.width * 0.1f);
				area.y += Math.round(area.height * 0.1f);
				area.width = Math.round(area.width * 0.8f);
				area.height = Math.round(area.height * 0.8f);
			}
			
			dialog.setBounds(area);
			dialog.setLayout(new FillLayout());
			
			C8oBrowser bro = new C8oBrowser(dialog, SWT.NONE);
			bro.setUrl(url);
			
			dialog.addListener(SWT.Close, e -> {
				ConvertigoPlugin.setProperty("tutoview.bound", dialog.getBounds().toString());
				dialog = null;
				bro.dispose();
			});
			
			dialog.open();
		});
	}
	
	private void onControl(String json) {
		try {
			controls = new JSONArray(json);
		} catch (JSONException e) {
			Engine.logStudio.warn("(TutoView) onControl not a JSON: " + json, e);
			return;
		}
		JSONArray currentControls = controls;
		Thread th = new Thread(() -> {
			while (!main.isDisposed() && currentControls == controls) {
				if (checkControls(currentControls)) {
					try {
						Engine.logStudio.debug("(TutoView) controls ok, go next!");
						browser.executeFunctionAndReturnValue("tutoGoNext");
						controls = null;
					} catch (Exception e) {
						
					}
				} else {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}
				}
			}
		});
		th.setName("Tutorial Thread");
		th.setDaemon(true);
		th.start();
	}
	
	private boolean checkControls(JSONArray controls) {
		int len = controls.length();
		for (int i = 0; i < len; i++) {
			try {
				JSONObject control = controls.getJSONObject(i);
				if (!checkControl(control)) {
					return false;
				}
			} catch (JSONException e) {
			}
		}
		return true;
	}
	
	private boolean checkControl(JSONObject control) throws JSONException {
		String type = control.has("type") ? control.getString("type") : "";
		if ("qnameExists".equals(type)) {
			try {
				String qname = control.getString("qname");
				DatabaseObject dbo = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
				Engine.logStudio.trace("(TutoView) checkControl '" + type + "' qname: " + qname + " dbo: " + dbo);
				return dbo != null;
			} catch (Exception e) {
				Engine.logStudio.trace("(TutoView) checkControl '" + type + "' exception: " + e.getMessage());
			}
		} else if ("property".equals(type)) {
			try {
				String qname = control.getString("qname");
				String name = control.getString("name");
				String expression = control.getString("expression");
				DatabaseObject dbo = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
				if (dbo == null) {
					Engine.logStudio.trace("(TutoView) checkControl '" + type + "' qname: " + qname + " name: " + name + " expression: " + expression + " dbo not found");
					return false;
				}
				BeanInfo bi = CachedIntrospector.getBeanInfo(dbo.getClass());

				PropertyDescriptor[] propertyDescriptors = bi.getPropertyDescriptors();
				for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
					if (propertyDescriptor.getName().equals(name)) {
						Object v = propertyDescriptor.getReadMethod().invoke(dbo);
						if (v != null) {
							Engine.logStudio.trace("(TutoView) checkControl '" + type + "' qname: " + qname + " name: " + name + " expression: " + expression + " value: " + v);
							return v.toString().matches(expression);
						}
						Engine.logStudio.trace("(TutoView) checkControl '" + type + "' qname: " + qname + " name: " + name + " expression: " + expression + " no value");
						return false;
					}
				}
				
				try {
					IonBean ionBean = (IonBean) dbo.getClass().getMethod("getIonBean").invoke(dbo);
					IonProperty ionProperty = ionBean.getProperty(name);
					String s = ionProperty.getMode() + ":" + ionProperty.getSmartValue();
					Engine.logStudio.trace("(TutoView) checkControl '" + type + "' qname: " + qname + " name: " + name + " expression: " + expression + " value: " + s);
					return s.matches(expression);
				} catch (Exception e) {
				}
				Engine.logStudio.trace("(TutoView) checkControl '" + type + "' qname: " + qname + " name: " + name + " expression: " + expression + " property not found");
			} catch (Exception e) {
				Engine.logStudio.trace("(TutoView) checkControl '" + type + "' exception: " + e.getMessage());
			}
		} else if ("ngxEditorOpen".equals(type)) {
			String project = control.getString("project");
			String url = control.getString("url");
			boolean[] ok = {false};
			ConvertigoPlugin.syncExec(() -> {
				IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				if (activePage != null) {
					IEditorReference[] editorRefs = activePage.getEditorReferences();
					for (int i = 0; i < editorRefs.length; i++) {
						IEditorReference editorRef = (IEditorReference) editorRefs[i];
						try {
							IEditorInput editorInput = editorRef.getEditorInput();
							if ((editorInput != null) && (editorInput instanceof ApplicationComponentEditorInput)) {
								if (((ApplicationComponentEditorInput) editorInput).getApplication().getProject().getName().equals(project)) {
									ApplicationComponentEditor editorPart = (ApplicationComponentEditor) editorRef.getEditor(false);
									Engine.logStudio.trace("(TutoView) checkControl '" + type + "' project: " + project + " url: " + url + " currentUrl: " + editorPart.getCurrentUrl());
									ok[0] = editorPart.getCurrentUrl().matches(url);
								}
							}
						} catch(Exception e) {
							Engine.logStudio.trace("(TutoView) checkControl '" + type + "' exception: " + e.getMessage());
						}
					}
				}
			});
			Engine.logStudio.trace("(TutoView) checkControl '" + type + "' project: " + project + " url: " + url + " ok ? " + ok[0]);
			return ok[0];
		} else if ("ngxEditorCheck".equals(type)) {
			String project = control.getString("project");
			String check = control.getString("check");
			boolean[] ok = {false};
			ConvertigoPlugin.syncExec(() -> {
				IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				if (activePage != null) {
					IEditorReference[] editorRefs = activePage.getEditorReferences();
					for (int i = 0; i < editorRefs.length; i++) {
						IEditorReference editorRef = (IEditorReference) editorRefs[i];
						try {
							IEditorInput editorInput = editorRef.getEditorInput();
							if ((editorInput != null) && (editorInput instanceof ApplicationComponentEditorInput)) {
								if (((ApplicationComponentEditorInput) editorInput).getApplication().getProject().getName().equals(project)) {
									ApplicationComponentEditor editorPart = (ApplicationComponentEditor) editorRef.getEditor(false);
									ok[0] = editorPart.check(check);
								}
							}
						} catch(Exception e) {
							Engine.logStudio.trace("(TutoView) checkControl '" + type + "' exception: " + e.getMessage());
						}
					}
				}
			});
			Engine.logStudio.trace("(TutoView) checkControl '" + type + "' project: " + project + " check: '" + check + "' ok ? " + ok[0]);
			return ok[0];
		} else if ("fileExists".equals(type)) {
			String project = control.getString("project");
			String subdir = control.getString("subDir");
			String fileExpression = control.getString("fileExpression");
			File dir = new File(Engine.projectDir(project), subdir);
			if (dir.exists() && dir.isDirectory()) {
				Engine.logStudio.trace("(TutoView) checkControl '" + type + "' project: " + project + " subdir: " + subdir + " fileExpression: " + fileExpression + " nb files: " + dir.list().length);
				for (File f: dir.listFiles()) {
					if (f.getName().matches(fileExpression)) {
						return true;
					}
				}
			} else {
				Engine.logStudio.trace("(TutoView) checkControl '" + type + "' project: " + project + " subdir: " + subdir + " fileExpression: " + fileExpression + " no dir!");
			}
		} else if ("deployment".equals(type)) {
			if (lastDeployment != null) {
				String project = control.getString("project");
				Engine.logStudio.trace("(TutoView) checkControl '" + type + "' lastDeployment: " + lastDeployment + " project: " + project);
				return lastDeployment.equals(project);
			}
			Engine.logStudio.trace("(TutoView) checkControl '" + type + "' lastDeployment: " + lastDeployment);
		} else if ("linkOpen".equals(type)) {
			if (lastLink != null) {
				String expression = control.getString("expression");
				Engine.logStudio.trace("(TutoView) checkControl '" + type + "' lastLink: " + lastLink + " expression: " + expression);
				return lastLink.matches(expression);
			}
			Engine.logStudio.trace("(TutoView) checkControl '" + type + "' lastLink: " + lastLink);
		}
		return false;
	}
	
	@Override
	public void setFocus() {
	}

	@Override
	public void onEvent(StudioEvent event) {
		if (StudioEvent.DEPLOYMENT.equals(event.type())) {
			lastDeployment = event.payload();
		} else if (StudioEvent.LINK_OPEN.equals(event.type())) {
			lastLink = event.payload();
		}
	}
}
