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
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.teamdev.jxbrowser.browser.callback.InjectJsCallback;
import com.teamdev.jxbrowser.browser.callback.InjectJsCallback.Response;
import com.teamdev.jxbrowser.cookie.Cookie;
import com.teamdev.jxbrowser.cookie.CookieStore;
import com.teamdev.jxbrowser.dom.Document;
import com.teamdev.jxbrowser.dom.Element;
import com.teamdev.jxbrowser.frame.Frame;
import com.teamdev.jxbrowser.js.JsAccessible;
import com.teamdev.jxbrowser.js.JsFunction;
import com.teamdev.jxbrowser.js.JsObject;
import com.teamdev.jxbrowser.js.JsPromise;
import com.teamdev.jxbrowser.navigation.event.FrameDocumentLoadFinished;
import com.teamdev.jxbrowser.navigation.event.NavigationStarted;
import com.teamdev.jxbrowser.net.HttpHeader;
import com.teamdev.jxbrowser.net.callback.BeforeStartTransactionCallback;
import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.beans.steps.IfStep;
import com.twinsoft.convertigo.beans.steps.JsonFieldStep;
import com.twinsoft.convertigo.beans.steps.JsonObjectStep;
import com.twinsoft.convertigo.beans.steps.JsonToXmlStep;
import com.twinsoft.convertigo.beans.steps.SimpleStep;
import com.twinsoft.convertigo.beans.steps.SmartType;
import com.twinsoft.convertigo.beans.steps.SmartType.Mode;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.beans.steps.XMLCopyStep;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.beans.variables.StepVariable;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin.PscException;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils.SelectionListener;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UnloadedProjectTreeObject;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.Accessibility;
import com.twinsoft.convertigo.engine.enums.JsonFieldType;
import com.twinsoft.convertigo.engine.util.ProjectUrlParser;

public class BaserowView extends ViewPart {
	private Cursor handCursor;
	private Composite main;
	private C8oBrowser browser;
	private String email;
	private String secret;
	private Project project;
	private JsFunction jsCall;
	private String authHeader;
	private String backendApi;

	private String table_id;
	private String table_name;
	private String database_id;
	private String database_name;
	private String view_id;
	private String view_name;

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
		handCursor.dispose();
		browser.dispose();
		main.dispose();
		super.dispose();
	}

	@Override
	public void createPartControl(Composite parent) {
		handCursor = new Cursor(parent.getDisplay(), SWT.CURSOR_HAND);
		GridLayout gl;
		RowLayout rl;
		main = new Composite(parent, SWT.NONE);
		main.setLayout(gl = new GridLayout(1, true));
		gl.marginHeight = gl.marginWidth = 0;

		Composite bar = new Composite(main, SWT.NONE);
		bar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));

		bar.setLayout(rl = new RowLayout());
		rl.center = false;
		rl.justify = false;
		Button currentProject = new Button(bar, SWT.NONE);
		currentProject.setVisible(false);

		currentProject.addSelectionListener((SelectionListener) e -> {
			Dialog dialog = new Dialog(parent.getShell()) {

				@Override
				protected Control createContents(Composite parent) {
					GridData gd;
					Button btn;
					Composite composite = new Composite(parent, SWT.NONE);
					composite.setLayoutData(gd = new GridData(GridData.FILL_BOTH));
					composite.setLayout(new GridLayout(2, true));
					Label tips = new Label(composite, SWT.NONE);
					tips.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
					gd.horizontalSpan = 2;
					tips.setText(currentProject.getText());
					tips = new Label(composite, SWT.NONE);
					tips.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
					gd.horizontalSpan = 2;
					tips.setText("Select sequence to create or update:");

					String prefix = com.twinsoft.convertigo.engine.util.StringUtils.normalize( 
							StringUtils.capitalize(database_name) + 
							StringUtils.capitalize(table_name));
					List<Button> cruds = new ArrayList<>(5);
					for (String type: Arrays.asList("List", "Create", "Read", "Update", "Delete")) {
						cruds.add(btn = new Button(composite, SWT.CHECK));
						btn.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.HORIZONTAL_ALIGN_CENTER));
						gd.horizontalSpan = 2;
						gd.minimumHeight = 50;
						if ("List".equals(type)) {
							btn.setText(prefix + StringUtils.capitalize(view_name) + type);
						} else {
							btn.setText(prefix + type);
						}
						btn.setData("type", type);
					}

					btn = new Button(composite, SWT.FLAT);

					btn.setText("Select All");
					btn.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
					btn.addSelectionListener((SelectionListener) e -> {
						for (Button b: cruds) {
							b.setSelection(true);
						}
					});
					btn = new Button(composite, SWT.FLAT);
					btn.setText("Select None");
					btn.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
					btn.addSelectionListener((SelectionListener) e -> {
						for (Button b: cruds) {
							b.setSelection(false);
						}
					});
					btn = new Button(composite, SWT.FLAT);
					btn.setText("Apply");
					btn.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.HORIZONTAL_ALIGN_CENTER));
					btn.addSelectionListener((SelectionListener) e -> {
						List<Pair<String, String>> stubs = new ArrayList<>(cruds.size());
						for (Button b: cruds) {
							if (b.getSelection()) {
								stubs.add(Pair.of((String) b.getData("type"), b.getText()));
							}
						}
						if (!stubs.isEmpty()) {
							createStub(stubs);
							close();
						}
					});
					btn = new Button(composite, SWT.FLAT);
					btn.setText("Cancel");
					btn.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.HORIZONTAL_ALIGN_CENTER));
					btn.addSelectionListener((SelectionListener) e -> close());

					for (Control c: composite.getChildren()) {
						if (c instanceof Button) {
							((Button) c).setCursor(handCursor);
						}
					}
					return composite;
				}
			};
			dialog.open();
		});

		ConvertigoPlugin.asyncExec(() -> {
			browser = new C8oBrowser(main, SWT.NONE);
			browser.setLayoutData(new GridData(GridData.FILL_BOTH));

			String url = "https://c8ocloud.convertigo.net/convertigo/projects/C8oCloudBaserow/DisplayObjects/mobile/";

			browser.getBrowser().set(InjectJsCallback.class, params -> {
				try {
					if (params.frame().browser().url().contains(url)) {
						JsObject window = params.frame().executeJavaScript("window");
						window.putProperty("studio", new StudioAPI());
					} else {
						jsCall = params.frame().executeJavaScript("async (url, auth) => {"
								+ "let res = await fetch(url, { headers: { Authorization: auth }});"
								+ "return await res.text();}");
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
					Matcher matcher = Pattern.compile("table/(\\w+)(?:/(\\w+))?").matcher(u);
					if (!matcher.find() ) {
						return;
					}
					table_id = matcher.group(1);
					view_id = matcher.group(2);
					callObject(view_id != null ?
							"database/views/" + view_id + "/" :
								"database/tables/" + table_id + "/", res -> {
									JSONObject table = view_id != null ? getObject(res, "table") : res;
									view_name = view_id != null ? get(res, "name") : "";
									table_name = get(table, "name");
									database_id = get(table, "database_id");
									callObject("applications/" + database_id + "/", database -> {
										database_name = get(database, "name");
										ConvertigoPlugin.asyncExec(() -> {
											if (project != null) {
												currentProject.setText("Import CRUD of '" + database_name + "." + table_name + "' sequences in the project '" + project.getName() + "'");
												currentProject.setVisible(true);
												currentProject.getParent().layout();
											}
										});
									});
								});
				} catch (Exception e) {
					e.printStackTrace();
				}
			});

			browser.getBrowser().profile().network().set(BeforeStartTransactionCallback.class, (params) -> {
				int idx;
				if ((idx = params.urlRequest().url().indexOf("/api/")) != -1) {
					for (HttpHeader header: params.httpHeaders()) {
						if (header.name().equals("Authorization")) {
							authHeader = header.value();
							backendApi = params.urlRequest().url().substring(0, idx + 5);
							break;
						}
					}
				}
				return com.teamdev.jxbrowser.net.callback.BeforeStartTransactionCallback.Response.proceed();
			});

			browser.setUrl(url);
			Engine.logStudio.debug("Debug the NoCodeDB view: " + browser.getDebugUrl() + "/json");
			main.layout(true);
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
						return;
					}
					TreeSelection selection = (TreeSelection) e.getSelection();
					if (selection.getFirstElement() instanceof TreeObject) {
						TreeObject to = (TreeObject) selection.getFirstElement();
						ProjectTreeObject prjtree = to.getProjectTreeObject();
						if (prjtree != null) {
							project = prjtree.getObject();
							if (table_id != null) {
								currentProject.setText("Import CRUD of '" + database_name + "." + table_name + "' sequences in the project '" + project.getName() + "'");
								currentProject.setVisible(true);
								bar.layout();
							}
						}
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
					}
					ConvertigoPlugin.asyncExec(() -> {
						try {
							ProjectUrlParser parser = new ProjectUrlParser("lib_BaseRow=https://github.com/convertigo/c8oprj-lib-baserow/archive/master.zip");
							String projectName = parser.getProjectName();
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

	private interface callObjectResult {
		public void result(JSONObject obj);
	}

	private interface callArrayResult {
		public void result(JSONArray arr);
	}

	private void callObject(String api, callObjectResult result) {
		try {
			C8oBrowser.run(() -> {
				JsPromise prom = jsCall.invoke(null, backendApi + api, authHeader);
				prom.then(txt -> {
					try {
						Engine.logStudio.warn(txt[0].toString());
						result.result(new JSONObject(txt[0].toString()));
					} catch (Exception e) {
						Engine.logStudio.warn("callObject failed", e);
					}
					return null;
				});
			});
		} catch (Exception e) {
			Engine.logStudio.warn("callObject failed", e);
		}
	}

	private void callArray(String api, callArrayResult result) {
		try {
			C8oBrowser.run(() -> {
				JsPromise prom = jsCall.invoke(null, backendApi + api, authHeader);
				prom.then(txt -> {
					try {
						result.result(new JSONArray(txt[0].toString()));
					} catch (Exception e) {
						Engine.logStudio.warn("callObject failed", e);
					}
					return null;
				});
			});
		} catch (Exception e) {
			Engine.logStudio.warn("callObject failed", e);
		}
	}

	private String get(JSONObject obj, String key) {
		try {
			return obj.getString(key);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	private JSONObject getObject(JSONObject obj, String key) {
		try {
			return obj.getJSONObject(key);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	private void addSample(JSONObject sample, String varName, String varType) throws JSONException {
		if (varType.equals("link_row")) {
			JSONObject obj = new JSONObject();
			obj.put("id", 0);
			obj.put("value", "");
			JSONArray ar = new JSONArray();
			ar.put(obj);
			sample.put(varName, ar);
		} else  if (varType.equals("boolean")) {
			sample.put(varName, false);
		} else if (varType.equals("file")) {
			JSONObject obj = new JSONObject();
			obj.put("url", "");
			JSONObject thumbnails = new JSONObject();
			JSONObject info = new JSONObject();
			info.put("url", "");
			info.put("width", 0);
			info.put("height", 0);
			thumbnails.put("tiny", info);
			thumbnails.put("small", info);
			thumbnails.put("card_cover", info);
			obj.put("thumbnails", thumbnails);
			obj.put("visible_name", "");
			obj.put("name", "");
			obj.put("size", 0);
			obj.put("mime_type", "");
			obj.put("is_image", true);
			obj.put("image_width", 0);
			obj.put("image_height", 0);
			obj.put("uploaded_at", "");
			JSONArray ar = new JSONArray();
			ar.put(obj);
			sample.put(varName, ar);
		} else if (varType.equals("single_select")) {
			JSONObject obj = new JSONObject();
			obj.put("id", 0);
			obj.put("value", "");
			obj.put("color", "");
			sample.put(varName, obj);
		} else {
			sample.put(varName, "");
		}
	}

	private void createStub(List<Pair<String, String>> stubs) {
		if (project == null) {
			return;
		}

		ProjectExplorerView pev = ConvertigoPlugin.getDefault().getProjectExplorerView();
		if (pev == null) {
			return;
		}

		callArray("database/fields/table/" + table_id + "/", arr -> {
			int len = arr.length();
			for (Pair<String, String> stub: stubs) {
				String type = stub.getKey();
				String sequenceName = stub.getValue();
				boolean isList = "List".equals(type);
				boolean isCreate = "Create".equals(type);
				boolean isRead = "Read".equals(type);
				boolean isUpdate = "Update".equals(type);
				boolean isDelete = "Delete".equals(type);

				try {
					Sequence s = project.getSequenceByName(sequenceName);
					project.removeSequence(s);
				} catch (EngineException e) {
				}

				try {
					GenericSequence sequence = new GenericSequence();
					sequence.setName(sequenceName);
					sequence.setComment(type + " row" + (isList ? "s" : "") + " of " + table_name + " from " + database_name);
					sequence.setAccessibility(Accessibility.Hidden);
					sequence.setAuthenticatedContextRequired(true);
					project.add(sequence);

					SimpleStep simpleStep = new SimpleStep();
					simpleStep.setName("apiKey");
					simpleStep.setCompilablePropertySourceValue("expression", "(__header_Authorization = 'Token ${lib_baserow.apikey.secret=}') == 'Token '");
					simpleStep.updateSymbols();
					sequence.add(simpleStep);

					if (isCreate || isUpdate || isRead) {
						JSONObject sample = new JSONObject();
						sample.put("id", 0);
						sample.put("order", "0");

						JsonObjectStep jsonObjectStep = null;
						if (isCreate || isUpdate) {
							jsonObjectStep = new JsonObjectStep();
							jsonObjectStep.setName("body");
							jsonObjectStep.setOutput(false);
							sequence.add(jsonObjectStep);
						}

						for (int i = 0; i < len; i++) {
							String varName = get(arr.getJSONObject(i), "name");
							String varType = get(arr.getJSONObject(i), "type");

							RequestableVariable var = null;
							if ((isCreate || isUpdate) && !"formula".equals(type)) {
								String comment = varName + " [" + varType + "]";
								var = new RequestableVariable();
								var.setName("field_" + com.twinsoft.convertigo.engine.util.StringUtils.normalize(varName));
								var.setComment(comment);
								sequence.add(var);
							}
							addSample(sample, varName, varType);
							if (varType.equals("link_row")) {
								if (var != null) {
									var.setValueOrNull("[]");
								}
							} else if (varType.equals("boolean")) {
								if (var != null) {
									var.setValueOrNull("false");
								}
							}

							if (jsonObjectStep != null && !"formula".equals(type)) {
								if (varType.equals("link_row")) {
									JsonToXmlStep jsonToXmlStep = new JsonToXmlStep();
									jsonToXmlStep.setName(varName);
									SmartType st = new SmartType();
									st.setMode(Mode.JS);
									st.setExpression("JSON.parse(" + var.getName() + ")");
									jsonToXmlStep.setJsonObject(st);
									jsonToXmlStep.setOutput(false);
									jsonObjectStep.add(jsonToXmlStep);
								} else {
									JsonFieldStep jsonFieldStep = new JsonFieldStep();
									jsonFieldStep.setName(varName);
									SmartType st = new SmartType();
									st.setMode(Mode.JS);
									st.setExpression(var.getName());
									jsonFieldStep.setValue(st);
									jsonFieldStep.setOutput(false);
									jsonObjectStep.add(jsonFieldStep);
								}
							}
						}

						TransactionStep transactionStep = new TransactionStep();
						if (isCreate) {
							transactionStep.setSourceTransaction("lib_BaseRow.Baserow_API_spec._api_database_rows_table__table_id___POST");
						}
						if (isUpdate) {
							transactionStep.setSourceTransaction("lib_BaseRow.Baserow_API_spec._api_database_rows_table__table_id___row_id___PATCH");
						}
						if (isRead) {
							transactionStep.setSourceTransaction("lib_BaseRow.Baserow_API_spec._api_database_rows_table__table_id___row_id___GET");
						}
						sequence.add(transactionStep);

						StepVariable stepVariable = new StepVariable();
						stepVariable.setName("__header_Authorization");
						transactionStep.add(stepVariable);

						stepVariable = new StepVariable();
						stepVariable.setName("table_id");
						stepVariable.setValueOrNull(table_id);
						transactionStep.add(stepVariable);

						stepVariable = new StepVariable();
						stepVariable.setName("user_field_names");
						stepVariable.setValueOrNull("true");
						transactionStep.add(stepVariable);

						if (isCreate || isUpdate) {
							stepVariable = new StepVariable();
							stepVariable.setName("__body");
							XMLVector<String> source = new XMLVector<String>();
							source.add(Long.toString(jsonObjectStep.priority));
							source.add("*");
							stepVariable.setSourceDefinition(source);
							transactionStep.add(stepVariable);
						}

						if (isCreate) {
							stepVariable = new StepVariable();
							stepVariable.setName("before");
							transactionStep.add(stepVariable);

							RequestableVariable var = new RequestableVariable();
							var.setName("before");
							var.setComment("If provided then the newly created row will be positioned before the row with the provided id.");
							sequence.add(var);
						}

						if (isUpdate || isRead) {
							stepVariable = new StepVariable();
							stepVariable.setName("row_id");
							transactionStep.add(stepVariable);

							RequestableVariable var = new RequestableVariable();
							var.setName("row_id");
							if (isUpdate) {
								var.setComment("Updates the row related to the value.");
							}
							if (isRead) {
								var.setComment("Returns the row related the provided value.");
							}
							var.setRequired(true);
							sequence.add(var);
						}

						XMLCopyStep xmlCopyStep = new XMLCopyStep();
						XMLVector<String> source = new XMLVector<String>();
						source.add(Long.toString(transactionStep.priority));
						source.add("document/object");
						xmlCopyStep.setSourceDefinition(source);
						sequence.add(xmlCopyStep);

						IfStep ifStep = new IfStep();
						ifStep.setCondition("false");
						sequence.add(ifStep);

						JsonToXmlStep jsonToXmlStep = new JsonToXmlStep();
						jsonToXmlStep.setName("object");
						jsonToXmlStep.setJsonSample(sample.toString(2));
						ifStep.add(jsonToXmlStep);
					}

					if (isDelete) {
						TransactionStep transactionStep = new TransactionStep();
						transactionStep.setSourceTransaction("lib_BaseRow.Baserow_API_spec._api_database_rows_table__table_id___DELETE");
						sequence.add(transactionStep);

						StepVariable stepVariable = new StepVariable();
						stepVariable.setName("__header_Authorization");
						transactionStep.add(stepVariable);

						stepVariable = new StepVariable();
						stepVariable.setName("table_id");
						stepVariable.setValueOrNull(table_id);
						transactionStep.add(stepVariable);

						stepVariable = new StepVariable();
						stepVariable.setName("row_id");
						transactionStep.add(stepVariable);

						RequestableVariable var = new RequestableVariable();
						var.setName("row_id");
						var.setComment("Deletes the row related to the value.");
						var.setRequired(true);
						sequence.add(var);

						JsonFieldStep jsonFieldStep = new JsonFieldStep();
						jsonFieldStep.setName("success");
						jsonFieldStep.setType(JsonFieldType.bool);
						SmartType st = new SmartType();
						st.setMode(Mode.SOURCE);
						XMLVector<String> source = new XMLVector<String>();
						source.add(Long.toString(transactionStep.priority));
						source.add("document/HttpInfo/status[@code=200]");
						st.setSourceDefinition(source);
						jsonFieldStep.setValue(st);
						sequence.add(jsonFieldStep);
					}

					if (isList) {
						JSONObject sample = new JSONObject();
						sample.put("count", 0);
						sample.put("next", "");
						sample.put("previous", "");
						JSONArray array = new JSONArray();
						sample.put("results", array);
						JSONObject object = new JSONObject();
						array.put(object);
						object.put("id", 0);
						object.put("order", "0");

						TransactionStep transactionStep = new TransactionStep();
						transactionStep.setSourceTransaction("lib_BaseRow.Baserow_API_spec._api_database_rows_table__table_id___GET");
						sequence.add(transactionStep);

						StepVariable stepVariable = new StepVariable();
						stepVariable.setName("__header_Authorization");
						transactionStep.add(stepVariable);

						stepVariable = new StepVariable();
						stepVariable.setName("table_id");
						stepVariable.setValueOrNull(table_id);
						transactionStep.add(stepVariable);

						stepVariable = new StepVariable();
						stepVariable.setName("user_field_names");
						stepVariable.setValueOrNull("true");
						transactionStep.add(stepVariable);

						if (view_id != null) {
							stepVariable = new StepVariable();
							stepVariable.setName("view_id");
							stepVariable.setValueOrNull(view_id);
							transactionStep.add(stepVariable);
						}

						String[] names = new String[len];
						for (int i = 0; i < len; i++) {
							String varName = get(arr.getJSONObject(i), "name");
							String varType = get(arr.getJSONObject(i), "type");
							addSample(object, varName, varType);
							names[i] = varName;
						}

						stepVariable = new StepVariable();
						stepVariable.setName("include_fields");
						transactionStep.add(stepVariable);

						RequestableVariable var = new RequestableVariable();
						var.setName("include_fields");
						var.setComment("All the fields are included in the response by default. You can select a subset of fields by providing the include query parameter. If you for example provide the following GET parameter `include=field_1,field_2` then only the fields withid `1` and id `2` are going to be selected and included in the response. If the `user_field_names` parameter is provided then instead include should be a comma separated list of the actual field names. For field names with commas you should surround the name with quotes like so: `include=My Field,\"Field With , \"`. A backslash can be used to escape field names which contain double quotes like so: `include=My Field,Field with \\\"`.");
						var.setValueOrNull(String.join(",", names));
						sequence.add(var);

						for (Pair<String, String> p: Arrays.asList(
								Pair.of("filter_type", "`AND`: Indicates that the rows must match all the provided filters.\r\n"
										+ "`OR`: Indicates that the rows only have to match one of the filters.\r\n"
										+ "\r\n"
										+ "This works only if two or more filters are provided."),
								Pair.of("order_by", "Optionally the rows can be ordered by provided field ids separated by comma. By default a field is ordered in ascending (A-Z) order, but by prepending the field with a '-' it can be ordered descending (Z-A). If the `user_field_names` parameter is provided then instead order_by should be a comma separated list of the actual field names. For field names with commas you should surround the name with quotes like so: `order_by=My Field,\"Field With , \"`. A backslash can be used to escape field names which contain double quotes like so: `order_by=My Field,Field with \\\"`."),
								Pair.of("page", "Defines which page of rows should be returned."),
								Pair.of("search", "If provided only rows with data that matches the search query are going to be returned."),
								Pair.of("size", "Defines how many rows should be returned per page."),
								Pair.of("filterExpression", "")
								)) {
							var = new RequestableVariable();
							var.setName(p.getLeft());
							var.setComment(p.getRight());
							sequence.add(var);

							stepVariable = new StepVariable();
							stepVariable.setName(p.getLeft());
							transactionStep.add(stepVariable);
						}

						XMLCopyStep xmlCopyStep = new XMLCopyStep();
						XMLVector<String> source = new XMLVector<String>();
						source.add(Long.toString(transactionStep.priority));
						source.add("document/object");
						xmlCopyStep.setSourceDefinition(source);
						sequence.add(xmlCopyStep);

						IfStep ifStep = new IfStep();
						ifStep.setCondition("false");
						sequence.add(ifStep);

						JsonToXmlStep jsonToXmlStep = new JsonToXmlStep();
						jsonToXmlStep.setName("object");
						jsonToXmlStep.setJsonSample(sample.toString(2));
						ifStep.add(jsonToXmlStep);
					}

				} catch (Exception e) {
					e.printStackTrace();
				}

			}

			ConvertigoPlugin.asyncExec(() -> {
				try {
					pev.reloadDatabaseObject(project);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		});
	}
}
