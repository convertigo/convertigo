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

package com.twinsoft.convertigo.eclipse.views.mobile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
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
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicInvoke;
import com.twinsoft.convertigo.beans.ngx.components.UIEventSubscriber;
import com.twinsoft.convertigo.beans.ngx.components.UIForm;
import com.twinsoft.convertigo.beans.ngx.components.UIPageEvent;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponent;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponentEvent;
import com.twinsoft.convertigo.beans.ngx.components.UIUseShared;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.picker.DatabaseObjectPickerProjectOrder;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.engine.util.GenericUtils;

class NgxPickerContentProvider extends AbstractRequestablePickerContentProvider {

	private Filter filter = Filter.Sequence;

	public void setFilterBy(Filter filter) {
		this.filter = filter;
	}
	
	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof TVObject) {
			return ((TVObject) parentElement).getChildren().toArray();
		}
		if (parentElement instanceof MobileComponent) {
			MobileComponent mobileComponent = (MobileComponent)parentElement;
			Project project = mobileComponent.getProject();
			
			ProjectExplorerView projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
			List<String> projectNames = DatabaseObjectPickerProjectOrder.getOrderedProjectNames(project);

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
				for (String projectName : projectNames) {
					try {
						Project p = projectExplorerView.getProject(projectName);
						if (!p.getSequencesList().isEmpty()) {
							var tvp = root.add(new TVObject(projectName, p, null));
							boolean isReferenced = !p.getName().equals(project.getName());
							addSequences(map, tvp, p, isReferenced);
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
						addFsObjects(map, tvd, p, isReferenced);
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
			return root.getChildren().toArray();
		}
		if (parentElement instanceof JSONObject) {
			JSONObject jsonObject = (JSONObject)parentElement;
			
			TVObject root = new TVObject("root", jsonObject, null);
			addJsonObjects(root);
			
			return root.getChildren().toArray();
		}
		return new Object[0];
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
			List<UIComponent> list = null;
			if (object instanceof ApplicationComponent app) {
				list = GenericUtils.cast(app.getUIAppEventList());
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
					for (var child : tvi.getChildren()) {
						TVObject tvo = (TVObject) child;
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
						if (page != null && tvLocals != null && tvLocals.getChildren().isEmpty()) {
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
			if (object instanceof UIUseShared) {
				UIUseShared uius = (UIUseShared)object;
				if (!uius.getSharedComponentQName().isEmpty()) {
					UISharedComponent uisc = uius.getTargetSharedComponent();
					if (uisc != null && uisc.isEnabled()) {
						if (!uius.isRecursive()) {
							list.addAll(uisc.getAllChildren());
						}
					}
				}
			}
			if (object instanceof UIDynamicInvoke) {
				UIDynamicInvoke uidi = (UIDynamicInvoke)object;
				if (!uidi.getSharedActionQName().isEmpty()) {
					UIActionStack uias = uidi.getTargetSharedAction();
					if (uias != null && uias.isEnabled()) {
						if (!uidi.isRecursive()) {
							list.addAll(uias.getAllChildren());
						}
					}
				}
			}
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
				locals = new TreeMap<>(GenericUtils.CASE_INSENSITIVE_ORDER);
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
			if (object instanceof UIUseShared) {
				UIUseShared uius = (UIUseShared)object;
				if (!uius.getSharedComponentQName().isEmpty()) {
					UISharedComponent uisc = uius.getTargetSharedComponent();
					if (uisc != null && uisc.isEnabled()) {
						if (!uius.isRecursive()) {
							list.addAll(uisc.getAllChildren());
						}
					}
				}
			}
			if (object instanceof UIDynamicInvoke) {
				UIDynamicInvoke uidi = (UIDynamicInvoke)object;
				if (!uidi.getSharedActionQName().isEmpty()) {
					UIActionStack uias = uidi.getTargetSharedAction();
					if (uias != null && uias.isEnabled()) {
						if (!uidi.isRecursive()) {
							list.addAll(uias.getAllChildren());
						}
					}
				}
			}
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
				globals = new TreeMap<>(GenericUtils.CASE_INSENSITIVE_ORDER);
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
	
	@Override
	protected String getExplicitSource(Object sourceData) {
		if (sourceData instanceof SourceData data) {
			return data.getSource();
		}
		return null;
	}

	@Override
	protected String computeLegacySource(TVObject node) {
		Object object = node.getObject();
		if (object instanceof com.twinsoft.convertigo.beans.core.Sequence sequence) {
			String marker = node.getInfos().optString("marker");
			return "'" + sequence.getQName() + (!marker.isEmpty() ? "#" : "") + marker + "'";
		}
		if (object instanceof com.twinsoft.convertigo.beans.couchdb.DesignDocument dd) {
			String db = dd.getParent().getQName();
			String ddoc = dd.getName();
			TVObject parent = node.getParent();
			String dview = parent == null ? "" : parent.getName();
			String includeDocs = node.getInfos().optString("include_docs", "false");
			return "'fs://" + db + "." + node.getName() + ", {ddoc='" + ddoc + "', view='" + dview + "', include_docs='" + includeDocs + "'}'";
		}
		if (object instanceof UIControlDirective directive) {
			return "item" + directive.priority;
		}
		if (object instanceof UIForm form) {
			return "form" + form.priority;
		}
		if (object instanceof ApplicationComponent) {
			return "router.sharedObject";
		}
		return "";
	}

	@Override
	protected SourceData createSequenceSourceData(String sequenceQName, String marker) {
		try {
			JSONObject json = new JSONObject().put("sequence", sequenceQName);
			if (marker != null && !marker.isEmpty()) {
				json.put("marker", marker);
			}
			return Filter.Sequence.toSourceData(json);
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	protected SourceData createDatabaseSourceData(String connectorQName, String documentQName, String queryView, String verb, String marker, boolean includeDocs) {
		try {
			JSONObject json = new JSONObject()
				.put("connector", connectorQName)
				.put("document", documentQName)
				.put("queryview", queryView)
				.put("verb", verb);
			if (marker != null && !marker.isEmpty()) {
				json.put("marker", marker);
			}
			if (includeDocs) {
				json.put("includeDocs", true);
			}
			return Filter.Database.toSourceData(json);
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}

}
