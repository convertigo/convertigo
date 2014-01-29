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
 * $URL: http://sourceus.twinsoft.fr/svn/convertigo/CEMS_opensource/branches/6.3.x/Studio/src/com/twinsoft/convertigo/eclipse/popup/actions/CustomStatsWizardPage.java $
 * $Author: jmc $
 * $Revision: 33092 $
 * $Date: 2014-01-15 12:44:33 +0100 (Thu, 02 Jan 2014) $
 */

package com.twinsoft.convertigo.eclipse.wizards;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.beans.connectors.CicsConnector;
import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector;
import com.twinsoft.convertigo.beans.connectors.SqlConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Criteria;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ExtractionRule;
import com.twinsoft.convertigo.beans.core.Pool;
import com.twinsoft.convertigo.beans.core.Project;
// import com.twinsoft.convertigo.beans.core.Reference;
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
import com.twinsoft.convertigo.beans.screenclasses.SiteClipperScreenClass;
import com.twinsoft.convertigo.beans.statements.HandlerStatement;
import com.twinsoft.convertigo.beans.steps.SimpleStep;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.beans.transactions.HttpTransaction;
import com.twinsoft.convertigo.beans.transactions.JavelinTransaction;
import com.twinsoft.convertigo.beans.transactions.JsonHttpTransaction;
import com.twinsoft.convertigo.beans.transactions.SiteClipperTransaction;
import com.twinsoft.convertigo.beans.transactions.SqlTransaction;
import com.twinsoft.convertigo.beans.transactions.XmlHttpTransaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectTreeObject;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;
import com.twinsoft.convertigo.engine.proxy.translated.ProxyTransaction;
// import com.twinsoft.convertigo.beans.core.Reference;

public class CustomStatsWizardPage extends WizardPage {

    protected Color FOREGROUND_COLOR = Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);
    protected Color BACKGROUND_COLOR = Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
    protected Color FOREGROUND_SELECTED_COLOR = Display.getDefault().getSystemColor(SWT.COLOR_BLUE);
    protected Color BACKGROUND_SELECTED_COLOR = Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW);    

    protected Cursor handCursor;
    protected CLabel currentSelectedItem = null;
    
    private Map<String, Image> icons = new Hashtable<String, Image>(256);
    
	Text labelDisplayString = null;
    
    public CustomStatsWizardPage(String pageName) {
		super(pageName);
	}

	public CustomStatsWizardPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}
	
	@Override
	public void dispose() {
		super.dispose();
		
    	for (Image image : icons.values()) {
            if (image != null)
            	image.dispose();
        }
    	icons.clear();
		
		if (handCursor != null)
			handCursor.dispose();
	}

    void computeStats(ProjectExplorerView explorerView) {
        try {
    		if (explorerView != null) {
    			try {
    				// explorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
        			ProjectTreeObject projectTreeObject = (ProjectTreeObject)explorerView.getFirstSelectedTreeObject();
        			Project project = (Project) projectTreeObject.getObject();

					new WalkHelper() {
						String displayString = "";

						int depth = 0;
						int sequenceJavascriptLines;
						int sequenceJavascriptFunction;
						
    					int connectorCount = 0;    					
						int httpConnectorCount = 0;
						int httpsConnectorCount = 0;
						int htmlConnectorCount = 0;
						int cicsConnectorCount = 0;
						int siteClipperConnectorCount = 0;
						int sqlConnectorCount = 0;
						int javelinConnectorCount = 0;
    					
    					int htmlScreenclassCount = 0;
    					int htmlCriteriaCount = 0;
    					int siteClipperScreenclassCount = 0;
    					int siteClipperCriteriaCount = 0;
    					int htmlExtractionRuleCount = 0;
    					int htmlTransactionVariableCount = 0;
    					
    					int sqlTransactionVariableCount = 0;
    					int javelinTransactionVariableCount = 0;
    					int javelinScreenclassCount = 0;
    					int javelinCriteriaCount = 0;
    					int javelinExtractionRuleCount = 0;
    					int javelinEntryHandlerCount = 0;
    					int javelinExitHandlerCount = 0;
    					int javelinHandlerCount = 0;
    					int javelinJavascriptLines = 0;
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
    					int selectInQueryCount = 0;

    					/*
    					 * transaction counters
    					 */
    					int transactionWithVariablesCount = 0;
    					int htmltransactionCount = 0;
    					int httpTransactionCount = 0;
    					int httpsTransactionCount = 0;
    					int xmlHttpTransactionCount = 0;
    					int xmlHttpsTransactionCount = 0;
    					int jsonHttpTransactionCount = 0;
    					int jsonHttpsTransactionCount = 0;
    					int proxyTransactionCount = 0;
    					int siteClipperTransactionCount = 0;
    					int javelinTransactionCount = 0;
    					int sqlTransactionCount = 0;
    					int transactionCount = 0;
    					int totalC8oObjects = 0;
    					
    					public void go(DatabaseObject project) {
    						try {
    		                	String projectName = project.getName();                
    							
								init(project);
								
								connectorCount = htmlConnectorCount + httpConnectorCount + httpsConnectorCount + cicsConnectorCount + siteClipperConnectorCount + sqlConnectorCount + javelinConnectorCount;
								
								totalC8oObjects = 1  
										+ connectorCount	// connectors
										+ htmlScreenclassCount
										+ htmlCriteriaCount
										+ htmlExtractionRuleCount
										+ htmlTransactionVariableCount
										+ handlerstatementCount 
										+ statementCount
										+ javelinScreenclassCount
										+ javelinCriteriaCount
										+ javelinExtractionRuleCount
										+ javelinTransactionCount
										+ javelinEntryHandlerCount
										+ javelinExitHandlerCount
										+ javelinHandlerCount
										+ javelinTransactionVariableCount
										+ sqlTransactionCount
										+ sqlTransactionVariableCount
										+ sheetCount
										+ jsonHttpTransactionCount
										+ jsonHttpsTransactionCount
										+ xmlHttpTransactionCount
										+ xmlHttpsTransactionCount
										+ httpTransactionCount
										+ httpsTransactionCount
										+ proxyTransactionCount
										+ siteClipperTransactionCount
										+ siteClipperScreenclassCount
										+ siteClipperCriteriaCount
										+ sequenceCount
										+ stepCount
										+ sequenceVariableCount
										+ sequenceJavascriptFunction
										+ poolCount
										+ referenceCount
										+ testcaseCount
										+ testcaseVariableCount;
								
								displayString = "projectName = " + projectName + " contains " + totalC8oObjects + " objects\r\n"			// ok
										+ " connectorCount = " + connectorCount + "\r\n\r\n";												// ok

								/*
								 * html connector
								 */
								if (htmlConnectorCount > 0) {
									displayString += 
										"HTML connector\r\n"
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
								if (javelinConnectorCount > 0) {
									displayString += 
										"Legacy connector (3270, 5250, VT220 etc...)\r\n"
										+ " screenclassCount = " + javelinScreenclassCount + "\r\n"											// ok
										+ " criteriaCount = " + javelinCriteriaCount + "\r\n"
										+ " extractionRuleCount = " + javelinExtractionRuleCount + "\r\n"
										+ " transactionCount = " + javelinTransactionCount + "\r\n"											// ok
										+ " handlerCount (Entry = " + javelinEntryHandlerCount + ", Exit = " + javelinExitHandlerCount + ", Handlers = " + javelinHandlerCount + "), total = "
										+ (int)(javelinEntryHandlerCount + javelinExitHandlerCount + javelinHandlerCount) + " in " + javelinJavascriptLines + " lines\r\n"										
										+ " variableCount = " + javelinTransactionVariableCount + "\r\n"
										+ "\r\n";
								}						
								
								/*
								 * SQL connector
								 */
								if (sqlConnectorCount > 0) {
									displayString += 
										"SQL connector\r\n"
										+ " SqltransactionCount = " + sqlTransactionCount + "\r\n"											// ok
										+ " SelectInQueryCount = " + selectInQueryCount + "\r\n"											// ok
										+ " TransactionVariableCount = " + sqlTransactionVariableCount + "\r\n";

									if (sheetCount > 0) {
										displayString += 
												"\r\nSheets\r\n" 
												+ " sheetCount = " + sheetCount + "\r\n";
									}

									displayString += "\r\n";
								}

								/*
								 * Http connector
								 */
								if (httpConnectorCount > 0) {
									displayString += 
										"HTTP connector\r\n"
										+ " JsonTransactionCount = " + jsonHttpTransactionCount + "\r\n"										// ok
										+ " XmlTransactionCount = " + xmlHttpTransactionCount + "\r\n"											// ok
										+ " HttpTransactionCount = " + httpTransactionCount + "\r\n"											// ok
										+ "\r\n";
								}						

								/*
								 * Https connector
								 */
								if (httpsConnectorCount > 0) {
									displayString += 
										"HTTPS connector\r\n"
										+ " JsonTransactionCount = " + jsonHttpsTransactionCount + "\r\n"										// ok
										+ " XmlTransactionCount = " + xmlHttpsTransactionCount + "\r\n"											// ok
										+ " HttpTransactionCount = " + httpsTransactionCount + "\r\n"											// ok
										+ "\r\n";
								}						
								
								/*
								 * Proxy connector
								 */
								if (proxyTransactionCount > 0) {
									displayString += 
										"Proxy connector\r\n"
										+ " TransactionCount = " + proxyTransactionCount + "\r\n"											// ok
										+ "\r\n";
								}						


								/*
								 * Siteclipper connector
								 */
								if (siteClipperTransactionCount > 0) {
									displayString += 
										"SiteClipper connector\r\n"
										+ " TransactionCount = " + siteClipperTransactionCount + "\r\n"										// ok
										+ " screenclassCount = " + siteClipperScreenclassCount + "\r\n"										// ok
										+ " criteriaCount = " + siteClipperCriteriaCount + "\r\n"
										+ "\r\n";
								}						

								/*
								 * Sequencer
								 */
								if (sequenceCount > 0) {
									displayString += 
										"Sequencer\r\n"
										+ " sequenceCount = " + sequenceCount + "\r\n"														// ok
										+ " stepCount = " + stepCount + "\r\n"																// ok
										+ " variableCount = " + sequenceVariableCount + "\r\n"
										+ " javascriptCode = " + sequenceJavascriptFunction + " functions in " + sequenceJavascriptLines + " lines"
										+  ((boolean)(sequenceJavascriptFunction == 0) ? " (declarations or so)\r\n":"\r\n")
										+ "\r\n";
								}
								
// 								displayString += " reqVariableCount = " + reqVariableCount + "\r\n";

								if (poolCount > 0) {
									displayString +=
										"Pools\r\n"
										+ " poolCount = " + poolCount + "\r\n";
								}
								
								if (referenceCount > 0) {
									displayString +=
										"References\r\n"
										+ " referenceCount = " + referenceCount + "\r\n";
								}
								
								if (testcaseCount > 0) {
									displayString +=
										"Test cases\r\n"
										+ " testcaseCount = " + testcaseCount + "\r\n"
										+ " testcaseVariableCount = " + testcaseVariableCount + "\r\n";
								}
								
								labelDisplayString.setText(displayString);
								
							} catch (Exception e) {
								e.printStackTrace();
							}
    					}
    					
						@Override
						protected void walk(DatabaseObject databaseObject) throws Exception {
							depth++;
							
							// String name = databaseObject.getName();
							
							// deal with connectors
							if (databaseObject instanceof Connector) {
								if (databaseObject instanceof HtmlConnector) {
									htmlConnectorCount++;
								}
								else
								if (databaseObject instanceof HttpConnector) {
									if (((HttpConnector)databaseObject).isHttps()) 
										httpsConnectorCount++;
									else
										httpConnectorCount++;
								}
								else
								if (databaseObject instanceof CicsConnector) {
									cicsConnectorCount++;
								}
								else
								if (databaseObject instanceof SiteClipperConnector) {
									siteClipperConnectorCount++;
								}
								else
								if (databaseObject instanceof SqlConnector) {
									sqlConnectorCount++;
								}
								else
								if (databaseObject instanceof JavelinConnector) {
									javelinConnectorCount++;
								}
							}					
/*							
							else
							if (databaseObject instanceof Reference) {    								
								referenceCount++;
							}
*/														
							else // deal with screenclasses
							if (databaseObject instanceof ScreenClass) {
								if (databaseObject instanceof JavelinScreenClass) {	// deal with javelinScreenClasses    								
									javelinScreenclassCount++;
								}
								else 
								if (databaseObject instanceof SiteClipperScreenClass) {	// deal with siteClipperScreenClasses    								
									siteClipperScreenclassCount++;
								}
								else {												// deal with html ScreenClasses
									htmlScreenclassCount++;
								}
							}
							else 
							if (databaseObject instanceof Criteria) {
								if (databaseObject.getParent() instanceof JavelinScreenClass) {																
									javelinCriteriaCount++;
								}
								else
								if (databaseObject.getParent() instanceof SiteClipperScreenClass) {																
									siteClipperCriteriaCount++;
								}
								else {
									htmlCriteriaCount++;
								}
							}
							else
							if (databaseObject instanceof ExtractionRule) {
								if (databaseObject.getParent() instanceof JavelinScreenClass) {																
									javelinExtractionRuleCount++;
								}
								else {
									htmlExtractionRuleCount++;
								}
							}
							else
							if (databaseObject instanceof Transaction) {
								if (databaseObject instanceof TransactionWithVariables) {
									if (databaseObject instanceof HtmlTransaction) {
										htmltransactionCount++;
									}
									else
									if (databaseObject instanceof JsonHttpTransaction) {
										if (((HttpConnector)databaseObject.getParent()).isHttps())
											jsonHttpsTransactionCount++;
										else
											jsonHttpTransactionCount++;
									}
									else
									if (databaseObject instanceof HttpTransaction) {
										if (((HttpConnector)databaseObject.getParent()).isHttps())
											httpsTransactionCount++;
										else
											httpTransactionCount++;
									}
									else
									if (databaseObject instanceof XmlHttpTransaction) {
										if (((HttpConnector)databaseObject.getParent()).isHttps())
											xmlHttpsTransactionCount++;
										else
											xmlHttpTransactionCount++;
									}								
									else
									if (databaseObject instanceof ProxyTransaction) {
										proxyTransactionCount++;
									}
									else
									if (databaseObject instanceof SiteClipperTransaction) {
										siteClipperTransactionCount++;
									}
									else
									if (databaseObject instanceof JavelinTransaction) {
										JavelinTransaction javelinTransaction = (JavelinTransaction)databaseObject;

										// Functions
										String line;
										int lineNumber = 0;
										BufferedReader br = new BufferedReader(new StringReader(javelinTransaction.handlers));

										while ((line = br.readLine()) != null) {
											line = line.trim();
											lineNumber++;
											if (line.startsWith("function ")) {
												try {
													String functionName = line.substring(9, line.indexOf(')') + 1);
													
													if (functionName.endsWith(JavelinTransaction.EVENT_ENTRY_HANDLER + "()")) {
														// TYPE_FUNCTION_SCREEN_CLASS_ENTRY
														javelinEntryHandlerCount++;
													} else if (functionName.endsWith(JavelinTransaction.EVENT_EXIT_HANDLER + "()")) {
														// TYPE_FUNCTION_SCREEN_CLASS_EXIT
														javelinExitHandlerCount++;
													} else {
														// TYPE_OTHER
														javelinHandlerCount++;
													}
												} catch(StringIndexOutOfBoundsException e) {
													// Ignore
												}
											}
										}

										// compute total number of lines of javascript
										javelinJavascriptLines += lineNumber;
										
										javelinTransactionCount++;
									}
									else
									if (databaseObject instanceof SqlTransaction) {
										SqlTransaction sqlTransaction = (SqlTransaction)databaseObject;
										/*
										 * count the number of SELECT
										 */
										String query = sqlTransaction.getSqlQuery();
										if (query != null) {
											query = query.toLowerCase();
											String pattern = "select";
											int lastIndex = 0;

											while(lastIndex != -1) {
												lastIndex = query.indexOf(pattern, lastIndex);
											    if (lastIndex != -1) {
											    	selectInQueryCount++;
											    	lastIndex += pattern.length();
											    }
											}
										}
										
										sqlTransactionCount++;
									}
								}
								else { // transaction with no variables
									transactionCount++;
								}
							}
							else // deal with statements
							if (databaseObject instanceof Statement) {
								// System.out.println(databaseObject.getClass().getName() + "\r\n");
								if (databaseObject instanceof HandlerStatement) {
									handlerstatementCount++;									
								}
								else { 				
									statementCount++;
								}
							}
							else // deal with variables
							if (databaseObject instanceof Variable) {
								if (databaseObject.getParent() instanceof Transaction) {
									if (databaseObject.getParent() instanceof JavelinTransaction) {
										javelinTransactionVariableCount++;
									}
									else
									if (databaseObject.getParent() instanceof HtmlTransaction) {
										htmlTransactionVariableCount++;
									}
									else
									if (databaseObject.getParent() instanceof SqlTransaction) {
										sqlTransactionVariableCount++;
									}
									else { // should be zero
										transactionVariableCount++;
									}
								}
								else
								if (databaseObject.getParent() instanceof Sequence) { 
    								sequenceVariableCount++;
								}
								else
								if (databaseObject.getParent() instanceof TestCase) { 
    								testcaseVariableCount++;
								}
							}
							else
							if (databaseObject instanceof TestCase) {    
								testcaseCount++;
							}
							else
							if (databaseObject instanceof Sequence) {    
								sequenceCount++;
							}
							else
							if (databaseObject instanceof Step) {
								if (databaseObject instanceof SimpleStep) {
									SimpleStep simpleStep = (SimpleStep)databaseObject;
									
									// Functions
									String line;
									int lineNumber = 0;
									BufferedReader br = new BufferedReader(new StringReader(simpleStep.getExpression()));

									while ((line = br.readLine()) != null) {
										line = line.trim();
										lineNumber++;
										if (line.startsWith("function ")) {
											try {
												sequenceJavascriptFunction++;
											} catch(StringIndexOutOfBoundsException e) {
												// Ignore
											}
										}
									}

									sequenceJavascriptLines += lineNumber;
									stepCount++;
								}
								else
									stepCount++;
							}
							else
							if (databaseObject instanceof Sheet) {    
								sheetCount++;
							}
							else
							if (databaseObject instanceof Pool) {    
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
        }        
    }
	
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		
		Composite compositeObjects = new Composite(container, SWT.NONE);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		//gridData.horizontalSpan = 2;
		gridData.verticalAlignment = GridData.BEGINNING;
		gridData.grabExcessVerticalSpace = true;
		compositeObjects.setLayoutData(gridData);
		compositeObjects.setLayout(new FillLayout());
		
		labelDisplayString = new Text(compositeObjects, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI | SWT.WRAP);
		labelDisplayString.setText("");

		setControl(container);
		
		computeStats(ConvertigoPlugin.getDefault().getProjectExplorerView());
	}
}
