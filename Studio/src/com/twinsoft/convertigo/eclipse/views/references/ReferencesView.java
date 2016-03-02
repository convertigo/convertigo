package com.twinsoft.convertigo.eclipse.views.references;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

import com.twinsoft.convertigo.beans.connectors.CicsConnector;
import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.connectors.ProxyHttpConnector;
import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector;
import com.twinsoft.convertigo.beans.connectors.SqlConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableStep;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.screenclasses.JavelinScreenClass;
import com.twinsoft.convertigo.beans.statements.ContinueWithSiteClipperStatement;
import com.twinsoft.convertigo.beans.statements.FunctionStatement;
import com.twinsoft.convertigo.beans.statements.ScHandlerStatement;
import com.twinsoft.convertigo.beans.steps.BlockStep;
import com.twinsoft.convertigo.beans.steps.BranchStep;
import com.twinsoft.convertigo.beans.steps.ElseStep;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.ThenStep;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.beans.steps.XMLComplexStep;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.beans.transactions.JavelinTransaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.editors.CompositeListener;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ViewContentProvider;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ConnectorTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ScreenClassTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.SequenceTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.StepTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TransactionTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UnloadedProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.references.model.AbstractNodeWithDatabaseObjectReference;
import com.twinsoft.convertigo.eclipse.views.references.model.AbstractParentNode;
import com.twinsoft.convertigo.eclipse.views.references.model.CicsConnectorNode;
import com.twinsoft.convertigo.eclipse.views.references.model.ConnectorNode;
import com.twinsoft.convertigo.eclipse.views.references.model.EntryHandlerNode;
import com.twinsoft.convertigo.eclipse.views.references.model.ExitHandlerNode;
import com.twinsoft.convertigo.eclipse.views.references.model.HtmlConnectorNode;
import com.twinsoft.convertigo.eclipse.views.references.model.HttpConnectorNode;
import com.twinsoft.convertigo.eclipse.views.references.model.InformationNode;
import com.twinsoft.convertigo.eclipse.views.references.model.IsUsedByNode;
import com.twinsoft.convertigo.eclipse.views.references.model.JavelinConnectorNode;
import com.twinsoft.convertigo.eclipse.views.references.model.ProjectNode;
import com.twinsoft.convertigo.eclipse.views.references.model.ProxyHttpConnectorNode;
import com.twinsoft.convertigo.eclipse.views.references.model.RequiresNode;
import com.twinsoft.convertigo.eclipse.views.references.model.RootNode;
import com.twinsoft.convertigo.eclipse.views.references.model.ScreenClassNode;
import com.twinsoft.convertigo.eclipse.views.references.model.SequenceNode;
import com.twinsoft.convertigo.eclipse.views.references.model.SequenceStepNode;
import com.twinsoft.convertigo.eclipse.views.references.model.SiteClipperConnectorNode;
import com.twinsoft.convertigo.eclipse.views.references.model.SqlConnectorNode;
import com.twinsoft.convertigo.eclipse.views.references.model.TransactionNode;
import com.twinsoft.convertigo.eclipse.views.references.model.TransactionStepNode;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class ReferencesView extends ViewPart implements CompositeListener,
		ISelectionListener {

	private TreeViewer treeViewer;

	public void objectSelected(CompositeEvent compositeEvent) {

	}

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
	public void dispose() {
		getSite().getPage().removeSelectionListener(this);
		super.dispose();
	}

	@Override
	public void setFocus() {

	}

	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			if (part instanceof ProjectExplorerView) {
				Object firstElement = ((IStructuredSelection) selection).getFirstElement();
				
				if (firstElement instanceof ScreenClassTreeObject) {
					handleScreenClassSelection(firstElement);
				} else if (firstElement instanceof TransactionTreeObject) {
					handleTransactionSelection(firstElement);
				} else if (firstElement instanceof ProjectTreeObject) {
					handleProjectSelection(firstElement);
				} else if (firstElement instanceof UnloadedProjectTreeObject) {
					handleProjectSelection(firstElement);
				} else if (firstElement instanceof SequenceTreeObject) {
					handleSequenceSelection(firstElement);
				} else if (firstElement instanceof ConnectorTreeObject) {
					handleConnectorSelection(firstElement);
				} else if (firstElement instanceof StepTreeObject) {
					handleCallStepselection(firstElement);
				} else {
					InformationNode root = new InformationNode(null, "root");
					root.addChild(new InformationNode(root, "References are not handled for this object"));
					treeViewer.setInput(root);
				}
			}
			else if (part == ReferencesView.this) {
				Object firstElement = ((IStructuredSelection) selection).getFirstElement();
				handleSelectedObjectInRefView(firstElement);
			}
		}
	}
	
	private void handleProjectSelection(Object firstElement) {
		List<String> projectNames = Engine.theApp.databaseObjectsManager.getAllProjectNamesList();
		ProjectExplorerView projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
		
		Project projectSelected = null;
		ProjectTreeObject projectTreeObjectSelected = null;
		UnloadedProjectTreeObject unloadedProjectTreeObjectSelected = null;
		
		if (firstElement instanceof ProjectTreeObject) {
			projectTreeObjectSelected = (ProjectTreeObject) firstElement;
			projectSelected = projectTreeObjectSelected.getObject();
		} else if (firstElement instanceof UnloadedProjectTreeObject) {
			unloadedProjectTreeObjectSelected = (UnloadedProjectTreeObject) firstElement;
			String projectNameSelected = unloadedProjectTreeObjectSelected.getName();
			projectSelected = getProject(projectNameSelected, projectExplorerView);
		}
		
		String projectNameSelected = projectSelected.getName();
		
		treeViewer.setInput(null);
		
		// Get the referencing sequences and transactions
		List<Sequence> sequences = projectSelected.getSequencesList();
		
		RootNode root = new RootNode();
		ProjectNode projectNode = new ProjectNode(root, projectNameSelected, projectSelected);
		root.addChild(projectNode);
		
		// Get all the projects needed to successfully execute the selected project
		// i.e. get all CallTransaction and CallSequence steps from the selected project
		// that refer to other projects
		RequiresNode requiresNode = new RequiresNode(root, "Requires");
		
		// Search for external sequence or transaction referenced by CallSequence or CallTransaction
		// from the selected project
		List<String> transactionList = new ArrayList<String>();
		List<String> sequenceList = new ArrayList<String>();
		for (Sequence sequence : sequences) {
			List<Step> steps = sequence.getSteps();
			for (Step step : steps) {
				getRequiredRequestables(step, projectSelected, projectExplorerView, requiresNode, transactionList, sequenceList);
			}
		}
		
		if (requiresNode.hasChildren()){
			projectNode.addChild(requiresNode);
		}
		else {
			projectNode.addChild(new InformationNode(projectNode, "This project does not require any other project"));
		}
		
		// Get all the projects using the selected project
		// i.e. get all CallTransaction and CallSequence steps that refer to transactions
		// or sequences from the selected project
		IsUsedByNode isUsedByNode = new IsUsedByNode(root, "Is used by");
		
		for (String projectName : projectNames) {
			if (!(projectName.equals(projectNameSelected))) {
				Project project = getProject(projectName, projectExplorerView);
				if (project == null) {
					// Unable to load the project, just ignore it
					ConvertigoPlugin.logWarning(
							"[References View] Unable to load the project \"" + projectName + "\"", false);
					continue;
				}

				ProjectNode projectFolderExports = new ProjectNode(root,
						projectName, project);

				List<Sequence> sequencesList = project.getSequencesList();
				for (Sequence sequence : sequencesList) {
					// Search for CallTransaction and CallSequence
					// referencing a transaction or sequence
					// from the selected project
					List<Step> stepList = sequence.getSteps();
					SequenceNode sequenceNode = new SequenceNode(root,
							sequence.getName(), sequence);
					for (Step step : stepList) {
						getUsedRequestables(step,
								projectSelected, sequenceNode);
					}
					if (sequenceNode.hasChildren()) {
						projectFolderExports.addChild(sequenceNode);
					}
				}

				if (projectFolderExports.hasChildren()) {
					isUsedByNode.addChild(projectFolderExports);
				}
			}
		}
		if (isUsedByNode.hasChildren()) {
			projectNode.addChild(isUsedByNode);
		} else {
			projectNode.addChild(new InformationNode(projectNode,
					"This project is not used by any other project"));
		}

		treeViewer.setInput(null);
		treeViewer.setInput(root);
		treeViewer.expandAll();
	}
	
	private void handleScreenClassSelection(Object firstElement) {
		
		ScreenClassTreeObject screenClassTreeObject = (ScreenClassTreeObject) firstElement;
		ScreenClass screenClass = screenClassTreeObject.getObject();
		String screenClassName = screenClassTreeObject.getName();
		
		// Get the referencing transactions
		Connector connector = screenClass.getConnector();
		List<Transaction> transactions = connector.getTransactionsList();
		
		RootNode root = new RootNode();
		
		ScreenClassNode screenClassFolder = new ScreenClassNode(root, screenClassName, screenClass);
		root.addChild(screenClassFolder);
		
		IsUsedByNode isUsedByNode = new IsUsedByNode(screenClassFolder, "Is used by");

		EntryHandlerNode entryFolder = new EntryHandlerNode(isUsedByNode, "Transaction entry handlers", null);
		ExitHandlerNode exitFolder = new ExitHandlerNode(isUsedByNode, "Transaction exit handlers", null);
		
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
								exitFolder.addChild(new TransactionNode(exitFolder, transaction.getName(), scHandlerStatement));
							}
						}
					}
				}
			}
		} else if (connector instanceof JavelinConnector) {
			for (Transaction transaction : transactions) {
				JavelinTransaction javelinTransaction = (JavelinTransaction) transaction; 
				if (javelinTransaction.handlers.indexOf("function on" + screenClassName + "Entry()") != -1) {
					entryFolder.addChild(new TransactionNode(entryFolder, transaction.getName(), transaction));
				}
				if (javelinTransaction.handlers.indexOf("function on" + screenClassName + "Exit()") != -1) {
					exitFolder.addChild(new TransactionNode(exitFolder, transaction.getName(), transaction));
				}
			}
		}
		
		if (entryFolder.hasChildren()) {
			isUsedByNode.addChild(entryFolder);
		}
		if (exitFolder.hasChildren()) {
			isUsedByNode.addChild(exitFolder);
		}
		
		if (!isUsedByNode.hasChildren()){
			screenClassFolder.addChild(new InformationNode(screenClassFolder, "This screen class is not used in any transaction"));
		} else {
			screenClassFolder.addChild(isUsedByNode);
		}
		
		// Build the treeviewer model				
		treeViewer.setInput(null);
		treeViewer.setInput(root);
		treeViewer.expandAll();

	}
	
	private void handleTransactionSelection(Object firstElement) {
		
		TransactionTreeObject transactionTreeObject = (TransactionTreeObject) firstElement;
		Transaction transaction = transactionTreeObject.getObject();
		String transactionName = transactionTreeObject.getName();
		
		// Get the referencing sequence steps
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
			
			IsUsedByNode isUsedByNode = new IsUsedByNode(transactionFolder, "Is used by");
			RequiresNode requiresNode = new RequiresNode(transactionFolder, "Requires");
			
			ProjectNode projectFolder = null;
			
			//Searching all objects are required transaction selected
			Connector connector = transaction.getConnector();
			if (connector instanceof HtmlConnector) {
				Project proj = ((HtmlConnector)connector).getProject();
				ProjectNode projectNode = new ProjectNode(requiresNode, transactionProjectName, proj);
				
				HtmlTransaction htmlTransaction = (HtmlTransaction) transaction;
				List<Statement> statements = htmlTransaction.getStatements();
				List<ScreenClass> screenClassList = new ArrayList<ScreenClass>();
				List<String> siteClipperConnectorNames = new ArrayList<String>();
				for (Statement statement : statements) {
					if (statement instanceof ScHandlerStatement) {
						ScHandlerStatement scHandlerStatement = (ScHandlerStatement) statement;
						String screenClassName = scHandlerStatement.getNormalizedScreenClassName();
						ScreenClass screenClass = ((HtmlConnector)connector).getScreenClassByName(screenClassName);

						if (screenClass != null) {
							if (!screenClassList.contains(screenClass)) {
								screenClassList.add(screenClass);
								requiresNode.addChild(new ScreenClassNode(requiresNode, screenClassName, screenClass));
							}
						}
					}
					
					List<Statement> statementList = ((FunctionStatement) statement).getStatements();
					for (Statement st : statementList) {
						if (st instanceof ContinueWithSiteClipperStatement) {
							ContinueWithSiteClipperStatement continueWithSiteClipperStatement = (ContinueWithSiteClipperStatement) st;
							String siteClipperconnectorName = continueWithSiteClipperStatement.getSiteClipperConnectorName();
							
							if (!siteClipperConnectorNames.contains(siteClipperconnectorName)) {
								siteClipperConnectorNames.add(siteClipperconnectorName);
								Connector siteClipperConnector = proj.getConnectorByName(siteClipperconnectorName);
								ConnectorNode connectorSiteClipperNode = new SiteClipperConnectorNode(projectNode, siteClipperconnectorName, siteClipperConnector);
								projectNode.addChild(connectorSiteClipperNode);
							}
						}
					}
				}
				if(projectNode.hasChildren()) {
					requiresNode.addChild(projectNode);
				}
			} else if (connector instanceof JavelinConnector) {
				
				JavelinTransaction javelinTransaction = (JavelinTransaction) transaction;
				String handlers = javelinTransaction.handlers;

				List<JavelinScreenClass> screenClasses = ((JavelinConnector)connector).getAllScreenClasses();
				List<JavelinScreenClass> screenClassList = new ArrayList<JavelinScreenClass>();
				for (JavelinScreenClass screenClass : screenClasses) {
					if (handlers.indexOf("function on" + screenClass.getName()) != -1) {
						if (!screenClassList.contains(screenClass)) {
							screenClassList.add(screenClass);
							requiresNode.addChild(new ScreenClassNode(requiresNode, screenClass.getName(), screenClass));
						}
					}
				}
			}
			
			
			//Searching all objects are used transaction selected 
			for (String projectName : projectNames) {
				project = getProject(projectName, projectExplorerView);

				projectFolder = new ProjectNode(isUsedByNode, project.getName(), project);
				List<Sequence> sequences = project.getSequencesList();
				
				for (Sequence sequence : sequences) {
					List<Step> stepList = sequence.getAllSteps();
					SequenceNode sequenceNode = new SequenceNode(projectFolder, sequence.getName(), sequence);
					for (Step step : stepList) {
						getTransactionReferencing (step, projectExplorerView, sequenceNode, transactionProjectName, transactionConnectorName, transactionName);						
					}
					if (sequenceNode.hasChildren()) {
						projectFolder.addChild(sequenceNode);
					}
				}
				if (projectFolder.hasChildren()) {
					isUsedByNode.addChild(projectFolder);
				} 
			}
			if (requiresNode.hasChildren()) {
				transactionFolder.addChild(requiresNode);
			}
			if (isUsedByNode.hasChildren()) {
				transactionFolder.addChild(isUsedByNode);
			}
			if (!transactionFolder.hasChildren()) {
				transactionFolder.addChild(new InformationNode(projectFolder, "This transaction is not used in any sequence"));
			}
			
			
			treeViewer.setInput(root);
			treeViewer.expandAll();
				
		} catch (EngineException e) {
			ConvertigoPlugin.logException(e, "Error while analyzing the projects hierarchy", true);
		}
	}

	private void handleSequenceSelection(Object firstElement) {
		SequenceTreeObject sequenceTreeObject = (SequenceTreeObject) firstElement;
		Sequence sequenceSelected = sequenceTreeObject.getObject();
		String sequenceSelectedName = sequenceSelected.getName();
		
//		String sequenceProjectName = sequenceSelected.getProject().getName();
		
		List<String> projectNames = Engine.theApp.databaseObjectsManager.getAllProjectNamesList();
		ProjectExplorerView projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
		
		treeViewer.setInput(null);
		
		// Get the referencing sequence steps
		List<String> referencingSequence = new ArrayList<String>();
		
		RootNode root = new RootNode();
		
		SequenceNode sequenceFolder = new SequenceNode(root, sequenceSelectedName, sequenceSelected);
		root.addChild(sequenceFolder);
		
		IsUsedByNode isUsedByNode = new IsUsedByNode(sequenceFolder, "Is used by");
		
		// Searching all objects that reference the selected sequence
		for (String projectName : projectNames) {
			Project project = getProject(projectName, projectExplorerView);
			
			ProjectNode projectFolder = null;
			projectFolder = new ProjectNode(isUsedByNode, project.getName(), project);
			List<Sequence> sequences = project.getSequencesList();
			referencingSequence.clear();
			
			for (Sequence sequence : sequences) {
				List<Step> steps = sequence.getSteps();
				
				for (Step step : steps) {
					SequenceNode sequenceNode = new SequenceNode(projectFolder, sequence.getName(), sequence);
					getSequenceReferencingIsUsedBy(step, sequenceSelected, sequenceNode);
					if (sequenceNode.hasChildren()) {
						projectFolder.addChild(sequenceNode);
					}
				}
			}
			if (projectFolder.hasChildren()) {
				isUsedByNode.addChild(projectFolder);
			}
		}
		
		List<Step> steps = sequenceSelected.getSteps();
		
		RequiresNode requiresNode = new RequiresNode(root, "Requires");			
		
		// Searching all objects that are referenced by the selected sequence
		List<String> transactionList = new ArrayList<String>();
		List<String> sequenceList = new ArrayList<String>();
		
		for (Step step : steps) {
			getSequenceReferencingRequires(step, sequenceSelected, projectExplorerView, requiresNode, transactionList, sequenceList);
		}
		
		if (requiresNode.hasChildren()) {
			sequenceFolder.addChild(requiresNode);
		}
		if (isUsedByNode.hasChildren()) {
			sequenceFolder.addChild(isUsedByNode);
		}
		if (!sequenceFolder.hasChildren()) {
			sequenceFolder.addChild(new InformationNode(sequenceFolder, "This sequence is not used in any sequence"));
		}

		treeViewer.setInput(root);
		treeViewer.expandAll();
	}

	private void handleConnectorSelection(Object firstElement) {
		ConnectorTreeObject connectorTreeObject = (ConnectorTreeObject) firstElement;
		Connector connectorSelected = connectorTreeObject.getObject();
		String connectorSelectedName = connectorSelected.getName();
		
		Project projectConnectorSelected = connectorSelected.getProject();
		String connectorProjectName = connectorSelected.getProject().getName();
		List<Transaction> transactions = connectorSelected.getTransactionsList();
		
		try {
			Project project = null;
			List<String> projectNames = Engine.theApp.databaseObjectsManager.getAllProjectNamesList();
			ProjectExplorerView projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
			
			treeViewer.setInput(null);
			
			RootNode root = new RootNode();
			ConnectorNode connectorNode = null;
			connectorNode = getConnectorNode(root, connectorSelected);
		
			root.addChild(connectorNode);
			
			IsUsedByNode isUsedByNode = new IsUsedByNode(connectorNode, "Is used by");
			RequiresNode requiresNode = new RequiresNode(connectorNode, "Requires");
			ProjectNode projectFolder = null;
			
			//Search handlers are that referenced by the selected connector for continue with site Clipper
			if (connectorSelected instanceof HtmlConnector) {
				ProjectNode projectNode = new ProjectNode(requiresNode, connectorProjectName, projectConnectorSelected);
				for (Transaction transaction : transactions) {
					List<Statement> statements = ((HtmlTransaction) transaction).getStatements();
					List<String> siteClipperConnectorNames = new ArrayList<String>();
					for (Statement statement : statements) {
						List<Statement> statementList = ((FunctionStatement) statement).getStatements();
						for (Statement st : statementList) {
							if (st instanceof ContinueWithSiteClipperStatement) {
								ContinueWithSiteClipperStatement continueWithSiteClipperStatement = (ContinueWithSiteClipperStatement) st;
								String siteClipperconnectorName = continueWithSiteClipperStatement.getSiteClipperConnectorName();
								
								if (!siteClipperConnectorNames.contains(siteClipperconnectorName)) {
									siteClipperConnectorNames.add(siteClipperconnectorName);
									Connector siteClipperConnector = projectConnectorSelected.getConnectorByName(siteClipperconnectorName);							
									ConnectorNode connectorSiteClipperNode = new SiteClipperConnectorNode(projectNode, siteClipperconnectorName, siteClipperConnector);
									projectNode.addChild(connectorSiteClipperNode);
								}
							}
						}
					}
				}
				if(projectNode.hasChildren()) {
					requiresNode.addChild(projectNode);
				}
			} else if (connectorSelected instanceof SiteClipperConnector) {
				List<Connector> connectors = projectConnectorSelected.getConnectorsList();
				ProjectNode projectNode = new ProjectNode(isUsedByNode, connectorProjectName, projectConnectorSelected);
				
				for (Connector connector : connectors) {
					if (connector instanceof HtmlConnector) {
						List<Transaction> transactionList = ((HtmlConnector)connector).getTransactionsList();
						for (Transaction transaction : transactionList) {
							List<Statement> statements = ((HtmlTransaction)transaction).getStatements();
							for (Statement statement : statements) {			
								List<Statement> statementList = ((FunctionStatement)statement).getStatements();
								for (Statement st : statementList) {
									if (st instanceof ContinueWithSiteClipperStatement) {
										String sourceSiteClipperConnectorName = ((ContinueWithSiteClipperStatement)st).getSiteClipperConnectorName();
										if (sourceSiteClipperConnectorName.equals(connectorSelectedName)) {
											ContinueWithSiteClipperStatement continueWithSiteClipperStatement = (ContinueWithSiteClipperStatement) st;
										
											HtmlConnectorNode htmlConnectorNode = new HtmlConnectorNode(projectNode, connector.getName(), connector);
											projectNode.addChild(htmlConnectorNode);
											TransactionNode transactionNode = new TransactionNode(htmlConnectorNode, transaction.getName(), continueWithSiteClipperStatement);
											htmlConnectorNode.addChild(transactionNode);
										}
									}
								}
							}
						}
					}
				}
				if (projectNode.hasChildren()) {
					isUsedByNode.addChild(projectNode);
				}
			}
			
			// Searching all objects that are referenced by the selected connector
			for (String projectName : projectNames) {
				project = getProject(projectName, projectExplorerView);
				
				
				projectFolder = new ProjectNode(isUsedByNode, projectName, project);
				List<Sequence> sequences = project.getSequencesList();
				
				for (Sequence sequence : sequences) {
					List<Step> steps = sequence.getSteps();
					SequenceNode sequenceNode = new SequenceNode(projectFolder, sequence.getName(), sequence);
					for (Step step : steps) {
							getConnectorReferencingIsUsedBy(step, projectExplorerView, sequenceNode, transactions, connectorProjectName, connectorSelectedName);
					}
					if (sequenceNode.hasChildren()) {
						projectFolder.addChild(sequenceNode);
					}
				}
				if (projectFolder.hasChildren()) {
					isUsedByNode.addChild(projectFolder);
				}
			}
			if (requiresNode.hasChildren()) {
				connectorNode.addChild(requiresNode);
			}
			if (isUsedByNode.hasChildren()) {
				connectorNode.addChild(isUsedByNode);
			}
			if (!connectorNode.hasChildren()) {
				connectorNode.addChild(new InformationNode(connectorNode, "This connector is not used by any other objects"));
			}
			

			treeViewer.setInput(root);
			treeViewer.expandAll();
			
		} catch (EngineException e) {
			ConvertigoPlugin.logException(e, "Error while analyzing the projects hierarchy", true);
		}
	}
	
	private void handleCallStepselection(Object firstElement) {
		try {
			ProjectExplorerView projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
			StepTreeObject stepTreeObject = (StepTreeObject) firstElement;
			Step step = stepTreeObject.getObject();
			RootNode root = new RootNode();
			if (step instanceof TransactionStep) {
				TransactionStep transactionStep = (TransactionStep) step;
				String transactionStepName = transactionStep.getName();
				TransactionStepNode transactionStepNode = new TransactionStepNode(root, transactionStepName, transactionStep);
				RequiresNode requiresNode = new RequiresNode(transactionStepNode,  "Requires");
				
				String transactionName = transactionStep.getTransactionName();
				String connectorName = transactionStep.getConnectorName();
				String projectName = transactionStep.getProjectName();
				
				Project project = getProject(projectName, projectExplorerView);
				ProjectNode projectNode = new ProjectNode(requiresNode,projectName, project);
				
				Connector connector = null;
				Transaction transaction = null;
				try {
					if (project != null) {
						connector =  project.getConnectorByName(connectorName);
						if (connector != null) {
							transaction = connector.getTransactionByName(transactionName);
						}
					}
				} catch (EngineException e) {
					connector = null;
					transaction = null;
				}
				
				ConnectorNode connectorNode = getConnectorNode(projectNode, connector);
				if (connectorNode == null)
					connectorNode = new ConnectorNode(projectNode, connectorName, connector);
				projectNode.addChild(connectorNode);
				
				TransactionNode transactionNode = new TransactionNode(projectNode, transactionName, transaction);
				connectorNode.addChild(transactionNode);
				
				requiresNode.addChild(projectNode);
				transactionStepNode.addChild(requiresNode);		
				root.addChild(transactionStepNode);
				
			} else if (step instanceof SequenceStep) {
				SequenceStep sequenceStep = (SequenceStep) step;
				String sequenceStepName = sequenceStep.getName();
				SequenceStepNode sequenceStepNode = new SequenceStepNode(root, sequenceStepName, sequenceStep);
				RequiresNode requiresNode = new RequiresNode(sequenceStepNode,  "Requires");
				
				String sequenceName = sequenceStep.getSequenceName();
				String projectName = sequenceStep.getProjectName();
				
				Project project = getProject(projectName, projectExplorerView);
				ProjectNode projectNode = new ProjectNode(requiresNode, projectName, project);
				
				Sequence sequence = null;
				try {
					if (project != null)
						sequence = project.getSequenceByName(sequenceName);
				} catch (EngineException e) {
					sequence = null;
				}
				
				projectNode.addChild(new SequenceNode(projectNode, sequenceName, sequence));
				requiresNode.addChild(projectNode);
				sequenceStepNode.addChild(requiresNode);
				root.addChild(sequenceStepNode);
			} else {
				root.addChild(new InformationNode(root, "References are not handled for this object"));
				treeViewer.setInput(root);
			}
			
			treeViewer.setInput(root);
			treeViewer.expandAll();
		
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Error while analyzing the projects hierarchy", true);
		}
	}

	private ConnectorNode getConnectorNode (AbstractParentNode root, Connector connector) {
		ConnectorNode connectorNode = null;
		if (connector != null) {
			String connectorName = connector.getName();
			
			if (connector instanceof HtmlConnector) {
				connectorNode = new HtmlConnectorNode(root, connectorName, connector);
			} else if (connector instanceof ProxyHttpConnector) {
				connectorNode = new ProxyHttpConnectorNode(root, connectorName, connector);
			} else if (connector instanceof JavelinConnector) {
				connectorNode = new JavelinConnectorNode(root, connectorName, connector);
			} else if (connector instanceof HttpConnector) {
				connectorNode = new HttpConnectorNode(root, connectorName, connector);
			} else if (connector instanceof SiteClipperConnector) {
				connectorNode = new SiteClipperConnectorNode(root, connectorName, connector);
			} else if (connector instanceof SqlConnector) {
				connectorNode = new SqlConnectorNode(root, connectorName, connector);
			} else if (connector instanceof CicsConnector) {
				connectorNode = new CicsConnectorNode(root, connectorName, connector);
			} else {
				connectorNode = new ConnectorNode(root, connectorName, connector);
			}
		}
		return connectorNode;
	}
	
	//Function to retrieve the object "project" whose name is passed as a parameter
	private Project getProject (String projectName, ProjectExplorerView projectExplorerView) {
		Project project;
		try {
			TreeObject projectTreeObject = ((ViewContentProvider) projectExplorerView.viewer
					.getContentProvider()).getProjectRootObject(projectName);
			if (projectTreeObject instanceof UnloadedProjectTreeObject) {
				project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName, false);
			} else {
				project = projectExplorerView.getProject(projectName);
			}
		} catch (EngineException e) {
			project = null;
		}
		
		return project;
	}
	
	private List<Step> getStepList (Step step) {
		List<Step> steps = null;
		
		if (step instanceof BlockStep) {
			steps = ((BlockStep) step).getAllSteps();
		} else if (step instanceof BranchStep) {
			steps = ((BranchStep) step).getAllSteps();
		} else if (step instanceof ThenStep) {
			steps = ((ThenStep) step).getAllSteps();
		} else if (step instanceof ElseStep) {
			steps = ((ElseStep) step).getAllSteps();
		} else if (step instanceof XMLComplexStep) {
			steps = ((XMLComplexStep) step).getAllSteps();
		}
		
		return steps;
	}
	
	private void getRequiredRequestables (Step step, Project projectSelected, ProjectExplorerView projectExplorerView, AbstractParentNode parentNode, List<String> transactionList, List<String> sequenceList) {
	 	
		try {
			if (step instanceof SequenceStep) {
				SequenceStep sequenceStep = (SequenceStep) step;
				String sourceProjectName = sequenceStep.getProjectName();
				
				if (!sourceProjectName.equals(projectSelected.getName())) {
					Project project;
					project = getProject(sourceProjectName, projectExplorerView);
					ProjectNode projectNode = new ProjectNode(parentNode, sourceProjectName, project);
					
					Sequence sourceSequence = null;
					String sourceSequenceName = sequenceStep.getSequenceName();
					
					try {
						if (project != null)
							sourceSequence = project.getSequenceByName(sourceSequenceName);
					} catch (EngineException e) {
						sourceSequence = null;
					}
					projectNode.addChild(new SequenceNode(projectNode, sourceSequenceName, sourceSequence));

					if (!sequenceList.contains(sourceProjectName+sourceSequenceName)) {
						sequenceList.add(sourceProjectName+sourceSequenceName);
						parentNode.addChild(projectNode);
					}

				}
			} else if (step instanceof TransactionStep) {
				TransactionStep transactionStep = (TransactionStep) step;
				
				String sourceProjectName = transactionStep.getProjectName();
				if (!sourceProjectName.equals(projectSelected.getName())) {
					Project project;	
					project = getProject(sourceProjectName, projectExplorerView);
					ProjectNode projectNode = new ProjectNode(parentNode, sourceProjectName, project);
					if (project != null) {
						Connector connector = project.getConnectorByName(transactionStep.getConnectorName());
						ConnectorNode connectorNode = null;
						connectorNode = getConnectorNode(projectNode, connector);
						projectNode.addChild(connectorNode);
						
						Transaction sourceTransaction = null;
						String sourceTransactionName = transactionStep.getTransactionName();
						
						try {
							if (connector != null)
								sourceTransaction = connector.getTransactionByName(sourceTransactionName);
						} catch (Exception e) {
							sourceTransaction = null;
						}
						
						connectorNode.addChild(new TransactionNode(connectorNode, sourceTransactionName, sourceTransaction));

						if (!transactionList.contains(project.getName()+connector.getName()+sourceTransactionName)) {
							transactionList.add(project.getName()+connector.getName()+sourceTransactionName);
							parentNode.addChild(projectNode);
						}
					}

				}
			} else if (isStepContainer(step)) {
				List<Step> steps = getStepList(step);
				if (steps != null) {
					for (Step s : steps) {
						getRequiredRequestables(s, projectSelected, projectExplorerView, parentNode, transactionList, sequenceList);
					}
				}
			}
		} catch (EngineException e) {
			ConvertigoPlugin.logException(e, "Unable to load the project", true);
		}
	}
	
	private void getUsedRequestables(Step step, Project projectSelected, AbstractParentNode parentNode) {
		try {
			if (step instanceof SequenceStep) {
				SequenceStep sequenceStep = (SequenceStep) step;
				String sourceProjectName = sequenceStep.getProjectName();
				if (sourceProjectName.equals(projectSelected.getName())) {
					Sequence sourceSequence = null;
					String sourceSequenceName = sequenceStep.getSequenceName();
					try {
						if (projectSelected != null)
							sourceSequence = projectSelected.getSequenceByName(sourceSequenceName);
					} catch (EngineException e) {
						sourceSequence = null;
					}
					SequenceStepNode sequenceStepNode = new SequenceStepNode(parentNode, step.getName() + " -> " + sequenceStep.getSourceSequence(), sourceSequence);
					parentNode.addChild(sequenceStepNode);
					
				}
			} else if (step instanceof TransactionStep) {
				TransactionStep transactionStep = (TransactionStep) step;
				String sourceProjectName = transactionStep.getProjectName();
				if (sourceProjectName.equals(projectSelected.getName())) {
					Transaction sourceTransaction = null;
					Connector connectorSelected = projectSelected.getConnectorByName(transactionStep.getConnectorName());
					try {
						if (connectorSelected != null)
							sourceTransaction = connectorSelected.getTransactionByName(transactionStep.getTransactionName());
					} catch (Exception e) {
						sourceTransaction = null;
					}
					TransactionStepNode transactionStepNode = new TransactionStepNode(parentNode, step.getName() + " -> " + ((TransactionStep) step).getSourceTransaction(), sourceTransaction);
					parentNode.addChild(transactionStepNode);
				}
			} else if (isStepContainer(step)) {
				List<Step> steps = getStepList(step);
				if (steps != null) {
					for (Step s : steps) {
						getUsedRequestables(s, projectSelected, parentNode);
					}
				}
			}
		} catch (EngineException e) {
					ConvertigoPlugin.logException(e, "Unable to load the project", true);
		}
	}

	private void getSequenceReferencingIsUsedBy (Step step, Sequence sequenceSelected, SequenceNode seNode) {
	
		if (step instanceof SequenceStep) { 
			String sourceSequence = ((SequenceStep)step).getSourceSequence();
			if (sourceSequence.equals(sequenceSelected.getProject().getName() + RequestableStep.SOURCE_SEPARATOR + sequenceSelected.getName())) {
				SequenceStepNode sequenceStepNode = new SequenceStepNode(seNode, step.getName(), step);
				seNode.addChild(sequenceStepNode);
			}
		} else 	if (isStepContainer(step)) {
			List<Step> steps = getStepList(step);
			for (Step s : steps) {
				getSequenceReferencingIsUsedBy(s, sequenceSelected, seNode);
			}
		}
	}
	
	
	private void getSequenceReferencingRequires (Step step, Sequence sequenceSelected, ProjectExplorerView projectExplorerView, RequiresNode requiresNode, List<String> transactionList, List<String> sequenceList) {
		try {			
			if (step instanceof SequenceStep) {
				SequenceStep sequenceStep = (SequenceStep) step;
				String projectName = sequenceStep.getProjectName();
				String sequenceName = sequenceStep.getSequenceName();
				
				Project project = getProject(projectName, projectExplorerView);
				ProjectNode projectNode = new ProjectNode(requiresNode, projectName, project);
				
				Sequence sequence = null;
				try {
					if (project != null)
						sequence =  project.getSequenceByName(sequenceStep.getSequenceName());
				} catch (EngineException e) {
					sequence = null;
				}
				
				SequenceNode sequenceNode = new SequenceNode(projectNode, sequenceName, sequence);
				projectNode.addChild(sequenceNode);
				
				if (!sequenceList.contains(projectName+sequenceName)) {
					sequenceList.add(projectName+sequenceName);
					requiresNode.addChild(projectNode);
				}
				
			} else if (step instanceof TransactionStep) {
				TransactionStep transactionStep = (TransactionStep) step;
				String projectName = transactionStep.getProjectName();
				String connectorName = transactionStep.getConnectorName();
				String transactionName = transactionStep.getTransactionName();
				
				Project project = getProject(projectName, projectExplorerView);
				ProjectNode projectNode = new ProjectNode(requiresNode, projectName, project);
				
				Connector connector = null;
				Transaction transaction = null;
				try {
					if (project != null) {
						connector =  project.getConnectorByName(connectorName);
						if (connector != null) {
							transaction = connector.getTransactionByName(transactionName);
						}
					}
				} catch (EngineException e) {
					connector = null;
					transaction = null;
				}
				
				ConnectorNode connectorNode = getConnectorNode(projectNode, connector);
				if (connectorNode == null)
					connectorNode = new ConnectorNode(projectNode, connectorName, connector);
				projectNode.addChild(connectorNode);
				
				TransactionNode transactionNode = new TransactionNode(projectNode, transactionName, transaction);
				connectorNode.addChild(transactionNode);
				
				if (!transactionList.contains(projectName+connectorName+transactionName)) {
					transactionList.add(projectName+connectorName+transactionName);
					requiresNode.addChild(projectNode);
				}
				
			} else if (isStepContainer(step)) {
				List<Step> steps = getStepList(step);
				
				for (Step s : steps) {
				getSequenceReferencingRequires(s, sequenceSelected, projectExplorerView, requiresNode, transactionList, sequenceList);
				}
			}
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Unable to load the project", true);
		}
	}
	
	private void getConnectorReferencingIsUsedBy(Step step,  ProjectExplorerView projectExplorerView, SequenceNode sequenceNode, List<Transaction> transactions, String connectorProjectName, String connectorSelectedName) {

		if (step instanceof TransactionStep) {
			TransactionStep transactionStep = (TransactionStep) step;
			String sourcetransaction = transactionStep.getSourceTransaction();
			for (Transaction transaction : transactions) {
				if (sourcetransaction.equals(connectorProjectName + RequestableStep.SOURCE_SEPARATOR + connectorSelectedName + RequestableStep.SOURCE_SEPARATOR + transaction.getName())){
					sequenceNode.addChild(new TransactionStepNode(sequenceNode, step.getName(),step));
				}
			}
		} else if (isStepContainer(step)) {
			List<Step> steps = getStepList(step);
			for (Step s : steps) {
			getConnectorReferencingIsUsedBy(s, projectExplorerView, sequenceNode, transactions, connectorProjectName, connectorSelectedName);
			}
		}
	}
	
	private void getTransactionReferencing(Step step,	ProjectExplorerView projectExplorerView, AbstractParentNode sequenceNode,	String transactionProjectName, String transactionConnectorName, String transactionName) {

		if (step instanceof TransactionStep) {
			String sourceTransaction = ((TransactionStep)step).getSourceTransaction();
			if (sourceTransaction.equals(transactionProjectName + RequestableStep.SOURCE_SEPARATOR + transactionConnectorName + RequestableStep.SOURCE_SEPARATOR + transactionName)){
				sequenceNode.addChild(new TransactionStepNode(sequenceNode, step.getName(),step));
			}
		} else if (isStepContainer(step)) {
			List<Step> steps = getStepList(step);
			for (Step s : steps) {
				getTransactionReferencing(s, projectExplorerView, sequenceNode, transactionProjectName, transactionConnectorName, transactionName);
			}
		}
	}
	
	private void handleSelectedObjectInRefView(Object firstElement) {
		if (firstElement != null) {
			if (firstElement instanceof AbstractNodeWithDatabaseObjectReference) {
				AbstractNodeWithDatabaseObjectReference abstractNode = (AbstractNodeWithDatabaseObjectReference) firstElement;
				DatabaseObject selectedDatabaseObject = abstractNode.getRefDatabaseObject();
				ProjectExplorerView projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
				TreeObject selectedTreeObject = projectExplorerView.findTreeObjectByUserObject(selectedDatabaseObject);
				
				if (selectedTreeObject != null) {
					projectExplorerView.setSelectedTreeObject(selectedTreeObject);
					
					if (selectedTreeObject instanceof UnloadedProjectTreeObject) {
						ConvertigoPlugin.infoMessageBox("This project is closed. Please open the project first.");
					}					
				}
			}
		}
	}
	
	private boolean isStepContainer(Step step) {
		return (step instanceof BlockStep || step instanceof BranchStep || step instanceof ThenStep || step instanceof ElseStep || step instanceof XMLComplexStep);
	}
}
