
package com.twinsoft.convertigo.eclipse.dialogs;

import java.io.BufferedReader;
import java.io.StringReader;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
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
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;
import com.twinsoft.convertigo.engine.proxy.translated.ProxyTransaction;

public class StatisticsDialog extends Dialog {
	private Display display;
	private Label topLabel;
	private Label labelImage;
	private Image imageLeft;
	private Composite descriptifRight;
	private String projectName, comment, version;
 
	/**
	 * Create the dialog.
	 * @param parentShell, errorMessage
	 */
	public StatisticsDialog(Shell parentShell, String projectName, String comment, String version) {
		super(parentShell);
		this.projectName = projectName;
		this.comment = comment;
		this.version = version;
		
		this.setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE);
	}

	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Statistics");
		newShell.setSize(800,500); 
		display = newShell.getDisplay();
	}

	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 5;
		gridLayout.numColumns = 2;
		Color white = new Color(display, 255,255,255);
		container.setLayout(gridLayout);
		
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		
		topLabel = new Label(container, SWT.NONE);
		topLabel.setText(projectName+(version.equals("") ? "" : " v"+version)+"\n"+comment);
		topLabel.setLayoutData(gridData);		
		
		gridData = new GridData(GridData.FILL_BOTH);
		gridData.grabExcessHorizontalSpace = false;
		
		imageLeft = new Image(display, getClass().getResourceAsStream("images/dialog_statistic.jpg"));
		labelImage = new Label(container, SWT.NONE);
		labelImage.setImage(imageLeft);
		labelImage.setBackground(white);
		labelImage.setLayoutData(gridData);
		
		gridData = new GridData(GridData.FILL_BOTH);
		gridData.grabExcessHorizontalSpace = true;
		descriptifRight = new Composite(container, SWT.NONE);
//		descriptifRight = new StyledText(container, SWT.READ_ONLY | SWT.V_SCROLL);
//		descriptifRight.setBackground(white);
//		descriptifRight.setLayoutData(gridData);
		
		computeStats(ConvertigoPlugin.getDefault().getProjectExplorerView());
		
		descriptifRight.setBackground(white);
		descriptifRight.setLayoutData(gridData);
		gridLayout = new GridLayout();
		descriptifRight.setLayout(gridLayout);
		
		return container;
	}
	
	private void computeStats(ProjectExplorerView explorerView) {
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
    					int javelinFunctionCount = 0;
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
    					int httptransactionCount = 0;
    					int jsonHttpTransactionCount = 0;
    					int proxyTransactionCount = 0;
    					int siteClipperTransactionCount = 0;
    					int xmlHttpTransactionCount = 0;
    					int javelinTransactionCount = 0;
    					int sqlTransactionCount = 0;
    					int transactionCount = 0;
    					int totalC8oObjects = 0;
    					
    					public void go(DatabaseObject project) {
    						try {
    		                	String projectName = project.getName();                
    							
								init(project);
								
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
										+ javelinFunctionCount
										+ javelinTransactionVariableCount
										+ sqlTransactionCount
										+ sqlTransactionVariableCount
										+ sheetCount
										+ jsonHttpTransactionCount
										+ xmlHttpTransactionCount
										+ httptransactionCount
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
								
								GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
								CLabel projectIntro = new CLabel(descriptifRight, SWT.NONE);
								
								displayString = projectName + " contains " + totalC8oObjects + " objects\r\n"			// ok
										+ " connectorCount = " + connectorCount;															// ok
								
								projectIntro.setImage(new Image(display, getClass().getResourceAsStream("images/project.png")));
								projectIntro.setText(displayString);
								projectIntro.setBackground(new Color(display, 255,255,255));
								projectIntro.setLayoutData(gridData);
								/*
								 * html connector
								 */
								
								StyledText text;
								if (htmltransactionCount > 0) {
									gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
									text = new StyledText(descriptifRight, SWT.READ_ONLY);
									
									displayString = 
										"HTML connector\r\n"
										+ " screenclassCount = " + htmlScreenclassCount + "\r\n"											// ok
										+ " criteriaCount = " + htmlCriteriaCount + "\r\n"
										+ " extractionRuleCount = " + htmlExtractionRuleCount + "\r\n"
										+ " transactionCount = " + htmltransactionCount + "\r\n"											// ok
										+ " transactionVariableCount = " + htmlTransactionVariableCount + "\r\n"
										+ " statementCount (handlers=" + handlerstatementCount + ", statements=" + statementCount +  ", total=" + (int)(handlerstatementCount + statementCount) + ")";
									
									text.setText(displayString);
									text.setLayoutData(gridData);
								}						

								/*
								 * javelin connector
								 */
								if (javelinScreenclassCount > 0) {
									gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
									text = new StyledText(descriptifRight, SWT.READ_ONLY);
									
									displayString = 
										"Javelin connector\r\n"
										+ " screenclassCount = " + javelinScreenclassCount + "\r\n"											// ok
										+ " criteriaCount = " + javelinCriteriaCount + "\r\n"
										+ " extractionRuleCount = " + javelinExtractionRuleCount + "\r\n"
										+ " transactionCount = " + javelinTransactionCount + "\r\n"											// ok
										+ " handlerCount (Entry = " + javelinEntryHandlerCount + ", Exit = " + javelinExitHandlerCount + ", Screenclass = " + javelinHandlerCount + ", functions = " + javelinFunctionCount 
										+  "), total = " + (int)(javelinEntryHandlerCount + javelinExitHandlerCount + javelinHandlerCount + javelinFunctionCount) + " in " + javelinJavascriptLines + " lines\r\n"										
										+ " variableCount = " + javelinTransactionVariableCount;
									
									text.setText(displayString);
									text.setLayoutData(gridData);
								}						
								
								/*
								 * SQL connector
								 */
								if (sqlTransactionCount > 0) {
									gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
									text = new StyledText(descriptifRight, SWT.READ_ONLY);
									
									displayString = 
										"SQL connector\r\n"
										+ " sqltransactionCount = " + sqlTransactionCount + "\r\n"											// ok
										+ " selectInQueryCount = " + selectInQueryCount + "\r\n"											// ok
										+ " transactionVariableCount = " + sqlTransactionVariableCount;

									if (sheetCount > 0) {
										displayString += 
												"\r\nSheets\r\n" 
												+ " sheetCount = " + sheetCount;
									}

									text.setText(displayString);
									text.setLayoutData(gridData);
								}

								/*
								 * Http connector
								 */
								if (jsonHttpTransactionCount > 0) {
									gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
									text = new StyledText(descriptifRight, SWT.READ_ONLY);
									
									displayString = 
										"HTTP connector\r\n"
										+ " JSONTransactionCount = " + jsonHttpTransactionCount + "\r\n"										// ok
										+ " xmlTransactionCount = " + xmlHttpTransactionCount + "\r\n"											// ok
										+ " HTTPtransactionCount = " + httptransactionCount;

									text.setText(displayString);
									text.setLayoutData(gridData);
								}						

								/*
								 * Proxy connector
								 */
								if (proxyTransactionCount > 0) {
									gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
									text = new StyledText(descriptifRight, SWT.READ_ONLY);
									
									displayString = 
										"Proxy connector\r\n"
										+ " TransactionCount = " + proxyTransactionCount;

									text.setText(displayString);
									text.setLayoutData(gridData);
								}						


								/*
								 * Siteclipper connector
								 */
								if (siteClipperTransactionCount > 0) {
									gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
									text = new StyledText(descriptifRight, SWT.READ_ONLY);
									
									displayString = 
										"SiteClipper connector\r\n"
										+ " TransactionCount = " + siteClipperTransactionCount + "\r\n"										// ok
										+ " screenclassCount = " + siteClipperScreenclassCount + "\r\n"										// ok
										+ " criteriaCount = " + siteClipperCriteriaCount;
									
									text.setText(displayString);
									text.setLayoutData(gridData);
								}						

								/*
								 * Sequencer
								 */
								if (sequenceCount > 0) {
									gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
									text = new StyledText(descriptifRight, SWT.READ_ONLY);
									
									displayString = 
										"Sequencer\r\n"
										+ " sequenceCount = " + sequenceCount + "\r\n"														// ok
										+ " stepCount = " + stepCount + "\r\n"																// ok
										+ " variableCount = " + sequenceVariableCount + "\r\n"
										+ " javascriptCode = " + sequenceJavascriptFunction + " functions in " + sequenceJavascriptLines + " lines"
										+  ((boolean)(sequenceJavascriptFunction == 0) ? " (declarations or so)":"");
									
									text.setText(displayString);
									text.setLayoutData(gridData);
								}
								
// 								displayString += " reqVariableCount = " + reqVariableCount + "\r\n";

								if (poolCount > 0) {
									gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
									text = new StyledText(descriptifRight, SWT.READ_ONLY);
									
									displayString =
										"Pools\r\n"
										+ " poolCount = " + poolCount;
									
									text.setText(displayString);
									text.setLayoutData(gridData);
									
								}
								
								if (referenceCount > 0) {
									gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
									text = new StyledText(descriptifRight, SWT.READ_ONLY);
									
									displayString =
										"References\r\n"
										+ " referenceCount = " + referenceCount;
									
									text.setText(displayString);
									text.setLayoutData(gridData);
								}
								
								if (testcaseCount > 0) {
									gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
									text = new StyledText(descriptifRight, SWT.READ_ONLY);
									
									displayString =
										"Test cases\r\n"
										+ " testcaseCount = " + testcaseCount + "\r\n"
										+ " testcaseVariableCount = " + testcaseVariableCount;
									
									text.setText(displayString);
									text.setLayoutData(gridData);
								}
								
//								descriptifRight.setText(displayString);
								
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
								connectorCount++;
							}							
							else
							if (databaseObject instanceof Reference) {    								
								referenceCount++;
							}							
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
										jsonHttpTransactionCount++;
									}
									else
									if (databaseObject instanceof HttpTransaction) {
										httptransactionCount++;
									}
									else
									if (databaseObject instanceof XmlHttpTransaction) {
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
														javelinFunctionCount++;
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
	
	/**
	 * Create contents of the button bar.
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button buttonOk = createButton(parent, IDialogConstants.OK_ID, "OK", true);
		buttonOk.setEnabled(true);
	}
}