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

package com.twinsoft.convertigo.eclipse.editors.connector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.event.EventListenerList;
import javax.xml.transform.TransformerException;

import org.apache.commons.httpclient.HttpStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XPath;
import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Criteria;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.core.StatementWithExpressions;
import com.twinsoft.convertigo.beans.extractionrules.HtmlExtractionRule;
import com.twinsoft.convertigo.beans.screenclasses.HtmlScreenClass;
import com.twinsoft.convertigo.beans.statements.HTTPStatement;
import com.twinsoft.convertigo.beans.statements.HandlerStatement;
import com.twinsoft.convertigo.beans.statements.ScEntryHandlerStatement;
import com.twinsoft.convertigo.beans.statements.ScExitHandlerStatement;
import com.twinsoft.convertigo.beans.statements.ScHandlerStatement;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.editors.CompositeListener;
import com.twinsoft.convertigo.eclipse.editors.connector.htmlconnector.DomTreeComposite;
import com.twinsoft.convertigo.eclipse.editors.connector.htmlconnector.HtmlXpathEvaluatorComposite;
import com.twinsoft.convertigo.eclipse.editors.connector.htmlconnector.XpathEvaluatorComposite;
import com.twinsoft.convertigo.eclipse.learnproxy.http.gui.HttpProxyEvent;
import com.twinsoft.convertigo.eclipse.learnproxy.http.gui.HttpProxyEventListener;
import com.twinsoft.convertigo.eclipse.moz.IWebViewerStudio;
import com.twinsoft.convertigo.eclipse.moz.XulWebViewerImpl;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ConnectorTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TransactionTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.eclipse.wizards.new_object.NewObjectWizard;
import com.twinsoft.convertigo.eclipse.wizards.new_statement.StatementGeneratorWizard;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineEvent;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EngineListener;
import com.twinsoft.convertigo.engine.KeyExpiredException;
import com.twinsoft.convertigo.engine.MaxCvsExceededException;
import com.twinsoft.convertigo.engine.ObjectWithSameNameException;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.MimeType;
import com.twinsoft.convertigo.engine.parsers.DocumentCompletedListener;
import com.twinsoft.convertigo.engine.parsers.HtmlParser;
import com.twinsoft.convertigo.engine.parsers.SelectionChangedListener;
import com.twinsoft.convertigo.engine.parsers.WebViewerTabManager;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.TwsCachedXPathAPI;
import com.twinsoft.util.TextCodec;

public class HtmlConnectorDesignComposite extends Composite implements EngineListener, ISelectionChangedListener, HttpProxyEventListener {


	private HtmlConnector htmlConnector = null;
	private HtmlScreenClass detectedScreenClass = null;
	private HtmlTransaction xmlizingTransaction = null;
	private HtmlTransaction runningTransaction = null;

	/** Parts of the editor */
	private HtmlXpathEvaluatorComposite xpathEvaluator = null;
	private DomTreeComposite domTreeComp = null;
	
	/** Toolbar */
	private ToolBar objectToolBar = null;
	private ToolBar learnToolBar = null;
	private ToolItem toolLearn = null;
	private ToolItem toolAccumulate = null;
	private ToolItem toolShowScreenclass = null;
	private ToolItem toolGenerateXml = null;
	private ToolItem toolStopTransaction = null;
	private ToolItem toggleAutoRefresh = null;
	
	
	private SashForm sashForm = null;
	private Composite upperPanel = null;
	private SashForm lowerSashForm = null;

	private WebViewerTabManager tabManager = null;
	private Image imageLearn = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/next_node.png"));
	private Image imageAccumulate = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/next_node.png"));
	private Image imageShowScreenclass = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/sync.png"));
	private Image imageAutoRefresh = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/autorefresh.png"));
	private Image imageGenerateXml = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/xml.png"));
	private Image imageStop = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/stop.d.png"));
	private Image imageDisableStop = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/stop.png"));
	
	private Text currentAbsoluteXpath = null;

	private ProjectExplorerView projectExplorerView = null;
	
	private SelectionChangedListener selectionChangedListener = new SelectionChangedListener() {
		
		public void changed() {
			getWebViewer().setFocus();
			Thread th = new Thread(new Runnable() {
				
				public void run() {					
					final Document dom = domTreeComp.getCurrentDom();
					Display.getDefault().asyncExec(new Runnable() {
						
						public void run () {
							try {
								String selectedXpath = getWebViewer().getSelectedXpath();

								NodeList nl = getXpathApi().selectNodeList(dom, selectedXpath);
								if (nl.getLength() == 0) {
									boolean bOk = MessageDialog.openConfirm(
											null,
											"Html tree synchronization",
											"The selected element was'nt found in the current HTML tree, would you like to synchronize it with the current HTML page displayed ?"
									);
									if (bOk) {
										// Synchronize the HTML Tree
										domTreeComp.displayXhtml(getWebViewer().getDom());

										// Hightlight the selected item
										selectedXpath = getWebViewer().getSelectedXpath();
									}
								}
								setCurrentAbsoluteXpath(selectedXpath);
								domTreeComp.selectElementInTree(selectedXpath);
							} catch (TransformerException e) {
								ConvertigoPlugin.logException(e, "Xpath not valid");
							}
						}
					});
				}
			});
			th.setName("Document completed Update");
			th.start();
		}
	};
	
	public DocumentCompletedListener documentCompletedListener = new DocumentCompletedListener() {
		public void completed() {
			ConvertigoPlugin.logDebug2("(HtmlConnectorDesignComposite) document completed");

			toolShowScreenclass.setEnabled(!htmlConnector.isLearning());
			toolGenerateXml.setEnabled(!htmlConnector.isLearning());

			if (toggleAutoRefresh.getSelection()) {
				Thread th = new Thread(new Runnable() {
					
					public void run() {
						Document dom = getWebViewer().getDom();
						for (int i = 0 ; i < 10 && dom == null; i++) {
							try {
								Thread.sleep(500);
							} catch (InterruptedException e) { }
							dom = getWebViewer().getDom();
						}
						
						if (dom != null) {
							final Document finalDom = dom;
							Display.getDefault().asyncExec(new Runnable() {
								public void run () {
									domTreeComp.displayXhtml(finalDom);
								}
							});
	
							if (htmlConnector.isLearning()) {
								HtmlTransaction htmlTransaction = (HtmlTransaction) htmlConnector.getLearningTransaction();
								htmlTransaction.setCurrentXmlDocument(dom);
								try {
									detectedScreenClass = htmlConnector.getCurrentScreenClass();
									Engine.theApp.fireObjectDetected(new EngineEvent(detectedScreenClass));
									ConvertigoPlugin.logDebug2("(HtmlConnectorDesignComposite) detected screen class '" + detectedScreenClass.getName() + "'");
								} catch (EngineException e) {
									ConvertigoPlugin.logInfo("Engine exception occurs: "+e.getMessage());
								}
							}
						}
					}
					
				});
				th.setName("Document completed Update");
				th.start();
			}
		}
	};

	public HtmlConnectorDesignComposite(Connector connector, Composite parent, int style) throws MaxCvsExceededException, KeyExpiredException {
		super(parent, style);
		htmlConnector = (HtmlConnector)connector;

		projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();

		// add ProjectExplorerView to the listeners of the associated html connector
		if (projectExplorerView != null) {
			projectExplorerView.addSelectionChangedListener(this);
			addCompositeListener(projectExplorerView);
		}

		// Set parser and add it as listener of change in HttpState
		HtmlParser htmlParser = new HtmlParser(null);
		htmlConnector.addHttpStateListener(htmlParser);
		htmlConnector.setHtmlParser(htmlParser);
		htmlConnector.context.htmlParser = htmlParser;

		try {
			initialize();
			if (htmlConnector.context.httpState == null) {
				htmlConnector.resetHttpState(htmlConnector.context);
			}
			domTreeComp.getTwsDomTree().addMenuMaker(xpathEvaluator.makeXPathMenuMaker(true));
			domTreeComp.getTwsDomTree().addMenuMaker(xpathEvaluator.makeStatementGeneratorsMenuMaker(true));
			domTreeComp.getTwsDomTree().addKeyAccelerator(xpathEvaluator.makeXPathKeyAccelerator(true));
		} catch (MaxCvsExceededException e) {
			// Remove ProjectExplorerView from listeners current composite view
			if (projectExplorerView != null) {
				projectExplorerView.removeSelectionChangedListener(this);
				removeCompositeListener(projectExplorerView);
			}
			// Release parser and remove it from HttpState listeners
			htmlConnector.removeHttpStateListener(htmlParser);
			htmlConnector.setHtmlParser(null);

			// Remove Studio context
			Engine.theApp.contextManager.remove(htmlConnector.context);

			dispose();
			
			throw e;
		} catch (KeyExpiredException e) {
			// Remove ProjectExplorerView from listeners current composite view
			if (projectExplorerView != null) {
				projectExplorerView.removeSelectionChangedListener(this);
				removeCompositeListener(projectExplorerView);
			}
			// Release parser and remove it from HttpState listeners
			htmlConnector.removeHttpStateListener(htmlParser);
			htmlConnector.setHtmlParser(null);

			// Remove Studio context
			Engine.theApp.contextManager.remove(htmlConnector.context);

			dispose();

			throw e;
		}

		// Registering as Engine listener
		Engine.theApp.addEngineListener(this);		
	}

	public TwsCachedXPathAPI getXpathApi() {
		return htmlConnector.context.getXpathApi();
	}

	private void initialize() throws MaxCvsExceededException, KeyExpiredException {
		setSize(new org.eclipse.swt.graphics.Point(435, 336));
		GridLayout gl = new GridLayout(1, false);
		gl.horizontalSpacing = gl.verticalSpacing = gl.marginWidth = gl.marginHeight = 0;
		
		setLayout(gl);
		setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		
		Composite absoluteXpathPan = new Composite(this, SWT.BORDER);
		absoluteXpathPan.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));
		gl = new GridLayout(10, false);
		gl.horizontalSpacing = gl.verticalSpacing = gl.marginWidth = gl.marginHeight = 0;
		absoluteXpathPan.setLayout(gl);
		
		createToolBars(absoluteXpathPan);
		
		new Label(absoluteXpathPan, SWT.NONE).setText("Current selection : ");
		
		currentAbsoluteXpath = new Text(absoluteXpathPan, SWT.BORDER);
		currentAbsoluteXpath.setEditable(false);
		currentAbsoluteXpath.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		createSashForm();
	}

	public void setCurrentAbsoluteXpath(String absoluteXpath) {
		currentAbsoluteXpath.setText(absoluteXpath);
	}

	public void close() {
		// Stop learning if needed
		stopLearn();

		// Remove ProjectExplorerView from listeners current composite view
		if (projectExplorerView != null) {
			projectExplorerView.removeSelectionChangedListener(this);
			removeCompositeListener(projectExplorerView);
		}

		// Deregister as Engine listener
		Engine.theApp.removeEngineListener(this);

		// Release parser and remove it from HttpState listeners
		HtmlParser htmlParser = htmlConnector.getHtmlParser();
		htmlConnector.removeHttpStateListener(htmlParser);
		htmlParser.release();

		// Reset
		htmlConnector.context = null;
		htmlConnector.setCurrentXmlDocument(null);
		htmlConnector.setHtmlParser(null);
	}

	@Override
	public void dispose() {
		//TODO:imageAttrib.dispose();
		//TODO:imageNode.dispose();
		imageLearn.dispose();
		imageAccumulate.dispose();
		imageShowScreenclass.dispose();
		imageGenerateXml.dispose();
		super.dispose();
	}

	private EventListenerList compositeListeners = new EventListenerList();

	public void addCompositeListener(CompositeListener compositeListener) {
		compositeListeners.add(CompositeListener.class, compositeListener);
	}

	public void removeCompositeListener(CompositeListener compositeListener) {
		compositeListeners.remove(CompositeListener.class, compositeListener);
	}

	public void fireObjectSelected(CompositeEvent compositeEvent) {
		// Guaranteed to return a non-null array
		Object[] listeners = compositeListeners.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2 ; i >= 0 ; i -= 2) {
			if (listeners[i] == CompositeListener.class) {
				((CompositeListener) listeners[i+1]).objectSelected(compositeEvent);
			}
		}
	}

	public void fireObjectChanged(CompositeEvent compositeEvent) {
		// Guaranteed to return a non-null array
		Object[] listeners = compositeListeners.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2 ; i >= 0 ; i -= 2) {
			if (listeners[i] == CompositeListener.class) {
				((CompositeListener) listeners[i+1]).objectChanged(compositeEvent);
			}
		}
	}

	/**
	 * This method initializes toolBar	
	 *
	 */
	private void createToolBars(Composite toolbars) {
		//Composite toolbars = new Composite(this, SWT.BORDER);
		//toolbars.setLayout(new GridLayout(4,false));
		
		// auto refresh
		ToolBar toggleAutoRefreshToolbar = new ToolBar(toolbars, SWT.NONE);
		
		toggleAutoRefresh = new ToolItem(toggleAutoRefreshToolbar, SWT.CHECK);
		toggleAutoRefresh.setToolTipText("Toggle auto refresh domTree");
		toggleAutoRefresh.setImage(imageAutoRefresh);
		toggleAutoRefresh.setSelection(true);
		toggleAutoRefresh.setEnabled(true);
		
		
		
		// DatabaseObject toolbar
		objectToolBar = new ToolBar(toolbars, SWT.NONE);
		toolShowScreenclass = new ToolItem(objectToolBar, SWT.PUSH);
		toolShowScreenclass.setToolTipText("Show current screen class");
		toolShowScreenclass.setImage(imageShowScreenclass);
		toolShowScreenclass.addSelectionListener(new org.eclipse.swt.events.SelectionListener() {
			
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				showCurrentScreenClass();
			}
			
			public void widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent e) {
			}
			
		});
		toolShowScreenclass.setEnabled(false);

		toolGenerateXml = new ToolItem(objectToolBar, SWT.PUSH);
		toolGenerateXml.setImage(imageGenerateXml);
		toolGenerateXml.setToolTipText("Generate XML");
		toolGenerateXml.addSelectionListener(new org.eclipse.swt.events.SelectionListener() {
			
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				generateXml();
			}

			public void widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent e) {
			}
			
		});
		toolGenerateXml.setEnabled(false);

		toolStopTransaction = new ToolItem(new ToolBar(toolbars, SWT.NONE), SWT.PUSH);
		toolStopTransaction.setDisabledImage(imageStop);
		toolStopTransaction.setToolTipText("Stop the current transaction");
		toolStopTransaction.setImage(imageDisableStop);
		toolStopTransaction.setEnabled(false);
		toolStopTransaction.addSelectionListener(new org.eclipse.swt.events.SelectionListener() {
			
					public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
						try {
		                	/*if (Engine.getProperty(EngineProperties.ConfigurationProperties.DOCUMENT_THREADING_USE_STOP_METHOD).equalsIgnoreCase("true")) {
		                		runningTransaction.runningThread.stop();
		                	}
		                	else {
		                		runningTransaction.runningThread.bContinue = false;
		                	}*/
							htmlConnector.context.abortRequestable();
						}
						catch(NullPointerException npe) {
							// Silently ignore: means the runningTransaction pointer has been set to null
							// because of normal transaction termination... 
						}
					}
					
					public void widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent e) {
					}
					
				});

		// Learn toolbar
		learnToolBar = new ToolBar(toolbars, SWT.NONE);

		toolLearn = new ToolItem(learnToolBar, SWT.CHECK);
		toolLearn.setToolTipText("Learn");
		toolLearn.setImage(imageLearn);
		toolLearn.addSelectionListener(new org.eclipse.swt.events.SelectionListener() {
			
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				if (toolLearn.getSelection()) {
					startLearn();
					toolAccumulate.setEnabled(true);
				} else {
					stopLearn();
					toolAccumulate.setSelection(false);
					toolAccumulate.setEnabled(false);
				}
			}
			
			public void widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent e) {
			}
			
		});
		toolLearn.setEnabled(false);

		toolAccumulate = new ToolItem(learnToolBar, SWT.CHECK);
		toolAccumulate.setToolTipText("Accumulate learning mode");
		toolAccumulate.setImage(imageAccumulate);
		toolAccumulate.addSelectionListener(new org.eclipse.swt.events.SelectionListener() {
			
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				if (toolAccumulate.getSelection()) {
					htmlConnector.setAccumulate(true);
				} else {
					htmlConnector.setAccumulate(false);
				}
			}
			
			public void widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent e) {
			}
			
		});
		toolAccumulate.setEnabled(false);
		
		new ToolItem(learnToolBar, SWT.SEPARATOR);
	}

	/**
	 * This method initializes sashForm	
	 * @throws MaxCvsExceededException 
	 * @throws KeyExpiredException 
	 *
	 */
	private void createSashForm() throws MaxCvsExceededException, KeyExpiredException {
		sashForm = new SashForm(this, SWT.BORDER);
		sashForm.setOrientation(org.eclipse.swt.SWT.VERTICAL);
		sashForm.setLayoutData(new org.eclipse.swt.layout.GridData(SWT.FILL, SWT.FILL, true, true));
		createComposite1();
		xpathEvaluator = new HtmlXpathEvaluatorComposite(sashForm, SWT.NONE, this);
		sashForm.setWeights(new int[]{80, 20});
		//sashForm.setSashWidth(3);
	}

	/**
	 * This method initializes composite1	
	 * @throws MaxCvsExceededException 
	 * @throws KeyExpiredException 
	 *
	 */
	private void createComposite1() throws MaxCvsExceededException, KeyExpiredException {
		GridLayout gl = new GridLayout();
		gl.horizontalSpacing = gl.marginWidth = gl.marginHeight = gl.verticalSpacing = 0;
		upperPanel = new Composite(sashForm, SWT.NONE);
		upperPanel.setLayout(gl);
		createUpperSashForm();
	}

	/**
	 * This method initializes lowerSashForm	
	 * @throws MaxCvsExceededException 
	 * @throws KeyExpiredException 
	 *
	 */
	private void createUpperSashForm() throws MaxCvsExceededException, KeyExpiredException {
		lowerSashForm = new SashForm(upperPanel, SWT.NONE);
		lowerSashForm.setOrientation(SWT.HORIZONTAL );
		lowerSashForm.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true));
		domTreeComp = new DomTreeComposite(lowerSashForm, SWT.None, this);
		createWebViewer();
		lowerSashForm.setWeights(new int[]{20,80});
		//lowerSashForm.setSashWidth(3);
	}



	/**
	 * This method initializes webViewer	
	 * @throws MaxCvsExceededException 
	 * @throws KeyExpiredException 
	 *
	 */
	private void createWebViewer() throws MaxCvsExceededException, KeyExpiredException {
		// Retrieve Basic credentials of connector
		String user = htmlConnector.getAuthUser();
		String password = htmlConnector.getAuthPassword();
		user = user.equals("") ? null : user;

		IWebViewerStudio webViewer = new XulWebViewerImpl(htmlConnector.context,lowerSashForm, SWT.NONE);
		tabManager = webViewer.getTabManager();
		tabManager.addChangeListener(new SelectionChangedListener() {
			
			public void changed() {
				documentCompletedListener.completed();
			}
			
		});
		webViewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		webViewer.setCredentials(user,password);
		ConvertigoPlugin.logDebug2("(HtmlConnectorDesignComposite) Connector credentials has been set: " + user + ",******");

		// Set webViewer as viewer for connector html parser
		htmlConnector.getHtmlParser().setWebViewer(webViewer);
		webViewer.setUrl(htmlConnector.getBaseUrl());

		webViewer.addSelectionChangedListener(selectionChangedListener);
		webViewer.addDocumentCompletedListener(documentCompletedListener);
	}

	public HtmlScreenClass getParentHtmlScreenClass() throws EngineException {
		HtmlScreenClass parentObject = null;

		// Case of Learning mode
		if (htmlConnector.isLearning()) {
			// In case of ScreenClass was deleted
			if ((detectedScreenClass != null) && (detectedScreenClass.getParent() == null)) {
				detectedScreenClass = null;
			}

			// Set parent ScreenClass to last detected ScreenClass
			parentObject = detectedScreenClass;
		}

		if (parentObject == null) {
			// Set parent ScreenClass to current selected ScreenClass
			Object object = projectExplorerView.getFirstSelectedDatabaseObject();
			if (object != null && object instanceof HtmlScreenClass) {
				parentObject = (HtmlScreenClass) object;
				if (!parentObject.getProject().equals(htmlConnector.getProject())) {
					parentObject = null;
				}
			}
			// Set parent ScreenClass to current ScreenClass
			if (parentObject == null) {
				parentObject = htmlConnector.getCurrentScreenClass();
			}
		}
		return parentObject;
	}

	public StatementWithExpressions getParentStatement() {
		StatementWithExpressions parentObject = null;

		// TODO Case of Learning mode
//		if (htmlConnector.isLearning()) {
//			// parent transaction is the learning transaction
//			parentObject = (HtmlTransaction)htmlConnector.getLearningTransaction();
//		}

		// Set parent statement to current selected StatementWithExpressions in properties view
		Object object = projectExplorerView.getFirstSelectedDatabaseObject();
		if (object != null && object instanceof StatementWithExpressions) {
			parentObject = (StatementWithExpressions) object;
			if (!parentObject.getProject().equals(htmlConnector.getProject())) {
				parentObject = null;
			}
		}
		
		return parentObject;
	}

	private void showCurrentScreenClass() {
		Thread th = new Thread(new Runnable() {
			public void run() {
				final Document currentWebDom = getWebViewer().getDom();

				if (currentWebDom == null) {
					ConvertigoPlugin.errorMessageBox("Mozilla retrieved Dom is null!");
					return;
				}

				//Engine.logBeans.debug3("(HtmlConnectorDesignComposite) showCurrentScreenClass dom:\n"+ XMLUtils.prettyPrintDOM(currentWebDom), null);
				ScreenClass htmlScreenClass = null;
				synchronized (htmlConnector) {
					Document currentDom = htmlConnector.getCurrentXmlDocument();
					htmlConnector.setCurrentXmlDocument(currentWebDom);
					try {
						htmlScreenClass = htmlConnector.getCurrentScreenClass();
					} catch (EngineException e) {
						ConvertigoPlugin.logInfo("Engine exception occurs: "+e.getMessage());
					}
					htmlConnector.setCurrentXmlDocument(currentDom);
				}
				fireObjectSelected(new CompositeEvent(htmlScreenClass));
			}
		});
		th.setName("Document completed Update");
		th.start();
	}

	protected void startLearn() {
		if (htmlConnector.isLearning()) {
			stopLearn();
		}

		// add current composite view to the HTTP proxy listeners
		getWebViewer().addHttpProxyEventListener(this);

		// set learning flag
		htmlConnector.markAsLearning(true);

		HtmlTransaction htmlTransaction = (HtmlTransaction)htmlConnector.getLearningTransaction();
		if (htmlTransaction == null) {
			Object object = projectExplorerView.getFirstSelectedDatabaseObject();
			if (object != null && object instanceof HtmlTransaction) {
				try {
					htmlTransaction = (HtmlTransaction) object;
					htmlTransaction.markAsLearning(true);
					ConvertigoPlugin.logDebug2("(HtmlConnector) learning transaction named '" + htmlTransaction.getName() + "'");
				}
				catch (Exception e) {}
			}
		}

		final HtmlTransaction transaction = htmlTransaction;
		Thread th = new Thread(new Runnable() {
			public void run() {
				Document dom = getWebViewer().getDom();
				transaction.setCurrentXmlDocument(dom);
			}
		});
		th.setName("Document completed Update");
		th.start();

		if (!toolLearn.isEnabled()) {
			toolLearn.setEnabled(true);
		}
		if (!toolLearn.getSelection()) {
			toolLearn.setSelection(true);
		}
	}

	protected void stopLearn() {
		if (!htmlConnector.isLearning()) {
			return;
		}

		// remove current composite view from HTTP proxy listeners
		getWebViewer().removeHttpProxyEventListener(this);

		HtmlTransaction htmlTransaction = (HtmlTransaction)htmlConnector.getLearningTransaction();
		htmlTransaction.setCurrentXmlDocument(null);

		// unset learning flag
		htmlConnector.markAsLearning(false);
		ConvertigoPlugin.logDebug2("(HtmlConnector) stop learning transaction named '" + htmlTransaction.getName() + "'");

		try {
			htmlTransaction.markAsLearning(false);
		} catch (EngineException e) {}

		if (toolLearn.isEnabled()) {
			toolLearn.setEnabled(false);
		}
		if (toolLearn.getSelection()) {
			toolLearn.setSelection(false);
		}
	}

	public void operationChanged(String operation) {

	}

	public void modelChanged(HttpProxyEvent event) {
		if (!checkProxySource(event)) {
			return;
		}

		String requestString = event.getRequest();
		String responseString = event.getResponse();
		boolean https = event.isHttps();
		int status = Integer.parseInt(event.getStatus());

		// do not record client redirection
		if ((status == HttpStatus.SC_MOVED_TEMPORARILY) ||
				(status == HttpStatus.SC_MOVED_PERMANENTLY) ||
				(status == HttpStatus.SC_SEE_OTHER) ||
				(status == HttpStatus.SC_TEMPORARY_REDIRECT)) {
			return;
		}

		/*if (requestString.indexOf(getServer()) == -1) {
			return;
		}*/

		Map<String, String> headers = parseResponseString(responseString);
		String	contentType = headers.get(HeaderName.ContentType.value().toLowerCase());

		// record only text/html or null Content-Type ...
		if (contentType == null) {
			return;
		}
		
		if (MimeType.Html.is(contentType) && MimeType.Plain.is(contentType)) {
			return;
		}

		ConvertigoPlugin.logDebug2("(HtmlConnectorDesignComposite) Learning statement...");

		try {
			String url, method, handlerName, transactionName, statementName, scHandlerName;
			String normalizedScreenClassName, screenClassName;
			HtmlTransaction htmlTransaction = null;
			HTTPStatement httpStatement = null;
			HtmlScreenClass htmlScreenClass = null;
			HandlerStatement handlerStatement = null;
			ScHandlerStatement scHandlerStatement = null;
			//Document dom = null;
			//Log log = null;
			int size, index1;
			boolean bContinue;

			index1 = 0;
			bContinue = true;
			normalizedScreenClassName = "Unknown";

			htmlTransaction = (HtmlTransaction)htmlConnector.getLearningTransaction();

			synchronized (htmlConnector) {
				//dom = htmlConnector.getCurrentXmlDocument();
				htmlScreenClass = htmlConnector.getCurrentScreenClass();
			}

			screenClassName = htmlScreenClass.getName();
			normalizedScreenClassName = StringUtils.normalize(htmlScreenClass.getName());
			ConvertigoPlugin.logDebug2("(HtmlConnectorDesignComposite) current screen class is '"+ screenClassName +"'");

			if (htmlTransaction != null) {
				transactionName = htmlTransaction.getName();

				ConvertigoPlugin.logDebug2("(HtmlConnectorDesignComposite) creating new HTTPStatement");
				ConvertigoPlugin.logDebug2(requestString);

				httpStatement = parseRequestString(requestString);
				httpStatement.setHttps(https);
				httpStatement.setPort(https ? 443:80);
				method = httpStatement.getMethod().toLowerCase();
				//size = httpStatement.getVariablesDefinitionSize();
				size = httpStatement.numberOfVariables();
				url = httpStatement.getUrl(htmlConnector.isHttps(), htmlConnector.getServer(), htmlConnector.getPort());

				while (bContinue) {
					statementName = method + ((index1==0)?" ":" "+index1) + " ("+ url + " - " + size +")";
					statementName = StringUtils.normalize(statementName);
					httpStatement.setName(statementName);
					httpStatement.hasChanged = true;
					httpStatement.bNew = true;

					if (htmlScreenClass == null) {
						try {
							httpStatement.priority = 0;
							htmlTransaction.addStatement(httpStatement);
							ConvertigoPlugin.logDebug2("(HtmlConnectorDesignComposite) added new HTTPStatement to default transaction '"+ transactionName +"'");
							fireObjectChanged(new CompositeEvent(htmlTransaction));
							Engine.theApp.fireObjectDetected(new EngineEvent(httpStatement));

							bContinue = false;
						}
						catch(ObjectWithSameNameException owsne) {
							index1++;
						}
					}
					else {
						if (htmlConnector.isAccumulating())
							handlerName = "on" + normalizedScreenClassName + "Exit";
						else
							handlerName = "on" + normalizedScreenClassName + "Entry";

						handlerStatement = htmlTransaction.getHandlerStatement(handlerName);
						if (handlerStatement != null) {
							try {
								handlerStatement.addStatement(httpStatement);
								ConvertigoPlugin.logDebug2("(HtmlConnectorDesignComposite) added new HTTPStatement to handler '"+ handlerName +"' of transaction '"+ transactionName +"'");
								fireObjectChanged(new CompositeEvent(handlerStatement));
								Engine.theApp.fireObjectDetected(new EngineEvent(httpStatement));

								bContinue = false;
							}
							catch(ObjectWithSameNameException owsne) {
								index1++;
							}
						}
						else {
							try {
								if (htmlConnector.isAccumulating())
									scHandlerStatement = new ScExitHandlerStatement(normalizedScreenClassName);
								else
									scHandlerStatement = new ScEntryHandlerStatement(normalizedScreenClassName);
								scHandlerName = scHandlerStatement.getName();
								scHandlerStatement.setName(scHandlerName);
								scHandlerStatement.hasChanged = true;
								scHandlerStatement.bNew = true;

								scHandlerStatement.priority = 0;
								htmlTransaction.addStatement(scHandlerStatement);
								ConvertigoPlugin.logDebug2("(HtmlConnectorDesignComposite) added new ScExitHandlerStatement '"+ handlerName +"' of transaction '"+ transactionName +"'");

								try {
									scHandlerStatement.addStatement(httpStatement);
									ConvertigoPlugin.logDebug2("(HtmlConnectorDesignComposite) added new HTTPStatement '"+ statementName +"' to ScExitHandlerStatement '"+ handlerName +"'");
									fireObjectChanged(new CompositeEvent(htmlTransaction));
									Engine.theApp.fireObjectDetected(new EngineEvent(httpStatement));

									bContinue = false;
								}
								catch(ObjectWithSameNameException owsne) {
									index1++;
								}
							}
							// Should not append
							catch(ObjectWithSameNameException owsne) {
								throw new EngineException(owsne.getMessage());
							}
						}
					}
				}
			}
			else {
				throw new EngineException("Found none learning transaction");
			}

		}
		catch (EngineException e) {
			ConvertigoPlugin.logException(e, "An exception occured while learning");
		}

	}

	public HTTPStatement parseRequestString(String requestString)
	{
		String		httpHeader, httpValue;
		String		method, uri, version, host;
		String		line, token, data;
		int			index, i = 0;

		HTTPStatement httpStatement = new HTTPStatement();

		try {
			StringTokenizer st = new StringTokenizer(requestString, "\r");
			while (st.hasMoreTokens()) {
				line =  st.nextToken();
				//line is not empty 
				if (line.trim().length() > 0) {
					//this is an header ==> <headername>:<space><headervalue>
					if ((index = line.indexOf(":")) != -1) {
						httpHeader = line.substring(1, index).toLowerCase(); 	// skip the first space
						httpValue  = line.substring(index+2).toLowerCase();	    // skip the space after ':'
						if (httpHeader.equalsIgnoreCase("host")) {
							host = httpValue;
							httpStatement.setHost(host);
						}
						else if (HeaderName.Accept.is(httpHeader) && !HeaderName.AcceptEncoding.is(httpHeader) ||
								HeaderName.Referer.is(httpHeader) ||
								HeaderName.ContentType.is(httpHeader)) {
							;// keep this header
						}
						else {
							httpHeader = HttpConnector.DYNAMIC_HEADER_PREFIX + httpHeader;// skip this header
						}
						httpStatement.addHeader(httpHeader, httpValue);
					}
					else {
						// this is the request-line with HTTP verb
						StringTokenizer stk = new StringTokenizer(line, " ");
						while (stk.hasMoreTokens()) {
							token = stk.nextToken();
							if (i == 0) {
								method = token;
								httpStatement.setMethod(method);
							}
							if (i == 1) {
								uri = token;
								if ((index = uri.indexOf("?")) != -1) {
									data = uri.substring(index+1);
									uri = uri.substring(0, index);
									parseHttpData(httpStatement, data, "GET");
								}
								// set uri as constant string
								httpStatement.setRequestUri("\""+ uri + "\"");
							}
							if (i == 2) {
								version = token;
								httpStatement.setHttpVersion(version);
							}
							i++;
						}
					}
				} else {
					// there was an empty line ==> Next token is DATA
					line =  st.nextToken();
					parseHttpData(httpStatement, line, "POST");
					break;
				}
			}
		}
		catch (Exception e) {
			;
		}
		return httpStatement;
	}

	private void parseHttpData(HTTPStatement httpStatement, String line, String method) {
		line = TextCodec.UTF8inURLDecode(line); //decode url variables

		StringTokenizer stData = new StringTokenizer(line, "&");
		String left, right, name, value, dataName;
		Object ob, dataValue;
		Map<String, Object> names;
		boolean bMulti;
		List<String> v;

		v = null;
		names = new HashMap<String, Object>();

		while (stData.hasMoreTokens()) {
			String	dataElement = stData.nextToken();
			if (dataElement.indexOf('=') != -1) {
				left = dataElement.substring(0, dataElement.indexOf('='));
				right = (dataElement.indexOf('=') < dataElement.length() ? dataElement.substring(dataElement.indexOf('=')+1):"");
				name = left.trim();
				value = right.trim();

				if (names.containsKey(name)) {
					ob = names.get(name);
					if (ob instanceof String) {
						v = new ArrayList<String>();
						v.add((String)ob);
					}
					else if (ob instanceof List) {
						v = GenericUtils.cast(ob);
					}

					if (v != null) {
						v.add(value);
						names.put(name,v);
					}
				}
				else {
					names.put(name,value);
				}
			}
		}

		for (Enumeration<String> e = Collections.enumeration(names.keySet()); e.hasMoreElements() ;) {
			dataName = e.nextElement();
			dataValue = names.get(dataName);
			bMulti = dataValue instanceof List;
			httpStatement.addData(dataName, dataValue, bMulti, method);
		}
	}

	public Map<String, String> parseResponseString(String responseString) {
		Map<String, String> htHeaders = new HashMap<String, String>();
		String		httpHeader, httpValue;
		String		line;
		int			index, i = 0;

		try {
			StringTokenizer st = new StringTokenizer(responseString, "\r");
			while (st.hasMoreTokens()) {
				line =  st.nextToken();
				//line is not empty 
				if (line.trim().length() > 0) {
					//this is an header ==> <headername>:<space><headervalue>
					if ((index = line.indexOf(":")) != -1) {
						httpHeader = line.substring(1, index).toLowerCase(); 	// skip the first space
						httpValue  = line.substring(index+2).toLowerCase();	    // skip the space after ':'
						htHeaders.put(httpHeader, httpValue);
					}
					else {
						// this is the status-line
						StringTokenizer stk = new StringTokenizer(line, " ");
						while (stk.hasMoreTokens()) {
							//token = stk.nextToken();
							if (i == 0)
								;//version = token;
							if (i == 1)
								;//status = token;
							if (i == 2)
								;//reason = token;
							i++;
						}
					}
				} else {
					// there was an empty line ==> Next token is DATA
					line =  st.nextToken();
					break;
				}
			}
		}
		catch (Exception e) {
			;
		}

		return htHeaders;
	}

	public void selectionChanged(SelectionChangedEvent event) {
		if (event.getSource() instanceof ISelectionProvider) {
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
			TreeObject treeObject = (TreeObject) selection.getFirstElement();
			if (treeObject != null) {
				ConnectorTreeObject connectorTreeObject = treeObject.getConnectorTreeObject();
				if (connectorTreeObject != null) {
					Connector connector =  (Connector)connectorTreeObject.getObject();
					if (connector.equals(htmlConnector)) {
						if (treeObject instanceof TransactionTreeObject) {
							if (!htmlConnector.isLearning())
								toolLearn.setEnabled(true);
						}
						else {
							if (!htmlConnector.isLearning())
								toolLearn.setEnabled(false);
						}
					}
					else {
						if (!htmlConnector.isLearning())
							toolLearn.setEnabled(false);
					}
				}
			}
		}
	}

	private boolean checkEventSource(EventObject event) {
		boolean isSourceFromConnector = false;
		Object source = event.getSource();
		if (event instanceof EngineEvent) {
			if (source instanceof DatabaseObject) {
				Connector connector = ((DatabaseObject)source).getConnector();
				if ((connector != null) && (connector.equals(htmlConnector)))
					isSourceFromConnector = true;
			}
		}
		return isSourceFromConnector;
	}

	private boolean checkProxySource(HttpProxyEvent event) {
		boolean isSourceFromConnector = false;
		String contextID = event.getContextID();
		if (contextID == null)
			ConvertigoPlugin.errorMessageBox("Missing contextID in HttpProxyEvent.\nPlease update convertigo HttpProxy.");

		if (contextID.equals(htmlConnector.context.contextID)) {
			isSourceFromConnector = true;
		}
		return isSourceFromConnector;
	}

	public void blocksChanged(EngineEvent engineEvent) {
		if (!checkEventSource(engineEvent))
			return;
	}

	public void documentGenerated(EngineEvent engineEvent) {
		if (!checkEventSource(engineEvent))
			return;

	}

	public void objectDetected(EngineEvent engineEvent) {
		if (!checkEventSource(engineEvent))
			return;

	}

	public void stepReached(EngineEvent engineEvent) {
		if (!checkEventSource(engineEvent))
			return;

	}

	public void transactionStarted(EngineEvent engineEvent) {
		if (!checkEventSource(engineEvent))
			return;
		runningTransaction = (HtmlTransaction) engineEvent.getSource();
		
		getDisplay().asyncExec(new Runnable(){
			public void run(){
				//TODO:treeToolBar.setEnabled(false);
				objectToolBar.setEnabled(false);
				learnToolBar.setEnabled(false);
				toolStopTransaction.setEnabled(true);
			}
		});
	}

	public void transactionFinished(EngineEvent engineEvent) {
		if (!checkEventSource(engineEvent))
			return;
		if (runningTransaction.equals(xmlizingTransaction))
			xmlizingTransaction = null;
		
		runningTransaction = null;
		getDisplay().asyncExec(new Runnable(){
			public void run(){
				//TODO:treeToolBar.setEnabled(true);
				objectToolBar.setEnabled(true);
				learnToolBar.setEnabled(true);
				toolStopTransaction.setEnabled(false);
			}
		});
	}
	
	public void clearEditor(EngineEvent engineEvent) {
		if (!checkEventSource(engineEvent))
			return;
	}

	public void sequenceFinished(EngineEvent engineEvent) {
		if (!checkEventSource(engineEvent))
			return;
	}

	public void sequenceStarted(EngineEvent engineEvent) {
		if (!checkEventSource(engineEvent))
			return;
	}
	
	private void generateXml() {
		Thread th = new Thread(new Runnable() {
			public void run() {
				final Document dom = getWebViewer().getDom();

				if (dom == null) {
					ConvertigoPlugin.errorMessageBox("Mozilla retrieved Dom is null!");
					return;
				}
				//Engine.logBeans.debug3("(HtmlConnectorDesignComposite) xmlizing dom:\n"+ XMLUtils.prettyPrintDOM(dom), null);

				// synchronized (htmlConnector) {
					Document currentDom = htmlConnector.getCurrentXmlDocument();
					
					// Report from 4.5: fix #401
					HtmlTransaction htmlTransaction = null;
					try {
						htmlTransaction = (HtmlTransaction)htmlConnector.getDefaultTransaction();
					} catch (Exception e1) {}
					
					if (htmlTransaction != null) {
						boolean isStateFull = htmlTransaction.isStateFull();
						xmlizingTransaction = htmlTransaction;
						xmlizingTransaction.setCurrentXmlDocument(dom);
						xmlizingTransaction.setStateFull(true);

						getDisplay().asyncExec(new Runnable(){
							public void run(){
								ConnectorEditor connectorEditor = ConvertigoPlugin.getDefault().getConnectorEditor(htmlConnector);
								if (connectorEditor == null)
									transactionFinished(new EngineEvent(xmlizingTransaction));
								else
									connectorEditor.getDocument(xmlizingTransaction.getName(), false);
							}
						});

						while (xmlizingTransaction != null) {
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {}
						}
						htmlTransaction.setCurrentXmlDocument(currentDom);
						htmlTransaction.setStateFull(isStateFull);
					}
				// }
			}
		});
		th.setName("Document completed Update");
		th.start();
	}

	public IWebViewerStudio getWebViewer() {
		return (IWebViewerStudio)tabManager.getCurrent();
	}

	public XpathEvaluatorComposite getXpathEvaluator(){
		return xpathEvaluator;
	}

	public DomTreeComposite getDomTreeComp() {
		return domTreeComp;
	}
	
	public void createScreenClassFromSelection() throws EngineException {
		String className = "com.twinsoft.convertigo.beans.core.ScreenClass";
		
		// Retrieve selected criteria xpath
		String criteriaXpath = xpathEvaluator.getSelectionXpath();

		// Retrieve parent ScreenClass
		HtmlScreenClass parentObject = getParentHtmlScreenClass();
		
		NewObjectWizard newObjectWizard = new NewObjectWizard(parentObject, className);
		WizardDialog wzdlg = new WizardDialog(Display.getCurrent().getActiveShell(), newObjectWizard);
		wzdlg.setPageSize(850, 650);
		wzdlg.open();
		if (wzdlg.getReturnCode() != Window.CANCEL) {
			HtmlScreenClass htmlScreenClass = (HtmlScreenClass)newObjectWizard.newBean;
			if (htmlScreenClass != null) {
				String screenClassName = htmlScreenClass.getName();
				
				// Add criteria to screen class
				createCriteria(htmlScreenClass, criteriaXpath);
				
				// Set detected ScreenClass to newly created one
				if (htmlConnector.isLearning()) {
					detectedScreenClass = htmlScreenClass;
					ConvertigoPlugin.logDebug2("(HtmlConnectorDesignComposite) Detected screen class is set to newly created one named '"+ screenClassName +"'");
				}
				else {
					ConvertigoPlugin.logDebug2("(HtmlConnectorDesignComposite) New screen class named '"+ screenClassName +"' has been added");
				}

				// Reload parent ScreenClass in Tree
				fireObjectChanged(new CompositeEvent(parentObject));

				// Set selection on newly created screenclass
				fireObjectSelected(new CompositeEvent(htmlScreenClass));

				// Highlight new detected ScreenClass in Tree
				if (htmlConnector.isLearning())
					Engine.theApp.fireObjectDetected(new EngineEvent(htmlScreenClass));

				//TODO:toolScreenclass.setEnabled(false);
			}
		}
	}
	
	public void createCriteriasFromSelection(Document dom) throws EngineException {
		String className = "com.twinsoft.convertigo.beans.core.Criteria";
		
		// Retrieve selected criterias xpath
		String criteriaXpath = xpathEvaluator.getSelectionXpath();

		// Retrieve parent ScreenClass
		HtmlScreenClass parentObject = getParentHtmlScreenClass();

		NewObjectWizard newObjectWizard = new NewObjectWizard(parentObject, className, criteriaXpath, dom);
		WizardDialog wzdlg = new WizardDialog(Display.getCurrent().getActiveShell(), newObjectWizard);
		wzdlg.setPageSize(850, 650);
		wzdlg.open();
		if (wzdlg.getReturnCode() != Window.CANCEL) {
			Criteria criteria = (Criteria)newObjectWizard.newBean;

			// Reload parent ScreenClass in Tree
			fireObjectChanged(new CompositeEvent(parentObject));

			// Set selection on last created criteria (will expand tree to new criteria)
			if (criteria != null) fireObjectSelected(new CompositeEvent(criteria));

			// Set back selection on parent ScreenClass
			fireObjectSelected(new CompositeEvent(parentObject));
		}
	}

	private XPath createCriteria(HtmlScreenClass parentObject, String criteriaXpath) {
		XPath criteria = null;
		try {
			criteria = new XPath();
			criteria.setXpath(criteriaXpath);
			criteria.hasChanged = true;
			criteria.bNew = true;
			parentObject.addCriteria(criteria);
			ConvertigoPlugin.logInfo("New criteria named '" + criteria.getName() + "' has been added to '"+ parentObject.getName() +"' screenclass");
		}
		catch (EngineException e) {
			String message = "Unable to create new criteria!";
			ConvertigoPlugin.logException(e, message);
		}
		return criteria;
	}

	public void createExtractionRuleFromSelection(Document dom) throws EngineException {
		String className = "com.twinsoft.convertigo.beans.core.ExtractionRule";
		
		// Retrieve selected extraction rule xpath
		String extractionrulesXpath = xpathEvaluator.getSelectionXpath();

		// Retrieve parent ScreenClass
		HtmlScreenClass parentObject = getParentHtmlScreenClass();

		// Add extraction rule to screen class
		NewObjectWizard newObjectWizard = new NewObjectWizard(parentObject, className, extractionrulesXpath, dom);
		WizardDialog wzdlg = new WizardDialog(Display.getCurrent().getActiveShell(), newObjectWizard);
		wzdlg.setPageSize(850, 650);
		wzdlg.open();
		if (wzdlg.getReturnCode() != Window.CANCEL) {
			HtmlExtractionRule extractionrule = (HtmlExtractionRule)newObjectWizard.newBean;
			
			// Reload parent ScreenClass in Tree
			fireObjectChanged(new CompositeEvent(parentObject));

			// Set selection on new extraction rule (will expand tree to new extraction rule)
			if (extractionrule != null) fireObjectSelected(new CompositeEvent(extractionrule));

			// Set back selection on parent ScreenClass
			fireObjectSelected(new CompositeEvent(parentObject));		
		}
	}
	
	public void createStatementFromSelection() {
		String className = "com.twinsoft.convertigo.beans.core.Statement";
		
		// Retrieve selected statement xpath
		String statementXpath = xpathEvaluator.getSelectionXpath();

		// Retrieve parent Statement
		Statement parentObject = getParentStatement();
		if (parentObject == null) {
			ConvertigoPlugin.errorMessageBox("Unable to create a new statement.\nThe selected handler belongs to a different HTML connector.");
			return;
		}
		
		// Add statement to parent statement
		NewObjectWizard newObjectWizard = new NewObjectWizard(parentObject, className, statementXpath, null);
		WizardDialog wzdlg = new WizardDialog(Display.getCurrent().getActiveShell(), newObjectWizard);
		wzdlg.setPageSize(850, 650);
		wzdlg.open();
		if (wzdlg.getReturnCode() != Window.CANCEL) {
			Statement statement = (Statement)newObjectWizard.newBean;
			
			// Reload parent statement in Tree
			fireObjectChanged(new CompositeEvent(parentObject));

			// Set selection on new statement (will expand tree to new statement)
			if (statement != null) fireObjectSelected(new CompositeEvent(statement));

			// Set back selection on parent statement
			fireObjectSelected(new CompositeEvent(parentObject));
		}
	}
	
	public void createStatementFromGenerator(Document dom) {
		// Retrieve selected statement generator xpath
		String statementGeneratorXpath = xpathEvaluator.getSelectionXpath();
		
		// retrieve element on which generate a statement 
		// every test has been done before activating the button, no need to do them again
		Element element = (Element)(dom.getDocumentElement().getChildNodes().item(0));
		boolean clickable = false, valuable = false, checkable = false, selectable = false, radioable = false, formable = false;

		if(element.getTagName().equalsIgnoreCase("A")){
			clickable = true;
		}else if(element.getTagName().equalsIgnoreCase("INPUT")){
			String type = element.getAttribute("type");
			clickable = Arrays.binarySearch( new String[]{"button", "checkbox", "radio", "submit"}, type)>-1; //warning, must be sort
			valuable = Arrays.binarySearch( new String[]{"", "password", "text"}, type)>-1; //warning, must be sort
			checkable = Arrays.binarySearch( new String[]{"checkbox", "radio"}, type)>-1; //warning, must be sort
			radioable = type.equals("radio");
		}else if(element.getTagName().equalsIgnoreCase("TEXTAREA")){
			valuable = true;
		}else if(element.getTagName().equalsIgnoreCase("SELECT")){
			selectable = true;
		}else if(element.getTagName().equalsIgnoreCase("FORM")){
			formable = true;
		}
		
		// Retrieve parent Statement
		StatementWithExpressions parentObject = getParentStatement();
		
		// launch wizard
		StatementGeneratorWizard statementGeneratorWizard = new StatementGeneratorWizard(parentObject, 
																						statementGeneratorXpath, 
																						new boolean[] {	clickable, 
																										valuable, 
																										checkable, 
																										selectable, 
																										radioable, 
																										formable
																									}
																						);
		
		WizardDialog wzdlg = new WizardDialog(Display.getCurrent().getActiveShell(), statementGeneratorWizard);
		wzdlg.open();
		if (wzdlg.getReturnCode() != Window.CANCEL) {
			// TODO
		}
	}

	public ProjectExplorerView getProjectExplorerView() {
		return projectExplorerView;
	}
	
	public HtmlConnector getHtmlConnector(){
		return htmlConnector;
	}
}