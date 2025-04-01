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

package com.twinsoft.convertigo.eclipse.views.mobile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.twinsoft.convertigo.beans.connectors.FullSyncConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Document;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.couchdb.DesignDocument;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.IAction;
import com.twinsoft.convertigo.beans.ngx.components.MobileComponent;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSource.Filter;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSource.SourceData;
import com.twinsoft.convertigo.beans.ngx.components.PageComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIActionEvent;
import com.twinsoft.convertigo.beans.ngx.components.UIActionStack;
import com.twinsoft.convertigo.beans.ngx.components.UIAppEvent;
import com.twinsoft.convertigo.beans.ngx.components.UIComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIControlDirective;
import com.twinsoft.convertigo.beans.ngx.components.UIControlDirective.AttrDirective;
import com.twinsoft.convertigo.beans.ngx.components.UIControlEvent;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicAction;
import com.twinsoft.convertigo.beans.ngx.components.UIEventSubscriber;
import com.twinsoft.convertigo.beans.ngx.components.UIForm;
import com.twinsoft.convertigo.beans.ngx.components.UIPageEvent;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponent;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponentEvent;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.CouchKey;
import com.twinsoft.convertigo.engine.util.GenericUtils;

class NgxPickerContentProvider implements ITreeContentProvider {
	
	private static Pattern INVALID_CHARACTERS = Pattern.compile("[~:\\-\\s]+");
	
	class TVObject {
		private String name;
		private Object object;
		private TVObject parent;
		private SourceData data;
		private JSONObject infos;
		private List<TVObject> children = new ArrayList<TVObject>();
		
		private TVObject(String name) {
			this(name, null, null);
		}
		
		private TVObject (String name, Object object, SourceData sd) {
			this(name, object, sd, null);
		}
		
		private TVObject (String name, Object object, SourceData data, JSONObject infos) {
			this.name = name;
			this.object = object;
			this.data = data;
			this.infos = infos == null ? new JSONObject(): infos;
		}

		public String toString() {
			return name;
		}
		
		public String getPath() {
			String path = INVALID_CHARACTERS.matcher(name).find() ?  "['"+name+"']":name;
			if (parent != null) {
				path = parent.getPath() + "?." + path;
			}
			return path;
		}
		
		public SourceData getSourceData() {
			return data;
		}
		
		public String getSource() {
			String param = "";
			if (object != null) {
				// New code
				if (data != null) {
					String source = data.getSource();
					if (source != null) {
						return source;
					}
				}
				
				// Old code
				if (object instanceof Sequence) {
					String marker = "";
					try {
						marker = infos.has("marker") ? infos.getString("marker"):"";
					} catch (JSONException e) {}
					Sequence sequence = (Sequence)object;
					param = "'"+ sequence.getQName() + (!marker.isEmpty() ? "#":"") + marker + "'";
				} else if (object instanceof DesignDocument) {
					DesignDocument dd = (DesignDocument)object;
					String db = dd.getParent().getQName();//parent.parent.parent.getName();
					String ddoc = dd.getName();
					String dview = parent.getName();
					String vm = name;
					String include_docs = "false";
					try {
						include_docs = infos.has("include_docs") ? infos.getString("include_docs"):"false";
					} catch (JSONException e) {}
					param = "'fs://"+ db +"."+ vm +", {ddoc='"+ddoc+"', view='"+dview+"', include_docs='"+include_docs+"'}'";
				} else if (object instanceof UIControlDirective) {
					UIControlDirective directive = (UIControlDirective)object;
					param = "item"+ directive.priority;
				} else if (object instanceof UIForm) {
					UIForm form = (UIForm)object;
					param = "form"+ form.priority;
				} else if (object instanceof ApplicationComponent) {
					param = "router.sharedObject";
				}
			} else {
				if (infos != null) {
					
				}
			}
			return param;
		}
		
		public TVObject getParent() {
			return parent;
		}
		
		public Object getObject() {
			return object;
		}
		
		public JSONObject getInfos() {
			return infos;
		}
		
		public String getName() {
			return name;
		}
		
		private TVObject add(TVObject child) {
			if (child != null) {
				child.parent = this;
				if (!children.contains(child)) {
					children.add(child);
				}
			}
			return child;
		}
		
		private boolean remove(TVObject child) {
			if (child != null) {
				if (children.contains(child)) {
					boolean removed = children.remove(child);
					if (removed) {
						child.parent = null;
						return true;
					}
				}
			}
			return false;
		}
		
		public boolean isEmpty() {
			return children.isEmpty();
		}
	}
	
	private Filter filter = Filter.Sequence;
	private Object selected = null;
	
	public NgxPickerContentProvider() {
		
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		ITreeContentProvider.super.inputChanged(viewer, oldInput, newInput);
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	public void setFilterBy(Filter filter) {
		this.filter = filter;
	}
	
	public void setSelectedDbo(Object object) {
		this.selected = object;
	}
	
	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof TVObject) {
			return ((TVObject) parentElement).children.toArray();
		} else if (parentElement instanceof MobileComponent) {
			MobileComponent mobileComponent = (MobileComponent)parentElement;
			Project project = mobileComponent.getProject();
			
			ProjectExplorerView projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
			List<String> projectNames = Engine.theApp.databaseObjectsManager.getAllProjectNamesList();
			projectNames.remove(project.getName());
			projectNames.add(0, project.getName());

			Map<String, Set<String>> map = mobileComponent.getApplication().getInfoMap();
			
			TVObject root = new TVObject("root", mobileComponent, null);
			if (filter.equals(Filter.Action)) {
				TVObject tvi = root.add(new TVObject("actions"));
				
				if (mobileComponent instanceof PageComponent) {
					tvi.add(new TVObject("locals"));
				}
				TVObject tvEvents = tvi.add(new TVObject("events"));
				TVObject tvControls = tvi.add(new TVObject("controls"));
				
				addActions(tvi, mobileComponent);
				
				if (tvEvents.isEmpty()) {
					tvi.remove(tvEvents);
				}
				if (tvControls.isEmpty()) {
					tvi.remove(tvControls);
				}
				
			}
			if (filter.equals(Filter.Shared)) {
				TVObject tvi = root.add(new TVObject("shared"));
				addSharedComponents(tvi, mobileComponent);
			}
			if (filter.equals(Filter.Sequence)) {
				TVObject tvs = root.add(new TVObject("sequences"));
				for (String projectName : projectNames) {
					try {
						Project p = projectExplorerView.getProject(projectName);
						if (!p.getSequencesList().isEmpty()) {
							var tvp = tvs.add(new TVObject(projectName));
							boolean isReferenced = !p.getName().equals(project.getName());
							addSequences(map, tvp, isReferenced ? p:project, isReferenced);
						}
					} catch (Exception e) {
					}
				}
			}
			if (filter.equals(Filter.Database)) {
				TVObject tvd = root.add(new TVObject("databases"));
				for (String projectName : projectNames) {
					try {
						Project p = projectExplorerView.getProject(projectName);
						boolean isReferenced = !p.getName().equals(project.getName());
						addFsObjects(map, tvd, isReferenced ? p:project, isReferenced);
					} catch (Exception e) {
					}
				}
			}
			if (filter.equals(Filter.Iteration)) {
				TVObject tvi = root.add(new TVObject("iterations"));
				addIterations(tvi, mobileComponent);
			}
			if (filter.equals(Filter.Form)) {
				TVObject tvi = root.add(new TVObject("forms"));
				addForms(tvi, mobileComponent);
			}
			if (filter.equals(Filter.Global)) {
				TVObject tvi = root.add(new TVObject("globals"));
				addGlobals(tvi, mobileComponent.getApplication());
			}
			if (filter.equals(Filter.Local)) {
				TVObject tvi = root.add(new TVObject("locals"));
				addLocals(tvi, mobileComponent.getApplication());
			}
			return root.children.toArray();
		} else if (parentElement instanceof JSONObject) {
			JSONObject jsonObject = (JSONObject)parentElement;
			
			TVObject root = new TVObject("root", jsonObject, null);
			addJsonObjects(root);
			
			return root.children.toArray();
		}
		return new Object[0];
	}
	
	@Override
	public Object getParent(Object element) {
		if (element instanceof TVObject) {
			return ((TVObject)element).parent;
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}

	private void addSequences(Map<String, Set<String>> map, TVObject tvs, Object object, boolean isReferenced) {
		if (object != null) {
			if (object instanceof Project) {
				Project project = (Project)object;
				for (Sequence s : project.getSequencesList()) {
					String label = isReferenced ? s.getQName():s.getName();
					
					SourceData sd = null;
					try {
						sd = Filter.Sequence.toSourceData(new JSONObject()
								.put("sequence", s.getQName()));
					} catch (JSONException e) {
						e.printStackTrace();
					}
					tvs.add(new TVObject(s.getName(), s, sd));
					
					Set<String> infos = map.get(s.getQName());
					if (infos != null) {
						for (String info: infos) {
							try {
								JSONObject jsonInfo = new JSONObject(info);
								if (jsonInfo.has("marker")) {
									String marker = jsonInfo.getString("marker");
									if (!marker.isEmpty()) {
										sd = Filter.Sequence.toSourceData(new JSONObject()
												.put("sequence", s.getQName())
												.put("marker", marker));
										tvs.add(new TVObject(label + "#" + marker, s, sd, jsonInfo));
									}
								}
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
	}
	
	private void addFsObjects(Map<String, Set<String>> map, TVObject tvd, Object object, boolean isReferenced) {
		if (object != null) {
			if (object instanceof Project) {
				Project project = (Project)object;
				
				for (Connector c : project.getConnectorsList()) {
					if (c instanceof FullSyncConnector) {
						String label = isReferenced ? c.getQName():c.getName();
						
						TVObject tvc = tvd.add(new TVObject(label));
						
						for (Document d : c.getDocumentsList()) {
							if (d instanceof DesignDocument) {
								
								TVObject tdd = tvc.add(new TVObject(d.getName()));
								JSONObject views = CouchKey.views.JSONObject(((DesignDocument)d).getJSONObject());
								if (views != null) {
									for (Iterator<String> it = GenericUtils.cast(views.keys()); it.hasNext(); ) {
										try {
											Set<String> infos = null;
											String view = it.next();
											
											String key = c.getQName() + "." + d.getName() + "." + view;
											
											TVObject tvv = tdd.add(new TVObject(view));
											
											SourceData sd = null;
											try {
												sd = Filter.Database.toSourceData(new JSONObject()
														.put("connector", c.getQName())
														.put("document", d.getQName())
														.put("queryview", view)
														.put("verb", "get"));
											} catch (JSONException e) {
												e.printStackTrace();
											}
											
											tvv.add(new TVObject("get", d, sd));
											infos = map.get(key+ ".get");
											if (infos == null) {
												infos = map.get(c.getQName() + ".get");
											}
											if (infos != null) {
												for (String info: infos) {
													try {
														JSONObject jsonInfo = new JSONObject(info);
														boolean includeDocs = false;
														if (jsonInfo.has("include_docs")) {
															includeDocs = Boolean.valueOf(jsonInfo
																	.getString("include_docs")).booleanValue();
														}
														if (jsonInfo.has("marker")) {
															String marker = jsonInfo.getString("marker");
															if (!marker.isEmpty()) {
																String name = "get" + "#" + marker;
																
																sd = Filter.Database.toSourceData(new JSONObject()
																		.put("connector", c.getQName())
																		.put("document", d.getQName())
																		.put("queryview", view)
																		.put("verb", "get")
																		.put("marker", marker)
																		.put("includeDocs", includeDocs));
																tvv.add(new TVObject(name, d, sd, jsonInfo));
															}
														}
													} catch (JSONException e) {
														e.printStackTrace();
													}
												}
											}
											
											try {
												sd = Filter.Database.toSourceData(new JSONObject()
														.put("connector", c.getQName())
														.put("document", d.getQName())
														.put("queryview", view)
														.put("verb", "view"));
											} catch (JSONException e) {
												e.printStackTrace();
											}
											tvv.add(new TVObject("view", d, sd));
											
											infos = map.get(key+ ".view");
											if (infos != null) {
												for (String info: infos) {
													try {
														JSONObject jsonInfo = new JSONObject(info);
														boolean includeDocs = false;
														if (jsonInfo.has("include_docs")) {
															includeDocs = Boolean.valueOf(jsonInfo
																	.getString("include_docs")).booleanValue();
														}
														if (jsonInfo.has("marker")) {
															String marker = jsonInfo.getString("marker");
															if (!marker.isEmpty()) {
																String name = "view" + "#" + marker;
																
																sd = Filter.Database.toSourceData(new JSONObject()
																		.put("connector", c.getQName())
																		.put("document", d.getQName())
																		.put("queryview", view)
																		.put("verb", "view")
																		.put("marker", marker)
																		.put("includeDocs", includeDocs));
																tvv.add(new TVObject(name, d, sd, jsonInfo));
															}
														}
													} catch (JSONException e) {
														e.printStackTrace();
													}
												}
											}
											
										} catch (Exception e) {
											e.printStackTrace();
										}
									}
								}
							}
						}
						
					}
				}
			}
		}
	}
	
	private void addIterations(TVObject tvi, Object object) {
		if (object != null) {
			List<UIComponent> list = null;
			if (object instanceof PageComponent) {
				list = ((PageComponent)object).getUIComponentList();
			} else if (object instanceof UIComponent) {
				list = ((UIComponent)object).getUIComponentList();
			}
			
			if (list != null) {
				for (UIComponent uic : list) {
					if (uic instanceof UIControlDirective) {
						// do not add to prevent selection on itself or children
						if (uic.equals(selected)) {
							return;
						}
						
						// do not add if not parent of selected (popped picker only)
						boolean showInPicker = true;
						if (selected != null && selected instanceof UIComponent) {
							String selectedQName = ((UIComponent)selected).getQName();
							String uicQName = uic.getQName() + ".";
							if (!selectedQName.startsWith(uicQName)) {
								showInPicker = false;
							}
						}
						
						UIControlDirective uicd = (UIControlDirective)uic;
						if (showInPicker && AttrDirective.isForDirective(uicd.getDirectiveName())) {
							SourceData sd = null;
							try {
								sd = Filter.Iteration.toSourceData(new JSONObject()
										.put("priority", uic.priority));
							} catch (JSONException e) {
								e.printStackTrace();
							}
							TVObject tuic = tvi.add(new TVObject(uic.toString(), uic, sd));
							addIterations(tuic, uic);
						} else {
							addIterations(tvi, uic);
						}
					} else {
						addIterations(tvi, uic);
					}
				}
			}
		}
	}
	
	private void addActions(TVObject tvi, Object object) {
		if (object != null) {
			List<? extends UIComponent> list = null;
			if (object instanceof ApplicationComponent) {
				ApplicationComponent app = (ApplicationComponent)object;
				list = app.getUIAppEventList();
				list.addAll(GenericUtils.cast(app.getUIEventSubscriberList()));
				list.addAll(GenericUtils.cast(app.getSharedActionList()));
			} else if (object instanceof UIAppEvent) {
				if (tvi != null && "actions".equals(tvi.getName())) {
					list = new ArrayList<>(Arrays.asList((UIAppEvent)object));
				} else {
					list = ((UIAppEvent)object).getUIComponentList();
				}
			} else if (object instanceof UIActionStack) {
				if (tvi != null && "actions".equals(tvi.getName())) {
					list = new ArrayList<>(Arrays.asList((UIActionStack)object));
				} else {
					list = ((UIActionStack)object).getUIComponentList();
				}
			} else if (object instanceof UISharedComponent) {
				list = ((UISharedComponent)object).getUIComponentList();
			} else if (object instanceof PageComponent) {
				list = ((PageComponent)object).getUIComponentList();
			} else if (object instanceof UIComponent) {
				list = ((UIComponent)object).getUIComponentList();
			}
			
			if (list != null) {
				TVObject tvLocals = null, tvEvents = null, tvControls = null;
				if (tvi != null && "actions".equals(tvi.getName())) {
					for (TVObject tvo: tvi.children) {
						if (tvo.getName().equals("locals")) {
							tvLocals = tvo;
						} else if (tvo.getName().equals("events")) {
							tvEvents = tvo;							
						} else if (tvo.getName().equals("controls")) {
							tvControls = tvo;							
						}
					}
				}
				for (UIComponent uic : list) {
					// do not add to prevent selection on itself or children
					if (uic.equals(selected)) {
						return;
					}
					
					// do not add if not parent of selected (popped picker only)
					boolean showInPicker = true;
					if (selected != null && selected instanceof UIComponent) {
						String selectedQName = ((UIComponent)selected).getQName();
						String uicQName = uic.getQName() + ".";
						if (!selectedQName.startsWith(uicQName)) {
							showInPicker = false;
						}
					}
					
					if (showInPicker) {
						PageComponent page = uic.getPage();
						if (page != null && tvLocals != null && tvLocals.children.isEmpty()) {
							try {
								JSONObject jsonInfos = new JSONObject();
								jsonInfos.put("navParams", new JSONObject().put("data", ""));

								SourceData sd = null;
								try {
									sd = Filter.Action.toSourceData(new JSONObject().put("pageLocals", "true"));
								} catch (Exception e) {
									e.printStackTrace();
								}
								
								tvLocals.add(new TVObject(page.getName(), page, sd, jsonInfos));
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
						
						if (uic instanceof UIAppEvent || uic instanceof UIPageEvent || uic instanceof UISharedComponentEvent || uic instanceof UIEventSubscriber) {
							SourceData sd = null;
							try {
								sd = Filter.Action.toSourceData(new JSONObject()
										.put("priority", uic.priority).put("rootEvent", true));
							} catch (JSONException e) {
								e.printStackTrace();
							}
							
							TVObject tve = tvEvents == null ?
									tvi.add(new TVObject(uic.toString(), uic, sd)) :
										tvEvents.add(new TVObject(uic.toString(), uic, sd));
							addActions(tve, uic);
						} else if (uic instanceof UIActionEvent || uic instanceof UIControlEvent) {
							SourceData sd = null;
							if (uic instanceof UIControlEvent) {
								try {
									sd = Filter.Action.toSourceData(new JSONObject()
											.put("priority", uic.priority).put("rootEvent", true));
								} catch (JSONException e) {
									e.printStackTrace();
								}
							}
							
							TVObject tve = tvControls == null ?
									tvi.add(new TVObject(uic.toString(), uic, sd)) :
										tvControls.add(new TVObject(uic.toString(), uic, sd));
							addActions(tve, uic);
						} else if (uic instanceof IAction || uic instanceof UIActionStack) {
							SourceData sd = null;
							try {
								sd = Filter.Action.toSourceData(new JSONObject()
										.put("priority", uic.priority));
							} catch (JSONException e) {
								e.printStackTrace();
							}
							
							TVObject tuic = tvi.add(new TVObject(uic.toString(), uic, sd));
							addActions(tuic, uic);
						} else {
							addActions(tvi, uic);
						}
					//} else {
					//	addActions(tvi, uic);
					}
				}
				
			}
		}
	}
	
	private void addSharedComponents(TVObject tvi, Object object) {
		if (object != null) {
			List<? extends UIComponent> list = null;
			if (object instanceof ApplicationComponent) {
				list = ((ApplicationComponent)object).getSharedComponentList();
			} else if (object instanceof UISharedComponent) {
				list = new ArrayList<>(Arrays.asList((UISharedComponent)object));
			}
			
			if (list != null) {
				for (UIComponent uic : list) {
					if (uic instanceof UISharedComponent) {
						// do not add to prevent selection on itself or children
						if (uic.equals(selected)) {
							return;
						}
						
						// do not add if not parent of selected (popped picker only)
						boolean showInPicker = true;
						if (selected != null && selected instanceof UIComponent) {
							String selectedQName = ((UIComponent)selected).getQName();
							String uicQName = uic.getQName() + ".";
							if (!selectedQName.startsWith(uicQName)) {
								showInPicker = false;
							}
						}
						
						if (showInPicker) {
							SourceData sd = null;
							try {
								sd = Filter.Shared.toSourceData(new JSONObject()
										.put("priority", uic.priority)
										.put("regular", ((UISharedComponent) uic).isRegular()));
							} catch (JSONException e) {
								e.printStackTrace();
							}
							
							tvi.add(new TVObject(uic.toString(), uic, sd));
						}
					}
				}
			}
		}
	}
	
	private void addForms(TVObject tvi, Object object) {
		if (object != null) {
			List<UIComponent> list = null;
			if (object instanceof PageComponent) {
				list = ((PageComponent)object).getUIComponentList();
			} else if (object instanceof UIComponent) {
				list = ((UIComponent)object).getUIComponentList();
			}
			
			if (list != null) {
				for (UIComponent uic : list) {
					if (uic instanceof UIForm) {
						// do not add to prevent selection on itself or children
						if (uic.equals(selected)) {
							return;
						}
						SourceData sd = null;
						try {
							sd = Filter.Form.toSourceData(new JSONObject()
									.put("priority", uic.priority)
									.put("identifier", ((UIForm) uic).getIdentifier()));
						} catch (JSONException e) {
							e.printStackTrace();
						}
						
						TVObject tuic = tvi.add(new TVObject(uic.toString(), uic, sd));
						addForms(tuic, uic);
					} else {
						addForms(tvi, uic);
					}
				}
			}
		}
	}
	
	private void getLocalActions(Object object, Map<String, UIDynamicAction> locals) {
		List<DatabaseObject> list = new ArrayList<>();
		if (object instanceof ApplicationComponent) {
			list.addAll(((ApplicationComponent)object).getAllChildren());
		} else if (object instanceof PageComponent) {
			list.addAll(((PageComponent)object).getAllChildren());
		} else if (object instanceof UIComponent) {
			list.addAll(((UIComponent)object).getAllChildren());
		}
		
		for (DatabaseObject dbo : list) {
			if (dbo instanceof UIDynamicAction) {
				UIDynamicAction uida = (UIDynamicAction)dbo;
				if (uida.isSetLocalAction()) {
					String key = uida.getSetActionKeyName();
					if (key != null && !key.isEmpty() && !locals.containsKey(key)) {
						locals.put(key, uida);
					}
				}
			}
			getLocalActions(dbo, locals);
		}
	}
	
	private void addLocals(TVObject tvi, Object object) {
		if (object != null) {
			Map<String, UIDynamicAction> locals = null;
			if (object instanceof ApplicationComponent) {
				locals = new TreeMap<>();
				getLocalActions(object, locals);
			}
			
			if (locals != null) {
				try {
					JSONObject jsonInfos = new JSONObject();
					for (String key: locals.keySet()) {
						jsonInfos.put(key, "");
					}

					SourceData sd = null;
					try {
						sd = Filter.Local.toSourceData(new JSONObject());
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					tvi.add(new TVObject("localObject", object, sd, jsonInfos));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private void getGlobalActions(Object object, Map<String, UIDynamicAction> globals) {
		List<DatabaseObject> list = new ArrayList<>();
		if (object instanceof ApplicationComponent) {
			list.addAll(((ApplicationComponent)object).getAllChildren());
		} else if (object instanceof PageComponent) {
			list.addAll(((PageComponent)object).getAllChildren());
		} else if (object instanceof UIComponent) {
			list.addAll(((UIComponent)object).getAllChildren());
		}
		
		for (DatabaseObject dbo : list) {
			if (dbo instanceof UIDynamicAction) {
				UIDynamicAction uida = (UIDynamicAction)dbo;
				if (uida.isSetGlobalAction()) {
					String key = uida.getSetActionKeyName();
					if (key != null && !key.isEmpty() && !globals.containsKey(key)) {
						globals.put(key, uida);
					}
				}
				if (uida.isFullSyncSyncAction()) {
					String key = "FullSyncSyncAction";
					if (!globals.containsKey(key)) {
						globals.put(key, uida);
					}
				}
			}
			getGlobalActions(dbo, globals);
		}
	}
	
	private void addGlobals(TVObject tvi, Object object) {
		if (object != null) {
			Map<String, UIDynamicAction> globals = null;
			if (object instanceof ApplicationComponent) {
				globals = new TreeMap<>();
				getGlobalActions(object, globals);
			}
			
			if (globals != null) {
				try {
					JSONObject jsonFSSA = new JSONObject().put("FullSyncSyncAction",
							new JSONObject().put("progress", new JSONObject()
												.put("changed","")
												.put("continuous","")
												.put("finished","")
												.put("pull","")
												.put("current","")
												.put("total","")
												.put("status","")
												.put("taskInfo","")
												.put("raw","")
											));
					
					JSONObject jsonInfos = new JSONObject();
					for (String key: globals.keySet()) {
						if ("FullSyncSyncAction".equals(key)) {
							jsonInfos.put(key, jsonFSSA.get(key));
						} else {
							jsonInfos.put(key, "");
						}
					}

					SourceData sd = null;
					try {
						sd = Filter.Global.toSourceData(new JSONObject());
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					tvi.add(new TVObject("sharedObject", object, sd, jsonInfos));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private void addJsonObjects(TVObject tvp) {
		try {
			if (tvp != null) {
				Object object = tvp.getObject();
				
				if (object instanceof JSONObject) {
					JSONObject jsonObject = (JSONObject)object;
					for (Iterator<String> it = GenericUtils.cast(jsonObject.keys()); it.hasNext();) {
						String key = it.next();
						TVObject tvo = new TVObject(key, jsonObject.get(key), null);
						addJsonObjects(tvo);
						tvp.add(tvo);
					}
				} else if (object instanceof JSONArray) {
					JSONArray jsonArray = (JSONArray)object;
					for (int i = 0; i < jsonArray.length(); i++) {
						TVObject tvo = new TVObject("["+i+"]", jsonArray.get(i), null);
						addJsonObjects(tvo);
						tvp.add(tvo);
					}
				} else {
					String key = object.toString();
					if (!key.isEmpty()) {
						TVObject tvo = new TVObject(key, object, null);
						tvp.add(tvo);
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
