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

package com.twinsoft.convertigo.eclipse.popup.actions;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;
import org.xml.sax.SAXException;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.statements.ElseStatement;
import com.twinsoft.convertigo.beans.statements.ThenStatement;
import com.twinsoft.convertigo.beans.steps.ElementStep;
import com.twinsoft.convertigo.beans.steps.ElseStep;
import com.twinsoft.convertigo.beans.steps.IThenElseContainer;
import com.twinsoft.convertigo.beans.steps.ThenStep;
import com.twinsoft.convertigo.beans.steps.XMLAttributeStep;
import com.twinsoft.convertigo.beans.steps.XMLElementStep;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dialogs.ButtonSpec;
import com.twinsoft.convertigo.eclipse.dialogs.CustomDialog;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.FolderTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.IPropertyTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreePath;
import com.twinsoft.convertigo.engine.ConvertigoException;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.CarUtils;
import com.twinsoft.convertigo.engine.util.ProjectUtils;

public class ClipboardAction extends MyAbstractAction {

	public ClipboardAction() {
		super();
	}

	public static String copy(ProjectExplorerView explorerView) throws EngineException, ParserConfigurationException {
		String sXml = null;
		if (explorerView != null) {
			int type = ProjectExplorerView.getTreeObjectType(explorerView.getLeadSelectionPath());
			TreePath[] selectedPaths = explorerView.getSelectionPaths();
			sXml = copy(explorerView, selectedPaths, type);
		}
		return sXml;
	}
	
	public static String copy(ProjectExplorerView explorerView, TreePath[] selectedPaths, int type) throws EngineException, ParserConfigurationException {
		String sXml = null;
		if (explorerView != null) {
			ConvertigoPlugin.clipboardManager2.reset();
			ConvertigoPlugin.clipboardManager2.objectsType = type;
			ConvertigoPlugin.clipboardManager2.isCopy = true;
			sXml = ConvertigoPlugin.clipboardManager2.copy(selectedPaths);
			
			for (int i = 0 ; i < selectedPaths.length ; i++) {
				TreeObject treeObject = (TreeObject)selectedPaths[i].getLastPathComponent();
				if (treeObject instanceof ProjectTreeObject)
					makeProjectTempArchive((ProjectTreeObject)treeObject);
			}
		}
		return sXml;
	}

	public static String cut(ProjectExplorerView explorerView) throws EngineException, ParserConfigurationException {
		String sXml = copy(explorerView);
		if (sXml != null) {
			ConvertigoPlugin.clipboardManager2.isCopy = false;
			ConvertigoPlugin.clipboardManager2.isCut = true;
		}
		return sXml;
	}
	
	public static String cut(ProjectExplorerView explorerView, TreePath[] selectedPaths, int type) throws EngineException, ParserConfigurationException {
		String sXml = copy(explorerView, selectedPaths, type);
		if (sXml != null) {
			ConvertigoPlugin.clipboardManager2.isCopy = false;
			ConvertigoPlugin.clipboardManager2.isCut = true;
		}
		return sXml;
	}

	public static void paste(String source, Shell shell, ProjectExplorerView explorerView, TreeObject selectedTreeObject) throws ConvertigoException, IOException, ParserConfigurationException, SAXException, CoreException {
		paste(source, shell, explorerView, selectedTreeObject, false);
	}
	
	public static void paste(String source, Shell shell, ProjectExplorerView explorerView, TreeObject selectedTreeObject, boolean isDND) throws ConvertigoException, IOException, ParserConfigurationException, SAXException, CoreException {
		if ((explorerView != null) && (selectedTreeObject != null)) {
			TreeObject targetTreeObject = null;
			Object targetObject = null;
			
			if (selectedTreeObject instanceof IPropertyTreeObject) {
				targetTreeObject = selectedTreeObject;
				targetObject = selectedTreeObject;
			}
			else {
    			targetTreeObject = explorerView.getFirstSelectedDatabaseObjectTreeObject(selectedTreeObject); // case of folder, retrieve owner object
    			targetObject = (DatabaseObject)targetTreeObject.getObject();
    			
    			// This is for enabling copy/paste inside the same data directory,
    			// i.e. without having to select the parent database object.
    			if (ConvertigoPlugin.clipboardManager2.objectsType == ProjectExplorerView.getTreeObjectType(new TreePath(targetTreeObject))) {
    				// Exception: if the copied object is a screen class,
    				// it must be different from the currently selected object.
    				if (ConvertigoPlugin.clipboardManager2.objectsType == ProjectExplorerView.TREE_OBJECT_TYPE_DBO_SCREEN_CLASS) {
    					CustomDialog customDialog = new CustomDialog(
    							shell,
    							"Paste a Screenclass",
    							"Do you want to paste the Screenclass as a sibling or as an inherited Screenclass?",
    							500, 150,
    							new ButtonSpec("As a sibling", true),
    							new ButtonSpec("As an inherited", false),
    							new ButtonSpec(IDialogConstants.CANCEL_LABEL, false)
    					);
    					int response = customDialog.open();
    					if (response == 0)
    						targetObject = ((DatabaseObject)targetObject).getParent();
    					else if (response == 2)
    						return;
    				}
    				else if (ConvertigoPlugin.clipboardManager2.objectsType == ProjectExplorerView.TREE_OBJECT_TYPE_DBO_STATEMENT_WITH_EXPRESSIONS) {
    					if (ConvertigoPlugin.clipboardManager2.objectsType == ProjectExplorerView.TREE_OBJECT_TYPE_FUNCTION) {
    						targetObject = ((DatabaseObject)targetObject).getParent();
    					}
    					else {
    						CustomDialog customDialog = new CustomDialog(
        							shell,
        							"Paste a statement",
        							"Do you want to paste the statement as a sibling or a child statement?",
        							500, 150,
        							new ButtonSpec("As a sibling", true),
        							new ButtonSpec("As a child", false),
        							new ButtonSpec(IDialogConstants.CANCEL_LABEL, false)
        					);
        					int response = customDialog.open();
	    					if (response == 0) {
	    						targetObject = ((DatabaseObject)targetObject).getParent();
	    					}
	    					else if (response == 2) {
	    						return;
	    					}
    					}
    				}
    				else if (ConvertigoPlugin.clipboardManager2.objectsType == ProjectExplorerView.TREE_OBJECT_TYPE_DBO_STEP_WITH_EXPRESSIONS ||
    						ConvertigoPlugin.clipboardManager2.objectsType == ProjectExplorerView.TREE_OBJECT_TYPE_DBO_STEP) {
    					targetObject = pasteStep(shell, source, (DatabaseObject)targetObject);
    					if (targetObject == null) return;
    				}
    				else if (isDND && ConvertigoPlugin.clipboardManager2.objectsType == ProjectExplorerView.TREE_OBJECT_TYPE_DBO_SEQUENCE) {
    					// Do not change target to parent
    				}
    				else {
   						targetObject = ((DatabaseObject)targetObject).getParent();
    				}
    				
					targetTreeObject = explorerView.findTreeObjectByUserObject(((DatabaseObject)targetObject));
    			}
				else {
					if (ConvertigoPlugin.clipboardManager2.objectsType == ProjectExplorerView.TREE_OBJECT_TYPE_DBO_STEP_WITH_EXPRESSIONS ||
						ConvertigoPlugin.clipboardManager2.objectsType == ProjectExplorerView.TREE_OBJECT_TYPE_DBO_STEP) {
						targetObject = pasteStep(shell, source, (DatabaseObject)targetObject);
						if (targetObject == null) return;
					}
				}
			}

            if (ConvertigoPlugin.clipboardManager2.isCut) {
        		TreeParent targetTreeParent = null;
        		String targetPath = targetTreeObject.getPath();
        		if (targetTreeObject instanceof DatabaseObjectTreeObject) {
        			targetTreeParent = ((DatabaseObjectTreeObject)targetTreeObject).getOwnerDatabaseObjectTreeObject();
    			}
        		else if (targetTreeObject instanceof IPropertyTreeObject) {
        			targetTreeParent = ((IPropertyTreeObject)targetTreeObject).getTreeObjectOwner();
        		}
        			
            	for (int i = 0 ; i < ConvertigoPlugin.clipboardManager2.objects.length ; i++) {
            		// Cut & paste
					ConvertigoPlugin.clipboardManager2.cutAndPaste(ConvertigoPlugin.clipboardManager2.objects[i], targetTreeObject);
					
					// Updating the tree
					// Report 4.5: fix #401
					//explorerView.reloadTreeObject(ConvertigoPlugin.clipboardManager2.parentTreeNodeOfCutObjects[i]);
					TreeObject parentTreeNodeOfCutObjects = ConvertigoPlugin.clipboardManager2.parentTreeNodeOfCutObjects[i];
					parentTreeNodeOfCutObjects.getProjectTreeObject().hasBeenModified(true);
					explorerView.reloadTreeObject(parentTreeNodeOfCutObjects);
            	}
            	
            	if (targetTreeObject != null) {
                	if (targetTreeObject.getParent() == null)
                		targetTreeObject = explorerView.findTreeObjectByPath(targetTreeParent, targetPath);
                	
                	if (targetTreeObject != null)
                		targetTreeObject.getProjectTreeObject().hasBeenModified(true);// Report 4.5: fix #401
            	}
            	
            	ConvertigoPlugin.clipboardManager2.reset();
            }
            else if (ConvertigoPlugin.clipboardManager2.isCopy){
            	if (source != null) {
                	// Paste
                    ConvertigoPlugin.clipboardManager2.paste(source, targetObject, true);

                    // case of project copy
                    if (ConvertigoPlugin.clipboardManager2.objectsType == ProjectExplorerView.TREE_OBJECT_TYPE_DBO_PROJECT) {
                    	Object[] pastedObjects = ConvertigoPlugin.clipboardManager2.pastedObjects;
                    	for (int i=0; i<pastedObjects.length; i++) {
                    		Object object = pastedObjects[i];
                    		if ((object != null) && (object instanceof Project)) {
                    			Project project = (Project)object;
                    			try {
                    				project = importProjectTempArchive(project.getOldName(), explorerView);
                    				if (project != null) {
                    					explorerView.importProjectTreeObject(project.getName());
                    				}
                    				else throw new EngineException("Unable to import project archive");
                    			}
                    			catch (Exception e1) {
                        			project = (Project) object;
                        			String oldName = project.getOldName();
                        			String newName = project.getName();
                        			
                    				// Copy old xsd file
                    				try {
                    					ProjectUtils.copyXsdFile(Engine.PROJECTS_PATH, oldName, newName);
                    				} catch (Exception e) {
                    					throw new ConvertigoException(e.getMessage());
                    				}
                    				
                    				// Copy old wsdl file
                    				try {
                    					ProjectUtils.copyWsdlFile(Engine.PROJECTS_PATH, oldName, newName);
                    				} catch (Exception e) {
                    					throw new ConvertigoException(e.getMessage());
                    				}
                        			
                    				// Create index.html
                    				try {
                    					ProjectUtils.copyIndexFile(newName);
    	            				} catch (Exception e) {
    	            					throw new ConvertigoException(e.getMessage());
    	            				}
                    				
                    				// Rename steps in xsd file
                    				try {
                    					ProjectUtils.renameStepsInXsd(Engine.PROJECTS_PATH, newName, ConvertigoPlugin.clipboardManager2.pastedSteps);
    	            				} catch (Exception e) {
    	            					throw new ConvertigoException(e.getMessage());
    	            				}
                    				
                            		explorerView.importProjectTreeObject(newName, true, oldName);
                    			}
                    		}
                    	}
                    }
            	}
            }

            // Updating the tree
            if (targetTreeObject != null) {
            	TreeObject treeObjectToReload = targetTreeObject;
            	TreeObject treeObjectToSelect = targetTreeObject;
        		if (targetTreeObject instanceof IPropertyTreeObject) {
        			treeObjectToSelect = ((IPropertyTreeObject)targetTreeObject).getTreeObjectOwner();
        			treeObjectToReload = treeObjectToSelect;
            		if (treeObjectToReload instanceof DatabaseObjectTreeObject) {
            			treeObjectToReload = treeObjectToReload.getParent();
    					if (treeObjectToReload instanceof FolderTreeObject)
    						treeObjectToReload = treeObjectToReload.getParent();
            		}
        		}
        		
        		if (treeObjectToReload != null) {
	                //explorerView.reloadTreeObject(targetTreeObject);
	                //explorerView.setSelectedTreeObject(targetTreeObject);
        			explorerView.objectChanged(new CompositeEvent(treeObjectToReload.getObject(),treeObjectToSelect.getPath()));
        		}
            }
		}
	}
	
	private static Object pasteStep(Shell shell, String source, DatabaseObject targetObject) throws ParserConfigurationException, SAXException, IOException {
		// Can only paste on Sequence or Step
		if (targetObject instanceof Sequence)
			return targetObject;
		else if (!(targetObject instanceof Step))
			return null;
		
		// Can not paste to IThenElseContainer
		if (targetObject instanceof IThenElseContainer)
			return null;
		else {
			List<Object> objects = ConvertigoPlugin.clipboardManager2.read(source);
			int size = objects.size();
			for (Object ob: objects) {
				// Can only paste step objects
				if (!(ob instanceof Step))
					return null;
				// Can paste only on step which may contain children
				if ((ob instanceof StepWithExpressions) && (!(targetObject instanceof StepWithExpressions)))
					return null;
				// Can not paste a ThenStep
				if (ob instanceof ThenStep)
					return null;
				// Can not paste a ElseStep
				if (ob instanceof ElseStep)
					return null;
				// Can not paste a ThenStatement
				if (ob instanceof ThenStatement)
					return null;
				// Can not paste a ElseStatement
				if (ob instanceof ElseStatement)
					return null;
				// Special case of XMLElementStep, ElementStep
				if ((targetObject instanceof XMLElementStep) || (targetObject instanceof ElementStep)) {
					// Case paste on itself -> target is changed to parent
					if ((size==1) && ((ob instanceof XMLElementStep) || (ob instanceof ElementStep))) {
						if (((Step)ob).getName().equals(targetObject.getName())) {
							return targetObject.getParent();
						}
						return null;
					}
					// Else, only accept paste of XMLAttributeStep
					else if (!(ob instanceof XMLAttributeStep)) {
						return null;
					}
				}
				// Case of step which may contain children 
				else if (targetObject instanceof StepWithExpressions){
					// Case paste on itself -> ask user what to do
					if ((size==1) && (ob.getClass().equals(targetObject.getClass()))) {
						if (((Step)ob).getName().equals(targetObject.getName())) {
							CustomDialog customDialog = new CustomDialog(
	    							shell,
	    							"Paste a step",
	    							"Do you want to paste the step as a sibling or a child step?",
	    							300, 150,
	    							new ButtonSpec("As a sibling", true),
	    							new ButtonSpec("As a child", false),
	    							new ButtonSpec(IDialogConstants.CANCEL_LABEL, false)
	    					);
	    					int response = customDialog.open();
							if (response == 0) {
								return targetObject.getParent();
							}
							else if (response == 2) {
								return null;
							}
							else
								break;
						}
					}
					// Else, paste
					break;
				}
				// Other case
				else {
					// Case paste on itself -> target is changed to parent
					if ((size==1) && (ob.getClass().equals(targetObject.getClass()))) {
						if (((Step)ob).getName().equals(targetObject.getName())) {
							return targetObject.getParent();
						}
						return null;
					}
					// Else, not permitted
					return null;
				}
			}
		}
		
		return targetObject;
	}
	
	private static void makeProjectTempArchive(ProjectTreeObject projectTreeObject) throws EngineException {
		Project project = projectTreeObject.getObject();		
				
		try {
			File exportDirectory = new File(Engine.USER_WORKSPACE_PATH + "/temp");
			if (!exportDirectory.exists()) exportDirectory.mkdir();
			String exportDirectoryPath = exportDirectory.getCanonicalPath();
			CarUtils.makeArchive(exportDirectoryPath, project);
		}
		catch (Exception e) {
			throw new EngineException("Unable to make a project copy archive",e);
		}
	}
	
	private static Project importProjectTempArchive(String projectName, ProjectExplorerView explorerView) throws EngineException {
		try {
			File importDirectory = new File(Engine.USER_WORKSPACE_PATH + "/temp");
			if (!importDirectory.exists()) importDirectory.mkdir();
			
			String importDirectoryPath, importArchiveFilename;
			importDirectoryPath = importDirectory.getCanonicalPath();
			importArchiveFilename = importDirectoryPath + "/" + projectName +".car";
			
			File f = new File(importArchiveFilename);
			if (f.exists()) {
				int index = 1;
				String targetProjectName = projectName;
				while (explorerView.getProjectRootObject(targetProjectName) != null) {
					targetProjectName = projectName + index++;
				}
				
				Project importedProject = Engine.theApp.databaseObjectsManager.deployProject(importArchiveFilename, targetProjectName, true);
				f.delete();
				return importedProject;
			}
		} catch (Exception e) {
			throw new EngineException("Unable to import project archive",e);
		}
		return null;
	}
}
