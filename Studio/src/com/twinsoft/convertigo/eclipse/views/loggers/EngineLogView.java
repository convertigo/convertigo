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

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
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
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.RetargetAction;
import org.eclipse.ui.part.ViewPart;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dialogs.EnginePreferenceDialog;
import com.twinsoft.convertigo.eclipse.dialogs.EventDetailsDialog;
import com.twinsoft.convertigo.eclipse.dialogs.EventDetailsDialogComposite;
import com.twinsoft.convertigo.eclipse.dialogs.LimitCharsLogsPreferenceDialog;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.logmanager.LogManager;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class EngineLogView extends ViewPart {
	private Thread logViewThread;
	private LogManager logManager;
	private long lastLogTime = -1;
	private List<LogLine> logLines = new ArrayList<LogLine>(1000);
	private Appender appender;
	private int counter = 0;
	
	private long charMeter = 0;
	private int curLine = 0;

	private boolean scrollLock = false;
	private boolean activateOnNewEvents = true;
	private ColumnInfo[] columnInfos = EngineLogView.clone(DEFAULT_COLUMN_INFOS);
	private int[] columnOrder = DEFAULT_COLUMN_ORDER.clone();
	private int limitLogChars = DEFAULT_MAX_LOG_CHARS;
	
	private static final int DEFAULT_MAX_LOG_CHARS = 100000;
	private static final int MAX_BUFFER_LINES = Integer.MAX_VALUE;
	
	private Action activateOnNewEventsAction, clearLogsAction, restoreDefaultsAction, selectColumnsAction, settingsEngine, limitLogCharsAction;
	private RetargetAction scrollLockAction, optionsAction, searchAction;	

	private EngineLogViewLabelProvider labelProvider;

	private static final ColumnInfo[] DEFAULT_COLUMN_INFOS = { new ColumnInfo("Date", false, 80),
			new ColumnInfo("Time", true, 90), new ColumnInfo("DeltaTime", true, 60),
			new ColumnInfo("Message", true, 400), new ColumnInfo("Level", false, 50),
			new ColumnInfo("Category", true, 80), new ColumnInfo("Thread", true, 180),
			new ColumnInfo("Project", true, 70), new ColumnInfo("Connector", true, 70),
			new ColumnInfo("Transaction", true, 70), new ColumnInfo("Sequence", true, 70),
			new ColumnInfo("ContextID", true, 160), new ColumnInfo("UID", false, 50),
			new ColumnInfo("User", false, 50), new ColumnInfo("ClientIP", false, 50),
			new ColumnInfo("ClientHostName", false, 50), new ColumnInfo("UUID", false, 50) };

	private static final int[] DEFAULT_COLUMN_ORDER = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };

	private static ColumnInfo[] clone(ColumnInfo[] array) {
		ColumnInfo[] clonedArray = array.clone();
		for(int i = 0; i < array.length; i++) {
			clonedArray[i] = (ColumnInfo) array[i].clone();
		}
		
		return clonedArray;
	}
	
	private Menu tableMenu;
	private MenuItem addVariableItem;

	private Text filterText, searchText;
	private Label infoSearch;
	private boolean searchCaseSensitive = false;
	
	private TableViewer tableViewer;
	private int selectedColumnIndex;

	public EngineLogView() {
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

	/*
	 * View state persistence
	 */

	private IMemento memento;

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		this.memento = memento;
	}

	@Override
	public void saveState(IMemento memento) {
		// Scroll lock
		memento.putBoolean("scrollLock", scrollLock);

		// Limit log chars
		memento.putInteger("limitLogChars", limitLogChars);

		// Activate on new events
		memento.putBoolean("activateOnNewEvents", activateOnNewEvents);

		// Column order
		memento.putString("columnOrder", Arrays.toString(tableViewer.getTable().getColumnOrder()));

		// Column information
		for (ColumnInfo columnInfo : columnInfos) {
			IMemento columnInfoMemento = memento.createChild("columnInfo");
			columnInfoMemento.putString("name", columnInfo.getName());
			columnInfoMemento.putBoolean("visible", columnInfo.isVisible());
			columnInfoMemento.putInteger("size", columnInfo.getSize());
		}

		super.saveState(memento);
	}

	private void restoreState() {
		if (memento == null)
			return;

		// Scroll lock
		Boolean bScrollLock = memento.getBoolean("scrollLock");
		if (bScrollLock != null)
			scrollLock = bScrollLock.booleanValue();
	
		//Limit log chars
		Integer iLimitLogChars = memento.getInteger("limitLogChars");
		if (iLimitLogChars != null)
			limitLogChars = iLimitLogChars.intValue();

		// Activate on new events
		Boolean bActivateOnNewEvents = memento.getBoolean("activateOnNewEvents");
		if (bActivateOnNewEvents != null)
			activateOnNewEvents = bActivateOnNewEvents.booleanValue();

		// Column order
		String columnOrderAsString = memento.getString("columnOrder");
		if (columnOrderAsString != null) {
			columnOrderAsString = columnOrderAsString.substring(1, columnOrderAsString.length() - 1);
			String[] columnOrders = columnOrderAsString.split(",");
			for (int i = 0; i < columnOrders.length; i++) {
				String column = columnOrders[i].trim();
				try {
					columnOrder[i] = Integer.parseInt(column);
				} catch (Exception e) {
					// Silently ignore (use default column order)
				}
			}
		}

		// Column information
		IMemento[] mementoColumnInfos = memento.getChildren("columnInfo");
		int i = 0;
		for (IMemento mementoColumnInfo : mementoColumnInfos) {
			columnInfos[i] = new ColumnInfo(mementoColumnInfo.getString("name"),
					mementoColumnInfo.getBoolean("visible"), mementoColumnInfo.getInteger("size"));
			i++;
		}
	}

	private Composite mainComposite;

	@Override
	public void createPartControl(Composite parent) {
		restoreState();

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
		createMenu();

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
		
		Button applyButton = new Button(compositeOptions, SWT.NONE);
		applyButton.setText("Apply");
		applyButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				clearLogs();
				setLogFilter();
			}
		});
		
		Button clearButton = new Button(compositeOptions, SWT.NONE);
		clearButton.setText("Clear");
		clearButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				filterText.setText("");
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

		GridLayout layout = new GridLayout(8, false);
		compositeSearch.setLayout(layout);
		layout.marginWidth = 3;

		Label label = new Label(compositeSearch, SWT.NONE);
		label.setText("Search");

		searchText = new Text(compositeSearch, SWT.BORDER);
		layoutData = new GridData();
		layoutData.horizontalAlignment = SWT.FILL;
		layoutData.grabExcessHorizontalSpace = true;
		searchText.setLayoutData(layoutData);
		searchText.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				applySearch();
			}
		});

		infoSearch = new Label(compositeSearch, SWT.NONE);
		infoSearch.setVisible(false);

		previousSearch = new Button(compositeSearch, SWT.NONE);
		previousSearch.setText(" < ");
		previousSearch.setEnabled(false);
		previousSearch.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				searchInLogs(-1);
			}
		});

		nextSearch = new Button(compositeSearch, SWT.NONE);
		nextSearch.setText(" > ");
		nextSearch.setEnabled(false);
		nextSearch.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				searchInLogs(1);
			}
		});

		Button checkCase = new Button(compositeSearch, SWT.CHECK);
		checkCase.setText("Case sensitive");
		checkCase.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				searchCaseSensitive = ((Button) e.widget).getSelection();
				applySearch();
			}
		});
		
		Button applyButton = new Button(compositeSearch, SWT.NONE);
		applyButton.setText("Apply");
		applyButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				applySearch();
			}
		});
		
		Button clearButton = new Button(compositeSearch, SWT.NONE);
		clearButton.setText("Clear");
		clearButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				searchText.setText("");
				applySearch();
			}
		});
	}

	private int currentFoundIndex = 0;
	private ArrayList<Integer> foundIndexes = new ArrayList<Integer>();
	
	private void applySearch() {
		currentFoundIndex = 0;
		nextSearch.setEnabled(false);
		previousSearch.setEnabled(false);
		
		
		String searchedText = searchText.getText();
		if ("".equals(searchedText)) {
			infoSearch.setVisible(false);
			GridData data = (GridData) infoSearch.getLayoutData();
			data.exclude = true;
			compositeSearch.layout();
			return;
		}

		foundIndexes.clear();
		Table table = tableViewer.getTable();
		int nLines = table.getItemCount();

		if (!searchCaseSensitive) {
			searchedText = searchedText.toLowerCase();
		}

		for (int i = 0; i < nLines; i++) {
			String cellText = table.getItem(i).getText(3);
			
			if (!searchCaseSensitive) {
				cellText = cellText.toLowerCase();
			}
			
			if (cellText.contains(searchedText))
				foundIndexes.add(i);
		}

		if (foundIndexes.isEmpty()) {
			infoSearch.setText("0 / 0");
		} else {
			int nFoundOccurrences = foundIndexes.size();
			infoSearch.setText((currentFoundIndex + 1) + "/" + nFoundOccurrences);
			tableViewer.getTable().setSelection(foundIndexes.get(currentFoundIndex));
			tableViewer.getTable().setFocus();
			nextSearch.setEnabled(nFoundOccurrences > 1);
		}

		infoSearch.setVisible(true);
		GridData data = (GridData) infoSearch.getLayoutData();
		data.exclude = false;
		compositeSearch.layout();
	}

	private void searchInLogs(int side) {
		int searchIndex = currentFoundIndex + side;
		if (searchIndex < 0 || searchIndex > foundIndexes.size())
			return;
		
		currentFoundIndex = searchIndex;
		Table table = tableViewer.getTable();
		
		table.setSelection(foundIndexes.get(currentFoundIndex));
		table.setFocus();

		// Disable "previous" if is the beginning
		if (foundIndexes.get(currentFoundIndex) == foundIndexes.get(0))
			previousSearch.setEnabled(false);
		else
			previousSearch.setEnabled(true);

		// Disable "next" if is the end
		if (foundIndexes.get(currentFoundIndex) == foundIndexes.get(foundIndexes.size() - 1))
			nextSearch.setEnabled(false);
		else
			nextSearch.setEnabled(true);
		
		infoSearch.setText((currentFoundIndex + 1) + "/" + foundIndexes.size());

		// To force components resizing if needed
		compositeSearch.layout();
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
		// Searching for the default column info
		for (ColumnInfo columnInfo : columnInfos) {
			if (columnInfo.getName().equals(columnName)) {
				return columnInfo;
			}
		}

		throw new NoSuchElementException("Column info for '" + columnName + "' not found");
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

		limitLogCharsAction = new Action("Limit log chars to " + limitLogChars) {
			public void run() {
				/* OPEN THE DIALOG TO SET THE LIMIT LOG CHARS */
				LimitCharsLogsPreferenceDialog dialog = new LimitCharsLogsPreferenceDialog(Display.getDefault().getActiveShell(), 
						limitLogChars);
				int result = dialog.open();
				if ( result == SWT.OK ) {
					limitLogChars = dialog.getLimitLogsChars();
					limitLogCharsAction.setText("Limit log chars to " + limitLogChars);
					saveState(memento);
				}
			}
		};
		limitLogCharsAction.setEnabled(true);
		        
		settingsEngine = new Action("Configure Log level"){
			public void run(){
				EnginePreferenceDialog dialog = new EnginePreferenceDialog(Display.getDefault().getActiveShell());
				dialog.open();
			}
		};
		settingsEngine.setImageDescriptor(ImageDescriptor.createFromImage(new Image(Display
				.getDefault(), getClass().getResourceAsStream("images/configure_log_level.png"))));
		settingsEngine.setEnabled(true);
		
		restoreDefaultsAction = new Action("Restore to default parameters") {
			public void run() {
				// Stop the current log thread
				logViewThread = null;

				// clean filter
				filterText.setText("");
				setLogFilter();

				clearLogs();

				// Clean all columns definition
				columnInfos = EngineLogView.clone(DEFAULT_COLUMN_INFOS);
				columnOrder = DEFAULT_COLUMN_ORDER.clone();

				compositeTableViewer.dispose();
				createTableViewer(mainComposite);

				mainComposite.layout(true);

				createLogViewThread();
			}
		};
		restoreDefaultsAction.setImageDescriptor(ImageDescriptor.createFromImage(new Image(Display
				.getDefault(), getClass().getResourceAsStream("images/restore_defaults.png"))));
		restoreDefaultsAction.setEnabled(true);

		clearLogsAction = new Action("Clear log viewer") {
			public void run() {
				logManager.setDateStart(new Date());
				clearLogs();
			}
		};
		clearLogsAction.setImageDescriptor(ImageDescriptor.createFromImage(new Image(Display.getDefault(),
				getClass().getResourceAsStream("images/clear_logs.png"))));

		selectColumnsAction = new Action("Select columns") {
			public void run() {
				Menu selectColumnsMenu = new Menu(mainComposite);

				int i = 0;
				for (ColumnInfo columnInfo : columnInfos) {
					String columnName = columnInfo.getName();
					MenuItem item = new MenuItem(selectColumnsMenu, SWT.CHECK);
					item.setText(columnName);
					item.setSelection(columnInfo.isVisible());

					final int _i = i;
					final ColumnInfo _columnInfo = columnInfo;

					item.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							boolean bVisible = ((MenuItem) e.widget).getSelection();
							_columnInfo.setVisible(bVisible);

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

		activateOnNewEventsAction = new RetargetAction("Toggle", "Activate on new events",
				IAction.AS_CHECK_BOX) {
			public void runWithEvent(Event event) {
				activateOnNewEvents = !activateOnNewEvents;
			}
		};
		activateOnNewEventsAction.setChecked(activateOnNewEvents);
		activateOnNewEventsAction.setEnabled(true);

		scrollLockAction = new RetargetAction("Toggle", "Scroll lock", IAction.AS_CHECK_BOX) {
			public void runWithEvent(Event event) {
				scrollLock = !scrollLock;
			}
		};
		scrollLockAction.setImageDescriptor(ImageDescriptor.createFromImage(new Image(Display.getDefault(),
				getClass().getResourceAsStream("images/scroll_lock.png"))));
		scrollLockAction.setChecked(scrollLock);
		scrollLockAction.setEnabled(true);
	}

	private void clearLogs() {
		logLines.clear();
		tableViewer.getTable().removeAll();
		lastLogTime = -1;
		counter = 0;
		charMeter = 0;
		curLine = 0;
	}

	private void createToolbar() {
		IToolBarManager manager = getViewSite().getActionBars().getToolBarManager();
		manager.add(settingsEngine);
		manager.add(optionsAction);
		manager.add(searchAction);
		manager.add(clearLogsAction);
		manager.add(scrollLockAction);
	}

	private void createMenu() {
		IMenuManager manager = getViewSite().getActionBars().getMenuManager();
		manager.add(selectColumnsAction);
		manager.add(restoreDefaultsAction);
		manager.add(new Separator());
		manager.add(limitLogCharsAction);
		manager.add(activateOnNewEventsAction);
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
		
		// Add "clear log" command
		new MenuItem(tableMenu, SWT.SEPARATOR);
		item = new MenuItem(tableMenu, SWT.PUSH);
		item.setText("Clear logs");
		item.setImage(new Image(Display.getDefault(), getClass().getResourceAsStream(
				"images/clear_logs.png")));
		item.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				clearLogsAction.run();
			}
		});
	}

	private String getSelectedCellText(String columnName, LogLine line) {

		Class<LogLine> logLineClass = GenericUtils.cast(line.getClass());

		Object result;
		try {
			Method getMethod = logLineClass.getMethod("get" + columnName);
			result = getMethod.invoke(line);

			// Case of empty cell
			if (result == null)
				result = "";
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

		for (ColumnInfo columnInfo : columnInfos) {
			createTableViewerColumn(columnInfo);
		}

		// Set the column saved order
		table.setColumnOrder(columnOrder);
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

				TableColumn tableColumn = (TableColumn) event.getSource();
				columnInfo.setSize(tableColumn.getWidth());
			}

			public void controlMoved(ControlEvent arg0) {
				int[] newColumnOrder = tableViewer.getTable().getColumnOrder(); 
				if (newColumnOrder.length == columnOrder.length) {
					columnOrder = newColumnOrder;
				}
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
		final ViewPart engineLogView = this;

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
					logManager.setMaxLines(MAX_BUFFER_LINES);
					
					curLine = 0;
					
					// Get the newest available lines
					while (getLogs()) {
						final int[] rmRow = {-1};
						
						int nbRow = curLine - 1;
						while (limitLogChars > 0 && charMeter > limitLogChars && rmRow[0] < nbRow) {
							rmRow[0]++;
							LogLine line = logLines.remove(0);
							curLine--;
							charMeter -= line.getMessage().length();
						}
						
						while (logLines.size() > 0 && limitLogChars > 0 && charMeter > limitLogChars) {
							LogLine line = logLines.remove(curLine = 0);
							charMeter -= line.getMessage().length();
						}
						
						final Object[][] buf = new Object[1][];
						buf[0] = logLines.subList(curLine, logLines.size()).toArray();
						curLine = logLines.size();
						
						// Refresh the list view
						Display.getDefault().syncExec(new Runnable() {
						
							public void run() {
								int topIndex = tableViewer.getTable().getTopIndex();
								if (!tableViewer.getTable().isDisposed()) {
									tableViewer.getTable().setVisible(false);
									if (rmRow[0] > -1) {
										topIndex -= rmRow[0] + 1;
										tableViewer.getTable().remove(0, rmRow[0]);
									}
								}
								
								if (!tableViewer.getTable().isDisposed()) {
									tableViewer.add(buf[0]);
									tableViewer.getTable().setVisible(true);
								}
								
								if (!tableViewer.getTable().isDisposed()) {
									tableViewer.getTable().setTopIndex(!scrollLock ? tableViewer.getTable().getItemCount() - 1 : Math.max(topIndex, 0));
								}
								
								if (activateOnNewEvents) {
									IWorkbenchWindow workbenchWindow = ConvertigoPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
									if (workbenchWindow != null) {
										workbenchWindow.getActivePage().bringToTop(engineLogView);
									}
								}
							}
							
						});
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

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,S");
	
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
				if (Thread.currentThread() != logViewThread) {
					return false;
				}
				logs = logManager.getLines();
			}
			
			HashMap<String, String> allExtras = new HashMap<String, String>();
			
			for (int i = 0; i < logs.length(); i++) {
				JSONArray logLine = (JSONArray) logs.get(i);
				
				String dateTime = logLine.getString(1);
				String[] dateTimeParts = dateTime.split(" ");
				String date = dateTimeParts[0];
				String time = dateTimeParts[1];
	
				String deltaTime;
				try {
					long currentDate = DATE_FORMAT.parse(dateTime).getTime();
					if (lastLogTime < 0) {
						deltaTime = "--";
					} else {
						long delta = currentDate - lastLogTime;
						lastLogTime = currentDate;
						if (delta < 1000) {
							deltaTime = StringUtils.leftPad(delta + " ms", 8);
						}
						else if (delta < 10000) {
							deltaTime = StringUtils.leftPad(Math.floor(delta / 10.0) / 100.0 + " s ", 8);
						}
						else {
							deltaTime = StringUtils.leftPad((int) Math.floor(delta / 1000.0) + " s ", 8);
						}
					}
					lastLogTime = currentDate;
				} catch (ParseException e) {
					deltaTime = "n/a";
				}
				
				int len = logLine.length();
				for (int j = 5; j < len; j++) {
					String extra = logLine.getString(j);
					int k = extra.indexOf("=");
					allExtras.put(extra.substring(0, k), extra.substring(k + 1));
				}

				// Build the message lines
				String message = logLine.getString(4);
				String[] messageLines = message.split("\n");
				if (messageLines.length > 1) {
					boolean firstLine = true;
					for (String messageLine : messageLines) {
						logLines.add(new LogLine(logLine.getString(0), date, time, deltaTime, logLine
								.getString(2), logLine.getString(3), messageLine, !firstLine, counter,
								message, allExtras));
						counter++;
						firstLine = false;

						charMeter += messageLine.length();
					}
				}
				else {
					logLines.add(new LogLine(logLine.getString(0), date, time, deltaTime, logLine
							.getString(2), logLine.getString(3), message, false, counter,
							message, allExtras));
					counter++;
					
					charMeter += message.length();
				}
			}
		} catch (JSONException e) {
			ConvertigoPlugin.logException(e, "Unable to process received Engine logs", false);
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Error while loading the Engine logs (" + e.getClass().getCanonicalName() + ")", false);
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e1) { }
		}
		return true;
	}
}
