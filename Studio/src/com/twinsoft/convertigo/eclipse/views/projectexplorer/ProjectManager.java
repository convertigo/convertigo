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

    public void saveSingleObject(DatabaseObject databaseObject, boolean bForce) throws ConvertigoException {
		if (databaseObject.hasChanged || bForce) {
			ProjectExplorerView projectExplorerView = getProjectExplorerView();
			
			// Case of already loaded project
			boolean bProject = ((currentProject != null) && (databaseObject.getQName().startsWith(currentProject.getQName())));
			if (bProject) {
				DatabaseObjectTreeObject treeObject = (DatabaseObjectTreeObject) projectExplorerView.findTreeObjectByUserObject(databaseObject);

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
				
				if (treeObject!=null) treeObject.hasBeenModified(false);
			}
			else {
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
