/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.popup.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepEvent;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.steps.ElementStep;
import com.twinsoft.convertigo.beans.steps.JsonFieldStep;
import com.twinsoft.convertigo.beans.steps.SmartType;
import com.twinsoft.convertigo.beans.steps.XMLAttributeStep;
import com.twinsoft.convertigo.beans.steps.XMLConcatStep;
import com.twinsoft.convertigo.beans.steps.XMLElementStep;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.StepTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.JsonFieldType;
import com.twinsoft.convertigo.engine.studio.ActionModel;
import com.twinsoft.convertigo.engine.studio.DatabaseObjectsAction;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class ChangeToXMLElementStepAction extends MyAbstractAction {

	public ChangeToXMLElementStepAction() {
	}

	public void selectionChanged(IAction action, ISelection selection) {
		try {
			boolean enable = false;
			super.selectionChanged(action, selection);
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			TreeObject treeObject = (TreeObject) structuredSelection.getFirstElement();
			if (treeObject instanceof DatabaseObjectTreeObject) {
				DatabaseObject dbo = (DatabaseObject) treeObject.getObject();
				ActionModel actionModel = DatabaseObjectsAction.selectionChanged(getClass().getName(), dbo);
				enable = actionModel.isEnabled;
			}
			action.setEnabled(enable);
		}
		catch (Exception e) {}
	}

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.eclipse.popup.actions.MyAbstractAction#run()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);

		Shell shell = getParentShell();
		shell.setCursor(waitCursor);

		try {
			ProjectExplorerView explorerView = getProjectExplorerView();
			if (explorerView != null) {
				TreeObject treeObject = explorerView.getFirstSelectedTreeObject();
				Object databaseObject = treeObject.getObject();

				// XML Concat step
				if ((databaseObject != null) && (databaseObject instanceof XMLConcatStep)) {
					XMLConcatStep concatStep = (XMLConcatStep)databaseObject;

					TreeParent treeParent = treeObject.getParent();
					DatabaseObjectTreeObject parentTreeObject = null;
					if (treeParent instanceof DatabaseObjectTreeObject)
						parentTreeObject = (DatabaseObjectTreeObject)treeParent;
					else
						parentTreeObject = (DatabaseObjectTreeObject)treeParent.getParent();

					if (parentTreeObject != null) {
						// New XMLElementStep step
						XMLElementStep elementStep = new XMLElementStep();
						if ( DatabaseObjectsManager.acceptDatabaseObjects(concatStep.getParent(), elementStep) ) {

							if ( concatStep.getSourcesDefinition().toString().equals("[[]]") ) {
								elementStep.setSourceDefinition(new XMLVector<String>());
							} else {
								// Set properties (Default value and Source)
								XMLVector<XMLVector<Object>> sources = concatStep.getSourcesDefinition();
								XMLVector<String> sourceDefinition = new XMLVector<String>();
								String defaultValue = "";
								for ( XMLVector<Object> source : sources ) {
									if ( sources.lastElement() == source ) {
										defaultValue += source.get(2);
									} else {
										defaultValue += source.get(2) + concatStep.getSeparator();
									}
									if (sourceDefinition.toString().equals("[]")
											&& (!source.get(1).toString().equals("") && !source.get(1).toString().equals("[]") ) ) {
										sourceDefinition = (XMLVector<String>) source.get(1);
									}
								}
								elementStep.setOutput(concatStep.isOutput());
								elementStep.setEnabled(concatStep.isEnabled());
								elementStep.setComment(concatStep.getComment());
								elementStep.setNodeName(concatStep.getNodeName());
								elementStep.setNodeText(defaultValue);
								elementStep.setSourceDefinition(sourceDefinition);

							}

							elementStep.bNew = true;
							elementStep.hasChanged = true;

							// Add new XMLElementStep step to parent
							DatabaseObject parentDbo = concatStep.getParent();
							parentDbo.add(elementStep);

							// Set correct order
							if (parentDbo instanceof StepWithExpressions)
								((StepWithExpressions)parentDbo).insertAtOrder(elementStep,concatStep.priority);
							else if (parentDbo instanceof Sequence)
								((Sequence)parentDbo).insertAtOrder(elementStep,concatStep.priority);

							// Add new XMLElementStep step in Tree
							StepTreeObject stepTreeObject = new StepTreeObject(explorerView.viewer,elementStep);
							treeParent.addChild(stepTreeObject);

							// Delete XMLConcatStep step
							long oldPriority = concatStep.priority;
							concatStep.delete();
							elementStep.getSequence().fireStepMoved(new StepEvent(elementStep,String.valueOf(oldPriority)));

							parentTreeObject.hasBeenModified(true);
							explorerView.reloadTreeObject(parentTreeObject);
							explorerView.setSelectedTreeObject(explorerView.findTreeObjectByUserObject(elementStep));
						} else {
							throw new EngineException("You cannot paste to a " + concatStep.getParent().getClass().getSimpleName() + " a database object of type " + elementStep.getClass().getSimpleName());
						}
					}
				}

				// XML Attribute
				if ((databaseObject != null) && (databaseObject instanceof XMLAttributeStep)) {
					XMLAttributeStep attributeStep = (XMLAttributeStep)databaseObject;

					TreeParent treeParent = treeObject.getParent();
					DatabaseObjectTreeObject parentTreeObject = null;
					if (treeParent instanceof DatabaseObjectTreeObject)
						parentTreeObject = (DatabaseObjectTreeObject)treeParent;
					else
						parentTreeObject = (DatabaseObjectTreeObject)treeParent.getParent();

					if (parentTreeObject != null) {

						// New XMLElement step
						XMLElementStep elementStep = new XMLElementStep();

						if ( DatabaseObjectsManager.acceptDatabaseObjects(attributeStep.getParent(), elementStep) ) {
							// Set properties
							elementStep.setOutput(attributeStep.isOutput());
							elementStep.setEnabled(attributeStep.isEnabled());
							elementStep.setComment(attributeStep.getComment());
							elementStep.setSourceDefinition(attributeStep.getSourceDefinition());
							elementStep.setNodeText(attributeStep.getNodeText());
							elementStep.setNodeName(attributeStep.getNodeName());

							elementStep.bNew = true;
							elementStep.hasChanged = true;

							// Add new XMLElement step to parent
							DatabaseObject parentDbo = attributeStep.getParent();

							parentDbo.add(elementStep);

							// Set correct order
							if (parentDbo instanceof StepWithExpressions)
								((StepWithExpressions)parentDbo).insertAtOrder(elementStep,attributeStep.priority);
							else if (parentDbo instanceof Sequence)
								((Sequence)parentDbo).insertAtOrder(elementStep,attributeStep.priority);

							// Add new XMLElement step in Tree
							StepTreeObject stepTreeObject = new StepTreeObject(explorerView.viewer,attributeStep);
							treeParent.addChild(stepTreeObject);

							// Delete XMLAttribute step
							long oldPriority = attributeStep.priority;
							attributeStep.delete();
							elementStep.getSequence().fireStepMoved(new StepEvent(elementStep,String.valueOf(oldPriority)));

							parentTreeObject.hasBeenModified(true);
							explorerView.reloadTreeObject(parentTreeObject);
							explorerView.setSelectedTreeObject(explorerView.findTreeObjectByUserObject(elementStep));
						} else {
							throw new EngineException("You cannot paste to a " + attributeStep.getParent().getClass().getSimpleName() + " a database object of type " + elementStep.getClass().getSimpleName());
						}
					}
				}

				// JElement
				if ((databaseObject != null) && (databaseObject instanceof ElementStep)) {
					ElementStep jelementStep = (ElementStep)databaseObject;

					TreeParent treeParent = treeObject.getParent();
					DatabaseObjectTreeObject parentTreeObject = null;
					if (treeParent instanceof DatabaseObjectTreeObject)
						parentTreeObject = (DatabaseObjectTreeObject)treeParent;
					else
						parentTreeObject = (DatabaseObjectTreeObject)treeParent.getParent();

					if (parentTreeObject != null) {

						// New XMLElement step
						XMLElementStep elementStep = new XMLElementStep();

						if ( DatabaseObjectsManager.acceptDatabaseObjects(jelementStep.getParent(), elementStep) ) {
							// Set properties
							elementStep.setOutput(jelementStep.isOutput());
							elementStep.setEnabled(jelementStep.isEnabled());
							elementStep.setComment(jelementStep.getComment());
							//elementStep.setSourceDefinition(jelementStep.getSourceDefinition());
							elementStep.setNodeText(jelementStep.getNodeText());
							elementStep.setNodeName(jelementStep.getNodeName());

							elementStep.bNew = true;
							elementStep.hasChanged = true;

							// Add new XMLElement step to parent
							DatabaseObject parentDbo = jelementStep.getParent();

							parentDbo.add(elementStep);

							for (Step step : jelementStep.getAllSteps()) {
								try {
									elementStep.addStep(step);
								} catch (Throwable t) {}
							}

							// Set correct order
							if (parentDbo instanceof StepWithExpressions)
								((StepWithExpressions)parentDbo).insertAtOrder(elementStep,jelementStep.priority);
							else if (parentDbo instanceof Sequence)
								((Sequence)parentDbo).insertAtOrder(elementStep,jelementStep.priority);

							// Add new XMLElement step in Tree
							StepTreeObject stepTreeObject = new StepTreeObject(explorerView.viewer,jelementStep);
							treeParent.addChild(stepTreeObject);

							// Delete XMLAttribute step
							long oldPriority = jelementStep.priority;
							jelementStep.delete();
							elementStep.getSequence().fireStepMoved(new StepEvent(elementStep,String.valueOf(oldPriority)));

							parentTreeObject.hasBeenModified(true);
							explorerView.reloadTreeObject(parentTreeObject);
							explorerView.setSelectedTreeObject(explorerView.findTreeObjectByUserObject(elementStep));
						} else {
							throw new EngineException("You cannot paste to a " + jelementStep.getParent().getClass().getSimpleName() + " a database object of type " + elementStep.getClass().getSimpleName());
						}
					}
				}
				
				if ((databaseObject != null) && (databaseObject instanceof JsonFieldStep)) {
					JsonFieldStep jsonFieldStep = (JsonFieldStep)databaseObject;

					TreeParent treeParent = treeObject.getParent();
					DatabaseObjectTreeObject parentTreeObject = null;
					if (treeParent instanceof DatabaseObjectTreeObject)
						parentTreeObject = (DatabaseObjectTreeObject)treeParent;
					else
						parentTreeObject = (DatabaseObjectTreeObject)treeParent.getParent();

					if (parentTreeObject != null) {

						// New XMLElement step
						XMLElementStep elementStep = new XMLElementStep();

						if ( DatabaseObjectsManager.acceptDatabaseObjects(jsonFieldStep.getParent(), elementStep) ) {
							// Set properties
							elementStep.setOutput(jsonFieldStep.isOutput());
							elementStep.setEnabled(jsonFieldStep.isEnabled());
							elementStep.setComment(jsonFieldStep.getComment());
							SmartType v = jsonFieldStep.getValue();
							if (v.isUseSource()) {
								elementStep.setSourceDefinition(v.getSourceDefinition());
							} else {
								elementStep.setNodeText(jsonFieldStep.getValue().toStringContent());
							}
							String nodeName = jsonFieldStep.getKey().toStringContent();
							String normalized = StringUtils.normalize(nodeName);
							elementStep.setNodeName(normalized);

							elementStep.bNew = true;
							elementStep.hasChanged = true;

							// Add new XMLElement step to parent
							DatabaseObject parentDbo = jsonFieldStep.getParent();

							parentDbo.add(elementStep);
							
							JsonFieldType type = jsonFieldStep.getType();
							if (!type.equals(JsonFieldType.string)) {
								XMLAttributeStep attr = new XMLAttributeStep();
								attr.setOutput(jsonFieldStep.isOutput());
								attr.setEnabled(jsonFieldStep.isEnabled());
								attr.setName("type");
								attr.setNodeName("type");
								attr.setNodeText(type.toString());
								elementStep.addStep(attr);
							}
							
							if (!normalized.equals(nodeName)) {
								XMLAttributeStep attr = new XMLAttributeStep();
								attr.setOutput(jsonFieldStep.isOutput());
								attr.setEnabled(jsonFieldStep.isEnabled());
								attr.setName("originalKeyName");
								attr.setNodeName("originalKeyName");
								attr.setNodeText(nodeName);
								elementStep.addStep(attr);
							}

							// Set correct order
							if (parentDbo instanceof StepWithExpressions)
								((StepWithExpressions)parentDbo).insertAtOrder(elementStep,jsonFieldStep.priority);
							else if (parentDbo instanceof Sequence)
								((Sequence)parentDbo).insertAtOrder(elementStep,jsonFieldStep.priority);

							// Add new XMLElement step in Tree
							StepTreeObject stepTreeObject = new StepTreeObject(explorerView.viewer,jsonFieldStep);
							treeParent.addChild(stepTreeObject);

							// Delete XMLAttribute step
							long oldPriority = jsonFieldStep.priority;
							jsonFieldStep.delete();
							String beanName = jsonFieldStep.getName();
							elementStep.setName(beanName);
							elementStep.getSequence().fireStepMoved(new StepEvent(elementStep,String.valueOf(oldPriority)));

							parentTreeObject.hasBeenModified(true);
							explorerView.reloadTreeObject(parentTreeObject);
							explorerView.setSelectedTreeObject(explorerView.findTreeObjectByUserObject(elementStep));
						} else {
							throw new EngineException("You cannot paste to a " + jsonFieldStep.getParent().getClass().getSimpleName() + " a database object of type " + elementStep.getClass().getSimpleName());
						}
					}
				}
			}
		}
		catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to change step to XMLElement step!");
		}
		finally {
			shell.setCursor(null);
			waitCursor.dispose();
		}
	}
}
