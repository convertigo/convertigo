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

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepEvent;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.steps.AttributeStep;
import com.twinsoft.convertigo.beans.steps.ElementStep;
import com.twinsoft.convertigo.beans.steps.JsonFieldStep;
import com.twinsoft.convertigo.beans.steps.SmartType;
import com.twinsoft.convertigo.beans.steps.SmartType.Mode;
import com.twinsoft.convertigo.beans.steps.XMLAttributeStep;
import com.twinsoft.convertigo.beans.steps.XMLElementStep;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.StepTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.JsonFieldType;
import com.twinsoft.convertigo.engine.studio.ActionModel;
import com.twinsoft.convertigo.engine.studio.DatabaseObjectsAction;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class ChangeToElementStepAction extends MyAbstractAction {

	public ChangeToElementStepAction() {
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

				// Attribute
				if ((databaseObject != null) && (databaseObject instanceof AttributeStep)) {
					AttributeStep attributeStep = (AttributeStep)databaseObject;

					TreeParent treeParent = treeObject.getParent();
					DatabaseObjectTreeObject parentTreeObject = null;
					if (treeParent instanceof DatabaseObjectTreeObject)
						parentTreeObject = (DatabaseObjectTreeObject)treeParent;
					else
						parentTreeObject = (DatabaseObjectTreeObject)treeParent.getParent();

					if (parentTreeObject != null) {

						// New Element step
						ElementStep jelementStep = new ElementStep();

						if ( DatabaseObjectsManager.acceptDatabaseObjects(attributeStep.getParent(), jelementStep) ) {
							// Set properties
							jelementStep.setOutput(attributeStep.isOutput());
							jelementStep.setEnabled(attributeStep.isEnabled());
							jelementStep.setComment(attributeStep.getComment());
							jelementStep.setExpression(attributeStep.getExpression());
							jelementStep.setNodeText(attributeStep.getNodeText());
							jelementStep.setNodeName(attributeStep.getNodeName());

							jelementStep.bNew = true;
							jelementStep.hasChanged = true;

							// Add new Element step to parent
							DatabaseObject parentDbo = attributeStep.getParent();

							parentDbo.add(jelementStep);

							// Set correct order
							if (parentDbo instanceof StepWithExpressions)
								((StepWithExpressions)parentDbo).insertAtOrder(jelementStep,attributeStep.priority);
							else if (parentDbo instanceof Sequence)
								((Sequence)parentDbo).insertAtOrder(jelementStep,attributeStep.priority);

							// Add new Element step in Tree
							StepTreeObject stepTreeObject = new StepTreeObject(explorerView.viewer,attributeStep);
							treeParent.addChild(stepTreeObject);

							// Delete Attribute step
							long oldPriority = attributeStep.priority;
							attributeStep.delete();
							jelementStep.getSequence().fireStepMoved(new StepEvent(jelementStep,String.valueOf(oldPriority)));

							parentTreeObject.hasBeenModified(true);
							explorerView.reloadTreeObject(parentTreeObject);
							explorerView.setSelectedTreeObject(explorerView.findTreeObjectByUserObject(jelementStep));
						} else {
							throw new EngineException("You cannot paste to a " + attributeStep.getParent().getClass().getSimpleName() + " a database object of type " + jelementStep.getClass().getSimpleName());
						}
					}
				}

				// XML Element
				if ((databaseObject != null) && (databaseObject instanceof XMLElementStep)) {
					XMLElementStep elementStep = (XMLElementStep)databaseObject;

					TreeParent treeParent = treeObject.getParent();
					DatabaseObjectTreeObject parentTreeObject = null;
					if (treeParent instanceof DatabaseObjectTreeObject)
						parentTreeObject = (DatabaseObjectTreeObject)treeParent;
					else
						parentTreeObject = (DatabaseObjectTreeObject)treeParent.getParent();

					if (parentTreeObject != null) {

						// New Element step
						ElementStep jelementStep = new ElementStep();

						if ( DatabaseObjectsManager.acceptDatabaseObjects(elementStep.getParent(), jelementStep) ) {
							// Set properties
							jelementStep.setOutput(elementStep.isOutput());
							jelementStep.setEnabled(elementStep.isEnabled());
							jelementStep.setComment(elementStep.getComment());
							//jelementStep.setSourceDefinition(elementStep.getSourceDefinition());
							jelementStep.setNodeText(elementStep.getNodeText());
							jelementStep.setNodeName(elementStep.getNodeName());

							jelementStep.bNew = true;
							jelementStep.hasChanged = true;

							// Add new XMLElement step to parent
							DatabaseObject parentDbo = elementStep.getParent();

							parentDbo.add(jelementStep);

							for (Step step : elementStep.getAllSteps()) {
								try {
									jelementStep.addStep(step);
								} catch (Throwable t) {}
							}

							// Set correct order
							if (parentDbo instanceof StepWithExpressions)
								((StepWithExpressions)parentDbo).insertAtOrder(jelementStep,elementStep.priority);
							else if (parentDbo instanceof Sequence)
								((Sequence)parentDbo).insertAtOrder(jelementStep,elementStep.priority);

							// Add new Element step in Tree
							StepTreeObject stepTreeObject = new StepTreeObject(explorerView.viewer,elementStep);
							treeParent.addChild(stepTreeObject);

							// Delete XMLAttribute step
							long oldPriority = elementStep.priority;
							elementStep.delete();
							jelementStep.getSequence().fireStepMoved(new StepEvent(jelementStep,String.valueOf(oldPriority)));

							parentTreeObject.hasBeenModified(true);
							explorerView.reloadTreeObject(parentTreeObject);
							explorerView.setSelectedTreeObject(explorerView.findTreeObjectByUserObject(jelementStep));
						} else {
							throw new EngineException("You cannot paste to a " + elementStep.getParent().getClass().getSimpleName() + " a database object of type " + jelementStep.getClass().getSimpleName());
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
						ElementStep elementStep = new ElementStep();

						if ( DatabaseObjectsManager.acceptDatabaseObjects(jsonFieldStep.getParent(), elementStep) ) {
							// Set properties
							elementStep.setOutput(jsonFieldStep.isOutput());
							elementStep.setEnabled(jsonFieldStep.isEnabled());
							elementStep.setComment(jsonFieldStep.getComment());
							SmartType value = jsonFieldStep.getValue();
							if (value.getMode() == Mode.JS) {
								elementStep.setExpression(jsonFieldStep.getValue().toStringContent());
							} else if (value.getMode() == Mode.PLAIN) {
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
			ConvertigoPlugin.logException(e, "Unable to change step to Element step!");
		}
		finally {
			shell.setCursor(null);
			waitCursor.dispose();
		}
	}
}
