/*
* Copyright (c) 2009-2014 Convertigo. All Rights Reserved.
*
* The copyright to the computer  program(s) herein  is the property
* of Convertigo.
* The program(s) may  be used  and/or copied  only with the written
* permission  of  Convertigo  or in accordance  with  the terms and
* conditions  stipulated  in the agreement/contract under which the
* program(s) have been supplied.
*
* Convertigo makes  no  representations  or  warranties  about  the
* suitability of the software, either express or implied, including
* but  not  limited  to  the implied warranties of merchantability,
* fitness for a particular purpose, or non-infringement. Convertigo
* shall  not  be  liable for  any damage  suffered by licensee as a
* result of using,  modifying or  distributing this software or its
* derivatives.
*/

/*
 * $URL: http://sourceus.twinsoft.fr/svn/convertigo/CEMS_opensource/branches/6.3.x/Studio/src/com/twinsoft/convertigo/eclipse/popup/actions/ProjectStatsAction.java $
 * $Author: jmc $
 * $Revision: 33092 $
 * $Date: 2014-01-02 12:44:33 +0100 (Thu, 02 Jan 2014) $
 */

package com.twinsoft.convertigo.eclipse.popup.actions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Criteria;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ExtractionRule;
import com.twinsoft.convertigo.beans.core.Pool;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Reference;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Sheet;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.screenclasses.JavelinScreenClass;
import com.twinsoft.convertigo.beans.statements.HTTPStatement;
import com.twinsoft.convertigo.beans.statements.HandlerStatement;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.beans.transactions.JavelinTransaction;
import com.twinsoft.convertigo.beans.transactions.JsonHttpTransaction;
import com.twinsoft.convertigo.beans.transactions.SiteClipperTransaction;
import com.twinsoft.convertigo.beans.transactions.SqlTransaction;
import com.twinsoft.convertigo.beans.transactions.XmlHttpTransaction;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;
import com.twinsoft.convertigo.engine.proxy.translated.ProxyTransaction;

public class ProjectStatsAction extends MyAbstractAction {
	
	int depth = 0;

	public ProjectStatsAction() {
		super();
	}

	@Override
	public void run() {
		final Display display = Display.getDefault();
		final Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		final Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
        try {
    		ProjectExplorerView explorerView = getProjectExplorerView();
    		if (explorerView != null) {
    			try {
        			ProjectTreeObject projectTreeObject = (ProjectTreeObject)explorerView.getFirstSelectedTreeObject();
        			Project project = (Project) projectTreeObject.getObject();

					new WalkHelper() {
    					int connectorCount = 0;
    					int htmlScreenclassCount = 0;
    					int htmlCriteriaCount = 0;
    					int htmlExtractionRuleCount = 0;
    					int htmlTransactionVariableCount = 0;
    					int sqlTransactionVariableCount = 0;
    					int javelinScreenclassCount = 0;
    					int javelinCriteriaCount = 0;
    					int javelinExtractionRuleCount = 0;
    					int statementCount = 0;
    					int poolCount = 0;
    					int handlerstatementCount = 0;
    					int reqVariableCount = 0;
    					int sequenceVariableCount = 0;
    					int transactionVariableCount = 0;
    					int testcaseVariableCount = 0;
    					int testcaseCount = 0;
    					int sequenceCount = 0;
    					int stepCount = 0;
    					int sheetCount = 0;
    					int referenceCount = 0;

    					/*
    					 * transaction counters
    					 */
    					int transactionWithVariablesCount = 0;
    					int htmltransactionCount = 0;
    					int jsonHttpTransactionCount = 0;
    					int xmlHttpTransactionCount = 0;
    					int javelinTransactionCount = 0;
    					int proxyTransactionCount = 0;
    					int siteClipperTransactionCount = 0;
    					int sqlTransactionCount = 0;
    					int transactionCount = 0;
    					
    					public void go(DatabaseObject project) {
    						try {
    							String displayString = "";
    		                	String projectName = project.getName();                
    							
								init(project);
								
								displayString = "projectName = " + projectName + "\r\n"														// ok
										+ " connectorCount = " + connectorCount + "\r\n";													// ok

								/*
								 * html connector
								 */
								if (htmltransactionCount > 0) {
									displayString += 
										"\r\nHTML connector\r\n"
										+ " screenclassCount = " + htmlScreenclassCount + "\r\n"											// ok
										+ " criteriaCount = " + htmlCriteriaCount + "\r\n"
										+ " extractionRuleCount = " + htmlExtractionRuleCount + "\r\n"
										+ " transactionCount = " + htmltransactionCount + "\r\n"											// ok
										+ " transactionVariableCount = " + htmlTransactionVariableCount + "\r\n"
										+ " statementCount (handlers=" + handlerstatementCount + ", statements=" + statementCount +  ", total=" + (int)(handlerstatementCount + statementCount) + ")\r\n"
										+ "\r\n";
								}						

								/*
								 * javelin connector
								 */
								if (javelinScreenclassCount > 0) {
									displayString += 
										"\r\nJavelin connector\r\n"
										+ " screenclassCount = " + javelinScreenclassCount + "\r\n"											// ok
										+ " criteriaCount = " + javelinCriteriaCount + "\r\n"
										+ " extractionRuleCount = " + javelinExtractionRuleCount + "\r\n"
										+ " transactionCount = " + javelinTransactionCount + "\r\n"											// ok
										+ "\r\n";
								}						
								
								/*
								 * SQL connector
								 */
								if (sqlTransactionCount > 0) {
									displayString += 
										"\r\nSQL connector\r\n"
										+ " sqltransactionCount = " + sqlTransactionCount + "\r\n"											// ok
										+ " transactionVariableCount = " + sqlTransactionVariableCount + "\r\n"
										+ " sheetCount = " + sheetCount + "\r\n"
										+ "\r\n";
								}

								/*
								 * Sequencer
								 */
								if (sequenceCount > 0) {
									displayString += 
										"\r\nSequencer\r\n"
										+ " sequenceCount = " + sequenceCount + "\r\n"														// ok
										+ " stepCount = " + stepCount + "\r\n"																// ok
										+ " reqVariableCount = " + reqVariableCount + "\r\n"
										+ "\r\n";
								}
								
								displayString += 
										" poolCount = " + poolCount + "\r\n"
										+ " variableCount = " + sequenceVariableCount + "\r\n";
								
								if (referenceCount > 0) {
									displayString +=
										"\r\nReferences\r\n"
										+ " referenceCount = " + referenceCount + "\r\n";
								}
								
								if (testcaseCount > 0) {
									displayString +=
										"\r\nTest cases\r\n"
										+ " testcaseCount = " + testcaseCount + "\r\n"
										+ " testcaseVariableCount = " + testcaseVariableCount + "\r\n";
								}
								
								// System.out.println(displayString);
								
								MessageBox messageBox = new MessageBox(shell, SWT.OK | SWT.ICON_INFORMATION | SWT.APPLICATION_MODAL);
								messageBox.setMessage(displayString);
								messageBox.open();
								
							} catch (Exception e) {
								e.printStackTrace();
							}
    					}
    					
						@Override
						protected void walk(DatabaseObject databaseObject) throws Exception {
							depth++;
							
							String name = databaseObject.getName();
							
							if (databaseObject instanceof Connector) {    								
								Connector connector = (Connector) databaseObject;
								connectorCount++;
							}							
							else
							if (databaseObject instanceof Reference) {    								
								Reference reference = (Reference) databaseObject;
								referenceCount++;
							}							
							else
							if (databaseObject instanceof JavelinScreenClass) {    								
								JavelinScreenClass screenclass = (JavelinScreenClass) databaseObject;
								javelinScreenclassCount++;
							}
							else 
							if (databaseObject instanceof Criteria) {
								Criteria criteria = (Criteria) databaseObject;
								
								if (databaseObject.getParent() instanceof JavelinScreenClass) {																
									javelinCriteriaCount++;
								}
								else {
									htmlCriteriaCount++;
								}
							}
							else
							if (databaseObject instanceof ExtractionRule) {
								ExtractionRule extractionRule = (ExtractionRule) databaseObject;
								
								if (databaseObject.getParent() instanceof JavelinScreenClass) {																
									javelinExtractionRuleCount++;
								}
								else {
									htmlExtractionRuleCount++;
								}
							}
							else
							if (databaseObject instanceof ScreenClass) {    								
								ScreenClass screenclass = (ScreenClass) databaseObject;
								htmlScreenclassCount++;
							}
							else
							if (databaseObject instanceof Transaction) {
								if (databaseObject instanceof TransactionWithVariables) {
									if (databaseObject instanceof HtmlTransaction) {
										HtmlTransaction htmlTransaction = (HtmlTransaction) databaseObject;
										htmltransactionCount++;
									}
									else
									if (databaseObject instanceof JsonHttpTransaction) {
										JsonHttpTransaction jsonHttpTransaction = (JsonHttpTransaction) databaseObject;
										jsonHttpTransactionCount++;
									}
									else
									if (databaseObject instanceof XmlHttpTransaction) {
										XmlHttpTransaction XmlHttpTransaction = (XmlHttpTransaction) databaseObject;
										xmlHttpTransactionCount++;
									}								
									else
									if (databaseObject instanceof ProxyTransaction) {
										ProxyTransaction proxyTransaction = (ProxyTransaction) databaseObject;
										proxyTransactionCount++;
									}
									else
									if (databaseObject instanceof SiteClipperTransaction) {
										SiteClipperTransaction javelinTransaction = (SiteClipperTransaction) databaseObject;
										siteClipperTransactionCount++;
									}
									else
									if (databaseObject instanceof JavelinTransaction) {
										JavelinTransaction javelinTransaction = (JavelinTransaction) databaseObject;
										javelinTransactionCount++;
									}
									else
									if (databaseObject instanceof SqlTransaction) {
										SqlTransaction javelinTransaction = (SqlTransaction) databaseObject;
										sqlTransactionCount++;
									}
								}
								else { // transaction with no variables
									Transaction transaction = (Transaction) databaseObject;
									transactionCount++;
								}
							}
							else // deal with statements
							if (databaseObject instanceof Statement) {
								// System.out.println(databaseObject.getClass().getName() + "\r\n");
								if (databaseObject instanceof HandlerStatement) {
									HandlerStatement handlerStatement = (HandlerStatement) databaseObject;
									handlerstatementCount++;
								}
								else { 				
									Statement statement = (Statement) databaseObject;
									statementCount++;
								}
							}
							else // deal with variables
							if (databaseObject instanceof Variable) {
								if (databaseObject.getParent() instanceof TestCase) { 
    								Variable variable = (Variable) databaseObject;
    								testcaseVariableCount++;
								}
								else
								if (databaseObject.getParent() instanceof Transaction) {
									if (databaseObject.getParent() instanceof HtmlTransaction) {
										Variable variable = (Variable) databaseObject;
										htmlTransactionVariableCount++;
									}
									else
									if (databaseObject.getParent() instanceof SqlTransaction) {
										Variable variable = (Variable) databaseObject;
										sqlTransactionVariableCount++;
									}
									else {
										Variable variable = (Variable) databaseObject;
										transactionVariableCount++;
									}
								}
								else
								if (databaseObject.getParent() instanceof Sequence) { 
    								Variable variable = (Variable) databaseObject;
    								sequenceVariableCount++;
								}
								
								if (databaseObject instanceof RequestableVariable) {    
									RequestableVariable variable = (RequestableVariable) databaseObject;
									reqVariableCount++;
								}
							}
							else
							if (databaseObject instanceof TestCase) {    
								TestCase testCase = (TestCase) databaseObject;
								testcaseCount++;
							}
							else
							if (databaseObject instanceof Sequence) {    
								Sequence sequence = (Sequence) databaseObject;
								sequenceCount++;
							}
							else
							if (databaseObject instanceof Step) {    
								Step step = (Step) databaseObject;
								stepCount++;
							}
							else
							if (databaseObject instanceof Sheet) {    
								Sheet sheet = (Sheet) databaseObject;
								sheetCount++;
							}
							else
							if (databaseObject instanceof Pool) {    
								Pool pool = (Pool) databaseObject;
								poolCount++;
							}
							
							super.walk(databaseObject);
						}				
						
					}.go(project);
					
    			} catch (Exception e) {
    				// Just ignore, should never happen
    			}
    		}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to compute statistics of the project!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }        
	}

}
