/*
 * Copyright (c) 2001-2022 Convertigo SA.
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
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
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
import com.twinsoft.convertigo.eclipse.property_editors.MobileSmartSourcePropertyDescriptor;
import com.twinsoft.convertigo.eclipse.property_editors.NgxSmartSourcePropertyDescriptor;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.FolderTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.IOrderableTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.IPropertyTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.MobileComponentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.MobileUIComponentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.NgxComponentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.NgxUIComponentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ObjectsFolderTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
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
import com.twinsoft.convertigo.engine.helpers.BatchOperationHelper;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;
import com.twinsoft.convertigo.engine.util.CachedIntrospector.Property;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.TwsCachedXPathAPI;
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
		} else {
			for (TransferData transferData : event.dataTypes) {
				if (MobileSourceTransfer.getInstance().isSupportedType(transferData)) {
					event.data = MobileSourceTransfer.getInstance().getMobileSource();
					break;
				}
			}
		}

		if ("gtk".equalsIgnoreCase(SWT.getPlatform()) && event.detail == 0) {
			event.detail = DND.DROP_MOVE;
		}

		detail = event.detail;
		if (ConvertigoPlugin.clipboardManagerDND.objects == null) {
			// DRAG not done from the treeview
			ConvertigoPlugin.clipboardManagerDND.isCopy = true;
			ConvertigoPlugin.clipboardManagerDND.isCut = false;
		} else {
			ConvertigoPlugin.clipboardManagerDND.isCopy = (event.detail == DND.DROP_COPY);
			ConvertigoPlugin.clipboardManagerDND.isCut = (event.detail == DND.DROP_MOVE);
		}
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
				break;
			}
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
									break;
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
		MobileBuilder mb = null;

		Engine.logStudio.info("---------------------- Drop started ----------------------");
		try {
			Object targetObject = getCurrentTarget();

			IEditorPart editorPart = ConvertigoPlugin.getDefault().getApplicationComponentEditor();
			if (editorPart != null) {
				IEditorInput input = editorPart.getEditorInput();
				if (input instanceof com.twinsoft.convertigo.eclipse.editors.mobile.ApplicationComponentEditorInput) {
					com.twinsoft.convertigo.eclipse.editors.mobile.ApplicationComponentEditorInput editorInput = GenericUtils.cast(input);
					mb = editorInput.getApplication().getProject().getMobileBuilder();
				}
				if (input instanceof com.twinsoft.convertigo.eclipse.editors.ngx.ApplicationComponentEditorInput) {
					com.twinsoft.convertigo.eclipse.editors.ngx.ApplicationComponentEditorInput editorInput = GenericUtils.cast(input);
					mb = editorInput.getApplication().getProject().getMobileBuilder();
				}
			}

			// Handle objects copy or move with Drag and drop
			if (targetObject instanceof TreeObject) {
				TreeObject targetTreeObject = (TreeObject)targetObject;
				if (targetTreeObject != null) {
					ProjectExplorerView	explorerView = targetTreeObject.getProjectExplorerView();

					Document document = null;
					try {
						Shell shell = Display.getDefault().getActiveShell();
						try {
							// Try to parse text data into an XML document
							String source = data.toString();
							document = XMLUtils.getDefaultDocumentBuilder().parse(new InputSource(new StringReader(source)));
							if (mb != null) {
								mb.prepareBatchBuild();
							}
							BatchOperationHelper.start();

							boolean insertBefore = (feedback & DND.FEEDBACK_INSERT_BEFORE) != 0;
							boolean insertAfter = (feedback & DND.FEEDBACK_INSERT_AFTER) != 0;
							TreeObject sourceObject = (TreeObject) getSelectedObject();
							if (insertBefore || insertAfter) {
								TreeParent targetTreeParent = ((TreeObject)targetObject).getParent();
								if (sourceObject.getParent() != targetTreeParent) {
									ProjectTreeObject prjTree = targetTreeParent.getProjectTreeObject();
									String path = targetTreeParent.getPath();
									ClipboardAction.dnd.paste(source, shell, explorerView, targetTreeParent, true);
									targetTreeParent = (TreeParent) explorerView.findTreeObjectByPath(prjTree, path);
								}
								explorerView.moveChildTo(targetTreeParent, sourceObject, targetTreeObject, insertBefore);
							} else {
								ClipboardAction.dnd.paste(source, shell, explorerView, targetTreeObject, true);
							}

							BatchOperationHelper.stop();
							return true;
						} catch (SAXException sax) {
							BatchOperationHelper.cancel();
							if (mb != null) {
								mb.prepareBatchBuild();
							}
							BatchOperationHelper.start();
							// Parse failed probably because data was not XML but an XPATH String
							// in this case, create DatabaseObjects of the correct Type according to the folder where the XPATH is dropped on
							performDrop(data, explorerView, targetTreeObject);
							BatchOperationHelper.stop();
							return true;
						}
					} catch (Exception e) {
						BatchOperationHelper.cancel();
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
		} finally {
			Engine.logStudio.info("---------------------- Drop ended   ----------------------");
			BatchOperationHelper.cancel();
		}
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

			databaseObject.isImporting = false; // needed !
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
							String variableName = variable.getName();
							Object variableValue = variable.getValueOrNull();
							UrlMappingParameter parameter = null;
							try {
								parameter = operation.getParameterByName(variableName);
							}
							catch (Exception e) {}
							if (parameter == null) {
								boolean acceptForm = operation.getMethod().equalsIgnoreCase(HttpMethodType.POST.name()) ||
										operation.getMethod().equalsIgnoreCase(HttpMethodType.PUT.name());
								parameter = acceptForm ? new FormParameter() : new QueryParameter();
								parameter.setName(variableName);
								parameter.setComment(variable.getComment());
								parameter.setArray(false);
								parameter.setExposed(((RequestableVariable)variable).isWsdl());
								parameter.setMultiValued(variable.isMultiValued());
								parameter.setRequired(variable.isRequired());
								parameter.setValueOrNull(!variable.isMultiValued() ? variableValue:null);
								parameter.setMappedVariableName(variableName);
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
					String variableName = variable.getName();
					Object variableValue = variable.getValueOrNull();
					try {
						parameter = operation.getParameterByName(variableName);
					}
					catch (Exception e) {}
					if (parameter == null) {
						boolean acceptForm = operation.getMethod().equalsIgnoreCase(HttpMethodType.POST.name()) ||
								operation.getMethod().equalsIgnoreCase(HttpMethodType.PUT.name());
						parameter = acceptForm ? new FormParameter() : new QueryParameter();
						parameter.setName(variableName);
						parameter.setComment(variable.getComment());
						parameter.setArray(false);
						parameter.setExposed(((RequestableVariable)variable).isWsdl());
						parameter.setMultiValued(variable.isMultiValued());
						parameter.setRequired(variable.isRequired());
						parameter.setValueOrNull(!variable.isMultiValued() ? variableValue:null);
						parameter.setMappedVariableName(variableName);
						parameter.bNew = true;
						operation.add(parameter);
						operation.hasChanged = true;
					}
					return true;
				}
			}
			// MOBILE COMPONENTS
			else if (parent instanceof com.twinsoft.convertigo.beans.mobile.components.MobileComponent) {
				return pasteMobileComponent(parent, databaseObject, element);
			}
			// NGX COMPONENTS
			else if (parent instanceof com.twinsoft.convertigo.beans.ngx.components.MobileComponent) {
				return pasteNgxComponent(parent, databaseObject, element);
			}
		}
		return false;
	}

	private boolean pasteMobileComponent(DatabaseObject parent, DatabaseObject databaseObject, Element element) throws EngineException {
		// MOBILE COMPONENTS
		if (parent instanceof com.twinsoft.convertigo.beans.mobile.components.MobileComponent) {

			if (parent.priority == databaseObject.priority) {
				return true;
			}

			// Case dbo is a Sequence
			if (databaseObject instanceof Sequence) {
				Sequence sequence = (Sequence)databaseObject;

				// Add child components to fill the form
				if (parent instanceof com.twinsoft.convertigo.beans.mobile.components.UIForm) {
					com.twinsoft.convertigo.beans.mobile.components.UIForm uiForm = GenericUtils.cast(parent);
					try {
						String projectName = ((Element)element.getElementsByTagName("project").item(0)).getAttribute("name");

						// add an onSubmit event with a callSequence
						com.twinsoft.convertigo.beans.mobile.components.UIControlEvent event = new com.twinsoft.convertigo.beans.mobile.components.UIControlEvent();
						event.setEventName(com.twinsoft.convertigo.beans.mobile.components.UIControlEvent.AttrEvent.onSubmit.name());
						event.bNew = true;
						event.hasChanged = true;

						DatabaseObject call = com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.createBean(com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.getComponentByName("CallSequenceAction"));
						if (call != null && call instanceof com.twinsoft.convertigo.beans.mobile.components.UIDynamicAction) {
							com.twinsoft.convertigo.beans.mobile.components.UIDynamicAction dynAction = GenericUtils.cast(call);
							com.twinsoft.convertigo.beans.mobile.components.dynamic.IonBean ionBean = dynAction.getIonBean();
							if (ionBean != null && ionBean.hasProperty("requestable")) {
								ionBean.setPropertyValue("requestable", new com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType(projectName + "." + sequence.getName()));
							}
						}
						call.bNew = true;
						call.hasChanged = true;

						event.add(call);

						// add a list of item with label & input for each variable
						DatabaseObject dboList = com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.createBean(com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.getComponentByName("List"));
						for (RequestableVariable variable: sequence.getVariables()) {
							DatabaseObject dboItem = com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.createBean(com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.getComponentByName("ListItem"));
							dboList.add(dboItem);

							DatabaseObject dboLabel = com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.createBean(com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.getComponentByName("Label"));
							dboItem.add(dboLabel);

							com.twinsoft.convertigo.beans.mobile.components.UIText uiText = new com.twinsoft.convertigo.beans.mobile.components.UIText();
							uiText.bNew = true;
							uiText.hasChanged = true;
							uiText.setTextSmartType(new com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType(variable.getName()+":"));
							dboLabel.add(uiText);

							DatabaseObject dboInput = com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.createBean(com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.getComponentByName("Input"));
							if (dboInput != null && dboInput instanceof com.twinsoft.convertigo.beans.mobile.components.UIDynamicElement) {
								com.twinsoft.convertigo.beans.mobile.components.UIDynamicElement dynElem = GenericUtils.cast(dboInput);
								com.twinsoft.convertigo.beans.mobile.components.dynamic.IonBean ionBean = dynElem.getIonBean();
								if (ionBean != null && ionBean.hasProperty("FormControlName")) {
									ionBean.setPropertyValue("FormControlName", new com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType(variable.getName()));
								}
								dboItem.add(dboInput);
							}
						}

						// add a buttonset with a submit and a reset button
						DatabaseObject dboBtnSet = com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.createBean(com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.getComponentByName("ButtonSet"));

						DatabaseObject dboSubmit = com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.createBean(com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.getComponentByName("SubmitButton"));
						dboBtnSet.add(dboSubmit);
						com.twinsoft.convertigo.beans.mobile.components.UIText sText = new com.twinsoft.convertigo.beans.mobile.components.UIText();
						sText.bNew = true;
						sText.hasChanged = true;
						sText.setTextSmartType(new com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType("Submit"));
						dboSubmit.add(sText);

						DatabaseObject dboReset = com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.createBean(com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.getComponentByName("ResetButton"));
						dboBtnSet.add(dboReset);
						com.twinsoft.convertigo.beans.mobile.components.UIText rText = new com.twinsoft.convertigo.beans.mobile.components.UIText();
						rText.bNew = true;
						rText.hasChanged = true;
						rText.setTextSmartType(new com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType("Reset"));
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
				// Add a CallSequenceAction
				if (parent instanceof com.twinsoft.convertigo.beans.mobile.components.UIPageEvent ||
						parent instanceof com.twinsoft.convertigo.beans.mobile.components.UIAppEvent ||
						parent instanceof com.twinsoft.convertigo.beans.mobile.components.UIActionEvent ||
						parent instanceof com.twinsoft.convertigo.beans.mobile.components.UIControlEvent ||
						parent instanceof com.twinsoft.convertigo.beans.mobile.components.IAction ||
						parent instanceof com.twinsoft.convertigo.beans.mobile.components.UIActionStack
						) {
					com.twinsoft.convertigo.beans.mobile.components.UIComponent uiComponent = GenericUtils.cast(parent);
					String projectName = ((Element)element.getElementsByTagName("project").item(0)).getAttribute("name");

					DatabaseObject call = com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.createBean(com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.getComponentByName("CallSequenceAction"));
					if (call != null && call instanceof com.twinsoft.convertigo.beans.mobile.components.UIDynamicAction) {
						com.twinsoft.convertigo.beans.mobile.components.UIDynamicAction dynAction = GenericUtils.cast(call);
						com.twinsoft.convertigo.beans.mobile.components.dynamic.IonBean ionBean = dynAction.getIonBean();
						if (ionBean != null && ionBean.hasProperty("requestable")) {
							ionBean.setPropertyValue("requestable", new com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType(projectName + "." + sequence.getName()));
							call.bNew = true;
							call.hasChanged = true;

							uiComponent.add(call);
							uiComponent.hasChanged = true;
						}
					}
					return true;
				}
			}
			// Case dbo is a SharedAction
			else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIActionStack) {
				com.twinsoft.convertigo.beans.mobile.components.UIActionStack stack = GenericUtils.cast(databaseObject);

				// Add an InvokeAction
				if (parent instanceof com.twinsoft.convertigo.beans.mobile.components.UIPageEvent ||
						parent instanceof com.twinsoft.convertigo.beans.mobile.components.UIAppEvent ||
						parent instanceof com.twinsoft.convertigo.beans.mobile.components.UIActionEvent ||
						parent instanceof com.twinsoft.convertigo.beans.mobile.components.UIControlEvent ||
						parent instanceof com.twinsoft.convertigo.beans.mobile.components.IAction ||
						parent instanceof com.twinsoft.convertigo.beans.mobile.components.UIActionStack
						) {
					com.twinsoft.convertigo.beans.mobile.components.UIComponent uiComponent = GenericUtils.cast(parent);

					String projectName = ((Element)element.getElementsByTagName("project").item(0)).getAttribute("name");
					String mobileAppName = ((Element)element.getElementsByTagName("mobileapplication").item(0)).getAttribute("name");
					String applicationName = ((Element)element.getElementsByTagName("application").item(0)).getAttribute("name");

					DatabaseObject invokeAction = com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.createBean(com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.getComponentByName("InvokeAction"));
					com.twinsoft.convertigo.beans.mobile.components.UIDynamicInvoke invoke = GenericUtils.cast(invokeAction);
					if (invoke != null) {
						invoke.setSharedActionQName(projectName + "." + mobileAppName + "." +  applicationName + "." + stack.getName());
						invoke.bNew = true;
						invoke.hasChanged = true;

						uiComponent.add(invoke);
						uiComponent.hasChanged = true;
					}
					return true;
				}
			}
			// Case dbo is a SharedComponent
			else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UISharedComponent) {
				com.twinsoft.convertigo.beans.mobile.components.UISharedComponent usc = GenericUtils.cast(databaseObject);

				// Add a UseShared component
				if (parent instanceof  com.twinsoft.convertigo.beans.mobile.components.PageComponent ||
						parent instanceof com.twinsoft.convertigo.beans.mobile.components.UISharedComponent ||
						parent instanceof com.twinsoft.convertigo.beans.mobile.components.UIElement &&
						!(parent instanceof com.twinsoft.convertigo.beans.mobile.components.UIUseShared)
						) {
					com.twinsoft.convertigo.beans.mobile.components.MobileComponent mc = GenericUtils.cast(parent);

					String projectName = ((Element)element.getElementsByTagName("project").item(0)).getAttribute("name");
					String mobileAppName = ((Element)element.getElementsByTagName("mobileapplication").item(0)).getAttribute("name");
					String applicationName = ((Element)element.getElementsByTagName("application").item(0)).getAttribute("name");

					com.twinsoft.convertigo.beans.mobile.components.UIUseShared use = new com.twinsoft.convertigo.beans.mobile.components.UIUseShared();
					if (use != null) {
						use.setSharedComponentQName(projectName + "." + mobileAppName + "." +  applicationName + "." + usc.getName());
						use.bNew = true;
						use.hasChanged = true;

						mc.add(use);
						mc.hasChanged = true;
					}
					return true;
				}
			}
		}
		return false;
	}

	private boolean pasteNgxComponent(DatabaseObject parent, DatabaseObject databaseObject, Element element) throws EngineException {
		// NGX COMPONENTS
		if (parent instanceof com.twinsoft.convertigo.beans.ngx.components.MobileComponent) {

			if (parent.priority == databaseObject.priority) {
				return true;
			}

			// Case dbo is a Sequence
			if (databaseObject instanceof Sequence) {
				Sequence sequence = (Sequence)databaseObject;

				// Add child components to fill the form
				if (parent instanceof com.twinsoft.convertigo.beans.ngx.components.UIForm) {
					com.twinsoft.convertigo.beans.ngx.components.UIForm uiForm = GenericUtils.cast(parent);
					try {
						String projectName = ((Element)element.getElementsByTagName("project").item(0)).getAttribute("name");

						// add an onSubmit event with a callSequence
						com.twinsoft.convertigo.beans.ngx.components.UIControlEvent event = new com.twinsoft.convertigo.beans.ngx.components.UIControlEvent();
						event.setEventName(com.twinsoft.convertigo.beans.ngx.components.UIControlEvent.AttrEvent.onSubmit.name());
						event.bNew = true;
						event.hasChanged = true;

						DatabaseObject call = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.createBean(com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.getComponentByName("CallSequenceAction"));
						if (call != null && call instanceof com.twinsoft.convertigo.beans.ngx.components.UIDynamicAction) {
							com.twinsoft.convertigo.beans.ngx.components.UIDynamicAction dynAction = GenericUtils.cast(call);
							com.twinsoft.convertigo.beans.ngx.components.dynamic.IonBean ionBean = dynAction.getIonBean();
							if (ionBean != null && ionBean.hasProperty("requestable")) {
								ionBean.setPropertyValue("requestable", new com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType(projectName + "." + sequence.getName()));
							}
						}
						call.bNew = true;
						call.hasChanged = true;

						event.add(call);

						// add a list of item with label & input for each variable
						DatabaseObject dboList = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.createBean(com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.getComponentByName("List"));
						for (RequestableVariable variable: sequence.getVariables()) {
							DatabaseObject dboItem = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.createBean(com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.getComponentByName("ListItem"));
							dboList.add(dboItem);

							DatabaseObject dboLabel = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.createBean(com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.getComponentByName("Label"));
							dboItem.add(dboLabel);

							com.twinsoft.convertigo.beans.ngx.components.UIText uiText = new com.twinsoft.convertigo.beans.ngx.components.UIText();
							uiText.bNew = true;
							uiText.hasChanged = true;
							uiText.setTextSmartType(new com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType(variable.getName()+":"));
							dboLabel.add(uiText);

							DatabaseObject dboInput = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.createBean(com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.getComponentByName("Input"));
							if (dboInput != null && dboInput instanceof com.twinsoft.convertigo.beans.ngx.components.UIDynamicElement) {
								com.twinsoft.convertigo.beans.ngx.components.UIDynamicElement dynElem = GenericUtils.cast(dboInput);
								com.twinsoft.convertigo.beans.ngx.components.dynamic.IonBean ionBean = dynElem.getIonBean();
								if (ionBean != null && ionBean.hasProperty("FormControlName")) {
									ionBean.setPropertyValue("FormControlName", new com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType(variable.getName()));
								}
								dboItem.add(dboInput);
							}
						}

						// add a buttonset with a submit and a reset button
						DatabaseObject dboBtnSet = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.createBean(com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.getComponentByName("ButtonSet"));

						DatabaseObject dboSubmit = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.createBean(com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.getComponentByName("SubmitButton"));
						dboBtnSet.add(dboSubmit);
						com.twinsoft.convertigo.beans.ngx.components.UIText sText = new com.twinsoft.convertigo.beans.ngx.components.UIText();
						sText.bNew = true;
						sText.hasChanged = true;
						sText.setTextSmartType(new com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType("Submit"));
						dboSubmit.add(sText);

						DatabaseObject dboReset = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.createBean(com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.getComponentByName("ResetButton"));
						dboBtnSet.add(dboReset);
						com.twinsoft.convertigo.beans.ngx.components.UIText rText = new com.twinsoft.convertigo.beans.ngx.components.UIText();
						rText.bNew = true;
						rText.hasChanged = true;
						rText.setTextSmartType(new com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType("Reset"));
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
				// Add a CallSequenceAction
				if (parent instanceof com.twinsoft.convertigo.beans.ngx.components.UIPageEvent ||
						parent instanceof com.twinsoft.convertigo.beans.ngx.components.UISharedComponentEvent ||
						parent instanceof com.twinsoft.convertigo.beans.ngx.components.UIAppEvent ||
						parent instanceof com.twinsoft.convertigo.beans.ngx.components.UIActionEvent ||
						parent instanceof com.twinsoft.convertigo.beans.ngx.components.UIControlEvent ||
						parent instanceof com.twinsoft.convertigo.beans.ngx.components.IAction ||
						parent instanceof com.twinsoft.convertigo.beans.ngx.components.UIActionStack
						) {
					com.twinsoft.convertigo.beans.ngx.components.UIComponent uiComponent = GenericUtils.cast(parent);
					String projectName = ((Element)element.getElementsByTagName("project").item(0)).getAttribute("name");

					DatabaseObject call = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.createBean(com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.getComponentByName("CallSequenceAction"));
					if (call != null && call instanceof com.twinsoft.convertigo.beans.ngx.components.UIDynamicAction) {
						com.twinsoft.convertigo.beans.ngx.components.UIDynamicAction dynAction = GenericUtils.cast(call);
						com.twinsoft.convertigo.beans.ngx.components.dynamic.IonBean ionBean = dynAction.getIonBean();
						if (ionBean != null && ionBean.hasProperty("requestable")) {
							ionBean.setPropertyValue("requestable", new com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType(projectName + "." + sequence.getName()));
							call.bNew = true;
							call.hasChanged = true;

							uiComponent.add(call);
							uiComponent.hasChanged = true;
						}
					}
					return true;
				}
			}
			// Case dbo is a SharedAction
			else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIActionStack) {
				com.twinsoft.convertigo.beans.ngx.components.UIActionStack stack = GenericUtils.cast(databaseObject);

				// Add an InvokeAction
				if (parent instanceof com.twinsoft.convertigo.beans.ngx.components.UIPageEvent ||
						parent instanceof com.twinsoft.convertigo.beans.ngx.components.UISharedComponentEvent ||
						parent instanceof com.twinsoft.convertigo.beans.ngx.components.UIAppEvent ||
						parent instanceof com.twinsoft.convertigo.beans.ngx.components.UIActionEvent ||
						parent instanceof com.twinsoft.convertigo.beans.ngx.components.UIControlEvent ||
						parent instanceof com.twinsoft.convertigo.beans.ngx.components.IAction ||
						parent instanceof com.twinsoft.convertigo.beans.ngx.components.UIActionStack
						) {
					com.twinsoft.convertigo.beans.ngx.components.UIComponent uiComponent = GenericUtils.cast(parent);

					String projectName = ((Element)element.getElementsByTagName("project").item(0)).getAttribute("name");
					String mobileAppName = ((Element)element.getElementsByTagName("mobileapplication").item(0)).getAttribute("name");
					String applicationName = ((Element)element.getElementsByTagName("application").item(0)).getAttribute("name");

					DatabaseObject invokeAction = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.createBean(com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.getComponentByName("InvokeAction"));
					com.twinsoft.convertigo.beans.ngx.components.UIDynamicInvoke invoke = GenericUtils.cast(invokeAction);
					if (invoke != null) {
						invoke.setSharedActionQName(projectName + "." + mobileAppName + "." +  applicationName + "." + stack.getName());
						invoke.bNew = true;
						invoke.hasChanged = true;

						uiComponent.add(invoke);
						uiComponent.hasChanged = true;
					}
					return true;
				}
			}
			// Case dbo is a SharedComponent
			else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UISharedComponent) {
				com.twinsoft.convertigo.beans.ngx.components.UISharedComponent usc = GenericUtils.cast(databaseObject);

				// Add a UseShared component
				if (parent instanceof  com.twinsoft.convertigo.beans.ngx.components.PageComponent ||
						parent instanceof com.twinsoft.convertigo.beans.ngx.components.UISharedComponent ||
						parent instanceof com.twinsoft.convertigo.beans.ngx.components.UIElement &&
						!(parent instanceof com.twinsoft.convertigo.beans.ngx.components.UIUseShared)
						) {
					com.twinsoft.convertigo.beans.ngx.components.MobileComponent mc = GenericUtils.cast(parent);

					String projectName = ((Element)element.getElementsByTagName("project").item(0)).getAttribute("name");
					String mobileAppName = ((Element)element.getElementsByTagName("mobileapplication").item(0)).getAttribute("name");
					String applicationName = ((Element)element.getElementsByTagName("application").item(0)).getAttribute("name");

					com.twinsoft.convertigo.beans.ngx.components.UIUseShared use = new com.twinsoft.convertigo.beans.ngx.components.UIUseShared();
					if (use != null) {
						String compQName = projectName + "." + mobileAppName + "." +  applicationName + "." + usc.getName();
						use.setSharedComponentQName(compQName);
						use.bNew = true;
						use.hasChanged = true;

						mc.add(use);
						mc.hasChanged = true;
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
		if (MobileSourceTransfer.getInstance().isSupportedType(transferType)) {
			MobileSource mobileSource = MobileSourceTransfer.getInstance().getMobileSource();
			if (mobileSource != null) {
				if (target instanceof MobileUIComponentTreeObject) {
					MobileUIComponentTreeObject mcto = GenericUtils.cast(target);

					com.twinsoft.convertigo.beans.mobile.components.MobileSmartSource mss = com.twinsoft.convertigo.beans.mobile.components.MobileSmartSource.valueOf(mobileSource.getJsonString());
					if (mss == null || !mss.isDroppableInto(mcto.getObject())) {
						return false;
					}

					for (IPropertyDescriptor descriptor : mcto.getPropertyDescriptors()) {
						if (descriptor instanceof MobileSmartSourcePropertyDescriptor) {
							if (!((MobileSmartSourcePropertyDescriptor)descriptor).isReadOnly()) {
								return true;
							}
						}
					}
				}
				if (target instanceof NgxUIComponentTreeObject) {
					NgxUIComponentTreeObject mcto = GenericUtils.cast(target);

					com.twinsoft.convertigo.beans.ngx.components.MobileSmartSource mss = com.twinsoft.convertigo.beans.ngx.components.MobileSmartSource.valueOf(mobileSource.getJsonString());
					if (mss == null || !mss.isDroppableInto(mcto.getObject())) {
						return false;
					}

					for (IPropertyDescriptor descriptor : mcto.getPropertyDescriptors()) {
						if (descriptor instanceof NgxSmartSourcePropertyDescriptor) {
							if (!((NgxSmartSourcePropertyDescriptor)descriptor).isReadOnly()) {
								return true;
							}
						}
					}
				}
			}
			return false;
		}
		if (TextTransfer.getInstance().isSupportedType(transferType)) {
			if (getCurrentOperation() == DND.DROP_MOVE) {
				Object targetObject = getCurrentTarget();
				Object sourceObject = getSelectedObject();
				if (targetObject != null && targetObject instanceof TreeObject) {
					TreeObject targetTreeObject = (TreeObject) targetObject;
					if (sourceObject != null && sourceObject instanceof TreeObject) {
						TreeObject sourceTreeObject = (TreeObject) sourceObject;
						boolean isFocus = sourceTreeObject.viewer.getControl().isFocusControl();
						if (isFocus && (sourceObject == targetObject || targetTreeObject.isChildOf(sourceTreeObject))) {
							return false;
						}
					}
					if (targetObject instanceof DatabaseObjectTreeObject) {
						try {
							String xmlData = TextTransfer.getInstance().nativeToJava(transferType).toString();
							List<Object> list = ConvertigoPlugin.clipboardManagerDND.read(xmlData);
							DatabaseObject databaseObject = (DatabaseObject) list.get(0);
							DatabaseObject targetDatabaseObject = ((DatabaseObjectTreeObject) target).getObject();
							if (DatabaseObjectsManager.acceptDatabaseObjects(targetDatabaseObject, databaseObject)) {
								return true;
							}
							DatabaseObject parentDatabaseObject = targetDatabaseObject.getParent();
							if (parentDatabaseObject != null && DatabaseObjectsManager.acceptDatabaseObjects(parentDatabaseObject, databaseObject)) {
								return true;
							}
							return false;
						} catch (Exception e) {
							e.printStackTrace(System.out);
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
						Long key = Long.valueOf(stepSource.getPriority());
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
			if (target instanceof TreeObject) {
				TreeObject targetTreeObject = (TreeObject) target;
				PaletteSource paletteSource = PaletteSourceTransfer.getInstance().getPaletteSource();
				if (paletteSource != null) {
					try {
						String xmlData = paletteSource.getXmlData();
						List<Object> list = ConvertigoPlugin.clipboardManagerDND.read(xmlData);
						DatabaseObject databaseObject = (DatabaseObject) list.get(0);

						if (targetTreeObject instanceof ObjectsFolderTreeObject) {
							ObjectsFolderTreeObject folderTreeObject = (ObjectsFolderTreeObject) targetTreeObject;
							int folderType = 0;
							try  {
								folderType = ProjectExplorerView.getDatabaseObjectType((DatabaseObject) folderTreeObject.getFirstChild().getObject());
							} catch (Exception e) {}
							int dboType = ProjectExplorerView.getDatabaseObjectType(databaseObject);
							if (folderType != dboType && !ProjectExplorerView.folderAcceptMobileComponent(folderTreeObject.folderType, databaseObject)) {
								return false;
							}
							// continue
							targetTreeObject = folderTreeObject.getParent();
						}
						if (targetTreeObject instanceof DatabaseObjectTreeObject) {
							if (getCurrentLocation() != 3) {
								targetTreeObject = ((DatabaseObjectTreeObject) targetTreeObject).getParentDatabaseObjectTreeObject();
							}
							
							DatabaseObject targetDatabaseObject = targetTreeObject == null ? null : ((DatabaseObjectTreeObject) targetTreeObject).getObject();
							if (targetDatabaseObject != null) {
								if (!DatabaseObjectsManager.acceptDatabaseObjects(targetDatabaseObject, databaseObject)) {
									return false;
								}
								if (targetTreeObject instanceof MobileComponentTreeObject) {
									if (!com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.acceptDatabaseObjects(targetDatabaseObject, databaseObject)) {
										return false;
									}
									if (!com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.isTplCompatible(targetDatabaseObject, databaseObject)) {
										return false;
									}
								}
								if (targetTreeObject instanceof NgxComponentTreeObject) {
									if (!com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.acceptDatabaseObjects(targetDatabaseObject, databaseObject)) {
										return false;
									}
									if (!com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.isTplCompatible(targetDatabaseObject, databaseObject)) {
										return false;
									}
								}
								return true;
							}
						}
					} catch (Exception e) {
						e.printStackTrace(System.out);
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
				if (targetTreeObject instanceof ObjectsFolderTreeObject) {
					ObjectsFolderTreeObject folderTreeObject = (ObjectsFolderTreeObject)targetTreeObject;
					targetTreeObject = folderTreeObject.getParent();
				}

				if (targetTreeObject instanceof DatabaseObjectTreeObject) {
					boolean insertBefore = (feedback & DND.FEEDBACK_INSERT_BEFORE) != 0;
					boolean insertAfter = (feedback & DND.FEEDBACK_INSERT_AFTER) != 0;
					DatabaseObjectTreeObject dbotree = (DatabaseObjectTreeObject) targetTreeObject;
					DatabaseObject parent = (insertBefore || insertAfter) ? dbotree.getObject().getParent() : dbotree.getObject();
					
					String xmlData = ((PaletteSource)data).getXmlData();
					Document document = XMLUtils.getDefaultDocumentBuilder().parse(new InputSource(new StringReader(xmlData)));
					Element rootElement = document.getDocumentElement();
					NodeList nodeList = rootElement.getChildNodes();
					boolean needNgxPaletteReload = false;
					int len = nodeList.getLength();
					Node node;
					// Special objects move from palette
					if (detail == DND.DROP_MOVE) {
						DatabaseObject dbop = null;
						for (int i = 0 ; i < len ; i++) {
							node = (Node) nodeList.item(i);
							if (node.getNodeType() != Node.TEXT_NODE) {
								try {
									Element prop = (Element) TwsCachedXPathAPI.getInstance().selectNode(node, "property[@name='name']/*");
									String name = prop.getAttribute("value");
									name = explorerView.edit(targetTreeObject, name);
									prop.setAttribute("value", name);
								} catch (Exception e) {
									Engine.logStudio.debug("Cannot rename on drop", e);
								}
								dbop = paste(node, parent, true);
								if (dbop == null) {
									throw new Exception();
								}
								if (dbop instanceof com.twinsoft.convertigo.beans.ngx.components.UIActionStack ||
										dbop instanceof com.twinsoft.convertigo.beans.ngx.components.UISharedRegularComponent) {
									needNgxPaletteReload = true;
								}
							}
						}
						
						if (dbop != null) {
							NewObjectWizard.afterBeanAdded(dbop, parent, parent.hasChanged);
						}
						
						if (insertBefore || insertAfter) {
							reloadTreeObject(explorerView, dbotree.getParentDatabaseObjectTreeObject());
							explorerView.moveLastTo(dbotree.getParent(), dbotree, insertBefore);
						} else {
							reloadTreeObject(explorerView, dbotree);
						}
						
						// Refresh ngx palette view
						if (needNgxPaletteReload) {
							ConvertigoPlugin.getDefault().refreshPaletteView();
						}
						if (dbop != null) {
							explorerView.setSelectedTreeObject(dbotree.findTreeObjectByUserObject(dbop));
						}
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

					MobileUIComponentTreeObject mcto = GenericUtils.cast(targetTreeObject);
					for (IPropertyDescriptor descriptor : mcto.getPropertyDescriptors()) {
						if (descriptor instanceof MobileSmartSourcePropertyDescriptor) {
							MobileSmartSourcePropertyDescriptor cspd = GenericUtils.cast(descriptor);
							if (!cspd.isReadOnly()) {
								String propertyName = (String) cspd.getId();
								String propertyLabel = (String) cspd.getDisplayName();
								MenuItem itemCheck = new MenuItem(dropMenu, SWT.NONE);
								itemCheck.setText(propertyLabel);
								itemCheck.addSelectionListener(new SelectionListener() {
									@Override
									public void widgetSelected(SelectionEvent e) {
										com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType cst = new com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType();
										cst.setMode(com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType.Mode.SOURCE);
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
				if (targetTreeObject instanceof NgxUIComponentTreeObject) {
					Shell shell = ConvertigoPlugin.getMainShell();
					Menu dropMenu = new Menu(shell, SWT.POP_UP);
					shell.setMenu(dropMenu);

					NgxUIComponentTreeObject mcto = GenericUtils.cast(targetTreeObject);
					for (IPropertyDescriptor descriptor : mcto.getPropertyDescriptors()) {
						if (descriptor instanceof NgxSmartSourcePropertyDescriptor) {
							NgxSmartSourcePropertyDescriptor cspd = GenericUtils.cast(descriptor);
							if (!cspd.isReadOnly()) {
								String propertyName = (String) cspd.getId();
								String propertyLabel = (String) cspd.getDisplayName();
								MenuItem itemCheck = new MenuItem(dropMenu, SWT.NONE);
								itemCheck.setText(propertyLabel);
								itemCheck.addSelectionListener(new SelectionListener() {
									@Override
									public void widgetSelected(SelectionEvent e) {
										com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType cst = new com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType();
										cst.setMode(com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType.Mode.SOURCE);
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
