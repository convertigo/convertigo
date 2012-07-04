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

package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableStep;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.StatementWithExpressions;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.beans.statements.HTTPStatement;
import com.twinsoft.convertigo.beans.statements.HandlerStatement;
import com.twinsoft.convertigo.beans.statements.ScHandlerStatement;
import com.twinsoft.convertigo.beans.statements.SimpleEventStatement;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.ConvertigoException;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;

public class ProjectManager {
    
	/**
     * The current project.
     */
    public ProjectTreeObject currentProjectTreeObject = null;
    public Project currentProject = null;
    
    public String currentProjectName = null;
    public String previousProjectName = null;

    /**
     * Indicates if the current project has been modified.
     */
    protected boolean bModified = false;
    public int nModifications = 0;
    
    public void setCurrentProject(ProjectTreeObject projectTreeObject) {
    	DatabaseObject databaseObject = (DatabaseObject)projectTreeObject.getObject();
    	if ((databaseObject != null) && (databaseObject instanceof Project)) {
    		if (currentProject != null)
    			previousProjectName = currentProjectName;
    		
    		currentProject = (Project)databaseObject;
    		currentProjectName = currentProject.getName();
    		currentProjectTreeObject = projectTreeObject;
    	}
    }
    
    public void copyProject(String projectName) throws EngineException {
        if (projectName == null || !Engine.theApp.databaseObjectsManager.existsProject(projectName)) {
            throw new IllegalArgumentException("The project \"" + projectName + "\" does not exist!");
        }
        
        int i = 1;
        while (Engine.theApp.databaseObjectsManager.existsProject(projectName + "_" + i)) {
            i++;
        }
        String copyProjectName = projectName + "_" + i;
        
        try {
            // Copy the whole directory
        	FileUtils.copyDirectory(new File(Engine.PROJECTS_PATH + "/" + projectName), new File(Engine.PROJECTS_PATH + "/" + copyProjectName));
            
            // Rename the project
            Project project = Engine.theApp.databaseObjectsManager.getProjectByName(copyProjectName);
            project.setName(copyProjectName);
            project.write();
            Engine.theApp.databaseObjectsManager.cacheRemoveObject(project.getQName());
        }
        catch(IOException e) {
            throw new EngineException("Unable to copy the directory for the project \"" + projectName + "\"", e);
        }
    }

    public void saveSingleObject(DatabaseObject databaseObject, boolean bForce) throws ConvertigoException {
		if (databaseObject.hasChanged || bForce) {
			ProjectExplorerView projectExplorerView = getProjectExplorerView();
			
			// Case of already loaded project
			boolean bProject = ((currentProject != null) && (databaseObject.getQName().startsWith(currentProject.getQName())));
			if (bProject) {
				DatabaseObjectTreeObject treeObject = (DatabaseObjectTreeObject) projectExplorerView.findTreeObjectByUserObject(databaseObject);

				if (treeObject != null) {
//					String latestSavedDatabaseObjectQName = treeObject.latestSavedDatabaseObjectQName;
//					String latestSavedDatabaseObjectName = treeObject.latestSavedDatabaseObjectName;
//					String newQName = databaseObject.getQName();
//					String newName = databaseObject.getName();
//					
//					if (!latestSavedDatabaseObjectQName.equalsIgnoreCase(newQName)) {
//						Engine.theApp.databaseObjectsManager.cacheRemoveObject(latestSavedDatabaseObjectQName);
//	
//						if (databaseObject instanceof Project) {
//							String latestSavedDatabaseObjectPath = latestSavedDatabaseObjectQName.substring(0, latestSavedDatabaseObjectQName.lastIndexOf("/_data/"));
//							File file = new File(Engine.PROJECTS_PATH + latestSavedDatabaseObjectPath);
//							if (file.exists()) {
//								// Rename dir
//								if (!file.renameTo(new File(Engine.PROJECTS_PATH + "/" + newName))) {
//									throw new ConvertigoException(
//										"Unable to rename the object path \"" +
//										Engine.PROJECTS_PATH + latestSavedDatabaseObjectPath + 
//										"\" to \"" + Engine.PROJECTS_PATH + "/" + newName +
//										"\".\n This directory is probably locked by another application or is (or one of its child objects is) read-only.");
//								}
//	
//								// Rename CVS if needed
//								if (treeObject.isUnderCvs) {
//								}
//							}
//	                    
//							// Update the project name
//							currentProjectName = newName;
//						}
//						else if (databaseObject instanceof Connector) {												
//							if (databaseObject instanceof Connector) {
//								// Rename dir in Traces directory
//								File file = new File(Engine.PROJECTS_PATH + "/" + currentProjectName + "/Traces/" + latestSavedDatabaseObjectName);
//								if (file.exists()) {
//									file.renameTo(new File(Engine.PROJECTS_PATH + "/" + currentProjectName + "/Traces/" + newName));
//								}
//							}
//						}
//					}
				}
				if (databaseObject instanceof HandlerStatement) {
					if (((HandlerStatement)databaseObject).getHandlerType().equals("")) {
						throw new ConvertigoException("Handler type for statement \""+ databaseObject.getName() +"\" must not be empty!");
					}
					if (databaseObject instanceof ScHandlerStatement) {
						if (((ScHandlerStatement)databaseObject).getNormalizedScreenClassName().equals("")) {
							throw new ConvertigoException("Normalized ScreenClass name for statement \""+ databaseObject.getName() +"\" must not be empty!");
						}
					}
				}
				if (databaseObject instanceof SimpleEventStatement) {
					if (((SimpleEventStatement)databaseObject).getAction().equals("")) {
						throw new ConvertigoException("Action for statement \""+ databaseObject.getName() +"\" must not be empty!");
					}
				}
				
				// Save the object
				databaseObject.write();
				Engine.theApp.databaseObjectsManager.cacheUpdateObject(databaseObject);
				if (treeObject!=null) treeObject.hasBeenModified(false);
			}
			else {
				// Save the object
				databaseObject.write();
				Engine.theApp.databaseObjectsManager.cacheUpdateObject(databaseObject);
				projectHasBeenModified(false);
			}
		}
    }    
    
    public void save(DatabaseObject parentDatabaseObject, final boolean bForce) throws ConvertigoException {
    	try {
			new WalkHelper() {
				
				@Override
				protected boolean before(DatabaseObject databaseObject, Class<? extends DatabaseObject> dboClass) {
					ConvertigoPlugin.logDebug3("   + " + dboClass.getSimpleName());
					return super.before(databaseObject, dboClass);
				}

				@Override
				protected void walk(DatabaseObject databaseObject) throws Exception {
			    	if (databaseObject != null) {
			    		ConvertigoPlugin.logDebug3("   - [" + databaseObject.getName() + "]");
				    	ConvertigoPlugin.logDebug3("Saving the object \"" + databaseObject.getQName() + "\"");
				        saveSingleObject(databaseObject, bForce);
						super.walk(databaseObject);
				        if (databaseObject instanceof Sequence) {
				        	Sequence sequence = (Sequence) databaseObject;
				        	sequence.getSteps(true);
				        	sequence.getVariables(true);
				        } else if (databaseObject instanceof TransactionWithVariables) {
							((TransactionWithVariables) databaseObject).getVariables(true);
						} else if (databaseObject instanceof StatementWithExpressions) {
							((StatementWithExpressions) databaseObject).getStatements(true);
						} else if (databaseObject instanceof HTTPStatement) {
							((HTTPStatement) databaseObject).getVariables(true);
						} else if (databaseObject instanceof StepWithExpressions) {
							((StepWithExpressions) databaseObject).getSteps(true);
						} else if (databaseObject instanceof RequestableStep) {
							((RequestableStep) databaseObject).getVariables(true);
						} else if (databaseObject instanceof TestCase) {
							((TestCase) databaseObject).getVariables(true);
						} else if (databaseObject instanceof ScreenClass) {
				        	ScreenClass screenClass = (ScreenClass) databaseObject;
				            screenClass.getCriterias(true);
				            screenClass.getExtractionRules(true);
				        }
			    	}
				}
				
			}.init(parentDatabaseObject);
			
		} catch (ConvertigoException e) {
			throw e;
		} catch (Exception e) {
			throw new ConvertigoException("Exception in save", e);
		}
    }

    private ProjectExplorerView projectExplorerView = null;
    
	public ProjectExplorerView getProjectExplorerView() {
		if (projectExplorerView == null) {
			try {
				IViewPart viewPart =  PlatformUI
											.getWorkbench()
											.getActiveWorkbenchWindow()
											.getActivePage()
											.findView("com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView");
				if (viewPart != null)
					projectExplorerView = (ProjectExplorerView)viewPart;
			}
			catch (Exception e) {;}
		}
		
		return projectExplorerView;
	}

	public void setProjectExplorerView(ProjectExplorerView projectExplorerView) {
		this.projectExplorerView = projectExplorerView;
	}
	
    private void projectHasBeenModified(boolean bModified) {
    	if (currentProjectTreeObject == null)
    		return;
    	currentProjectTreeObject.hasBeenModified(bModified);
    }
    
	public int getNumberOfObjects(String projectName) {
		return 100;
	}
}
