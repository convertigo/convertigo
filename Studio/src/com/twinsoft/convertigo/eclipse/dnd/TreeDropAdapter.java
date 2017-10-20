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

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.PropertySheet;
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
import com.twinsoft.convertigo.beans.core.MobileComponent;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.beans.core.UrlMappingOperation;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType;
import com.twinsoft.convertigo.beans.mobile.components.UIComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIControlCallSequence;
import com.twinsoft.convertigo.beans.mobile.components.UIControlEvent;
import com.twinsoft.convertigo.beans.mobile.components.UIForm;
import com.twinsoft.convertigo.beans.mobile.components.UIText;
import com.twinsoft.convertigo.beans.mobile.components.UIControlEvent.AttrEvent;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.IonBean;
import com.twinsoft.convertigo.beans.mobile.components.UIDynamicElement;
import com.twinsoft.convertigo.beans.rest.FormParameter;
import com.twinsoft.convertigo.beans.rest.QueryParameter;
import com.twinsoft.convertigo.beans.screenclasses.HtmlScreenClass;
import com.twinsoft.convertigo.beans.statements.XpathableStatement;
import com.twinsoft.convertigo.beans.steps.IThenElseContainer;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.SmartType;
import com.twinsoft.convertigo.beans.steps.SmartType.Mode;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.beans.steps.XMLElementStep;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.beans.variables.StepMultiValuedVariable;
import com.twinsoft.convertigo.beans.variables.StepVariable;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.popup.actions.ClipboardAction;
import com.twinsoft.convertigo.eclipse.popup.actions.DatabaseObjectDecreasePriorityAction;
import com.twinsoft.convertigo.eclipse.popup.actions.DatabaseObjectIncreasePriorityAction;
import com.twinsoft.convertigo.eclipse.property_editors.MobileSmartSourcePropertyDescriptor;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.FolderTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.IOrderableTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.IPropertyTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.MobileUIComponentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ObjectsFolderTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.PropertyTableRowTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.PropertyTableTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ScreenClassTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.eclipse.wizards.new_object.NewObjectWizard;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.InvalidOperationException;
import com.twinsoft.convertigo.engine.ObjectWithSameNameException;
import com.twinsoft.convertigo.engine.enums.HttpMethodType;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;
import com.twinsoft.convertigo.engine.util.CachedIntrospector.Property;
import com.twinsoft.convertigo.engine.util.GenericUtils;
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
			// case of DND drop with stepSource on Linux, see #4473
			for (TransferData transferData : event.dataTypes) {
				if (StepSourceTransfer.getInstance().isSupportedType(transferData)) {
					event.data = StepSourceTransfer.getInstance().getStepSource();
					break;
				}
				if (PaletteSourceTransfer.getInstance().isSupportedType(transferData)) {
					event.data = PaletteSourceTransfer.getInstance().getPaletteSource();
					break;
				}
				if (MobileSourceTransfer.getInstance().isSupportedType(transferData)) {
					event.data = MobileSourceTransfer.getInstance().getMobileSource();
					break;
				}
			}
			if (event.data == null) {
				return;
			}
		}
		
		detail = event.detail;
		ConvertigoPlugin.clipboardManagerDND.isCopy = (event.detail == DND.DROP_COPY);
		ConvertigoPlugin.clipboardManagerDND.isCut = (event.detail == DND.DROP_MOVE);
		super.drop(event);
	}


	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerDropAdapter#dragOver(org.eclipse.swt.dnd.DropTargetEvent)
	 */
	@Override
	public void dragOver(DropTargetEvent event) {
		super.dragOver(event);
		
		// Overrides feedback: by default is DND.FEEDBACK_SELECT
		feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_SCROLL | DND.FEEDBACK_EXPAND;
		
		boolean shouldFeedBack = true;
		if (PaletteSourceTransfer.getInstance().isSupportedType(event.currentDataType)) {
			shouldFeedBack = false;
		}
		
		// Handles feedback for objects that can be ordered
		if (shouldFeedBack && getCurrentOperation() == DND.DROP_MOVE) {
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
											feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_SCROLL | DND.FEEDBACK_EXPAND;
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
		
		// Handle tree objects reordering with Drag and Drop
		if (data instanceof String) {
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
						
						ClipboardAction.dnd.paste(source, shell, explorerView, targetTreeObject, true);
			            return true;
				    } catch (SAXException sax) {
						// Parse failed probably because data was not XML but an XPATH String
						// in this case, create DatabaseObjects of the correct Type according to the folder where the XPATH is dropped on  
						performDrop(data, explorerView, targetTreeObject);
						return true;
				    }
				} catch (Exception e) {
					if (e instanceof ObjectWithSameNameException) {
						document = null;
					}
					if (e instanceof InvalidOperationException) {
						document = null;
					}
					
					// Case of unauthorized databaseObject paste
					if (document != null) {
						try {
							if (!(targetTreeObject instanceof IPropertyTreeObject)) {
								Element rootElement = document.getDocumentElement();
								NodeList nodeList = rootElement.getChildNodes();
								boolean unauthorized = false;
								int len = nodeList.getLength();
								Node node;
								
								// case of folder, retrieve owner object
								targetTreeObject = explorerView.getFirstSelectedDatabaseObjectTreeObject(targetTreeObject);
								
								if (detail == DND.DROP_COPY) {
									for (int i = 0 ; i < len ; i++) {
										node = (Node) nodeList.item(i);
										if (node.getNodeType() != Node.TEXT_NODE) {
											// Special objects paste
											if (!paste(node, targetTreeObject)) {
												unauthorized = true; // Real unauthorized databaseObject paste
											}
										}
									}
									reloadTreeObject(explorerView, targetTreeObject);
								}
								else if (detail == DND.DROP_MOVE) {
									for (int i = 0 ; i < len ; i++) {
										node = (Node) nodeList.item(i);
										if (node.getNodeType() != Node.TEXT_NODE) {
											// Special objects move
											if (!move(node, targetTreeObject)) {
												unauthorized = true; // Real unauthorized databaseObject move
											}
										}
									}
									reloadTreeObject(explorerView, targetTreeObject);
								}
								else {
									unauthorized = true; // Real unauthorized databaseObject
								}
								
								if (unauthorized) {
									throw e;
								}
								return true;
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
		Object object = ConvertigoPlugin.clipboardManagerDND.read(node);
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
					!(childNodeName.equalsIgnoreCase("docdata")) &&
					!(childNodeName.equalsIgnoreCase("beandata")) &&
					!(childNodeName.equalsIgnoreCase("dnd"))) {
					paste(childNode, databaseObject, bChangeName);
				}
			}

			databaseObject.isSubLoaded = true;
			return databaseObject;
		}
		return null;
	}
	
	private boolean paste(Node node, TreeObject targetTreeObject) throws EngineException {
		if (targetTreeObject instanceof DatabaseObjectTreeObject) {
			DatabaseObject parent = ((DatabaseObjectTreeObject) targetTreeObject).getObject();
			
			DatabaseObject databaseObject = paste(node, null, true);
			Element element = (Element)((Element)node).getElementsByTagName("dnd").item(0);
			
			// SEQUENCER
			if (parent instanceof Sequence || parent instanceof StepWithExpressions) {
				
				if (parent instanceof XMLElementStep)
					return false;
				if (parent instanceof IThenElseContainer)
					return false;
				
				// Add a TransactionStep
				if (databaseObject instanceof Transaction) {
					String projectName = ((Element)element.getElementsByTagName("project").item(0)).getAttribute("name");
					String connectorName = ((Element)element.getElementsByTagName("connector").item(0)).getAttribute("name");
					
					Transaction transaction = (Transaction)databaseObject;
					TransactionStep transactionStep = new TransactionStep();
					transactionStep.setSourceTransaction(projectName + TransactionStep.SOURCE_SEPARATOR + connectorName +
							TransactionStep.SOURCE_SEPARATOR + transaction.getName());
					transactionStep.bNew = true;
					parent.add(transactionStep);
					parent.hasChanged = true;
					if (transaction instanceof TransactionWithVariables) {
						for (Variable variable: ((TransactionWithVariables)transaction).getVariablesList()) {
							StepVariable stepVariable = variable.isMultiValued() ? new StepMultiValuedVariable():new StepVariable();
							stepVariable.setName(variable.getName());
							stepVariable.setComment(variable.getComment());
							stepVariable.setDescription(variable.getDescription());
							stepVariable.setRequired(variable.isRequired());
							stepVariable.setValueOrNull(variable.getValueOrNull());
							stepVariable.setVisibility(variable.getVisibility());
							transactionStep.addVariable(stepVariable);
						}
					}
					return true;
				}
				// Add a SequenceStep
				else if (databaseObject instanceof Sequence) {
					String projectName = ((Element)element.getElementsByTagName("project").item(0)).getAttribute("name");
					
					Sequence seq = (Sequence)databaseObject;
					SequenceStep sequenceStep = new SequenceStep();
					sequenceStep.setSourceSequence(projectName + SequenceStep.SOURCE_SEPARATOR + seq.getName());
					sequenceStep.bNew = true;
					parent.add(sequenceStep);
					parent.hasChanged = true;
					for (Variable variable: seq.getVariablesList()) {
						StepVariable stepVariable = variable.isMultiValued() ? new StepMultiValuedVariable():new StepVariable();
						stepVariable.setName(variable.getName());
						stepVariable.setComment(variable.getComment());
						stepVariable.setDescription(variable.getDescription());
						stepVariable.setRequired(variable.isRequired());
						stepVariable.setValueOrNull(variable.getValueOrNull());
						stepVariable.setVisibility(variable.getVisibility());
						sequenceStep.addVariable(stepVariable);
					}
					return true;
				}
			}
			// URLMAPPER
			else if (parent instanceof UrlMappingOperation) {
				
				// Set associated requestable, add all parameters for operation
				if (databaseObject instanceof RequestableObject) {
					String dboQName = "";
					if (databaseObject instanceof Sequence) {
						dboQName = ((Element)element.getElementsByTagName("project").item(0)).getAttribute("name") +
								"." + databaseObject.getName();
					}
					else if (databaseObject instanceof Transaction) {
						dboQName = ((Element)element.getElementsByTagName("project").item(0)).getAttribute("name") +
								"." + ((Element)element.getElementsByTagName("connector").item(0)).getAttribute("name") +
								"." + databaseObject.getName();
					}
					
					UrlMappingOperation operation = (UrlMappingOperation) parent;
					operation.setTargetRequestable(dboQName);
					if (operation.getComment().isEmpty()) {
						operation.setComment(databaseObject.getComment());
					}
					operation.hasChanged = true;
					try {
						StringTokenizer st = new StringTokenizer(dboQName,".");
						int count = st.countTokens();
						Project p = Engine.theApp.databaseObjectsManager.getProjectByName(st.nextToken());
						List<RequestableVariable> variables = new ArrayList<RequestableVariable>();
						if (count == 2) {
							variables = p.getSequenceByName(st.nextToken()).getVariablesList();
						}
						else if (count == 3) {
							variables = ((TransactionWithVariables)p.getConnectorByName(st.nextToken()).getTransactionByName(st.nextToken())).getVariablesList();
						}
						
						for (RequestableVariable variable: variables) {
							UrlMappingParameter parameter = null;
							try {
								parameter = operation.getParameterByName(variable.getName());
							}
							catch (Exception e) {}
							if (parameter == null) {
								boolean acceptForm = operation.getMethod().equalsIgnoreCase(HttpMethodType.POST.name()) ||
										operation.getMethod().equalsIgnoreCase(HttpMethodType.PUT.name());
								parameter = acceptForm ? new FormParameter() : new QueryParameter();
								parameter.setComment(variable.getComment());
								parameter.setName(variable.getName());
								parameter.setMappedVariableName(variable.getName());
								parameter.setMultiValued(variable.isMultiValued());
								parameter.bNew = true;
								operation.add(parameter);
								operation.hasChanged = true;
							}
						}
					}
					catch (Exception e) {}
					
					return true;
				}
				// Add a parameter to mapping operation
				else if (databaseObject instanceof RequestableVariable) {
					RequestableVariable variable = (RequestableVariable)databaseObject;
					UrlMappingOperation operation = (UrlMappingOperation) parent;
					UrlMappingParameter parameter = null;
					try {
						parameter = operation.getParameterByName(variable.getName());
					}
					catch (Exception e) {}
					if (parameter == null) {
						boolean acceptForm = operation.getMethod().equalsIgnoreCase(HttpMethodType.POST.name()) ||
								operation.getMethod().equalsIgnoreCase(HttpMethodType.PUT.name());
						parameter = acceptForm ? new FormParameter() : new QueryParameter();
						parameter.setComment(variable.getComment());
						parameter.setName(variable.getName());
						parameter.setMappedVariableName(variable.getName());
						parameter.setMultiValued(variable.isMultiValued());
						parameter.bNew = true;
						operation.add(parameter);
						operation.hasChanged = true;
					}
					return true;
				}
			}
			// MOBILE COMPONENTS
			else if (parent instanceof UIForm) {
				UIForm uiForm = (UIForm)parent;
				
				// Add child components to fill the form
				if (databaseObject instanceof Sequence) {
					try {
						String projectName = ((Element)element.getElementsByTagName("project").item(0)).getAttribute("name");
						
						Sequence sequence = (Sequence)databaseObject;
						
						// add an onSubmit event with a callSequence
						UIControlEvent event = new UIControlEvent();
						event.setEventName(AttrEvent.onSubmit.name());
						event.bNew = true;
						event.hasChanged = true;
						
						UIControlCallSequence call = new UIControlCallSequence();
						call.setTarget(projectName + "." + sequence.getName());
						call.bNew = true;
						call.hasChanged = true;
						
						event.add(call);
						
						// add a list of item with label & input for each variable
						DatabaseObject dboList = ComponentManager.createBean(ComponentManager.getComponentByName("List"));
						for (RequestableVariable variable: sequence.getVariables()) {
							DatabaseObject dboItem = ComponentManager.createBean(ComponentManager.getComponentByName("ListItem"));
							dboList.add(dboItem);
							
							DatabaseObject dboLabel = ComponentManager.createBean(ComponentManager.getComponentByName("Label"));
							dboItem.add(dboLabel);
							
							UIText uiText = new UIText();
							uiText.bNew = true;
							uiText.hasChanged = true;
							uiText.setTextSmartType(new MobileSmartSourceType(variable.getName()+":"));
							dboLabel.add(uiText);
							
							DatabaseObject dboInput = ComponentManager.createBean(ComponentManager.getComponentByName("Input"));
							if (dboInput != null && dboInput instanceof UIDynamicElement) {
								IonBean ionBean = ((UIDynamicElement)dboInput).getIonBean();
								if (ionBean != null && ionBean.hasProperty("FormControlName")) {
									ionBean.setPropertyValue("FormControlName", new MobileSmartSourceType(variable.getName()));
								}
								dboItem.add(dboInput);
							}
						}
						
						// add a buttonset with a submit and a reset button
						DatabaseObject dboBtnSet = ComponentManager.createBean(ComponentManager.getComponentByName("ButtonSet"));
						
						DatabaseObject dboSubmit = ComponentManager.createBean(ComponentManager.getComponentByName("SubmitButton"));
						dboBtnSet.add(dboSubmit);
						UIText sText = new UIText();
						sText.bNew = true;
						sText.hasChanged = true;
						sText.setTextSmartType(new MobileSmartSourceType("Submit"));
						dboSubmit.add(sText);
						
						DatabaseObject dboReset = ComponentManager.createBean(ComponentManager.getComponentByName("ResetButton"));
						dboBtnSet.add(dboReset);
						UIText rText = new UIText();
						rText.bNew = true;
						rText.hasChanged = true;
						rText.setTextSmartType(new MobileSmartSourceType("Reset"));
						dboReset.add(rText);
						
						uiForm.add(event);
						uiForm.add(dboList);
						uiForm.add(dboBtnSet);
					}
					catch (Exception e) {
						throw new EngineException("Unable to create filled Form from requestable", e);
					}
					return true;
				}
			}
		}
		return false;
	}

	private boolean move(Node node, TreeObject targetTreeObject) throws EngineException {
		if (targetTreeObject instanceof DatabaseObjectTreeObject) {
			DatabaseObject parent = ((DatabaseObjectTreeObject) targetTreeObject).getObject();
			
			DatabaseObject databaseObject = paste(node, null, true);
			Element element = (Element)((Element)node).getElementsByTagName("dnd").item(0);
			
			// SEQUENCER
			if (parent instanceof Sequence || parent instanceof StepWithExpressions) {
				;
			}
			// URLMAPPER
			else if (parent instanceof UrlMappingOperation) {
				// Set associated requestable
				if (databaseObject instanceof RequestableObject) {
					String dboQName = "";
					if (databaseObject instanceof Sequence) {
						dboQName = ((Element)element.getElementsByTagName("project").item(0)).getAttribute("name") +
								"." + databaseObject.getName();
					}
					else if (databaseObject instanceof Transaction) {
						dboQName = ((Element)element.getElementsByTagName("project").item(0)).getAttribute("name") +
								"." + ((Element)element.getElementsByTagName("connector").item(0)).getAttribute("name") +
								"." + databaseObject.getName();
					}
					
					UrlMappingOperation operation = (UrlMappingOperation) parent;
					operation.setTargetRequestable(dboQName);
					if (operation.getComment().isEmpty()) {
						operation.setComment(databaseObject.getComment());
					}
					operation.hasChanged = true;
					
					return true;
				}
			}
			else if (parent instanceof UrlMappingParameter) {
				// Set associated mapped variable for parameter
				if (databaseObject instanceof RequestableVariable) {
					RequestableVariable variable = (RequestableVariable)databaseObject;
					UrlMappingParameter parameter = (UrlMappingParameter)parent;
					parameter.setMappedVariableName(variable.getName());
					parameter.hasChanged = true;
					return true;
				}
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerDropAdapter#validateDrop(java.lang.Object, int, org.eclipse.swt.dnd.TransferData)
	 */
	@Override
	public boolean validateDrop(Object target, int operation, TransferData transferType) {
		if (TextTransfer.getInstance().isSupportedType(transferType)) {
			if (getCurrentOperation() == DND.DROP_MOVE) {
				Object targetObject = getCurrentTarget();
				Object sourceObject = getSelectedObject();
				if ((sourceObject != null) && (targetObject != null)) {
					if ((sourceObject instanceof TreeObject) && (targetObject instanceof TreeObject)) {
						if (((TreeObject)targetObject).isChildOf((TreeObject)sourceObject)) {
							return false;
						}
						if ((sourceObject instanceof DatabaseObjectTreeObject) && (targetObject instanceof DatabaseObjectTreeObject)) {
							try {
								String xmlData = TextTransfer.getInstance().nativeToJava(transferType).toString();
								List<Object> list = ConvertigoPlugin.clipboardManagerDND.read(xmlData);
								DatabaseObject databaseObject = (DatabaseObject) list.get(0);
								DatabaseObject parentDatabaseObject = ((DatabaseObjectTreeObject)target).getObject().getParent();
								if (!DatabaseObjectsManager.acceptDatabaseObjects(parentDatabaseObject, databaseObject)) {
									return false;
								}
								return true;
							} catch (Exception e) {
								e.printStackTrace(System.out);
							}
						}
					}
				}
			}
			return true;
		}
		if (StepSourceTransfer.getInstance().isSupportedType(transferType)) {
			if (target instanceof TreeObject) {
				TreeObject targetTreeObject = (TreeObject) target;
				// Check for drop to a step which contains a stepSource definition
				//if (targetTreeObject.getObject() instanceof IStepSourceContainer) {
				Object ob = targetTreeObject.getObject();
				if (ob instanceof Step && ((Step) ob).canWorkOnSource() || ob instanceof IStepSourceContainer) {
					StepSource stepSource = StepSourceTransfer.getInstance().getStepSource();
					if (stepSource != null) {
						Step targetStep = (Step)((ob instanceof StepVariable) ? ((StepVariable) ob).getParent() : ob);
						
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
		if (PaletteSourceTransfer.getInstance().isSupportedType(transferType)) {
			if (target instanceof DatabaseObjectTreeObject) {
				DatabaseObject parentDatabaseObject = ((DatabaseObjectTreeObject)target).getObject();
				PaletteSource paletteSource = PaletteSourceTransfer.getInstance().getPaletteSource();
				if (paletteSource != null) {
					try {
						String xmlData = paletteSource.getXmlData();
						List<Object> list = ConvertigoPlugin.clipboardManagerDND.read(xmlData);
						DatabaseObject databaseObject = (DatabaseObject) list.get(0);
						if (!DatabaseObjectsManager.acceptDatabaseObjects(parentDatabaseObject, databaseObject)) {
							return false;
						}
						if (parentDatabaseObject instanceof MobileComponent && databaseObject instanceof UIComponent) {
							if (!ComponentManager.acceptDatabaseObjects(parentDatabaseObject, databaseObject)) {
								return false;
							}
						}
						return true;
					} catch (Exception e) {
						e.printStackTrace(System.out);
					}
				}
			}
		}
		if (MobileSourceTransfer.getInstance().isSupportedType(transferType)) {
			if (target instanceof MobileUIComponentTreeObject) {
				MobileUIComponentTreeObject mcto = (MobileUIComponentTreeObject)target;
				for (IPropertyDescriptor descriptor : mcto.getPropertyDescriptors()) {
					if (descriptor instanceof MobileSmartSourcePropertyDescriptor) {
						if (!((MobileSmartSourcePropertyDescriptor)descriptor).isReadOnly()) {
							return true;
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
		
	private void performDrop(Object data, final ProjectExplorerView explorerView, TreeObject targetTreeObject) throws EngineException, IOException {
		boolean needReload = false;
		final DatabaseObject dbo;
		
		if (data instanceof String) {
			String source = data.toString();
			if (targetTreeObject instanceof ObjectsFolderTreeObject) {
				ObjectsFolderTreeObject folderTreeObject = (ObjectsFolderTreeObject) targetTreeObject;
				dbo = (DatabaseObject) folderTreeObject.getParent().getObject();
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
							HtmlScreenClass newSc = createHtmlScreenClass(dbo.priority + 1);
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
				final DatabaseObjectTreeObject databaseObjectTreeObject = (DatabaseObjectTreeObject)targetTreeObject;
				dbo = (DatabaseObject)targetTreeObject.getObject();
				
				final Set<PropertyDescriptor> propertyDescriptors = new TreeSet<PropertyDescriptor>(new Comparator<PropertyDescriptor>() {

					@Override
					public int compare(PropertyDescriptor o1, PropertyDescriptor o2) {
						return o1.getDisplayName().compareTo(o2.getDisplayName());
					}
				});
				
				propertyDescriptors.addAll(CachedIntrospector.getPropertyDescriptors(dbo, Property.smartType));
				propertyDescriptors.addAll(CachedIntrospector.getPropertyDescriptors(dbo, Property.sourceDefinition));
				propertyDescriptors.addAll(CachedIntrospector.getPropertyDescriptors(dbo, Property.sourcesDefinition));
				
				if (!propertyDescriptors.isEmpty()) {
					// Retrieve Source definition
					final XMLVector<String> sourceDefinition = new XMLVector<String>(2);
					sourceDefinition.add(((StepSource) data).getPriority());
					sourceDefinition.add(((StepSource) data).getXpath());
					
					SelectionListener selectionListener = new SelectionListener() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							PropertyDescriptor propertyDescriptor = (e == null) ?
									propertyDescriptors.iterator().next() :
									(PropertyDescriptor) e.widget.getData();
							String propertyName = propertyDescriptor.getName();
							
							if (propertyDescriptor.getPropertyType().isAssignableFrom(SmartType.class)) {
								SmartType smartType = new SmartType();
								smartType.setMode(Mode.SOURCE);
								smartType.setSourceDefinition(sourceDefinition);
								
								databaseObjectTreeObject.setPropertyValue(propertyDescriptor.getName(), smartType);
							} else if (propertyName.equals("sourceDefinition")) {
								// Use setPropertyValue in order to set object's value and fire necessary events
								databaseObjectTreeObject.setPropertyValue(propertyDescriptor.getName(), sourceDefinition);							
							} else if (propertyName.equals("sourcesDefinition")) {
								try {
									XMLVector<XMLVector<Object>> sourcesDefinition = GenericUtils.cast(propertyDescriptor.getReadMethod().invoke(dbo));
									sourcesDefinition = new XMLVector<XMLVector<Object>>(sourcesDefinition); // make a copy to make a property change
									XMLVector<Object> row = new XMLVector<Object>();
									row.add("");
									row.add(sourceDefinition);
									row.add("");
									sourcesDefinition.add(row);
									databaseObjectTreeObject.setPropertyValue(propertyName, sourcesDefinition);
								} catch (Exception ex) {
									ConvertigoPlugin.logError("failed to add to sourcesDefinition of " + dbo.getName());
								}
							}
							
							// Properties view needs to be refreshed
							refreshPropertiesView(explorerView, databaseObjectTreeObject);
						}
						
						@Override
						public void widgetDefaultSelected(SelectionEvent e) {
						}
					};
					
					if (propertyDescriptors.size() == 1) {
						selectionListener.widgetSelected(null);
					} else {
						Shell shell = ConvertigoPlugin.getMainShell();
						Menu dropMenu = new Menu(shell, SWT.POP_UP);
		                shell.setMenu(dropMenu);
		                
		                for (PropertyDescriptor propertyDescriptor: propertyDescriptors) {
			                MenuItem itemCheck = new MenuItem(dropMenu, SWT.NONE);
			                itemCheck.setText(propertyDescriptor.getDisplayName());
			                itemCheck.setData(propertyDescriptor);
			                itemCheck.addSelectionListener(selectionListener);
		                }
		                dropMenu.setVisible(true);
					}
				}				
			}
		}
		else if (data instanceof PaletteSource) {
			try {
				if (targetTreeObject instanceof DatabaseObjectTreeObject) {
					DatabaseObject parent = (DatabaseObject)targetTreeObject.getObject();
					String xmlData = ((PaletteSource)data).getXmlData();
					Document document = XMLUtils.getDefaultDocumentBuilder().parse(new InputSource(new StringReader(xmlData)));
					Element rootElement = document.getDocumentElement();
					NodeList nodeList = rootElement.getChildNodes();
					int len = nodeList.getLength();
					Node node;
					// Special objects move from palette
					if (detail == DND.DROP_MOVE) {
						for (int i = 0 ; i < len ; i++) {
							node = (Node) nodeList.item(i);
							if (node.getNodeType() != Node.TEXT_NODE) {
								if (paste(node, parent, true) == null) {
									throw new Exception();
								}
							}
						}
						reloadTreeObject(explorerView, targetTreeObject);
					}
				}
				else {
					throw new Exception();
				}
			} catch (Exception ex) {
				ConvertigoPlugin.logError("failed to add from palette");
			}
		}
		else if (data instanceof MobileSource) {
			try {
				String jsonString = ((MobileSource)data).getJsonString();
				
				if (targetTreeObject instanceof MobileUIComponentTreeObject) {
					Shell shell = ConvertigoPlugin.getMainShell();
					Menu dropMenu = new Menu(shell, SWT.POP_UP);
	                shell.setMenu(dropMenu);
					
					MobileUIComponentTreeObject mcto = (MobileUIComponentTreeObject)targetTreeObject;
					for (IPropertyDescriptor descriptor : mcto.getPropertyDescriptors()) {
						if (descriptor instanceof MobileSmartSourcePropertyDescriptor) {
							MobileSmartSourcePropertyDescriptor cspd = (MobileSmartSourcePropertyDescriptor)descriptor;
							if (!cspd.isReadOnly()) {
								String propertyName = (String) cspd.getId();
								String propertyLabel = (String) cspd.getDisplayName();
				                MenuItem itemCheck = new MenuItem(dropMenu, SWT.NONE);
				                itemCheck.setText(propertyLabel);
				                itemCheck.addSelectionListener(new SelectionListener() {
									@Override
									public void widgetSelected(SelectionEvent e) {
										MobileSmartSourceType cst = new MobileSmartSourceType();
										cst.setMode(MobileSmartSourceType.Mode.SOURCE);
										cst.setSmartValue(jsonString);
										
										mcto.setPropertyValue(propertyName, cst);
										refreshPropertiesView(explorerView, mcto);
									}
									
									@Override
									public void widgetDefaultSelected(SelectionEvent e) {
									}
								});
							}
						}
					}
					dropMenu.setVisible(true);
					
				}
				else {
					throw new Exception();
				}
			} catch (Exception ex) {
				ConvertigoPlugin.logError("failed to add mobile source");
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
		
		PropertySheet propertySheet = ConvertigoPlugin.getDefault().getPropertiesView();
		propertySheet.partActivated(explorerView);
		propertySheet.selectionChanged(explorerView, structuredSelection);
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
