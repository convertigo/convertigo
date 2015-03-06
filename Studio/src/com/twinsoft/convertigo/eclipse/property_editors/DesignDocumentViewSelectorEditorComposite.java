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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import com.twinsoft.convertigo.beans.connectors.CouchDbConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Document;
import com.twinsoft.convertigo.beans.core.Listener;
import com.twinsoft.convertigo.beans.couchdb.DesignDocument;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ListenerTreeObject;
import com.twinsoft.convertigo.engine.enums.CouchKey;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class DesignDocumentViewSelectorEditorComposite extends AbstractDialogComposite implements ITreeContentProvider, ILabelProvider, ISelectionChangedListener {

	private TreeViewer couchdbViews;
	
	private class TVObject {
		protected String name;
		protected TVObject parent;

		public TVObject(String name) {
			this.name = name;
		}
	}
	
	private class TVRoot extends TVObject {
		private List<TVConnector> connectors;

		public TVRoot() {
			super("root");
			connectors = new ArrayList<TVConnector>();
		}

		public void addConnector(TVConnector connector) {
			connectors.add(connector);
			connector.parent = this;
		}

		public List<TVConnector> getConnectors() {
			return connectors;
		}
	}
	
	private class TVConnector extends TVObject {
		private List<TVDocument> documents;
		private String projectName;
		
		public TVConnector(String name, String projectName) {
			super(name);
			this.projectName = projectName;
			documents = new ArrayList<TVDocument>();
		}

		public void addDocument(TVDocument document) {
			documents.add(document);
			document.parent = this;
		}

		public List<TVDocument> getDocuments() {
			return documents;
		}
	}
	
	private class TVDocument extends TVObject {
		private List<TVView> views;

		public TVDocument(String name) {
			super(name);
			views = new ArrayList<TVView>();
		}

		public void addView(TVView view) {
			views.add(view);
			view.parent = this;
		}

		public List<TVView> getViews() {
			return views;
		}
	}
	
	private class TVView extends TVObject {
		public TVView(String name) {
			super(name);
		}
		
		public String getTargetName() {
			TVDocument tvDocument = (TVDocument)parent;
			TVConnector tvConnector = (TVConnector) tvDocument.parent;
			return tvConnector.projectName + "." + tvConnector.name 
							+ "." + tvDocument.name + "." + name;
		}
	}
	
	private String targetView;
	
	public DesignDocumentViewSelectorEditorComposite(Composite parent, int style, AbstractDialogCellEditor cellEditor) {
		super(parent, style, cellEditor);
		
		targetView = (String) cellEditor.databaseObjectTreeObject
				.getPropertyValue(cellEditor.propertyDescriptor.getId());
		
		initialize();
	}

	private void initialize() {
		setLayout(new FillLayout());
		couchdbViews = new TreeViewer(this);
		couchdbViews.setContentProvider(this);
		couchdbViews.setLabelProvider(this);
		couchdbViews.setInput(getInitalInput());
		couchdbViews.expandAll();
		couchdbViews.addSelectionChangedListener(this);

		//couchdbViews.collapseAll();
	}
	
	private TVView selectedTVView = null;
	
	@Override
	public void performPostDialogCreation() {
		if (selectedTVView != null) {
			ISelection selection = new StructuredSelection(selectedTVView);
			couchdbViews.setSelection(selection, true);
		} else {
			if (targetView != null) {
				ConvertigoPlugin.logError("Target view not found ('" + targetView + "'). "
						+ "Please check your connector and choose another view.", true);
			}
			parentDialog.enableOK(false);
		}
	}
	
	private Object getInitalInput() {
		TVRoot tvRoot = new TVRoot();

		try {
			ProjectExplorerView projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
			ListenerTreeObject listenerTreeObject = (ListenerTreeObject) projectExplorerView.getFirstSelectedTreeObject();
			if (listenerTreeObject != null) {
				Listener listener = listenerTreeObject.getObject();
				Connector connector = listener.getConnector();
				if (connector instanceof CouchDbConnector) {
					TVConnector tvConnector = new TVConnector(connector.getName(), connector.getProject().getName());
					tvRoot.addConnector(tvConnector);
					for (Document document : connector.getDocumentsList()) {
						TVDocument tvDocument = new TVDocument(document.getName());
						tvConnector.addDocument(tvDocument);
						if (document instanceof DesignDocument) {
							JSONObject json = ((DesignDocument)document).getJSONObject();
							JSONObject views = CouchKey.views.JSONObject(json);
							if (views != null) {
								for (Iterator<String> i = GenericUtils.cast(views.keys()); i.hasNext(); ) {
									TVView tvView = new TVView(i.next());
									tvDocument.addView(tvView);
									if (tvView.getTargetName().equals(targetView)) {
										selectedTVView = tvView;
									}
								}
							}
						}
					}
				}
			}
			
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Error while analyzing the views hierarchy", true);
		}

		return tvRoot;
	}
	
	public Object getValue() {
		TVView tvView = (TVView) ((IStructuredSelection) couchdbViews.getSelection()).getFirstElement();
		if (tvView != null) {
			targetView = tvView.getTargetName();
		}
		return targetView;
	}

	@Override
	public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof TVDocument) {
			TVDocument tvDocument = (TVDocument) parentElement;
			return tvDocument.getViews().toArray();
		}
		else if (parentElement instanceof TVConnector) {
			TVConnector tvConnector = (TVConnector) parentElement;
			return tvConnector.getDocuments().toArray();
		}
		else if (parentElement instanceof TVRoot) {
			TVRoot tvRoot = (TVRoot) parentElement;
			return tvRoot.getConnectors().toArray();
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
		if (event.getSelection() instanceof IStructuredSelection) {
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
			Object selectedObject = selection.getFirstElement();
			parentDialog.enableOK(selectedObject instanceof TVView);
		}
	}

}
