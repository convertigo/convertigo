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

import java.awt.AWTKeyStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;

import com.twinsoft.api.Session;
import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.core.Block;
import com.twinsoft.convertigo.beans.core.BlockFactoryWithVector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.transactions.JavelinTransaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.trace.TracePlayerThread;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ConnectorTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.FolderTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TraceTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.ContextManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineEvent;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.LogParameters;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.util.Log4jHelper;
import com.twinsoft.convertigo.engine.util.LogWrapper;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.tas.Authentication;
import com.twinsoft.twinj.ActionTbl;
import com.twinsoft.twinj.Javelin;
import com.twinsoft.twinj.twinxEvent0;
import com.twinsoft.twinj.twinxEvent1;
import com.twinsoft.twinj.twinxEvent2;
import com.twinsoft.twinj.twinxEvent3;
import com.twinsoft.twinj.twinxListener;
import com.twinsoft.twinj.zoneListener;

public class JavelinConnectorComposite extends AbstractConnectorComposite implements zoneListener,
		twinxListener, KeyListener, IConnectable, IRecordable, IRefreshable, IScreenClassAware, IBlockizable,
		ILinkable, IAddable, IResetable {

	public JavelinConnectorComposite(ConnectorEditorPart connectorEditorPart, Connector connector,
			Composite parent, int style) {
		super(connectorEditorPart, connector, parent, style);
	}

	Frame frame;

	protected void initialize() {
		frame = SWT_AWT.new_Frame(this);
		frame.setLayout(new BorderLayout());
		frame.setBackground(new Color(100, 200, 255));
		renew();
	}

	private boolean isPlaying = false;
	private boolean isClosing = false;

	public Javelin getJavelin() {
		JavelinConnector javelinConnector = (JavelinConnector) connector;
		Javelin javelin = javelinConnector.javelin;
		return javelin;
	}

	protected void initJavelin() {
		initJavelin(false);
	}

	protected void initJavelin(boolean bRecord) {
		String traceFilePath = null;
		if (bRecord) {
			Timestamp ts = new Timestamp(System.currentTimeMillis());
			traceFilePath = Engine.PROJECTS_PATH + "/" + connector.getProject().getName() + "/Traces/"
					+ connector.getName() + "/";
			traceFilePath += ts.toString().substring(0, 10) + "_trace.etr";
			File file = new File(traceFilePath);
			if (!file.exists()) {
				try {
					file.createNewFile();

					ProjectExplorerView explorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
					if (explorerView != null) {
						ConnectorTreeObject connectorTreeObject = (ConnectorTreeObject) explorerView
								.findTreeObjectByUserObject(connector);
						for (TreeObject treeObject : connectorTreeObject.getChildren()) {
							if (treeObject instanceof FolderTreeObject) {
								if (treeObject.getName().equals("Traces")) {
									TraceTreeObject traceTreeObject = new TraceTreeObject(explorerView.viewer,
											file);
									((FolderTreeObject) treeObject).addChild(traceTreeObject);
									explorerView.refreshTreeObject(treeObject);
									explorerView.setSelectedTreeObject(traceTreeObject);
								}
							}
						}
					}
				} catch (IOException e) {
					ConvertigoPlugin.errorMessageBox("Unable to create the trace file");
				}
			}
		}

		// The following code allows to push relevant MDC values into Javelin's
		// threads
		JavelinObjectCreatorThread javelinObjectCreatorThread = new JavelinObjectCreatorThread(this,
				traceFilePath);
		javelinObjectCreatorThread.start();

		while (javelinObjectCreatorThread.isAlive()) {
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
			}
		}
	}

	String sessionId;

	class JavelinObjectCreatorThread extends Thread {

		public boolean finished = false;
		String traceFilePath;

		final JavelinConnectorComposite javelinConnectorComposite;

		public JavelinObjectCreatorThread(JavelinConnectorComposite javelinConnectorComposite,
				String traceFilePath) {
			this.traceFilePath = traceFilePath;
			this.javelinConnectorComposite = javelinConnectorComposite;
		}

		public void run() {
			JavelinConnector javelinConnector = (JavelinConnector) connector;
			Javelin javelin = javelinConnector.javelin;

			if (javelin == null) {
				try {
					String serviceCode = null;
					if (isPlaying) {
						String tracePlayerPort = ConvertigoPlugin
								.getProperty(ConvertigoPlugin.PREFERENCE_TRACEPLAYER_PORT);
						serviceCode = ",DIR|localhost:" + tracePlayerPort;
					} else
						serviceCode = javelinConnector.getServiceCode();

					long emulatorID = javelinConnector.emulatorID;

					Authentication auth = Engine.theApp.getAuthenticationObject(
							javelinConnector.getVirtualServer(), null);

					if (auth.getCurrentUser() == null) {
						String cariocaUserName = EnginePropertiesManager
								.getProperty(PropertyName.CARIOCA_DEFAULT_USER_NAME);
						String cariocaPassword = EnginePropertiesManager
								.getProperty(PropertyName.CARIOCA_DEFAULT_USER_PASSWORD);
						auth.login(cariocaUserName, cariocaPassword);
					}
					
					String projectName = javelinConnector.getProject().getName();
					String connectorName = javelinConnector.getName();
					String contextType = ContextManager.CONTEXT_TYPE_TRANSACTION;
					sessionId = Engine.theApp.contextManager.computeStudioContextName(contextType, projectName, connectorName);
					
					LogParameters logParameters = new LogParameters();
					Log4jHelper.mdcSet(logParameters);
					Log4jHelper.mdcPut(Log4jHelper.mdcKeys.Project, projectName);
					Log4jHelper.mdcPut(Log4jHelper.mdcKeys.Connector, connectorName);
					Log4jHelper.mdcPut(Log4jHelper.mdcKeys.ContextID, sessionId);

					com.twinsoft.api.Session session = Engine.theApp.sessionManager.addSession(
							(int) emulatorID, auth, serviceCode, sessionId,
							javelinConnector.getJavelinLanguage(), javelinConnector.isSslEnabled(),
							javelinConnector.isSslTrustAllServerCertificates(),
							javelinConnector.getIbmTerminalType(), traceFilePath);

					if (session != null) {
						clear();
						javelin = session.getJavelinObject();

						// Handling the TAB key for the emulator
						Set<AWTKeyStroke> set = new HashSet<AWTKeyStroke>();
						set.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB, KeyEvent.CTRL_MASK, false));
						javelin.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, set);

						// Handling the BACKTAB key for the emulator
						set = new HashSet<AWTKeyStroke>();
						set.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB, KeyEvent.CTRL_MASK
								| KeyEvent.SHIFT_MASK, false));
						javelin.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, set);

						frame.add(javelin, BorderLayout.CENTER);
						frame.doLayout();
						javelin.invalidate();
						javelin.validate();
						javelin.addtwinxListener(javelinConnectorComposite);
						javelin.addZoneListener(javelinConnectorComposite);
						javelin.setLog(new LogWrapper(Engine.logEmulators));

						javelin.setDataStableOnCursorOn(false);
					} else {
						String message = "Unable to add a new session: the service '" + serviceCode
								+ "' is probably not authorized.";
						javelin = null;
						throw new EngineException(message);
					}

					javelinConnector.javelin = javelin;
				} catch (Exception e) {
					ConvertigoPlugin.logException(e, "Unable to create the connector visual object!");
					javelin = null;
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.twinsoft.convertigo.eclipse.editors.connector.AbstractConnectorComposite
	 * #close()
	 */
	public void close() {
		isClosing = true;
		clear();

		TracePlayerThread tracePlayerThread = null;
		if (projectExplorerView != null) {
			tracePlayerThread = projectExplorerView.tracePlayerThread;
		}

		if ((tracePlayerThread != null) && (tracePlayerThread.connectorName != null)
				&& (tracePlayerThread.connectorName.equals(connector.getName()))) {
			tracePlayerThread.stopPlayer();
		}

		super.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	public void dispose() {
		super.dispose();
	}

	public void clear() {
		JavelinConnector javelinConnector = (JavelinConnector) connector;
		Javelin javelin = javelinConnector.javelin;

		if (javelin != null) {
			javelinConnector.javelin.disconnect();
			frame.remove(javelin);
			javelin.removetwinxListener(this);
			javelin.removeZoneListener(this);

			try {
				Engine.theApp.sessionManager.removeSession(connector.context.contextID);
			} catch (NullPointerException e) {
				// Silently ignore: if the Studio is being closed, the Engine
				// could
				// be yet closed when this method is called, that's why the
				// Engine
				// instance could be null.
			}

			javelinConnector.javelin = null;

			// if (!isClosing)
			// redraw();

			timeoutForConnect = 10000;
			timeoutForDataStable = 7000;
			dataStableThreshold = 3000;
		}
		if (!isClosing) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					redraw();
					toolBarSetEnable("NewScreenClass", false);
					toolBarSetEnable("NewTagName", false);
					// toolBarSetEnable("Link", false);
					toolBarSetEnable("Add", false);
				}
			});
		}
		latestBlockFactory = null;
		isPlaying = false;
	}

	int timeoutForConnect = 10000;
	int timeoutForDataStable = 7000;
	int dataStableThreshold = 3000;

	public void initConnector(Transaction transaction) {
		JavelinTransaction javelinTransaction = (JavelinTransaction) transaction;
		timeoutForConnect = javelinTransaction.getTimeoutForConnect();
		timeoutForDataStable = javelinTransaction.getTimeoutForDataStable();
		dataStableThreshold = javelinTransaction.getDataStableThreshold();
	}

	public void connect() {
		// The following code allows to push relevant MDC values into Javelin's
		// threads
		JavelinObjectConnectThread javelinObjectConnectThread = new JavelinObjectConnectThread();
		javelinObjectConnectThread.start();
	}

	class JavelinObjectConnectThread extends Thread {

		public JavelinObjectConnectThread() {
		}

		public void run() {
			final Cursor[] waitCursor = { null };
			final Shell[] shell = { null };
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					waitCursor[0] = new Cursor(Display.getDefault(), SWT.CURSOR_WAIT);

					shell[0] = getShell();
					shell[0].setCursor(waitCursor[0]);
				}
			});

			try {
				JavelinConnector javelinConnector = (JavelinConnector) connector;

				LogParameters logParameters = new LogParameters();
				Log4jHelper.mdcSet(logParameters);
				Log4jHelper.mdcPut(Log4jHelper.mdcKeys.Project, javelinConnector.getProject().getName());
				Log4jHelper.mdcPut(Log4jHelper.mdcKeys.Connector, javelinConnector.getName());
				Log4jHelper.mdcPut(Log4jHelper.mdcKeys.ContextID, sessionId);

				TracePlayerThread tracePlayerThread = null;
				if (projectExplorerView != null)
					tracePlayerThread = projectExplorerView.tracePlayerThread;

				if ((tracePlayerThread != null) && (tracePlayerThread.connectorName != null)
						&& (tracePlayerThread.connectorName.equals(connector.getName()))) {
					String tracePlayerPort = ConvertigoPlugin
							.getProperty(ConvertigoPlugin.PREFERENCE_TRACEPLAYER_PORT);
					javelinConnector.javelin.Connect("DIR#localhost:" + tracePlayerPort);
				} else
					javelinConnector.javelin.connect(timeoutForConnect);

				if (!javelinConnector.javelin.isConnected()) {
					throw new EngineException(
							"Unable to connect the connector! See the connector console for more details...");
				}
				javelinConnector.javelin.requestFocus();
			} catch (Throwable e) {
				ConvertigoPlugin.logException(e,
						"An unexpected error has occured during the connection of the emulator.");
			} finally {
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						shell[0].setCursor(null);
						waitCursor[0].dispose();
					}
				});
			}
		}
	}

	public void disconnect() {
		// The following code allows to push relevant MDC values
		JavelinObjectDisconnectThread javelinObjectDisconnectThread = new JavelinObjectDisconnectThread();
		javelinObjectDisconnectThread.start();
	}

	class JavelinObjectDisconnectThread extends Thread {

		public JavelinObjectDisconnectThread() {
		}

		public void run() {
			final Cursor[] waitCursor = { null };
			final Shell[] shell = { null };
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					waitCursor[0] = new Cursor(Display.getDefault(), SWT.CURSOR_WAIT);

					shell[0] = getShell();
					shell[0].setCursor(waitCursor[0]);
				}
			});

			try {
				JavelinConnector javelinConnector = (JavelinConnector) connector;
				javelinConnector.javelin.disconnect();
				if (javelinConnector.javelin.isConnected()) {
					throw new EngineException(
							"Unable to disconnect the connector! See the connector console for more details...");
				}

				TracePlayerThread tracePlayerThread = null;
				if (projectExplorerView != null) {
					tracePlayerThread = projectExplorerView.tracePlayerThread;
				}

				if ((tracePlayerThread != null) && (tracePlayerThread.connectorName != null)
						&& (tracePlayerThread.connectorName.equals(connector.getName()))) {
					// tracePlayerThread.stopPlayer();
				}
			} catch (Throwable e) {
				ConvertigoPlugin.logException(e,
						"An unexpected error has occured during the disconnection of the emulator.");
			} finally {
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						shell[0].setCursor(null);
						waitCursor[0].dispose();
					}
				});
			}
		}
	}

	public void refresh() {
		JavelinConnector javelinConnector = (JavelinConnector) connector;
		if (javelinConnector.javelin != null) {
			javelinConnector.javelin.invalidate();
			javelinConnector.javelin.repaint();
			javelinConnector.javelin.validate();
			javelinConnector.javelin.requestFocus();
		}
	}

	public void renew(boolean play) {
		clear();
		isPlaying = true;
		initJavelin();
	}

	public void renew() {
		clear();
		initJavelin();
	}

	public void record(boolean start) {
		clear();
		initJavelin(start);
	}

	/**
	 * Sets the value of the selected property with the selected zone from the
	 * connector
	 */
	public void link() {
		try {
			if (connector != null) {
				// gets the projectExplorerView
				ProjectExplorerView pev = ConvertigoPlugin.getDefault().getProjectExplorerView();
				if (pev != null) {
					// gets the databaseObject of the selected element in
					// project tree
					DatabaseObject databaseObject = (DatabaseObject) pev.getFirstSelectedDatabaseObject();

					// gets the BeanInfo
					BeanInfo bi = pev.getFirstSelectedDatabaseObjectBeanInfo();

					// gets the propertyDescriptor of the selected property in
					// the Properties panel
					PropertyDescriptor propertyDescriptor = ConvertigoPlugin.getDefault()
							.getSelectedPropertyDescriptor(bi);

					if (propertyDescriptor != null) {
						Class<?> editorClass = propertyDescriptor.getPropertyEditorClass();
						// test if the selected property is linkable with a
						// screen selection zone
						if (editorClass != null) {

							// gets the value Object to put in the property
							Object val = null;
							Method propertySelectionModifierGetValueMethod = editorClass.getMethod(
									"getSelectionZoneValue", new Class[] { DatabaseObject.class,
											Connector.class, Method.class });
							if (propertySelectionModifierGetValueMethod != null) {
								val = propertySelectionModifierGetValueMethod.invoke(null, new Object[] {
										databaseObject, connector, propertyDescriptor.getWriteMethod() });
							} else {
								// the selected property is not linkable with a
								// screen selection zone
							}

							if (val == null) {
								// no selection made in the connector
								// display the property's value
								Method propertySelectionModifierDisplayValueMethod = editorClass.getMethod(
										"displayPropertyValueFromSelectionZone", new Class[] {
												DatabaseObject.class, Connector.class, Method.class });
								if (propertySelectionModifierDisplayValueMethod != null) {
									propertySelectionModifierDisplayValueMethod.invoke(null, new Object[] {
											databaseObject, connector, propertyDescriptor.getReadMethod() });
								} else {
									// the selected property is not linkable
									// with a screen selection zone
								}
							} else {
								// selection is made in the connector
								// sets the value in the property
								((DatabaseObjectTreeObject) pev.getFirstSelectedTreeObject())
										.setPropertyValue(propertyDescriptor.getName(), val);
							}
						}
					} else {
						// no PropertyDescriptor corresponding to the selected
						// property
					}
				} else {
					// pev null
				}
			} else {
				// connector null
			}
		} catch (NoSuchMethodException e) {
			// the selected property is not linkable with a screen selection
			// zone
			ConvertigoPlugin.logInfo("The selected Property is not linkable with a screen selection zone.");
		} catch (Throwable e) {
			String message = "Error : " + e.getMessage();
			ConvertigoPlugin.logException(e, message);
		}
	}

	/**
	 * Adds an element to the selected property with the selected zone from the
	 * connector
	 */
	public void add() {
		try {
			if (connector != null) {
				// gets the projectExplorerView
				ProjectExplorerView pev = ConvertigoPlugin.getDefault().getProjectExplorerView();
				if (pev != null) {
					// gets the databaseObject of the selected element in
					// project tree
					DatabaseObject databaseObject = (DatabaseObject) pev.getFirstSelectedDatabaseObject();

					// gets the BeanInfo
					BeanInfo bi = pev.getFirstSelectedDatabaseObjectBeanInfo();

					// gets the propertyDescriptor of the selected property in
					// the Properties panel
					PropertyDescriptor propertyDescriptor = ConvertigoPlugin.getDefault()
							.getSelectedPropertyDescriptor(bi);

					if (propertyDescriptor != null) {
						// test if the selected property is addable with a
						// screen selection zone (we can add an element to the
						// property from the selected zone)
						Class<?> editorClass = propertyDescriptor.getPropertyEditorClass();
						if (editorClass != null) {

							Method propertySelectionAddMethod = editorClass.getMethod(
									"addPropertyElementFromSelectionZone", new Class[] { DatabaseObject.class,
											Connector.class, Method.class });
							if (propertySelectionAddMethod != null) {
								propertySelectionAddMethod.invoke(null, new Object[] { databaseObject,
										connector, propertyDescriptor.getWriteMethod() });
							} else {
								// the selected property is not addable with a
								// screen selection zone
							}

							databaseObject.hasChanged = true;
							pev.updateDatabaseObject(databaseObject);

							StructuredSelection structuredSelection = new StructuredSelection(
									((DatabaseObjectTreeObject) pev.getFirstSelectedTreeObject()));
							ConvertigoPlugin.getDefault().getPropertiesView()
									.selectionChanged((IWorkbenchPart) pev, structuredSelection);

						}
					} else {
						// no PropertyDescriptor corresponding to the selected
						// property
					}
				} else {
					// pev null
				}
			} else {
				// connector null
			}
		} catch (NoSuchMethodException e) {
			// the selected property is not addable with a screen selection zone
			ConvertigoPlugin
					.logInfo("Not possible to add an element from a selection zone to the selected Property.");
		} catch (Throwable e) {
			String message = "Error : " + e.getMessage();
			ConvertigoPlugin.logException(e, message);
		}
	}

	public void handleCommDropped(twinxEvent0 arg0) {
		toolBarSetEnable("Connect", true);
		toolBarSetEnable("Disconnect", false);

		// Learning mode
		if (connector.isLearning()) {
			synchronized (this) {
				writeLine("javelin.disconnect();");
			}
		}

	}

	public void handleConnectToUrl(twinxEvent2 arg0) {
		// TODO Auto-generated method stub

	}

	public void handleConnected(twinxEvent0 arg0) {
		toolBarSetEnable("Connect", false);
		toolBarSetEnable("Disconnect", true);

		JavelinConnector javelinConnector = (JavelinConnector) connector;
		Javelin javelin = javelinConnector.javelin;
		javelin.requestFocus();

		// Learning mode
		if (javelinConnector.isLearning()) {
			JavelinTransaction learnTransaction = (JavelinTransaction) javelinConnector
					.getLearningTransaction();
			synchronized (this) {
				writeLine("javelin.connect(" + learnTransaction.getTimeoutForConnect() + ");");
			}
		}

	}

	public void handleDataStable(twinxEvent3 arg0) {
		// TODO Auto-generated method stub
	}

	public void handleDblClick(twinxEvent0 arg0) {
		// TODO Auto-generated method stub

	}

	public void handleFirstData(twinxEvent0 arg0) {
		// TODO Auto-generated method stub

	}

	public void handleStartLearn(twinxEvent0 arg0) {
		// TODO Auto-generated method stub

	}

	public void handleStopLearn(twinxEvent0 arg0) {
		// TODO Auto-generated method stub

	}

	public void handleWaitAtDone(twinxEvent1 arg0) {
		// TODO Auto-generated method stub

	}

	private static final ActionTbl[][] KEYS_TABLE = { null, // emulatorID = 0
			new ActionTbl[] { // emulatorID = 1, VDX
					new ActionTbl("KSuite", KeyEvent.VK_DOWN, false, false, false, 0, false, false, false),
					new ActionTbl("KRetour", KeyEvent.VK_UP, false, false, false, 0, false, false, false),
					new ActionTbl("KEnvoi", KeyEvent.VK_ENTER, false, false, false, 0, false, false, false),
					new ActionTbl("KSommaire", KeyEvent.VK_HOME, false, false, false, 0, false, false, false),
					new ActionTbl("KGuide", KeyEvent.VK_END, false, false, false, 0, false, false, false),
					new ActionTbl("KAnnulation", KeyEvent.VK_RIGHT, false, false, false, 0, false, false,
							false),
					new ActionTbl("KRepetition", KeyEvent.VK_TAB, false, false, false, 0, false, false, false),
					new ActionTbl("KCorrection", KeyEvent.VK_BACK_SPACE, false, false, false, 0, false, false,
							false),
					new ActionTbl("KCnxFin", KeyEvent.VK_F9, false, false, false, 0, false, false, false),
					null }, new ActionTbl[] { // emulatorID = 2, VT
			new ActionTbl("F01", KeyEvent.VK_F1, false, false, false, 0, false, false, false),
					new ActionTbl("F02", KeyEvent.VK_F2, false, false, false, 0, false, false, false),
					new ActionTbl("F03", KeyEvent.VK_F3, false, false, false, 0, false, false, false),
					new ActionTbl("F04", KeyEvent.VK_F4, false, false, false, 0, false, false, false),
					new ActionTbl("F05", KeyEvent.VK_F5, false, false, false, 0, false, false, false),
					new ActionTbl("F06", KeyEvent.VK_F6, false, false, false, 0, false, false, false),
					new ActionTbl("F07", KeyEvent.VK_F7, false, false, false, 0, false, false, false),
					new ActionTbl("F08", KeyEvent.VK_F8, false, false, false, 0, false, false, false),
					new ActionTbl("F09", KeyEvent.VK_F9, false, false, false, 0, false, false, false),
					new ActionTbl("F10", KeyEvent.VK_F10, false, false, false, 0, false, false, false),
					new ActionTbl("F11", KeyEvent.VK_F11, false, false, false, 0, false, false, false),
					new ActionTbl("F12", KeyEvent.VK_F12, false, false, false, 0, false, false, false),
					new ActionTbl("UP", KeyEvent.VK_UP, false, false, false, 0, false, false, false),
					new ActionTbl("DOWN", KeyEvent.VK_DOWN, false, false, false, 0, false, false, false),
					new ActionTbl("LEFT", KeyEvent.VK_LEFT, false, false, false, 0, false, false, false),
					new ActionTbl("RIGHT", KeyEvent.VK_RIGHT, false, false, false, 0, false, false, false),
					new ActionTbl("PGUP", KeyEvent.VK_PAGE_UP, false, false, false, 0, false, false, false),
					new ActionTbl("PGDN", KeyEvent.VK_PAGE_DOWN, false, false, false, 0, false, false, false),
					new ActionTbl("HOME", KeyEvent.VK_HOME, false, false, false, 0, false, false, false),
					new ActionTbl("INS", KeyEvent.VK_INSERT, false, false, false, 0, false, false, false),
					new ActionTbl("DEL", KeyEvent.VK_DELETE, false, false, false, 0, false, false, false),
					new ActionTbl("END", KeyEvent.VK_END, false, false, false, 0, false, false, false),
					new ActionTbl("TAB", KeyEvent.VK_TAB, false, false, false, 0, false, false, false),
					new ActionTbl("BS", KeyEvent.VK_BACK_SPACE, false, false, false, 0, false, false, false),
					new ActionTbl("ENTER", KeyEvent.VK_ENTER, false, false, false, 0, false, false, false),
					null }, new ActionTbl[] { // emulatorID = 3, Bull
					// Function Keys
					new ActionTbl("FKC01", KeyEvent.VK_F1, false, false, false, 0, false, false, false),
					new ActionTbl("FKC02", KeyEvent.VK_F2, false, false, false, 0, false, false, false),
					new ActionTbl("FKC03", KeyEvent.VK_F3, false, false, false, 0, false, false, false),
					new ActionTbl("FKC04", KeyEvent.VK_F4, false, false, false, 0, false, false, false),
					new ActionTbl("FKC05", KeyEvent.VK_F5, false, false, false, 0, false, false, false),
					new ActionTbl("FKC06", KeyEvent.VK_F6, false, false, false, 0, false, false, false),
					new ActionTbl("FKC07", KeyEvent.VK_F7, false, false, false, 0, false, false, false),
					new ActionTbl("FKC08", KeyEvent.VK_F8, false, false, false, 0, false, false, false),
					new ActionTbl("FKC09", KeyEvent.VK_F9, false, false, false, 0, false, false, false),
					new ActionTbl("FKC10", KeyEvent.VK_F10, false, false, false, 0, false, false, false),
					new ActionTbl("FKC11", KeyEvent.VK_F11, false, false, false, 0, false, false, false),
					new ActionTbl("FKC12", KeyEvent.VK_F12, false, false, false, 0, false, false, false),

					// Arrow Keys
					new ActionTbl("UP", KeyEvent.VK_UP, false, false, false, 0, false, false, false),
					new ActionTbl("DOWN", KeyEvent.VK_DOWN, false, false, false, 0, false, false, false),
					new ActionTbl("LEFT", KeyEvent.VK_LEFT, false, false, false, 0, false, false, false),
					new ActionTbl("RIGHT", KeyEvent.VK_RIGHT, false, false, false, 0, false, false, false),

					// Clear Active Partition
					new ActionTbl("CLEARAP", KeyEvent.VK_PAGE_UP, false, false, false, 0, false, false, false),
					// Clear End of Partition
					new ActionTbl("CLEAREP", KeyEvent.VK_PAGE_DOWN, false, false, false, 0, false, false,
							false),
					// Init Active Partition
					new ActionTbl("INITAP", KeyEvent.VK_PAGE_UP, false, true, false, 0, false, false, false),
					// Init Both Partitions
					new ActionTbl("INITBP", KeyEvent.VK_END, false, true, false, 0, false, false, false),
					// Init Part of Partition
					new ActionTbl("INITPA", KeyEvent.VK_HOME, false, true, false, 0, false, false, false),
					// Clear Control Area
					new ActionTbl("CLEARCTRL", KeyEvent.VK_LEFT, true, false, false, 0, false, false, false),
					// InsertLine
					new ActionTbl("INSLINE", KeyEvent.VK_DOWN, false, true, false, 0, false, false, false),
					// Suppress Line
					new ActionTbl("SUPLINE", KeyEvent.VK_UP, false, true, false, 0, false, false, false),
					// Erase end of Line
					new ActionTbl("ERAEOL", KeyEvent.VK_RIGHT, false, true, false, 0, false, false, false),
					// Cursor to begin of line
					new ActionTbl("CURBOL", KeyEvent.VK_LEFT, false, true, false, 0, false, false, false),

					// Delete Char
					new ActionTbl("DELCHAR", KeyEvent.VK_DELETE, false, false, false, 0, false, false, false),
					// INsertChar
					new ActionTbl("INSCHAR", KeyEvent.VK_INSERT, false, false, false, 0, false, false, false),
					// Cursor to Home
					new ActionTbl("CURHOME", KeyEvent.VK_HOME, false, false, false, 0, false, false, false),
					// INsertChar
					new ActionTbl("INS", KeyEvent.VK_INSERT, false, false, false, 0, false, false, false),
					// TAB
					new ActionTbl("TAB", KeyEvent.VK_TAB, false, false, false, 0, false, false, false),
					// Back TAB
					new ActionTbl("BTAB", KeyEvent.VK_TAB, true, false, false, 0, false, false, false),
					// INsert TAB Pos
					new ActionTbl("INSTAB", KeyEvent.VK_F1, false, true, false, 0, false, false, false),
					// Clear TAB Pos
					new ActionTbl("CLRTAB", KeyEvent.VK_F2, false, true, false, 0, false, false, false),
					// Delete chars in field
					new ActionTbl("BS", KeyEvent.VK_BACK_SPACE, false, false, false, 0, false, false, false),
					// Transmit all Partiion
					new ActionTbl("XMITALL", KeyEvent.VK_ENTER, false, true, false, 0, false, false, false),
					// Transmit
					new ActionTbl("XMIT", KeyEvent.VK_ENTER, false, false, false, 0, false, false, false),
					// Break
					new ActionTbl("BREAK", KeyEvent.VK_PAUSE, false, false, false, 0, false, false, false),
					// PrintScreen
					new ActionTbl("PRTSCR", KeyEvent.VK_F10, false, true, false, 0, false, false, false),
					// Recall Last Command
					new ActionTbl("RECALL", KeyEvent.VK_F9, false, true, false, 0, false, false, false),
					// Send file command
					new ActionTbl("FILEXFER_SEND", KeyEvent.VK_F11, false, true, false, 0, false, false, false),
					// receive file command
					new ActionTbl("FILEXFER_RECEIVE", KeyEvent.VK_F12, false, true, false, 0, false, false,
							false), null }, null, // emulatorID = 4, HTML
			new ActionTbl[] { // emulatorID = 5, 3270
					new ActionTbl("KEY_PA1", KeyEvent.VK_INSERT, false, false, true, 0, false, false, false),
					new ActionTbl("KEY_PA2", KeyEvent.VK_HOME, false, false, true, 0, false, false, false),
					new ActionTbl("KEY_PA3", KeyEvent.VK_PAGE_UP, true, false, false, 0, false, false, false),
					new ActionTbl("KEY_SYSREQ", KeyEvent.VK_S, false, true, false, 0, false, false, false),
					new ActionTbl("KEY_ENTER", KeyEvent.VK_ENTER, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_ATTN", KeyEvent.VK_ESCAPE, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF1", KeyEvent.VK_F1, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF2", KeyEvent.VK_F2, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF3", KeyEvent.VK_F3, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF4", KeyEvent.VK_F4, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF5", KeyEvent.VK_F5, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF6", KeyEvent.VK_F6, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF7", KeyEvent.VK_F7, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF8", KeyEvent.VK_F8, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF9", KeyEvent.VK_F9, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF10", KeyEvent.VK_F10, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF11", KeyEvent.VK_F11, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF12", KeyEvent.VK_F12, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF13", KeyEvent.VK_F1, true, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF14", KeyEvent.VK_F2, true, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF15", KeyEvent.VK_F3, true, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF16", KeyEvent.VK_F4, true, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF17", KeyEvent.VK_F5, true, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF18", KeyEvent.VK_F6, true, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF19", KeyEvent.VK_F7, true, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF20", KeyEvent.VK_F8, true, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF21", KeyEvent.VK_F9, true, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF22", KeyEvent.VK_F10, true, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF23", KeyEvent.VK_F11, true, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF24", KeyEvent.VK_F12, true, false, false, 0, false, false, false),
					new ActionTbl("KEY_CURRIGHT", KeyEvent.VK_RIGHT, false, false, false, 0, false, false,
							false),
					new ActionTbl("KEY_CURLEFT", KeyEvent.VK_LEFT, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_CURUP", KeyEvent.VK_UP, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_CURDOWN", KeyEvent.VK_DOWN, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_BACKSP", KeyEvent.VK_BACK_SPACE, false, false, false, 0, false, false,
							false),
					new ActionTbl("KEY_TAB", KeyEvent.VK_TAB, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_BACKTAB", KeyEvent.VK_TAB, true, false, false, 0, false, false, false),
					new ActionTbl("KEY_NEWLINE", KeyEvent.VK_ENTER, true, false, false, 0, false, false, false),
					new ActionTbl("KEY_HOME", KeyEvent.VK_HOME, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_INSERT", KeyEvent.VK_INSERT, false, false, false, 0, false, false,
							false),
					new ActionTbl("KEY_DELCHAR", KeyEvent.VK_DELETE, false, false, false, 0, false, false,
							false),
					new ActionTbl("KEY_RESET", KeyEvent.VK_R, false, true, false, 0, false, false, false),
					new ActionTbl("KEY_DUP", KeyEvent.VK_INSERT, false, true, true, 0, false, false, false),
					new ActionTbl("KEY_FLDMRK", KeyEvent.VK_HOME, true, false, false, 0, false, false, false),
					new ActionTbl("KEY_ERASEEOF", KeyEvent.VK_DELETE, true, false, false, 0, false, false,
							false),
					new ActionTbl("KEY_ERASEINPUT", KeyEvent.VK_END, false, false, true, 0, false, false,
							false),
					new ActionTbl("KEY_CURSEL", KeyEvent.VK_F3, false, false, true, 0, false, false, false),
					new ActionTbl("KEY_CLEAR", KeyEvent.VK_PAUSE, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_ROLLUP", KeyEvent.VK_PAGE_UP, false, false, false, 0, false, false,
							false),
					new ActionTbl("KEY_ROLLDOWN", KeyEvent.VK_PAGE_DOWN, false, false, false, 0, false, false,
							false), null }, null, // emulatorID = 6, 3287
			new ActionTbl[] { // emulatorID = 7, 5250
					new ActionTbl("KEY_PA1", KeyEvent.VK_INSERT, false, false, true, 0, false, false, false),
					new ActionTbl("KEY_PA2", KeyEvent.VK_HOME, false, false, true, 0, false, false, false),
					new ActionTbl("KEY_PA3", KeyEvent.VK_PAGE_UP, true, false, false, 0, false, false, false),
					new ActionTbl("KEY_SYSREQ", KeyEvent.VK_S, false, true, false, 0, false, false, false),
					new ActionTbl("KEY_ENTER", KeyEvent.VK_ENTER, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_ATTN", KeyEvent.VK_ESCAPE, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF1", KeyEvent.VK_F1, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF2", KeyEvent.VK_F2, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF3", KeyEvent.VK_F3, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF4", KeyEvent.VK_F4, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF5", KeyEvent.VK_F5, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF6", KeyEvent.VK_F6, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF7", KeyEvent.VK_F7, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF8", KeyEvent.VK_F8, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF9", KeyEvent.VK_F9, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF10", KeyEvent.VK_F10, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF11", KeyEvent.VK_F11, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF12", KeyEvent.VK_F12, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF13", KeyEvent.VK_F1, true, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF14", KeyEvent.VK_F2, true, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF15", KeyEvent.VK_F3, true, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF16", KeyEvent.VK_F4, true, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF17", KeyEvent.VK_F5, true, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF18", KeyEvent.VK_F6, true, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF19", KeyEvent.VK_F7, true, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF20", KeyEvent.VK_F8, true, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF21", KeyEvent.VK_F9, true, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF22", KeyEvent.VK_F10, true, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF23", KeyEvent.VK_F11, true, false, false, 0, false, false, false),
					new ActionTbl("KEY_PF24", KeyEvent.VK_F12, true, false, false, 0, false, false, false),
					new ActionTbl("KEY_CURRIGHT", KeyEvent.VK_RIGHT, false, false, false, 0, false, false,
							false),
					new ActionTbl("KEY_CURLEFT", KeyEvent.VK_LEFT, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_CURUP", KeyEvent.VK_UP, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_CURDOWN", KeyEvent.VK_DOWN, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_BACKSP", KeyEvent.VK_BACK_SPACE, false, false, false, 0, false, false,
							false),
					new ActionTbl("KEY_TAB", KeyEvent.VK_TAB, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_BACKTAB", KeyEvent.VK_TAB, true, false, false, 0, false, false, false),
					new ActionTbl("KEY_NEWLINE", KeyEvent.VK_ENTER, true, false, false, 0, false, false, false),
					new ActionTbl("KEY_HOME", KeyEvent.VK_HOME, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_INSERT", KeyEvent.VK_INSERT, false, false, false, 0, false, false,
							false),
					new ActionTbl("KEY_DELCHAR", KeyEvent.VK_DELETE, false, false, false, 0, false, false,
							false),
					new ActionTbl("KEY_RESET", KeyEvent.VK_R, false, true, false, 0, false, false, false),
					new ActionTbl("KEY_DUP", KeyEvent.VK_INSERT, false, true, true, 0, false, false, false),
					new ActionTbl("KEY_FLDMRK", KeyEvent.VK_HOME, true, false, false, 0, false, false, false),
					new ActionTbl("KEY_ERASEEOF", KeyEvent.VK_DELETE, true, false, false, 0, false, false,
							false),
					new ActionTbl("KEY_ERASEINPUT", KeyEvent.VK_END, false, false, true, 0, false, false,
							false),
					new ActionTbl("KEY_CURSEL", KeyEvent.VK_F3, false, false, true, 0, false, false, false),
					new ActionTbl("KEY_CLEAR", KeyEvent.VK_PAUSE, false, false, false, 0, false, false, false),
					new ActionTbl("KEY_ROLLUP", KeyEvent.VK_PAGE_UP, false, false, false, 0, false, false,
							false),
					new ActionTbl("KEY_ROLLDOWN", KeyEvent.VK_PAGE_DOWN, false, false, false, 0, false, false,
							false), null }, null, // emulatorID = 8, twinpay
			null, // emulatorID = 9, mediatel
	};

	public String findActionName(long emulatorID, KeyEvent event) {
		int j = (int) emulatorID;
		ActionTbl[] actionTbl = KEYS_TABLE[j];
		for (int i = 0; actionTbl[i] != null; i++) {
			if ((event.getKeyCode() == actionTbl[i].m_StdKeyCode)
					&& (event.isControlDown() == actionTbl[i].m_StdKeyCtrl)
					&& (event.isShiftDown() == actionTbl[i].m_StdKeyShft)
					&& (event.isAltDown() == actionTbl[i].m_StdKeyAlt)) {
				return (actionTbl[i].m_ActionName);
			}
		}
		return null;
	}

	private long doActionTime = 0;
	private boolean bDoAction = false;
	private boolean bWaitForDataStable = false;
	private boolean bHandlerOpened = false;
	private String keysBuffer = "";
	private ScreenClass learnScreenClass = null;
	private String handlers = null;

	public void keyPressed(java.awt.event.KeyEvent keyEvent) {
		ScreenClass currentScreenClass = null;
		JavelinConnector javelinConnector = (JavelinConnector) connector;

		// Learning mode
		if (javelinConnector.isLearning()) {
			synchronized (javelinConnector) {
				currentScreenClass = javelinConnector.getCurrentScreenClass();
			}

			synchronized (this) {
				char c = keyEvent.getKeyChar();

				// Avoiding handle null key char
				if (c == 0x0)
					return;

				if (Character.isLetterOrDigit(c) || c == ' ') {
					keysBuffer += c;
				} else {
					String doActionName = findActionName(javelinConnector.emulatorID, keyEvent);
					if (doActionName != null) {
						if (learnScreenClass == null) {
							learnScreenClass = currentScreenClass;
						} else if (!learnScreenClass.equals(currentScreenClass)) {
							if (javelinConnector.isAccumulating()) {
								closeScreenClassHandler();
								openScreenClassHandler(false);
								closeScreenClassHandler("accumulate");
							} else {
								closeScreenClassHandler("redetect");
							}
							// learnScreenClass =
							// projectExplorerView.getLastDetectedScreenClass();
							learnScreenClass = currentScreenClass;
							openScreenClassHandler(true);
						}

						// Writing wait sync function if needed (only for
						// videotex)
						if (bDoAction && (javelinConnector.emulatorID == 1)) {
							long rightNow = new java.util.Date().getTime();
							writeLine("javelin.waitSync(" + (rightNow - doActionTime) + ");");
							bDoAction = false;
						}

						// Flushing alphanumeric buffer
						if (keysBuffer.length() != 0) {
							writeLine("javelin.send(\"" + keysBuffer + "\");");
							keysBuffer = "";
						}

						// Writing emulator action
						writeLine("javelin.doAction(\"" + doActionName + "\");");
						bDoAction = true;
						doActionTime = new java.util.Date().getTime();
					}
				}
			}
		}
	}

	public void keyReleased(java.awt.event.KeyEvent keyEvent) {
	}

	public void keyTyped(java.awt.event.KeyEvent keyEvent) {
	}

	private String handlerName = null;

	private void openScreenClassHandler(boolean bEntry) {
		if ((learnScreenClass == null) || bHandlerOpened)
			return;

		String learnScreenClassName = learnScreenClass.getName();

		if (bEntry) {
			String line = java.text.MessageFormat.format("// Entry handler for screen class \"{0}\"",
					new Object[] { learnScreenClassName });

			writeLine("\n" + line);

			writeLine("function on" + StringUtils.normalize(learnScreenClassName) + "Entry() {");

			handlerName = "on" + StringUtils.normalize(learnScreenClassName) + "Entry()";
		} else {
			String line = java.text.MessageFormat.format("// Exit handler for the screen class \"{0}\"",
					new Object[] { learnScreenClassName });
			writeLine("\n" + line);

			writeLine("function on" + StringUtils.normalize(learnScreenClassName) + "Exit() {");

			handlerName = "on" + StringUtils.normalize(learnScreenClassName) + "Exit()";
		}

		bWaitForDataStable = true;
		bDoAction = false;
		bHandlerOpened = true;
	}

	private void closeScreenClassHandler() {
		closeScreenClassHandler(null);
	}

	private void closeScreenClassHandler(String returnedVerb) {
		if ((learnScreenClass == null) || !bHandlerOpened)
			return;

		if (bWaitForDataStable) {
			writeLine("javelin.waitForDataStable(timeout, threshold);");
			writeLine("");
		}

		if (returnedVerb == null)
			writeLine("return;");
		else
			writeLine("return \"" + returnedVerb + "\";");
		writeLine("}");

		if (bHandlerOpened) {
			fireHandlerChanged(handlerName);
			handlers = "";
			handlerName = null;
		}

		bHandlerOpened = false;
	}

	public void createWaitAtFromSelectionZone() {
		JavelinConnector javelinConnector = (JavelinConnector) connector;
		if (javelinConnector.isLearning()) {
			long timeout = 1000;

			if (!bHandlerOpened)
				openScreenClassHandler(true);

			// If there was a pending doAction command, clear it
			if (bDoAction) {
				bDoAction = false;
				long rightNow = new java.util.Date().getTime();
				timeout = rightNow - doActionTime;
			}

			Javelin javelin = getJavelin();
			Rectangle zone = javelin.getSelectionZone();
			String strZone = javelin.getString(zone.x, zone.y, zone.width);
			writeLine("javelin.waitAt(\"" + strZone + "\", " + zone.x + ", " + zone.y + ", "
					+ ((int) (timeout * 1.3)) + ");");
			bWaitForDataStable = false;

			javelin.setSelectionZone(new Rectangle(0, 0, 0, 0));
			javelin.requestFocus();
		}
	}

	public BlockFactoryWithVector latestBlockFactory;

	public void blocksChanged(EngineEvent e, boolean bShow) {
		ConvertigoPlugin.logDebug("The blocks vector has changed: we update the Javelin hilighted zones.");
		latestBlockFactory = (BlockFactoryWithVector) e.getSource();
		if (bShow) {
			showBlocks(latestBlockFactory);
		}
	}

	public void showBlocks(boolean bLatestBlockFactory) {
		if (bLatestBlockFactory)
			showBlocks(latestBlockFactory);
		else
			showBlocks(null);
	}

	private void showBlocks(BlockFactoryWithVector bf) {
		JavelinConnector javelinConnector = (JavelinConnector) connector;
		Javelin javelin = javelinConnector.javelin;

		if (javelin == null)
			return;

		Vector<Rectangle> vz = null;
		Vector<Color> vc = null;
		int len;

		if (bf != null) {
			vz = new Vector<Rectangle>(bf.list.size());
			vc = new Vector<Color>(bf.list.size());
			Rectangle rect;
			// Block block;

			for (Block block : bf.list) {
				String size = block.getOptionalAttribute("size");
				try {
					len = Integer.parseInt(size);
				} catch (Exception e) {
					len = block.length;
				}
				rect = new Rectangle(block.column, block.line, len, 1);
				vz.add(rect);
				vc.add(Color.gray);
			}
		}

		javelin.showZones(vz, vc);
	}

	public void handleSelectionChanged(twinxEvent0 arg0) {
		final Javelin javelin = getJavelin();

		Rectangle selectedZone = javelin.getSelectionZone();
		final boolean bSelected = (selectedZone != null) && (selectedZone.getWidth() != 0)
				&& (selectedZone.getHeight() != 0);

		// enable or disable New beans buttons
		toolBarSetEnable("NewScreenClass", bSelected);
		toolBarSetEnable("NewTagName", bSelected);
		toolBarSetEnable("NewWaitAt", bSelected && connector.isLearning());

		// enable or disable the link button
		// getDisplay().syncExec(new Runnable() {
		// public void run() {
		// boolean enable = false;
		// if (projectExplorerView != null) {
		// BeanInfo bi =
		// projectExplorerView.getFirstSelectedDatabaseObjectBeanInfo();
		// if (bi != null) {
		// PropertyDescriptor pd =
		// ConvertigoPlugin.getDefault().getSelectedPropertyDescriptor(bi);
		// try {
		// enable = pd != null &&
		// bSelected &&
		// getLastDetectedScreenClass() != null &&
		// !javelin.getSelectionZone().isEmpty() &&
		// (
		// pd.getPropertyEditorClass().isAssignableFrom(
		// Class.forName("com.twinsoft.convertigo.eclipse.property_editors.ZoneEditor")
		// ) ||
		// pd.getPropertyEditorClass().isAssignableFrom(
		// Class.forName("com.twinsoft.convertigo.eclipse.property_editors.JavelinAttributeEditor")
		// ) ||
		// pd.getPropertyEditorClass().isAssignableFrom(
		// Class.forName("com.twinsoft.convertigo.eclipse.property_editors.JavelinStringEditor")
		// )
		// );
		// } catch (ClassNotFoundException e) {
		// enable = false;
		// }
		// }
		// }
		// toolBarSetEnable("Link", enable);
		// }
		// });

		// enable or disable the add button
		getDisplay().syncExec(new Runnable() {
			public void run() {
				boolean enable = false;
				if (projectExplorerView != null) {
					BeanInfo bi = ConvertigoPlugin.getDefault().getProjectExplorerView()
							.getFirstSelectedDatabaseObjectBeanInfo();
					if (bi != null) {
						PropertyDescriptor pd = ConvertigoPlugin.getDefault()
								.getSelectedPropertyDescriptor(bi);
						try {
							enable = pd != null
									&& bSelected
									&&
									// getLastDetectedScreenClass() != null &&
									// // seems to be broken and not very
									// usefull, commented for bug #952
									!javelin.getSelectionZone().isEmpty()
									&& (pd.getPropertyEditorClass().isAssignableFrom(Class
											.forName("com.twinsoft.convertigo.eclipse.property_editors.ColumnEditor")));
						} catch (ClassNotFoundException e) {
							enable = false;
						}
					}
				}
				toolBarSetEnable("Add", enable);
			}
		});
	}

	/**
	 * Highlight the current screen class in tree view
	 * 
	 */
	public void goToCurrentScreenClass() {
		synchronized (connector) {
			ScreenClass screenClass = ((JavelinConnector) connector).getCurrentScreenClass();
			fireObjectSelected(new CompositeEvent(screenClass));

			if (connector.isLearning()) {
				if (bHandlerOpened) {
					closeScreenClassHandler("redetect");
				}
				openScreenClassHandler(true);
				getJavelin().requestFocus();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.twinsoft.convertigo.eclipse.editors.connector.AbstractConnectorComposite
	 * #setAccumulate(boolean)
	 */
	public void setAccumulate(boolean accumulate) {
		if (accumulate) {
			// Is there still a screen class handler being generated?
			if (learnScreenClass != null) {
				if (!bHandlerOpened)
					openScreenClassHandler(true);

				// Writing wait sync function if needed
				if (bDoAction) {
					long rightNow = new java.util.Date().getTime();
					writeLine("javelin.waitSync(" + (rightNow - doActionTime) + ");");
					bDoAction = false;
				}

				closeScreenClassHandler("redetect");
			}

			openScreenClassHandler(false);
			closeScreenClassHandler("accumulate");

			learnScreenClass = null;
		}

		super.setAccumulate(accumulate);
		getJavelin().requestFocus();
	}

	private void writeLine(String line) {
		if (line != null) {
			int len = line.length();
			String tab = ((!bHandlerOpened || (bHandlerOpened && (len <= 1))) ? "" : "    ");
			handlers += tab + line + "\n";
			if (!bHandlerOpened) {
				fireHandlerChanged();
				handlers = "";
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.twinsoft.convertigo.eclipse.editors.connector.AbstractConnectorComposite
	 * #startLearn()
	 */
	public void startLearn() {
		super.startLearn();
		handlers = "";
		getJavelin().addKeyListener(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.twinsoft.convertigo.eclipse.editors.connector.AbstractConnectorComposite
	 * #stopLearn()
	 */
	public void stopLearn() {
		if (!handlers.equals("")) {
			// Is there still a screen class handler being generated?
			if (bHandlerOpened)
				closeScreenClassHandler();

			fireHandlerChanged();
		}

		handlers = "";
		getJavelin().removeKeyListener(this);
		super.stopLearn();
	}

	private void fireHandlerChanged() {
		fireHandlerChanged(null);
	}

	private void fireHandlerChanged(String newScreenClassHandler) {
		JavelinTransaction transaction = (JavelinTransaction) connector.getLearningTransaction();
		transaction.handlers += handlers;
		transaction.hasChanged = true;
		ConvertigoPlugin.logDebug2("(JavelinConnectorComposite) added new handler to transaction '"
				+ transaction.getName() + "'");
		fireObjectChanged(new CompositeEvent(transaction, newScreenClassHandler));
	}

	public void reset() {
		JavelinConnector javelinConnector = (JavelinConnector) connector;
		Javelin javelin = javelinConnector.javelin;

		int emulatorID = (int) javelinConnector.emulatorID;

		switch (emulatorID) {
		case Session.EmulIDSNA:
		case Session.EmulIDAS400:
			MessageBox messageBox = new MessageBox(getShell(), SWT.OK | SWT.CANCEL | SWT.ICON_QUESTION
					| SWT.APPLICATION_MODAL);
			String message = "This will send a KEY_RESET to the emulator.";
			messageBox.setMessage(message);
			int ret = messageBox.open();
			if (ret == SWT.OK) {
				javelin.doAction("KEY_RESET");
				Engine.logEmulators
						.info("KEY_RESET has been sent to the emulator, because of an user request.");
			}
			break;
		default:
			ConvertigoPlugin
					.warningMessageBox("The Reset function is only available for IBM emulators (3270 and AS/400).");
			break;
		}
	}

	protected void clearContent() {
		// Nothing to do in Javelin connector
	}
}
