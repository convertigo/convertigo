/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import java.beans.BeanInfo;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.swing.event.EventListenerList;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
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
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
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
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.navigator.resources.ProjectExplorer;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.services.IEvaluationService;
import org.eclipse.ui.wizards.IWizardCategory;
import org.eclipse.ui.wizards.IWizardDescriptor;
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
import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.MobilePlatform;
import com.twinsoft.convertigo.beans.core.Pool;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Reference;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Sheet;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.core.StatementWithExpressions;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.UrlAuthentication;
import com.twinsoft.convertigo.beans.core.UrlMapper;
import com.twinsoft.convertigo.beans.core.UrlMapping;
import com.twinsoft.convertigo.beans.core.UrlMappingOperation;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter;
import com.twinsoft.convertigo.beans.core.UrlMappingResponse;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.couchdb.DesignDocument;
import com.twinsoft.convertigo.beans.references.ProjectSchemaReference;
import com.twinsoft.convertigo.beans.statements.FunctionStatement;
import com.twinsoft.convertigo.beans.statements.HandlerStatement;
import com.twinsoft.convertigo.beans.steps.FunctionStep;
import com.twinsoft.convertigo.beans.transactions.JavelinTransaction;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.beans.variables.StepVariable;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.actions.ProjectExplorerSaveAllAction;
import com.twinsoft.convertigo.eclipse.dialogs.ButtonSpec;
import com.twinsoft.convertigo.eclipse.dialogs.CustomDialog;
import com.twinsoft.convertigo.eclipse.dnd.MobileSourceTransfer;
import com.twinsoft.convertigo.eclipse.dnd.PaletteSourceTransfer;
import com.twinsoft.convertigo.eclipse.dnd.StepSourceTransfer;
import com.twinsoft.convertigo.eclipse.dnd.TreeDragListener;
import com.twinsoft.convertigo.eclipse.dnd.TreeDropAdapter;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.editors.CompositeListener;
import com.twinsoft.convertigo.eclipse.editors.StartupEditor;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditorInput;
import com.twinsoft.convertigo.eclipse.popup.actions.ClipboardCopyAction;
import com.twinsoft.convertigo.eclipse.popup.actions.ClipboardCutAction;
import com.twinsoft.convertigo.eclipse.popup.actions.ClipboardPasteAction;
import com.twinsoft.convertigo.eclipse.popup.actions.DatabaseObjectDecreasePriorityAction;
import com.twinsoft.convertigo.eclipse.popup.actions.DatabaseObjectDeleteAction;
import com.twinsoft.convertigo.eclipse.popup.actions.DatabaseObjectIncreasePriorityAction;
import com.twinsoft.convertigo.eclipse.popup.actions.DeletePropertyTableColumnAction;
import com.twinsoft.convertigo.eclipse.popup.actions.DeletePropertyTableRowAction;
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
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ConnectorTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.CriteriaTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DesignDocumentFilterTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DesignDocumentFunctionTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DesignDocumentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DesignDocumentUpdateTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DesignDocumentValidateTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DesignDocumentViewTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DocumentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ExtractionRuleTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.FullSyncListenerTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.HandlersDeclarationTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.IClosableTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.IDesignTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.IEditableTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.IPropertyTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ListenerTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.MobileApplicationComponentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.MobileApplicationTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.MobilePageComponentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.MobilePlatformTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.MobileRouteActionComponentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.MobileRouteComponentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.MobileRouteEventComponentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.MobileUIComponentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.NgxApplicationComponentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.NgxPageComponentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.NgxUIComponentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ObjectsFolderTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.PropertyTableColumnTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.PropertyTableRowTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.PropertyTableTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ReferenceTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ResourceTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ScreenClassTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.SequenceTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.SheetTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.StatementTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.StepTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TemplateTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TestCaseTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TraceTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TransactionTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UnloadedProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UrlAuthenticationTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UrlMapperTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UrlMappingOperationTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UrlMappingParameterTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UrlMappingResponseTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UrlMappingTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.VariableTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.VariableTreeObject2;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.XMLRecordDescriptionTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.XMLTableDescriptionTreeObject;
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
import com.twinsoft.convertigo.engine.enums.RequestAttribute;
import com.twinsoft.convertigo.engine.helpers.BatchOperationHelper;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.ProjectUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class ProjectExplorerView extends ViewPart implements ObjectsProvider, CompositeListener, EngineListener, MigrationListener {

	public static final int TREE_OBJECT_TYPE_UNKNOWN = 0;

	public static final int TREE_OBJECT_TYPE_DBO = 0x100;						// 0000 0001 0000 0000

	public static final int TREE_OBJECT_TYPE_DBO_PROJECT = 0x101;				// 0000 0001 0000 0001
	public static final int TREE_OBJECT_TYPE_DBO_POOL = 0x102;					// 0000 0001 0000 0010
	public static final int TREE_OBJECT_TYPE_DBO_TRANSACTION = 0x103;			// 0000 0001 0000 0011
	public static final int TREE_OBJECT_TYPE_DBO_SHEET = 0x104;					// 0000 0001 0000 0100
	public static final int TREE_OBJECT_TYPE_DBO_ROOT_SCREEN_CLASS = 0x105;		// 0000 0001 0000 0101
	public static final int TREE_OBJECT_TYPE_DBO_SCREEN_CLASS = 0x106;			// 0000 0001 0000 0110
	public static final int TREE_OBJECT_TYPE_DBO_BLOCK_FACTORY = 0x107;			// 0000 0001 0000 0111
	public static final int TREE_OBJECT_TYPE_DBO_CRITERIA = 0x108;				// 0000 0001 0000 1000
	public static final int TREE_OBJECT_TYPE_DBO_EXTRACTION_RULE = 0x109;		// 0000 0001 0000 1001
	public static final int TREE_OBJECT_TYPE_DBO_CONNECTOR = 0x10A;				// 0000 0001 0000 1010
	public static final int TREE_OBJECT_TYPE_DBO_STATEMENT = 0x10B;
	public static final int TREE_OBJECT_TYPE_DBO_STATEMENT_WITH_EXPRESSIONS = 0x10C;
	public static final int TREE_OBJECT_TYPE_DBO_STEP = 0x10D;
	public static final int TREE_OBJECT_TYPE_DBO_STEP_WITH_EXPRESSIONS = 0x10E;
	public static final int TREE_OBJECT_TYPE_DBO_SEQUENCE = 0x10F;
	public static final int TREE_OBJECT_TYPE_DBO_TESTCASE = 0x110;
	public static final int TREE_OBJECT_TYPE_DBO_MOBILEAPPLICATION = 0x112;
	public static final int TREE_OBJECT_TYPE_DBO_MOBILEPLATFORM = 0x111;
	public static final int TREE_OBJECT_TYPE_DBO_DOCUMENT = 0x113;
	public static final int TREE_OBJECT_TYPE_DBO_LISTENER = 0x114;
	public static final int TREE_OBJECT_TYPE_DBO_URLMAPPER = 0x115;
	public static final int TREE_OBJECT_TYPE_DBO_URLMAPPING = 0x116;
	public static final int TREE_OBJECT_TYPE_DBO_URLMAPPINGOPERATION = 0x117;
	public static final int TREE_OBJECT_TYPE_DBO_URLMAPPINGPARAMETER = 0x118;
	public static final int TREE_OBJECT_TYPE_DBO_URLMAPPINGRESPONSE = 0x119;

	public static final int TREE_OBJECT_TYPE_DBO_MOBILE_APPLICATIONCOMPONENT = 0x11A;
	public static final int TREE_OBJECT_TYPE_DBO_MOBILE_PAGECOMPONENT = 0x11B;
	public static final int TREE_OBJECT_TYPE_DBO_MOBILE_UICOMPONENT = 0x11C;
	public static final int TREE_OBJECT_TYPE_DBO_MOBILE_ROUTECOMPONENT = 0x11D;
	public static final int TREE_OBJECT_TYPE_DBO_MOBILE_ROUTEEVENTCOMPONENT = 0x11E;
	public static final int TREE_OBJECT_TYPE_DBO_MOBILE_ROUTEACTIONCOMPONENT = 0x11F;

	public static final int TREE_OBJECT_TYPE_DBO_REFERENCE = 0x120;
	public static final int TREE_OBJECT_TYPE_DBO_VARIABLE = 0x121;
	public static final int TREE_OBJECT_TYPE_DBO_INDEX = 0x122;
	public static final int TREE_OBJECT_TYPE_DBO_MB_MENU = 0x123;
	public static final int TREE_OBJECT_TYPE_DBO_MB_STYLE = 0x124;

	public static final int TREE_OBJECT_TYPE_DBO_PROPERTY_TABLE = 0x300;
	public static final int TREE_OBJECT_TYPE_DBO_PROPERTY_TABLE_ROW = 0x301;
	public static final int TREE_OBJECT_TYPE_DBO_PROPERTY_TABLE_COLUMN = 0x302;

	public static final int TREE_OBJECT_TYPE_DBO_INHERITED = 0x400;				// 0000 0100 0000 0000

	public static final int TREE_OBJECT_TYPE_FOLDER = 0x200;					// 0000 0010 0000 0000

	public static final int TREE_OBJECT_TYPE_FOLDER_POOLS = 0x201;				// 0000 0010 0000 0001
	public static final int TREE_OBJECT_TYPE_FOLDER_TRANSACTIONS = 0x202;		// 0000 0010 0000 0010
	public static final int TREE_OBJECT_TYPE_FOLDER_SHEETS = 0x203;				// 0000 0010 0000 0011
	public static final int TREE_OBJECT_TYPE_FOLDER_SCREEN_CLASSES = 0x204; 	// 0000 0010 0000 0100
	public static final int TREE_OBJECT_TYPE_FOLDER_CRITERIAS = 0x205;			// 0000 0010 0000 0101
	public static final int TREE_OBJECT_TYPE_FOLDER_EXTRACTION_RULES = 0x206;	// 0000 0010 0000 0110
	public static final int TREE_OBJECT_TYPE_FOLDER_CONNECTORS = 0x207;
	public static final int TREE_OBJECT_TYPE_FOLDER_SEQUENCES = 0x208;
	public static final int TREE_OBJECT_TYPE_FOLDER_STEPS = 0x209;
	public static final int TREE_OBJECT_TYPE_FOLDER_VARIABLES = 0x20A;
	public static final int TREE_OBJECT_TYPE_FOLDER_TESTCASES = 0x20B;
	public static final int TREE_OBJECT_TYPE_FOLDER_MOBILEPLATFORMS = 0x20C;
	public static final int TREE_OBJECT_TYPE_FOLDER_DOCUMENTS = 0x20D;
	public static final int TREE_OBJECT_TYPE_FOLDER_LISTENERS = 0x20E;
	public static final int TREE_OBJECT_TYPE_FOLDER_MAPPINGS = 0x20F;
	public static final int TREE_OBJECT_TYPE_FOLDER_OPERATIONS = 0x210;
	public static final int TREE_OBJECT_TYPE_FOLDER_PARAMETERS = 0x211;
	public static final int TREE_OBJECT_TYPE_FOLDER_EVENTS = 0x212;
	public static final int TREE_OBJECT_TYPE_FOLDER_ROUTES = 0x213;
	public static final int TREE_OBJECT_TYPE_FOLDER_ACTIONS = 0x214;
	public static final int TREE_OBJECT_TYPE_FOLDER_CONTROLS = 0x215;
	public static final int TREE_OBJECT_TYPE_FOLDER_SOURCES = 0x216;
	public static final int TREE_OBJECT_TYPE_FOLDER_STYLES = 0x217;
	public static final int TREE_OBJECT_TYPE_FOLDER_ATTRIBUTES = 0x218;
	public static final int TREE_OBJECT_TYPE_FOLDER_VALIDATORS = 0x219;
	public static final int TREE_OBJECT_TYPE_FOLDER_MENUS = 0x21A;
	public static final int TREE_OBJECT_TYPE_FOLDER_AUTHENTICATIONS = 0x21B;
	public static final int TREE_OBJECT_TYPE_FOLDER_INDEXES = 0x21C;

	public static final int TREE_OBJECT_TYPE_MISC = 0x8000;						// 1000 0000 0000 0000

	public static final int TREE_OBJECT_TYPE_HANDLERS_DECLARATION = 0x8001;		// 1000 0000 0000 0001
	public static final int TREE_OBJECT_TYPE_VARIABLE = 0x8002;					// 1000 0000 0000 0010
	public static final int TREE_OBJECT_TYPE_FUNCTION = 0x8003;


	public TreeViewer viewer;

	//	private DrillDownAdapter drillDownAdapter;

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
	private Action showStepInPickerAction;

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

		// Set view reference to this new instance (if view is closed and reopened)
		ConvertigoPlugin.projectManager.setProjectExplorerView(this);
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	private void packColumns() {
		ConvertigoPlugin.asyncExec(() -> {
			for (TreeColumn tc : viewer.getTree().getColumns()) {
				try {
					tc.pack();
				} catch (Exception e) {
				}
			}
		});
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	@SuppressWarnings("deprecation")
	public void createPartControl(Composite parent) {
		parent.setLayout(GridLayoutFactory.fillDefaults().margins(0, 0).spacing(0, 0).create());
		viewContentProvider = new ViewContentProvider(this);

		Composite stack = new Composite(parent, SWT.NONE);
		stack.setLayoutData(new GridData(GridData.FILL_BOTH));
		StackLayout stackLayout = new StackLayout();
		stack.setLayout(stackLayout);

		Composite rateUsBar = new Composite(parent, SWT.NONE);
		rateUsBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		rateUsBar.setLayout(GridLayoutFactory.fillDefaults().numColumns(5).margins(0, 0).spacing(2, 2).create());

		String[][] rateDef = {
				{"2", "Star us on GitHub", "icons/studio/github-16.png", "https://github.com/convertigo/convertigo"},
				{"1", null, "icons/studio/twitter-16.png", "https://twitter.com/convertigo"},
				{"1", null, "icons/studio/linkedin-16.png", "https://www.linkedin.com/company/convertigo/"},
				{null, null, "icons/studio/close-16.png", null}
		};

		for (String[] def: rateDef) {
			try {
				Button rateUsButton = new Button(rateUsBar, SWT.PUSH);
				rateUsButton.setData("style", "background-color: rgb(0, 200, 247); color: white");
				if (def[1] != null) {
					rateUsButton.setText(def[1]);
				}
				GridDataFactory gdf = GridDataFactory.fillDefaults();
				if (def[0] != null) {
					gdf.grab(true, false).span(Integer.parseInt(def[0]), 1);
				}
				rateUsButton.setLayoutData(gdf.create());
				rateUsButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						if (def[3] != null) {
							Program.launch(def[3]);
						} else {
							rateUsBar.dispose();
							parent.layout(true);
						}
					}
				});
				rateUsButton.setImage(ConvertigoPlugin.getDefault().getStudioIcon(def[2]));
			} catch (Exception e2) {
			}
		}

		Label noEngine = new Label(stack, SWT.CENTER);
		noEngine.setText("\n"
				+ "Convertigo Studio isn't completely installed,\n"
				+ "you have to complete the registration before\n"
				+ "starting building your projects.\n\n"
				+ "Please click here to register.");
		noEngine.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseDown(MouseEvent e) {
				ConvertigoPlugin.getDefault().runSetup();
			}
		});

		Composite noProject = new Composite(stack, SWT.NONE);
		noProject.setLayout(new GridLayout(1, false));

		ToolBar toolbar = new ToolBar(noProject, SWT.VERTICAL | SWT.FLAT);
		toolbar.setLayoutData(new GridData(GridData.CENTER, GridData.CENTER, true, true));

		String[][] defs = {
				{"Start Low Code FullStack Web/Desktop or Mobile app project", "web_color_32x32.png", "NewNgxBuilderWizard"},
				{"Start Low Code Back End project", "sequence_color_32x32.gif", "NewSequencerWizard"},
				{"Start Hello World sample project", "panel_color_32x32.gif", "NewSampleHelloWorldWizard"},
				{"Start another type of project", "convertigo_logo_32x32.png", null}
		};

		SelectionListener listener = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				ICommandService commandService = (ICommandService) PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getService(ICommandService.class);

				IEvaluationService evaluationService = (IEvaluationService) PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getService(IEvaluationService.class);
				try {

					for (IWizardDescriptor c : PlatformUI.getWorkbench().getNewWizardRegistry().getRootCategory().getWizards()) {
						System.out.println(c.getId() + ": " + c.getLabel() + " " + c.getCategory().getId());
					}

					for (IWizardCategory c: PlatformUI.getWorkbench().getNewWizardRegistry().getRootCategory().getCategories()) {
						System.out.println(c.getId() + ": " + c.getLabel() + " " + Arrays.toString(c.getWizards()));
					}

					Command c = commandService.getCommand("org.eclipse.ui.newWizard");

					Map<String, String> params = new HashMap<String, String>();
					params.put("newWizardId", (String) e.widget.getData("newWizardId"));

					c.executeWithChecks(new ExecutionEvent(c, params, null, evaluationService.getCurrentState()));

				} catch (Exception ex) {
					throw new RuntimeException("Open new wizard command not found");
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		};

		for (int i = 0; i < defs.length; i++) {
			ToolItem btn = new ToolItem(toolbar, SWT.PUSH);
			btn.setText("     " + defs[i][0] + "     ");
			try {
				btn.setImage(ConvertigoPlugin.getDefault().getStudioIcon("icons/studio/" + defs[i][1]));
			} catch (IOException e1) {
			}
			if (defs[i][2] != null) {
				btn.setData("newWizardId", "com.twinsoft.convertigo.eclipse.wizards." + defs[i][2]);
			}
			btn.addSelectionListener(listener);
		}

		viewer = new TreeViewer(stack,  SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION) {
			@Override
			public void refresh(Object element) {
				if (!Engine.isStarted) {
					return;
				}
				if (Engine.objectsProvider != null) {
					ConvertigoPlugin.asyncExec(() -> super.refresh(element));
					packColumns();
				}

				if (viewContentProvider.getTreeRoot() != null && viewContentProvider.getTreeRoot().hasChildren()) {
					if (stackLayout.topControl != getTree()) {
						stackLayout.topControl = getTree();
						stack.layout(true);
					}
				} else {
					if (stackLayout.topControl != noProject) {
						stackLayout.topControl = noProject;
						stack.layout(true);
					}
				}
			}

			@Override
			public void update(Object element, String[] properties) {
				super.update(element, properties);
				packColumns();
			}
		};
		stackLayout.topControl = noEngine;
		viewer.setData(ProjectExplorerView.class.getCanonicalName(), this);
		viewer.setContentProvider(viewContentProvider);
		viewer.addSelectionChangedListener((event) -> {
			packColumns();
		});

		// DND support
		int ops = DND.DROP_COPY | DND.DROP_MOVE ;
		Transfer[] dragtfs = new Transfer[] {TextTransfer.getInstance()};
		Transfer[] droptfs = new Transfer[] {TextTransfer.getInstance(), StepSourceTransfer.getInstance(), PaletteSourceTransfer.getInstance(), MobileSourceTransfer.getInstance()};
		viewer.addDragSupport(ops, dragtfs, new TreeDragListener(viewer));
		viewer.addDropSupport(ops, droptfs, new TreeDropAdapter(viewer));

		viewer.addTreeListener(new ITreeViewerListener() {

			@Override
			public void treeExpanded(TreeExpansionEvent event) {
				packColumns();
			}

			@Override
			public void treeCollapsed(TreeExpansionEvent event) {
				packColumns();
			}
		});
		viewer.setSorter(new TreeObjectSorter());
		viewer.setInput(getViewSite());

		TreeViewerColumn treeViewerColumn = new TreeViewerColumn(viewer, SWT.LEFT);

		ILabelProvider lp = new ViewLabelProvider();
		ILabelDecorator ld = PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator();

		treeViewerColumn.setLabelProvider(new DecoratingColumnLabelProvider(lp, ld));

		treeViewerColumn = new TreeViewerColumn(viewer, SWT.LEFT);
		treeViewerColumn.setLabelProvider(new CommentColumnLabelProvider());
		treeViewerColumn.setEditingSupport(new CommentEditingSupport(viewer));

		//drillDownAdapter = new DrillDownAdapter(viewer);

		makeActions();
		hookContextMenu();
		hookSelectionChangedEvent();
		hookDoubleClickAction();
		hookGlobalActions();
		hookKeyboardActions();
		contributeToActionBars();

		getSite().setSelectionProvider(viewer);

		ConvertigoPlugin.runAtStartup(() -> initialize());
	}

	public void initialize() {
		if (Engine.objectsProvider != this) {
			IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			if (activePage != null) {
				IEditorReference[] editorRefs = activePage.getEditorReferences();
				for (int i = 0; i < editorRefs.length; i++) {
					IEditorReference editorRef = (IEditorReference) editorRefs[i];
					IEditorPart editor = editorRef.getEditor(false);
					if (!(editor instanceof StartupEditor)) {
						activePage.closeEditor(editor, false);
					}
				}
			}

			// Studio mode
			Engine.setObjectsProvider(this);

			// Loads projects
			if (Engine.isStarted) {
				((ViewContentProvider) viewer.getContentProvider()).loadProjects();
				viewer.refresh();
			}

			// Registering as Engine listener
			if (Engine.theApp != null) {
				Engine.theApp.addEngineListener(this);
				Engine.theApp.addMigrationListener(this);
			}
		}
		try {
			boolean bHide = "true".equals(ConvertigoPlugin.getProperty(ConvertigoPlugin.PREFERENCE_HIDE_LIB_PROJECTS));
			ActionContributionItem item = (ActionContributionItem) getViewSite().getActionBars().getToolBarManager().find("com.twinsoft.convertigo.eclipse.views.projectexplorer.viewContribution.toggleLibs");
			if (item != null) {
				item.getAction().setChecked(bHide);
			}
		} catch (Exception e) {
			e.printStackTrace();
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
		menuMgr.setOverrides(new ConvertigoContributionManager());
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
		//boolean bAlt = (event.stateMask & SWT.ALT) != 0;
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
					deleteDatabaseObjectAction.runWithEvent(null);
				} else if (object instanceof PropertyTableRowTreeObject) {
					deletePropertyTableRowAction.run();
				} else if (object instanceof PropertyTableColumnTreeObject) {
					deletePropertyTableColumnAction.run();
				}
			}
		}

		if (bCtrl) {
			// Copy/Cut/Paste
			if (c == 'c') {
				copyAction.runWithEvent(null);
			}
			if (c == 'x') {
				cutAction.runWithEvent(null);
			}
			if (c == 'v') {
				pasteAction.runWithEvent(null);
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
			increasePriorityAction.runWithEvent(null);
		}
		if ((c == '-') || (keyCode == SWT.KEYPAD_SUBTRACT)) {
			decreasePriorityAction.runWithEvent(null);
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
		//statusLine.setMessage("Convertigo Studio "+ com.twinsoft.convertigo.engine.Version.fullProductVersion + " - " + analyzeKey());
		statusLine.setMessage("Convertigo EMS Studio "+ com.twinsoft.convertigo.engine.Version.fullProductVersion);
	}

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
				IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
				TreeObject treeObject = (TreeObject) selection.getFirstElement();

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
				} else if (treeObject instanceof ResourceTreeObject) {
					try {
						IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
						IDE.openEditor(page, (IFile) treeObject.getObject());
					} catch (Exception e) {
						e.printStackTrace();
					}
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
		showStepInPickerAction = new ShowStepInPickerAction();
	}

	private EventListenerList treeObjectListeners = new EventListenerList();

	public void addTreeObjectListener(TreeObjectListener treeObjectListener) {
		treeObjectListeners.add(TreeObjectListener.class, treeObjectListener);
	}

	public void removeTreeObjectListener(TreeObjectListener treeObjectListener) {
		treeObjectListeners.remove(TreeObjectListener.class, treeObjectListener);
	}

	public void fireTreeObjectPropertyChanged(TreeObjectEvent treeObjectEvent) {
		// Guaranteed to return a non-null array
		Object[] listeners = treeObjectListeners.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2 ; i >= 0 ; i-=2) {
			if (listeners[i] == TreeObjectListener.class) {
				try {
					((TreeObjectListener) listeners[i+1]).treeObjectPropertyChanged(treeObjectEvent);
				} catch (Exception e){
					String message = "fireTreeObjectPropertyChanged failed for treeObject: " + ((TreeObject) listeners[i+1]).getName();
					ConvertigoPlugin.logException(e, message, false);
				};
			}
		}
	}

	public List<TreeObject> addedTreeObjects = new ArrayList<TreeObject>();

	public void fireTreeObjectAdded(TreeObjectEvent treeObjectEvent) {
		// Guaranteed to return a non-null array
		Object[] listeners = treeObjectListeners.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2 ; i >= 0 ; i-=2) {
			if (listeners[i] == TreeObjectListener.class) {
				try {
					((TreeObjectListener) listeners[i+1]).treeObjectAdded(treeObjectEvent);
				} catch (Exception e){
					String message = "fireTreeObjectAdded failed for treeObject: " + ((TreeObject) listeners[i+1]).getName();
					ConvertigoPlugin.logException(e, message, false);
				};
			}
		}

		DatabaseObjectTreeObject treeObject = (DatabaseObjectTreeObject) treeObjectEvent.getSource();
		DatabaseObject databaseObject = (DatabaseObject) treeObject.getObject();

		// Case of Project added
		if (databaseObject instanceof Project) {
			ProjectTreeObject projectTreeObject = (ProjectTreeObject) treeObject;

			ConvertigoPlugin.logInfo("Loaded project "+ Project.formatNameWithHash((Project)databaseObject));
			Engine.logStudio.info("[treeview] Loaded project "+ Project.formatNameWithHash((Project)databaseObject));

			// Case of project copy : update references in steps if needed
			if (treeObjectEvent.oldValue != null) {
				String oldName = (String) treeObjectEvent.oldValue;
				String newName = (String) treeObjectEvent.newValue;
				boolean updateReferences = false;
				int update = 0;
				if (loadedProjectsHaveReferences()) {
					Shell shell = Display.getDefault().getActiveShell();
					CustomDialog customDialog = new CustomDialog(
							shell,
							"Update object references",
							"Do you want to update "
									+ "project"
									+ " references ?\n You can replace '"
									+ oldName
									+ "' by '"
									+ newName
									+ "' in all loaded projects \n or replace '"
									+ oldName
									+ "' by '"
									+ newName
									+ "' in current project only.",
									670, 200,
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

				if (updateReferences) {
					treeObjectEvent.update = update;
					fireTreeObjectPropertyChanged(treeObjectEvent);
					projectTreeObject.save(false);
				}
			}

			projectTreeObject.checkMissingProjects();
		}
	}

	public void fireTreeObjectRemoved(TreeObjectEvent treeObjectEvent) {
		// Prevents removed object to receive events
		if (treeObjectEvent.getSource() instanceof TreeObjectListener) {
			removeTreeObjectListener((TreeObjectListener) treeObjectEvent.getSource());
		}

		// Guaranteed to return a non-null array
		Object[] listeners = treeObjectListeners.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2 ; i >= 0 ; i-=2) {
			if (listeners[i] == TreeObjectListener.class) {
				try {
					((TreeObjectListener) listeners[i + 1]).treeObjectRemoved(treeObjectEvent);
				} catch (Exception e){
					String message = "fireTreeObjectRemoved failed for treeObject: " + ((TreeObject)listeners[i + 1]).getName();
					ConvertigoPlugin.logException(e, message);
				};
			}
		}
	}

	public IEditorPart getConnectorEditor(Connector connector) {
		IEditorPart editorPart = null;
		IWorkbenchPage activePage = PlatformUI
				.getWorkbench()
				.getActiveWorkbenchWindow()
				.getActivePage();

		if (activePage != null) {
			if (connector != null) {
				IEditorReference[] editorRefs = activePage.getEditorReferences();
				for (int i = 0; i < editorRefs.length; i++) {
					IEditorReference editorRef = (IEditorReference) editorRefs[i];
					try {
						IEditorInput editorInput = editorRef.getEditorInput();
						if ((editorInput != null) && (editorInput instanceof ConnectorEditorInput)) {
							if (((ConnectorEditorInput) editorInput).is(connector)) {
								editorPart = editorRef.getEditor(false);
								break;
							}
						}
					} catch(PartInitException e) {
						//ConvertigoPlugin.logException(e, "Error while retrieving the connector editor '" + editorRef.getName() + "'");
					}
				}
			}
		}
		return editorPart;
	}

	//private TreeObject oldSelection = null;
	TreeItem lastItem[] = new TreeItem[0];

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				//oldSelection = null;
				doubleClickAction.run();
			}
		});
	}

	private Set<TreeObject> reduceWithCommonParents(Set<TreeObject> items) {
		Map<TreeObject, TreeObject> parents = new HashMap<TreeObject, TreeObject>();
		Set<TreeObject> newSet = new HashSet<TreeObject>();
		boolean addParent = false;
		for (TreeObject item: items) {
			TreeObject parent = item.getParent();
			if (parent != null && !newSet.contains(parent)) {
				if (parents.containsKey(parent)) {
					newSet.add(parent);
					newSet.remove(parents.get(parent));
					addParent = true;
				} else {
					parents.put(parent, item);
					newSet.add(item);
				}
			}
		}

		if (addParent) {
			newSet = reduceWithCommonParents(newSet);
		}

		return newSet;
	}

	private void hookSelectionChangedEvent() {
		addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				TreeObject treeObject = (TreeObject) selection.getFirstElement();

				if (treeObject != null) {
					// remember current project
					ProjectTreeObject projectTreeObject = null;
					if (treeObject instanceof ProjectTreeObject) {
						projectTreeObject = (ProjectTreeObject) treeObject;
					} else {
						projectTreeObject = treeObject.getProjectTreeObject();
					}
					if (projectTreeObject != null) {
						ConvertigoPlugin.projectManager.setCurrentProject(projectTreeObject);

						IProject prj = projectTreeObject.getIProject();
						List<Object> res = new LinkedList<>();
						TreeObject current = treeObject;
						while (res.isEmpty()) {
							if (current instanceof ProjectTreeObject) {
								res.add(0, prj);
							} else if (current instanceof MobileApplicationTreeObject) {
								res.add(0, prj.findMember("DisplayObjects"));
							} else if (current instanceof MobilePlatformTreeObject) {
								res.add(0, prj.findMember("DisplayObjects/platforms/" + ((MobilePlatformTreeObject) current).getName() + "/"));
							} else if (current instanceof NgxApplicationComponentTreeObject || current instanceof MobileApplicationComponentTreeObject) {
								res.add(0, prj.findMember("DisplayObjects/mobile/"));
							}
							current = current.getParent();
						}
						while (res.get(0) != prj) {
							res.add(0, ((IResource) res.get(0)).getParent());
						}

						Object[] resObj = res.toArray();

						try {
							IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
							ProjectExplorer pe;
							if ((pe = (ProjectExplorer) activePage.findView("org.eclipse.ui.navigator.ProjectExplorer")) != null) {
								CommonViewer cv = pe.getCommonViewer();
								ITreeSelection ts = new TreeSelection(new org.eclipse.jface.viewers.TreePath(resObj));
								cv.setSelection(ts, false);
								cv.setExpandedElements(resObj);
								Tree tree = cv.getTree();
								tree.setTopItem(tree.getSelection()[0]);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					boolean doRefresh = true;

					if (treeObject instanceof DatabaseObjectTreeObject) {
						DatabaseObjectTreeObject dbot = (DatabaseObjectTreeObject) treeObject;
						if (dbot.isEditingComment) {
							doRefresh = dbot.isEditingComment = false;
						}
					}

					if (doRefresh) {
						Set<TreeObject> items = new HashSet<TreeObject>();
						for (TreeItem item: lastItem) {
							if (item != null && !item.isDisposed()) {
								items.add((TreeObject) item.getData());
							}
						}
						items.add(treeObject);

						items = reduceWithCommonParents(items);
						for (TreeObject item: items) {
							viewer.refresh(item, true);
						}
					}

					lastItem = viewer.getTree().getSelection();
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
		bars.setGlobalActionHandler(ActionFactory.UNDO.getId(), undoAction);
		bars.setGlobalActionHandler(ActionFactory.REDO.getId(), redoAction);
		 */
	}

	public void loadProject(UnloadedProjectTreeObject unloadedProjectTreeObject) {
		loadProject(unloadedProjectTreeObject, false, null);
	}

	protected synchronized void loadProject(UnloadedProjectTreeObject unloadedProjectTreeObject, boolean isCopy, String originalName) {
		String projectName = unloadedProjectTreeObject.toString();
		if (!MigrationManager.isProjectMigrated(projectName)) {
			String message = "Could not load the project \"" + projectName + "\" while it is still migrating.";
			ConvertigoPlugin.logDebug("[ProjectExplorerView] loadProject : " + message);
			ConvertigoPlugin.logError(message, Boolean.TRUE);
		} else {
			try {
				ProjectLoadingJob job = new ProjectLoadingJob(viewer, unloadedProjectTreeObject, isCopy, originalName);
				job.setUser(true);
				job.schedule();
			} catch(Exception e) {
				String message = "Error while loading the project \"" + projectName + "\".\n" + e.getMessage();
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

	private void edit(TreeObject treeObject, boolean newObject) {
		final Tree tree = viewer.getTree();
		final TreeEditor editor = new TreeEditor (tree);
		final Color black = getSite().getShell().getDisplay().getSystemColor (SWT.COLOR_BLACK);

		TreeItem[] items = tree.getSelection();
		if (items.length > 0) {
			final TreeItem item = items[0];
			final TreeObject theTreeObject = treeObject;

			if (treeObject instanceof ProjectTreeObject) {
				if (((ProjectTreeObject) treeObject).getModified()) {
					// Project need to be saved to avoid xsd/wsdl modification errors - Fix ticket #2265
					ConvertigoPlugin.warningMessageBox("Please save project before renaming it.");
					return;
				}
			}
			if (treeObject.getObject() instanceof HandlerStatement) {
				return;
			}
			if ((item != null) && lastItem.length > 0 && (item == lastItem[0])) {
				boolean isCarbon = SWT.getPlatform().equals("carbon");
				final Composite composite = new Composite (tree, SWT.NONE);
				if (!isCarbon) {
					composite.setBackground(black);
				}
				final Text text = new Text(composite, SWT.NONE);
				final int inset = isCarbon ? 0 : 1;
				composite.addListener (SWT.Resize, new Listener() {
					public void handleEvent (Event e) {
						Rectangle rect = composite.getClientArea();
						text.setBounds(rect.x + inset, rect.y + inset, rect.width - inset * 2, rect.height - inset * 2);
					}
				});
				Listener textListener = new Listener() {
					public void handleEvent (final Event e) {
						MobileBuilder mb = MobileBuilder.getBuilderOf(theTreeObject.getObject());

						String newName = null;
						String oldName = null;
						boolean needRefresh = false;
						boolean needProjectReload = false;

						if (theTreeObject instanceof DatabaseObjectTreeObject) {
							oldName = ((DatabaseObject) ((DatabaseObjectTreeObject) theTreeObject).getObject()).getName();
						} else if (theTreeObject instanceof TraceTreeObject) {
							oldName = ((TraceTreeObject) theTreeObject).getName();
						} else if (theTreeObject instanceof DesignDocumentViewTreeObject) {
							oldName = ((DesignDocumentViewTreeObject) theTreeObject).getName();
						} else if (theTreeObject instanceof DesignDocumentFunctionTreeObject) {
							oldName = ((DesignDocumentFunctionTreeObject) theTreeObject).getName();
						}

						switch (e.type) {
						case SWT.FocusOut:
							editingTextCtrl = null;
							composite.dispose();
							break;
						case SWT.Verify:
							String newText = text.getText();
							String leftText = newText.substring(0, e.start);
							String rightText = newText.substring(e.end, newText.length ());
							GC gc = new GC(text);
							Point size = gc.textExtent(leftText + e.text + rightText);
							gc.dispose();
							size = text.computeSize(size.x, SWT.DEFAULT);
							editor.horizontalAlignment = SWT.LEFT;
							Rectangle itemRect = item.getBounds(), rect = tree.getClientArea();
							editor.minimumWidth = Math.max(size.x, itemRect.width) + inset * 2;
							int left = itemRect.x, right = rect.x + rect.width;
							editor.minimumWidth = Math.min(editor.minimumWidth, right - left);
							editor.minimumHeight = size.y + inset * 2;
							editor.layout();
							break;
						case SWT.Traverse:
							switch (e.detail) {
							case SWT.TRAVERSE_RETURN:
								Engine.logStudio.info("---------------------- Rename started ----------------------");
								if (mb != null) {
									mb.prepareBatchBuild();
								}

								newName = text.getText();

								// Save and close editors
								if (theTreeObject instanceof IClosableTreeObject) {
									((IClosableTreeObject) theTreeObject).closeAllEditors(true);
								}

								if (theTreeObject instanceof DatabaseObjectTreeObject) {
									DatabaseObjectTreeObject dbObjectTreeObject = (DatabaseObjectTreeObject) theTreeObject;
									if (dbObjectTreeObject.rename(newName, Boolean.TRUE)) {
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
								} else if (theTreeObject instanceof DesignDocumentViewTreeObject) {
									DesignDocumentViewTreeObject ddvto = (DesignDocumentViewTreeObject)theTreeObject;
									if (ddvto.rename(newName, Boolean.TRUE)) {
										item.setText(newName);
										needRefresh = true;
									}
								} else if (theTreeObject instanceof DesignDocumentFunctionTreeObject) {
									DesignDocumentFunctionTreeObject ddfto = (DesignDocumentFunctionTreeObject)theTreeObject;
									if (ddfto.rename(newName, Boolean.TRUE)) {
										item.setText(newName);
										needRefresh = true;
									}
								}
								//FALL THROUGH
							case SWT.TRAVERSE_ESCAPE:
								editingTextCtrl = null;
								composite.dispose ();
								e.doit = false;
							}
							break;
						}

						if (needRefresh) {
							boolean needNgxPaletteReload = false;
							if (!newObject) {
								boolean updateDlg = false;
								boolean updateReferences = false;
								int update = 0;
								// Updates references to object if needed
								if ((theTreeObject instanceof ProjectTreeObject) ||
										(theTreeObject instanceof SequenceTreeObject) ||
										(theTreeObject instanceof ConnectorTreeObject) ||
										(theTreeObject instanceof TransactionTreeObject) ||
										(theTreeObject instanceof VariableTreeObject2) ||
										(theTreeObject instanceof IDesignTreeObject) ||
										(theTreeObject instanceof MobilePageComponentTreeObject) ||
										(theTreeObject instanceof MobileUIComponentTreeObject) ||
										(theTreeObject instanceof NgxPageComponentTreeObject) ||
										(theTreeObject instanceof NgxUIComponentTreeObject)) {
									String objectType = "";
									if (theTreeObject instanceof ProjectTreeObject) {
										objectType = "project";
										updateDlg = true;
									} else if (theTreeObject instanceof SequenceTreeObject) {
										objectType = "sequence";
										updateDlg = true;
									} else if (theTreeObject instanceof ConnectorTreeObject) {
										objectType = "connector";
										updateDlg = true;
									} else if (theTreeObject instanceof TransactionTreeObject) {
										objectType = "transaction";
										updateDlg = true;
									} else if (theTreeObject instanceof VariableTreeObject2) {
										objectType = "variable";
										updateDlg = ((DatabaseObject)theTreeObject.getObject()) instanceof RequestableVariable ? true:false;
									} else if (theTreeObject instanceof DesignDocumentTreeObject) {
										objectType = "document";
										updateDlg = true;
									} else if (theTreeObject instanceof DesignDocumentViewTreeObject) {
										objectType = "view";
										updateDlg = true;
									} else if (theTreeObject instanceof DesignDocumentFilterTreeObject) {
										objectType = "filter";
										updateDlg = true;
									} else if (theTreeObject instanceof DesignDocumentUpdateTreeObject) {
										objectType = "update";
										updateDlg = true;
									} else if (theTreeObject instanceof DesignDocumentValidateTreeObject) {
										objectType = "validate";
										updateDlg = true;
									} else if (theTreeObject instanceof MobilePageComponentTreeObject) {
										objectType = "page";
										updateDlg = true;
									} else if (theTreeObject instanceof NgxPageComponentTreeObject) {
										objectType = "page";
										updateDlg = true;
									} else if (theTreeObject instanceof MobileUIComponentTreeObject) {
										DatabaseObject dbo = (DatabaseObject)theTreeObject.getObject();
										if (dbo instanceof com.twinsoft.convertigo.beans.mobile.components.UIDynamicMenu) {
											objectType = "menu";
											updateDlg = true;
										}
										if (dbo instanceof com.twinsoft.convertigo.beans.mobile.components.UIActionStack) {
											objectType = "shared action";
											updateDlg = true;
										}
										if (dbo instanceof com.twinsoft.convertigo.beans.mobile.components.UISharedComponent) {
											objectType = "shared component";
											updateDlg = true;
										}
										if (dbo instanceof com.twinsoft.convertigo.beans.mobile.components.UIStackVariable) {
											objectType = "variable";
											updateDlg = true;
										}
										if (dbo instanceof com.twinsoft.convertigo.beans.mobile.components.UICompVariable) {
											objectType = "variable";
											updateDlg = true;
										}
									} else if (theTreeObject instanceof NgxUIComponentTreeObject) {
										DatabaseObject dbo = (DatabaseObject)theTreeObject.getObject();
										if (dbo instanceof com.twinsoft.convertigo.beans.ngx.components.UIDynamicMenu) {
											objectType = "menu";
											updateDlg = true;
										}
										if (dbo instanceof com.twinsoft.convertigo.beans.ngx.components.UIActionStack) {
											objectType = "shared action";
											updateDlg = true;
										}
										if (dbo instanceof com.twinsoft.convertigo.beans.ngx.components.UISharedComponent) {
											objectType = "shared component";
											updateDlg = true;
										}
										if (dbo instanceof com.twinsoft.convertigo.beans.ngx.components.UIStackVariable) {
											objectType = "variable";
											updateDlg = true;
										}
										if (dbo instanceof com.twinsoft.convertigo.beans.ngx.components.UICompVariable) {
											objectType = "variable";
											updateDlg = true;
										}
										if (dbo instanceof com.twinsoft.convertigo.beans.ngx.components.UICompEvent) {
											objectType = "event";
											updateDlg = true;
										}
									}

									if (updateDlg) {
										Shell shell = Display.getDefault().getActiveShell();
										CustomDialog customDialog = new CustomDialog(
												shell,
												"Update object references",
												"Do you want to update "
														+ objectType
														+ " references ?\n You can replace '"
														+ oldName
														+ "' by '"
														+ newName
														+ "' in all loaded projects \n or replace '"
														+ oldName
														+ "' by '"
														+ newName
														+ "' in current project only.",
														670, 200,
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
									treeObjectEvent = new TreeObjectEvent(theTreeObject, "name", oldName, newName, update);
								} else {
									treeObjectEvent = new TreeObjectEvent(theTreeObject, "name", oldName, newName);
								}
								BatchOperationHelper.start();
								ProjectExplorerView.this.refreshTree();
								ProjectExplorerView.this.setSelectedTreeObject(theTreeObject);
								ProjectExplorerView.this.fireTreeObjectPropertyChanged(treeObjectEvent);
								if (updateReferences && needProjectReload) {
									((ProjectTreeObject) theTreeObject).save(false);
								}
								if (mb != null) {
									if (theTreeObject instanceof MobilePageComponentTreeObject) {
										try {
											mb.pageRenamed((com.twinsoft.convertigo.beans.mobile.components.PageComponent) theTreeObject.getObject(), oldName);
										} catch (EngineException e1) {
											e1.printStackTrace();
										}
									}
									if (theTreeObject instanceof NgxUIComponentTreeObject) {
										if (theTreeObject.getObject() instanceof com.twinsoft.convertigo.beans.ngx.components.UIActionStack) {
											needNgxPaletteReload = true;
										}
										if (theTreeObject.getObject() instanceof com.twinsoft.convertigo.beans.ngx.components.UISharedRegularComponent) {
											needNgxPaletteReload = true;
										}
									}
								}
								BatchOperationHelper.stop();
								Engine.logStudio.info("---------------------- Rename ended   ----------------------");
							} else {
								BatchOperationHelper.start();
								
								ProjectExplorerView.this.fireTreeObjectPropertyChanged(new TreeObjectEvent(theTreeObject, "name", oldName, newName));
								ProjectExplorerView.this.refreshTree();
								ProjectExplorerView.this.setSelectedTreeObject(theTreeObject);
								
								if (theTreeObject instanceof NgxUIComponentTreeObject) {
									if (theTreeObject.getObject() instanceof com.twinsoft.convertigo.beans.ngx.components.UIActionStack) {
										needNgxPaletteReload = true;
									}
									if (theTreeObject.getObject() instanceof com.twinsoft.convertigo.beans.ngx.components.UISharedRegularComponent) {
										needNgxPaletteReload = true;
									}
								}
								
								BatchOperationHelper.stop();
								Engine.logStudio.info("---------------------- Rename ended   ----------------------");
							}
							
							StructuredSelection structuredSelection = new StructuredSelection(theTreeObject);
							ISelectionListener listener = null;

							ConvertigoPlugin plugin = ConvertigoPlugin.getDefault();
							// refresh properties view
							listener = plugin.getPropertiesView();
							if (listener != null) {
								listener.selectionChanged(
										(IWorkbenchPart) ProjectExplorerView.this,
										structuredSelection);
								plugin.refreshPropertiesView();
							}
							plugin.getPropertiesView().setPinned(isCarbon);
							// refresh references view
							listener = plugin.getReferencesView();
							if (listener != null)
								listener.selectionChanged(
										(IWorkbenchPart) ProjectExplorerView.this,
										structuredSelection);

							// Refresh ngx palette view
							if (needNgxPaletteReload) {
								plugin.refreshPaletteView();
							}
						}
						if (needProjectReload) {
							Engine.theApp.databaseObjectsManager.clearCache(newName);
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
					text.setText(((DatabaseObjectTreeObject)theTreeObject).getName());
				} else if (theTreeObject instanceof TraceTreeObject) {
					text.setText(((TraceTreeObject)theTreeObject).getName());
				} else if (theTreeObject instanceof DesignDocumentViewTreeObject) {
					text.setText(((DesignDocumentViewTreeObject)theTreeObject).getName());
				} else if (theTreeObject instanceof DesignDocumentFunctionTreeObject) {
					text.setText(((DesignDocumentFunctionTreeObject)theTreeObject).getName());
				}
				text.selectAll();
				text.setFocus();
				editingTextCtrl = text;
			}
			lastItem[0] = item;
		}
	}

	private boolean checkReload(TreeParent parentTreeObject, DatabaseObject parentDatabaseObject) {
		boolean ok = parentTreeObject.getObject() == parentDatabaseObject && !(parentDatabaseObject instanceof DesignDocument);
		if (ok) {
			try {
				List<DatabaseObject> dboChildren = parentDatabaseObject.getDatabaseObjectChildren();
				List<DatabaseObjectTreeObject> dbotChildren = parentTreeObject.getDatabaseObjectTreeObjectChildren();
				if (ok = dboChildren.size() == dbotChildren.size()) {
					Iterator<DatabaseObject> idbo = dboChildren.iterator();
					Iterator<DatabaseObjectTreeObject> idbot = dbotChildren.iterator();
					while (ok && idbo.hasNext()) {
						ok = checkReload(idbot.next(), idbo.next());
					}
				}
			} catch (Exception e) {
				ok = false;
			}
		}
		return ok;
	}
	
	private void reload(TreeParent parentTreeObject, DatabaseObject parentDatabaseObject) throws EngineException, IOException {
		if (!checkReload(parentTreeObject, parentDatabaseObject)) {
			try {
				ModalContext.run(new ReloadWithProgress(viewer, parentTreeObject, parentDatabaseObject), true, new NullProgressMonitor(), ConvertigoPlugin.getDisplay());
			} catch (InvocationTargetException e) {
			} catch (InterruptedException e) {
			}
		}
	}

	private class ReloadWithProgress implements IRunnableWithProgress, DatabaseObjectListener {
		private TreeParent parentTreeObject;
		private DatabaseObject parentDatabaseObject;
		private TreeViewer viewer;
		private Object[] objects = null;
		private String[] expendedPaths = null;
		private IProgressMonitor monitor;

		public ReloadWithProgress(TreeViewer viewer, TreeParent parentTreeObject, DatabaseObject parentDatabaseObject) {
			super();
			this.viewer = viewer;
			this.parentTreeObject = parentTreeObject;
			this.parentDatabaseObject = parentDatabaseObject;
		}

		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			String dboName = (parentDatabaseObject instanceof Step) ? ((Step)parentDatabaseObject).getStepNodeName():parentDatabaseObject.getName();

			this.monitor = monitor;

			try {
				int worksNumber = 10;

				monitor.beginTask("Reloading \""+ dboName + "\" object", worksNumber);

				monitor.subTask("Storing expanded paths...");
				ConvertigoPlugin.syncExec(() -> {
					// Store the currently expanded tree objects
					objects = viewer.getExpandedElements();
					if (objects != null) {
						expendedPaths = new String[objects.length];
						for (int i = 0 ; i < objects.length ; i++) {
							TreeObject object = (TreeObject)objects[i];
							expendedPaths[i] = object.getPath();
						}
					}
				});

				try {
					// First remove all children of object
					monitor.subTask("Removing objects...");
					parentTreeObject.removeAllChildren();

					// Then load object again
					monitor.subTask("Loading objects...");
					Engine.theApp.databaseObjectsManager.addDatabaseObjectListener(this);
					loadDatabaseObject(parentTreeObject, parentDatabaseObject, monitor);
				}
				finally {
					Engine.theApp.databaseObjectsManager.removeDatabaseObjectListener(this);
				}
			}
			catch (Exception e) {
				ConvertigoPlugin.logException(
						e, "Failure when loading objects");
			}
			finally {
				// Updating the tree viewer
				if (parentTreeObject != null) {
					ConvertigoPlugin.syncExec(() -> {
						// Reload is complete, notify now for newly added objects
						Set<Object> done = new HashSet<Object>();
						for (TreeObject ob: addedTreeObjects) {
							fireTreeObjectAdded(new TreeObjectEvent(ob, null, null, null, 0, done));
						}
						addedTreeObjects.clear();
						done.clear();
						refreshTreeObject(parentTreeObject);
					});

					if (expendedPaths != null) {
						ConvertigoPlugin.asyncExec(() -> {
							for (int i = 0; i < expendedPaths.length; i++) {
								String previousPath = expendedPaths[i];
								TreeObject treeObject = findTreeObjectByPath(parentTreeObject, previousPath);
								if (treeObject != null)
									objects[i] = treeObject;
							}

							viewer.setExpandedElements(objects);
						});
					}
				}
			}
		}

		/* (non-Javadoc)
		 * @see com.twinsoft.convertigo.engine.DatabaseObjectListener#databaseObjectLoaded(com.twinsoft.convertigo.engine.DatabaseObjectLoadedEvent)
		 */
		public void databaseObjectLoaded(DatabaseObjectLoadedEvent event) {
			DatabaseObject dbo = (DatabaseObject) event.getSource();
			String dboName = dbo instanceof Step ? ((Step)dbo).getStepNodeName():dbo.getName();
			monitor.subTask("Object \"" + dboName + "\" loaded");
			monitor.worked(1);
		}

		/* (non-Javadoc)
		 * @see com.twinsoft.convertigo.engine.DatabaseObjectListener#databaseObjectImported(com.twinsoft.convertigo.engine.DatabaseObjectImportedEvent)
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

		file = new File(Engine.projectDir(projectName) + "/_private");
		if (!file.exists())
			file.mkdir();

		file = new File(Engine.projectDir(projectName) + "/Traces");
		if (!file.exists())
			file.mkdir();

		file = new File(Engine.projectDir(projectName) + "/xsd/internal");
		if (!file.exists())
			file.mkdirs();

		file = new File(Engine.projectDir(projectName) + "/wsdl");
		if (!file.exists())
			file.mkdir();
	}

	private void createFiles(String projectName) {
		createIndexFile(projectName);
	}

	private void createIndexFile(String projectName) {
		try {
			ProjectUtils.copyIndexFile(projectName);
		} catch (Exception e) {
			ConvertigoPlugin.logException(e,"Error creating index.html file for project '" + projectName + "'", Boolean.FALSE);
		}
	}

	private void loadDatabaseObject(TreeParent parentTreeObject, DatabaseObject parentDatabaseObject, IProgressMonitor monitor) throws EngineException, IOException {
		loadDatabaseObject(parentTreeObject, parentDatabaseObject, null, monitor);
	}

	public void loadDatabaseObject(TreeParent parentTreeObject, DatabaseObject parentDatabaseObject, ProjectLoadingJob projectLoadingJob) throws EngineException, IOException {
		loadDatabaseObject(parentTreeObject, parentDatabaseObject, projectLoadingJob, projectLoadingJob.getMonitor());
	}

	private void loadDatabaseObject(TreeParent parentTreeObject, DatabaseObject parentDatabaseObject, ProjectLoadingJob projectLoadingJob, final IProgressMonitor monitor) throws EngineException, IOException {
		// Add load subtask here because of databaseObjectLoaded event no more received since memory improvement
		// (getSubDatabaseObject called only when necessary)

		try {
			new WalkHelper() {
				// recursion parameters
				TreeParent parentTreeObject;
				ProjectLoadingJob projectLoadingJob;

				// sibling parameters
				ObjectsFolderTreeObject currentTreeFolder = null;

				public void init(DatabaseObject databaseObject, TreeParent parentTreeObject, ProjectLoadingJob projectLoadingJob) throws Exception {
					this.parentTreeObject = parentTreeObject;
					this.projectLoadingJob = projectLoadingJob;

					walkInheritance = true;
					super.init(databaseObject);
				}

				protected ObjectsFolderTreeObject getFolder(TreeParent treeParent, int folderType) {
					ObjectsFolderTreeObject ofto = null;
					for (TreeObject to: treeParent.getChildren()) {
						if (to instanceof ObjectsFolderTreeObject) {
							if (((ObjectsFolderTreeObject)to).folderType == folderType) {
								ofto = (ObjectsFolderTreeObject) to;
								break;
							}
						}
					}
					if (ofto == null) {
						ofto = new ObjectsFolderTreeObject(viewer, folderType);
						treeParent.addChild(ofto);
					}
					return ofto;
				}

				@Override
				protected void walk(DatabaseObject databaseObject) throws Exception {
					// retrieve recursion parameters
					final TreeParent parentTreeObject = this.parentTreeObject;
					final ProjectLoadingJob projectLoadingJob = this.projectLoadingJob;

					// retrieve sibling parameters
					ObjectsFolderTreeObject currentTreeFolder = this.currentTreeFolder;

					String dboName = (databaseObject instanceof Step) ? ((Step) databaseObject).getStepNodeName() : databaseObject.getName();
					monitor.subTask("Loading databaseObject '"+ dboName +"'...");

					DatabaseObjectTreeObject databaseObjectTreeObject = null;

					// first call case, the tree object already exists and its content is just refreshed
					if (parentTreeObject.getObject() == databaseObject) {
						databaseObjectTreeObject = (DatabaseObjectTreeObject) parentTreeObject;
					}
					// recursive call case, the tree object doesn't exist and must be added to the parent tree object
					else {
						int folderType = Integer.MIN_VALUE;
						if (databaseObject instanceof Connector) {
							folderType = ObjectsFolderTreeObject.FOLDER_TYPE_CONNECTORS;
							databaseObjectTreeObject = new ConnectorTreeObject(viewer, (Connector) databaseObject, false);

						} else if (databaseObject instanceof Sequence) {
							folderType = ObjectsFolderTreeObject.FOLDER_TYPE_SEQUENCES;
							databaseObjectTreeObject = new SequenceTreeObject(viewer, (Sequence) databaseObject, false);

						} else if (databaseObject instanceof MobileApplication) {
							databaseObjectTreeObject = new MobileApplicationTreeObject(viewer, (MobileApplication) databaseObject, false);

						} else if (databaseObject instanceof MobilePlatform) {
							folderType = ObjectsFolderTreeObject.FOLDER_TYPE_PLATFORMS;
							databaseObjectTreeObject = new MobilePlatformTreeObject(viewer, (MobilePlatform) databaseObject, false);

						}
						/**************************************************************************************************/
						/***                 com.twinsoft.convertigo.beans.mobile.components                           ****/
						/**************************************************************************************************/
						else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.MobileComponent) {
							if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent) {
								databaseObjectTreeObject = new MobileApplicationComponentTreeObject(viewer, GenericUtils.cast(databaseObject), false);

							} else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.RouteComponent) {
								folderType = ObjectsFolderTreeObject.FOLDER_TYPE_ROUTES;
								databaseObjectTreeObject = new MobileRouteComponentTreeObject(viewer, GenericUtils.cast(databaseObject), false);

							} else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.RouteEventComponent) {
								folderType = ObjectsFolderTreeObject.FOLDER_TYPE_EVENTS;
								databaseObjectTreeObject = new MobileRouteEventComponentTreeObject(viewer, GenericUtils.cast(databaseObject), false);

							} else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.RouteActionComponent) {
								folderType = ObjectsFolderTreeObject.FOLDER_TYPE_ACTIONS;
								databaseObjectTreeObject = new MobileRouteActionComponentTreeObject(viewer, GenericUtils.cast(databaseObject), false);

							} else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.PageComponent) {
								folderType = ObjectsFolderTreeObject.FOLDER_TYPE_PAGES;
								databaseObjectTreeObject = new MobilePageComponentTreeObject(viewer, GenericUtils.cast(databaseObject), false);

							} else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIActionStack) {
								folderType = ObjectsFolderTreeObject.FOLDER_TYPE_SHARED_ACTIONS;
								databaseObjectTreeObject = new MobileUIComponentTreeObject(viewer, GenericUtils.cast(databaseObject), false);

							} else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UISharedComponent) {
								folderType = ObjectsFolderTreeObject.FOLDER_TYPE_SHARED_COMPONENTS;
								databaseObjectTreeObject = new MobileUIComponentTreeObject(viewer, GenericUtils.cast(databaseObject), false);

							} else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIDynamicMenu) {
								folderType = ObjectsFolderTreeObject.FOLDER_TYPE_MENUS;
								databaseObjectTreeObject = new MobileUIComponentTreeObject(viewer, GenericUtils.cast(databaseObject), false);

							} else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIComponent) {
								if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIAttribute) {
									folderType = ObjectsFolderTreeObject.FOLDER_TYPE_ATTRIBUTES;
									if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIControlAttr) {
										folderType = ObjectsFolderTreeObject.FOLDER_TYPE_CONTROLS;
									}
								}
								else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIStyle) {
									folderType = ObjectsFolderTreeObject.FOLDER_TYPE_STYLES;
								}
								else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIControlVariable) {
									folderType = ObjectsFolderTreeObject.FOLDER_TYPE_VARIABLES;
								}
								else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UICompVariable) {
									folderType = ObjectsFolderTreeObject.FOLDER_TYPE_VARIABLES;
								}
								else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIStackVariable) {
									folderType = ObjectsFolderTreeObject.FOLDER_TYPE_VARIABLES;
								}
								else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIFormValidator) {
									folderType = ObjectsFolderTreeObject.FOLDER_TYPE_VALIDATORS;
								}
								else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIAppEvent) {
									folderType = ObjectsFolderTreeObject.FOLDER_TYPE_EVENTS;
								}
								else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIPageEvent) {
									folderType = ObjectsFolderTreeObject.FOLDER_TYPE_EVENTS;
								}
								else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIEventSubscriber) {
									folderType = ObjectsFolderTreeObject.FOLDER_TYPE_EVENTS;
								}
								databaseObjectTreeObject = new MobileUIComponentTreeObject(viewer, GenericUtils.cast(databaseObject), false);
							}
						}
						/**************************************************************************************************/
						/***                   com.twinsoft.convertigo.beans.ngx.components                            ****/
						/**************************************************************************************************/
						else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.MobileComponent) {
							if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent) {
								databaseObjectTreeObject = new NgxApplicationComponentTreeObject(viewer, GenericUtils.cast(databaseObject), false);

							} else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.PageComponent) {
								folderType = ObjectsFolderTreeObject.FOLDER_TYPE_PAGES;
								databaseObjectTreeObject = new NgxPageComponentTreeObject(viewer, GenericUtils.cast(databaseObject), false);

							} else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIActionStack) {
								folderType = ObjectsFolderTreeObject.FOLDER_TYPE_SHARED_ACTIONS;
								databaseObjectTreeObject = new NgxUIComponentTreeObject(viewer, GenericUtils.cast(databaseObject), false);

							} else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UISharedComponent) {
								folderType = ObjectsFolderTreeObject.FOLDER_TYPE_SHARED_COMPONENTS;
								databaseObjectTreeObject = new NgxUIComponentTreeObject(viewer, GenericUtils.cast(databaseObject), false);

							} else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIDynamicMenu) {
								folderType = ObjectsFolderTreeObject.FOLDER_TYPE_MENUS;
								databaseObjectTreeObject = new NgxUIComponentTreeObject(viewer, GenericUtils.cast(databaseObject), false);

							} else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIComponent) {
								if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIAttribute) {
									folderType = ObjectsFolderTreeObject.FOLDER_TYPE_ATTRIBUTES;
									if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIControlAttr) {
										folderType = ObjectsFolderTreeObject.FOLDER_TYPE_CONTROLS;
									}
								}
								else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIDynamicAttr) {
									folderType = ObjectsFolderTreeObject.FOLDER_TYPE_ATTRIBUTES;
								}
								else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIStyle) {
									folderType = ObjectsFolderTreeObject.FOLDER_TYPE_STYLES;
								}
								else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIControlVariable) {
									folderType = ObjectsFolderTreeObject.FOLDER_TYPE_VARIABLES;
								}
								else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UICompVariable) {
									folderType = ObjectsFolderTreeObject.FOLDER_TYPE_VARIABLES;
								}
								else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIStackVariable) {
									folderType = ObjectsFolderTreeObject.FOLDER_TYPE_VARIABLES;
								}
								else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIAppEvent) {
									folderType = ObjectsFolderTreeObject.FOLDER_TYPE_EVENTS;
								}
								else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIPageEvent) {
									folderType = ObjectsFolderTreeObject.FOLDER_TYPE_EVENTS;
								}
								else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UISharedComponentEvent) {
									folderType = ObjectsFolderTreeObject.FOLDER_TYPE_EVENTS;
								}
								else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIEventSubscriber) {
									folderType = ObjectsFolderTreeObject.FOLDER_TYPE_EVENTS;
								}
								databaseObjectTreeObject = new NgxUIComponentTreeObject(viewer, GenericUtils.cast(databaseObject), false);
							}
						} else if (databaseObject instanceof UrlMapper) {
							databaseObjectTreeObject = new UrlMapperTreeObject(viewer, (UrlMapper) databaseObject, false);

						} else if (databaseObject instanceof UrlAuthentication) {
							folderType = ObjectsFolderTreeObject.FOLDER_TYPE_AUTHENTICATIONS;
							databaseObjectTreeObject = new UrlAuthenticationTreeObject(viewer, (UrlAuthentication) databaseObject, false);

						} else if (databaseObject instanceof UrlMapping) {
							folderType = ObjectsFolderTreeObject.FOLDER_TYPE_MAPPINGS;
							databaseObjectTreeObject = new UrlMappingTreeObject(viewer, (UrlMapping) databaseObject, false);

						} else if (databaseObject instanceof UrlMappingOperation) {
							folderType = ObjectsFolderTreeObject.FOLDER_TYPE_OPERATIONS;
							databaseObjectTreeObject = new UrlMappingOperationTreeObject(viewer, (UrlMappingOperation) databaseObject, false);

						} else if (databaseObject instanceof UrlMappingParameter) {
							folderType = ObjectsFolderTreeObject.FOLDER_TYPE_PARAMETERS;
							databaseObjectTreeObject = new UrlMappingParameterTreeObject(viewer, (UrlMappingParameter) databaseObject, false);

						} else if (databaseObject instanceof UrlMappingResponse) {
							folderType = ObjectsFolderTreeObject.FOLDER_TYPE_RESPONSES;
							databaseObjectTreeObject = new UrlMappingResponseTreeObject(viewer, (UrlMappingResponse) databaseObject, false);

						} else if (databaseObject instanceof Reference) {
							folderType = ObjectsFolderTreeObject.FOLDER_TYPE_REFERENCES;
							databaseObjectTreeObject = new ReferenceTreeObject(viewer, (Reference) databaseObject, false);

						} else if (databaseObject instanceof Pool) {
							folderType = ObjectsFolderTreeObject.FOLDER_TYPE_POOLS;
							databaseObjectTreeObject = new DatabaseObjectTreeObject(viewer, databaseObject, false);

						} else if (databaseObject instanceof Transaction) {
							folderType = ObjectsFolderTreeObject.FOLDER_TYPE_TRANSACTIONS;
							databaseObjectTreeObject = new TransactionTreeObject(viewer, (Transaction) databaseObject, false);

						} else if (databaseObject instanceof ScreenClass) {
							if (databaseObject.getParent() instanceof IScreenClassContainer<?>) {
								folderType = ObjectsFolderTreeObject.FOLDER_TYPE_SCREEN_CLASSES;
								databaseObjectTreeObject = new ScreenClassTreeObject(viewer, (ScreenClass) databaseObject, false);
							} else {
								folderType = ObjectsFolderTreeObject.FOLDER_TYPE_INHERITED_SCREEN_CLASSES;
								databaseObjectTreeObject = new ScreenClassTreeObject(viewer, (ScreenClass) databaseObject, false);
							}

						} else if (databaseObject instanceof Sheet) {
							folderType = ObjectsFolderTreeObject.FOLDER_TYPE_SHEETS;
							databaseObjectTreeObject = new SheetTreeObject(viewer,  (Sheet) databaseObject, parentTreeObject.getObject() != databaseObject.getParent());

						} else if (databaseObject instanceof TestCase) {
							folderType = ObjectsFolderTreeObject.FOLDER_TYPE_TESTCASES;
							databaseObjectTreeObject = new TestCaseTreeObject(viewer, (TestCase) databaseObject, false);

						} else if (databaseObject instanceof Variable) {
							folderType = ObjectsFolderTreeObject.FOLDER_TYPE_VARIABLES;
							databaseObjectTreeObject = new VariableTreeObject2(viewer, (Variable) databaseObject, false);

						} else if (databaseObject instanceof Step) {
							if (databaseObject.getParent() instanceof Sequence) {
								folderType = ObjectsFolderTreeObject.FOLDER_TYPE_STEPS;
							}
							databaseObjectTreeObject = new StepTreeObject(viewer, (Step) databaseObject, false);

						} else if (databaseObject instanceof Statement) {
							if (databaseObject.getParent() instanceof Transaction) {
								folderType = ObjectsFolderTreeObject.FOLDER_TYPE_FUNCTIONS;
							}
							databaseObjectTreeObject = new StatementTreeObject(viewer, (Statement) databaseObject, false);

						} else if (databaseObject instanceof Criteria) {
							folderType = ObjectsFolderTreeObject.FOLDER_TYPE_CRITERIAS;
							databaseObjectTreeObject = new CriteriaTreeObject(viewer, (Criteria) databaseObject, parentTreeObject.getObject() != databaseObject.getParent());

						} else if (databaseObject instanceof ExtractionRule) {
							folderType = ObjectsFolderTreeObject.FOLDER_TYPE_EXTRACTION_RULES;
							databaseObjectTreeObject = new ExtractionRuleTreeObject(viewer, (ExtractionRule) databaseObject, parentTreeObject.getObject() != databaseObject.getParent());

						} else if (databaseObject instanceof BlockFactory) {
							databaseObjectTreeObject = new DatabaseObjectTreeObject(viewer, databaseObject, parentTreeObject.getObject() != databaseObject.getParent());

						} else if (databaseObject instanceof com.twinsoft.convertigo.beans.core.Document) {
							folderType = ObjectsFolderTreeObject.FOLDER_TYPE_DOCUMENTS;
							com.twinsoft.convertigo.beans.core.Document document = (com.twinsoft.convertigo.beans.core.Document)databaseObject;
							String documentRenderer = document.getRenderer();
							if (documentRenderer.equals("DesignDocumentTreeObject"))
								databaseObjectTreeObject = new DesignDocumentTreeObject(viewer, document, false);
							else
								databaseObjectTreeObject = new DocumentTreeObject(viewer, document, false);

						} else if (databaseObject instanceof com.twinsoft.convertigo.beans.core.Listener) {
							folderType = ObjectsFolderTreeObject.FOLDER_TYPE_LISTENERS;
							com.twinsoft.convertigo.beans.core.Listener listener = (com.twinsoft.convertigo.beans.core.Listener) databaseObject;
							String listenerRenderer = listener.getRenderer();
							if (listenerRenderer.equals("FullSyncListenerTreeObject")) {
								databaseObjectTreeObject = new FullSyncListenerTreeObject(viewer, listener, false);
							} else {
								databaseObjectTreeObject = new ListenerTreeObject(viewer, listener, false);
							}

						} else if (databaseObject instanceof com.twinsoft.convertigo.beans.core.Index) {
							folderType = ObjectsFolderTreeObject.FOLDER_TYPE_INDEXES;
							databaseObjectTreeObject = new DatabaseObjectTreeObject(viewer, databaseObject, false);

						} else {
							// unknow DBO case !!!
							databaseObjectTreeObject = new DatabaseObjectTreeObject(viewer, databaseObject, false);
						}

						// no virtual folder
						if (folderType == Integer.MIN_VALUE) {
							parentTreeObject.addChild(databaseObjectTreeObject);
						}
						// virtual folder creation or reuse
						else {
							/* fixed #5416 */
							//if (currentTreeFolder == null || currentTreeFolder.folderType != folderType) {
							//	currentTreeFolder = new ObjectsFolderTreeObject(viewer, folderType);
							//	parentTreeObject.addChild(currentTreeFolder);
							//}
							currentTreeFolder = getFolder(parentTreeObject, folderType);

							currentTreeFolder.addChild(databaseObjectTreeObject);
						}

						// case databaseObject has been changed through dbo::preconfigure, mark projectTreeObject as modified
						if ((databaseObject.bNew) || (databaseObject.hasChanged && !databaseObject.bNew)) {
							databaseObjectTreeObject.hasBeenModified(true);
						}

						// new value of recursion parameters
						this.parentTreeObject = databaseObjectTreeObject;
					}

					// special databaseObject cases
					if (databaseObject instanceof Project) {
						Project project = (Project) databaseObject;

						// Creates directories and files
						createDirsAndFiles(project.getName());

						// Creates or Refresh xsd and wsdl folders
						IFolder xsdFolder, wsdlFolder = null;
						IFolder xsdInternalFolder = null;
						try {
							wsdlFolder = ((ProjectTreeObject)parentTreeObject).getFolder("wsdl");
							if (!wsdlFolder.exists())
								wsdlFolder.create(true, true, null);
							else
								wsdlFolder.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());

							xsdFolder = ((ProjectTreeObject)parentTreeObject).getFolder("xsd");
							if (!xsdFolder.exists())
								xsdFolder.create(true, true, null);
							else {
								xsdInternalFolder = xsdFolder.getFolder("internal");
								if (!xsdInternalFolder.exists())
									xsdInternalFolder.create(true, true, null);
								xsdFolder.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
							}
						}
						catch (Exception e) {
							e.printStackTrace();
						}

						// Connectors
						IFolder xsdConnectorInternalFolder = null;
						Collection<Connector> connectors = project.getConnectorsList();
						if (connectors.size() != 0) {
							// Set default connector if none
							if (project.getDefaultConnector() == null) {
								// Report from 4.5: fix #401
								ConvertigoPlugin.logWarning(null, "Project \""+ project.getName() +"\" has no default connector. Try to set a default one.");
								Connector defaultConnector = connectors.iterator().next();
								try {
									project.setDefaultConnector(defaultConnector);
									defaultConnector.hasChanged = true;
								} catch (Exception e) {
									ConvertigoPlugin.logWarning(e, "Unable to set a default connector for project \""+ project.getName() +"\"");
								}
							}

							// Refresh Traces folder
							IFolder ifolder = ((ProjectTreeObject)parentTreeObject).getFolder("Traces");
							if(ifolder.exists()) {
								try {
									ifolder.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
								} catch (CoreException e) {
								}
							}

							// Creates or Refresh internal xsd connector folders
							for (Connector connector : connectors) {
								try {
									xsdConnectorInternalFolder = xsdInternalFolder.getFolder(connector.getName());
									if (!xsdConnectorInternalFolder.exists())
										xsdConnectorInternalFolder.create(true, true, null);
									else
										xsdConnectorInternalFolder.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
								}
								catch (Exception e) {
									e.printStackTrace();
								}
							}
						}

					} else if (databaseObject instanceof Connector) {
						Connector connector = (Connector) databaseObject;

						// Open connector editor
						if (projectLoadingJob != null && connector.isDefault) {
							projectLoadingJob.setDefaultConnectorTreeObject((ConnectorTreeObject) databaseObjectTreeObject);
						}

						// Traces
						if (connector instanceof JavelinConnector) {
							String projectName = databaseObject.getProject().getName();

							if (projectLoadingJob == null) {
								if (MigrationManager.isProjectMigrated(projectName)) {
									UnloadedProjectTreeObject unloadedProjectTreeObject = new UnloadedProjectTreeObject(databaseObjectTreeObject.viewer, projectName);
									this.projectLoadingJob = new ProjectLoadingJob(databaseObjectTreeObject.viewer, unloadedProjectTreeObject);
									this.projectLoadingJob.loadTrace(databaseObjectTreeObject, new File(Engine.projectDir(projectName) + "/Traces/" + connector.getName()));
								}
							}
							if (projectLoadingJob != null) {
								projectLoadingJob.loadTrace(databaseObjectTreeObject, new File(Engine.projectDir(projectName) + "/Traces/" + connector.getName()));
							}
						}

					} else if (databaseObject instanceof Transaction) {
						Transaction transaction = (Transaction) databaseObject;

						// Functions
						List<HandlersDeclarationTreeObject> treeObjects = new LinkedList<HandlersDeclarationTreeObject>();
						String line, lineReaded;
						int lineNumber = 0;
						BufferedReader br = new BufferedReader(new StringReader(transaction.handlers));

						line = br.readLine();
						while (line != null) {
							lineReaded = line.trim();
							lineNumber++;
							if (lineReaded.startsWith("function ")) {
								try {
									String functionName = lineReaded.substring(9, lineReaded.indexOf(')') + 1);
									HandlersDeclarationTreeObject handlersDeclarationTreeObject = null;

									if (functionName.endsWith(JavelinTransaction.EVENT_ENTRY_HANDLER + "()")) {
										handlersDeclarationTreeObject = new HandlersDeclarationTreeObject(viewer, functionName, HandlersDeclarationTreeObject.TYPE_FUNCTION_SCREEN_CLASS_ENTRY, lineNumber);
									} else if (functionName.endsWith(JavelinTransaction.EVENT_EXIT_HANDLER + "()")) {
										handlersDeclarationTreeObject = new HandlersDeclarationTreeObject(viewer, functionName, HandlersDeclarationTreeObject.TYPE_FUNCTION_SCREEN_CLASS_EXIT, lineNumber);
									} else {
										handlersDeclarationTreeObject = new HandlersDeclarationTreeObject(viewer, functionName, HandlersDeclarationTreeObject.TYPE_OTHER, lineNumber);
									}
									if (handlersDeclarationTreeObject != null) {
										treeObjects.add(handlersDeclarationTreeObject);
									}
								} catch(StringIndexOutOfBoundsException e) {
									throw new EngineException("Exception in reading line of a transaction", e);
								}
							}
							line = br.readLine();
						}

						if (treeObjects.size() != 0) {
							ObjectsFolderTreeObject objectsFolderTreeObject = new ObjectsFolderTreeObject(viewer, ObjectsFolderTreeObject.FOLDER_TYPE_FUNCTIONS);
							databaseObjectTreeObject.addChild(objectsFolderTreeObject);

							for (HandlersDeclarationTreeObject handlersDeclarationTreeObject: treeObjects) {
								objectsFolderTreeObject.addChild(handlersDeclarationTreeObject);
							}
						}

					} else if (databaseObject instanceof Sheet) {
						addTemplates((Sheet) databaseObject, databaseObjectTreeObject);

					} else if (databaseObject instanceof ITablesProperty) {
						ITablesProperty iTablesProperty = (ITablesProperty) databaseObject;
						String[] tablePropertyNames = iTablesProperty.getTablePropertyNames();

						for (int i = 0; i < tablePropertyNames.length; i++) {
							String tablePropertyName = tablePropertyNames[i];
							String tableRenderer = iTablesProperty.getTableRenderer(tablePropertyName);
							XMLVector<XMLVector<Object>> xmlv = iTablesProperty.getTableData(tablePropertyName);
							if (tableRenderer.equals("XMLTableDescriptionTreeObject")) {
								XMLTableDescriptionTreeObject propertyXMLTableTreeObject = new XMLTableDescriptionTreeObject(viewer, tablePropertyName, xmlv, databaseObjectTreeObject);
								databaseObjectTreeObject.addChild(propertyXMLTableTreeObject);
							} else if (tableRenderer.equals("XMLRecordDescriptionTreeObject")) {
								XMLRecordDescriptionTreeObject propertyXMLRecordTreeObject = new XMLRecordDescriptionTreeObject(viewer, tablePropertyName, xmlv, databaseObjectTreeObject);
								databaseObjectTreeObject.addChild(propertyXMLRecordTreeObject);
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
			IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(sheet.getProject().getName());

			IFile file = project.getFile(new Path(xslFileName));
			if (file.exists()) {
				Document doc = parseXslFile(file);

				NodeList nl = doc.getElementsByTagName("xsl:include");
				for (int i=0; i< nl.getLength(); i++) {
					Node node = nl.item(i);
					NamedNodeMap attributes = node.getAttributes();
					Node 	href    = attributes.getNamedItem("href");
					String	name    = href.getNodeValue();
					// do not add includes statring by ../ as there are system includes
					if (!name.startsWith("../")) {
						templateTreeObject = new TemplateTreeObject(viewer, "["+name.substring(name.lastIndexOf('/')+1, name.lastIndexOf('.')) + "]", name);
						treeObject.addChild(templateTreeObject);
					}
				}
			}
		}
		catch (CoreException e) {
			ConvertigoPlugin.logInfo("Error opening Ressources for project '" + sheet.getProject().getName() + "': "+ e.getMessage());
		}
		catch (Exception ee) {
			ConvertigoPlugin.logInfo("Error Parsing XSL file '" + xslFileName + "': "+ ee.getMessage());
		}
	}

	/**
	 * Parses as a DOM the IFile passed in argument ..
	 *
	 * @param 	file to parse
	 * @return 	parsed Document
	 */
	private Document parseXslFile(IFile file) throws Exception
	{
		Document doc;
		doc = XMLUtils.getDefaultDocumentBuilder().parse(new InputSource(file.getContents()));
		return doc;
	}

	public TreeObject findTreeObjectByPath(TreeParent treeParent, String path) {
		TreeObject foundObject = null;

		if (treeParent == null)
			return null;

		String treeParentPath = treeParent.getPath();

		if (treeParentPath.length() > path.length())
			return null;

		if (treeParent instanceof DatabaseObjectTreeObject) {
			if (((DatabaseObjectTreeObject)treeParent).isInherited)
				return null;
		}

		if (treeParent instanceof PropertyTableTreeObject) {
			if (((PropertyTableTreeObject)treeParent).isInherited())
				return null;
		}

		if (treeParentPath.equals(path))
			return treeParent;

		for(TreeObject treeObject : treeParent.getChildren()) {
			if (treeObject instanceof TreeParent) {
				foundObject = findTreeObjectByPath((TreeParent) treeObject, path);
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
		if (ConvertigoPlugin.projectManager.hasProjectExplorerView()) {
			addedTreeObjects.clear();

			//close all opened editors
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
	}

	private ListenerList<ISelectionChangedListener> selectionChangedListeners = new ListenerList<ISelectionChangedListener>();

	public synchronized void addSelectionChangedListener(ISelectionChangedListener listener) {
		selectionChangedListeners.add(listener);
		viewer.addSelectionChangedListener(listener);
	}

	public synchronized void removeSelectionChangedListener(ISelectionChangedListener listener) {
		selectionChangedListeners.remove(listener);
		viewer.removeSelectionChangedListener(listener);
	}

	public synchronized void clearSelectionChangedListeners() {
		Object[] listeners = selectionChangedListeners.getListeners();
		for (int i=0; i<listeners.length; i++) {
			removeSelectionChangedListener((ISelectionChangedListener)listeners[i]);
		}
		selectionChangedListeners.clear();
	}

	//******************************** HELPER METHODS FOR ACTIONS **************************************//

	public void loadSelectedUnloadedProjectTreeObject() {
		//		TreeObject treeObject = getFirstSelectedTreeObject();
		//		if ((treeObject != null) && (treeObject instanceof UnloadedProjectTreeObject))
		//			loadProject((UnloadedProjectTreeObject)treeObject);
		TreeObject[] treeObjects = getSelectedTreeObjects();
		if ((treeObjects != null)) {
			for (TreeObject treeObject :treeObjects) {
				if (treeObject instanceof UnloadedProjectTreeObject) {
					loadProject((UnloadedProjectTreeObject)treeObject);
				}
			}
		}
	}

	public void removeProjectTreeObject(TreeObject treeObject) {
		if ((treeObject != null) && ((treeObject instanceof ProjectTreeObject) || (treeObject instanceof UnloadedProjectTreeObject))) {
			TreeParent invisibleRoot = treeObject.getParent();
			if (treeObject instanceof ProjectTreeObject) {
				ProjectTreeObject projectTreeObject = (ProjectTreeObject)treeObject;
				projectTreeObject.closeAllEditors();
			}
			invisibleRoot.removeChild(treeObject);
			ConvertigoPlugin.asyncExec(() -> viewer.refresh());
		}
	}

	public void closeAllProjects() {
		ViewContentProvider provider = (ViewContentProvider)viewer.getContentProvider();
		if (provider != null) {
			Object[] objects = provider.getChildren(provider.getTreeRoot());
			for (int i=0; i<objects.length; i++) {
				TreeObject treeObject = (TreeObject)objects[i];
				if (treeObject instanceof ProjectTreeObject) {
					ProjectTreeObject projectTreeObject = (ProjectTreeObject)treeObject;
					ConvertigoPlugin.projectManager.setCurrentProject(projectTreeObject);//
					projectTreeObject.close();// close all editors
				}
			}
		}
	}

	private boolean loadedProjectsHaveReferences() {
		ViewContentProvider provider = (ViewContentProvider)viewer.getContentProvider();
		if (provider != null) {
			Object[] objects = provider.getChildren(provider.getTreeRoot());
			for (int i=0; i<objects.length; i++) {
				TreeObject treeObject = (TreeObject)objects[i];
				if (treeObject instanceof ProjectTreeObject) {
					// Check for references on projects
					ProjectTreeObject projectTreeObject = (ProjectTreeObject)treeObject;
					List<Reference> references = ((Project)projectTreeObject.getObject()).getReferenceList();
					if (references.size() > 0) {
						for (Reference reference: references) {
							if (reference instanceof ProjectSchemaReference) {
								return true;
							}
						}
					}
					// Check for sequences (potential call steps)
					List<Sequence> sequences = ((Project)projectTreeObject.getObject()).getSequencesList();
					if (sequences.size() > 0) {
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
			for (TreeObject treeObject :treeObjects) {
				if (treeObject instanceof ProjectTreeObject) {
					unloadProjectTreeObject((ProjectTreeObject)treeObject);
				}
			}
		}
	}

	protected UnloadedProjectTreeObject unloadProjectTreeObject(ProjectTreeObject projectTreeObject) {
		if (projectTreeObject == null) throw new IllegalArgumentException("ProjectExplorerView.unloadProjectTreeObject(): project tree object cannot be null!");
		String projectName = projectTreeObject.getName();
		TreeParent invisibleRoot = projectTreeObject.getParent();
		if (projectTreeObject.close()) {
			UnloadedProjectTreeObject unloadedProjectTreeObject = new UnloadedProjectTreeObject(viewer, projectName);
			invisibleRoot.addChild(unloadedProjectTreeObject);
			invisibleRoot.removeChild(projectTreeObject);
			viewer.refresh();

			try {
				ConvertigoPlugin.getDefault().closeProjectPluginResource(projectName);
			} catch (CoreException e) {
				ConvertigoPlugin.logException(e, "Unable to unload the project '" + projectTreeObject.getName() + "'");
			}

			ConvertigoPlugin.getDefault().refreshPaletteView();

			return unloadedProjectTreeObject;
		}
		return null;
	}

	public void importProjectTreeObject(String projectName) throws CoreException {
		importProjectTreeObject(projectName, false, null);
	}

	public void importProjectTreeObject(String projectName, boolean isCopy, String originalName) throws CoreException {
		TreeParent invisibleRoot = ((ViewContentProvider)viewer.getContentProvider()).getTreeRoot();
		UnloadedProjectTreeObject unloadedProjectTreeObject = new UnloadedProjectTreeObject(viewer, projectName);
		invisibleRoot.addChild(unloadedProjectTreeObject);
		loadProject(unloadedProjectTreeObject, isCopy, originalName);
	}

	public boolean isProjectLoaded(String projectName) {
		boolean bLoaded = false;
		TreeObject treeObject = getFirstSelectedTreeObject();
		if (treeObject != null) {
			TreeParent invisibleRoot = null;
			TreeObject treeParent = treeObject;
			while ((treeParent = treeParent.getParent()) != null)
				invisibleRoot = (TreeParent)treeParent;

			for (TreeObject child : invisibleRoot.getChildren()) {
				if (child.getName().equals(projectName)) {
					bLoaded = child instanceof ProjectTreeObject;
					break;
				}
			}

		}
		return bLoaded;
	}

	public void reloadDatabaseObject(DatabaseObject databaseObject) throws EngineException, IOException {
		DatabaseObjectTreeObject treeObject = (DatabaseObjectTreeObject) findTreeObjectByUserObject(databaseObject);
		treeObject.hasBeenModified(databaseObject.hasChanged);
		reloadTreeObject(treeObject);
	}

	public void reloadFirstSelectedTreeObject() throws EngineException, IOException {
		TreeObject object = getFirstSelectedTreeObject();
		reloadTreeObject(object);
	}

	public void reloadTreeObject(TreeObject object) throws EngineException, IOException {
		if (object != null) {
			if (object instanceof DatabaseObjectTreeObject) {
				reload((TreeParent) object, (DatabaseObject) object.getObject());
			} else {
				reloadTreeObject(object.getParent());
			}
		}
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

	public void refreshSelectedTreeObjects() {
		ISelection selection = viewer.getSelection();
		if(!selection.isEmpty()) viewer.setSelection(selection, false);
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
		if(!selection.isEmpty()) viewer.setSelection(selection,true);
	}

	public void refreshTreeObject(TreeObject object, boolean bRecurse) {
		if (object != null) {
			if (bRecurse && (object instanceof TreeParent))
				for (TreeObject child : ((TreeParent)object).getChildren())
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
			viewer.update(object,null);
		}
	}

	public void updateDatabaseObject(DatabaseObject databaseObject) {
		DatabaseObjectTreeObject treeObject = (DatabaseObjectTreeObject) findTreeObjectByUserObject(databaseObject);
		updateTreeObject(treeObject);
	}

	public TreeParent getDatabaseObjectTreeParent(TreeObject treeObject) {
		TreeParent treeParent = null;
		if (treeObject != null) {
			treeParent = treeObject.getParent();
			while (!(treeParent instanceof DatabaseObjectTreeObject) && (treeParent != null))
				treeParent = treeParent.getParent();
			if (treeParent == null)
				treeParent = (TreeParent)treeObject;
		}
		return treeParent;
	}

	public TreeObject getFirstSelectedTreeObject() {
		ISelection selection = viewer.getSelection();
		IStructuredSelection structuredSelection = (IStructuredSelection) selection;
		TreeObject selectedTreeObject = (TreeObject) structuredSelection.getFirstElement();
		return selectedTreeObject;
	}

	public DatabaseObjectTreeObject getFirstSelectedDatabaseObjectTreeObject(TreeObject selection){
		while((selection!=null) && !(selection instanceof DatabaseObjectTreeObject))
			selection = selection.getParent();
		return (DatabaseObjectTreeObject) selection;
	}

	/*public DatabaseObjectTreeObject getFirstSelectedDatabaseObjectTreeObject(){
		TreeObject selection = getFirstSelectedTreeObject();
		return getFirstSelectedDatabaseObjectTreeObject(selection);
	}*/

	public void setSelectedTreeObject(TreeObject object) {
		StructuredSelection structuredSelection = new StructuredSelection(object);
		viewer.setSelection(structuredSelection);
	}

	public TreeObject[] getSelectedTreeObjects() {
		TreeObject[] treeObjects = null;
		ISelection selection = viewer.getSelection();
		if (!selection.isEmpty()) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			Object[] treeArray = structuredSelection.toArray();
			treeObjects = new TreeObject[structuredSelection.size()];
			for (int i = 0; i < treeObjects.length; i++) {
				treeObjects[i] = ((TreeObject) treeArray[i]).check();
			}
		}
		return treeObjects;
	}

	public void setSelectedTreeObjects(TreeObject[] treeObjects) {
		if ((treeObjects != null) && (treeObjects.length > 0)) {
			StructuredSelection structuredSelection = new StructuredSelection(treeObjects);
			viewer.setSelection(structuredSelection);
		}
	}

	public Object[] getSelectedDatabaseObjects() {
		Object[] databaseObjects = null;
		TreeObject[] treeObjects = getSelectedTreeObjects();
		if (treeObjects != null) {
			int len = treeObjects.length;
			databaseObjects = new Object[len];
			for (int i=0; i<len; i++)
				databaseObjects[i] = ((TreeObject)treeObjects[i]).getObject();
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
		renameSelectedTreeObject(false);
	}
	
	public void renameSelectedTreeObject(boolean newObject) {
		TreeObject treeObject = getFirstSelectedTreeObject();
		if ((treeObject != null) &&
				((treeObject instanceof DatabaseObjectTreeObject) ||
						(treeObject instanceof TraceTreeObject) ||
						(treeObject instanceof DesignDocumentViewTreeObject) ||
						(treeObject instanceof DesignDocumentFilterTreeObject) ||
						(treeObject instanceof DesignDocumentUpdateTreeObject)))
		{
			edit(treeObject, newObject);
		}
	}

	private DatabaseObjectTreeObject findTreeObjectByUserObjectFromCache(DatabaseObject databaseObject) {
		DatabaseObjectTreeObject databaseObjectTreeObject = databaseObjectTreeObjectCache.get(databaseObject);
		if (databaseObjectTreeObject != null) {
			if (databaseObjectTreeObject.getObject().equals(databaseObject) && databaseObjectTreeObject.parent != null) {
				return databaseObjectTreeObject;
			} else {
				databaseObjectTreeObjectCache.remove(databaseObject);
			}
		}
		return null;
	}

	private DatabaseObjectTreeObject findTreeObjectByUserObject(DatabaseObject databaseObject, ProjectTreeObject projectTreeObject) {
		DatabaseObjectTreeObject databaseObjectTreeObject = null;
		if (projectTreeObject.getObject().equals(databaseObject)) {
			databaseObjectTreeObject = projectTreeObject;
		} else {
			DatabaseObject parentDatabaseObject = databaseObject.getParent();
			if (parentDatabaseObject != null) {
				DatabaseObjectTreeObject parentDatabaseObjectTreeObject = findTreeObjectByUserObjectFromCache(parentDatabaseObject);
				if (parentDatabaseObjectTreeObject == null) {
					parentDatabaseObjectTreeObject = findTreeObjectByUserObject(parentDatabaseObject, projectTreeObject);
				}
				if (parentDatabaseObjectTreeObject != null) {
					databaseObjectTreeObject = parentDatabaseObjectTreeObject.findDatabaseObjectTreeObjectChild(databaseObject);
				}
			}
		}
		if (databaseObjectTreeObject != null) {
			databaseObjectTreeObjectCache.put(databaseObject, databaseObjectTreeObject);
		}
		return databaseObjectTreeObject;
	}

	public TreeObject findTreeObjectByUserObject(DatabaseObject databaseObject) {
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

			if (databaseProject == null) {
				return null;
			}

			ViewContentProvider provider = (ViewContentProvider) viewer.getContentProvider();
			if (provider != null) {
				Object[] objects = provider.getChildren(provider.getTreeRoot());
				for (int i = 0; i < objects.length; i++) {
					TreeObject treeObject = (TreeObject) objects[i];
					if (treeObject instanceof ProjectTreeObject) {
						ProjectTreeObject projectTreeObject = (ProjectTreeObject) treeObject;
						Project project = projectTreeObject.getObject();
						if (project.getName().equals(databaseProject.getName())) {
							return isProject ? projectTreeObject : findTreeObjectByUserObject(databaseObject, projectTreeObject);
						}
					}
					else if (treeObject instanceof UnloadedProjectTreeObject) {
						UnloadedProjectTreeObject unloadedProjectTreeObject = (UnloadedProjectTreeObject) treeObject;

						if (unloadedProjectTreeObject.getName().equals(databaseProject.getName())) {
							TreeParent parent = unloadedProjectTreeObject.getParent();
							String path = unloadedProjectTreeObject.getPath();

							return findTreeObjectByPath(parent, path);
						}
					}
				}
			}
		}
		return null;
	}

	public static int getTreeObjectType(TreePath path) {
		TreeObject treeNode = (TreeObject)path.getLastPathComponent();
		return getTreeObjectType(treeNode);
	}

	public static int getTreeObjectType(TreeObject treeNode) {
		if (treeNode instanceof ObjectsFolderTreeObject) {
			int folderType = ((ObjectsFolderTreeObject)treeNode).folderType;

			if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_POOLS) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_POOLS;
			}
			else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_CONNECTORS) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_CONNECTORS;
			}
			else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_TRANSACTIONS) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_TRANSACTIONS;
			}
			else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_SHEETS) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_SHEETS;
			}
			else if ((folderType == ObjectsFolderTreeObject.FOLDER_TYPE_SCREEN_CLASSES) || (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_INHERITED_SCREEN_CLASSES)) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_SCREEN_CLASSES;
			}
			else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_EXTRACTION_RULES) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_EXTRACTION_RULES;
			}
			else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_CRITERIAS) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_CRITERIAS;
			}
			else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_SEQUENCES) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_SEQUENCES;
			}
			else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_STEPS) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_STEPS;
			}
			else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_VARIABLES) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_VARIABLES;
			}
			else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_TESTCASES) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_TESTCASES;
			}
			else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_DOCUMENTS) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_DOCUMENTS;
			}
			else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_LISTENERS) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_LISTENERS;
			}
			else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_AUTHENTICATIONS) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_AUTHENTICATIONS;
			}
			else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_MAPPINGS) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_MAPPINGS;
			}
			else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_OPERATIONS) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_OPERATIONS;
			}
			else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_PARAMETERS) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_PARAMETERS;
			}
			else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_EVENTS) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_EVENTS;
			}
			else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_ACTIONS) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_ACTIONS;
			}
			else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_ROUTES) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_ROUTES;
			}
			else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_CONTROLS) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_CONTROLS;
			}
			else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_SOURCES) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_SOURCES;
			}
			else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_STYLES) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_STYLES;
			}
			else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_ATTRIBUTES) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_ATTRIBUTES;
			}
			else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_VALIDATORS) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_VALIDATORS;
			}
			else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_MENUS) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_MENUS;
			}
			else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_PLATFORMS) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_MOBILEPLATFORMS;
			}
			else if (folderType == ObjectsFolderTreeObject.FOLDER_TYPE_INDEXES) {
				return ProjectExplorerView.TREE_OBJECT_TYPE_FOLDER_INDEXES;
			}
		}
		else if (treeNode instanceof HandlersDeclarationTreeObject) {
			return ProjectExplorerView.TREE_OBJECT_TYPE_HANDLERS_DECLARATION;
		}
		else if (treeNode instanceof VariableTreeObject) {
			return ProjectExplorerView.TREE_OBJECT_TYPE_VARIABLE;
		}
		else if (treeNode instanceof VariableTreeObject2) {
			return ProjectExplorerView.TREE_OBJECT_TYPE_VARIABLE;
		}
		else if (treeNode instanceof IPropertyTreeObject) {
			int result = 0;

			if (treeNode instanceof PropertyTableTreeObject) {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_PROPERTY_TABLE;
			}
			else if (treeNode instanceof PropertyTableRowTreeObject) {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_PROPERTY_TABLE_ROW;
			}
			else if (treeNode instanceof PropertyTableColumnTreeObject) {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_PROPERTY_TABLE_COLUMN;
			}

			if (((IPropertyTreeObject)treeNode).isInherited()) {
				result |= ProjectExplorerView.TREE_OBJECT_TYPE_DBO_INHERITED;
			}

			return result;
		}
		else if (treeNode instanceof DatabaseObjectTreeObject) {
			int result = 0;

			DatabaseObject databaseObject = (DatabaseObject) treeNode.getObject();

			result = getDatabaseObjectType(databaseObject);

			if (((DatabaseObjectTreeObject)treeNode).isInherited) {
				result |= ProjectExplorerView.TREE_OBJECT_TYPE_DBO_INHERITED;
			}

			return result;
		}

		return ProjectExplorerView.TREE_OBJECT_TYPE_UNKNOWN;
	}

	public static int getDatabaseObjectType(DatabaseObject databaseObject) {
		int result = 0;
		if (databaseObject instanceof Project) {
			result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_PROJECT;
		}
		else if (databaseObject instanceof Connector) {
			result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_CONNECTOR;
		}
		else if (databaseObject instanceof Sequence) {
			result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_SEQUENCE;
		}
		else if (databaseObject instanceof MobileApplication) {
			result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_MOBILEAPPLICATION;
		}
		else if (databaseObject instanceof MobilePlatform) {
			result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_MOBILEPLATFORM;
		}
		/**************************************************************************************************/
		/***                 com.twinsoft.convertigo.beans.mobile.components                           ****/
		/**************************************************************************************************/
		else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.MobileComponent) {
			if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent) {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_MOBILE_APPLICATIONCOMPONENT;
			}
			else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.RouteComponent) {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_MOBILE_ROUTECOMPONENT;
			}
			else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.RouteEventComponent) {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_MOBILE_ROUTEEVENTCOMPONENT;
			}
			else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.RouteActionComponent) {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_MOBILE_ROUTEACTIONCOMPONENT;
			}
			else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.PageComponent) {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_MOBILE_PAGECOMPONENT;
			}
			else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIDynamicMenu)
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_MB_MENU;
			else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIStyle)
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_MB_STYLE;
			else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIComponent) {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_MOBILE_UICOMPONENT;
			}
		}
		/**************************************************************************************************/
		/***                 com.twinsoft.convertigo.beans.ngx.components                              ****/
		/**************************************************************************************************/
		else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.MobileComponent) {
			if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent) {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_MOBILE_APPLICATIONCOMPONENT;
			}
			else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.PageComponent) {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_MOBILE_PAGECOMPONENT;
			}
			else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIDynamicMenu)
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_MB_MENU;
			else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIStyle)
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_MB_STYLE;
			else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIComponent) {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_MOBILE_UICOMPONENT;
			}
		}
		else if (databaseObject instanceof UrlMapper) {
			result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_URLMAPPER;
		}
		else if (databaseObject instanceof UrlMapping) {
			result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_URLMAPPING;
		}
		else if (databaseObject instanceof UrlMappingOperation) {
			result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_URLMAPPINGOPERATION;
		}
		else if (databaseObject instanceof UrlMappingParameter) {
			result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_URLMAPPINGPARAMETER;
		}
		else if (databaseObject instanceof UrlMappingResponse) {
			result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_URLMAPPINGRESPONSE;
		}
		else if (databaseObject instanceof Criteria) {
			result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_CRITERIA;
		}
		else if (databaseObject instanceof ExtractionRule) {
			result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_EXTRACTION_RULE;
		}
		else if (databaseObject instanceof Pool) {
			result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_POOL;
		}
		else if (databaseObject instanceof Transaction) {
			result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_TRANSACTION;
		}
		else if (databaseObject instanceof BlockFactory) {
			result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_BLOCK_FACTORY;
		}
		else if (databaseObject instanceof Sheet) {
			result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_SHEET;
		}
		else if (databaseObject instanceof StatementWithExpressions) {
			result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_STATEMENT_WITH_EXPRESSIONS;
			if (databaseObject instanceof FunctionStatement) {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_FUNCTION;
			}
		}
		else if (databaseObject instanceof Statement) {
			result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_STATEMENT;
		}
		else if (databaseObject instanceof FunctionStep) {
			result = ProjectExplorerView.TREE_OBJECT_TYPE_FUNCTION;
		}
		else if (databaseObject instanceof Step) {
			result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_STEP;
		}
		else if (databaseObject instanceof TestCase) {
			result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_TESTCASE;
		}
		else if (databaseObject instanceof ScreenClass) {
			if (databaseObject.getParent() instanceof Project) {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_ROOT_SCREEN_CLASS;
			}
			else {
				result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_SCREEN_CLASS;
			}
		}
		else if (databaseObject instanceof com.twinsoft.convertigo.beans.core.Document) {
			result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_DOCUMENT;
		}
		else if (databaseObject instanceof com.twinsoft.convertigo.beans.core.Listener) {
			result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_LISTENER;
		}
		else if (databaseObject instanceof com.twinsoft.convertigo.beans.core.Reference) {
			result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_REFERENCE;
		}
		else if (databaseObject instanceof com.twinsoft.convertigo.beans.core.Variable) {
			result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_VARIABLE;
		}
		else if (databaseObject instanceof com.twinsoft.convertigo.beans.core.Index) {
			result = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_INDEX;
		}
		return result;
	}

	public TreePath[] getSelectionPaths() {
		TreePath[] treePaths = null;
		TreeObject treeObject = null;
		TreePath treePath = null;

		TreeObject[] treeObjects = getSelectedTreeObjects();
		if (treeObjects != null) {
			int len = treeObjects.length;
			treePaths = new TreePath[len];
			for (int i=0;i<len;i++) {
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
		((UndoAction)undoAction).update();
		((RedoAction)redoAction).update();
	}

	public void addUndoableEdit(UndoableEdit edit) {
		undoManager.addEdit(edit);
		updateUndoRedo();
	}

	public Project getProject(String projectName) throws EngineException {
		Project project = ((ViewContentProvider) viewer.getContentProvider()).getProject(projectName);
		return project;
	}

	public TreeObject getProjectRootObject(String projectName) throws EngineException {
		return ((ViewContentProvider) viewer.getContentProvider()).getProjectRootObject(projectName);
	}

	public Collection<ProjectTreeObject> getOpenedProjects() {
		return ((ViewContentProvider) viewer.getContentProvider()).getOpenedProjects();
	}

	/**
	 * Gets the BeanInfo corresponding to the first selected dababase Object
	 * @return BeanInfo
	 */
	public BeanInfo getFirstSelectedDatabaseObjectBeanInfo() {
		BeanInfo databaseObjectBeanInfo;

		Object obj = ConvertigoPlugin.getDefault().getProjectExplorerView().getFirstSelectedDatabaseObject();
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
			Class<? extends DatabaseObject> beanClass = GenericUtils.cast(Class.forName(beanClassName));
			databaseObjectBeanInfo = CachedIntrospector.getBeanInfo(beanClass);
			return databaseObjectBeanInfo;
		} catch (Exception e) {
			String message = "Error while introspecting object " + databaseObject.getName() + " (" + databaseObject.getQName() + ")";
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
				screenClassTreeObject = (ScreenClassTreeObject)findTreeObjectByUserObject(lastDetectedScreenClass);
			}
		}
		return screenClassTreeObject;
	}

	public ScreenClass getLastDetectedScreenClass() {
		return lastDetectedScreenClass;
	}

	public void objectDetected(EngineEvent engineEvent) {
		final Object source = engineEvent.getSource();
		boolean highlightDetectedObject = ConvertigoPlugin.getHighlightDetectedObject();

		if (source instanceof Step) {
			try {
				highlightDetectedObject = Boolean.TRUE.equals(RequestAttribute.debug.get(((Step) source).getSequence().context.httpServletRequest));
			} catch (Exception e) {
				// silently ignore
			}
		}

		if (highlightDetectedObject) {
			if (source instanceof DatabaseObject) {
				ConvertigoPlugin.syncExec(() -> {
					DatabaseObjectTreeObject databaseTreeObject = (DatabaseObjectTreeObject) findTreeObjectByUserObject((DatabaseObject)source);
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
							lastDetectedScreenClass = (ScreenClass)source;
							lastDetectedScreenClassTreeObject = (ScreenClassTreeObject)databaseTreeObject;
						}
					}
				});
			}
		}
		else {
			if (source instanceof ScreenClass) {
				lastDetectedScreenClass = (ScreenClass)source;
			}
		}
	}

	public void objectChanged(CompositeEvent compositeEvent) {
		final Object data = compositeEvent.data;
		final Object source = compositeEvent.getSource();
		if (source instanceof DatabaseObject) {
			ConvertigoPlugin.syncExec(() -> {
				DatabaseObjectTreeObject databaseObjectTreeObject = (DatabaseObjectTreeObject) findTreeObjectByUserObject((DatabaseObject)source);
				try {
					reloadTreeObject(databaseObjectTreeObject);

					if ((data != null) && (data instanceof String)) {
						// case of learned Javelin transaction, expand to see newly added handlers
						if (databaseObjectTreeObject instanceof TransactionTreeObject) {
							viewer.expandToLevel(databaseObjectTreeObject, 2);
						}

						// case of we need to select a treeObject given its path
						TreeObject treeObjectToSelect = findTreeObjectByPath(databaseObjectTreeObject, (String)data);
						if (treeObjectToSelect != null) {
							viewer.expandToLevel(treeObjectToSelect, 0);
							setSelectedTreeObject(treeObjectToSelect);

							StructuredSelection structuredSelection = new StructuredSelection(treeObjectToSelect);
							ConvertigoPlugin.getDefault().getPropertiesView().selectionChanged((IWorkbenchPart)ProjectExplorerView.this, structuredSelection);
						}
					}
				} catch (EngineException e) {
					ConvertigoPlugin.logException(e, "Unexpected exception");
				} catch (IOException e) {
					ConvertigoPlugin.logException(e, "Unexpected exception");
				}
			});
		}
	}

	public void objectSelected(CompositeEvent compositeEvent) {
		final Object source = compositeEvent.getSource();
		if (source instanceof DatabaseObject) {
			ConvertigoPlugin.syncExec(() -> {
				DatabaseObjectTreeObject databaseTreeObject = (DatabaseObjectTreeObject) findTreeObjectByUserObject((DatabaseObject)source);
				if (databaseTreeObject != null) {
					viewer.expandToLevel(databaseTreeObject, 0);
					setSelectedTreeObject(databaseTreeObject);

					StructuredSelection structuredSelection = new StructuredSelection(databaseTreeObject);
					ConvertigoPlugin.getDefault().getPropertiesView().selectionChanged((IWorkbenchPart)ProjectExplorerView.this, structuredSelection);
				}
			});
		}
	}

	public void documentGenerated(EngineEvent engineEvent) {
		ConvertigoPlugin.syncExec(() -> {
			if (lastDetectedDatabaseObjectTreeObject != null) {
				lastDetectedDatabaseObjectTreeObject.isDetectedObject = false;
				updateTreeObject(lastDetectedDatabaseObjectTreeObject);
			}
			lastDetectedDatabaseObjectTreeObject = null;
			lastDetectedScreenClassTreeObject = null;
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
		final String projectName = (String)engineEvent.getSource();
		if (projectName != null) {
			ConvertigoPlugin.asyncExec(() -> {
				ConvertigoPlugin.logDebug("[ProjectExplorerView] event 'projectMigrated' received for project "+projectName);
				((ViewContentProvider) viewer.getContentProvider()).loadProject(projectName);
				viewer.refresh();
			});
		}
	}

	public void migrationFinished(EngineEvent engineEvent) {
		ConvertigoPlugin.asyncExec(() -> {
			ConvertigoPlugin.logDebug("[ProjectExplorerView] event 'migrationFinished' received");
			if (!Engine.isStudioMode()) {
				Engine.theApp.referencedProjectManager.check();
			}
			refreshProjects();
			refreshTree();
		});
	}

	public boolean importProject(String filePath, String targetProjectName) throws EngineException, IOException, CoreException {
		TreeObject[] projectTreeObject = {null};
		if (targetProjectName != null) {
			projectTreeObject[0] = ((ViewContentProvider) viewer.getContentProvider()).getProjectRootObject(targetProjectName);
		}

		// if project already exists, backup it and delete it after
		if (projectTreeObject[0] != null) {
			if (Engine.isProjectFile(filePath)) {
				DatabaseObjectsManager.deleteDir(new File(Engine.projectDir(targetProjectName) + "/_data"));
				DatabaseObjectsManager.deleteDir(new File(Engine.projectDir(targetProjectName) + "/_private"));
			}
			if (projectTreeObject[0] instanceof ProjectTreeObject) {
				projectTreeObject[0] = unloadProjectTreeObject((ProjectTreeObject) projectTreeObject[0]);
			}
		}

		ConvertigoPlugin.logInfo("Import project from file \"" + filePath + "\"");

		boolean doImport;
		if (Engine.isProjectFile(filePath)) {
			ConvertigoPlugin.getDefault().declareProject(targetProjectName, new File(filePath));
			doImport = true;
		} else if (((filePath.endsWith(".car") || filePath.endsWith(".zip")) && (targetProjectName != null)) ||
				filePath.matches("https?://.+")) {
			doImport = false;
		} else {
			return false;
		}

		Engine.execute(() -> {
			Exception[] exception = {null};
			try {
				Project importedProject = doImport ?
						Engine.theApp.databaseObjectsManager.importProject(filePath, true) :
							Engine.theApp.databaseObjectsManager.deployProject(filePath, targetProjectName, true);
				ConvertigoPlugin.syncExec(() -> {
					try {
						// project's name may have been changed because of non-normalized name (fix ticket #788 : Cannot import project 213.car)
						String projectName = importedProject.getName();

						// loads project into tree view
						if (projectTreeObject[0] == null) {
							importProjectTreeObject(projectName);
						} else {
							//							// recreate project resource
							ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);
							reloadProject(projectTreeObject[0]);
						}

						refreshTree();
					} catch (Exception e) {
						exception[0] = e;
					}
				});
			} catch (Exception e) {
				exception[0] = e;
			}
			if (exception[0] != null) {
				ConvertigoPlugin.syncExec(() -> {
					Engine.logStudio.error("Failed to import project", exception[0]);
					ConvertigoPlugin.errorMessageBox("Failed to import project [" + exception[0].getClass().getSimpleName() + "]: " + exception[0].getMessage());
				});
			}
		});

		return true;
	}

	public Comparator<TreeObject> getViewerComparator() {
		Comparator<TreeObject> comparator = null;
		ViewerComparator sorter = viewer != null ? viewer.getComparator() : new ViewerComparator();
		comparator = new Comparator<TreeObject>() {
			@Override
			public int compare(TreeObject o1, TreeObject o2) {
				return sorter.compare(viewer, o1, o2);
			}
		};
		return comparator;
	}

	public void moveChildTo(TreeParent parent, TreeObject src, TreeObject target, boolean insertBefore) {
		List<? extends TreeObject> children = parent.getChildren();
		int destPosition = children.indexOf(target);
		int srcPosition = src != null ? children.indexOf(src) : (children.size() - 1);
		if (destPosition != -1 && srcPosition != -1) {
			int delta = destPosition - srcPosition;
			int count = (delta < 0) ? (insertBefore ? delta : delta + 1)
					: (insertBefore ? delta - 1 : delta);
			setSelectedTreeObject(children.get(srcPosition));
			if (count != 0) {
				if (count < 0) {
					new DatabaseObjectIncreasePriorityAction(Math.abs(count)).run();
				} else {
					new DatabaseObjectDecreasePriorityAction(Math.abs(count)).run();
				}
			}
		}
	}

	public void moveLastTo(TreeParent parent, TreeObject target, boolean insertBefore) {
		moveChildTo(parent, null, target, insertBefore);
	}

	public static boolean folderAcceptMobileComponent(int folderType, DatabaseObject databaseObject) {
		/**************************************************************************************************/
		/***                 com.twinsoft.convertigo.beans.mobile.components                           ****/
		/**************************************************************************************************/
		if (databaseObject != null && databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.MobileComponent) {
			switch (folderType) {
			case ObjectsFolderTreeObject.FOLDER_TYPE_ACTIONS:
				return databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.RouteActionComponent;
			case ObjectsFolderTreeObject.FOLDER_TYPE_ATTRIBUTES:
				return databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIAttribute &&
						!(databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIControlAttr);
			case ObjectsFolderTreeObject.FOLDER_TYPE_CONTROLS:
				return databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIControlAttr;
			case ObjectsFolderTreeObject.FOLDER_TYPE_EVENTS:
				return databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIAppEvent ||
						databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIPageEvent ||
						databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIEventSubscriber ||
						databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.RouteEventComponent ;
			case ObjectsFolderTreeObject.FOLDER_TYPE_MENUS:
				return databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIDynamicMenu;
			case ObjectsFolderTreeObject.FOLDER_TYPE_PAGES:
				return databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.PageComponent;
			case ObjectsFolderTreeObject.FOLDER_TYPE_ROUTES:
				return databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.RouteComponent;
			case ObjectsFolderTreeObject.FOLDER_TYPE_SHARED_ACTIONS:
				return databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIActionStack;
			case ObjectsFolderTreeObject.FOLDER_TYPE_SHARED_COMPONENTS:
				return databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UISharedComponent;
			case ObjectsFolderTreeObject.FOLDER_TYPE_STYLES:
				return databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIStyle;
			case ObjectsFolderTreeObject.FOLDER_TYPE_VALIDATORS:
				return databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIFormValidator;
			case ObjectsFolderTreeObject.FOLDER_TYPE_VARIABLES:
				return databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIStackVariable ||
						databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UICompVariable ||
						databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIControlVariable;
			}
		}
		/**************************************************************************************************/
		/***                 com.twinsoft.convertigo.beans.ngx.components                              ****/
		/**************************************************************************************************/
		if (databaseObject != null && databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.MobileComponent) {
			switch (folderType) {
			case ObjectsFolderTreeObject.FOLDER_TYPE_ACTIONS:
				//return databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.RouteActionComponent;
			case ObjectsFolderTreeObject.FOLDER_TYPE_ATTRIBUTES:
				return (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIAttribute ||
						databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIDynamicAttr) &&
						!(databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIControlAttr);
			case ObjectsFolderTreeObject.FOLDER_TYPE_CONTROLS:
				return databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIControlAttr;
			case ObjectsFolderTreeObject.FOLDER_TYPE_EVENTS:
				return databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIAppEvent ||
						databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIPageEvent ||
						databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UISharedComponentEvent ||
						databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIEventSubscriber;
			case ObjectsFolderTreeObject.FOLDER_TYPE_MENUS:
				return databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIDynamicMenu;
			case ObjectsFolderTreeObject.FOLDER_TYPE_PAGES:
				return databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.PageComponent;
			case ObjectsFolderTreeObject.FOLDER_TYPE_SHARED_ACTIONS:
				return databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIActionStack;
			case ObjectsFolderTreeObject.FOLDER_TYPE_SHARED_COMPONENTS:
				return databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UISharedComponent;
			case ObjectsFolderTreeObject.FOLDER_TYPE_STYLES:
				return databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIStyle;
			case ObjectsFolderTreeObject.FOLDER_TYPE_VARIABLES:
				return databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIStackVariable ||
						databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UICompVariable ||
						databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIControlVariable;
			}
		}
		return false;
	}
}