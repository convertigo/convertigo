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

package com.twinsoft.convertigo.eclipse.editors.connector;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.text.MessageFormat;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.twinsoft.convertigo.beans.connectors.CicsConnector;
import com.twinsoft.convertigo.beans.connectors.CouchDbConnector;
import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.connectors.SapJcoConnector;
import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector;
import com.twinsoft.convertigo.beans.connectors.SqlConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IScreenClassContainer;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.popup.actions.CreateScreenClassFromSelectionZoneAction;
import com.twinsoft.convertigo.eclipse.popup.actions.CreateTagNameFromSelectionZoneAction;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ViewImageProvider;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.ContextManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineEvent;
import com.twinsoft.convertigo.engine.EngineListener;
import com.twinsoft.convertigo.engine.RequestableEngineEvent;
import com.twinsoft.convertigo.engine.enums.JsonOutput;
import com.twinsoft.convertigo.engine.enums.JsonOutput.JsonRoot;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.enums.RequestAttribute;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class ConnectorEditorPart extends Composite implements EngineListener {

	private ConnectorEditor editor = null;
	private SashForm sashForm = null;
	private Composite compositeOutput = null;
	private AbstractConnectorComposite compositeConnector = null;
	private TabFolder tabFolderOutputDesign = null;
	private Connector connector;
	private Composite compositeDesign = null;
	private Label labelNoDesign = null;
	private ToolBar toolBar = null;
	private Map<String, Integer> toolItemsIds = null;
	private Action createScreenClassFromSelectionZoneAction = null;
	private Action createTagNameFromSelectionZoneAction = null;

	private Image imageRenew = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/eclipse/editors/images/renew.png"));
	private Image imageConnect = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/eclipse/editors/images/connect.png"));
	private Image imageDisableConnect = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/eclipse/editors/images/connect.d.png"));
	private Image imageDisconnect = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/eclipse/editors/images/disconnect.png"));
	private Image imageDisableDisconnect = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/eclipse/editors/images/disconnect.d.png"));
	private Image imageReset = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/eclipse/editors/images/reset.png"));
	private Image imageRefresh = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/eclipse/editors/images/refresh.png"));
	private Image imageDebug = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/eclipse/editors/images/debug.png"));
	private Image imageDisableDebug = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/eclipse/editors/images/debug.d.png"));
	private Image imageRun = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/eclipse/editors/images/run.png"));
	private Image imageDisableRun = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/eclipse/editors/images/run.d.png"));
	private Image imagePause = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/eclipse/editors/images/pause.png"));
	private Image imageDisablePause = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/eclipse/editors/images/pause.d.png"));
	private Image imageStep = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/eclipse/editors/images/step_by_step.png"));
	private Image imageDisableStep = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/eclipse/editors/images/step_by_step.d.png"));
	private Image imageGenerate = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/beans/steps/images/transactionstep_16x16.png"));
	private Image imageRenderXml = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/eclipse/editors/images/xml.png"));
	private Image imageRenderJson = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/eclipse/editors/images/json.png"));
	private Image imageStop = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/eclipse/editors/images/stop.d.png"));
	private Image imageDisableStop = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/eclipse/editors/images/stop.png"));
	private Image imageShowScreenclass = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/eclipse/editors/images/goto_screen_class.png"));
	private Image imageLink = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/eclipse/editors/images/bound_property.png"));
	private Image imageDisableLink = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/eclipse/editors/images/bound_property.d.png"));
	private Image imageAddFromSelection = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/eclipse/editors/images/new_line.png"));
	private Image imageDisableAddFromSelection = new Image(Display.getCurrent(), getClass()
			.getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/new_line.d.png"));
	private Image imageTestConnection = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/eclipse/editors/images/test_connection.png"));
	private Image imageShowBlocks = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/eclipse/editors/images/show_blocks.png"));
	private Image imageNewScreenclass = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/eclipse/editors/images/new_screenclass.png"));
	private Image imageDisableNewScreenclass = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/eclipse/editors/images/new_screenclass.d.png"));
	private Image imageNewTagName = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/beans/common/images/tagname_color_16x16.png"));
	private Image imageRecord = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/eclipse/editors/images/record.png"));
	private Image imageLearn = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/eclipse/editors/images/next_node.png"));
	private Image imageAccumulate = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/eclipse/editors/images/next_node.png"));
	private Image imageNewWaitAt = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/eclipse/editors/images/write_wait_zone.png"));
	private Image imageDisableNewWaitAt = new Image(Display.getCurrent(), getClass().getResourceAsStream(
			"/com/twinsoft/convertigo/eclipse/editors/images/write_wait_zone.d.png"));
	private Image imageDisabledFullResult = ViewImageProvider.getImageFromCache("/com/twinsoft/convertigo/eclipse/editors/images/forward_history.d.png");
	private Image imageFullResult = ViewImageProvider.getImageFromCache("/com/twinsoft/convertigo/eclipse/editors/images/forward_history.png");
	
	private String shortResultXML;
	private String shortResultJSON;
	private String fullResultXML;
	private String fullResultJSON;
	private Map<String, String[]> lastParameters = null;
	
	private ConnectorEditorInput inputXML = null;
	private ConnectorEditorInput inputJSON = null;
	private ConnectorEditorInput inputTXT = null;


	private boolean useType;
	private JsonRoot jsonRoot;

	ConnectorEditorPart(ConnectorEditor editor, Connector connector, Composite parent, int style) {
		super(parent, style);
		this.editor = editor;
		this.connector = connector;
		this.context = getStudioContext();
		this.contextID = context.contextID;
		this.projectName = context.projectName;
		toolItemsIds = new HashMap<String, Integer>();
		
		inputXML = new ConnectorEditorInput(connector, connector.getQName() + ".xml");
		inputJSON = new ConnectorEditorInput(connector, connector.getQName() + ".json");
		inputTXT = new ConnectorEditorInput(connector, "wait.txt");
		
		initialize();

		// Registering as Engine listener
		Engine.theApp.addEngineListener(this);
	}

	private Context getStudioContext() {
		return getStudioContext(false);
	}

	private Context getStudioContext(boolean bForce) {

		String projectName = connector.getParentName();
		String connectorName = connector.getName();
		String contextType = ContextManager.CONTEXT_TYPE_TRANSACTION;
		String contextID = Engine.theApp.contextManager.computeStudioContextName(contextType, projectName, connectorName);
			
		Context ctx = Engine.theApp.contextManager.get(contextID);
		if ((ctx == null) || bForce) {
			ctx = new Context(contextID);
			ctx.contextID = contextID;
			ctx.name = contextID;
			ctx.projectName = projectName;
			ctx.setConnector(connector);
			ctx.lastAccessTime = System.currentTimeMillis();

			Engine.theApp.contextManager.add(ctx);
		}
		return ctx;
	}

	private void initialize() {
		GridLayout gridLayout1 = new GridLayout();
		gridLayout1.horizontalSpacing = 0;
		gridLayout1.marginWidth = 0;
		gridLayout1.marginHeight = 0;
		gridLayout1.verticalSpacing = 0;
		this.setLayout(gridLayout1);
		createTabFolderOutputDesign();
		setSize(new org.eclipse.swt.graphics.Point(547, 360));

		if (toolItemRenewConnector != null)
			toolItemRenewConnector.setEnabled(true);
		if (toolItemConnect != null)
			toolItemConnect.setEnabled(true);
		if (toolItemDisconnect != null)
			toolItemDisconnect.setEnabled(false);
		if (toolItemRefresh != null)
			toolItemRefresh.setEnabled(true);

		if (toolItemGenerate != null)
			toolItemGenerate.setEnabled(true);
		if (toolItemStopTransaction != null)
			toolItemStopTransaction.setEnabled(false);

		if (toolItemDebug != null)
			toolItemDebug.setEnabled(true);
		if (toolItemRun != null)
			toolItemRun.setEnabled(false);
		if (toolItemPause != null)
			toolItemPause.setEnabled(false);
		if (toolItemStep != null)
			toolItemStep.setEnabled(false);

		if (toolItemShowBlocks != null)
			toolItemShowBlocks.setEnabled(true);
		if (tooItemNewScreenClassFromSelectionZone != null)
			tooItemNewScreenClassFromSelectionZone.setEnabled(false);
		if (tooItemNewTagNameFromSelectionZone != null)
			tooItemNewTagNameFromSelectionZone.setEnabled(false);
		if (tooItemNewWaitAtFromSelectionZone != null)
			tooItemNewWaitAtFromSelectionZone.setEnabled(false);

		if (toolLearn != null)
			toolLearn.setEnabled(false);
		if (toolAccumulate != null)
			toolAccumulate.setEnabled(false);

		createScreenClassFromSelectionZoneAction = new CreateScreenClassFromSelectionZoneAction();
		createTagNameFromSelectionZoneAction = new CreateTagNameFromSelectionZoneAction();
	}

	private TabItem tabItemOutput = null;
	private TabItem tabItemDesign = null;

	/**
	 * This method initializes tabFolder
	 * 
	 */
	private void createTabFolderOutputDesign() {
		GridData gridData2 = new org.eclipse.swt.layout.GridData();
		gridData2.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData2.grabExcessVerticalSpace = true;
		gridData2.grabExcessHorizontalSpace = true;
		gridData2.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		tabFolderOutputDesign = new TabFolder(this, SWT.BOTTOM);
		tabFolderOutputDesign.setLayoutData(gridData2);
		createCompositeDesign();
		createCompositeOutput();
		tabItemOutput = new TabItem(tabFolderOutputDesign, SWT.NONE);
		tabItemOutput.setText("Output");
		tabItemOutput.setControl(compositeOutput);
		tabItemDesign = new TabItem(tabFolderOutputDesign, SWT.NONE);
		tabItemDesign.setText("Design");
		tabItemDesign.setControl(compositeDesign);
	}
	
	private void createCompositeOutput() {
		GridLayout gridLayout = new GridLayout();
		gridLayout.horizontalSpacing = 0;
		gridLayout.marginHeight = 0;
		gridLayout.verticalSpacing = 0;
		gridLayout.numColumns = 1;
		gridLayout.marginWidth = 0;
		GridData gridData = new org.eclipse.swt.layout.GridData();
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		compositeOutput = new Composite(tabFolderOutputDesign, SWT.NONE);
		compositeOutput.setLayout(gridLayout);
		createCompositeOutputHeader();
		createSashForm();
		createComposite();
	}

	private ToolItem toolItemRenewConnector = null;
	private ToolItem toolItemConnect = null;
	private ToolItem toolItemDisconnect = null;
	private ToolItem toolItemRefresh = null;
	private ToolItem toolItemReset = null;
	private ToolItem toolItemGenerate = null;
	private ToolItem toolItemRenderXml = null;
	private ToolItem toolItemRenderJson = null;
	private ToolItem toolItemStopTransaction = null;
	private ToolItem toolItemDebug = null;
	private ToolItem toolItemRun = null;
	private ToolItem toolItemPause = null;
	private ToolItem toolItemStep = null;
	private ToolItem toolItemShowBlocks = null;
	private ToolItem tooItemNewScreenClassFromSelectionZone = null;
	private ToolItem tooItemNewTagNameFromSelectionZone = null;
	private ToolItem tooItemNewWaitAtFromSelectionZone = null;
	private ToolItem toolItemLink = null;
	private ToolItem toolItemAdd = null;
	private ToolItem toolItemRecord = null;
	private ToolItem toolLearn = null;
	private ToolItem toolAccumulate = null;
	private ToolItem toolTestConnection = null;
	private ToolItem toolItemFullResult = null;

	private boolean bDebug = false;
	private boolean bShowBlocks = false;
	private boolean bDebugStepByStep = false;
	private DatabaseObject debugDatabaseObject = new Project();

	/**
	 * This method initializes toolBar
	 * 
	 */
	private void createToolBar() {
		int incr = 0;
		getConnectorCompositeClass();

		GridData gridData5 = new org.eclipse.swt.layout.GridData();
		gridData5.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData5.grabExcessHorizontalSpace = true;
		gridData5.verticalAlignment = org.eclipse.swt.layout.GridData.BEGINNING;
		toolBar = new ToolBar(compositeOutputHeader, SWT.FLAT);
		toolBar.setLayoutData(gridData5);
		
		if (IConnectable.class.isAssignableFrom(compositeConnectorClass)) {
			toolItemRenewConnector = new ToolItem(toolBar, SWT.PUSH);
			toolItemRenewConnector.setImage(imageRenew);
			toolItemRenewConnector.setToolTipText("Renew the connector");
			toolItemRenewConnector.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					compositeConnector.renew();
				}
	
				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
			toolItemsIds.put("Renew", Integer.valueOf(incr));
			incr++;
			
			toolItemConnect = new ToolItem(toolBar, SWT.PUSH);
			toolItemConnect.setToolTipText("Connect the connector");
			toolItemConnect.setImage(imageConnect);
			toolItemConnect.setDisabledImage(imageDisableConnect);
			toolItemConnect.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					((IConnectable) compositeConnector).connect();
				}

				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
			toolItemsIds.put("Connect", Integer.valueOf(incr));
			incr++;

			toolItemDisconnect = new ToolItem(toolBar, SWT.PUSH);
			toolItemDisconnect.setImage(imageDisconnect);
			toolItemDisconnect.setToolTipText("Disconnect the connector");
			toolItemDisconnect.setDisabledImage(imageDisableDisconnect);
			toolItemDisconnect.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					((IConnectable) compositeConnector).disconnect();
				}

				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
			toolItemsIds.put("Disconnect", Integer.valueOf(incr));
			incr++;
		}

		if (IRefreshable.class.isAssignableFrom(compositeConnectorClass)) {
			toolItemRefresh = new ToolItem(toolBar, SWT.PUSH);
			toolItemRefresh.setImage(imageRefresh);
			toolItemRefresh.setToolTipText("Refresh connector content");
			toolItemRefresh.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					((IRefreshable) compositeConnector).refresh();
				}

				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
			toolItemsIds.put("Refresh", Integer.valueOf(incr));
			incr++;
		}

		if (IResetable.class.isAssignableFrom(compositeConnectorClass)) {
			toolItemReset = new ToolItem(toolBar, SWT.PUSH);
			toolItemReset.setImage(imageReset);
			toolItemReset.setToolTipText("Reset the connector");
			toolItemReset.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					((IResetable) compositeConnector).reset();
				}

				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
			toolItemsIds.put("Reset", Integer.valueOf(incr));
			incr++;
		}

		if ((IScreenClassAware.class.isAssignableFrom(compositeConnectorClass))
				|| (IScreenClassContainer.class.isAssignableFrom(connector.getClass()))) {
			new ToolItem(toolBar, SWT.SEPARATOR);
			incr++;

			toolItemDebug = new ToolItem(toolBar, SWT.CHECK);
			toolItemDebug.setImage(imageDebug);
			toolItemDebug.setDisabledImage(imageDisableDebug);
			toolItemDebug.setToolTipText("Debug mode");
			toolItemDebug.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					if (ConvertigoPlugin.projectManager.currentProject == null)
						return;
					if (toolItemDebug.getSelection()) {
						try {
							ConvertigoPlugin.getDefault().debugConsoleStream
									.write("Starting debug mode in step by step state...\n");
						} catch (IOException ex) {
						}
						bDebug = true;
						bDebugStepByStep = true;
						toolItemRun.setEnabled(true);
						toolItemPause.setEnabled(false);
						toolItemStep.setEnabled(true);

						connector.markAsDebugging(true);
					} else {
						try {
							ConvertigoPlugin.getDefault().debugConsoleStream.write("Stopping debug mode.\n");
						} catch (IOException ex) {
						}
						bDebug = false;
						bDebugStepByStep = false;
						toolItemRun.setEnabled(false);
						toolItemPause.setEnabled(false);
						toolItemStep.setEnabled(false);

						synchronized (debugDatabaseObject) {
							debugDatabaseObject.notify();
						}

						connector.markAsDebugging(false);
					}
				}

				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
			toolItemsIds.put("Debug", Integer.valueOf(incr));
			incr++;

			toolItemRun = new ToolItem(toolBar, SWT.PUSH);
			toolItemRun.setImage(imageRun);
			toolItemRun.setDisabledImage(imageDisableRun);
			toolItemRun.setToolTipText("Continuous debug mode");
			toolItemRun.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					if (ConvertigoPlugin.projectManager.currentProject == null) {
						return;
					}
					try {
						ConvertigoPlugin.getDefault().debugConsoleStream
								.write("Changing debug state to continuous\n");
					} catch (IOException ex) {
					}
					bDebugStepByStep = Boolean.valueOf(false);
					toolItemRun.setEnabled(false);
					toolItemStep.setEnabled(false);
					toolItemPause.setEnabled(true);
					synchronized (debugDatabaseObject) {
						debugDatabaseObject.notify();
					}
				}

				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
			toolItemsIds.put("Run", Integer.valueOf(incr));
			incr++;

			toolItemPause = new ToolItem(toolBar, SWT.PUSH);
			toolItemPause.setImage(imagePause);
			toolItemPause.setDisabledImage(imageDisablePause);
			toolItemPause.setToolTipText("Pause the debug process");
			toolItemPause.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					if (ConvertigoPlugin.projectManager.currentProject == null) {
						return;
					}
					try {
						ConvertigoPlugin.getDefault().debugConsoleStream
								.write("Changing debug state to step by step\n");
					} catch (IOException ex) {
					}
					bDebugStepByStep = Boolean.valueOf(true);
					toolItemRun.setEnabled(true);
					toolItemStep.setEnabled(true);
					toolItemPause.setEnabled(false);
				}

				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
			toolItemsIds.put("Pause", Integer.valueOf(incr));
			incr++;

			toolItemStep = new ToolItem(toolBar, SWT.PUSH);
			toolItemStep.setImage(imageStep);
			toolItemStep.setDisabledImage(imageDisableStep);
			toolItemStep.setToolTipText("Step by step debug mode");
			toolItemStep.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					if (ConvertigoPlugin.projectManager.currentProject == null)
						return;
					synchronized (debugDatabaseObject) {
						debugDatabaseObject.notify();
						toolItemStep.setEnabled(false);
					}
				}

				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
			toolItemsIds.put("Step", Integer.valueOf(incr));
			incr++;

			new ToolItem(toolBar, SWT.SEPARATOR);
			incr++;
		}

		toolItemGenerate = new ToolItem(toolBar, SWT.PUSH);
		toolItemGenerate.setImage(imageGenerate);
		toolItemGenerate.setToolTipText(compositeConnector instanceof JavelinConnectorComposite ? "Execute" : "Execute again");
		toolItemGenerate.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				editor.setDirty(true);
				toolItemRenderJson.setEnabled(false);
				toolItemRenderXml.setEnabled(false);
				if (lastParameters != null) {
					ConvertigoPlugin.getDefault().runRequestable(projectName, lastParameters);
				} else {
					getDocument();
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		toolItemsIds.put("GenerateXML", Integer.valueOf(incr));
		incr++;

		toolItemStopTransaction = new ToolItem(toolBar, SWT.PUSH);
		toolItemStopTransaction.setDisabledImage(imageStop);
		toolItemStopTransaction.setToolTipText("Stop the current transaction");
		toolItemStopTransaction.setImage(imageDisableStop);
		toolItemStopTransaction.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				try {
					/*
					 * if
					 * (Engine.getProperty(EngineProperties.ConfigurationProperties
					 * .
					 * DOCUMENT_THREADING_USE_STOP_METHOD).equalsIgnoreCase("true"
					 * )) { runningTransaction.runningThread.stop(); } else {
					 * runningTransaction.runningThread.bContinue = false; }
					 */
					if (context != null) {
						context.abortRequestable();
					}

					// Creating a new context in order to release the lock
					// semaphore
					if (connector instanceof JavelinConnector) {
						context = getStudioContext(true);// force creation of a
															// new context
						contextID = context.contextID;
						projectName = context.projectName;
					}

				} catch (NullPointerException npe) {
					// Silently ignore: means the runningTransaction pointer has
					// been set to null
					// because of normal transaction termination...
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		toolItemsIds.put("StopTransaction", Integer.valueOf(incr));
		incr++;
		
		new ToolItem(toolBar, SWT.SEPARATOR);
		incr++;

		SelectionListener sl = new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				ConvertigoPlugin.setProperty(ConvertigoPlugin.PREFERENCE_EDITOR_OUTPUT_MODE, e.widget == toolItemRenderJson ? "json" : "xml");
				Engine.execute(() -> {
					renderDocument();
				});
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		};
		
		toolItemRenderXml = new ToolItem(toolBar, SWT.RADIO);
		toolItemRenderXml.setImage(imageRenderXml);
		toolItemRenderXml.setToolTipText("XML Requester");
		toolItemRenderXml.addSelectionListener(sl);
		toolItemsIds.put("RenderXML", Integer.valueOf(incr));
		incr ++;

		toolItemRenderJson = new ToolItem(toolBar, SWT.RADIO);
		toolItemRenderJson.setImage(imageRenderJson);
		toolItemRenderJson.setToolTipText("JSON Requester");
		toolItemRenderJson.addSelectionListener(sl);
		toolItemsIds.put("RenderJSON", Integer.valueOf(incr));

		if ("json".equals(ConvertigoPlugin.getProperty(ConvertigoPlugin.PREFERENCE_EDITOR_OUTPUT_MODE))) {
			toolItemRenderJson.setSelection(true);
		} else {
			toolItemRenderXml.setSelection(true);
		}
		incr ++;
		
		if (IScreenClassAware.class.isAssignableFrom(compositeConnectorClass)) {
			new ToolItem(toolBar, SWT.SEPARATOR);
			incr++;

			ToolItem toolItemGotoCurrentScreenClass = new ToolItem(toolBar, SWT.PUSH);
			toolItemGotoCurrentScreenClass.setImage(imageShowScreenclass);
			toolItemGotoCurrentScreenClass.setToolTipText("Go to current screen class object");
			toolItemGotoCurrentScreenClass.setText("");
			toolItemGotoCurrentScreenClass
					.addSelectionListener(new SelectionListener() {
						public void widgetSelected(SelectionEvent e) {
							((IScreenClassAware) compositeConnector).goToCurrentScreenClass();
						}

						public void widgetDefaultSelected(SelectionEvent e) {
						}
					});
			toolItemsIds.put("GoToCurrentScreenClass", Integer.valueOf(incr));
			incr++;

			if (ILinkable.class.isAssignableFrom(compositeConnectorClass)) {
				toolItemLink = new ToolItem(toolBar, SWT.PUSH);
				toolItemLink.setImage(imageLink);
				// toolItemLink.setDisabledImage(imageDisableLink);
				toolItemLink
						.setToolTipText("Link current selection to highlighted property in Properties panel");
				toolItemLink.addSelectionListener(new SelectionListener() {
					public void widgetSelected(SelectionEvent e) {
						((ILinkable) compositeConnector).link();
					}

					public void widgetDefaultSelected(SelectionEvent e) {
					}
				});
				// toolItemLink.setEnabled(false);
				toolItemLink.setEnabled(true);
				toolItemsIds.put("Link", Integer.valueOf(incr));
				incr++;
			}
			if (IAddable.class.isAssignableFrom(compositeConnectorClass)) {
				toolItemAdd = new ToolItem(toolBar, SWT.PUSH);
				toolItemAdd.setImage(imageAddFromSelection);
				toolItemAdd.setDisabledImage(imageDisableAddFromSelection);
				toolItemAdd
						.setToolTipText("Add element from current selection to highlighted property in Properties panel");
				toolItemAdd.addSelectionListener(new SelectionListener() {
					public void widgetSelected(SelectionEvent e) {
						((IAddable) compositeConnector).add();
					}

					public void widgetDefaultSelected(SelectionEvent e) {
					}
				});
				toolItemAdd.setEnabled(false);
				toolItemsIds.put("Add", Integer.valueOf(incr));
				incr++;
			}
		}
		
		if (connector instanceof SqlConnector) {	
			new ToolItem(toolBar, SWT.SEPARATOR);
			incr++;
			toolTestConnection = new ToolItem(toolBar, SWT.PUSH);

			toolTestConnection.setImage(imageTestConnection);
			toolTestConnection.setToolTipText("Test SQL connection");
			toolTestConnection.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					Thread t = Thread.currentThread();
					ClassLoader cl = t.getContextClassLoader(); 
					try {
						t.setContextClassLoader(connector.getProject().getProjectClassLoader());
						((SqlConnector) connector).open();
						
						MessageBox mb = new MessageBox(getParent().getShell(), SWT.ICON_WORKING | SWT.OK);
						mb.setMessage("Connection parameters are correct.");
						mb.open();
					} catch (Exception e1) {
						Engine.logBeans.error("Test connection failed!"+e1.getMessage());
						MessageBox mb = new MessageBox(getParent().getShell(), SWT.ICON_ERROR | SWT.OK);
						mb.setMessage("Failed to connect to the database! \n"+e1.getMessage());
						mb.open();
					} finally {
						t.setContextClassLoader(cl);
						((SqlConnector) connector).close();
					}
				}

				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
			toolItemsIds.put("Test SQL Connection", Integer.valueOf(incr));
			incr++;
			
		}

		if (IBlockizable.class.isAssignableFrom(compositeConnectorClass)) {
			new ToolItem(toolBar, SWT.SEPARATOR);
			incr++;

			toolItemShowBlocks = new ToolItem(toolBar, SWT.CHECK);
			toolItemShowBlocks.setImage(imageShowBlocks);
			toolItemShowBlocks.setToolTipText("Show Blocks");
			toolItemShowBlocks.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					if (ConvertigoPlugin.projectManager.currentProject == null)
						return;
					if (toolItemShowBlocks.getSelection()) {
						bShowBlocks = true;
						((IBlockizable) compositeConnector).showBlocks(true);
					} else {
						bShowBlocks = false;
						((IBlockizable) compositeConnector).showBlocks(false);
					}
				}

				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
			toolItemsIds.put("ShowBlocks", Integer.valueOf(incr));
			incr++;
		}

		if (IScreenClassAware.class.isAssignableFrom(compositeConnectorClass)) {
			new ToolItem(toolBar, SWT.SEPARATOR);
			incr++;

			tooItemNewScreenClassFromSelectionZone = new ToolItem(toolBar, SWT.PUSH);
			tooItemNewScreenClassFromSelectionZone.setImage(imageNewScreenclass);
			tooItemNewScreenClassFromSelectionZone.setDisabledImage(imageDisableNewScreenclass);
			tooItemNewScreenClassFromSelectionZone.setToolTipText("New ScreenClass");
			tooItemNewScreenClassFromSelectionZone
					.addSelectionListener(new SelectionListener() {
						public void widgetSelected(SelectionEvent e) {
							createScreenClassFromSelectionZoneAction.run();
						}

						public void widgetDefaultSelected(SelectionEvent e) {
						}
					});
			toolItemsIds.put("NewScreenClass", Integer.valueOf(incr));
			incr++;

			tooItemNewTagNameFromSelectionZone = new ToolItem(toolBar, SWT.PUSH);
			tooItemNewTagNameFromSelectionZone.setImage(imageNewTagName);
			tooItemNewTagNameFromSelectionZone.setToolTipText("New TagName");
			tooItemNewTagNameFromSelectionZone
					.addSelectionListener(new SelectionListener() {
						public void widgetSelected(SelectionEvent e) {
							createTagNameFromSelectionZoneAction.run();
						}

						public void widgetDefaultSelected(SelectionEvent e) {
						}
					});
			toolItemsIds.put("NewTagName", Integer.valueOf(incr));
			incr++;

			tooItemNewWaitAtFromSelectionZone = new ToolItem(toolBar, SWT.PUSH);
			tooItemNewWaitAtFromSelectionZone.setImage(imageNewWaitAt);
			tooItemNewWaitAtFromSelectionZone.setDisabledImage(imageDisableNewWaitAt);
			tooItemNewWaitAtFromSelectionZone.setToolTipText("New WaitAt");
			tooItemNewWaitAtFromSelectionZone
					.addSelectionListener(new SelectionListener() {
						public void widgetSelected(SelectionEvent e) {
							if (compositeConnector instanceof JavelinConnectorComposite) {
								((JavelinConnectorComposite) compositeConnector)
										.createWaitAtFromSelectionZone();
							}
						}

						public void widgetDefaultSelected(SelectionEvent e) {
						}
					});
			toolItemsIds.put("NewWaitAt", Integer.valueOf(incr));
			incr++;

		}

		if (IRecordable.class.isAssignableFrom(compositeConnectorClass)) {
			new ToolItem(toolBar, SWT.SEPARATOR);
			incr++;

			toolItemRecord = new ToolItem(toolBar, SWT.CHECK);
			toolItemRecord.setImage(imageRecord);
			toolItemRecord.setToolTipText("Record trace");
			toolItemRecord.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					if (ConvertigoPlugin.projectManager.currentProject == null)
						return;
					if (toolItemRecord.getSelection()) {
						((IRecordable) compositeConnector).record(true);
					} else {
						((IRecordable) compositeConnector).record(false);
					}
				}

				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
			toolItemsIds.put("Record", Integer.valueOf(incr));
			incr++;
		}

		if ((ILearnable.class.isAssignableFrom(compositeConnectorClass))
				&& (!HttpConnectorComposite.class.equals(compositeConnectorClass))
				&& (!SiteClipperConnectorComposite.class.equals(compositeConnectorClass))) {
			new ToolItem(toolBar, SWT.SEPARATOR);
			incr++;

			toolLearn = new ToolItem(toolBar, SWT.CHECK);
			toolLearn.setToolTipText("Learn");
			toolLearn.setImage(imageLearn);
			toolLearn.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					if (toolLearn.getSelection()) {
						compositeConnector.startLearn();
						toolAccumulate.setEnabled(true);
					} else {
						compositeConnector.stopLearn();
						toolAccumulate.setSelection(false);
						toolAccumulate.setEnabled(false);
					}
				}

				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
			toolItemsIds.put("Learn", Integer.valueOf(incr));
			incr++;

			toolAccumulate = new ToolItem(toolBar, SWT.CHECK);
			toolAccumulate.setToolTipText("Accumulate learning mode");
			toolAccumulate.setImage(imageAccumulate);
			toolAccumulate.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					if (toolAccumulate.getSelection())
						compositeConnector.setAccumulate(true);
					else
						compositeConnector.setAccumulate(false);
				}

				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
			toolItemsIds.put("Accumulate", Integer.valueOf(incr));
			incr++;
			
			new ToolItem(toolBar, SWT.SEPARATOR);
			incr ++;
		}
		
		toolItemFullResult = new ToolItem(toolBar, SWT.PUSH);
		toolItemFullResult.setDisabledImage(imageDisabledFullResult);
		toolItemFullResult.setToolTipText("Show the full result");
		toolItemFullResult.setImage(imageFullResult);
		toolItemFullResult.setEnabled(false);
		toolItemFullResult.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				if (toolItemRenderJson.getSelection()) {
					if (fullResultJSON != null) {
						getInput().fileWrite(fullResultJSON);
						fullResultJSON = null;
						toolItemFullResult.setEnabled(false);
					}
				} else if (fullResultXML != null) {
					getInput().fileWrite(fullResultXML);
					toolItemFullResult.setEnabled(false);
					fullResultXML = null;
				}
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		incr ++;
	}

	void toolBarSetEnable(String toolItemId, boolean enable) {
		try {
			final int i = toolItemsIds.get(toolItemId).intValue();
			final boolean enabled = enable;
			getDisplay().asyncExec(() -> {
				if (toolBar.isDisposed())
					return;
				ToolItem[] toolItems = toolBar.getItems();
				ToolItem toolItem = toolItems[i];
				if (toolItem != null)
					toolItem.setEnabled(enabled);
			});
		} catch (Exception e) {
		}
		;
	}

	void toolBarSetSelection(String toolItemId, boolean select) {
		try {
			final int i = toolItemsIds.get(toolItemId).intValue();
			final boolean selected = select;
			getDisplay().syncExec(() -> {
				if (toolBar.isDisposed())
					return;
				ToolItem[] toolItems = toolBar.getItems();
				ToolItem toolItem = toolItems[i];
				if (toolItem != null)
					toolItem.setSelection(selected);
			});
		} catch (Exception e) {
		}
		;
	}

	/**
	 * This method initializes sashForm
	 * 
	 */
	private void createSashForm() {
		GridData gridData3 = new org.eclipse.swt.layout.GridData();
		gridData3.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData3.grabExcessHorizontalSpace = true;
		gridData3.grabExcessVerticalSpace = true;
		gridData3.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		sashForm = new SashForm(compositeOutput, SWT.NONE);
		sashForm.setLayoutData(gridData3);
		createCompositeConnector();
		createCompositeXml();
	}

	private Class<?> compositeConnectorClass; // @jve:decl-index=0:

	private void getConnectorCompositeClass() {
		if (connector instanceof JavelinConnector) {
			compositeConnectorClass = JavelinConnectorComposite.class;
		} else if (connector instanceof HttpConnector) {
			compositeConnectorClass = HttpConnectorComposite.class;
		} else if (connector instanceof CicsConnector) {
			compositeConnectorClass = CicsConnectorComposite.class;
		} else if (connector instanceof SqlConnector) {
			compositeConnectorClass = SqlConnectorComposite.class;
		} else if (connector instanceof SapJcoConnector) {
			compositeConnectorClass = SapJcoConnectorComposite.class;
		} else if (connector instanceof SiteClipperConnector) {
			compositeConnectorClass = SiteClipperConnectorComposite.class;
		} else if (connector instanceof CouchDbConnector) {
			compositeConnectorClass = CouchDbConnectorComposite.class;
		} else {
			throw new IllegalArgumentException("The connector class is not handled: "
					+ connector.getClass().getName());
		}
	}

	/**
	 * This method initializes compositeConnector
	 * 
	 */
	private void createCompositeConnector() {
		GridLayout gridLayout2 = new GridLayout();
		gridLayout2.horizontalSpacing = 0;
		gridLayout2.marginWidth = 0;
		gridLayout2.marginHeight = 0;
		gridLayout2.verticalSpacing = 0;

		try {
			int connectorCompositeStyle = (connector instanceof JavelinConnector ? SWT.EMBEDDED | SWT.LEFT
					: SWT.NONE);
			Constructor<?> constructor = compositeConnectorClass.getConstructor(new Class[] {
					ConnectorEditorPart.class, Connector.class, Composite.class, int.class });
			compositeConnector = (AbstractConnectorComposite) constructor.newInstance(new Object[] { this,
					connector, sashForm, Integer.valueOf(connectorCompositeStyle) });

			compositeConnector.setParent(sashForm);
			compositeConnector.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
			compositeConnector.setLayout(gridLayout2);
		} catch (Exception e) {
			ConvertigoPlugin.logException(e,
					"An unexpected exception has occured while creating the connector composite.");
		}

		// test if the compositeConnector needs a zoneListener
		// if (ILinkable.class.isAssignableFrom(compositeConnectorClass)) {
		// ToolItem[] ti_tab = toolBar.getItems();
		// 
		// int i = ((Integer)toolItemsIds.get("Link")).intValue();
		// ((ILinkable) compositeConnector).monitor(ti_tab[i]);
		// }
	}

	public Connector getConnector() {
		return connector;
	}

	public AbstractConnectorComposite getConnectorComposite() {
		return compositeConnector;
	}

	public Composite getConnectorDesignComposite() {
		return compositeDesign;
	}

	/**
	 * This method initializes compositeXml
	 * 
	 */
	private void createCompositeXml() {
		compositeXml = new Composite(sashForm, SWT.NONE);
		compositeXml.setLayout(new FillLayout());

		editor.createEditorControl(compositeXml);
	}

	/**
	 * This method initializes compositeDesign
	 * 
	 */
	private void createCompositeDesign() {
		if (connector instanceof SapJcoConnector) {
			try {
				compositeDesign = new SapJcoConnectorDesignComposite(connector, tabFolderOutputDesign, SWT.NONE);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if (connector instanceof SqlConnector) {
			try {
				compositeDesign = new SqlConnectorDesignComposite(connector, tabFolderOutputDesign, SWT.NONE);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			compositeDesign = new Composite(tabFolderOutputDesign, SWT.NONE);
			labelNoDesign = new Label(compositeDesign, SWT.NONE | SWT.WRAP);
			labelNoDesign.setFont(new Font(null, "Tahoma", 10, 0));
			labelNoDesign.setBounds(new org.eclipse.swt.graphics.Rectangle(10, 10, 300, 200));
			labelNoDesign.setText("This connector does not provide any design tool");
		}
	}

	void close() {
		connector.markAsDebugging(false);

		compositeConnector.close();
		if (compositeDesign instanceof SapJcoConnectorDesignComposite) {
			((SapJcoConnectorDesignComposite) compositeDesign).close();
		}

		// Remove Studio context
		Engine.theApp.contextManager.remove(context);
		connector.release();

		// Deregister as Engine listener
		Engine.theApp.removeEngineListener(this);

	}

	@Override
	public void dispose() {
		imageRenew.dispose();
		imageConnect.dispose();
		imageDisableConnect.dispose();
		imageDisconnect.dispose();
		imageDisableDisconnect.dispose();
		imageRefresh.dispose();
		imageDebug.dispose();
		imageDisableDebug.dispose();
		imageRun.dispose();
		imageDisableRun.dispose();
		imagePause.dispose();
		imageDisablePause.dispose();
		imageStep.dispose();
		imageDisableStep.dispose();
		imageGenerate.dispose();
		imageRenderXml.dispose();
		imageRenderJson.dispose();
		imageStop.dispose();
		imageDisableStop.dispose();
		imageShowScreenclass.dispose();
		imageLink.dispose();
		imageDisableLink.dispose();
		imageAddFromSelection.dispose();
		imageDisableAddFromSelection.dispose();
		imageShowBlocks.dispose();
		imageNewScreenclass.dispose();
		imageDisableNewScreenclass.dispose();
		imageNewTagName.dispose();
		imageRecord.dispose();
		imageLearn.dispose();
		imageAccumulate.dispose();
		imageNewWaitAt.dispose();
		imageDisableNewWaitAt.dispose();
		imageDisabledFullResult.dispose();
		imageFullResult.dispose();
		
		super.dispose();
	}

	private void getDocument() {
		getDocument(null, null, null, false);
	}

	private Context context;
	private String contextID = null;
	private String projectName = null;

	void getDocument(String transactionName, String testcaseName, String stubFileName, boolean isStubRequested) {
		final Map<String, String[]> parameters = new HashMap<String, String[]>();
		
		editor.setDirty(true);
		toolItemRenderJson.setEnabled(false);
		toolItemRenderXml.setEnabled(false);

		parameters.put(Parameter.Connector.getName(), new String[]{connector.getName()});

		if (transactionName != null) {
			parameters.put(Parameter.Transaction.getName(), new String[]{transactionName});
		}

		parameters.put(Parameter.Context.getName(), new String[]{contextID});

		if (testcaseName != null) {
			parameters.put(Parameter.Testcase.getName(), new String[]{testcaseName});
		}

		if (stubFileName != null) {
			parameters.put(Parameter.StubFilename.getName(), new String[]{stubFileName});
		}
		
		if (isStubRequested) {
			parameters.put(Parameter.Stub.getName(), new String[]{"true"});
		}

		ConvertigoPlugin.getDefault().runRequestable(projectName, parameters);
	}

	public void transactionStarted(EngineEvent engineEvent) {
		if (!checkEventSource(engineEvent))
			return;
		clearEditor(engineEvent);
		if (engineEvent.getSource() instanceof Transaction tr) {
			context = tr.context;
			RequestAttribute.debug.set(tr.context.httpServletRequest, bDebug);
			useType = context.project != null && context.project.getJsonOutput() == JsonOutput.useType;
			jsonRoot = context.project != null ? context.project.getJsonRoot() : JsonRoot.docNode;
		}
		getDisplay().syncExec(() -> {
			toolItemStopTransaction.setEnabled(true);
			toolItemGenerate.setEnabled(false);
			toolItemRenderJson.setEnabled(false);
			toolItemRenderXml.setEnabled(false);
		});
	}

	public void transactionFinished(EngineEvent engineEvent) {
		if (!checkEventSource(engineEvent))
			return;
		
		if (!(compositeConnector instanceof JavelinConnectorComposite) && context != null) {
			lastParameters = new HashMap<>(context.httpServletRequest.getParameterMap());
		}
		
		context = null;
		
		getDisplay().asyncExec(() -> {
			toolItemRenderJson.setEnabled(true);
			toolItemRenderXml.setEnabled(true);
			
			if ("json".equals(ConvertigoPlugin.getProperty(ConvertigoPlugin.PREFERENCE_EDITOR_OUTPUT_MODE))) {
				toolItemRenderJson.setSelection(true);
				toolItemRenderXml.setSelection(false);
			} else {
				toolItemRenderJson.setSelection(false);
				toolItemRenderXml.setSelection(true);
			}
			try {
				toolItemStopTransaction.setEnabled(false);
				toolItemGenerate.setEnabled(true);
			} catch (Exception e) {
			}
		});
	}

	public org.w3c.dom.Document lastGeneratedDocument;

	private Composite compositeXml = null;

	private Composite compositeOutputHeader = null;

	private Composite compositeOutputFooter = null;

	private Label label1 = null;

	private Label labelLastDetectedScreenClass = null;

	private ScreenClass lastDetectedScreenClass = null;

	private boolean checkEventSource(EventObject event) {
		if (isDisposed()) {
			Engine.theApp.removeEngineListener(this);
			return false;
		}
		boolean isSourceFromConnector = false;
		if (event instanceof RequestableEngineEvent) {
			RequestableEngineEvent requestableEvent = (RequestableEngineEvent) event;
			
			String connectorName = requestableEvent.getConnectorName();
			if (connectorName != null) {
				if (connectorName.equals(connector.getName()) && requestableEvent.getProjectName().equals(connector.getProject().getName())) {
					isSourceFromConnector = true;
				}
			}
		}
		else if (event instanceof EngineEvent) {
			Object ob = ((EngineEvent)event).getSource();
			if (ob instanceof DatabaseObject) {
				try {
					String projectName = ((DatabaseObject)ob).getProject().getName();
					String connectorName = ((DatabaseObject)ob).getConnector().getName();
					if (connectorName.equals(connector.getName()) && projectName.equals(connector.getProject().getName())) {
						isSourceFromConnector = true;
					}
				}
				catch (Exception e){}
			}
		}
		return isSourceFromConnector;
	}

	public void documentGenerated(EngineEvent engineEvent) {
		if (!checkEventSource(engineEvent))
			return;

		if (bDebug) {
			try {
				ConvertigoPlugin.getDefault().debugConsoleStream
						.write("The XML document has been successfully generated.\n");
			} catch (IOException e) {
			}
		}
		lastGeneratedDocument = (org.w3c.dom.Document) engineEvent.getSource();
		shortResultJSON = shortResultXML = fullResultJSON = fullResultXML = null;
		renderDocument();
	}
	
	private void renderDocument() {
		String str;
		boolean[] isJsonMode = {false};
		getDisplay().syncExec(() -> {
			editor.setDirty(false);
			toolItemRenderJson.setEnabled(true);
			toolItemRenderXml.setEnabled(true);
			isJsonMode[0] = toolItemRenderJson.getSelection();
			
			if (isJsonMode[0] && shortResultJSON != null) {
				if (editor.getEditorInput() != inputJSON) {
					editor.setInput(inputJSON);
				}
				toolItemFullResult.setEnabled(fullResultJSON != null);
			} else if (!isJsonMode[0] && shortResultXML != null) {
				if (editor.getEditorInput() != inputXML) {
					editor.setInput(inputXML);
				}
				toolItemFullResult.setEnabled(fullResultXML != null);
			}
		});
		
		if ((isJsonMode[0] && shortResultJSON != null) || (!isJsonMode[0] && shortResultXML != null)) {
			return;
		}
		
		ConnectorEditorInput input = getInput();
		if (isJsonMode[0]) {
			try {
				str =  XMLUtils.XmlToJson(lastGeneratedDocument.getDocumentElement(), true, useType, jsonRoot);
				str = str.replaceAll("\n( +)", "\n$1$1");
			} catch (JSONException e) {
				str = e.getMessage();
			}
			input = inputJSON;
		} else {
			str = XMLUtils.prettyPrintDOMWithEncoding(lastGeneratedDocument);
			input = inputXML;
		}
		
		boolean hasFull = str.length() > 10000;
		if (hasFull) {
			if (isJsonMode[0]) {
				fullResultJSON = str;
			} else {
				fullResultXML = str;
			}
			
			str = str.substring(0, 10000) + "\n... [reduced content, click the Full Result button in the toolbar to show the full version]";
		}
		
		if (isJsonMode[0]) {
			shortResultJSON = str;
		} else {
			shortResultXML = str;
		}
		
		input.fileWrite(str);
		
		ConnectorEditorInput i = input;
		getDisplay().asyncExec(() -> {
			toolItemFullResult.setEnabled(hasFull);
			if (editor.getEditorInput() != i) {
				editor.setInput(i);
			}
		});
	}

	public void clearEditor(EngineEvent engineEvent) {
		if (!checkEventSource(engineEvent))
			return;

		getDisplay().syncExec(() -> {
			if (!inputTXT.fileExists()) {
				inputTXT.fileWrite("Please wait during the Sequence execution.");
			}
			if (editor.getEditorInput() != inputTXT) {
				editor.setInput(inputTXT);
			}
		});
	}

	public void blocksChanged(EngineEvent engineEvent) {
		if (!checkEventSource(engineEvent))
			return;

		if (bDebug) {
			try {
				ConvertigoPlugin.getDefault().debugConsoleStream.write("The blocks vector has changed.\n");
			} catch (IOException ex) {
			}
		}
		((IBlockizable) compositeConnector).blocksChanged(engineEvent, bShowBlocks);
	}

	public void objectDetected(EngineEvent engineEvent) {
		if (!checkEventSource(engineEvent))
			return;

		final Object source = engineEvent.getSource();
		getDisplay().syncExec(() -> {
			if (source instanceof ScreenClass) {
				lastDetectedScreenClass = (ScreenClass) source;
				labelLastDetectedScreenClass.setText(lastDetectedScreenClass.getName());
				compositeOutputFooter.layout();
			}

			if (bDebug) {
				String message = MessageFormat.format(
						"The following database object has been detected: \"{0}\"\n",
						new Object[] { ((DatabaseObject) source).getName() });
				try {
					ConvertigoPlugin.getDefault().debugConsoleStream.write(message);
				} catch (IOException e) {
				}
			}
		});
	}

	public void stepReached(EngineEvent engineEvent) {
		if (!checkEventSource(engineEvent))
			return;

		if (!bDebug)
			return;
		final Object source = engineEvent.getSource();
		synchronized (debugDatabaseObject) {
			debugDatabaseObject = (DatabaseObject) source;
		}

		try {
			ConvertigoPlugin.getDefault().debugConsoleStream
					.write("Step reached after having applied database object: "
							+ debugDatabaseObject.getName() + "\n");
		} catch (IOException e1) {
		}

		if (bDebugStepByStep) {
			try {
				synchronized (debugDatabaseObject) {
					getDisplay().syncExec(() -> {
						toolItemStep.setEnabled(true);
					});
					debugDatabaseObject.wait();
				}
			} catch (InterruptedException e) {
				try {
					ConvertigoPlugin.getDefault().debugConsoleStream.write("Next step required\n");
				} catch (IOException ex) {
				}
			}
		}
	}

	public void sequenceFinished(EngineEvent engineEvent) {
		if (!checkEventSource(engineEvent))
			return;
	}

	public void sequenceStarted(EngineEvent engineEvent) {
		if (!checkEventSource(engineEvent))
			return;
	}

	/**
	 * This method initializes compositeOutputHeader
	 * 
	 */
	private void createCompositeOutputHeader() {
		final Color background = getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);

		GridLayout gridLayout3 = new GridLayout();
		gridLayout3.numColumns = 2;
		GridData gridData1 = new org.eclipse.swt.layout.GridData();
		gridData1.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData1.grabExcessHorizontalSpace = true;
		gridData1.grabExcessVerticalSpace = false;
		gridData1.verticalAlignment = org.eclipse.swt.layout.GridData.BEGINNING;
		compositeOutputHeader = new Composite(compositeOutput, SWT.NONE);
		compositeOutputHeader.setBackground(background);
		compositeOutputHeader.setLayoutData(gridData1);
		compositeOutputHeader.setLayout(gridLayout3);

		createToolBar();
	}

	/**
	 * This method initializes composite
	 * 
	 */
	private void createComposite() {
		GridData gridData7 = new org.eclipse.swt.layout.GridData();
		gridData7.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData7.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		GridData gridData4 = new org.eclipse.swt.layout.GridData();
		gridData4.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData4.grabExcessHorizontalSpace = false;
		gridData4.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		GridLayout gridLayout4 = new GridLayout();
		gridLayout4.numColumns = 2;
		compositeOutputFooter = new Composite(compositeOutput, SWT.NONE);
		compositeOutputFooter.setBackground(new Color(Display.getCurrent(), 162, 194, 250));
		compositeOutputFooter.setLayout(gridLayout4);
		compositeOutputFooter.setLayoutData(gridData4);
		label1 = new Label(compositeOutputFooter, SWT.NONE);
		label1.setBackground(new Color(Display.getCurrent(), 162, 194, 250));
		label1.setText("Last detected screen class:");
		label1.setToolTipText("Displays the current screen class name");

		labelLastDetectedScreenClass = new Label(compositeOutputFooter, SWT.NONE);
		labelLastDetectedScreenClass.setBackground(new Color(Display.getCurrent(), 162, 194, 250));
		labelLastDetectedScreenClass.setText("(unknown)");
		labelLastDetectedScreenClass.setLayoutData(gridData7);
		labelLastDetectedScreenClass.setToolTipText("Displays the current screen class name");
	}

	public ScreenClass getLastDetectedScreenClass() {
		return lastDetectedScreenClass;
	}
	
	private ConnectorEditorInput getInput() {
		return (ConnectorEditorInput) editor.getEditorInput();
	}
}
