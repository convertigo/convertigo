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

import java.beans.BeanInfo;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.swing.event.EventListenerList;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IProgressService;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Criteria;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ExtractionRule;
import com.twinsoft.convertigo.beans.core.IScreenClassContainer;
import com.twinsoft.convertigo.beans.core.ITablesProperty;
import com.twinsoft.convertigo.beans.core.MobileDevice;
import com.twinsoft.convertigo.beans.core.Pool;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Sheet;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.core.StatementWithExpressions;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.statements.FunctionStatement;
import com.twinsoft.convertigo.beans.statements.HandlerStatement;
import com.twinsoft.convertigo.beans.steps.FunctionStep;
import com.twinsoft.convertigo.beans.transactions.JavelinTransaction;
import com.twinsoft.convertigo.beans.variables.StepVariable;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.actions.ProjectExplorerSaveAllAction;
import com.twinsoft.convertigo.eclipse.dialogs.ButtonSpec;
import com.twinsoft.convertigo.eclipse.dialogs.CustomDialog;
import com.twinsoft.convertigo.eclipse.dnd.StepSourceTransfer;
import com.twinsoft.convertigo.eclipse.dnd.TreeDragListener;
import com.twinsoft.convertigo.eclipse.dnd.TreeDropAdapter;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.editors.CompositeListener;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditorInput;
import com.twinsoft.convertigo.eclipse.popup.actions.ClipboardCopyAction;
import com.twinsoft.convertigo.eclipse.popup.actions.ClipboardCutAction;
import com.twinsoft.convertigo.eclipse.popup.actions.ClipboardPasteAction;
import com.twinsoft.convertigo.eclipse.popup.actions.DatabaseObjectDecreasePriorityAction;
import com.twinsoft.convertigo.eclipse.popup.actions.DatabaseObjectDeleteAction;
import com.twinsoft.convertigo.eclipse.popup.actions.DatabaseObjectIncreasePriorityAction;
import com.twinsoft.convertigo.eclipse.popup.actions.DeletePropertyTableColumnAction;
import com.twinsoft.convertigo.eclipse.popup.actions.DeletePropertyTableRowAction;
import com.twinsoft.convertigo.eclipse.popup.actions.ProjectValidateXSDAction;
import com.twinsoft.convertigo.eclipse.popup.actions.RedoAction;
import com.twinsoft.convertigo.eclipse.popup.actions.SequenceExecuteSelectedAction;
import com.twinsoft.convertigo.eclipse.popup.actions.ShowStepInPickerAction;
import com.twinsoft.convertigo.eclipse.popup.actions.TestCaseExecuteSelectedAction;
import com.twinsoft.convertigo.eclipse.popup.actions.TracePlayAction;
import com.twinsoft.convertigo.eclipse.popup.actions.TransactionEditHandlersAction;
import com.twinsoft.convertigo.eclipse.popup.actions.TransactionExecuteDefaultAction;
import com.twinsoft.convertigo.eclipse.popup.actions.TransactionExecuteSelectedAction;
import com.twinsoft.convertigo.eclipse.popup.actions.UndoAction;
import com.twinsoft.convertigo.eclipse.trace.TracePlayerThread;
import com.twinsoft.convertigo.engine.DatabaseObjectImportedEvent;
import com.twinsoft.convertigo.engine.DatabaseObjectListener;
import com.twinsoft.convertigo.engine.DatabaseObjectLoadedEvent;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineEvent;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EngineListener;
import com.twinsoft.convertigo.engine.MigrationListener;
import com.twinsoft.convertigo.engine.MigrationManager;
import com.twinsoft.convertigo.engine.ObjectsProvider;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.ProjectUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class ProjectExplorerView extends ViewPart implements ObjectsProvider,
		CompositeListener, EngineListener, MigrationListener {

	public static final int TREE_OBJECT_TYPE_UNKNOWN = 0;

	public static final int TREE_OBJECT_TYPE_DBO = 0x100; // 0000 0001 0000 0000

	public static final int TREE_OBJECT_TYPE_DBO_PROJECT = 0x101; // 0000 0001
																	// 0000 0001
	public static final int TREE_OBJECT_TYPE_DBO_POOL = 0x102; // 0000 0001 0000
																// 0010
	public static final int TREE_OBJECT_TYPE_DBO_TRANSACTION = 0x103; // 0000
																		// 0001
																		// 0000
																		// 0011
	public static final int TREE_OBJECT_TYPE_DBO_SHEET = 0x104; // 0000 0001
																// 0000 0100
	public static final int TREE_OBJECT_TYPE_DBO_ROOT_SCREEN_CLASS = 0x105; // 0000
																			// 0001
																			// 0000
																			// 0101
	public static final int TREE_OBJECT_TYPE_DBO_SCREEN_CLASS = 0x106; // 0000
																		// 0001
																		// 0000
																		// 0110
	public static final int TREE_OBJECT_TYPE_DBO_BLOCK_FACTORY = 0x107; // 0000
																		// 0001
																		// 0000
																		// 0111
	public static final int TREE_OBJECT_TYPE_DBO_CRITERIA = 0x108; // 0000 0001
																	// 0000 1000
	public static final int TREE_OBJECT_TYPE_DBO_EXTRACTION_RULE = 0x109; // 0000
																			// 0001
																			// 0000
																			// 1001
	public static final int TREE_OBJECT_TYPE_DBO_CONNECTOR = 0x10A; // 0000 0001
																	// 0000 1010
	public static final int TREE_OBJECT_TYPE_DBO_STATEMENT = 0x10B;
	public static final int TREE_OBJECT_TYPE_DBO_STATEMENT_WITH_EXPRESSIONS = 0x10C;
	public static final int TREE_OBJECT_TYPE_DBO_STEP = 0x10D;
	public static final int TREE_OBJECT_TYPE_DBO_STEP_WITH_EXPRESSIONS = 0x10E;
	public static final int TREE_OBJECT_TYPE_DBO_SEQUENCE = 0x10F;
	public static final int TREE_OBJECT_TYPE_DBO_TESTCASE = 0x110;
	public static final int TREE_OBJECT_TYPE_DBO_MOBILEDEVICE = 0x111;

	public static final int TREE_OBJECT_TYPE_DBO_PROPERTY_TABLE = 0x300;
	public static final int TREE_OBJECT_TYPE_DBO_PROPERTY_TABLE_ROW = 0x301;
	public static final int TREE_OBJECT_TYPE_DBO_PROPERTY_TABLE_COLUMN = 0x302;

	public static final int TREE_OBJECT_TYPE_DBO_INHERITED = 0x400; // 0000 0100
																	// 0000 0000

	public static final int TREE_OBJECT_TYPE_FOLDER = 0x200; // 0000 0010 0000
																// 0000

	public static final int TREE_OBJECT_TYPE_FOLDER_POOLS = 0x201; // 0000 0010
																	// 0000 0001
	public static final int TREE_OBJECT_TYPE_FOLDER_TRANSACTIONS = 0x202; // 0000
																			// 0010
																			// 0000
																			// 0010
	public static final int TREE_OBJECT_TYPE_FOLDER_SHEETS = 0x203; // 0000 0010
																	// 0000 0011
	public static final int TREE_OBJECT_TYPE_FOLDER_SCREEN_CLASSES = 0x204; // 0000
																			// 0010
																			// 0000
																			// 0100
	public static final int TREE_OBJECT_TYPE_FOLDER_CRITERIAS = 0x205; // 0000
																		// 0010
																		// 0000
																		// 0101
	public static final int TREE_OBJECT_TYPE_FOLDER_EXTRACTION_RULES = 0x206; // 0000
																				// 0010
																				// 0000
																				// 0110
	public static final int TREE_OBJECT_TYPE_FOLDER_CONNECTORS = 0x207;
	public static final int TREE_OBJECT_TYPE_FOLDER_SEQUENCES = 0x208;
	public static final int TREE_OBJECT_TYPE_FOLDER_STEPS = 0x209;
	public static final int TREE_OBJECT_TYPE_FOLDER_VARIABLES = 0x20A;
	public static final int TREE_OBJECT_TYPE_FOLDER_TESTCASES = 0x20B;
	public static final int TREE_OBJECT_TYPE_FOLDER_MOBILEDEVICES = 0x20C;

	public static final int TREE_OBJECT_TYPE_MISC = 0x8000; // 1000 0000 0000
															// 0000

	public static final int TREE_OBJECT_TYPE_HANDLERS_DECLARATION = 0x8001; // 1000
																			// 0000
																			// 0000
																			// 0001
	public static final int TREE_OBJECT_TYPE_VARIABLE = 0x8002; // 1000 0000
																// 0000 0010
	public static final int TREE_OBJECT_TYPE_FUNCTION = 0x8003;

	public TreeViewer viewer;

	// private DrillDownAdapter drillDownAdapter;

	private UndoManager undoManager;

	private Action doubleClickAction;
	private Action redoAction;
	private Action undoAction;
	private Action copyAction;
	private Action cutAction;
	private Action pasteAction;
	private Action deleteDatabaseObjectAction;
	private Action deletePropertyTableRowAction;
	private Action deletePropertyTableColumnAction;
	private Action decreasePriorityAction;
	private Action increasePriorityAction;
	private Action executeTransaction;
	private Action executeDefaultTransaction;
	private Action transactionEditHandlersAction;
	private Action tracePlayAction;
	private Action executeSequence;
	private Action executeTestCase;
	private Action projectValidateXSDAction;
	private Action showStepInPickerAction;
	// private Action projectCleanXSDAction;

	public Action projectExplorerSaveAllAction;

	private ViewContentProvider viewContentProvider = null;

	private Map<DatabaseObject, DatabaseObjectTreeObject> databaseObjectTreeObjectCache = new WeakHashMap<DatabaseObject, DatabaseObjectTreeObject>();

	/**
	 * The constructor.
	 */
	public ProjectExplorerView() {
		// Initialize the undo.redo system
		undoManager = new UndoManager();
		undoManager.setLimit(100);

		// Set view reference to this new instance (if view is closed and
		// reopened)
		ConvertigoPlugin.projectManager.setProjectExplorerView(this);
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(Composite parent) {
		viewContentProvider = new ViewContentProvider(this);

		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(viewContentProvider);

		// DND support
		int ops = DND.DROP_COPY | DND.DROP_MOVE;
		Transfer[] dragtfs = new Transfer[] { TextTransfer.getInstance() };
		Transfer[] droptfs = new Transfer[] { TextTransfer.getInstance(),
				StepSourceTransfer.getInstance() };
		viewer.addDragSupport(ops, dragtfs, new TreeDragListener(viewer));
		viewer.addDropSupport(ops, droptfs, new TreeDropAdapter(viewer));

		ILabelProvider lp = new ViewLabelProvider();
		ILabelDecorator ld = new ViewLabelDecorator();
		// viewer.setLabelProvider(lp);
		viewer.setLabelProvider(new DecoratingLabelProvider(lp, ld));

		viewer.setSorter(new TreeObjectSorter());
		viewer.setInput(getViewSite());

		// drillDownAdapter = new DrillDownAdapter(viewer);

		makeActions();
		hookContextMenu();
		hookSelectionChangedEvent();
		hookDoubleClickAction();
		hookGlobalActions();
		hookKeyboardActions();
		contributeToActionBars();

		getSite().setSelectionProvider(viewer);

		int nbRetry = 0;
		while (!Engine.isStartFailed && !Engine.isStarted) {
			try {
				Thread.sleep(500);
				nbRetry++;
			} catch (InterruptedException e) {
				// Ignore
			}

			// Aborting if too many retries
			if (nbRetry > 360) {
				return;
			}
		}

		if (Engine.isStarted) {
			initialize();
		}
	}

	public void initialize() {
		if (Engine.objectsProvider != this) {

			// Loads projects
			if (Engine.isStarted) {
				((ViewContentProvider) viewer.getContentProvider()).loadProjects();
				viewer.refresh();
			}

			// Studio mode
			Engine.setObjectsProvider(this);

			// Registering as Engine listener
			if (Engine.theApp != null) {
				Engine.theApp.addEngineListener(this);
				Engine.theApp.addMigrationListener(this);
			}
		}
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				ProjectExplorerView.this.fillContextMenu(manager);
			}
		});

		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void hookKeyboardActions() {
		viewer.getControl().addKeyListener(new KeyAdapter() {

			@Override
			public void keyReleased(KeyEvent event) {
				handleKeyReleased(event);
			}

		});
	}

	private void handleKeyReleased(KeyEvent event) {
		boolean bCtrl = (((event.stateMask & SWT.CONTROL) != 0) || ((event.stateMask & SWT.CTRL) != 0));
		boolean bAlt = (event.stateMask & SWT.ALT) != 0;
		int stateMask = event.stateMask;
		int keyCode = event.keyCode;
		char c = event.character;

		if (stateMask == 0) {
			// F2 for renaming
			if (keyCode == SWT.F2) {
				renameSelectedTreeObject();
			}
			// F5 for refreshing and executing selected requestable
			if (keyCode == SWT.F5) {
				refreshTree();
				Object object = getFirstSelectedDatabaseObject();
				if (object instanceof Transaction) {
					executeTransaction.run();
				} else if (object instanceof Sequence) {
					executeSequence.run();
				} else if (object instanceof TestCase) {
					executeTestCase.run();
				}
			}

			// DEL for deleting
			if (c == SWT.DEL) {
				Object object = getFirstSelectedTreeObject();
				if (object instanceof DatabaseObjectTreeObject) {
					deleteDatabaseObjectAction.run();
				} else if (object instanceof PropertyTableRowTreeObject) {
					deletePropertyTableRowAction.run();
				} else if (object instanceof PropertyTableColumnTreeObject) {
					deletePropertyTableColumnAction.run();
				}
			}
		}

		if (bAlt) {
			// Schema validation
			if (c == 'j') {
				projectValidateXSDAction.run();
			}
		}

		if (bCtrl) {
			// Copy/Cut/Paste
			if (c == 'c') {
				copyAction.run();
			}
			if (c == 'x') {
				cutAction.run();
			}
			if (c == 'v') {
				pasteAction.run();
			}

			if (c == 'g') {
				transactionEditHandlersAction.run();
			}
			// Saving
			if ((c == 's') || (keyCode == 115)) {
				projectExplorerSaveAllAction.run();
			}

			// F5 for executing default transaction
			if (keyCode == SWT.F5) {
				executeDefaultTransaction.run();
			}
		}

		// +/- for Priority
		if ((c == '+') || (keyCode == SWT.KEYPAD_ADD)) {
			increasePriorityAction.run();
		}
		if ((c == '-') || (keyCode == SWT.KEYPAD_SUBTRACT)) {
			decreasePriorityAction.run();
		}
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
		fillStatusBar(bars.getStatusLineManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
	}

	private void fillLocalToolBar(IToolBarManager manager) {

	}

	private void fillStatusBar(IStatusLineManager statusLine) {
		// Since Convertigo 4.6, no key check is required
		// statusLine.setMessage("Convertigo Studio "+
		// com.twinsoft.convertigo.engine.Version.fullProductVersion + " - " +
		// analyzeKey());
		statusLine.setMessage("Convertigo EMS Studio "
				+ com.twinsoft.convertigo.engine.Version.fullProductVersion);
	}

	// private String analyzeKey() {
	// String message = "";
	// Object[] args;
	// switch(Registration.keyType) {
	// case Registration.KEY_TYPE_EVALUATION:
	// args = new Object[] { new Integer(Registration.remainingDays) };
	// message =
	// MessageFormat.format("Evaluation version (remaining {0} day(s)...)",
	// args);
	// break;
	// case Registration.KEY_TYPE_COMMERCIAL:
	// args = new Object[] { Registration.registeredUser,
	// Integer.toString(Registration.licenseNumber) };
	// message = MessageFormat.format("Registered to {0}, license #{1}", args);
	// break;
	// }
	// return message;
	// }
	//
	private void fillContextMenu(IMenuManager manager) {
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	public TracePlayerThread tracePlayerThread = null;

	private void makeActions() {
		tracePlayAction = new TracePlayAction();
		doubleClickAction = new Action() {
			@Override
			public void run() {
				IStructuredSelection selection = (IStructuredSelection) viewer
						.getSelection();
				TreeObject treeObject = (TreeObject) selection
						.getFirstElement();

				if (treeObject instanceof UnloadedProjectTreeObject) {
					loadProject((UnloadedProjectTreeObject) treeObject);
				} else if (treeObject instanceof ConnectorTreeObject) {
					((ConnectorTreeObject) treeObject).launchEditor();
				} else if (treeObject instanceof SequenceTreeObject) {
					((SequenceTreeObject) treeObject).launchEditor();
				} else if (treeObject instanceof StepTreeObject) {
					showStepInPickerAction.run();
					if (treeObject instanceof IEditableTreeObject) {
						((IEditableTreeObject) treeObject).launchEditor(null);
					}
				} else if (treeObject instanceof VariableTreeObject2) {
					if (treeObject.getObject() instanceof StepVariable) {
						showStepInPickerAction.run();
					}
				} else if (treeObject instanceof IEditableTreeObject) {
					((IEditableTreeObject) treeObject).launchEditor(null);
				} else if (treeObject instanceof TraceTreeObject) {
					tracePlayAction.run();
				}
			}
		};

		undoAction = new UndoAction();
		redoAction = new RedoAction();
		copyAction = new ClipboardCopyAction();
		cutAction = new ClipboardCutAction();
		pasteAction = new ClipboardPasteAction();
		deleteDatabaseObjectAction = new DatabaseObjectDeleteAction();
		deletePropertyTableRowAction = new DeletePropertyTableRowAction();
		deletePropertyTableColumnAction = new DeletePropertyTableColumnAction();
		projectExplorerSaveAllAction = new ProjectExplorerSaveAllAction();
		decreasePriorityAction = new DatabaseObjectDecreasePriorityAction();
		increasePriorityAction = new DatabaseObjectIncreasePriorityAction();
		executeTransaction = new TransactionExecuteSelectedAction();
		executeDefaultTransaction = new TransactionExecuteDefaultAction();
		transactionEditHandlersAction = new TransactionEditHandlersAction();
		executeSequence = new SequenceExecuteSelectedAction();
		executeTestCase = new TestCaseExecuteSelectedAction();
		projectValidateXSDAction = new ProjectValidateXSDAction();
		showStepInPickerAction = new ShowStepInPickerAction();
		// projectCleanXSDAction = new ProjectCleanXSDAction();
	}

	private EventListenerList treeObjectListeners = new EventListenerList();

	public void addTreeObjectListener(TreeObjectListener treeObjectListener) {
		treeObjectListeners.add(TreeObjectListener.class, treeObjectListener);
	}

	public void removeTreeObjectListener(TreeObjectListener treeObjectListener) {
		treeObjectListeners
				.remove(TreeObjectListener.class, treeObjectListener);
	}

	public void fireTreeObjectPropertyChanged(TreeObjectEvent treeObjectEvent) {
		// Guaranteed to return a non-null array
		Object[] listeners = treeObjectListeners.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == TreeObjectListener.class) {
				try {
					((TreeObjectListener) listeners[i + 1])
							.treeObjectPropertyChanged(treeObjectEvent);
				} catch (Exception e) {
					String message = "fireTreeObjectPropertyChanged failed for treeObject: "
							+ ((TreeObject) listeners[i + 1]).getName();
					ConvertigoPlugin.logException(e, message);
				}
				;
			}
		}
	}

	protected List<TreeObject> addedTreeObjects = new ArrayList<TreeObject>();

	public void fireTreeObjectAdded(TreeObjectEvent treeObjectEvent) {
		// Guaranteed to return a non-null array
		Object[] listeners = treeObjectListeners.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == TreeObjectListener.class) {
				try {
					((TreeObjectListener) listeners[i + 1])
							.treeObjectAdded(treeObjectEvent);
				} catch (Exception e) {
					String message = "fireTreeObjectAdded failed for treeObject: "
							+ ((TreeObject) listeners[i + 1]).getName();
					ConvertigoPlugin.logException(e, message);
				}
				;
			}
		}
		DatabaseObjectTreeObject treeObject = (DatabaseObjectTreeObject) treeObjectEvent
				.getSource();
		DatabaseObject databaseObject = (DatabaseObject) treeObject.getObject();

		// Case of Project added
		if (databaseObject instanceof Project) {
			// Case of project copy : update references in steps if needed
			if (treeObjectEvent.oldValue != null) {
				String oldName = (String) treeObjectEvent.oldValue;
				String newName = (String) treeObjectEvent.newValue;
				boolean updateReferences = false;
				int update = 0;
				if (loadedProjectsContainsSequence()) {
					Shell shell = Display.getDefault().getActiveShell();
					CustomDialog customDialog = new CustomDialog(
							shell,
							"Update object references in steps",
							"Do you want to update project references in steps?\n You can replace '"
									+ oldName
									+ "' by '"
									+ newName
									+ "' in all loaded projects or replace '"
									+ oldName
									+ "' by '"
									+ newName
									+ "' in current project only.\n- click Cancel for none update.",
									670, 170,
							new ButtonSpec("Replace in all loaded projects",
									true),
							new ButtonSpec("Replace in current project", false),
							new ButtonSpec("Do not replace", false));
					int response = customDialog.open();
					if (response == 0) {
						updateReferences = true;
						update = TreeObjectEvent.UPDATE_ALL;
					}
					if (response == 1) {
						updateReferences = true;
						update = TreeObjectEvent.UPDATE_LOCAL;
					}
				}

				if (updateReferences) {
					treeObjectEvent.update = update;
					fireTreeObjectPropertyChanged(treeObjectEvent);
					((ProjectTreeObject) treeObject).save(false);
				}
			} else {
				List<String> list = ((ProjectTreeObject) treeObject)
						.getMissingTargetProjectList();
				if (!list.isEmpty()) {
					String message = "Some target project(s) are missing :\n";
					for (String s : list) {
						message += "   > \"" + s + "\" project\n";
					}
					message += "Please import missing project(s),\nor correct your sequence(s) before clean.";
					ConvertigoPlugin.logError(message, true);
				}

				/*
				 * try { ((ProjectTreeObject)treeObject).isXsdValid(); } catch
				 * (Exception e) { Shell shell =
				 * Display.getDefault().getActiveShell(); MessageBox messageBox
				 * = new MessageBox(shell,SWT.YES | SWT.NO | SWT.CANCEL |
				 * SWT.ICON_QUESTION | SWT.APPLICATION_MODAL);
				 * messageBox.setMessage
				 * ("Project's XSD file is corrupted. Would you like to clean it ?"
				 * ); int response = messageBox.open(); if (response == SWT.YES)
				 * { try { projectCleanXSDAction.run(); } catch (Exception e1) {
				 * ConvertigoPlugin.logException(e1,
				 * "Unable to clean project's XSD file"); } } }
				 */
			}
		}
	}

	public void fireTreeObjectRemoved(TreeObjectEvent treeObjectEvent) {
		// Prevents removed object to receive events
		if (treeObjectEvent.getSource() instanceof TreeObjectListener) {
			removeTreeObjectListener((TreeObjectListener) treeObjectEvent
					.getSource());
		}

		// Guaranteed to return a non-null array
		Object[] listeners = treeObjectListeners.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == TreeObjectListener.class) {
				try {
					((TreeObjectListener) listeners[i + 1])
							.treeObjectRemoved(treeObjectEvent);
				} catch (Exception e) {
					String message = "fireTreeObjectRemoved failed for treeObject: "
							+ ((TreeObject) listeners[i + 1]).getName();
					ConvertigoPlugin.logException(e, message);
				}
				;
			}
		}
	}

	public IEditorPart getConnectorEditor(Connector connector) {
		IEditorPart editorPart = null;
		IWorkbenchPage activePage = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage();

		if (activePage != null) {
			if (connector != null) {
				IEditorReference[] editorRefs = activePage
						.getEditorReferences();
				for (int i = 0; i < editorRefs.length; i++) {
					IEditorReference editorRef = (IEditorReference) editorRefs[i];
					try {
						IEditorInput editorInput = editorRef.getEditorInput();
						if ((editorInput != null)
								&& (editorInput instanceof ConnectorEditorInput)) {
							if (((ConnectorEditorInput) editorInput).connector
									.equals(connector)) {
								editorPart = editorRef.getEditor(false);
								break;
							}
						}
					} catch (PartInitException e) {
						// ConvertigoPlugin.logException(e,
						// "Error while retrieving the connector editor '" +
						// editorRef.getName() + "'");
					}
				}
			}
		}
		return editorPart;
	}

	// private TreeObject oldSelection = null;
	final TreeItem lastItem[] = new TreeItem[1];

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				// oldSelection = null;
				doubleClickAction.run();
			}
		});
	}

	private void hookSelectionChangedEvent() {
		addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event
						.getSelection();
				TreeObject treeObject = (TreeObject) selection
						.getFirstElement();

				if (treeObject != null) {
					// remember current project
					ProjectTreeObject projectTreeObject = null;
					if (treeObject instanceof ProjectTreeObject) {
						projectTreeObject = (ProjectTreeObject) treeObject;
					} else {
						projectTreeObject = treeObject.getProjectTreeObject();
					}
					if (projectTreeObject != null) {
						ConvertigoPlugin.projectManager
								.setCurrentProject(projectTreeObject);
					}

					// oldSelection = treeObject;
					TreeItem[] items = viewer.getTree().getSelection();
					if (items.length > 0) {
						lastItem[0] = items[0];
					}
				}
			}
		});
	}

	private void hookGlobalActions() {
		IActionBars bars = getViewSite().getActionBars();

		bars.setGlobalActionHandler(ActionFactory.COPY.getId(), copyAction);
		bars.setGlobalActionHandler(ActionFactory.CUT.getId(), cutAction);
		bars.setGlobalActionHandler(ActionFactory.PASTE.getId(), pasteAction);

		/*
		 * bars.setGlobalActionHandler(ActionFactory.UNDO.getId(), undoAction);
		 * bars.setGlobalActionHandler(ActionFactory.REDO.getId(), redoAction);
		 */
	}

	public void loadProject(UnloadedProjectTreeObject unloadedProjectTreeObject) {
		loadProject(unloadedProjectTreeObject, false, null);
	}

	protected synchronized void loadProject(
			UnloadedProjectTreeObject unloadedProjectTreeObject,
			boolean isCopy, String originalName) {
		String projectName = unloadedProjectTreeObject.toString();
		if (!MigrationManager.isProjectMigrated(projectName)) {
			String message = "Could not load the project \"" + projectName
					+ "\" while it is still migrating.";
			ConvertigoPlugin.logDebug("[ProjectExplorerView] loadProject : "
					+ message);
			ConvertigoPlugin.logError(message, Boolean.TRUE);
		} else {
			try {
				ProjectLoadingJob job = new ProjectLoadingJob(viewer,
						unloadedProjectTreeObject, isCopy, originalName);
				job.setUser(true);
				job.schedule();
			} catch (Exception e) {
				String message = "Error while loading the project \""
						+ projectName + "\".\n" + e.getMessage();
				ConvertigoPlugin.logException(e, message);
			}
		}
	}

	private Text editingTextCtrl = null;

	public boolean isEditing() {
		return editingTextCtrl != null;
	}

	public String getEditingText() {
		if (editingTextCtrl != null) {
			return editingTextCtrl.getText();
		}
		return null;
	}

	public void setEditingText(String text) {
		if ((editingTextCtrl != null) && (text != null)) {
			String selection = editingTextCtrl.getSelectionText();
			String oldText = editingTextCtrl.getText();
			String newText = "";
			int caret = editingTextCtrl.getCaretPosition();

			if (selection.equals(oldText)) {
				editingTextCtrl.setText(text);
			} else {
				if (caret == 0) {
					newText = text + oldText;
				} else if (caret == oldText.length()) {
					newText = oldText + text;
				} else {
					String part1 = oldText.substring(0, caret);
					String part2 = oldText.substring(caret, oldText.length());
					newText = part1 + text + part2;
				}
				editingTextCtrl.setText(newText);
			}
		}
	}

	private void edit(TreeObject treeObject) {
		final Tree tree = viewer.getTree();
		final TreeEditor editor = new TreeEditor(tree);
		final Color black = getSite().getShell().getDisplay()
				.getSystemColor(SWT.COLOR_BLACK);

		TreeItem[] items = tree.getSelection();
		if (items.length > 0) {
			final TreeItem item = items[0];
			final TreeObject theTreeObject = treeObject;

			if (treeObject instanceof ProjectTreeObject) {
				if (((ProjectTreeObject) treeObject).getModified()) {
					// Project need to be saved to avoid xsd/wsdl modification
					// errors - Fix ticket #2265
					ConvertigoPlugin
							.warningMessageBox("Please save project before renaming it.");
					return;
				}
			}
			if (treeObject.getObject() instanceof HandlerStatement) {
				return;
			}
			if ((item != null) && (item == lastItem[0])) {
				boolean isCarbon = SWT.getPlatform().equals("carbon");
				final Composite composite = new Composite(tree, SWT.NONE);
				if (!isCarbon) {
					composite.setBackground(black);
				}
				final Text text = new Text(composite, SWT.NONE);
				final int inset = isCarbon ? 0 : 1;
				composite.addListener(SWT.Resize, new Listener() {
					public void handleEvent(Event e) {
						Rectangle rect = composite.getClientArea();
						text.setBounds(rect.x + inset, rect.y + inset,
								rect.width - inset * 2, rect.height - inset * 2);
					}
				});
				Listener textListener = new Listener() {
					public void handleEvent(final Event e) {
						String newName = null;
						String oldName = null;
						boolean needRefresh = false;
						boolean needProjectReload = false;

						if (theTreeObject instanceof DatabaseObjectTreeObject) {
							oldName = ((DatabaseObject) ((DatabaseObjectTreeObject) theTreeObject)
									.getObject()).getName();
						} else if (theTreeObject instanceof TraceTreeObject) {
							oldName = ((TraceTreeObject) theTreeObject)
									.getName();
						}

						switch (e.type) {
						case SWT.FocusOut:
							editingTextCtrl = null;
							composite.dispose();
							break;
						case SWT.Verify:
							String newText = text.getText();
							String leftText = newText.substring(0, e.start);
							String rightText = newText.substring(e.end,
									newText.length());
							GC gc = new GC(text);
							Point size = gc.textExtent(leftText + e.text
									+ rightText);
							gc.dispose();
							size = text.computeSize(size.x, SWT.DEFAULT);
							editor.horizontalAlignment = SWT.LEFT;
							Rectangle itemRect = item.getBounds(),
							rect = tree.getClientArea();
							editor.minimumWidth = Math.max(size.x,
									itemRect.width) + inset * 2;
							int left = itemRect.x,
							right = rect.x + rect.width;
							editor.minimumWidth = Math.min(editor.minimumWidth,
									right - left);
							editor.minimumHeight = size.y + inset * 2;
							editor.layout();
							break;
						case SWT.Traverse:
							switch (e.detail) {
							case SWT.TRAVERSE_RETURN:
								newName = text.getText();
								if (theTreeObject instanceof DatabaseObjectTreeObject) {
									DatabaseObjectTreeObject dbObjectTreeObject = (DatabaseObjectTreeObject) theTreeObject;
									if (dbObjectTreeObject.rename(newName,
											Boolean.TRUE)) {
										item.setText(newName);
										needRefresh = true;
										if (theTreeObject instanceof ProjectTreeObject) {
											needProjectReload = true;
										}
									}
								} else if (theTreeObject instanceof TraceTreeObject) {
									TraceTreeObject traceTreeObject = (TraceTreeObject) theTreeObject;
									traceTreeObject.rename(newName);
									if (traceTreeObject.hasChanged) {
										item.setText(newName);
										traceTreeObject.hasChanged = false;
										needRefresh = true;
									}
								}
								// FALL THROUGH
							case SWT.TRAVERSE_ESCAPE:
								editingTextCtrl = null;
								composite.dispose();
								e.doit = false;
							}
							break;
						}

						if (needRefresh) {
							boolean updateReferences = false;
							int update = 0;
							// Updates references in
							// SequenceStep/TransactionStep if needed
							if ((theTreeObject instanceof ProjectTreeObject)
									|| (theTreeObject instanceof SequenceTreeObject)
									|| (theTreeObject instanceof ConnectorTreeObject)
									|| (theTreeObject instanceof TransactionTreeObject)) {
								String objectType = "";
								if (theTreeObject instanceof ProjectTreeObject) {
									objectType = "project";
								} else if (theTreeObject instanceof SequenceTreeObject) {
									objectType = "sequence";
								} else if (theTreeObject instanceof ConnectorTreeObject) {
									objectType = "connector";
								} else if (theTreeObject instanceof TransactionTreeObject) {
									objectType = "transaction";
								}

								if (loadedProjectsContainsSequence()) {
									Shell shell = Display.getDefault()
											.getActiveShell();
									CustomDialog customDialog = new CustomDialog(
											shell,
											"Update object references in steps",
											"Do you want to update "
													+ objectType
													+ " references in steps?\n You can replace '"
													+ oldName
													+ "' by '"
													+ newName
													+ "' in all loaded projects \n or replace '"
													+ oldName
													+ "' by '"
													+ newName
													+ "' in current project only.",
													670, 170,
											new ButtonSpec("Replace in all loaded projects", true),
											new ButtonSpec("Replace in current project", false),
											new ButtonSpec("Do not replace anywhere", false));
									int response = customDialog.open();
									if (response == 0) {
										updateReferences = true;
										update = TreeObjectEvent.UPDATE_ALL;
									}
									if (response == 1) {
										updateReferences = true;
										update = TreeObjectEvent.UPDATE_LOCAL;
									}
								}
							}

							TreeObjectEvent treeObjectEvent = null;
							if (updateReferences) {
								treeObjectEvent = new TreeObjectEvent(
										theTreeObject, "name", oldName,
										newName, update);
							} else {
								treeObjectEvent = new TreeObjectEvent(
										theTreeObject, "name", oldName, newName);
							}

							ProjectExplorerView.this.refreshTree();
							ProjectExplorerView.this
									.setSelectedTreeObject(theTreeObject);
							ProjectExplorerView.this
									.fireTreeObjectPropertyChanged(treeObjectEvent);
							if (updateReferences && needProjectReload) {
								((ProjectTreeObject) theTreeObject).save(false);
							}

							StructuredSelection structuredSelection = new StructuredSelection(theTreeObject);
							ISelectionListener listener = null;
							
							// refresh properties view
							listener = ConvertigoPlugin.getDefault().getPropertiesView();
							if (listener != null)
								listener.selectionChanged(
											(IWorkbenchPart) ProjectExplorerView.this,
											structuredSelection);
							
							// refresh references view
							listener = ConvertigoPlugin.getDefault().getReferencesView();
							if (listener != null)
								listener.selectionChanged(
										(IWorkbenchPart) ProjectExplorerView.this,
										structuredSelection);
						}
						if (needProjectReload) {
							reloadProject(theTreeObject);
							refreshTree();
						}
					}
				};
				text.addListener(SWT.FocusOut, textListener);
				text.addListener(SWT.Traverse, textListener);
				text.addListener(SWT.Verify, textListener);
				editor.setEditor(composite, item);
				if (theTreeObject instanceof DatabaseObjectTreeObject) {
					text.setText(((DatabaseObjectTreeObject) theTreeObject)
							.getName());
				} else if (theTreeObject instanceof TraceTreeObject) {
					text.setText(((TraceTreeObject) theTreeObject).getName());
				}
				text.selectAll();
				text.setFocus();
				editingTextCtrl = text;
			}
			lastItem[0] = item;
		}
	}

	private void reload(TreeParent parentTreeObject,
			DatabaseObject parentDatabaseObject, boolean bDynamicSchemaUpdate)
			throws EngineException, IOException {
		IProgressService progressService = PlatformUI.getWorkbench()
				.getProgressService();
		try {
			progressService.busyCursorWhile(new ReloadWithProgress(viewer,
					parentTreeObject, parentDatabaseObject,
					bDynamicSchemaUpdate));
		} catch (InvocationTargetException e) {
		} catch (InterruptedException e) {
		}

	}

	private class ReloadWithProgress implements IRunnableWithProgress,
			DatabaseObjectListener {
		private ProjectTreeObject projectTreeObject;
		private TreeParent parentTreeObject;
		private DatabaseObject parentDatabaseObject;
		private TreeViewer viewer;
		private Object[] objects = null;
		private String[] expendedPaths = null;
		private IProgressMonitor monitor;
		private boolean bDynamicSchemaUpdate = true;

		public ReloadWithProgress(TreeViewer viewer,
				TreeParent parentTreeObject,
				DatabaseObject parentDatabaseObject,
				boolean bDynamicSchemaUpdate) {
			super();
			this.viewer = viewer;
			this.parentTreeObject = parentTreeObject;
			this.parentDatabaseObject = parentDatabaseObject;
			this.bDynamicSchemaUpdate = bDynamicSchemaUpdate;
		}

		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			String dboName = (parentDatabaseObject instanceof Step) ? ((Step) parentDatabaseObject)
					.getStepNodeName() : parentDatabaseObject.getName();

			this.monitor = monitor;

			try {
				int worksNumber = 10;

				// try {
				// String latestSavedDatabaseObjectQName =
				// ((DatabaseObjectTreeObject)parentTreeObject).latestSavedDatabaseObjectQName;
				// String latestSavedDatabaseObjectPath =
				// latestSavedDatabaseObjectQName.substring(0,
				// latestSavedDatabaseObjectQName.lastIndexOf('/'));
				// File file = new File(Engine.PROJECTS_PATH +
				// latestSavedDatabaseObjectPath);
				// worksNumber = 2 *
				// ConvertigoPlugin.projectManager.getNumberOfObjects(file);
				// }
				// catch (Exception e) {}

				monitor.beginTask("Reloading \"" + dboName + "\" object",
						worksNumber);

				monitor.subTask("Storing expanded paths...");
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						// Store the currently expanded tree objects
						objects = viewer.getExpandedElements();
						if (objects != null) {
							expendedPaths = new String[objects.length];
							for (int i = 0; i < objects.length; i++) {
								TreeObject object = (TreeObject) objects[i];
								expendedPaths[i] = object.getPath();
							}
						}
					}
				});

				try {
					projectTreeObject = parentTreeObject.getProjectTreeObject();

					if (!bDynamicSchemaUpdate
							&& (parentTreeObject instanceof DatabaseObjectTreeObject))
						projectTreeObject
								.setDynamicSchemaUpdate(bDynamicSchemaUpdate);

					// First remove all children of object
					monitor.subTask("Removing objects...");
					parentTreeObject.removeAllChildren();

					// Then load object again
					monitor.subTask("Loading objects...");
					Engine.theApp.databaseObjectsManager
							.addDatabaseObjectListener(this);
					loadDatabaseObject(parentTreeObject, parentDatabaseObject,
							monitor);
				} finally {
					projectTreeObject.resetDynamicSchemaUpdate();

					Engine.theApp.databaseObjectsManager
							.removeDatabaseObjectListener(this);
				}
			} catch (Exception e) {
				ConvertigoPlugin.logException(
											e, "Failure when loading objects");
			} finally {
				// Updating the tree viewer
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						if (parentTreeObject != null) {

							// Reload is complete, notify now for newly added
							// objects
							for (TreeObject ob : addedTreeObjects) {
								fireTreeObjectAdded(new TreeObjectEvent(ob));
							}
							addedTreeObjects.clear();

							// if DynamicSchemaUpdate has been disabled (for
							// load performances)
							// update project's xsd and wsdl files now
							if (!bDynamicSchemaUpdate
									&& (parentTreeObject instanceof DatabaseObjectTreeObject)) {
								try {
									projectTreeObject
											.updateWebService((DatabaseObjectTreeObject) parentTreeObject);
								} catch (Throwable e) {
									ConvertigoPlugin.logException(
											e,
											"Unable to update web service for project \""
													+ projectTreeObject
															.getName() + "\"");
								}
							}

							refreshTreeObject(parentTreeObject);

							// Restore the previously expanded tree objects
							if (expendedPaths != null) {
								for (int i = 0; i < expendedPaths.length; i++) {
									String previousPath = expendedPaths[i];
									TreeObject treeObject = findTreeObjectByPath(
											parentTreeObject, previousPath);
									if (treeObject != null)
										objects[i] = treeObject;
								}

								viewer.setExpandedElements(objects);
							}
						}
					}
				});
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.twinsoft.convertigo.engine.DatabaseObjectListener#
		 * databaseObjectLoaded
		 * (com.twinsoft.convertigo.engine.DatabaseObjectLoadedEvent)
		 */
		public void databaseObjectLoaded(DatabaseObjectLoadedEvent event) {
			DatabaseObject dbo = (DatabaseObject) event.getSource();
			String dboName = dbo instanceof Step ? ((Step) dbo)
					.getStepNodeName() : dbo.getName();
			monitor.subTask("Object \"" + dboName + "\" loaded");
			monitor.worked(1);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.twinsoft.convertigo.engine.DatabaseObjectListener#
		 * databaseObjectImported
		 * (com.twinsoft.convertigo.engine.DatabaseObjectImportedEvent)
		 */
		public void databaseObjectImported(DatabaseObjectImportedEvent event) {

		}
	}

	protected void createDirsAndFiles(String projectName) {
		createDir(projectName);
		createFiles(projectName);
	}

	protected void createDir(String projectName) {
		File file = null;

		file = new File(Engine.PROJECTS_PATH + "/" + projectName + "/_private");
		if (!file.exists())
			file.mkdir();

		file = new File(Engine.PROJECTS_PATH + "/" + projectName + "/Traces");
		if (!file.exists())
			file.mkdir();
	}

	private void createFiles(String projectName) {
		createXsd(projectName);
		createWsdl(projectName);
		createIndexFile(projectName);
	}

	private void createIndexFile(String projectName) {
		try {
			ProjectUtils.copyIndexFile(projectName);
		} catch (Exception e) {
			ConvertigoPlugin.logException(e,
					"Error creating index.html file for project '"
							+ projectName + "'", Boolean.FALSE);
		}
	}

	private void createXsd(String projectName) {
		try {
			ProjectUtils.createXsdFile(Engine.PROJECTS_PATH, projectName);
		} catch (Exception e) {
			ConvertigoPlugin
					.logException(e, "Error creating xsd file for project '"
							+ projectName + "'", Boolean.FALSE);
		}

		createTempXsd(projectName);
	}

	private void createWsdl(String projectName) {
		try {
			ProjectUtils.createWsdlFile(Engine.PROJECTS_PATH, projectName);
		} catch (Exception e) {
			ConvertigoPlugin.logException(e,
					"Error creating wsdl file for project '" + projectName
							+ "'", Boolean.FALSE);
		}

		createTempWsdl(projectName);
	}

	private void createTempXsd(String projectName) {
		String xsdPath = Engine.PROJECTS_PATH + "/" + projectName + "/"
				+ projectName + ".xsd";
		String tempPath = Engine.PROJECTS_PATH + "/" + projectName + "/"
				+ projectName + ".temp.xsd";
		try {
			copyToTemp(projectName, xsdPath, tempPath);
		} catch (Exception e) {
			ConvertigoPlugin.logException(e,
					"Error creating temporary xsd file for project '"
							+ projectName + "'", Boolean.FALSE);
		}
	}

	private void createTempWsdl(String projectName) {
		String wsdlPath = Engine.PROJECTS_PATH + "/" + projectName + "/"
				+ projectName + ".wsdl";
		String tempPath = Engine.PROJECTS_PATH + "/" + projectName + "/"
				+ projectName + ".temp.wsdl";
		try {
			copyToTemp(projectName, wsdlPath, tempPath);
		} catch (Exception e) {
			ConvertigoPlugin.logException(e,
					"Error creating temporary wsdl file for project '"
							+ projectName + "'", Boolean.FALSE);
		}
	}

	private void copyToTemp(String projectName, String sourceFilePath,
			String targetFilePath) throws EngineException {
		try {
			File sourceFile = new File(sourceFilePath);
			if (sourceFile.exists()) {
				File targetFile = new File(targetFilePath);
				if (!targetFile.exists()) {
					try {
						if (targetFile.createNewFile()) {
							String line;
							BufferedReader br = new BufferedReader(
									new FileReader(sourceFilePath));
							BufferedWriter bw = new BufferedWriter(
									new FileWriter(targetFilePath));
							while ((line = br.readLine()) != null) {
								line = line.replaceAll(projectName + ".xsd",
										projectName + ".temp.xsd");
								bw.write(line);
								bw.newLine();
							}
							bw.close();
							br.close();
						} else {
							throw new EngineException("Error creating '"
									+ targetFilePath + "'");
						}
					} catch (IOException e) {
						throw new EngineException("Error writing from '"
								+ sourceFilePath + "' to '" + targetFilePath
								+ "'");
					}
				}
			} else {
				throw new EngineException("'" + sourceFilePath
						+ "' does not exist");
			}
		} catch (Exception e) {
			throw new EngineException("Unable to copy '" + sourceFilePath
					+ "' to '" + targetFilePath + "'", e);
		}
	}

	private void loadDatabaseObject(TreeParent parentTreeObject,
			DatabaseObject parentDatabaseObject, IProgressMonitor monitor)
			throws EngineException, IOException {
		loadDatabaseObject(parentTreeObject, parentDatabaseObject, null,
				monitor);
	}

	public void loadDatabaseObject(TreeParent parentTreeObject,
			DatabaseObject parentDatabaseObject,
			ProjectLoadingJob projectLoadingJob) throws EngineException,
			IOException {
		loadDatabaseObject(parentTreeObject, parentDatabaseObject,
				projectLoadingJob, projectLoadingJob.getMonitor());
	}

	private void loadDatabaseObject(TreeParent parentTreeObject,
			DatabaseObject parentDatabaseObject,
			ProjectLoadingJob projectLoadingJob, final IProgressMonitor monitor)
			throws EngineException, IOException {
		// Add load subtask here because of databaseObjectLoaded event no more
		// received since memory improvement
		// (getSubDatabaseObject called only when necessary)

		try {
			new WalkHelper() {
				// recursion parameters
				TreeParent parentTreeObject;
				ProjectLoadingJob projectLoadingJob;

				// sibling parameters
				ObjectsFolderTreeObject currentTreeFolder = null;

				public void init(DatabaseObject databaseObject,
						TreeParent parentTreeObject,
						ProjectLoadingJob projectLoadingJob) throws Exception {
					this.parentTreeObject = parentTreeObject;
					this.projectLoadingJob = projectLoadingJob;

					walkInheritance = true;
					super.init(databaseObject);
				}

				@Override
				protected void walk(DatabaseObject databaseObject)
						throws Exception {
					// retrieve recursion parameters
					final TreeParent parentTreeObject = this.parentTreeObject;
					final ProjectLoadingJob projectLoadingJob = this.projectLoadingJob;

					// retrieve sibling parameters
					ObjectsFolderTreeObject currentTreeFolder = this.currentTreeFolder;

					String dboName = (databaseObject instanceof Step) ? ((Step) databaseObject)
							.getStepNodeName() : databaseObject.getName();
					monitor.subTask("Loading databaseObject '" + dboName
							+ "'...");

					DatabaseObjectTreeObject databaseObjectTreeObject = null;

					// first call case, the tree object already exists and its
					// content is just refreshed
					if (parentTreeObject.getObject() == databaseObject) {
						databaseObjectTreeObject = (DatabaseObjectTreeObject) parentTreeObject;
					}
					// recurcive call case, the tree object doesn't exist and
					// must be added to the parent tree object
					else {
						int folderType = Integer.MIN_VALUE;
						if (databaseObject instanceof Connector) {
							folderType = ObjectsFolderTreeObject.FOLDER_TYPE_CONNECTORS;
							databaseObjectTreeObject = new ConnectorTreeObject(
									viewer, (Connector) databaseObject, false);

						} else if (databaseObject instanceof Sequence) {
							folderType = ObjectsFolderTreeObject.FOLDER_TYPE_SEQUENCES;
							databaseObjectTreeObject = new SequenceTreeObject(
									viewer, (Sequence) databaseObject, false);

						} else if (databaseObject instanceof MobileDevice) {
							folderType = ObjectsFolderTreeObject.FOLDER_TYPE_MOBILEDEVICES;
							databaseObjectTreeObject = new MobileDeviceTreeObject(
									viewer, (MobileDevice) databaseObject,
									false);

						} else if (databaseObject instanceof Pool) {
							folderType = ObjectsFolderTreeObject.FOLDER_TYPE_POOLS;
							databaseObjectTreeObject = new DatabaseObjectTreeObject(
									viewer, databaseObject, false);

						} else if (databaseObject instanceof Transaction) {
							folderType = ObjectsFolderTreeObject.FOLDER_TYPE_TRANSACTIONS;
							databaseObjectTreeObject = new TransactionTreeObject(
									viewer, (Transaction) databaseObject, false);

						} else if (databaseObject instanceof ScreenClass) {
							if (databaseObject.getParent() instanceof IScreenClassContainer<?>) {
								folderType = ObjectsFolderTreeObject.FOLDER_TYPE_SCREEN_CLASSES;
								databaseObjectTreeObject = new ScreenClassTreeObject(
										viewer, (ScreenClass) databaseObject,
										false);
							} else {
								folderType = ObjectsFolderTreeObject.FOLDER_TYPE_INHERITED_SCREEN_CLASSES;
								databaseObjectTreeObject = new ScreenClassTreeObject(
										viewer, (ScreenClass) databaseObject,
										false);
							}

						} else if (databaseObject instanceof Sheet) {
							folderType = ObjectsFolderTreeObject.FOLDER_TYPE_SHEETS;
							databaseObjectTreeObject = new SheetTreeObject(
									viewer,
									(Sheet) databaseObject,
									parentTreeObject.getObject() != databaseObject
											.getParent());

						} else if (databaseObject instanceof TestCase) {
							folderType = ObjectsFolderTreeObject.FOLDER_TYPE_TESTCASES;
							databaseObjectTreeObject = new TestCaseTreeObject(
									viewer, (TestCase) databaseObject, false);

						} else if (databaseObject instanceof Variable) {
							folderType = ObjectsFolderTreeObject.FOLDER_TYPE_VARIABLES;
							databaseObjectTreeObject = new VariableTreeObject2(
									viewer, (Variable) databaseObject, false);

						} else if (databaseObject instanceof Step) {
							if (databaseObject.getParent() instanceof Sequence) {
								folderType = ObjectsFolderTreeObject.FOLDER_TYPE_STEPS;
							}
							databaseObjectTreeObject = new StepTreeObject(
									viewer, (Step) databaseObject, false);

						} else if (databaseObject instanceof Statement) {
							if (databaseObject.getParent() instanceof Transaction) {
								folderType = ObjectsFolderTreeObject.FOLDER_TYPE_FUNCTIONS;
							}
							databaseObjectTreeObject = new StatementTreeObject(
									viewer, (Statement) databaseObject, false);

						} else if (databaseObject instanceof Criteria) {
							folderType = ObjectsFolderTreeObject.FOLDER_TYPE_CRITERIAS;
							databaseObjectTreeObject = new CriteriaTreeObject(
									viewer,
									(Criteria) databaseObject,
									parentTreeObject.getObject() != databaseObject
											.getParent());

						} else if (databaseObject instanceof ExtractionRule) {
							folderType = ObjectsFolderTreeObject.FOLDER_TYPE_EXTRACTION_RULES;
							databaseObjectTreeObject = new ExtractionRuleTreeObject(
									viewer,
									(ExtractionRule) databaseObject,
									parentTreeObject.getObject() != databaseObject
											.getParent());

						} else if (databaseObject instanceof BlockFactory) {
							databaseObjectTreeObject = new DatabaseObjectTreeObject(
									viewer,
									databaseObject,
									parentTreeObject.getObject() != databaseObject
											.getParent());

						} else {
							// unknow DBO case !!!
							databaseObjectTreeObject = new DatabaseObjectTreeObject(
									viewer, databaseObject, false);
						}
						// no virtual folder
						if (folderType == Integer.MIN_VALUE) {
							parentTreeObject.addChild(databaseObjectTreeObject);
						}
						// virtual folder creation or reuse
						else {
							if (currentTreeFolder == null
									|| currentTreeFolder.folderType != folderType) {
								currentTreeFolder = new ObjectsFolderTreeObject(
										viewer, folderType);
								parentTreeObject.addChild(currentTreeFolder);
							}
							currentTreeFolder
									.addChild(databaseObjectTreeObject);
						}

						// new value of recursion parameters
						this.parentTreeObject = databaseObjectTreeObject;
					}

					// special databaseObject cases
					if (databaseObject instanceof Project) {
						Project project = (Project) databaseObject;

						// Creates directories and files
						createDirsAndFiles(project.getName());

						// Connectors
						Collection<Connector> connectors = project
								.getConnectorsList();
						if (connectors.size() != 0) {
							// Set default connector if none
							if (project.getDefaultConnector() == null) {
								// Report from 4.5: fix #401
								ConvertigoPlugin
										.logWarning(
												null,
												"Project \""
														+ project.getName()
														+ "\" has no default connector. Try to set a default one.");
								Connector defaultConnector = connectors
										.iterator().next();
								try {
									project.setDefaultConnector(defaultConnector);
									defaultConnector.hasChanged = true;
								} catch (Exception e) {
									ConvertigoPlugin.logWarning(e,
											"Unable to set a default connector for project \""
													+ project.getName() + "\"");
								}
							}

							// Refresh Traces folder
							IFolder ifolder = ((ProjectTreeObject) parentTreeObject)
									.getFolder("Traces");
							if (ifolder.exists()) {
								try {
									ifolder.refreshLocal(
											IResource.DEPTH_INFINITE,
											new NullProgressMonitor());
								} catch (CoreException e) {
								}
							}
						}

					} else if (databaseObject instanceof Connector) {
						Connector connector = (Connector) databaseObject;

						// Open connector editor
						if (projectLoadingJob != null && connector.isDefault) {
							projectLoadingJob
									.setDefaultConnectorTreeObject((ConnectorTreeObject) databaseObjectTreeObject);
						}

						// Traces
						if (connector instanceof JavelinConnector) {
							String projectName = databaseObject.getProject()
									.getName();

							if (projectLoadingJob == null) {
								if (MigrationManager
										.isProjectMigrated(projectName)) {
									UnloadedProjectTreeObject unloadedProjectTreeObject = new UnloadedProjectTreeObject(
											databaseObjectTreeObject.viewer,
											projectName);
									this.projectLoadingJob = new ProjectLoadingJob(
											databaseObjectTreeObject.viewer,
											unloadedProjectTreeObject);
									this.projectLoadingJob.loadTrace(
											databaseObjectTreeObject, new File(
													Engine.PROJECTS_PATH + "/"
															+ projectName
															+ "/Traces/"
															+ connector.getName()));
								}
							}
							if (projectLoadingJob != null) {
								projectLoadingJob.loadTrace(
										databaseObjectTreeObject, new File(
												Engine.PROJECTS_PATH + "/"
														+ projectName
														+ "/Traces/"
														+ connector.getName()));
							}
						}

					} else if (databaseObject instanceof Transaction) {
						Transaction transaction = (Transaction) databaseObject;

						// Functions
						List<HandlersDeclarationTreeObject> treeObjects = new LinkedList<HandlersDeclarationTreeObject>();
						String line;
						int lineNumber = 0;
						BufferedReader br = new BufferedReader(
								new StringReader(transaction.handlers));

						while ((line = br.readLine()) != null) {
							line = line.trim();
							lineNumber++;
							if (line.startsWith("function ")) {
								try {
									String functionName = line.substring(9,
											line.indexOf(')') + 1);
									HandlersDeclarationTreeObject handlersDeclarationTreeObject;

									if (functionName
											.endsWith(JavelinTransaction.EVENT_ENTRY_HANDLER
													+ "()")) {
										handlersDeclarationTreeObject = new HandlersDeclarationTreeObject(
												viewer,
												functionName,
												HandlersDeclarationTreeObject.TYPE_FUNCTION_SCREEN_CLASS_ENTRY,
												lineNumber);
									} else if (functionName
											.endsWith(JavelinTransaction.EVENT_EXIT_HANDLER
													+ "()")) {
										handlersDeclarationTreeObject = new HandlersDeclarationTreeObject(
												viewer,
												functionName,
												HandlersDeclarationTreeObject.TYPE_FUNCTION_SCREEN_CLASS_EXIT,
												lineNumber);
									} else {
										handlersDeclarationTreeObject = new HandlersDeclarationTreeObject(
												viewer,
												functionName,
												HandlersDeclarationTreeObject.TYPE_OTHER,
												lineNumber);
									}
									treeObjects
											.add(handlersDeclarationTreeObject);
								} catch (StringIndexOutOfBoundsException e) {
									// Ignore
								}
							}
						}

						if (treeObjects.size() != 0) {
							ObjectsFolderTreeObject objectsFolderTreeObject = new ObjectsFolderTreeObject(
									viewer,
									ObjectsFolderTreeObject.FOLDER_TYPE_FUNCTIONS);
							databaseObjectTreeObject
									.addChild(objectsFolderTreeObject);

							for (HandlersDeclarationTreeObject handlersDeclarationTreeObject : treeObjects) {
								objectsFolderTreeObject
										.addChild(handlersDeclarationTreeObject);
							}
						}

					} else if (databaseObject instanceof Sheet) {
						addTemplates((Sheet) databaseObject,
								databaseObjectTreeObject);

					} else if (databaseObject instanceof ITablesProperty) {
						ITablesProperty iTablesProperty = (ITablesProperty) databaseObject;
						String[] tablePropertyNames = iTablesProperty
								.getTablePropertyNames();

						for (int i = 0; i < tablePropertyNames.length; i++) {
							String tablePropertyName = tablePropertyNames[i];
							String tableRenderer = iTablesProperty
									.getTableRenderer(tablePropertyName);
							XMLVector<XMLVector<Object>> xmlv = iTablesProperty
									.getTableData(tablePropertyName);
							if (tableRenderer
									.equals("XMLTableDescriptionTreeObject")) {
								XMLTableDescriptionTreeObject propertyXMLTableTreeObject = new XMLTableDescriptionTreeObject(
										viewer, tablePropertyName, xmlv,
										databaseObjectTreeObject);
								databaseObjectTreeObject
										.addChild(propertyXMLTableTreeObject);
							} else if (tableRenderer
									.equals("XMLRecordDescriptionTreeObject")) {
								XMLRecordDescriptionTreeObject propertyXMLRecordTreeObject = new XMLRecordDescriptionTreeObject(
										viewer, tablePropertyName, xmlv,
										databaseObjectTreeObject);
								databaseObjectTreeObject
										.addChild(propertyXMLRecordTreeObject);
							}
						}

					}

					monitor.worked(1);

					// children cannot be added in the current virtual folder
					this.currentTreeFolder = null;

					super.walk(databaseObject);

					// restore recursion parameters
					this.parentTreeObject = parentTreeObject;
					this.projectLoadingJob = projectLoadingJob;

					// restore sibling parameters
					this.currentTreeFolder = currentTreeFolder;
				}

			}.init(parentDatabaseObject, parentTreeObject, projectLoadingJob);
		} catch (EngineException e) {
			throw e;
		} catch (Exception e) {
			throw new EngineException("Exception in copyDatabaseObject", e);
		}
	}

	private void addTemplates(Sheet sheet, DatabaseObjectTreeObject treeObject) {
		TemplateTreeObject templateTreeObject;

		String xslFileName = sheet.getUrl();

		try {
			// Refresh project resource
			IProject project = ConvertigoPlugin.getDefault()
					.getProjectPluginResource(sheet.getProject().getName());

			IFile file = project.getFile(new Path(xslFileName));
			if (file.exists()) {
				Document doc = parseXslFile(file);

				NodeList nl = doc.getElementsByTagName("xsl:include");
				for (int i = 0; i < nl.getLength(); i++) {
					Node node = nl.item(i);
					NamedNodeMap attributes = node.getAttributes();
					Node href = attributes.getNamedItem("href");
					String name = href.getNodeValue();
					// do not add includes statring by ../ as there are system
					// includes
					if (!name.startsWith("../")) {
						templateTreeObject = new TemplateTreeObject(viewer, "["
								+ name.substring(name.lastIndexOf('/') + 1,
										name.lastIndexOf('.')) + "]", name);
						treeObject.addChild(templateTreeObject);
					}
				}
			}
		} catch (CoreException e) {
			ConvertigoPlugin.logInfo("Error opening Ressources for project '"
					+ sheet.getProject().getName() + "': " + e.getMessage());
		} catch (Exception ee) {
			ConvertigoPlugin.logInfo("Error Parsing XSL file '" + xslFileName
					+ "': " + ee.getMessage());
		}
	}

	/**
	 * Parses as a DOM the IFile passed in argument ..
	 * 
	 * @param file
	 *            to parse
	 * @return parsed Document
	 */
	private Document parseXslFile(IFile file) throws Exception {
		Document doc;
		doc = XMLUtils.getDefaultDocumentBuilder().parse(
				new InputSource(file.getContents()));
		return doc;
	}

	// private void loadTrace(TreeParent parentTreeObject, File dir) {
	// FolderTreeObject folderTreeObject = new FolderTreeObject(viewer,
	// "Traces");
	// parentTreeObject.addChild(folderTreeObject);
	//
	// if (!dir.exists()) {
	// if (!dir.mkdir())
	// return;
	// }
	// File[] files = dir.listFiles(new FilenameFilter() {
	// public boolean accept(File dir, String name) {
	// if (new File(dir, name).isFile() && (name.endsWith(".etr"))) return true;
	// return false;
	// }
	// });
	// if (files == null) return;
	//
	// File file;
	// TraceTreeObject traceTreeObject;
	// for (int i = 0; i < files.length; i++) {
	// file = files[i];
	// traceTreeObject = new TraceTreeObject(viewer, file);
	// folderTreeObject.addChild(traceTreeObject);
	// }
	// }

	public TreeObject findTreeObjectByPath(TreeParent treeParent, String path) {
		TreeObject foundObject = null;

		if (treeParent == null)
			return null;

		String treeParentPath = treeParent.getPath();

		if (treeParentPath.length() > path.length())
			return null;

		if (treeParent instanceof DatabaseObjectTreeObject) {
			if (((DatabaseObjectTreeObject) treeParent).isInherited)
				return null;
		}

		if (treeParent instanceof PropertyTableTreeObject) {
			if (((PropertyTableTreeObject) treeParent).isInherited())
				return null;
		}

		if (treeParentPath.equals(path))
			return treeParent;

		for (TreeObject treeObject : treeParent.getChildren()) {
			if (treeObject instanceof TreeParent) {
				foundObject = findTreeObjectByPath((TreeParent) treeObject,
						path);
				if (foundObject != null)
					break;
			} else if (treeObject.getPath().equals(path)) {
				foundObject = treeObject;
				break;
			}
		}
		return foundObject;
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	public void close() {
		addedTreeObjects.clear();

		// close all opened editors
		closeAllProjects();

		// Remove all listeners
		clearSelectionChangedListeners();

		// Deregister as Engine listener
		if (Engine.theApp != null) {
			Engine.theApp.removeEngineListener(this);
			Engine.theApp.removeMigrationListener(this);
		}

		ConvertigoPlugin.projectManager.setProjectExplorerView(null);
	}

	private ListenerList selectionChangedListeners = new ListenerList();

	public synchronized void addSelectionChangedListener(
			ISelectionChangedListener listener) {
		selectionChangedListeners.add(listener);
		viewer.addSelectionChangedListener(listener);
	}

	public synchronized void removeSelectionChangedListener(
			ISelectionChangedListener listener) {
		selectionChangedListeners.remove(listener);
		viewer.removeSelectionChangedListener(listener);
	}

	public synchronized void clearSelectionChangedListeners() {
		Object[] listeners = selectionChangedListeners.getListeners();
		for (int i = 0; i < listeners.length; i++) {
			removeSelectionChangedListener((ISelectionChangedListener) listeners[i]);
		}
		selectionChangedListeners.clear();
	}

	// ******************************** HELPER METHODS FOR ACTIONS
	// **************************************//

	public void loadSelectedUnloadedProjectTreeObject() {
		// TreeObject treeObject = getFirstSelectedTreeObject();
		// if ((treeObject != null) && (treeObject instanceof
		// UnloadedProjectTreeObject))
		// loadProject((UnloadedProjectTreeObject)treeObject);
		TreeObject[] treeObjects = getSelectedTreeObjects();
		if ((treeObjects != null)) {
			for (TreeObject treeObject : treeObjects) {
				if (treeObject instanceof UnloadedProjectTreeObject) {
					loadProject((UnloadedProjectTreeObject) treeObject);
				}
			}
		}
	}

	public void removeProjectTreeObject(TreeObject treeObject) {
		if ((treeObject != null)
				&& ((treeObject instanceof ProjectTreeObject) || (treeObject instanceof UnloadedProjectTreeObject))) {
			TreeParent invisibleRoot = treeObject.getParent();
			if (treeObject instanceof ProjectTreeObject) {
				ProjectTreeObject projectTreeObject = (ProjectTreeObject) treeObject;
				projectTreeObject.closeAllEditors();
			}
			invisibleRoot.removeChild(treeObject);
			viewer.refresh();
		}
	}

	public void closeAllProjects() {
		ViewContentProvider provider = (ViewContentProvider) viewer
				.getContentProvider();
		if (provider != null) {
			Object[] objects = provider.getElements(getViewSite());
			for (int i = 0; i < objects.length; i++) {
				TreeObject treeObject = (TreeObject) objects[i];
				if (treeObject instanceof ProjectTreeObject) {
					ProjectTreeObject projectTreeObject = (ProjectTreeObject) treeObject;
					ConvertigoPlugin.projectManager
							.setCurrentProject(projectTreeObject);//
					projectTreeObject.close();// close all editors
				}
			}
		}
	}

	private boolean loadedProjectsContainsSequence() {
		ViewContentProvider provider = (ViewContentProvider) viewer
				.getContentProvider();
		if (provider != null) {
			Object[] objects = provider.getElements(getViewSite());
			for (int i = 0; i < objects.length; i++) {
				TreeObject treeObject = (TreeObject) objects[i];
				if (treeObject instanceof ProjectTreeObject) {
					ProjectTreeObject projectTreeObject = (ProjectTreeObject) treeObject;
					List<Sequence> vSequences = ((Project) projectTreeObject
							.getObject()).getSequencesList();
					int size = vSequences.size();
					if (size > 0) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public void unloadSelectedProjectTreeObject() {
		TreeObject[] treeObjects = getSelectedTreeObjects();
		if ((treeObjects != null)) {
			for (TreeObject treeObject : treeObjects) {
				if (treeObject instanceof ProjectTreeObject) {
					unloadProjectTreeObject((ProjectTreeObject) treeObject);
				}
			}
		}
	}

	protected UnloadedProjectTreeObject unloadProjectTreeObject(
			ProjectTreeObject projectTreeObject) {
		if (projectTreeObject == null)
			throw new IllegalArgumentException(
					"ProjectExplorerView.unloadProjectTreeObject(): project tree object can not be null!");
		String projectName = projectTreeObject.getName();
		TreeParent invisibleRoot = projectTreeObject.getParent();
		if (projectTreeObject.close()) {
			UnloadedProjectTreeObject unloadedProjectTreeObject = new UnloadedProjectTreeObject(
					viewer, projectName);
			invisibleRoot.addChild(unloadedProjectTreeObject);
			invisibleRoot.removeChild(projectTreeObject);
			viewer.refresh();

			try {
				ConvertigoPlugin.getDefault().closeProjectPluginResource(
						projectName);
			} catch (CoreException e) {
				ConvertigoPlugin.logException(
						e,
						"Unable to unload the project '"
								+ projectTreeObject.getName() + "'");
			}

			return unloadedProjectTreeObject;
		}
		return null;
	}

	public void importProjectTreeObject(String projectName)
			throws CoreException {
		importProjectTreeObject(projectName, false, null);
	}

	public void importProjectTreeObject(String projectName, boolean isCopy,
			String originalName) throws CoreException {
		TreeParent invisibleRoot = ((ViewContentProvider) viewer
				.getContentProvider()).getTreeRoot();
		UnloadedProjectTreeObject unloadedProjectTreeObject = new UnloadedProjectTreeObject(
				viewer, projectName);
		invisibleRoot.addChild(unloadedProjectTreeObject);
		ConvertigoPlugin.getDefault().createProjectPluginResource(projectName);
		loadProject(unloadedProjectTreeObject, isCopy, originalName);
	}

	public boolean isProjectLoaded(String projectName) {
		boolean bLoaded = false;
		TreeObject treeObject = getFirstSelectedTreeObject();
		if (treeObject != null) {
			TreeParent invisibleRoot = null;
			TreeObject treeParent = treeObject;
			while ((treeParent = treeParent.getParent()) != null)
				invisibleRoot = (TreeParent) treeParent;

			for (TreeObject child : invisibleRoot.getChildren()) {
				if (child.getName().equals(projectName)) {
					bLoaded = child instanceof ProjectTreeObject;
					break;
				}
			}

		}
		return bLoaded;
	}

	public void reloadDatabaseObject(DatabaseObject databaseObject)
			throws EngineException, IOException {
		DatabaseObjectTreeObject treeObject = findTreeObjectByUserObject(databaseObject);
		treeObject.hasBeenModified(databaseObject.hasChanged);
		reloadTreeObject(treeObject);
	}

	public void reloadFirstSelectedTreeObject() throws EngineException,
			IOException {
		TreeObject object = getFirstSelectedTreeObject();
		reloadTreeObject(object);
	}

	public void reloadTreeObject(TreeObject object) throws EngineException,
			IOException {
		reloadTreeObject(object, true);
	}

	public void reloadTreeObjectWithoutDynamicUpdate(TreeObject object)
			throws EngineException, IOException {
		reloadTreeObject(object, false);
	}

	protected void reloadTreeObject(TreeObject object,
			boolean bDynamicSchemaUpdate) throws EngineException, IOException {
		if (object != null)
			reload((TreeParent) object, (DatabaseObject) object.getObject(),
					bDynamicSchemaUpdate);
	}

	public void refreshProjects() {
		((ViewContentProvider) viewer.getContentProvider()).refreshProjects();
	}
	
	public void reloadProject(TreeObject projectTreeObject) {
		((ViewContentProvider) viewer.getContentProvider()).reloadProject(projectTreeObject);
	}
	
	public void refreshTree() {
		viewer.refresh();
	}

	public void refreshFirstSelectedTreeObject() {
		refreshFirstSelectedTreeObject(false);
	}

	public void refreshFirstSelectedTreeObject(boolean bRecurse) {
		TreeObject object = getFirstSelectedTreeObject();
		refreshTreeObject(object, bRecurse);
	}

	public void refreshTreeObject(TreeObject object) {
		ISelection selection = viewer.getSelection();
		refreshTreeObject(object, false);
		if (!selection.isEmpty())
			viewer.setSelection(selection, true);
	}

	public void refreshTreeObject(TreeObject object, boolean bRecurse) {
		if (object != null) {
			if (bRecurse && (object instanceof TreeParent))
				for (TreeObject child : ((TreeParent) object).getChildren())
					refreshTreeObject(child, true);
			object.update();
			viewer.refresh(object);
		}
	}

	public void updateFirstSelectedTreeObject() {
		TreeObject object = getFirstSelectedTreeObject();
		updateTreeObject(object);
	}

	public void updateTreeObject(TreeObject object) {
		if (object != null) {
			object.update();
			viewer.update(object, null);
		}
	}

	public void updateDatabaseObject(DatabaseObject databaseObject) {
		DatabaseObjectTreeObject treeObject = findTreeObjectByUserObject(databaseObject);
		updateTreeObject(treeObject);
	}

	public TreeParent getDatabaseObjectTreeParent(TreeObject treeObject) {
		TreeParent treeParent = null;
		if (treeObject != null) {
			treeParent = treeObject.getParent();
			while (!(treeParent instanceof DatabaseObjectTreeObject)
					&& (treeParent != null))
				treeParent = treeParent.getParent();
			if (treeParent == null)
				treeParent = (TreeParent) treeObject;
		}
		return treeParent;
	}

	public TreeObject getFirstSelectedTreeObject() {
		ISelection selection = viewer.getSelection();
		IStructuredSelection structuredSelection = (IStructuredSelection) selection;
		TreeObject selectedTreeObject = (TreeObject) structuredSelection
				.getFirstElement();
		return selectedTreeObject;
	}

	public DatabaseObjectTreeObject getFirstSelectedDatabaseObjectTreeObject(
			TreeObject selection) {
		while ((selection != null)
				&& !(selection instanceof DatabaseObjectTreeObject))
			selection = selection.getParent();
		return (DatabaseObjectTreeObject) selection;
	}

	/*
	 * public DatabaseObjectTreeObject
	 * getFirstSelectedDatabaseObjectTreeObject(){ TreeObject selection =
	 * getFirstSelectedTreeObject(); return
	 * getFirstSelectedDatabaseObjectTreeObject(selection); }
	 */

	public void setSelectedTreeObject(TreeObject object) {
		StructuredSelection structuredSelection = new StructuredSelection(
				object);
		viewer.setSelection(structuredSelection);
	}

	public TreeObject[] getSelectedTreeObjects() {
		TreeObject[] treeObjects = null;
		ISelection selection = viewer.getSelection();
		if (!selection.isEmpty()) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			Object[] treeArray = structuredSelection.toArray();
			treeObjects = new TreeObject[structuredSelection.size()];
			for (int i = 0; i < treeObjects.length; i++)
				treeObjects[i] = (TreeObject) treeArray[i];
		}
		return treeObjects;
	}

	public void setSelectedTreeObjects(TreeObject[] treeObjects) {
		if ((treeObjects != null) && (treeObjects.length > 0)) {
			StructuredSelection structuredSelection = new StructuredSelection(
					treeObjects);
			viewer.setSelection(structuredSelection);
		}
	}

	public Object[] getSelectedDatabaseObjects() {
		Object[] databaseObjects = null;
		TreeObject[] treeObjects = getSelectedTreeObjects();
		if (treeObjects != null) {
			int len = treeObjects.length;
			databaseObjects = new Object[len];
			for (int i = 0; i < len; i++)
				databaseObjects[i] = ((TreeObject) treeObjects[i]).getObject();
		}
		return databaseObjects;
	}

	public Object getFirstSelectedDatabaseObject() {
		Object object = null;
		TreeObject treeObject = getFirstSelectedTreeObject();
		if (treeObject != null)
			object = treeObject.getObject();
		return object;
	}

	public void renameSelectedTreeObject() {
		TreeObject treeObject = getFirstSelectedTreeObject();
		if ((treeObject != null)
				&& ((treeObject instanceof DatabaseObjectTreeObject) || (treeObject instanceof TraceTreeObject))) {
			edit(treeObject);
		}
	}

	private DatabaseObjectTreeObject findTreeObjectByUserObjectFromCache(
			DatabaseObject databaseObject) {
		DatabaseObjectTreeObject databaseObjectTreeObject = databaseObjectTreeObjectCache
				.get(databaseObject);
		if (databaseObjectTreeObject != null) {
			if (databaseObjectTreeObject.getObject().equals(databaseObject) && databaseObjectTreeObject.parent != null) {
				return databaseObjectTreeObject;
			} else {
				databaseObjectTreeObjectCache.remove(databaseObject);
			}
		}
		return null;
	}

	private DatabaseObjectTreeObject findTreeObjectByUserObject(
			DatabaseObject databaseObject, ProjectTreeObject projectTreeObject) {
		DatabaseObjectTreeObject databaseObjectTreeObject;
		if (projectTreeObject.getObject().equals(databaseObject)) {
			databaseObjectTreeObject = projectTreeObject;
		} else {
			DatabaseObject parentDatabaseObject = databaseObject.getParent();
			DatabaseObjectTreeObject parentDatabaseObjectTreeObject = findTreeObjectByUserObjectFromCache(parentDatabaseObject);
			if (parentDatabaseObjectTreeObject == null) {
				parentDatabaseObjectTreeObject = findTreeObjectByUserObject(
						parentDatabaseObject, projectTreeObject);
			}
			databaseObjectTreeObject = parentDatabaseObjectTreeObject
					.findDatabaseObjectTreeObjectChild(databaseObject);
		}
		databaseObjectTreeObjectCache.put(databaseObject,
				databaseObjectTreeObject);
		return databaseObjectTreeObject;
	}

	public DatabaseObjectTreeObject findTreeObjectByUserObject(
			DatabaseObject databaseObject) {
		DatabaseObjectTreeObject databaseObjectTreeObject = findTreeObjectByUserObjectFromCache(databaseObject);
		if (databaseObjectTreeObject != null) {
			return databaseObjectTreeObject;
		}
		boolean isProject = false;
		if (databaseObject != null) {
			Project databaseProject = null;
			if (databaseObject instanceof Project) {
				isProject = true;
				databaseProject = (Project) databaseObject;
			} else {
				databaseProject = databaseObject.getProject();
			}

			ViewContentProvider provider = (ViewContentProvider) viewer
					.getContentProvider();
			if (provider != null) {
				Object[] objects = provider.getElements(getViewSite());
				for (int i = 0; i < objects.length; i++) {
					TreeObject treeObject = (TreeObject) objects[i];
					if (treeObject instanceof ProjectTreeObject) {
						ProjectTreeObject projectTreeObject = (ProjectTreeObject) treeObject;
						Project project = projectTreeObject.getObject();
						if (project.getName().equals(databaseProject.getName())) {
							return isProject ? projectTreeObject
									: findTreeObjectByUserObject(
											databaseObject, projectTreeObject);
						}
					}
				}
			}
		}
		return null;
	}

	public DatabaseObjectTreeObject findTreeObjectByUserObjectQName(
			String databaseObjectQName) {
		DatabaseObjectTreeObject databaseTreeObject = null;
		ViewContentProvider provider = (ViewContentProvider) viewer
				.getContentProvider();
		if (provider != null) {
			Object[] objects = provider.getElements(getViewSite());
			for (int i = 0; i < objects.length; i++) {
				TreeObject treeObject = (TreeObject) objects[i];
				if (treeObject instanceof ProjectTreeObject) {
					Project project = (Project) treeObject.getObject();
					if (project.getQName().equals(databaseObjectQName)) {
						databaseTreeObject = (ProjectTreeObject) treeObject;
						break;
					} else {
						databaseTreeObject = (DatabaseObjectTreeObject) ((ProjectTreeObject) treeObject)
								.findTreeObjectByUserObjectQName(databaseObjectQName);
					}
				}
			}
		}
		return databaseTreeObject;
	}

	public static int getTreeObjectType(TreePath path) {
		TreeObject treeNode = (TreeObject) path.getLastPathComponent();
		return getTreeObjectType(treeNode);
	}

	public static int getTreeObjectType(TreeObject treeNode) {
		if (treeNode instanceof ObjectsFolderTreeObject) {
			int folderType = ((ObjectsFolderTreeObject) treeNode).folderType;

			if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_POOLS) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_POOLS;
			} else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_CONNECTORS) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_CONNECTORS;
			} else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_MOBILEDEVICES) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_MOBILEDEVICES;
			} else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_TRANSACTIONS) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_TRANSACTIONS;
			} else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_SHEETS) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_SHEETS;
			} else if ((folderType == ObjectsFolderTreeObject.FOLDER_TYPE_SCREEN_CLASSES)
					|| (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_INHERITED_SCREEN_CLASSES)) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_SCREEN_CLASSES;
			} else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_EXTRACTION_RULES) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_EXTRACTION_RULES;
			} else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_CRITERIAS) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_CRITERIAS;
			} else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_SEQUENCES) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_SEQUENCES;
			} else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_STEPS) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_STEPS;
			} else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_VARIABLES) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_VARIABLES;
			} else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_TESTCASES) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_TESTCASES;
			}
		} else if (treeNode instanceof HandlersDeclarationTreeObject) {
			return ProjectExplorerView.TREE_OBJECT_TYPE_HANDLERS_DECLARATION;
		} else if (treeNode instanceof VariableTreeObject) {
			return ProjectExplorerView.TREE_OBJECT_TYPE_VARIABLE;
		} else if (treeNode instanceof VariableTreeObject2) {
			return ProjectExplorerView.TREE_OBJECT_TYPE_VARIABLE;
		} else if (treeNode instanceof IPropertyTreeObject) {
			int result = 0;

			if (treeNode instanceof PropertyTableTreeObject) {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_PROPERTY_TABLE;
			} else if (treeNode instanceof PropertyTableRowTreeObject) {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_PROPERTY_TABLE_ROW;
			} else if (treeNode instanceof PropertyTableColumnTreeObject) {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_PROPERTY_TABLE_COLUMN;
			}

			if (((IPropertyTreeObject) treeNode).isInherited()) {
				result |= ProjectExplorerView.TREE_OBJECT_TYPE_DBO_INHERITED;
			}

			return result;
		} else if (treeNode instanceof DatabaseObjectTreeObject) {
			int result = 0;

			DatabaseObject databaseObject = (DatabaseObject) treeNode
					.getObject();

			if (databaseObject instanceof Project) {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_PROJECT;
			} else if (databaseObject instanceof Connector) {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_CONNECTOR;
			} else if (databaseObject instanceof Sequence) {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_SEQUENCE;
			} else if (databaseObject instanceof MobileDevice) {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_MOBILEDEVICE;
			} else if (databaseObject instanceof Criteria) {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_CRITERIA;
			} else if (databaseObject instanceof ExtractionRule) {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_EXTRACTION_RULE;
			} else if (databaseObject instanceof Pool) {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_POOL;
			} else if (databaseObject instanceof Transaction) {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_TRANSACTION;
			} else if (databaseObject instanceof BlockFactory) {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_BLOCK_FACTORY;
			} else if (databaseObject instanceof Sheet) {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_SHEET;
			} else if (databaseObject instanceof StatementWithExpressions) {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_STATEMENT_WITH_EXPRESSIONS;
				if (databaseObject instanceof FunctionStatement) {
					result = ProjectExplorerView.TREE_OBJECT_TYPE_FUNCTION;
				}
			} else if (databaseObject instanceof Statement) {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_STATEMENT;
			} else if (databaseObject instanceof StepWithExpressions) {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_STEP_WITH_EXPRESSIONS;
				if (databaseObject instanceof FunctionStep) {
					result = ProjectExplorerView.TREE_OBJECT_TYPE_FUNCTION;
				}
			} else if (databaseObject instanceof Step) {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_STEP;
			} else if (databaseObject instanceof TestCase) {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_TESTCASE;
			} else if (databaseObject instanceof ScreenClass) {
				if (databaseObject.getParent() instanceof Project) {
					result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_ROOT_SCREEN_CLASS;
				} else {
					result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_SCREEN_CLASS;
				}
			}

			if (((DatabaseObjectTreeObject) treeNode).isInherited) {
				result |= ProjectExplorerView.TREE_OBJECT_TYPE_DBO_INHERITED;
			}

			return result;
		}

		return ProjectExplorerView.TREE_OBJECT_TYPE_UNKNOWN;
	}

	public TreePath[] getSelectionPaths() {
		TreePath[] treePaths = null;
		TreeObject treeObject = null;
		TreePath treePath = null;

		TreeObject[] treeObjects = getSelectedTreeObjects();
		if (treeObjects != null) {
			int len = treeObjects.length;
			treePaths = new TreePath[len];
			for (int i = 0; i < len; i++) {
				treeObject = treeObjects[i];
				treePath = new TreePath(treeObject.getParents(true));
				if (treePath != null)
					treePaths[i] = treePath;
			}
		}

		return treePaths;
	}

	public TreePath getLeadSelectionPath() {
		TreePath lead = null;
		TreeObject treeObject = getFirstSelectedTreeObject();
		if (treeObject != null)
			lead = new TreePath(treeObject.getParents(true));
		return lead;
	}

	public UndoManager getUndoManager() {
		return undoManager;
	}

	public void updateUndoRedo() {
		((UndoAction) undoAction).update();
		((RedoAction) redoAction).update();
	}

	public void addUndoableEdit(UndoableEdit edit) {
		undoManager.addEdit(edit);
		updateUndoRedo();
	}

	public Project getProject(String projectName) throws EngineException {
		Project project = ((ViewContentProvider) viewer.getContentProvider())
				.getProject(projectName);
		return project;
	}

	public ProjectTreeObject getProjectRootObject(String projectName)
			throws EngineException {
		ProjectTreeObject project = (ProjectTreeObject) ((ViewContentProvider) viewer
				.getContentProvider()).getProjectRootObject(projectName);
		return project;
	}

	/**
	 * Gets the BeanInfo corresponding to the first selected dababase Object
	 * 
	 * @return BeanInfo
	 */
	public BeanInfo getFirstSelectedDatabaseObjectBeanInfo() {
		BeanInfo databaseObjectBeanInfo;

		Object obj = ConvertigoPlugin.getDefault().getProjectExplorerView()
				.getFirstSelectedDatabaseObject();
		if (obj == null)
			return null;

		DatabaseObject databaseObject;
		try {
			databaseObject = (DatabaseObject) obj;
		} catch (ClassCastException e) {
			return null;
		}

		try {
			String beanClassName = databaseObject.getClass().getName();
			Class<? extends DatabaseObject> beanClass = GenericUtils.cast(Class
					.forName(beanClassName));
			databaseObjectBeanInfo = CachedIntrospector.getBeanInfo(beanClass);
			return databaseObjectBeanInfo;
		} catch (Exception e) {
			String message = "Error while introspecting object "
					+ databaseObject.getName() + " ("
					+ databaseObject.getQName() + ")";
			ConvertigoPlugin.logException(e, message);
			return null;
		}
	}

	public void blocksChanged(EngineEvent engineEvent) {
	}

	private DatabaseObjectTreeObject lastDetectedDatabaseObjectTreeObject = null;
	private ScreenClassTreeObject lastDetectedScreenClassTreeObject = null;
	private ScreenClass lastDetectedScreenClass = null;

	public ScreenClassTreeObject getLastDetectedScreenClassTreeObject() {
		ScreenClassTreeObject screenClassTreeObject = lastDetectedScreenClassTreeObject;
		if (screenClassTreeObject == null) {
			if (lastDetectedScreenClass != null) {
				screenClassTreeObject = (ScreenClassTreeObject) findTreeObjectByUserObject(lastDetectedScreenClass);
			}
		}
		return screenClassTreeObject;
	}

	public ScreenClass getLastDetectedScreenClass() {
		return lastDetectedScreenClass;
	}

	public void objectDetected(EngineEvent engineEvent) {
		final Object source = engineEvent.getSource();
		boolean highlightDetectedObject = ConvertigoPlugin
				.getHighlightDetectedObject();
		if (highlightDetectedObject) {
			if (source instanceof DatabaseObject) {
				getSite().getShell().getDisplay().syncExec(new Runnable() {
					public void run() {
						DatabaseObjectTreeObject databaseTreeObject = findTreeObjectByUserObject((DatabaseObject) source);
						if (databaseTreeObject != null) {
							if (lastDetectedDatabaseObjectTreeObject != null) {
								lastDetectedDatabaseObjectTreeObject.isDetectedObject = false;
								updateTreeObject(lastDetectedDatabaseObjectTreeObject);
							}
							databaseTreeObject.isDetectedObject = true;
							updateTreeObject(databaseTreeObject);
							viewer.expandToLevel(databaseTreeObject, 0);
							lastDetectedDatabaseObjectTreeObject = databaseTreeObject;
							if (databaseTreeObject instanceof ScreenClassTreeObject) {
								lastDetectedScreenClass = (ScreenClass) source;
								lastDetectedScreenClassTreeObject = (ScreenClassTreeObject) databaseTreeObject;
							}
						}
					}
				});
			}
		} else {
			if (source instanceof ScreenClass) {
				lastDetectedScreenClass = (ScreenClass) source;
			}
		}
	}

	public void objectChanged(CompositeEvent compositeEvent) {
		final Object data = compositeEvent.data;
		final Object source = compositeEvent.getSource();
		if (source instanceof DatabaseObject) {
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					DatabaseObjectTreeObject databaseObjectTreeObject = findTreeObjectByUserObject((DatabaseObject) source);
					try {
						reloadTreeObject(databaseObjectTreeObject);

						if ((data != null) && (data instanceof String)) {
							// case of learned Javelin transaction, expand to
							// see newly added handlers
							if (databaseObjectTreeObject instanceof TransactionTreeObject) {
								viewer.expandToLevel(databaseObjectTreeObject,
										2);
							}

							// case of we need to select a treeObject given its
							// path
							TreeObject treeObjectToSelect = findTreeObjectByPath(
									databaseObjectTreeObject, (String) data);
							if (treeObjectToSelect != null) {
								viewer.expandToLevel(treeObjectToSelect, 0);
								setSelectedTreeObject(treeObjectToSelect);

								StructuredSelection structuredSelection = new StructuredSelection(
										treeObjectToSelect);
								ConvertigoPlugin
										.getDefault()
										.getPropertiesView()
										.selectionChanged(
												(IWorkbenchPart) ProjectExplorerView.this,
												structuredSelection);
							}
						}
					} catch (EngineException e) {
						ConvertigoPlugin
								.logException(e, "Unexpected exception");
					} catch (IOException e) {
						ConvertigoPlugin
								.logException(e, "Unexpected exception");
					}
				}
			});
		}
	}

	public void objectSelected(CompositeEvent compositeEvent) {
		final Object source = compositeEvent.getSource();
		if (source instanceof DatabaseObject) {
			getSite().getShell().getDisplay().syncExec(new Runnable() {
				public void run() {
					DatabaseObjectTreeObject databaseTreeObject = findTreeObjectByUserObject((DatabaseObject) source);
					if (databaseTreeObject != null) {
						viewer.expandToLevel(databaseTreeObject, 0);
						setSelectedTreeObject(databaseTreeObject);

						StructuredSelection structuredSelection = new StructuredSelection(
								databaseTreeObject);
						ConvertigoPlugin
								.getDefault()
								.getPropertiesView()
								.selectionChanged(
										(IWorkbenchPart) ProjectExplorerView.this,
										structuredSelection);
					}
				}
			});
		}
	}

	public void documentGenerated(EngineEvent engineEvent) {
		getSite().getShell().getDisplay().syncExec(new Runnable() {
			public void run() {
				if (lastDetectedDatabaseObjectTreeObject != null) {
					lastDetectedDatabaseObjectTreeObject.isDetectedObject = false;
					updateTreeObject(lastDetectedDatabaseObjectTreeObject);
				}
				lastDetectedDatabaseObjectTreeObject = null;
				lastDetectedScreenClassTreeObject = null;
			}
		});
	}

	public void stepReached(EngineEvent engineEvent) {
		objectDetected(engineEvent);
	}

	public void transactionStarted(EngineEvent engineEvent) {
	}

	public void transactionFinished(EngineEvent engineEvent) {
	}

	public void sequenceFinished(EngineEvent engineEvent) {
	}

	public void sequenceStarted(EngineEvent engineEvent) {
	}

	public void clearEditor(EngineEvent engineEvent) {
	}

	public void projectMigrated(EngineEvent engineEvent) {
		final String projectName = (String) engineEvent.getSource();
		if (projectName != null) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					try {
						ConvertigoPlugin
								.logDebug("[ProjectExplorerView] event 'projectMigrated' received for project "
										+ projectName);
						((ViewContentProvider) viewer.getContentProvider())
								.loadProject(projectName);
						viewer.refresh();
					} catch (Throwable t) {
						t.printStackTrace();
					}
				}
			});
		}
	}

	public void migrationFinished(EngineEvent engineEvent) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				try {
					ConvertigoPlugin
							.logDebug("[ProjectExplorerView] event 'migrationFinished' received");
					refreshProjects();
					refreshTree();
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		});
	}

	public boolean importProject(String filePath,
			ProjectTreeObject projectTreeObject) throws EngineException,
			IOException, CoreException {
		return importProject(filePath, projectTreeObject.getName());
	}

	public boolean importProject(String filePath, String targetProjectName)
			throws EngineException, IOException, CoreException {
		return importProject(filePath, targetProjectName, false);
	}

	public boolean importProject(String filePath, String targetProjectName,
			boolean reload) throws EngineException, IOException, CoreException {
		TreeObject projectTreeObject = null;
		if (targetProjectName != null) {
			projectTreeObject = ((ViewContentProvider) viewer
					.getContentProvider())
					.getProjectRootObject(targetProjectName);
		}

		// if project already exists, backup it and delete it after
		if (projectTreeObject != null) {
			if (filePath.endsWith(".xml")) {
				DatabaseObjectsManager.deleteDir(new File(Engine.PROJECTS_PATH
						+ "/" + targetProjectName + "/_data"));
				DatabaseObjectsManager.deleteDir(new File(Engine.PROJECTS_PATH
						+ "/" + targetProjectName + "/_private"));
			}
			if (!reload) {
				// delete project resource (but not content)
				ConvertigoPlugin.getDefault().deleteProjectPluginResource(
						false, targetProjectName);
			}
		}

		ConvertigoPlugin.logInfo("Import project from file \"" + filePath
				+ "\"");

		Project importedProject = null;
		if (filePath.endsWith(".xml")) {
			importedProject = Engine.theApp.databaseObjectsManager
					.importProject(filePath);
		} else if (filePath.endsWith(".car") && (targetProjectName != null)) {
			importedProject = Engine.theApp.databaseObjectsManager
					.deployProject(filePath, targetProjectName, true);
		}

		if (importedProject != null) {
			// project's name may have been changed because of non-normalized
			// name (fix ticket #788 : Can not import project 213.car)
			targetProjectName = importedProject.getName();

			// loads project into tree view
			if (projectTreeObject == null) {
				importProjectTreeObject(targetProjectName);
			} else {
				// recreate project resource
				ConvertigoPlugin.getDefault().getProjectPluginResource(targetProjectName);
				reloadProject(projectTreeObject);
			}

			refreshTree();
			return true;
		}
		return false;
	}
}