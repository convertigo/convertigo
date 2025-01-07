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

package com.twinsoft.convertigo.eclipse.views.projectexplorer.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.PartInitException;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.SimpleStep;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.jscript.JScriptEditorInput;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;

public class StepTreeObject extends DatabaseObjectTreeObject implements INamedSourceSelectorTreeObject, IEditableTreeObject, IOrderableTreeObject {
	
	public StepTreeObject(Viewer viewer, Step object) {
		this(viewer, object, false);
	}
	
	public StepTreeObject(Viewer viewer, Step object, boolean inherited) {
		super(viewer, object, inherited);
		setEnabled(getObject().isEnabled());
	}
	
	@Override
	public Step getObject(){
		return (Step) super.getObject();
	}

	@Override
    public boolean isEnabled() {
		setEnabled(getObject().isEnabled());
    	return super.isEnabled();
    }

	@Override
	public void hasBeenModified(boolean modified) {
		super.hasBeenModified(modified);
	}
	
	@Override
	public NamedSourceSelector getNamedSourceSelector() {
		return new NamedSourceSelector() {
			@Override
			Object thisTreeObject() {
				return StepTreeObject.this;
			}
			
			@Override
			protected List<String> getPropertyNamesForSource(Class<?> c) {
				List<String> list = new ArrayList<String>();
				
				if (getObject() instanceof TransactionStep) {
					if (ProjectTreeObject.class.isAssignableFrom(c) ||
						ConnectorTreeObject.class.isAssignableFrom(c) ||
						TransactionTreeObject.class.isAssignableFrom(c))
					{
							list.add("sourceTransaction");
					}
				}
				if (getObject() instanceof SequenceStep) {
					if (ProjectTreeObject.class.isAssignableFrom(c) ||
						SequenceTreeObject.class.isAssignableFrom(c))
					{
							list.add("sourceSequence");
					}
				}
				
				return list;
			}
			
			@Override
			protected boolean isNamedSource(String propertyName) {
				if (getObject() instanceof TransactionStep) {
					return "sourceTransaction".equals(propertyName);
				}
				if (getObject() instanceof SequenceStep) {
					return "sourceSequence".equals(propertyName);
				}
				return false;
			}
			
			@Override
			public boolean isSelectable(String propertyName, Object nsObject) {
				if (getObject() instanceof TransactionStep) {
					if ("sourceTransaction".equals(propertyName)) {
						return nsObject instanceof Transaction;
					}
				}
				if (getObject() instanceof SequenceStep) {
					if ("sourceSequence".equals(propertyName)) {
						return nsObject instanceof Sequence;
					}
				}
				return false;
			}

			@Override
			protected void handleSourceCleared(String propertyName) {
				// nothing to do
			}

			@Override
			protected void handleSourceRenamed(String propertyName, String oldName, String newName) {
				if (isNamedSource(propertyName)) {
					boolean hasBeenRenamed = false;
					
					String pValue = (String) getPropertyValue(propertyName);
					if (pValue != null && pValue.startsWith(oldName)) {
						String _pValue = newName + pValue.substring(oldName.length());
						if (!pValue.equals(_pValue)) {
							if (getObject() instanceof TransactionStep) {
								if ("sourceTransaction".equals(propertyName)) {
									((TransactionStep)getObject()).setSourceTransaction(_pValue);
									hasBeenRenamed = true;
								}
							}
							if (getObject() instanceof SequenceStep) {
								if ("sourceSequence".equals(propertyName)) {
									((SequenceStep)getObject()).setSourceSequence(_pValue);
									hasBeenRenamed = true;
								}
							}
						}
					}
			
					if (hasBeenRenamed) {
						hasBeenModified(true);
						
						ConvertigoPlugin.projectManager.getProjectExplorerView().updateTreeObject(StepTreeObject.this);
						getDescriptors();// refresh editors (e.g labels in combobox)
					}
				}
			}
		};
	}
	
	@Override
	public void treeObjectAdded(TreeObjectEvent treeObjectEvent) {
		super.treeObjectAdded(treeObjectEvent);
	}

	@Override
	public void treeObjectRemoved(TreeObjectEvent treeObjectEvent) {
		super.treeObjectRemoved(treeObjectEvent);
		
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		Object object = getObject();
		
		// Case this is a transaction step
		if (object instanceof TransactionStep) {
			if ((treeObject instanceof ProjectTreeObject) ||
				(treeObject instanceof UnloadedProjectTreeObject) ||
				(treeObject instanceof ConnectorTreeObject) ||
				(treeObject instanceof TransactionTreeObject)) {
			
		    	// Refresh label in case of broken properties
		    	try {
		    		getProjectExplorerView().updateTreeObject(this);
		    		
				} catch (Exception e) {
					ConvertigoPlugin.logWarning(e, "Could not update Transaction step\""+ getName()+"\" !");
				}
			}
		}

		// Case this is a sequence step
		if (object instanceof SequenceStep) {
			if ((treeObject instanceof ProjectTreeObject) ||
				(treeObject instanceof UnloadedProjectTreeObject) ||
				(treeObject instanceof SequenceTreeObject)) {
			
		    	// Refresh label in case of broken properties
		    	try {
		    		getProjectExplorerView().updateTreeObject(this);
		    		
				} catch (Exception e) {
					ConvertigoPlugin.logWarning(e, "Could not update Sequence step \""+ getName()+"\" !");
				}
			}
		}
	}
	
	public void launchEditor(String editorType) {
		try {
			// Get editor type
			if (editorType == null) {
				if (getObject() instanceof SimpleStep) {
					editorType = "JscriptStepEditor";
				} else {
					return;
				}
			}

			// Open editor
			if (editorType.equals("JscriptStepEditor")) {
				JScriptEditorInput.openJScriptEditor(this, "expression");
			}

		} catch (PartInitException e) {
			ConvertigoPlugin.logException(e, "Error while loading the step editor '" + getObject().getName() + "'");
		}
	}

	@Override
	public boolean testAttribute(Object target, String name, String value) {
		if (getObject().testAttribute(name, value)) {
			return true;
		}
		return super.testAttribute(target, name, value);
	}
	
	@Override
	public void closeAllEditors(boolean save) {
		closeAllJsEditors(getObject(), save);
	}
}
