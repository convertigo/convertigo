package com.twinsoft.convertigo.eclipse.views.references;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableStep;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.statements.ScHandlerStatement;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.beans.transactions.JavelinTransaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.editors.CompositeListener;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ScreenClassTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TransactionTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.UnloadedProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ViewContentProvider;
import com.twinsoft.convertigo.eclipse.views.references.model.AbstractNode;
import com.twinsoft.convertigo.eclipse.views.references.model.EntryHandlerFolder;
import com.twinsoft.convertigo.eclipse.views.references.model.ExitHandlerFolder;
import com.twinsoft.convertigo.eclipse.views.references.model.Folder;
import com.twinsoft.convertigo.eclipse.views.references.model.ProjectNode;
import com.twinsoft.convertigo.eclipse.views.references.model.RootNode;
import com.twinsoft.convertigo.eclipse.views.references.model.ScreenClassNode;
import com.twinsoft.convertigo.eclipse.views.references.model.SequenceNode;
import com.twinsoft.convertigo.eclipse.views.references.model.TransactionNode;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class ReferencesView extends ViewPart implements CompositeListener,
		ISelectionListener {

	private TreeViewer treeViewer;

	@Override
	public void objectSelected(CompositeEvent compositeEvent) {

	}

	@Override
	public void objectChanged(CompositeEvent compositeEvent) {

	}

	@Override
	public void createPartControl(Composite parent) {
		treeViewer = new TreeViewer(parent);
		treeViewer.setContentProvider(new ViewRefContentProvider());
		treeViewer.setLabelProvider(new ViewRefLabelProvider());
		treeViewer.setInput(null);
		treeViewer.expandAll();

		getSite().setSelectionProvider(treeViewer);
		getSite().getPage().addSelectionListener(this);
		
	}

	@Override
	public void setFocus() {

	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (part != ReferencesView.this	&& selection instanceof IStructuredSelection) {
			System.out.println(((IStructuredSelection) selection).toList());
			Object firstElement = ((IStructuredSelection) selection).getFirstElement();
			
			if (firstElement instanceof ScreenClassTreeObject) {
				selectionScreenClass(firstElement);
			} else if (firstElement instanceof TransactionTreeObject) {
				selectionTransaction(firstElement);
			} else if (firstElement instanceof ProjectTreeObject) {
				selectionProject(firstElement);
			} else {
				Folder root = new Folder(null, "root");
				root.addChild(new Folder(root, "This object is not referenced"));
				treeViewer.setInput(root);
			}
		}
		if (part == ReferencesView.this	&& selection instanceof IStructuredSelection) {
			System.out.println(((IStructuredSelection) selection).toList());
			Object firstElement = ((IStructuredSelection) selection).getFirstElement();
			selectionChangedEvent(firstElement);
		}
	}
	
	private void selectionProject (Object firstElement) {
		ProjectTreeObject projectTreeObjectSelected = (ProjectTreeObject) firstElement;
		Project projectSelected = projectTreeObjectSelected.getObject();
		String projectNameSelected = projectSelected.getName();
		
		try {
			Project project = null;
			List<String> projectNames = Engine.theApp.databaseObjectsManager.getAllProjectNamesList();
			ProjectExplorerView projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
			
			treeViewer.setInput(null);
			
			// Get the referencing sequence
			List<String> referencingSequence = new ArrayList<String>();
			List<String> referencingTransaction = new ArrayList<String>();
			List<Connector> connectors = projectSelected.getConnectorsList();
			List<Sequence> sequences = projectSelected.getSequencesList();
			
			RootNode root = new RootNode();
			ProjectNode projectNode = new ProjectNode(root, projectNameSelected, projectSelected);
			root.addChild(projectNode);
			EntryHandlerFolder entryHandlerFolder = new EntryHandlerFolder(root, "Reference objects used by the project", null);
			ExitHandlerFolder exitHandlerFolder = new ExitHandlerFolder(root, "Reference objects that use the project", null);
			
			
			for (Sequence sequence : sequences) {
				List<Step> steps = sequence.getSteps();
				for (Step step : steps) {
					if (step instanceof SequenceStep) {
						SequenceStep sequenceStep = (SequenceStep) step;
						String sourceSequence = sequenceStep.getSourceSequence();
						for (Sequence seq : sequences) {
							if (sourceSequence.startsWith(projectNameSelected)) {
								if (sourceSequence.endsWith(seq.getName())) {
									if (isNotExist(referencingSequence, seq.getName())) {
										referencingSequence.add(seq.getName());
										projectNode.addChild(new SequenceNode(projectNode, seq.getName(), seq));
									}
								}
							}
						}
					} else if (step instanceof TransactionStep) {
						TransactionStep transactionStep = (TransactionStep) step;
						String sourceTransaction = transactionStep.getSourceTransaction();
						for (Connector connector : connectors) {
							List<Transaction> transactionList = connector.getTransactionsList();
							for (Transaction transaction : transactionList) {
								if (sourceTransaction.startsWith(projectNameSelected)) {
									if (sourceTransaction.endsWith(transaction.getName())) {
										if (isNotExist(referencingTransaction, transaction.getName())) {
											referencingTransaction.add(transaction.getName());
											projectNode.addChild(new TransactionNode(projectNode, transaction.getName(), transaction));
										}
									}
								} 
							}
						}
					}
				}
			}
			
			
			for (String projectName : projectNames) {
				TreeObject projectTreeObject = ((ViewContentProvider) projectExplorerView.viewer
						.getContentProvider()).getProjectRootObject(projectName);
				if (projectTreeObject instanceof UnloadedProjectTreeObject) {
					project = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
				} else {
					project = projectExplorerView.getProject(projectName);
				}
				if (!(projectName.equals(projectNameSelected))) {
					List<Sequence> sequenceList = project.getSequencesList();
					List<Connector> connectorList = project.getConnectorsList();
					
					ProjectNode projectFolderExit = new ProjectNode(root, projectName, project);
					ProjectNode projectFolderEntry = new ProjectNode(root, projectName, project);
					referencingSequence.clear();
					
					for (Sequence sequence : sequenceList) {
						List<Step> stepList = sequence.getSteps();
						for (Step step : stepList) {
							if (step instanceof SequenceStep) {
								String sourceSequence = ((SequenceStep) step).getSourceSequence();
								if (sourceSequence.startsWith(projectNameSelected)) {
									for (Sequence seq : sequences) {
										String sequenceName = seq.getName();
										if (sourceSequence.endsWith(sequenceName)) {
											if (isNotExist(referencingSequence, sequence.getName())) {
												referencingSequence.add(sequence.getName());
												SequenceNode sequenceNode = new SequenceNode(root, sequence.getName(), sequence);
												sequenceNode.addChild(new SequenceNode(sequenceNode, sequenceName, seq));
												projectFolderExit.addChild(sequenceNode);
											}
										}
									}
								}
							} else if (step instanceof TransactionStep) {
								String sourceTransaction = ((TransactionStep)step).getSourceTransaction();
								if (sourceTransaction.startsWith(projectNameSelected)) {
									for (Connector connector : connectors) {
										List<Transaction> transactions = connector.getTransactionsList();
										for (Transaction transaction : transactions) {
											String transactionName = transaction.getName();
											if (sourceTransaction.endsWith(transactionName)) {
												if (isNotExist(referencingSequence, sequence.getName())) {
													referencingSequence.add(sequence.getName());
													SequenceNode sequenceNode = new SequenceNode(root, sequence.getName(), sequence);
													sequenceNode.addChild(new TransactionNode(sequenceNode, transactionName, transaction));
													projectFolderExit.addChild(sequenceNode);
													
												}
											}
										}
									}
								}
							}
						}

						String sequenceName = sequence.getName();
						for (Sequence seq : sequences) {
							List<Step> steps = seq.getSteps();
							for (Step step_seq : steps) {
								if (step_seq instanceof SequenceStep) {
									String sourceSequence = ((SequenceStep) step_seq).getSourceSequence();
									if (sourceSequence.startsWith(projectName)) {
										if (sourceSequence.endsWith(sequenceName)) {
											if (isNotExist(referencingSequence, sequenceName)) {
												referencingSequence.add(sequenceName);
												projectFolderEntry.addChild(new SequenceNode(projectFolderEntry, sequenceName, sequence));
											}
										}
									}
								}
							}
						}
					}
					
					for (Connector connector : connectorList) {
						List<Transaction> transactionList = connector.getTransactionsList();
						for (Transaction transaction : transactionList) {
							String TransactionName = transaction.getName();
							for (Sequence sequence : sequences) {
								List<Step> stepList = sequence.getSteps();
								for (Step step : stepList) {
									if (step instanceof TransactionStep) {
										String sourceTransaction = ((TransactionStep)step).getSourceTransaction();
										if (sourceTransaction.startsWith(projectName)) {
											if (sourceTransaction.endsWith(TransactionName)) {
												if (isNotExist(referencingTransaction, TransactionName)) {
													referencingTransaction.add(TransactionName);
													projectFolderEntry.addChild(new TransactionNode(projectFolderEntry, TransactionName, transaction));
												}
											}
										}
									}
								}
							}
						}
					}
					if (projectFolderEntry.hasChildren()){
						entryHandlerFolder.addChild(projectFolderEntry);
					}
					if (projectFolderExit.hasChildren()){
						exitHandlerFolder.addChild(projectFolderExit);
					}
				}
			}
			if (entryHandlerFolder.hasChildren()){
				projectNode.addChild(entryHandlerFolder);
			}
			
			if (exitHandlerFolder.hasChildren()){
				projectNode.addChild(exitHandlerFolder);
			}	
			
			if (!projectNode.hasChildren()) {
				projectNode.addChild(new Folder(projectNode, "This project is not used in any object"));
			}
			
			treeViewer.setInput(null);
			treeViewer.setInput(root);
			treeViewer.expandAll();
			
		} catch (EngineException e) {
			ConvertigoPlugin.logException(e, "Error while analyzing the projects hierarchy", true);
		}
	}
	
	private void selectionScreenClass(Object firstElement) {
		
		ScreenClassTreeObject screenClassTreeObject = (ScreenClassTreeObject) firstElement;
		ScreenClass screenClass = screenClassTreeObject.getObject();
		String screenClassName = screenClassTreeObject.getName();
		
		// Get the referencing transactions
		List<String> referencingTransactions = new ArrayList<String>();
		Connector connector = screenClass.getConnector();
		List<Transaction> transactions = connector.getTransactionsList();
		
		RootNode root = new RootNode();
		
		ScreenClassNode screenClassFolder = new ScreenClassNode(root, screenClassName, screenClass);
		root.addChild(screenClassFolder);
		EntryHandlerFolder entryFolder = new EntryHandlerFolder(root, "Handlers ScreenClass entry", null);
		ExitHandlerFolder exitFolder = new ExitHandlerFolder(root, "Handlers ScreenClass Exit", null);
		
		if (connector instanceof HtmlConnector) {
			for (Transaction transaction : transactions) {
				HtmlTransaction htmlTransaction = (HtmlTransaction) transaction;
				List<Statement> statements = htmlTransaction.getStatements();
				for (Statement statement : statements) {
					if (statement instanceof ScHandlerStatement) {
						ScHandlerStatement scHandlerStatement = (ScHandlerStatement) statement;
						if (scHandlerStatement.getNormalizedScreenClassName().equals(screenClassName)) {
							if (scHandlerStatement.getName().endsWith("Entry")) {
								entryFolder.addChild(new TransactionNode(entryFolder, transaction.getName(), scHandlerStatement));
							} else {
								exitFolder.addChild(new TransactionNode(entryFolder, transaction.getName(), scHandlerStatement));
							}
						}
					}
				}
			}
			if (entryFolder.hasChildren()) {
				screenClassFolder.addChild(entryFolder);
			}
			if (exitFolder.hasChildren()) {
				screenClassFolder.addChild(exitFolder);
			}
		} else if (connector instanceof JavelinConnector) {
			for (Transaction transaction : transactions) {
				JavelinTransaction javelinTransaction = (JavelinTransaction) transaction; 
				String blocks = javelinTransaction.handlers;
				Matcher matcher = Pattern.compile(screenClassName).matcher(blocks);
				if (matcher.find()) {
					referencingTransactions.add(transaction.getName());
				}
			}
		}
		
		if (!screenClassFolder.hasChildren()){
			screenClassFolder.addChild(new Folder(screenClassFolder, "This screen class is not used in any transaction"));
		}
		// Build the treeviewer model				
		treeViewer.setInput(null);
		treeViewer.setInput(root);
		treeViewer.expandAll();

	}
	
	private void selectionTransaction(Object firstElement) {
		
		TransactionTreeObject transactionTreeObject = (TransactionTreeObject) firstElement;
		Transaction transaction = transactionTreeObject.getObject();
		String transactionName = transactionTreeObject.getName();
		
		// Get the referencing sequence
		List<String> referencingSequence = new ArrayList<String>();

		String transactionProjectName = transaction.getProject().getName();
		String transactionConnectorName = transaction.getParent().getName();
		
		try {
			Project project = null;
			List<String> projectNames = Engine.theApp.databaseObjectsManager.getAllProjectNamesList();
			ProjectExplorerView projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
			
			treeViewer.setInput(null);
			
			RootNode root = new RootNode();
			
			TransactionNode transactionFolder = new TransactionNode(root, transactionName, transaction);
			root.addChild(transactionFolder);
			ProjectNode projectFolder = null;
			
			for (String projectName : projectNames) {
				TreeObject projectTreeObject = ((ViewContentProvider) projectExplorerView.viewer
						.getContentProvider()).getProjectRootObject(projectName);
				if (projectTreeObject instanceof UnloadedProjectTreeObject) {
					project = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
				} else {
					project = projectExplorerView.getProject(projectName);
				}

				projectFolder = new ProjectNode(transactionFolder, project.getName(), project);
				List<Sequence> sequences = project.getSequencesList();
				referencingSequence.clear();
				
				for (Sequence sequence : sequences) {
					List<Step> stepList = sequence.getAllSteps();
					for (Step step : stepList) {
						if (step instanceof TransactionStep) {
							String sourceTransactionName = ((TransactionStep) step).getSourceTransaction();
							if (sourceTransactionName.equals(transactionProjectName + RequestableStep.SOURCE_SEPARATOR +
									transactionConnectorName + RequestableStep.SOURCE_SEPARATOR +
									transactionName)) {
								if (isNotExist(referencingSequence, sequence.getName())) {
									referencingSequence.add(sequence.getName());
									projectFolder.addChild(new SequenceNode(projectFolder, sequence.getName(), sequence));
								}
							}
						}
					}
				}
				if (projectFolder.hasChildren()) {
					transactionFolder.addChild(projectFolder);
				} 
			}
			
			
			if (!transactionFolder.hasChildren()) {
				transactionFolder.addChild(new Folder(projectFolder, "This transaction is not used in any sequence"));
			}

			treeViewer.setInput(root);
			treeViewer.expandAll();
				
		} catch (EngineException e) {
			ConvertigoPlugin.logException(e, "Error while analyzing the projects hierarchy", true);
		}
	}
	
	//Fonction qui permet de vérifier si un élément exist déja dans une liste
	public boolean isNotExist(List<String> list, String source) {
		int tmp = -1;
		for (String var : list) {
			if (var.equals(source)) {
				tmp = 1;
			}
		}
		if (tmp < 0) return true;
		return false;
	}

	private void selectionChangedEvent(Object firstElement) {
		if (firstElement != null) {
			if (firstElement instanceof AbstractNode) {
				AbstractNode abstractNode = (AbstractNode) firstElement;
				DatabaseObject selectedDatabaseObject = abstractNode.getRefDatabaseObject();
				ProjectExplorerView projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
				TreeObject selectedTreeObject = projectExplorerView.findTreeObjectByUserObject(selectedDatabaseObject);
				if (selectedTreeObject != null) {
					projectExplorerView.setSelectedTreeObject(selectedTreeObject);
				} 
//				else {
//					try {
//						Project project = selectedDatabaseObject.getProject();
//						UnloadedProjectTreeObject projectTreeObject = (UnloadedProjectTreeObject) ((ViewContentProvider) projectExplorerView.viewer
//								.getContentProvider()).getProjectRootObject(project.getName());
//						projectExplorerView.loadProject(projectTreeObject);
//						ConvertigoPlugin.infoMessageBox("This project is close.\n ");
//						TreeObject selectedTreeObj = projectExplorerView.findTreeObjectByUserObject(selectedDatabaseObject);
//						if (selectedTreeObj != null) {
//							projectExplorerView.setSelectedTreeObject(selectedTreeObject);
//						}
//					} catch (EngineException e) {
//						ConvertigoPlugin.logException(e, "Error while analyzing the projects hierarchy", true);
//					}
//					
//				}
			}
		}
	}
	
}
