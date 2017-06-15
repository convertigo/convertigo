/*
 * Copyright (c) 2001-2016 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.eclipse.views.mobile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.twinsoft.convertigo.beans.connectors.FullSyncConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Document;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.couchdb.DesignDocument;
import com.twinsoft.convertigo.beans.mobile.components.MobileSmartSource.Filter;
import com.twinsoft.convertigo.beans.mobile.components.PageComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIControlDirective;
import com.twinsoft.convertigo.beans.mobile.components.UIControlDirective.AttrDirective;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.CouchKey;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class MobilePickerContentProvider implements ITreeContentProvider {
	
	private static Pattern INVALID_CHARACTERS = Pattern.compile("[~:]+");
	
	public class TVObject {
		private String name;
		private Object object;
		private TVObject parent;
		private List<TVObject> children = new ArrayList<TVObject>();
		
		private TVObject(String name) {
			this(name, null);
		}
		
		private TVObject (String name, Object object) {
			this.name = name;
			this.object = object;
		}

		public String toString() {
			return name;
		}
		
		public String getSourcePath() {
			String path = INVALID_CHARACTERS.matcher(name).find() ?  "['"+name+"']":name;
			if (parent != null) {
				path = parent.getSourcePath() + (path.startsWith("[") ? "":"?.") + path;
			}
			return path;
		}
		
		public String getSourceData() {
			String param = "";
			if (object != null) {
				if (object instanceof Sequence) {
					Sequence sequence = (Sequence)object;
					param = "'"+ sequence.getQName() +"'";
				} else if (object instanceof DesignDocument) {
					DesignDocument dd = (DesignDocument)object;
					String db = parent.parent.parent.getName();
					String ddoc = dd.getName();
					String dview = parent.getName();
					String verb = name;
					param = "'fs://"+ db +"."+ verb +", {ddoc='"+ddoc+"', view='"+dview+"'}'";
				} else if (object instanceof UIControlDirective) {
					UIControlDirective directive = (UIControlDirective)object;
					param = "item"+ directive.priority;
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
		
	}
	
	private Filter filter = Filter.Sequence;
	private Object selected = null;
	
	public MobilePickerContentProvider() {
		
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
		} else if (parentElement instanceof PageComponent) {
			PageComponent pageComponent = (PageComponent)parentElement;
			Project project = pageComponent.getProject();
			
			ProjectExplorerView projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
			List<String> projectNames = Engine.theApp.databaseObjectsManager.getAllProjectNamesList();
			
			TVObject root = new TVObject("root", pageComponent);
			if (filter.equals(Filter.Sequence)) {
				TVObject tvs = root.add(new TVObject("sequences"));
				for (String projectName : projectNames) {
					try {
						Project p = projectExplorerView.getProject(projectName);
						boolean isReferenced = !p.getName().equals(project.getName());
						addSequences(tvs, isReferenced ? p:project, isReferenced);
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
						addFsObjects(tvd, isReferenced ? p:project, isReferenced);
					} catch (Exception e) {
					}
				}
			}
			if (filter.equals(Filter.Iteration)) {
				TVObject tvi = root.add(new TVObject("iterations"));
				addIterations(tvi, pageComponent);
			}
			return root.children.toArray();
		} else if (parentElement instanceof JSONObject) {
			JSONObject jsonObject = (JSONObject)parentElement;
			
			TVObject root = new TVObject("root", jsonObject);
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

	private void addSequences(TVObject tvs, Object object, boolean isReferenced) {
		if (object != null) {
			if (object instanceof Project) {
				Project project = (Project)object;
				for (Sequence s : project.getSequencesList()) {
					String label = isReferenced ? s.getQName():s.getName();
					tvs.add(new TVObject(label, s));
				}
			}
		}
	}
	
	private void addFsObjects(TVObject tvd, Object object, boolean isReferenced) {
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
											TVObject tvv = tdd.add(new TVObject(it.next()));
											tvv.add(new TVObject("get", d));
											tvv.add(new TVObject("view", d));
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
						
						UIControlDirective uicd = (UIControlDirective)uic;
						if (AttrDirective.RepeatForEach.equals(AttrDirective.getDirective(uicd.getDirectiveName()))) {
							TVObject tuic = tvi.add(new TVObject(uic.toString(), uic));
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
	
	private void addJsonObjects(TVObject tvp) {
		try {
			if (tvp != null) {
				Object object = tvp.getObject();
				
				if (object instanceof JSONObject) {
					JSONObject jsonObject = (JSONObject)object;
					for (Iterator<String> it = GenericUtils.cast(jsonObject.keys()); it.hasNext();) {
						String key = it.next();
						TVObject tvo = new TVObject(key, jsonObject.get(key));
						addJsonObjects(tvo);
						tvp.add(tvo);
					}
				} else if (object instanceof JSONArray) {
					JSONArray jsonArray = (JSONArray)object;
					for (int i = 0; i < jsonArray.length(); i++) {
						TVObject tvo = new TVObject("["+i+"]", jsonArray.get(i));
						addJsonObjects(tvo);
						tvp.add(tvo);
					}
				} else {
					String key = object.toString();
					if (!key.isEmpty()) {
						TVObject tvo = new TVObject(key, object);
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
