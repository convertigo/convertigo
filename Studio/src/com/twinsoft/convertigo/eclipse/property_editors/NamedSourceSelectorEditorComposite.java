/*
 * Copyright (c) 2001-2011 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.property_editors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Document;
import com.twinsoft.convertigo.beans.core.Listener;
import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.couchdb.AbstractFullSyncFilterListener;
import com.twinsoft.convertigo.beans.couchdb.AbstractFullSyncViewListener;
import com.twinsoft.convertigo.beans.couchdb.DesignDocument;
import com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType;
import com.twinsoft.convertigo.beans.mobile.components.PageComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIDynamicMenu;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
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

public class NamedSourceSelectorEditorComposite extends AbstractDialogComposite implements ITreeContentProvider, ILabelProvider, ISelectionChangedListener {

	private class TVObject {
		private String name;
		private boolean isSelectable = false;
		private TVObject parent;
		private List<Object> children = new ArrayList<Object>();
		
		public TVObject(String name) {
			this(name, false);
		}
		
		public TVObject (String name, boolean isSelectable) {
			this.name = name;
			this.isSelectable = isSelectable;
		}
		
		public boolean isRoot() {
			return name.equals("root");
		}
		
		public String getTargetName() {
			String targetName = name;
			if (parent != null && !parent.isRoot()) {
				targetName = parent.getTargetName() + "." + targetName;
			}
			return targetName;
		}
		
		public TVObject add(TVObject child) {
			if (child != null) {
				child.parent = this;
				if (child.isSelectable) {
					if (!children.contains(child)) {
						children.add(child);
						if (child.getTargetName().equals(sourcedObjectName)) {
							selectedTVObject = child;
						}
						if (parent != null) {
							parent.showInParent(this);
						}
					}
				}
			}
			return child;
		}
		
		private void showInParent(TVObject child) {
			if (child != null) {
				if (!children.contains(child)) {
					children.add(child);
					if (parent != null) {
						parent.showInParent(this);
					}
				}
			}
		}
		
		public void addObject(Object object) {
			if (object != null) {
				if (object instanceof DatabaseObject) {
					DatabaseObject dbo = (DatabaseObject)object;
					
					TVObject tvObject = add(new TVObject(dbo.getName(), isSelectable(dbo)));
					
					if (object instanceof Project) {
						MobileApplication mba = ((Project)object).getMobileApplication();
						if (mba != null) {
							tvObject.addObject(mba);
						}
						for (Connector connector : ((Project)object).getConnectorsList()) {
							tvObject.addObject(connector);
						}
						for (Sequence sequence : ((Project)object).getSequencesList()) {
							tvObject.addObject(sequence);
						}
					}
					else if (object instanceof MobileApplication) {
						ApplicationComponent ac = ((MobileApplication)object).getApplicationComponent();
						if (ac != null) {
							tvObject.addObject(ac);
						}
					}
					else if (object instanceof ApplicationComponent) {
						for (UIDynamicMenu menu: ((ApplicationComponent)object).getMenuComponentList()) {
							tvObject.addObject(menu);
						}
						for (PageComponent page: ((ApplicationComponent)object).getPageComponentList()) {
							tvObject.addObject(page);
						}
					}
					else if (object instanceof Connector) {
						for (Transaction transaction : ((Connector)object).getTransactionsList()) {
							tvObject.addObject(transaction);
						}
						for (Document document : ((Connector)object).getDocumentsList()) {
							tvObject.addObject(document);
						}
						for (Listener listener : ((Connector)object).getListenersList()) {
							tvObject.addObject(listener);
						}
					}
					else if (object instanceof Transaction) {
						//
					}
					else if (object instanceof Sequence) {
						//
					}
					else if (object instanceof Listener) {
						//
					}
					else if (object instanceof Document) {
						if (object instanceof DesignDocument) {
							JSONObject json = ((DesignDocument)object).getJSONObject();
							if (dboto.getObject() instanceof AbstractFullSyncViewListener) {
								JSONObject views = CouchKey.views.JSONObject(json);
								if (views != null) {
									for (Iterator<String> it = GenericUtils.cast(views.keys()); it.hasNext(); ) {
										String key = it.next();
										String viewName = tvObject.getTargetName() + "." + key;
										tvObject.add(new TVObject(key, isSelectable(viewName)));
									}
								}
							}
							if (dboto.getObject() instanceof AbstractFullSyncFilterListener) {
								JSONObject filters = CouchKey.filters.JSONObject(json);
								if (filters != null) {
									for (Iterator<String> it = GenericUtils.cast(filters.keys()); it.hasNext(); ) {
										String key = it.next();
										String filterName = tvObject.getTargetName() + "." + key;
										tvObject.add(new TVObject(key, isSelectable(filterName)));
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	private class TVRoot extends TVObject {
		public TVRoot() {
			super("root");
		}
		
		@Override
		public void addObject(Object object) {
			if (object == null) {
				List<String> projectNames = Engine.theApp.databaseObjectsManager.getAllProjectNamesList();
				ProjectExplorerView projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
				for (String projectName : projectNames) {
					Project project = null;
					try {
						TreeObject projectTreeObject = ((ViewContentProvider) projectExplorerView.viewer
								.getContentProvider()).getProjectRootObject(projectName);
						if (projectTreeObject instanceof UnloadedProjectTreeObject) {
							project = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
						} else {
							project = projectExplorerView.getProject(projectName);
						}
					}
					catch (EngineException e) {
						ConvertigoPlugin.logError("Project \""+projectName+"\" could not be loaded yet", true);
					}
					
					if (project == null) {
						continue;
					}
					
					super.addObject(project);
				}
			}
			else {
				super.addObject(object);
			}
		}
		
	}

	private TreeViewer viewer;
	private String sourcedObjectName;
	private TVObject selectedTVObject;
	private DatabaseObjectTreeObject dboto;
	private String propertyName;
	
	public NamedSourceSelectorEditorComposite(Composite parent, int style, AbstractDialogCellEditor cellEditor) {
		super(parent, style, cellEditor);
		
		dboto =  cellEditor.databaseObjectTreeObject;
		propertyName = (String) cellEditor.propertyDescriptor.getId();
		Object pValue = dboto.getPropertyValue(propertyName);
		if (pValue instanceof MobileSmartSourceType) {
			sourcedObjectName = ((MobileSmartSourceType)pValue).getSmartValue();
		} else {
			sourcedObjectName = (String)pValue;
		}
		
		initialize();
	}

	private boolean isSelectable(Object object) {
		if (dboto instanceof INamedSourceSelectorTreeObject) {
			return ((INamedSourceSelectorTreeObject)dboto).getNamedSourceSelector().isSelectable(propertyName, object);
		}
		return false;
	}
	
	private void initialize() {
		setLayout(new FillLayout());
		viewer = new TreeViewer(this);
		viewer.setContentProvider(this);
		viewer.setLabelProvider(this);
		viewer.setInput(getInitalInput());
		viewer.expandAll();
		viewer.addSelectionChangedListener(this);
	}
	
	@Override
	public void performPostDialogCreation() {
		if (selectedTVObject != null) {
			ISelection selection = new StructuredSelection(selectedTVObject);
			viewer.setSelection(selection, true);
		} else {
			if (sourcedObjectName != null && !sourcedObjectName.isEmpty()) {
				ConvertigoPlugin.logError("Source not found ('" + sourcedObjectName + "'). "
						+ "Please verify and choose another source.", true);
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
	
	public Object getValue() {
		TVObject tvSelected = (TVObject) ((IStructuredSelection) viewer.getSelection()).getFirstElement();
		if (tvSelected != null) {
			sourcedObjectName = tvSelected.getTargetName();
		}
		else if (selectedTVObject != null) {
			if (SWT.YES == ConvertigoPlugin.questionMessageBox(parentDialog.getShell(), "Do you want to clear the previously sourced object ?")) {
				sourcedObjectName = "";
			}
		}
		return sourcedObjectName;
	}

	@Override
	public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof TVObject) {
			return ((TVObject) parentElement).children.toArray();
		}
		return new Object[0];
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	public Object getParent(Object parentElement) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public Image getImage(Object element) {
		return null;
	}

	@Override
	public String getText(Object element) {
		if (element instanceof TVObject) {
			return ((TVObject) element).name;
		}
		return null;
	}
	

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		try {
			if (event.getSelection() instanceof IStructuredSelection) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				parentDialog.enableOK(selection.isEmpty() || ((TVObject) selection.getFirstElement()).isSelectable);
			}
		}
		catch (Exception e) {
			parentDialog.enableOK(false);
		}
	}

}
