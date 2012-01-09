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
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;

import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Criteria;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ExtractionRule;
import com.twinsoft.convertigo.beans.core.IScreenClassContainer;
import com.twinsoft.convertigo.beans.core.MobileDevice;
import com.twinsoft.convertigo.beans.core.Pool;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableStep;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Sheet;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.core.StatementWithExpressions;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.screenclasses.JavelinScreenClass;
import com.twinsoft.convertigo.beans.statements.HTTPStatement;
import com.twinsoft.convertigo.beans.statements.HandlerStatement;
import com.twinsoft.convertigo.beans.statements.ScHandlerStatement;
import com.twinsoft.convertigo.beans.statements.SimpleEventStatement;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.beans.variables.HttpStatementVariable;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.ConvertigoException;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

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
            copyDir(Engine.PROJECTS_PATH + "/" + projectName, Engine.PROJECTS_PATH + "/" + copyProjectName);
            
            // Rename the project
            Project project = Engine.theApp.databaseObjectsManager.getProjectByName0(copyProjectName);
            project.setName(copyProjectName);
            project.write();
            Engine.theApp.databaseObjectsManager.cacheRemoveObject(project.getQName());
        }
        catch(IOException e) {
            throw new EngineException("Unable to copy the directory for the project \"" + projectName + "\"", e);
        }
    }
    
    // Deletes all files and subdirectories under dir.
    // Returns true if all deletions were successful.
    // If a deletion fails, the method stops attempting to delete and returns false.
    private boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
    
        // The directory is now empty so delete it
        return dir.delete();
    }

    
    private void copyDir(String source, String dest) throws IOException {
        File fDest = new File(dest);
        fDest.mkdir();
        
        File fSource = new File(source);
        File[] files = fSource.listFiles();
        File file;
        for (int i = 0 ; i < files.length ; i++) {
            file = files[i];
            if (file.isDirectory()) {
                copyDir(file.getAbsolutePath(), dest + "/" + file.getName());
            }
            else {
                copyFile(file.getAbsolutePath(), dest + "/" + file.getName());
            }
        }
    }
    
    private void copyFile(String source, String dest) throws IOException {
        File inputFile = new File(source);
        File outputFile = new File(dest);

        FileInputStream in = new FileInputStream(inputFile);
        FileOutputStream out = new FileOutputStream(outputFile);
        int c;

        while ((c = in.read()) != -1)
           out.write(c);

        in.close();
        out.close();
    }

    public void saveSingleObject(DatabaseObject databaseObject, boolean bForce) throws ConvertigoException {
		if (databaseObject.hasChanged || bForce) {
			ProjectExplorerView projectExplorerView = getProjectExplorerView();
			
			// Case of already loaded project
			boolean bProject = ((currentProject != null) && (databaseObject.getQName().startsWith(currentProject.getPath())));
			if (bProject) {
				DatabaseObjectTreeObject treeObject = (DatabaseObjectTreeObject)projectExplorerView.findTreeObjectByUserObject(databaseObject);

				if (treeObject != null) {
					String latestSavedDatabaseObjectQName = treeObject.latestSavedDatabaseObjectQName;
					String latestSavedDatabaseObjectName = treeObject.latestSavedDatabaseObjectName;
					String newQName = databaseObject.getQName();
					String newName = databaseObject.getName();
					
					if (!latestSavedDatabaseObjectQName.equalsIgnoreCase(newQName)) {
						Engine.theApp.databaseObjectsManager.cacheRemoveObject(latestSavedDatabaseObjectQName);
	
						if (databaseObject instanceof Project) {
							String latestSavedDatabaseObjectPath = latestSavedDatabaseObjectQName.substring(0, latestSavedDatabaseObjectQName.lastIndexOf("/_data/"));
							File file = new File(Engine.PROJECTS_PATH + latestSavedDatabaseObjectPath);
							if (file.exists()) {
								// Rename dir
								if (!file.renameTo(new File(Engine.PROJECTS_PATH + "/" + newName))) {
									throw new ConvertigoException(
										"Unable to rename the object path \"" +
										Engine.PROJECTS_PATH + latestSavedDatabaseObjectPath + 
										"\" to \"" + Engine.PROJECTS_PATH + "/" + newName +
										"\".\n This directory is probably locked by another application or is (or one of its child objects is) read-only.");
								}
	
								// Rename CVS if needed
								if (treeObject.isUnderCvs) {
								}
							}
	                    
							// Update the project name
							currentProjectName = newName;
						}
						else if ((databaseObject instanceof Connector) || 
								(databaseObject instanceof Sequence) || 
								(databaseObject instanceof Transaction) || 
								(databaseObject instanceof ScreenClass) || 
								(databaseObject instanceof Statement) || 
								(databaseObject instanceof Step) || 
								(databaseObject instanceof TestCase)) {
							String latestSavedDatabaseObjectPath = latestSavedDatabaseObjectQName.substring(0, latestSavedDatabaseObjectQName.lastIndexOf('/'));
							File file = new File(Engine.PROJECTS_PATH + latestSavedDatabaseObjectPath);
							if (file.exists()) {
								// Rename dir
								if (!file.renameTo(new File(Engine.PROJECTS_PATH + databaseObject.getPath()))) {
									throw new ConvertigoException(
										"Unable to rename the object path \"" +
										Engine.PROJECTS_PATH + latestSavedDatabaseObjectPath + 
										"\" to \"" + Engine.PROJECTS_PATH + databaseObject.getPath() +
										"\".\n This directory is probably locked by another application or is (or one of its child objects is) read-only.");
								}
	
								// Rename CVS if needed
								if (treeObject.isUnderCvs) {
								}
							}
							else {
								DatabaseObjectTreeObject parentTreeObject = (DatabaseObjectTreeObject)projectExplorerView.findTreeObjectByUserObject(databaseObject.getParent());
								String latestSavedParentObjectQName = parentTreeObject.latestSavedDatabaseObjectQName;
								String latestSavedParentObjectPath = latestSavedParentObjectQName.substring(0, latestSavedParentObjectQName.lastIndexOf('/'));
								
								// One of parents has been renamed before while saving project
								if (latestSavedDatabaseObjectQName.indexOf(latestSavedParentObjectPath) == -1) {
									// Must delete previously renamed directory
									String dataDirectory = "";
									if (databaseObject instanceof Connector) dataDirectory = Connector.DATA_DIRECTORY;
									else if (databaseObject instanceof Sequence) dataDirectory = Sequence.DATA_DIRECTORY;
									else if (databaseObject instanceof Transaction) dataDirectory = Transaction.DATA_DIRECTORY;
									else if (databaseObject instanceof ScreenClass) dataDirectory = ScreenClass.DATA_DIRECTORY;
									else if (databaseObject instanceof Statement) dataDirectory = Statement.DATA_DIRECTORY;
									else if (databaseObject instanceof Step) dataDirectory = Step.DATA_DIRECTORY;
									else if (databaseObject instanceof TestCase) dataDirectory = TestCase.DATA_DIRECTORY;
									
									int i = latestSavedDatabaseObjectQName.lastIndexOf('/'+dataDirectory+'/');
									int j = latestSavedDatabaseObjectQName.lastIndexOf('/');
									String databaseObjectRenamedPath = latestSavedParentObjectPath + latestSavedDatabaseObjectQName.substring(i, j);
									File renamedDir = new File(Engine.PROJECTS_PATH + databaseObjectRenamedPath);
									if (databaseObject.hasChanged && renamedDir.exists()) {
										// delete file first (If pathname denotes a directory, then the directory must be empty in order to be deleted)
										if (!deleteDir(renamedDir)) {
											throw new ConvertigoException(
													"Unable to delete the previously renamed object \"" +
													Engine.PROJECTS_PATH + databaseObjectRenamedPath +
													"\".\n This file is probably locked by another application.");
										}
									}
								}
							}
							
							if (databaseObject instanceof Connector) {
								// Rename dir in Traces directory
								file = new File(Engine.PROJECTS_PATH + "/" + currentProjectName + "/Traces/" + latestSavedDatabaseObjectName);
								if (file.exists()) {
									file.renameTo(new File(Engine.PROJECTS_PATH + "/" + currentProjectName + "/Traces/" + newName));
								}
							}
						}
						else {
							File file = new File(Engine.PROJECTS_PATH + latestSavedDatabaseObjectQName);
							if (file.exists()) {
								// Delete old file
								if (!file.delete()) {
									throw new ConvertigoException(
										"Unable to delete the old object \"" +
										Engine.PROJECTS_PATH + latestSavedDatabaseObjectQName +
										"\".\n This file is probably locked by another application.");
								}
	
								// Rename CVS if needed
								if (treeObject.isUnderCvs) {
								}
							}
						}
	
						treeObject.latestSavedDatabaseObjectQName = newQName;
						treeObject.latestSavedDatabaseObjectName = newName;
					}
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
    
    public void save(DatabaseObject parentDatabaseObject, boolean bForce) throws ConvertigoException {
    	if (parentDatabaseObject != null) {
	    	ConvertigoPlugin.logDebug3("Saving the object \"" + parentDatabaseObject.getQName() + "\"");
	        saveSingleObject(parentDatabaseObject, bForce);
	
	        if (parentDatabaseObject instanceof Project) {
	        	ConvertigoPlugin.logDebug3("   + Project");
	            Project project = (Project) parentDatabaseObject;
	            
				// Connectors
	            ConvertigoPlugin.logDebug3("   + Connectors");
				for (Connector connector : project.getConnectorsList()) {
					ConvertigoPlugin.logDebug3("   - [" + connector.getName() + "]");
					save(connector, bForce);
				}
				
				// Sequences
	            ConvertigoPlugin.logDebug3("   + Sequences");
				for (Sequence sequence : project.getSequencesList()) {
					ConvertigoPlugin.logDebug3("   - [" + sequence.getName() + "]");
					save(sequence, bForce);
				}
				
				// Mobile devices
	            ConvertigoPlugin.logDebug3("   + Mobile devices");
				for (MobileDevice device : project.getMobileDeviceList()) {
					ConvertigoPlugin.logDebug3("   - [" + device.getName() + "]");
					save(device, bForce);
				}
	        }
			else if (parentDatabaseObject instanceof Sequence) {
				Sequence sequence = (Sequence) parentDatabaseObject;
				// Test Cases
				ConvertigoPlugin.logDebug3("   + TestCases");
				for (TestCase testCase : ((Sequence)parentDatabaseObject).getTestCasesList()) {
					ConvertigoPlugin.logDebug3("   - [" + testCase.getName() + "]");
					save(testCase, bForce);
				}
				
				// Steps
				ConvertigoPlugin.logDebug3("   + Steps");
				for (Step step : sequence.getSteps()) {
					ConvertigoPlugin.logDebug3("   - [" + step.getName() + "]");
					save(step, bForce);
				}
				sequence.getSteps(true);
				
				// Sheets
				ConvertigoPlugin.logDebug3("   + Sheets");
				for (Sheet sheet : sequence.getSheetsList()) {
					ConvertigoPlugin.logDebug3("   - [" + sheet.getName() + "]");
					save(sheet, bForce);
				}

				// Variables
				ConvertigoPlugin.logDebug3("   + Variables");
				for (RequestableVariable variable : sequence.getVariablesList()) {
					ConvertigoPlugin.logDebug3("   - [" + variable.getName() + "]");
					save(variable, bForce);
				}
				sequence.getVariables(true);
			}
			else if (parentDatabaseObject instanceof Connector) {
				Connector connector = (Connector) parentDatabaseObject;
				
				// Pools
				ConvertigoPlugin.logDebug3("   + Pools");
				for (Pool pool : connector.getPoolsList()) {
					ConvertigoPlugin.logDebug3("   - [" + pool.getName() + "]");
					save(pool, bForce);
				}
	            
				// Transactions
				ConvertigoPlugin.logDebug3("   + Transactions");
				for (Transaction transaction : connector.getTransactionsList()) {
					ConvertigoPlugin.logDebug3("   - [" + transaction.getName() + "]");
					save(transaction, bForce);
				}
	            
				if (parentDatabaseObject instanceof IScreenClassContainer<?>) {
					// Root screen class
					ConvertigoPlugin.logDebug3("   + Screen classes");
					ScreenClass defaultScreenClass = ((IScreenClassContainer<?>) parentDatabaseObject).getDefaultScreenClass();
					ConvertigoPlugin.logDebug3("   - [" + defaultScreenClass.getName() + "]");
					save(defaultScreenClass, bForce);
				}			
			}
			else if (parentDatabaseObject instanceof Transaction) {
				Transaction transaction = (Transaction) parentDatabaseObject;
				
				// Sheets
				ConvertigoPlugin.logDebug3("   + Sheets");
				for (Sheet sheet : transaction.getSheetsList()) {
					ConvertigoPlugin.logDebug3("   - [" + sheet.getName() + "]");
					save(sheet, bForce);
				}
				
				if (parentDatabaseObject instanceof HtmlTransaction) {
					
					// Statements
					ConvertigoPlugin.logDebug3("   + Statements");
					for (Statement statement : ((HtmlTransaction)parentDatabaseObject).getStatements()) {
						ConvertigoPlugin.logDebug3("   - [" + statement.getName() + "]");
						save(statement, bForce);
					}
				}

				if (parentDatabaseObject instanceof TransactionWithVariables) {
					// Test Cases
					ConvertigoPlugin.logDebug3("   + TestCases");
					for (TestCase testCase : ((TransactionWithVariables)parentDatabaseObject).getTestCasesList()) {
						ConvertigoPlugin.logDebug3("   - [" + testCase.getName() + "]");
						save(testCase, bForce);
					}
					
					// Variables
					ConvertigoPlugin.logDebug3("   + Variables");
					for (RequestableVariable variable : ((TransactionWithVariables)parentDatabaseObject).getVariablesList()) {
						ConvertigoPlugin.logDebug3("   - [" + variable.getName() + "]");
						save(variable, bForce);
					}
					((TransactionWithVariables)parentDatabaseObject).getVariables(true);
				}
			}
			else if (parentDatabaseObject instanceof StatementWithExpressions) {
				StatementWithExpressions statementWE = (StatementWithExpressions)parentDatabaseObject;
				
				// Statements
				ConvertigoPlugin.logDebug3("   + Statements");
				for (Statement statement : statementWE.getStatements()) {
					ConvertigoPlugin.logDebug3("   - [" + statement.getName() + "]");
					save(statement, bForce);
				}
				statementWE.getStatements(true);
			}
			else if (parentDatabaseObject instanceof HTTPStatement) {
				HTTPStatement httpStatement = (HTTPStatement)parentDatabaseObject;
				
				// Variables
				ConvertigoPlugin.logDebug3("   + Variables");
				for (HttpStatementVariable variable : httpStatement.getVariables()) {
					ConvertigoPlugin.logDebug3("   - [" + variable.getName() + "]");
					save(variable, bForce);
				}
				httpStatement.getVariables(true);
			}
			else if (parentDatabaseObject instanceof StepWithExpressions) {
				StepWithExpressions stepWE = (StepWithExpressions)parentDatabaseObject;
				
				// Steps
				ConvertigoPlugin.logDebug3("   + Steps");
				for (Step step : stepWE.getSteps()) {
					ConvertigoPlugin.logDebug3("   - [" + step.getName() + "]");
					save(step, bForce);
				}
				stepWE.getSteps(true);
			}
			else if (parentDatabaseObject instanceof RequestableStep) {
				RequestableStep requestableStep = (RequestableStep)parentDatabaseObject;
				
				// Variables
				ConvertigoPlugin.logDebug3("   + Variables");
				for (Variable variable : requestableStep.getVariables()) {
					ConvertigoPlugin.logDebug3("   - [" + variable.getName() + "]");
					save(variable, bForce);
				}
				requestableStep.getVariables(true);
			}
			else if (parentDatabaseObject instanceof TestCase) {
				TestCase testCase = (TestCase)parentDatabaseObject;
				
				// Variables
				ConvertigoPlugin.logDebug3("   + Variables");
				for (Variable variable : testCase.getVariables()) {
					ConvertigoPlugin.logDebug3("   - [" + variable.getName() + "]");
					save(variable, bForce);
				}
				testCase.getVariables(true);
			}
	        else if (parentDatabaseObject instanceof ScreenClass) {
	        	ScreenClass screenClass = (ScreenClass)parentDatabaseObject;
	        	
	            // Block factory
	            if (parentDatabaseObject instanceof JavelinScreenClass) {
	                BlockFactory blockFactory = ((JavelinScreenClass)parentDatabaseObject).getBlockFactory();
	                save(blockFactory, bForce);
	                ConvertigoPlugin.logDebug3("   - BlockFactory [" + blockFactory.getName() + "]");
	            }
	            
	            // Criterias
	            ConvertigoPlugin.logDebug3("   + Criterias");
	            for (Criteria criteria : screenClass.getLocalCriterias()) {
	                save(criteria, bForce);
	                ConvertigoPlugin.logDebug3("   - [" + criteria.getName() + "]");
	            }
	            
	            // Extraction rules
	            ConvertigoPlugin.logDebug3("   + Extraction rules");
	            for (ExtractionRule extractionRule : screenClass.getLocalExtractionRules()) {
	                save(extractionRule, bForce);
	                ConvertigoPlugin.logDebug3("   - [" + extractionRule.getName() + "]");
	            }
	            
	            // Sheets
	            ConvertigoPlugin.logDebug3("   + Sheets");
	            for (Sheet sheet : screenClass.getLocalSheets()) {
	                save(sheet, bForce);
	                ConvertigoPlugin.logDebug3("   - [" + sheet.getName() + "]");
	            }
	            
	            // Inherited screen classes
	            ConvertigoPlugin.logDebug3("   + Inherited screen classes");
	            for (ScreenClass inheritedScreenClass : screenClass.getInheritedScreenClasses()) {
	                save(inheritedScreenClass, bForce);
	                ConvertigoPlugin.logDebug3("   - [" + inheritedScreenClass.getName() + "]");
	            }
	            
	            screenClass.getCriterias(true);
	            screenClass.getExtractionRules(true);
	        }
	        //projectHasBeenModified(false);
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
		File dir = new File(Engine.PROJECTS_PATH + "/" + projectName + "/_data");
		return getNumberOfObjects(dir);
	}

	public int getNumberOfObjects(File dir) {
		File[] xml = dir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.getName().toLowerCase().endsWith(".xml");
			}
		});
		
		if (xml == null) return 0;
		
		File[] subdirs = dir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});
		
		int sum = 0;
		if (subdirs != null) {
			for (int i = 0; i < subdirs.length; i++) {
				sum += getNumberOfObjects(subdirs[i]);
			}
		}
		
		return sum + xml.length;
	}

    
}
