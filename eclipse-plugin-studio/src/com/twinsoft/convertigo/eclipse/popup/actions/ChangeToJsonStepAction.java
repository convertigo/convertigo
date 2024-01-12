/*
 * Copyright (c) 2001-2024 Convertigo SA.
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
import com.twinsoft.convertigo.beans.steps.JsonArrayStep;
import com.twinsoft.convertigo.beans.steps.JsonFieldStep;
import com.twinsoft.convertigo.beans.steps.JsonObjectStep;
import com.twinsoft.convertigo.beans.steps.SmartType;
import com.twinsoft.convertigo.beans.steps.SmartType.Mode;
import com.twinsoft.convertigo.beans.steps.XMLAttributeStep;
import com.twinsoft.convertigo.beans.steps.XMLComplexStep;
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

public class ChangeToJsonStepAction extends MyAbstractAction {

	public ChangeToJsonStepAction() {
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

				// XML Concat step
				if ((databaseObject != null) && (databaseObject instanceof Step)) {
					Step oldStep = (Step) databaseObject;

					TreeParent treeParent = treeObject.getParent();
					DatabaseObjectTreeObject parentTreeObject = null;
					if (treeParent instanceof DatabaseObjectTreeObject)
						parentTreeObject = (DatabaseObjectTreeObject)treeParent;
					else
						parentTreeObject = (DatabaseObjectTreeObject)treeParent.getParent();

					if (parentTreeObject != null) {
						// New XMLElementStep step
						String id = getActionId();
						Step newStep = null;
						if (id.endsWith("changeToArray")) {
							newStep = new JsonArrayStep();
						} else if (id.endsWith("changeToObject")) {
							newStep = new JsonObjectStep();
						} else if (id.endsWith("changeToField")) {
							newStep = new JsonFieldStep();
						} else if (id.endsWith("changeToComplex")) {
							newStep = new XMLComplexStep();
						}
						
						if (newStep != null) {
							if (DatabaseObjectsManager.acceptDatabaseObjects(oldStep.getParent(), newStep)) {
								newStep.setOutput(oldStep.isOutput());
								newStep.setEnabled(oldStep.isEnabled());
								newStep.setComment(oldStep.getComment());
								
								newStep.bNew = true;
								newStep.hasChanged = true;
								
								DatabaseObject parentDbo = oldStep.getParent();
								parentDbo.add(newStep);
								
								SmartType key = null;
								
								if (newStep instanceof JsonArrayStep && oldStep instanceof JsonObjectStep) {
									((JsonArrayStep) newStep).setKey(((JsonObjectStep) oldStep).getKey());
								} else if (newStep instanceof JsonObjectStep && oldStep instanceof JsonArrayStep) {
									((JsonObjectStep) newStep).setKey(((JsonArrayStep) oldStep).getKey());
								} else {
									try {
										key = (SmartType) newStep.getClass().getMethod("getKey").invoke(newStep);
										key.setExpression(oldStep.getStepNodeName());
									} catch (Exception e) {
									}
								}
								
								if (newStep instanceof XMLComplexStep) {
									XMLComplexStep complex = (XMLComplexStep) newStep;
									try {
										XMLAttributeStep attr = new XMLAttributeStep();
										attr.setOutput(oldStep.isOutput());
										attr.setEnabled(oldStep.isEnabled());
										attr.setName("type");
										attr.setNodeName("type");
										attr.setNodeText(oldStep instanceof JsonArrayStep ? "array" : "object");
										complex.addStep(attr);
										key = (SmartType) oldStep.getClass().getMethod("getKey").invoke(oldStep);
										String nodeName = key.toStringContent();
										String normalized = StringUtils.normalize(nodeName);
										complex.setNodeName(normalized);
										if (!normalized.equals(nodeName)) {
											attr = new XMLAttributeStep();
											attr.setOutput(oldStep.isOutput());
											attr.setEnabled(oldStep.isEnabled());
											attr.setName("originalKeyName");
											attr.setNodeName("originalKeyName");
											attr.setNodeText(nodeName);
											complex.addStep(attr);
										}
									} catch (Exception e) {
									}
								}
								
								if (newStep instanceof JsonFieldStep) {
									SmartType val = ((JsonFieldStep) newStep).getValue();
									if (oldStep instanceof XMLElementStep) {
										XMLVector<String> src = ((XMLElementStep) oldStep).getSourceDefinition();
										if (src.size() > 0) {
											val.setSourceDefinition(src);
											val.setMode(Mode.SOURCE);
										} else {
											val.setExpression(((XMLElementStep) oldStep).getNodeText());
										}
									} else if (oldStep instanceof ElementStep) {
										String exp = ((ElementStep) oldStep).getExpression();
										if (org.apache.commons.lang3.StringUtils.isNotBlank(exp)) {
											val.setExpression(((ElementStep) oldStep).getExpression());
											val.setMode(Mode.JS);
										} else {
											val.setExpression(((ElementStep) oldStep).getNodeText());
										}
									}
								}

								for (DatabaseObject child: oldStep.getAllChildren()) {
									try {
										if (child instanceof XMLAttributeStep) {
											XMLAttributeStep attr = (XMLAttributeStep) child;
											String name = attr.getNodeName();
											if ("originalKeyName".equals(name)) {
												String val = attr.getNodeText();
												if (key != null && !val.isBlank()) {
													key.setExpression(val);
												}
												continue;
											} else if ("type".equals(name)) {
												String val = attr.getNodeText();
												if (newStep instanceof JsonFieldStep && !val.isBlank()) {
													((JsonFieldStep) newStep).setType(JsonFieldType.parse(val));
												}
												continue;
											}
										}
										if (newStep instanceof StepWithExpressions) {
											newStep.add(child);
										}
									} catch (Throwable t) {}
								}
								
								if (parentDbo instanceof StepWithExpressions) {
									((StepWithExpressions) parentDbo).insertAtOrder(newStep, oldStep.priority);
								} else if (parentDbo instanceof Sequence) {
									((Sequence) parentDbo).insertAtOrder(newStep, oldStep.priority);
								}

								StepTreeObject stepTreeObject = new StepTreeObject(explorerView.viewer, newStep);
								treeParent.addChild(stepTreeObject);

								long oldPriority = oldStep.priority;
								oldStep.delete();
								String beanName = oldStep.getName();
								newStep.setName(beanName);
								newStep.getSequence().fireStepMoved(new StepEvent(newStep, String.valueOf(oldPriority)));

								parentTreeObject.hasBeenModified(true);
								explorerView.reloadTreeObject(parentTreeObject);
								explorerView.setSelectedTreeObject(explorerView.findTreeObjectByUserObject(newStep));
							} else {
								throw new EngineException("You cannot paste to a " + oldStep.getParent().getClass().getSimpleName() + " a database object of type " + newStep.getClass().getSimpleName());
							}
						}
					}
				}
			}
		}
		catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to change step to JSON step!");
		}
		finally {
			shell.setCursor(null);
			waitCursor.dispose();
		}
	}
}
