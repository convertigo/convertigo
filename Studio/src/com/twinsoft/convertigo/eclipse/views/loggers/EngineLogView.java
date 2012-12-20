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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/branches/6.2.x/Studio/src/com/twinsoft/convertigo/eclipse/views/projectexplorer/ClipboardManager2.java $
 * $Author: nicolasa $
 * $Revision: 31165 $
 * $Date: 2012-07-20 17:45:54 +0200 (ven., 20 juil. 2012) $
 */

package com.twinsoft.convertigo.eclipse.views.loggers;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.actions.RetargetAction;
import org.eclipse.ui.part.ViewPart;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dialogs.EventDetailsDialog;
import com.twinsoft.convertigo.eclipse.dialogs.EventDetailsDialogComposite;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.logmanager.LogManager;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class EngineLogView extends ViewPart {
	private Thread logViewThread;
	private LogManager logManager;
	private List<LogLine> logLines = new LinkedList<LogLine>();
	private Appender appender;
	private int counter = 0;
	private boolean scrollLock = false;

	private Action clearLogsAction, restoreDefaultsAction, selectColumnsAction;
	private RetargetAction scrollLockAction, optionsAction, searchAction;

	private EngineLogViewLabelProvider labelProvider;

	private static final String PREFS_COLUMN_INFOS = "EngineLogView.column_infos.";
	private static final String PREFS_COLUMN_ORDER = "EngineLogView.column_order";

	private static final ColumnInfo[] DEFAULT_COLUMN_INFOS = { new ColumnInfo("Time", true, 180),
			new ColumnInfo("Message", true, 400), new ColumnInfo("Level", false, 50),
			new ColumnInfo("Category", true, 80), new ColumnInfo("Thread", true, 180),
			new ColumnInfo("Project", true, 70), new ColumnInfo("Connector", true, 70),
			new ColumnInfo("Transaction", true, 70), new ColumnInfo("Sequence", true, 70),
			new ColumnInfo("ContextID", true, 160), new ColumnInfo("UID", false, 50),
			new ColumnInfo("User", false, 50), new ColumnInfo("ClientIP", false, 50),
			new ColumnInfo("ClientHostName", false, 50) };

	private static final int[] DEFAULT_COLUMN_ORDER = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13 };

	private Menu tableMenu;
	private MenuItem addVariableItem;

	private Text filterText, searchText;
	private Label infoSearch;

	private TableViewer tableViewer;
	private int selectedColumnIndex;

	private IPreferenceStore preferenceStore;

	public EngineLogView() {
		preferenceStore = ConvertigoPlugin.getDefault().getPreferenceStore();

		labelProvider = new EngineLogViewLabelProvider();
		appender = new AppenderSkeleton() {

			@Override
			protected void append(LoggingEvent arg0) {
				synchronized (this) {
					this.notifyAll();
				}
			}

			public void close() {
			}

			public boolean requiresLayout() {
				return false;
			}
		};
	}

	@Override
	public void dispose() {
		Engine.logConvertigo.removeAppender(appender);

		logViewThread = null;

		// Notify the log viewer thread possibly waiting on the appender lock
		synchronized (appender) {
			appender.notifyAll();
		}

		super.dispose();
	}

	private Composite mainComposite;

	@Override
	public void createPartControl(Composite parent) {
		mainComposite = parent;

		GridLayout layout = new GridLayout(1, false);
		parent.setLayout(layout);
		layout.marginWidth = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;

		// Options composite
		createOptions(parent);

		// Search composite
		createSearch(parent);

		// Table viewer composite
		createTableViewer(parent);

		createActions();
		createToolbar();

		createLogViewThread();
	}

	/*
	 * Options
	 */

	private Composite compositeOptions;

	private void createOptions(Composite parent) {
		compositeOptions = new Composite(parent, SWT.NONE);
		compositeOptions.setVisible(false);

		GridData layoutData;

		layoutData = new GridData();
		layoutData.horizontalAlignment = SWT.FILL;
		layoutData.grabExcessHorizontalSpace = true;
		layoutData.exclude = true;
		compositeOptions.setLayoutData(layoutData);

		GridLayout layout = new GridLayout(4, false);
		compositeOptions.setLayout(layout);
		layout.marginWidth = 3;

		// Button selectColumns = new Button(compositeOptions, SWT.NONE);
		// selectColumns.setText("Selected columns");
		// final Menu selectedColumnsMenu = new Menu(selectColumns);
		// selectColumns.addSelectionListener(new SelectionAdapter() {
		// @Override
		// public void widgetSelected(SelectionEvent e) {
		// // Position the menu below and vertically aligned with the
		// // the drop down button.
		// final Button button = (Button) e.widget;
		//
		// Point point = button.toDisplay(new Point(e.x, e.y));
		// selectedColumnsMenu.setLocation(point.x, point.y);
		// selectedColumnsMenu.setVisible(true);
		// }
		// });
		//
		Label label = new Label(compositeOptions, SWT.NONE);
		label.setText("Filter");

		filterText = new Text(compositeOptions, SWT.BORDER);
		layoutData = new GridData();
		layoutData.horizontalAlignment = SWT.FILL;
		layoutData.grabExcessHorizontalSpace = true;
		filterText.setLayoutData(layoutData);
		filterText.addListener(SWT.DefaultSelection, new Listener() {
			public void handleEvent(Event e) {
				clearLogs();
				setLogFilter();
			}
		});
	}

	private void setLogFilter() {
		try {
			logManager.setFilter(filterText.getText());
		} catch (ServiceException e) {
			ConvertigoPlugin.logException(e, "Unable to apply the filter", true);
		}
	}

	/*
	 * Search
	 */

	private Button previousSearch, nextSearch;
	private Composite compositeSearch;

	private void createSearch(Composite parent) {
		compositeSearch = new Composite(parent, SWT.NONE);
		compositeSearch.setVisible(false);

		GridData layoutData;

		layoutData = new GridData();
		layoutData.horizontalAlignment = SWT.FILL;
		layoutData.grabExcessHorizontalSpace = true;
		layoutData.exclude = true;
		compositeSearch.setLayoutData(layoutData);

		GridLayout layout = new GridLayout(5, false);
		compositeSearch.setLayout(layout);
		layout.marginWidth = 3;

		Label label = new Label(compositeSearch, SWT.NONE);
		label.setText("Search");

		searchText = new Text(compositeSearch, SWT.BORDER);
		layoutData = new GridData();
		layoutData.horizontalAlignment = SWT.FILL;
		layoutData.grabExcessHorizontalSpace = true;
		searchText.setLayoutData(layoutData);

		infoSearch = new Label(compositeSearch, SWT.NONE);
		infoSearch.setVisible(false);

		previousSearch = new Button(compositeSearch, SWT.NONE);
		previousSearch.setText(" < ");
		previousSearch.setEnabled(false);

		nextSearch = new Button(compositeSearch, SWT.NONE);
		nextSearch.setText(" > ");
		nextSearch.setEnabled(false);

		searchText.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				applySearch();
			}
		});

		nextSearch.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				searchInLogs(1);
			}
		});

		previousSearch.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				searchInLogs(-1);
			}
		});
	}

	private int currentFoundIndex = 0;
	private ArrayList<Integer> foundIndexes = new ArrayList<Integer>();

	private void applySearch() {
		foundIndexes.clear();
		for (int i = 0; i < tableViewer.getTable().getItemCount(); i++) {
			if (tableViewer.getTable().getItem(i).getText(0).contains(searchText.getText()))
				foundIndexes.add(i);
		}
		if (foundIndexes.isEmpty())
			infoSearch.setText("0 / 0");
		else {
			infoSearch.setText((currentFoundIndex + 1) + "/" + foundIndexes.size());
			// On the first match
			tableViewer.getTable().setSelection(foundIndexes.get(currentFoundIndex));
			tableViewer.getTable().setFocus();
			nextSearch.setEnabled(true);
		}
		infoSearch.setVisible(true);
		compositeSearch.layout();
	}

	private void searchInLogs(int side) {
		currentFoundIndex += side;
		tableViewer.getTable().setSelection(foundIndexes.get(currentFoundIndex));

		// Disable "second" if is the beginning
		if (foundIndexes.get(currentFoundIndex) == foundIndexes.get(0))
			previousSearch.setEnabled(false);
		else
			previousSearch.setEnabled(true);

		// Disable "first" if is the end
		if (foundIndexes.get(currentFoundIndex) == foundIndexes.get(foundIndexes.size() - 1))
			nextSearch.setEnabled(false);
		else
			nextSearch.setEnabled(true);
		infoSearch.setText((currentFoundIndex + 1) + "/" + foundIndexes.size());
		tableViewer.getTable().setFocus();
	}

	/*
	 * Table viewer
	 */

	private Composite compositeTableViewer;

	private void createTableViewer(Composite parent) {
		compositeTableViewer = new Composite(parent, SWT.NONE);

		GridData layoutData;

		layoutData = new GridData();
		layoutData.horizontalAlignment = SWT.FILL;
		layoutData.verticalAlignment = SWT.FILL;
		layoutData.grabExcessHorizontalSpace = true;
		layoutData.grabExcessVerticalSpace = true;
		compositeTableViewer.setLayoutData(layoutData);

		GridLayout layout = new GridLayout(1, false);
		compositeTableViewer.setLayout(layout);
		layout.marginWidth = 10;

		tableViewer = new TableViewer(compositeTableViewer, SWT.RESIZE | SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.FULL_SELECTION | SWT.BORDER | SWT.VERTICAL | SWT.FILL);

		layoutData = new GridData();
		layoutData.horizontalAlignment = SWT.FILL;
		layoutData.verticalAlignment = SWT.FILL;
		layoutData.grabExcessHorizontalSpace = true;
		layoutData.grabExcessVerticalSpace = true;
		tableViewer.getTable().setLayoutData(layoutData);

		createColumns();
		createContextualTableViewerMenu();

		final Table table = tableViewer.getTable();
		table.setHeaderVisible(true);
		table.pack();
		tableViewer.setLabelProvider(labelProvider);
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.setInput(logLines);
		tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				ISelection selection = event.getSelection();
				if (selection instanceof IStructuredSelection) {
					Object selectedObject = ((IStructuredSelection) selection).getFirstElement();
					if (selectedObject instanceof LogLine) {
						LogLine logLine = (LogLine) selectedObject;
						EventDetailsDialog dialog = new EventDetailsDialog(Display.getCurrent()
								.getActiveShell(), EventDetailsDialogComposite.class, "Event Details",
								logLine);
						dialog.open();
					}
				}
			}
		});

		table.addMenuDetectListener(new MenuDetectListener() {
			public void menuDetected(MenuDetectEvent event) {
				Point pt = Display.getCurrent().map(null, table, new Point(event.x, event.y));
				if (tableViewer.getCell(pt) != null) {
					selectedColumnIndex = tableViewer.getCell(pt).getColumnIndex();
					addVariableItem.setEnabled(selectedColumnIndex > 4 && selectedColumnIndex < 14 ? true
							: false);
				}
			}
		});

		/*
		 * IMPORTANT: Dispose the menus (only the current menu, set with
		 * setMenu(), will be automatically disposed)
		 */
		table.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				tableMenu.dispose();
			}
		});

		// Make the selection available to other views
		getSite().setSelectionProvider(tableViewer);
	}

	private ColumnInfo getColumnInfo(String columnName) {
		String columnInfoAsString = preferenceStore.getString(PREFS_COLUMN_INFOS + columnName);
		if ("".equals(columnInfoAsString)) {
			// Searching for the default column info
			for (ColumnInfo columnInfo : DEFAULT_COLUMN_INFOS) {
				if (columnInfo.getName().equals(columnName)) {
					return new ColumnInfo(columnInfo.getName(), columnInfo.isVisible(), columnInfo.getSize());
				}
			}

			throw new NoSuchElementException("Column info for '" + columnName + "' not found");
		} else {
			return ColumnInfo.parse(columnName, columnInfoAsString);
		}
	}

	private void setColumnSize(String columnName, int size) {
		ColumnInfo columnInfo = getColumnInfo(columnName);
		columnInfo.setSize(size);
		preferenceStore.setValue(PREFS_COLUMN_INFOS + columnName, columnInfo.toString());
	}

	private void setColumnVisibility(String columnName, boolean visibility) {
		ColumnInfo columnInfo = getColumnInfo(columnName);
		columnInfo.setVisibility(visibility);
		preferenceStore.setValue(PREFS_COLUMN_INFOS + columnName, columnInfo.toString());
	}

	private int[] getColumnOrder() {
		String columnOrderAsString = preferenceStore.getString(PREFS_COLUMN_ORDER);
		if ("".equals(columnOrderAsString)) {
			return DEFAULT_COLUMN_ORDER;
		} else {
			try {
				int[] columnOrder = DEFAULT_COLUMN_ORDER;
				columnOrderAsString = columnOrderAsString.substring(1, columnOrderAsString.length() - 1);
				String[] sp = columnOrderAsString.split(",");
				int i = 0;
				for (String co : sp) {
					columnOrder[i] = Integer.parseInt(co.trim());
					i++;
				}
				return columnOrder;
			} catch (Exception e) {
				return DEFAULT_COLUMN_ORDER;
			}
		}
	}

	private void setColumnOrder(int[] columnOrder) {
		String s = Arrays.toString(columnOrder);
		preferenceStore.setValue(PREFS_COLUMN_ORDER, s);
	}

	private void createActions() {
		optionsAction = new RetargetAction("Toggle", "Options", IAction.AS_CHECK_BOX) {
			public void runWithEvent(Event event) {
				GridData data = (GridData) compositeOptions.getLayoutData();
				data.exclude = compositeOptions.isVisible();
				compositeOptions.setVisible(!compositeOptions.isVisible());
				mainComposite.layout(true);
			}
		};
		optionsAction.setImageDescriptor(ImageDescriptor.createFromImage(new Image(Display.getDefault(),
				getClass().getResourceAsStream("images/options.png"))));
		optionsAction.setEnabled(true);
		optionsAction.setChecked(false);

		searchAction = new RetargetAction("Toggle", "Search", IAction.AS_CHECK_BOX) {
			public void runWithEvent(Event event) {
				GridData data = (GridData) compositeSearch.getLayoutData();
				data.exclude = compositeSearch.isVisible();
				compositeSearch.setVisible(!compositeSearch.isVisible());
				mainComposite.layout(true);
			}
		};
		searchAction.setImageDescriptor(ImageDescriptor.createFromImage(new Image(Display.getDefault(),
				getClass().getResourceAsStream("images/search.png"))));
		searchAction.setEnabled(true);
		searchAction.setChecked(false);

		restoreDefaultsAction = new Action("Restore To Default Parameters") {
			public void run() {
				// Stop the current log thread
				logViewThread = null;

				// clean filter
				filterText.setText("");
				setLogFilter();

				clearLogs();

				// Clean all columns definition
				for (ColumnInfo columnInfo : DEFAULT_COLUMN_INFOS) {
					preferenceStore.setToDefault(PREFS_COLUMN_INFOS + columnInfo.getName());
				}
				preferenceStore.setToDefault(PREFS_COLUMN_ORDER);

				compositeTableViewer.dispose();
				createTableViewer(mainComposite);

				mainComposite.layout(true);

				createLogViewThread();
			}
		};
		restoreDefaultsAction.setImageDescriptor(ImageDescriptor.createFromImage(new Image(Display
				.getDefault(), getClass().getResourceAsStream("images/restore_defaults.png"))));

		clearLogsAction = new Action("Clear Log Viewer") {
			public void run() {
				clearLogs();
			}
		};
		clearLogsAction.setImageDescriptor(ImageDescriptor.createFromImage(new Image(Display.getDefault(),
				getClass().getResourceAsStream("images/clear_logs.png"))));

		selectColumnsAction = new Action("Select Columns") {
			public void run() {
				Menu selectColumnsMenu = new Menu(mainComposite);

				int i = 0;
				for (ColumnInfo columnInfo : DEFAULT_COLUMN_INFOS) {
					// Get the real column info
					columnInfo = getColumnInfo(columnInfo.getName());

					final String columnName = columnInfo.getName();
					MenuItem item = new MenuItem(selectColumnsMenu, SWT.CHECK);
					item.setText(columnName);
					item.setSelection(columnInfo.isVisible());

					final int _i = i;
					final ColumnInfo _columnInfo = columnInfo;

					item.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							boolean bVisible = ((MenuItem) e.widget).getSelection();
							setColumnVisibility(columnName, bVisible);

							TableColumn column = tableViewer.getTable().getColumn(_i);
							column.setResizable(bVisible);
							column.setMoveable(bVisible);
							column.setWidth(bVisible ? _columnInfo.getSize() : 0);
						};
					});
					i++;
				}

				selectColumnsMenu.setVisible(true);
			}
		};
		selectColumnsAction.setImageDescriptor(ImageDescriptor.createFromImage(new Image(
				Display.getDefault(), getClass().getResourceAsStream("images/select_columns.png"))));

		scrollLockAction = new RetargetAction("Toggle", "Scroll Lock", IAction.AS_CHECK_BOX) {
			public void runWithEvent(Event event) {
				scrollLock = !scrollLock;
			}
		};
		scrollLockAction.setImageDescriptor(ImageDescriptor.createFromImage(new Image(Display.getDefault(),
				getClass().getResourceAsStream("images/scroll_lock.png"))));
		scrollLockAction.setEnabled(true);
	}

	private void clearLogs() {
		logLines.clear();
		tableViewer.getTable().clearAll();
		tableViewer.refresh();
		counter = 0;
	}

	private void createToolbar() {
		IToolBarManager manager = getViewSite().getActionBars().getToolBarManager();
		manager.add(optionsAction);
		manager.add(searchAction);
		manager.add(selectColumnsAction);
		manager.add(restoreDefaultsAction);
		manager.add(clearLogsAction);
		manager.add(scrollLockAction);
	}

	private void handleContextualTableViewerMenuSelection(int i) {
		IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
		setFilterText(selection, i);

		if (!compositeOptions.isVisible()) {
			optionsAction.setChecked(true);
			compositeOptions.setVisible(true);
			GridData data = (GridData) compositeOptions.getLayoutData();
			data.exclude = false;

			mainComposite.layout(true);
		}
	}

	private void createContextualTableViewerMenu() {
		tableMenu = new Menu(mainComposite.getShell(), SWT.POP_UP);
		tableViewer.getTable().setMenu(tableMenu);

		// Add "equals" command
		MenuItem item = new MenuItem(tableMenu, SWT.PUSH);
		item.setText("Add \"equals\" command");
		item.setImage(new Image(Display.getDefault(), getClass().getResourceAsStream(
				"images/log_ctx_menu_add_equals.png")));
		item.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				handleContextualTableViewerMenuSelection(0);
			}
		});

		// Add "contains" command
		item = new MenuItem(tableMenu, SWT.PUSH);
		item.setText("Add \"contains\" command");
		item.setImage(new Image(Display.getDefault(), getClass().getResourceAsStream(
				"images/log_ctx_menu_add_contains.png")));
		item.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				handleContextualTableViewerMenuSelection(1);
			}
		});

		// Add "start with" command
		new MenuItem(tableMenu, SWT.SEPARATOR);
		item = new MenuItem(tableMenu, SWT.PUSH);
		item.setText("Add \"starts with\" command");
		item.setImage(new Image(Display.getDefault(), getClass().getResourceAsStream(
				"images/log_ctx_menu_add_startswith.png")));
		item.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				handleContextualTableViewerMenuSelection(2);
			}
		});

		// Add "end with" command
		item = new MenuItem(tableMenu, SWT.PUSH);
		item.setText("Add \"ends with\" command");
		item.setImage(new Image(Display.getDefault(), getClass().getResourceAsStream(
				"images/log_ctx_menu_add_endswith.png")));
		item.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				handleContextualTableViewerMenuSelection(3);
			}
		});

		// Add variable command
		new MenuItem(tableMenu, SWT.SEPARATOR);
		addVariableItem = new MenuItem(tableMenu, SWT.PUSH);
		addVariableItem.setText("Add variable");
		addVariableItem.setImage(new Image(Display.getDefault(), getClass().getResourceAsStream(
				"images/log_ctx_menu_add_variable.png")));
		addVariableItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				handleContextualTableViewerMenuSelection(4);
			}
		});
		addVariableItem.setEnabled(false);
	}

	private String getSelectedCellText(String columnName, LogLine line) {
		
		Class<LogLine> logLineClass = GenericUtils.cast(line.getClass());
		
		Object result;
		try {
			Method getMethod = logLineClass.getMethod("get" + columnName);
			result = getMethod.invoke(line);
			
			// Case of empty cell
			if (result == null) result = "";
		} catch (Exception e) {
			return null;
		}
		
		return (String) result;
	}

	private void setFilterText(IStructuredSelection selection, int buttonIndex) {
		if (!selection.isEmpty()) {
			LogLine logline = (LogLine) selection.getFirstElement();

			String columnName = tableViewer.getTable().getColumn(selectedColumnIndex).getText();

			String cellValue = getSelectedCellText(columnName, logline);

			if (cellValue == null)
				return;

			String variableName = columnName.toLowerCase();

			String filter = filterText.getText();
			String txt;
			if (filter.contains("==") || filter.contains("contains") || filter.contains("startsWith")
					|| filter.contains("endsWith")) {
				filter = filter + " and ";
			}
			switch (buttonIndex) {
			case 0:
				txt = filter + "(" + variableName + " == \"" + cellValue.replaceAll("\"", "\\\\\"") + "\")";
				filterText.setText(txt);
				break;
			case 1:
				txt = filter + "(" + variableName + ".contains(\"" + cellValue.replaceAll("\"", "\\\\\"")
						+ "\"))";
				filterText.setText(txt);
				break;
			case 2:
				txt = filter + "(" + variableName + ".startsWith(\"" + cellValue.replaceAll("\"", "\\\\\"")
						+ "\"))";
				filterText.setText(txt);
				break;
			case 3:
				txt = filter + "(" + variableName + ".endsWith(\"" + cellValue.replaceAll("\"", "\\\\\"")
						+ "\"))";
				filterText.setText(txt);
				break;
			case 4:
				// Add variable
				txt = filter
						+ "("
						+ variableName
						+ " == \""
						+ ((cellValue != null && cellValue != "") ? cellValue.replaceAll("\"", "\\\\\"")
								: "undefined") + "\")";
				filterText.setText(txt);
				break;
			default:
				break;
			}
		}
	}

	private void createColumns() {
		Table table = tableViewer.getTable();

		while (table.getColumnCount() > 0) {
			table.getColumns()[0].dispose();
		}

		for (ColumnInfo columnInfo : DEFAULT_COLUMN_INFOS) {
			// Get the real column info
			columnInfo = getColumnInfo(columnInfo.getName());
			createTableViewerColumn(columnInfo);
		}

		// Set the column saved order
		table.setColumnOrder(getColumnOrder());
	}

	private TableViewerColumn createTableViewerColumn(ColumnInfo columnInfo) {
		TableViewerColumn viewerColumn = new TableViewerColumn(tableViewer, SWT.VIRTUAL);
		TableColumn column = viewerColumn.getColumn();
		final String columnName = columnInfo.getName();
		column.setText(columnName);

		column.setResizable(columnInfo.isVisible());
		column.setMoveable(columnInfo.isVisible());
		column.setWidth(columnInfo.isVisible() ? columnInfo.getSize() : 0);

		column.addControlListener(new ControlListener() {
			public void controlResized(ControlEvent event) {
				// Get the current column info
				ColumnInfo columnInfo = getColumnInfo(columnName);

				// Update column size only if the column is visible
				if (columnInfo.isVisible()) {
					TableColumn tableColumn = (TableColumn) event.getSource();
					setColumnSize(columnName, tableColumn.getWidth());
				}
			}

			public void controlMoved(ControlEvent arg0) {
				setColumnOrder(tableViewer.getTable().getColumnOrder());
			}
		});

		return viewerColumn;
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {
		tableViewer.getControl().setFocus();
	}

	private void createLogViewThread() {
		logViewThread = new Thread(new Runnable() {
			public void run() {
				try {
					// Wait for the Convertigo engine fully starts the log
					// objects
					while (Engine.logConvertigo == null) {
						Thread.sleep(100);
					}

					// Attach the Log4J appender
					Engine.logConvertigo.addAppender(appender);

					logManager = new LogManager();
					logManager.setContinue(true);
					logManager.setDateStart(new Date(Engine.startStopDate - 10000));
					logManager.setMaxLines(50);

					// Get the newest available lines
					while (getLogs()) {
						// Refresh the list view
						Display.getDefault().asyncExec(new Runnable() {
							public void run() {
								tableViewer.refresh();
								if (!scrollLock) {
									tableViewer.reveal(tableViewer.getElementAt(tableViewer.getTable()
											.getItemCount() - 1));
								}
							}
						});

						// We must release some CPU time in order to allow
						// the GUI to be refreshed
						// Thread.sleep(500);
					}
				} catch (InterruptedException e) {
					ConvertigoPlugin.logException(e, "The engine log viewer thread has been interrupted");
				}
			}
		});
		logViewThread.setDaemon(true);
		logViewThread.setName("EngineLogViewerThread");
		logViewThread.start();
	}

	private boolean getLogs() {
		try {
			JSONArray logs = logManager.getLines();
			boolean interrupted = false;
			while (logs.length() == 0 && !interrupted && Thread.currentThread() == logViewThread) {
				synchronized (appender) {
					try {
						appender.wait(300);
					} catch (InterruptedException e) {
						interrupted = true;
					}
				}

				// Detect if the view has been closed
				if (Thread.currentThread() != logViewThread)
					return false;

				logs = logManager.getLines();
			}

			List<String> extraList = new LinkedList<String>();
			List<String> messageList = new LinkedList<String>();
			HashMap<String, String> allExtras = new HashMap<String, String>();
			for (int i = 0; i < logs.length(); i++) {
				JSONArray logLine = (JSONArray) logs.get(i);
				String extract, extra = "";
				extraList.clear();
				messageList.clear();
				for (int j = 5; j < logLine.length(); j++) {
					extract = logLine.getString(j) + ";";
					allExtras.put(extract.substring(0, extract.indexOf("=")),
							extract.substring(extract.indexOf("=") + 1, extract.indexOf(";")));
					extra = logLine.getString(j) + ";";
					extraList.add(logLine.getString(j));
				}
				String message = logLine.getString(4);
				int position = 0;
				if (message.contains("\n")) {
					messageList.add(message.substring(0, message.indexOf("\n") + "\n".length()));
					while ((message.contains("\n")) && (!"\n".equals(""))) {
						position = message.indexOf("\n");
						message = message.substring(position + "\n".length(), message.length());
						messageList.add(message.substring(0, message.indexOf("\n") + "\n".length()));
					}
				}
				message = logLine.getString(4);
				if (message.contains("\n")) {
					if (messageList.size() > extraList.size()) {
						boolean subLine = false;
						for (int k = 0; k < messageList.size(); k++) {
							if (k > 0)
								subLine = true;
							if (k < extraList.size() && extraList.size() != 0) {
								logLines.add(new LogLine(logLine.getString(0), logLine.getString(1), logLine
										.getString(2), logLine.getString(3), messageList.get(k), extraList
										.get(k), subLine, counter, logLine.getString(4), allExtras));
							} else {
								logLines.add(new LogLine(logLine.getString(0), logLine.getString(1), logLine
										.getString(2), logLine.getString(3), messageList.get(k), " ",
										subLine, counter, logLine.getString(4), allExtras));
							}
						}
						counter++;
					}
				} else {
					logLines.add(new LogLine(logLine.getString(0), logLine.getString(1),
							logLine.getString(2), logLine.getString(3), logLine.getString(4), extra, false,
							counter, logLine.getString(4), allExtras));
					counter++;
				}
			}
		} catch (IOException e) {
			ConvertigoPlugin.logException(e, "Error while loading the Engine logs", true);
		} catch (JSONException e) {
			ConvertigoPlugin.logException(e, "Unable to process received Engine logs", true);
		}
		logManager.setContinue(true);
		return true;
	}
}
