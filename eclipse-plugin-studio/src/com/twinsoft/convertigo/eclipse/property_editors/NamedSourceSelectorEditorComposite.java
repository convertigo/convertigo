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

package com.twinsoft.convertigo.eclipse.property_editors;

import java.util.Iterator;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Document;
import com.twinsoft.convertigo.beans.core.IApplicationComponent;
import com.twinsoft.convertigo.beans.core.Listener;
import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.couchdb.AbstractFullSyncFilterListener;
import com.twinsoft.convertigo.beans.couchdb.AbstractFullSyncViewListener;
import com.twinsoft.convertigo.beans.couchdb.DesignDocument;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.picker.DatabaseObjectPickerFilter;
import com.twinsoft.convertigo.eclipse.views.picker.DatabaseObjectPickerFilterSupport;
import com.twinsoft.convertigo.eclipse.views.picker.DatabaseObjectPickerLabelProvider;
import com.twinsoft.convertigo.eclipse.views.picker.DatabaseObjectPickerNode;
import com.twinsoft.convertigo.eclipse.views.picker.DatabaseObjectPickerProjectOrder;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ViewContentProvider;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.INamedSourceSelectorTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UnloadedProjectTreeObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.CouchKey;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class NamedSourceSelectorEditorComposite extends AbstractDialogComposite implements ISelectionChangedListener {

	private class TVObject extends DatabaseObjectPickerNode {

		public TVObject(String name) {
			this(name, false);
		}

		public TVObject(String name, boolean selectable) {
			this(name, name, null, selectable);
		}

		public TVObject(String name, String label, Object object, boolean selectable) {
			super(name, label, object, selectable, null, null);
		}

		public boolean isRoot() {
			return "root".equals(getName());
		}

		public String getTargetName() {
			String targetName = getName();
			if (getParent() instanceof TVObject parent && !parent.isRoot()) {
				targetName = parent.getTargetName() + "." + targetName;
			}
			return targetName;
		}

		@Override
		public String getTechnicalText() {
			return getTargetName();
		}

		public TVObject add(TVObject child) {
			if (child != null && (child.isSelectable() || !child.isEmpty())) {
				if (!getChildren().contains(child)) {
					super.add(child);
					if (child.getTargetName().equals(sourcedObjectName)) {
						selectedTVObject = child;
					}
					if (getParent() instanceof TVObject parent) {
						parent.showInParent(this);
					}
				}
				return child;
			}
			return null;
		}

		private void showInParent(TVObject child) {
			if (child != null && !getChildren().contains(child)) {
				super.add(child);
				if (getParent() instanceof TVObject parent) {
					parent.showInParent(this);
				}
			}
		}

		public void addObject(Object object) {
			if (!(object instanceof DatabaseObject dbo)) {
				return;
			}

			boolean selectable = NamedSourceSelectorEditorComposite.this.isSelectable(dbo);
			TVObject tvObject = new TVObject(dbo.getName(), dbo.toString(), dbo, selectable);
			super.add(tvObject);

			if (dbo instanceof Project project) {
				if (project.getName().equals(currentProjectName)) {
					currentProjectTVObject = tvObject;
				}
				MobileApplication mobileApplication = project.getMobileApplication();
				if (mobileApplication != null) {
					tvObject.addObject(mobileApplication);
				}
				for (Connector connector : project.getConnectorsList()) {
					tvObject.addObject(connector);
				}
				for (Sequence sequence : project.getSequencesList()) {
					tvObject.addObject(sequence);
				}
			} else if (dbo instanceof MobileApplication mobileApplication) {
				IApplicationComponent applicationComponent = mobileApplication.getApplicationComponent();
				if (applicationComponent != null) {
					tvObject.addObject(applicationComponent);
				}
			}
			// MOBILE COMPONENTS
			else if (dbo instanceof com.twinsoft.convertigo.beans.mobile.components.MobileComponent mobileDbo) {
				if (mobileDbo instanceof com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent mobileApp) {
					for (com.twinsoft.convertigo.beans.mobile.components.UIDynamicMenu menu : mobileApp.getMenuComponentList()) {
						tvObject.addObject(menu);
					}
					for (com.twinsoft.convertigo.beans.mobile.components.PageComponent page : mobileApp.getPageComponentList()) {
						tvObject.addObject(page);
					}
					for (com.twinsoft.convertigo.beans.mobile.components.UIActionStack stack : mobileApp.getSharedActionList()) {
						tvObject.addObject(stack);
					}
					for (com.twinsoft.convertigo.beans.mobile.components.UISharedComponent shared : mobileApp.getSharedComponentList()) {
						tvObject.addObject(shared);
					}
				} else if (mobileDbo instanceof com.twinsoft.convertigo.beans.mobile.components.UIDynamicMenu menu) {
					for (com.twinsoft.convertigo.beans.mobile.components.UIComponent component : menu.getUIComponentList()) {
						tvObject.addObject(component);
					}
				} else if (mobileDbo instanceof com.twinsoft.convertigo.beans.mobile.components.PageComponent page) {
					for (com.twinsoft.convertigo.beans.mobile.components.UIComponent component : page.getUIComponentList()) {
						tvObject.addObject(component);
					}
				} else if (mobileDbo instanceof com.twinsoft.convertigo.beans.mobile.components.UIComponent component) {
					for (com.twinsoft.convertigo.beans.mobile.components.UIComponent child : component.getUIComponentList()) {
						tvObject.addObject(child);
					}
				}
			}
			// NGX COMPONENTS
			else if (dbo instanceof com.twinsoft.convertigo.beans.ngx.components.MobileComponent ngxDbo) {
				if (ngxDbo instanceof com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent ngxApp) {
					for (com.twinsoft.convertigo.beans.ngx.components.UIDynamicMenu menu : ngxApp.getMenuComponentList()) {
						tvObject.addObject(menu);
					}
					for (com.twinsoft.convertigo.beans.ngx.components.PageComponent page : ngxApp.getPageComponentList()) {
						tvObject.addObject(page);
					}
					for (com.twinsoft.convertigo.beans.ngx.components.UIActionStack stack : ngxApp.getSharedActionList()) {
						tvObject.addObject(stack);
					}
					for (com.twinsoft.convertigo.beans.ngx.components.UISharedComponent shared : ngxApp.getSharedComponentList()) {
						tvObject.addObject(shared);
					}
				} else if (ngxDbo instanceof com.twinsoft.convertigo.beans.ngx.components.UIDynamicMenu menu) {
					for (com.twinsoft.convertigo.beans.ngx.components.UIComponent component : menu.getUIComponentList()) {
						tvObject.addObject(component);
					}
				} else if (ngxDbo instanceof com.twinsoft.convertigo.beans.ngx.components.PageComponent page) {
					for (com.twinsoft.convertigo.beans.ngx.components.UIComponent component : page.getUIComponentList()) {
						tvObject.addObject(component);
					}
				} else if (ngxDbo instanceof com.twinsoft.convertigo.beans.ngx.components.UIComponent component) {
					for (com.twinsoft.convertigo.beans.ngx.components.UIComponent child : component.getUIComponentList()) {
						tvObject.addObject(child);
					}
				}
			} else if (dbo instanceof Connector connector) {
				for (Transaction transaction : connector.getTransactionsList()) {
					tvObject.addObject(transaction);
				}
				for (Document document : connector.getDocumentsList()) {
					tvObject.addObject(document);
				}
				for (Listener listener : connector.getListenersList()) {
					tvObject.addObject(listener);
				}
			} else if (dbo instanceof Document document && document instanceof DesignDocument designDocument) {
				JSONObject json = designDocument.getJSONObject();
				DatabaseObject currentObject = dboto.getObject();
				if (currentObject instanceof AbstractFullSyncViewListener
						|| currentObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIDynamicElement
						|| currentObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIDynamicElement) {
					JSONObject views = CouchKey.views.JSONObject(json);
					if (views != null) {
						for (Iterator<String> it = GenericUtils.cast(views.keys()); it.hasNext();) {
							String key = it.next();
							String viewName = tvObject.getTargetName() + "." + key;
								tvObject.add(new TVObject(key, NamedSourceSelectorEditorComposite.this.isSelectable(viewName)));
						}
					}
				}
				if (currentObject instanceof AbstractFullSyncFilterListener) {
					JSONObject filters = CouchKey.filters.JSONObject(json);
					if (filters != null) {
						for (Iterator<String> it = GenericUtils.cast(filters.keys()); it.hasNext();) {
							String key = it.next();
							String filterName = tvObject.getTargetName() + "." + key;
								tvObject.add(new TVObject(key, NamedSourceSelectorEditorComposite.this.isSelectable(filterName)));
						}
					}
				}
			}

			if (tvObject.getTargetName().equals(sourcedObjectName)) {
				selectedTVObject = tvObject;
			}
			if (!(tvObject.isSelectable() || !tvObject.isEmpty())) {
				super.remove(tvObject);
				return;
			}
			if (getParent() instanceof TVObject parent) {
				parent.showInParent(this);
			}
		}
	}

	private class TVRoot extends TVObject {

		public TVRoot() {
			super("root");
		}

		@Override
		public void addObject(Object object) {
			if (object != null) {
				super.addObject(object);
				return;
			}

			List<String> projectNames = DatabaseObjectPickerProjectOrder.getOrderedProjectNames(getCurrentProject());
			ProjectExplorerView projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
			for (String projectName : projectNames) {
				Project project = null;
				try {
					TreeObject projectTreeObject = ((ViewContentProvider) projectExplorerView.viewer.getContentProvider()).getProjectRootObject(projectName);
					if (projectTreeObject instanceof UnloadedProjectTreeObject) {
						project = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
					} else if (projectTreeObject != null) {
						project = projectExplorerView.getProject(projectName);
					}
				} catch (EngineException e) {
					ConvertigoPlugin.logError("Project \"" + projectName + "\" could not be loaded yet", true);
				}
				if (project != null) {
					super.addObject(project);
				}
			}
		}
	}

	private final DatabaseObjectTreeObject dboto;
	private final String propertyName;
	private final String currentProjectName;
	private final DatabaseObjectPickerFilter pickerFilter = new DatabaseObjectPickerFilter();
	private TreeViewer viewer;
	private Text filterText;
	private String sourcedObjectName;
	private TVObject selectedTVObject;
	private TVObject currentProjectTVObject;

	public NamedSourceSelectorEditorComposite(Composite parent, int style, AbstractDialogCellEditor cellEditor) {
		super(parent, style, cellEditor);

		dboto = cellEditor.databaseObjectTreeObject;
		propertyName = (String) cellEditor.propertyDescriptor.getId();
		Object pValue = dboto.getPropertyValue(propertyName);
			if (pValue instanceof com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType mobileSmartSourceType) {
				sourcedObjectName = mobileSmartSourceType.getSmartValue();
			} else if (pValue instanceof com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType ngxSmartSourceType) {
				sourcedObjectName = ngxSmartSourceType.getSmartValue();
		} else {
			sourcedObjectName = (String) pValue;
		}

		Project currentProject = getCurrentProject();
		currentProjectName = currentProject == null ? null : currentProject.getName();

		initialize();
	}

	private Project getCurrentProject() {
		DatabaseObject dbo = dboto.getObject();
		if (dbo instanceof Project project) {
			return project;
		}
		return dbo == null ? null : dbo.getProject();
	}

	private boolean isSelectable(Object object) {
		if (dboto instanceof INamedSourceSelectorTreeObject selectorTreeObject) {
			return selectorTreeObject.getNamedSourceSelector().isSelectable(propertyName, object);
		}
		return false;
	}

	private void initialize() {
		setLayout(new GridLayout(1, false));

		filterText = DatabaseObjectPickerFilterSupport.createFilterText(this);

		viewer = new TreeViewer(this, SWT.BORDER | SWT.SINGLE);
		viewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer.setContentProvider(new ITreeContentProvider() {

			@Override
			public void inputChanged(org.eclipse.jface.viewers.Viewer viewer, Object oldInput, Object newInput) {
			}

			@Override
			public void dispose() {
			}

			@Override
			public Object[] getElements(Object inputElement) {
				return getChildren(inputElement);
			}

			@Override
			public Object[] getChildren(Object parentElement) {
				if (parentElement instanceof DatabaseObjectPickerNode node) {
					return node.getChildren().toArray();
				}
				return new Object[0];
			}

			@Override
			public Object getParent(Object element) {
				if (element instanceof DatabaseObjectPickerNode node) {
					return node.getParent();
				}
				return null;
			}

			@Override
			public boolean hasChildren(Object element) {
				return element instanceof DatabaseObjectPickerNode node && !node.isEmpty();
			}
		});
		viewer.setLabelProvider(new DatabaseObjectPickerLabelProvider());
		viewer.addFilter(pickerFilter);
		viewer.setInput(getInitalInput());
		viewer.addSelectionChangedListener(this);

		applyDefaultExpansion();
		DatabaseObjectPickerFilterSupport.bind(filterText, pickerFilter, this::refreshViewer);
	}

	@Override
	public void performPostDialogCreation() {
		if (selectedTVObject != null) {
			ISelection selection = new StructuredSelection(selectedTVObject);
			viewer.setSelection(selection, true);
			expandAncestors(selectedTVObject);
		} else {
			if (sourcedObjectName != null && !sourcedObjectName.isEmpty()) {
				ConvertigoPlugin.logError("Source not found ('" + sourcedObjectName + "'). Please verify and choose another source.", true);
			}
			parentDialog.enableOK(false);
		}
	}

	private Object getInitalInput() {
		TVRoot tvRoot = new TVRoot();
		try {
			if (dboto.getObject() instanceof AbstractFullSyncFilterListener) {
				tvRoot.add(new TVObject("_doc_ids", true));
			}
			tvRoot.addObject(null);
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Error while analyzing the projects hierarchy", true);
		}
		return tvRoot;
	}

	private void refreshViewer() {
		Object currentSelection = ((IStructuredSelection) viewer.getSelection()).getFirstElement();
		viewer.refresh();
		if (pickerFilter.isActive()) {
			viewer.expandAll();
		} else {
			applyDefaultExpansion();
		}
		if (currentSelection != null) {
			viewer.setSelection(new StructuredSelection(currentSelection), true);
		} else if (selectedTVObject != null) {
			viewer.setSelection(new StructuredSelection(selectedTVObject), true);
		}
	}

	private void applyDefaultExpansion() {
		viewer.collapseAll();
		if (currentProjectTVObject != null) {
			viewer.setExpandedState(currentProjectTVObject, true);
		}
		expandAncestors(selectedTVObject);
	}

	private void expandAncestors(TVObject tvObject) {
		while (tvObject != null) {
			viewer.setExpandedState(tvObject, true);
			tvObject = tvObject.getParent() instanceof TVObject parent ? parent : null;
		}
	}

	public Object getValue() {
		TVObject tvSelected = (TVObject) ((IStructuredSelection) viewer.getSelection()).getFirstElement();
		if (tvSelected != null) {
			sourcedObjectName = tvSelected.getTargetName();
		} else if (selectedTVObject != null) {
			if (SWT.YES == ConvertigoPlugin.questionMessageBox(parentDialog.getShell(), "Do you want to clear the previously sourced object ?")) {
				sourcedObjectName = "";
			}
		}
		return sourcedObjectName;
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		try {
			if (event.getSelection() instanceof IStructuredSelection selection) {
				Object selected = selection.getFirstElement();
				parentDialog.enableOK(selection.isEmpty() || (selected instanceof TVObject tvObject && tvObject.isSelectable()));
			}
		} catch (Exception e) {
			parentDialog.enableOK(false);
		}
	}
}
