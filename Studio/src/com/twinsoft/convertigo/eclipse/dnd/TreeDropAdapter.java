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

package com.twinsoft.convertigo.eclipse.dnd;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.common.XPath;
import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.IXPathable;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.screenclasses.HtmlScreenClass;
import com.twinsoft.convertigo.beans.statements.XpathableStatement;
import com.twinsoft.convertigo.beans.steps.IThenElseContainer;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.beans.steps.XMLElementStep;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.beans.variables.StepMultiValuedVariable;
import com.twinsoft.convertigo.beans.variables.StepVariable;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.popup.actions.ClipboardAction;
import com.twinsoft.convertigo.eclipse.popup.actions.DatabaseObjectDecreasePriorityAction;
import com.twinsoft.convertigo.eclipse.popup.actions.DatabaseObjectIncreasePriorityAction;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.FolderTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.IOrderableTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.IPropertyTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ObjectsFolderTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.PropertyTableRowTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.PropertyTableTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ScreenClassTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.wizards.NewObjectWizard;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.ObjectWithSameNameException;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class TreeDropAdapter extends ViewerDropAdapter {
	private int detail = DND.DROP_NONE;
	private int feedback = DND.FEEDBACK_NONE;
	
	/**
	 * @param viewer
	 */
	public TreeDropAdapter(Viewer viewer) {
		super(viewer);
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerDropAdapter#drop(org.eclipse.swt.dnd.DropTargetEvent)
	 */
	@Override
	public void drop(DropTargetEvent event) {
		if (event.data == null) {
			return;
		}
		
		detail = event.detail;
		ConvertigoPlugin.clipboardManager2.isCopy = (event.detail == DND.DROP_COPY);
		ConvertigoPlugin.clipboardManager2.isCut = (event.detail == DND.DROP_MOVE);
		super.drop(event);
	}


	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerDropAdapter#dragOver(org.eclipse.swt.dnd.DropTargetEvent)
	 */
	@Override
	public void dragOver(DropTargetEvent event) {
		super.dragOver(event);
		
		// Overrides feedback: by default is DND.FEEDBACK_SELECT
		feedback = DND.FEEDBACK_SELECT;
		
		// Handles feedback for objects that can be ordered
		if (getCurrentOperation() == DND.DROP_MOVE) {
			Object targetObject = getCurrentTarget();
			Object sourceObject = getSelectedObject();
			if ((sourceObject != null) && (targetObject != null)) {
				// Source and target objects must be different
				if (!sourceObject.equals(targetObject)) {
					if ((sourceObject instanceof TreeObject) && (targetObject instanceof TreeObject)) {
						// Source and target objects can be ordered
						if ((sourceObject instanceof IOrderableTreeObject) && (targetObject instanceof IOrderableTreeObject)) {
							// Source and target objects must have the same parent
							if (((TreeObject)sourceObject).getParent().equals(((TreeObject)targetObject).getParent())) {
								boolean srcInherited = false;
								if (sourceObject instanceof DatabaseObjectTreeObject) srcInherited = ((DatabaseObjectTreeObject)sourceObject).isInherited;
								else if (sourceObject instanceof IPropertyTreeObject) srcInherited = ((IPropertyTreeObject)sourceObject).isInherited();
								// Source object musn't be inherited
								if (!srcInherited) {
									int currentLocation = getCurrentLocation();
									switch (currentLocation) {
										case LOCATION_BEFORE:
											feedback = DND.FEEDBACK_INSERT_BEFORE;
											break;
										case LOCATION_AFTER:
											feedback = DND.FEEDBACK_INSERT_AFTER;
											break;
										case LOCATION_ON:
										default:
											feedback = DND.FEEDBACK_SELECT;
											break;
									}									
								}
							}
						}
					}
				}
			}
		}
		
		// Set feedback
		event.feedback = feedback;
	}


	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerDropAdapter#performDrop(java.lang.Object)
	 */
	@Override
	public boolean performDrop(Object data) {
		Object targetObject = getCurrentTarget();
		
		// Handle objects reordering with Drag and Drop
		boolean insertBefore = (feedback & DND.FEEDBACK_INSERT_BEFORE) != 0;
		boolean insertAfter = (feedback & DND.FEEDBACK_INSERT_AFTER) != 0;
		if (insertBefore || insertAfter) {
			Object sourceObject = getSelectedObject();
			TreeParent targetTreeParent = ((TreeObject)targetObject).getParent();
			List<? extends TreeObject> children = targetTreeParent.getChildren();
			int destPosition = children.indexOf(targetObject);
			int srcPosition = children.indexOf(sourceObject);
			int delta = destPosition - srcPosition;
			int count = (delta < 0) ? (insertBefore ? delta:delta+1):(insertBefore ? delta-1:delta);
			if (count != 0) {
				if (count < 0)
					new DatabaseObjectIncreasePriorityAction(Math.abs(count)).run();
				else
					new DatabaseObjectDecreasePriorityAction(Math.abs(count)).run();
			}
			return true;
		}
		
		// Handle objects copy or move with Drag and drop
		if (targetObject instanceof TreeObject) {
			TreeObject targetTreeObject = (TreeObject)targetObject;
			if (targetTreeObject != null) {
				ProjectExplorerView	explorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
				
				Document document = null;
				try {
					Shell shell = Display.getDefault().getActiveShell();
					try {
						// Try to parse text data into an XML document
						String source = data.toString();
						document = XMLUtils.getDefaultDocumentBuilder().parse(new InputSource(new StringReader(source)));
						
						ClipboardAction.paste(source, shell, explorerView, targetTreeObject, true);
			            return true;
				    } catch (SAXException sax) {
						// Parse failed probably because data was not XML but an XPATH String
						// in this case, create DatabaseObjects of the correct Type according to the folder where the XPATH is dropped on  
						performDrop(data, explorerView, targetTreeObject);
						return true;
				    }
				} catch (Exception e) {
					// Case of unauthorized databaseObject paste
					if (document != null) {
						try {
							if (!(targetTreeObject instanceof IPropertyTreeObject)) {
								Element rootElement = document.getDocumentElement();
								NodeList nodeList = rootElement.getChildNodes();
								int len = nodeList.getLength();
								Node node;
								
								if (detail == DND.DROP_COPY) {
									for (int i = 0 ; i < len ; i++) {
										node = (Node) nodeList.item(i);
										if (node.getNodeType() != Node.TEXT_NODE) {
											paste(node, targetTreeObject);
										}
									}
									reloadTreeObject(explorerView, targetTreeObject);
									return true;
								}
								else {
									return false;
								}
							}
						} catch (Exception ex) {
							ConvertigoPlugin.errorMessageBox(ex.getMessage());
							return false;
						}
					}
					else {
						ConvertigoPlugin.errorMessageBox(e.getMessage());
						return false;
					}
				}
			}
		}
		return false;
	}

	public DatabaseObject paste(Node node, DatabaseObject parentDatabaseObject, boolean bChangeName) throws EngineException {
		Object object = ConvertigoPlugin.clipboardManager2.read(node);
		if (object instanceof DatabaseObject) {
			DatabaseObject databaseObject = (DatabaseObject)object;
			String dboName = databaseObject.getName();
			String name = null;
			
			boolean bContinue = true;
			int index = 0;
			
			while (bContinue) {
				if (bChangeName) {
					if (index == 0) name = dboName;
					else name = dboName + index;
					databaseObject.setName(name);
				}
				
				databaseObject.hasChanged = true;
				databaseObject.bNew = true;

				try {
					if (parentDatabaseObject != null) parentDatabaseObject.add(databaseObject);
					bContinue = false;
				}
				catch(ObjectWithSameNameException owsne) {
					if ((parentDatabaseObject instanceof HtmlTransaction) && (databaseObject instanceof Statement))
						throw new EngineException("HtmlTransaction already contains a statement named \""+ name +"\".", owsne);
						
					if ((parentDatabaseObject instanceof Sequence) && (databaseObject instanceof Step))
						throw new EngineException("Sequence already contains a step named \""+ name +"\".", owsne);
					
					// Silently ignore
					index++;
				}
			}
			
			NodeList childNodes = node.getChildNodes();
			int len = childNodes.getLength();
	            
			Node childNode;
			String childNodeName;
	            
			for (int i = 0 ; i < len ; i++) {
				childNode = childNodes.item(i);
	                
				if (childNode.getNodeType() != Node.ELEMENT_NODE) continue;
	                
				childNodeName = childNode.getNodeName();
	                
				if (!(childNodeName.equalsIgnoreCase("property")) && 
					!(childNodeName.equalsIgnoreCase("handlers")) &&
					!(childNodeName.equalsIgnoreCase("wsdltype")) &&
					!(childNodeName.equalsIgnoreCase("dnd"))) {
					paste(childNode, databaseObject, bChangeName);
				}
			}

			databaseObject.isSubLoaded = true;
			return databaseObject;
		}
		return null;
	}
	
	private void paste(Node node, TreeObject targetTreeObject) throws EngineException {
		if (targetTreeObject instanceof DatabaseObjectTreeObject) {
			DatabaseObject parent = (DatabaseObject)targetTreeObject.getObject();
			
			DatabaseObject databaseObject = paste(node, null, true);
			Element element = (Element)((Element)node).getElementsByTagName("dnd").item(0);
			
			if (parent instanceof Sequence || parent instanceof StepWithExpressions) {
				
				if (parent instanceof XMLElementStep)
					return;
				if (parent instanceof IThenElseContainer)
					return;
				
				// Add a TransactionStep
				if (databaseObject instanceof Transaction) {
					String projectName = ((Element)element.getElementsByTagName("project").item(0)).getAttribute("name");
					String connectorName = ((Element)element.getElementsByTagName("connector").item(0)).getAttribute("name");
					
					Transaction transaction = (Transaction)databaseObject;
					TransactionStep transactionStep = new TransactionStep();
					transactionStep.setName("Call_"+transaction.getName());
					transactionStep.setProjectName(projectName);
					transactionStep.setConnectorName(connectorName);
					transactionStep.setTransactionName(transaction.getName());
					transactionStep.bNew = true;
					parent.add(transactionStep);
					parent.hasChanged = true;
					if (transaction instanceof TransactionWithVariables) {
						for (Variable variable: ((TransactionWithVariables)transaction).getVariablesList()) {
							StepVariable stepVariable = variable.isMultiValued() ? new StepMultiValuedVariable():new StepVariable();
							stepVariable.setName(variable.getName());
							stepVariable.setComment(variable.getComment());
							stepVariable.setDescription(variable.getDescription());
							stepVariable.setValueOrNull(variable.getValueOrNull());
							stepVariable.setVisibility(variable.getVisibility());
							transactionStep.addVariable(stepVariable);
						}
					}
					
				}
				// Add a SequenceStep
				else if (databaseObject instanceof Sequence) {
					String projectName = ((Element)element.getElementsByTagName("project").item(0)).getAttribute("name");
					
					Sequence seq = (Sequence)databaseObject;
					SequenceStep sequenceStep = new SequenceStep();
					sequenceStep.setName("Call_"+seq.getName());
					sequenceStep.setProjectName(projectName);
					sequenceStep.setSequenceName(seq.getName());
					sequenceStep.bNew = true;
					parent.add(sequenceStep);
					parent.hasChanged = true;
					for (Variable variable: seq.getVariablesList()) {
						StepVariable stepVariable = variable.isMultiValued() ? new StepMultiValuedVariable():new StepVariable();
						stepVariable.setName(variable.getName());
						stepVariable.setComment(variable.getComment());
						stepVariable.setDescription(variable.getDescription());
						stepVariable.setValueOrNull(variable.getValueOrNull());
						stepVariable.setVisibility(variable.getVisibility());
						sequenceStep.addVariable(stepVariable);
					}
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerDropAdapter#validateDrop(java.lang.Object, int, org.eclipse.swt.dnd.TransferData)
	 */
	@Override
	public boolean validateDrop(Object target, int operation, TransferData transferType) {
		if (TextTransfer.getInstance().isSupportedType(transferType)) {
			return true;
		}
		if (StepSourceTransfer.getInstance().isSupportedType(transferType)) {
			if (target instanceof TreeObject) {
				TreeObject targetTreeObject = (TreeObject)target;
				// Check for drop to a step which contains a stepSource definition
				if (targetTreeObject.getObject() instanceof IStepSourceContainer) {
					Object ob = targetTreeObject.getObject();
					Step targetStep = (Step)((ob instanceof StepVariable) ? ((StepVariable)ob).getParent():ob);
					StepSource stepSource = (StepSource)StepSourceTransfer.getInstance().nativeToJava(transferType);
					if (stepSource != null) {
						// Check for drop to a step in the same sequence
						Long key = new Long(stepSource.getPriority());
						Step sourceStep = targetStep.getSequence().loadedSteps.get(key);
						if ((sourceStep != null) && (!targetStep.equals(sourceStep))) {
							// Check for drop on a 'following' step
							try {
								List<TreeObject> siblings = new ArrayList<TreeObject>();
								getNextSiblings(siblings, targetTreeObject.getProjectTreeObject(), sourceStep);
								//System.out.println("siblings: "+siblings.toString());
								return siblings.contains(targetTreeObject);
							}
							catch (Exception e) {e.printStackTrace(System.out);};
						}
					}
				}
			}
		}
		return false;
	}
	
	public void getNextSiblings(List<TreeObject> siblings, TreeParent parent, Object object) {
		if ((parent != null) && (object != null)) {
			if (parent.hasChildren()) {
				for (TreeObject treeObject: parent.getChildren()) {
					if (treeObject.getObject().equals(object)) {
						siblings.add(treeObject);
						if (treeObject instanceof TreeParent) {
							siblings.addAll(((TreeParent)treeObject).getAllChildren());
						}
						continue;
					}
					if (!siblings.isEmpty())
						siblings.add(treeObject);
					if (treeObject instanceof TreeParent) {
						getNextSiblings(siblings, (TreeParent)treeObject, object);
					}
				}
			}
		}
	}
		
	private void performDrop(Object data, ProjectExplorerView explorerView, TreeObject targetTreeObject) throws EngineException, IOException {
		boolean needReload = false;
		DatabaseObject dbo;
		
		if (data instanceof String) {
			String source = data.toString();
			if (targetTreeObject instanceof ObjectsFolderTreeObject) {
				ObjectsFolderTreeObject folderTreeObject = (ObjectsFolderTreeObject)targetTreeObject;
				dbo = (DatabaseObject)folderTreeObject.getParent().getObject();
				switch (folderTreeObject.folderType) {
					case ObjectsFolderTreeObject.FOLDER_TYPE_CRITERIAS:
						if (dbo instanceof HtmlScreenClass) {
							// Creates a XPath criteria for this screen class
							if (!dbo.equals(((HtmlConnector)dbo.getConnector()).getDefaultScreenClass())) {
								((HtmlScreenClass)dbo).addCriteria(createXPath(source));
								needReload = true;
							}
						}
						break;
						
					case ObjectsFolderTreeObject.FOLDER_TYPE_INHERITED_SCREEN_CLASSES:
						if (dbo instanceof HtmlScreenClass) {
							// Creates an inherited screen class with an XPath criteria for this screen class
							HtmlScreenClass newSc = createHtmlScreenClass(dbo.priority+1);
							((HtmlScreenClass)dbo).addInheritedScreenClass(newSc);
							newSc.addCriteria(createXPath(source));
							needReload = true;
						}
						break;
						
					case ObjectsFolderTreeObject.FOLDER_TYPE_EXTRACTION_RULES:
						if (dbo instanceof HtmlScreenClass) {
			    			NewObjectWizard newObjectWizard = new NewObjectWizard(dbo, "com.twinsoft.convertigo.beans.core.ExtractionRule",source,null);
			        		WizardDialog wzdlg = new WizardDialog(Display.getDefault().getActiveShell(), newObjectWizard);
			        		wzdlg.setPageSize(850, 650);
			        		wzdlg.open();
			        		needReload = true;
						}
						break;
						
					default:
						break;
				}
				
				if (needReload)
					reloadTreeObject(explorerView, folderTreeObject.getParent());
			}
			else if (targetTreeObject instanceof DatabaseObjectTreeObject) {
				DatabaseObjectTreeObject databaseObjectTreeObject = (DatabaseObjectTreeObject)targetTreeObject;
				dbo = (DatabaseObject)targetTreeObject.getObject();
				if (databaseObjectTreeObject instanceof ScreenClassTreeObject) {
					if (dbo instanceof HtmlScreenClass) {
						// Creates an inherited screen class with an XPath criteria for this screen class
						HtmlScreenClass newSc = createHtmlScreenClass(dbo.priority+1);
						((HtmlScreenClass)dbo).addInheritedScreenClass(newSc);
						newSc.addCriteria(createXPath(source));
						needReload = true;
					}
				}
				else if (dbo instanceof IXPathable) {
					// Set XPath property
					if(dbo instanceof XpathableStatement)
						((XpathableStatement)dbo).setPureXpath(source);
					else ((IXPathable)dbo).setXpath(source);
					((DatabaseObject)dbo).hasChanged = true;
					needReload = true;
				}
				if (needReload)
					reloadTreeObject(explorerView, databaseObjectTreeObject);
			}
			else if (targetTreeObject instanceof IPropertyTreeObject) {
				IPropertyTreeObject pto = null;
				if (detail == DND.DROP_MOVE) {
					// Set XPath property
					if (targetTreeObject instanceof IXPathable) {
						((IXPathable)targetTreeObject).setXpath("."+source);
						needReload = true;
					}
					// Add new row with xpath
					else if (targetTreeObject instanceof PropertyTableTreeObject) {
//						// See Ticket #679 : Drag and drop without Control
//						PropertyTableTreeObject description = (PropertyTableTreeObject)targetTreeObject;
//						pto = description.addNewRow();
//						needReload = true;
//						if ((pto != null) && (pto instanceof IXPathable)) {
//							((IXPathable)pto).setXpath("."+source);
//						}
						String label = ((PropertyTableTreeObject)targetTreeObject).getRowDefaultLabel().toLowerCase();
						throw new EngineException("Please hold on the 'Ctrl' key while dragging to create a new "+label);
					}
				}
				else if (detail == DND.DROP_COPY) {
					// Add new row with xpath
					if (targetTreeObject instanceof PropertyTableTreeObject) {
						PropertyTableTreeObject description = (PropertyTableTreeObject)targetTreeObject;
						pto = description.addNewRow();
						needReload = true;
						if ((pto != null) && (pto instanceof IXPathable)) {
							((IXPathable)pto).setXpath("."+source);
						}
					}
					// Add new column with xpath
					else if (targetTreeObject instanceof PropertyTableRowTreeObject) {
						PropertyTableRowTreeObject row = (PropertyTableRowTreeObject)targetTreeObject;
						pto = row.addNewColumn();
						needReload = true;
						if ((pto != null) && (pto instanceof IXPathable)) {
							((IXPathable)pto).setXpath("."+source);
						}
					}
				}
				
				if (needReload) {
					pto = (pto == null) ? (IPropertyTreeObject)targetTreeObject:pto;
					targetTreeObject = ((IPropertyTreeObject)targetTreeObject).getTreeObjectOwner();
		    		if (targetTreeObject instanceof DatabaseObjectTreeObject) {
		   				//reloadTreeObject(explorerView, targetTreeObject);
			    		
    					TreeParent treeParent = targetTreeObject.getParent();
    					if (treeParent instanceof FolderTreeObject)
    						treeParent = treeParent.getParent();
				    	explorerView.objectChanged(new CompositeEvent(treeParent.getObject(),pto.getPath()));
		    		}
				}
			}
		}
		else if (data instanceof StepSource) {
			if (targetTreeObject instanceof DatabaseObjectTreeObject) {
				DatabaseObjectTreeObject databaseObjectTreeObject = (DatabaseObjectTreeObject)targetTreeObject;
				dbo = (DatabaseObject)targetTreeObject.getObject();
				
				if (dbo instanceof IStepSourceContainer) {
					// Retrieve Source definition
					XMLVector<String> sourceDefinition = new XMLVector<String>();
					sourceDefinition.add(((StepSource)data).getPriority());
					sourceDefinition.add(((StepSource)data).getXpath());
					
					// Use setPropertyValue in order to set object's value and fire necessary events
					databaseObjectTreeObject.setPropertyValue("sourceDefinition", sourceDefinition);
					
			        // Properties view needs to be refreshed
					if (databaseObjectTreeObject.equals(explorerView.getFirstSelectedTreeObject()))
						refreshPropertiesView(explorerView, databaseObjectTreeObject);
				}
				
			}
		}
	}
	
	private void reloadTreeObject(ProjectExplorerView explorerView, TreeObject treeObject) throws EngineException, IOException {
        explorerView.reloadTreeObject(treeObject);
        explorerView.setSelectedTreeObject(treeObject);
        
        // Properties view needs to be refreshed
        refreshPropertiesView(explorerView, treeObject);
	}

	private void refreshPropertiesView(ProjectExplorerView explorerView, TreeObject treeObject) {
		StructuredSelection structuredSelection = new StructuredSelection(treeObject);
		ConvertigoPlugin.getDefault().getPropertiesView().selectionChanged((IWorkbenchPart)explorerView, structuredSelection);
	}
	
	private XPath createXPath(String source) throws EngineException {
		XPath xpCriterion = new XPath();
		xpCriterion.setXpath(source);
		xpCriterion.hasChanged = true;
		xpCriterion.bNew = true;
		return xpCriterion;
	}
	
	private HtmlScreenClass createHtmlScreenClass(long priority) throws EngineException {
		HtmlScreenClass htmlSc = new HtmlScreenClass();
		htmlSc.priority = priority;
		htmlSc.hasChanged = true;
		htmlSc.bNew = true;
		return htmlSc;
	}
	
}
