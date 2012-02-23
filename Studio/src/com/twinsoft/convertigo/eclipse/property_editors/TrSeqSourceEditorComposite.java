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
 * $URL: http://sourceus.twinsoft.fr/svn/CEMS_opensource/branches/6.0.x/Studio/src/com/twinsoft/convertigo/eclipse/property_editors/CariocaServiceCodeEditorComposite.java $
 * $Author: fabienb $
 * $Revision: 28379 $
 * $Date: 2011-09-27 11:38:59 +0200 (mar., 27 sept. 2011) $
 */

package com.twinsoft.convertigo.eclipse.property_editors;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

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

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableStep;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.UnloadedProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ViewContentProvider;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class TrSeqSourceEditorComposite extends AbstractDialogComposite implements ITreeContentProvider,
		ILabelProvider, ISelectionChangedListener {

	private TreeViewer requestables;

	private class TVObject {
		protected String name;
		protected TVObject parent;

		public TVObject(String name) {
			this.name = name;
		}
	}

	private class TVRoot extends TVObject {
		public TVRoot() {
			super("root");
			projects = new ArrayList<TVProject>();
		}

		private List<TVProject> projects;

		public void addProject(TVProject project) {
			projects.add(project);
			project.parent = this;
		}

		public List<TVProject> getProjects() {
			return projects;
		}
	}

	private class TVProject extends TVObject {
		public TVProject(String name) {
			super(name);
			connectors = new ArrayList<TVConnector>();
			sequences = new ArrayList<TVSequence>();
		}

		private List<TVConnector> connectors;

		public void addConnector(TVConnector connector) {
			connectors.add(connector);
			connector.parent = this;
		}

		public List<TVConnector> getConnectors() {
			return connectors;
		}

		private List<TVSequence> sequences;

		public void addSequence(TVSequence sequence) {
			sequences.add(sequence);
			sequence.parent = this;
		}

		public List<TVSequence> getSequences() {
			return sequences;
		}
	}

	private class TVConnector extends TVObject {
		public TVConnector(String name) {
			super(name);
			transactions = new ArrayList<TVTransaction>();
		}

		private List<TVTransaction> transactions;

		public void addTransaction(TVTransaction transaction) {
			transactions.add(transaction);
			transaction.parent = this;
		}

		public List<TVTransaction> getTransactions() {
			return transactions;
		}
	}

	private class TVTransaction extends TVObject {
		public TVTransaction(String name) {
			super(name);
		}
	}

	private class TVSequence extends TVObject {
		public TVSequence(String name) {
			super(name);
		}
	}

	private enum RequestableType {
		TRANSACTION, SEQUENCE
	};

	private RequestableType requestableType;

	private String requestableSource;
	private String requestableSourceProject;
	private String requestableSourceSequence;
	private String requestableSourceConnector;
	private String requestableSourceTransaction;

	public TrSeqSourceEditorComposite(Composite parent, int style, AbstractDialogCellEditor cellEditor) {
		super(parent, style, cellEditor);

		requestableSource = (String) cellEditor.databaseObjectTreeObject
				.getPropertyValue(cellEditor.propertyDescriptor.getId());

		StringTokenizer st = new StringTokenizer(requestableSource, RequestableStep.SOURCE_SEPARATOR);
		requestableSourceProject = st.nextToken();
		requestableSourceSequence = null;
		requestableSourceConnector = null;
		requestableSourceTransaction = null;
		if (st.countTokens() == 1) {
			requestableType = RequestableType.SEQUENCE;
			requestableSourceSequence = st.nextToken();
		} else {
			requestableType = RequestableType.TRANSACTION;
			requestableSourceConnector = st.nextToken();
			requestableSourceTransaction = st.nextToken();
		}

		initialize();
	}

	private void initialize() {
		setLayout(new FillLayout());
		requestables = new TreeViewer(this);
		requestables.setContentProvider(this);
		requestables.setLabelProvider(this);
		requestables.setInput(getInitalInput());
		requestables.expandAll();
		requestables.addSelectionChangedListener(this);

		requestables.collapseAll();
	}

	@Override
	public void performPostDialogCreation() {
		if (selectedTVObject != null) {
			ISelection selection = new StructuredSelection(selectedTVObject);
			requestables.setSelection(selection, true);
		} else {
			ConvertigoPlugin.logError("Requestable source not found ('" + requestableSource
					+ "'). Please check your projects or choose another source.", true);
			parentDialog.enableOK(false);
		}
	}

	private TVObject selectedTVObject = null;

	private Object getInitalInput() {
		TVRoot tvRoot = new TVRoot();

		try {
			List<String> projectNames = Engine.theApp.databaseObjectsManager.getAllProjectNamesList();
			ProjectExplorerView projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
			for (String projectName : projectNames) {
				Project project = null;
				TreeObject projectTreeObject = ((ViewContentProvider) projectExplorerView.viewer
						.getContentProvider()).getProjectRootObject(projectName);
				if (projectTreeObject instanceof UnloadedProjectTreeObject) {
					project = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
				} else {
					project = projectExplorerView.getProject(projectName);
				}

				TVProject tvProject = new TVProject(projectName);
				tvRoot.addProject(tvProject);

				if (requestableType == RequestableType.TRANSACTION) {
					List<Connector> connectors = project.getConnectorsList();
					for (Connector connector : connectors) {
						String connectorName = connector.getName();
						TVConnector tvConnector = new TVConnector(connectorName);
						tvProject.addConnector(tvConnector);

						List<Transaction> transactions = connector.getTransactionsList();

						for (Transaction transaction : transactions) {
							String transactionName = transaction.getName();
							TVTransaction tvTransaction = new TVTransaction(transactionName);
							tvConnector.addTransaction(tvTransaction);
							if (projectName.equals(requestableSourceProject)
									&& connectorName.equals(requestableSourceConnector)
									&& transactionName.equals(requestableSourceTransaction)) {
								selectedTVObject = tvTransaction;
							}
						}
					}
				} else {
					List<Sequence> sequences = project.getSequencesList();
					for (Sequence sequence : sequences) {
						String sequenceName = sequence.getName();
						TVSequence tvSequence = new TVSequence(sequenceName);
						tvProject.addSequence(tvSequence);
						if (projectName.equals(requestableSourceProject)
								&& sequenceName.equals(requestableSourceSequence)) {
							selectedTVObject = tvSequence;
						}
					}
				}
			}
		} catch (EngineException e) {
			ConvertigoPlugin.logException(e, "Error while analyzing the projects hierarchy", true);
		}

		return tvRoot;
	}

	public Object getValue() {
		if (requestableType == RequestableType.TRANSACTION) {
			TVTransaction tvTransaction = (TVTransaction) ((IStructuredSelection) requestables.getSelection())
					.getFirstElement();
			requestableSourceTransaction = tvTransaction.name;
			TVConnector tvConnector = (TVConnector) tvTransaction.parent;
			requestableSourceConnector = tvConnector.name;
			TVProject tvProject = (TVProject) tvConnector.parent;
			requestableSourceProject = tvProject.name;
			requestableSource = requestableSourceProject + RequestableStep.SOURCE_SEPARATOR
					+ requestableSourceConnector + RequestableStep.SOURCE_SEPARATOR
					+ requestableSourceTransaction;
		} else {
			TVSequence tvSequence = (TVSequence) ((IStructuredSelection) requestables.getSelection())
					.getFirstElement();
			requestableSourceSequence = tvSequence.name;
			TVProject tvProject = (TVProject) tvSequence.parent;
			requestableSourceProject = tvProject.name;
			requestableSource = requestableSourceProject + RequestableStep.SOURCE_SEPARATOR
					+ requestableSourceSequence;
		}
		return requestableSource;
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof TVProject) {
			TVProject tvProject = (TVProject) parentElement;
			List<Object> list = new ArrayList<Object>();
			list.addAll(tvProject.getConnectors());
			list.addAll(tvProject.getSequences());
			return list.toArray();
		} else if (parentElement instanceof TVConnector) {
			TVConnector tvConnector = (TVConnector) parentElement;
			return tvConnector.getTransactions().toArray();

		} else if (parentElement instanceof TVRoot) {
			TVRoot tvRoot = (TVRoot) parentElement;
			return tvRoot.getProjects().toArray();
		}
		return new Object[0];
	}

	public Object getParent(Object element) {
		if (element instanceof TVObject) {
			return ((TVObject) element).parent;
		}
		return null;
	}

	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}

	public void addListener(ILabelProviderListener listener) {
	}

	public void removeListener(ILabelProviderListener listener) {
	}

	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	public void selectionChanged(SelectionChangedEvent event) {
		if (event.getSelection() instanceof IStructuredSelection) {
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
			Object selectedObject = selection.getFirstElement();

			parentDialog.enableOK((selectedObject instanceof TVSequence)
					|| (selectedObject instanceof TVTransaction));
		}
	}

	public Image getImage(Object element) {
		return null;
	}

	public String getText(Object element) {
		if (element instanceof TVObject) {
			return ((TVObject) element).name;
		}
		return null;
	}

}