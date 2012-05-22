package com.twinsoft.convertigo.eclipse.views.loggers;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
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
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.RetargetAction;
import org.eclipse.ui.part.ViewPart;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dialogs.EventDetailsDialog;
import com.twinsoft.convertigo.eclipse.dialogs.EventDetailsDialogComposite;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.editors.CompositeListener;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.logmanager.LogManager;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;

public class EngineLogView extends ViewPart implements CompositeListener {
	private TableViewer tableViewer;
	private Thread thread;
	private LogManager logManager;
	private List<LogLine> logLines = new LinkedList<LogLine>();
	private Appender appender;
	private EngineLogViewComparator comparator;
	private int counter = 0;
	
	private boolean scrollLock = false;
	private Action clearLogsAction;
	private RetargetAction scrollLockAction;
	private EngineLogViewLabelProvider labelProvider;
	private static final String ENGINE_LOG_VIEW_COL_ORDER = "EngineLogView.COL_ORDER";
	private static final String ENGINE_LOG_VIEW_COL_HIDE = "EngineLogView.COL_HIDE";
	private IMemento memento;
	private Menu headerMenu;
	private Menu tableMenu;
	private Text filterText;
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
   		thread = null;
	}

	@Override
	public void createPartControl(Composite parent) {
		GridLayout gridLayout = new GridLayout(2, false);
		parent.setLayout(gridLayout);
		
		Label filterLabel = new Label(parent, SWT.NONE);
		filterLabel.setText("Filter: ");
		filterText = new Text(parent, SWT.BORDER | SWT.SEARCH);
		filterText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		
		createActions();
		createToolbar();
		
		filterText.addListener(SWT.DefaultSelection, new Listener() {
			public void handleEvent(Event e) {
				setLogFilter();
			}
		});
		
		createViewer(parent);

		comparator = new EngineLogViewComparator();
		tableViewer.setComparator(comparator);
		if (memento != null) {
			int[] columnOrder = new int[tableViewer.getTable().getColumnCount()];
			for (int i=0; i < tableViewer.getTable().getColumnCount(); i++) {
				int order = memento.getInteger(ENGINE_LOG_VIEW_COL_ORDER + "_" + i);
				columnOrder[i] = order;
				boolean hide = memento.getBoolean(ENGINE_LOG_VIEW_COL_HIDE + "_" + i);
				if (hide) {
					tableViewer.getTable().getColumn(i).setWidth(0);
					tableViewer.getTable().getColumn(i).setResizable(false);
					headerMenu.getItem(i-1).setSelection(false);
				}
			}
			tableViewer.getTable().setColumnOrder(columnOrder);
		}
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		this.memento = memento;
	}

	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		Table table = tableViewer.getTable();
		int[] columnOrder = new int[table.getColumnCount()];
		columnOrder = table.getColumnOrder();
		for (int i=0; i < columnOrder.length; i++) {
			memento.putInteger(ENGINE_LOG_VIEW_COL_ORDER + "_" + i, columnOrder[i]);
			memento.putBoolean(ENGINE_LOG_VIEW_COL_HIDE + "_" + i, !table.getColumn(i).getResizable());
		}
	}

	private void createActions() {
		clearLogsAction = new Action("Clear Log Viewer") {
			public void run() {
				tableViewer.getTable().clearAll();
			}
		};
		clearLogsAction.setImageDescriptor(ImageDescriptor.createFromImage(new Image(Display.getDefault(), getClass().getResourceAsStream("images/clear_logs.png"))));
		
		scrollLockAction = new RetargetAction("Toggle","Scroll Lock", IAction.AS_CHECK_BOX) {
			public void runWithEvent(Event event) {
				scrollLock = !scrollLock;
			}
		};
		
		scrollLockAction.setImageDescriptor(ImageDescriptor.createFromImage(new Image(Display.getDefault(), getClass().getResourceAsStream("images/scroll_lock.png"))));
		scrollLockAction.setEnabled(true);
	}
	
	private void createToolbar() {
		IToolBarManager manager = getViewSite().getActionBars().getToolBarManager();
		manager.add(clearLogsAction);
		manager.add(scrollLockAction);
	}
	
	private void createViewer(Composite parent) {
		Composite compositeTableViewer = new Composite(parent, SWT.NONE);
		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = 2;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		compositeTableViewer.setLayoutData(gridData);
		
		tableViewer = new TableViewer(compositeTableViewer, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		
		createColumns(compositeTableViewer, tableViewer);
		createTableMenu(compositeTableViewer);
		
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
						EventDetailsDialog dialog = new EventDetailsDialog(Display.getCurrent().getActiveShell()
								, EventDetailsDialogComposite.class, "Event Details", logLine);
						dialog.open();
					}
				}
			}
		});
		
		table.addListener(SWT.MenuDetect, new Listener() {
			public void handleEvent(Event event) {
				Point pt = Display.getCurrent().map(null, table, new Point(event.x, event.y));
				Rectangle clientArea = table.getClientArea();
				boolean header = clientArea.y <= pt.y && pt.y < (clientArea.y + table.getHeaderHeight());
				selectedColumnIndex = tableViewer.getCell(pt).getColumnIndex();
				table.setMenu(header ? headerMenu : tableMenu);
			}
		});
		
		/* IMPORTANT: Dispose the menus (only the current menu, set with setMenu(), will be automatically disposed) */
		table.addListener(SWT.Dispose, new Listener() {
			public void handleEvent(Event event) {
				headerMenu.dispose();
				tableMenu.dispose();
			}
		});
		
		thread = new Thread(new Runnable() {
			
			public void run() {
				try {
					while (Engine.logConvertigo == null) {
							Thread.sleep(500);
					}
			        Engine.logConvertigo.addAppender(appender);

					logManager = new LogManager();
					logManager.setContinue(true);
					logManager.setDateStart(new Date(new Date().getTime() - 60000));
					logManager.setMaxLines(50);
					
					while (Thread.currentThread() == thread) {
						updateLogs();
						Display.getDefault().asyncExec(new Runnable() {
							public void run() {
								tableViewer.refresh();
								if (!scrollLock) {
									tableViewer.reveal(tableViewer.getElementAt(tableViewer.getTable().getItemCount()-1));
								}
							}
						});
					}
				} catch (InterruptedException e) {
					ConvertigoPlugin.logException(e, "The \"StudioLogViewer\" thread has been interrupted");
				} 
			}
		});
		thread.setDaemon(true);
		thread.setName("StudioLogViewer");
		thread.start();
		// Make the selection available to other views
		getSite().setSelectionProvider(tableViewer);
	}
	
	private void createTableMenu(final Composite parent) {
		tableMenu = new Menu(parent.getShell(), SWT.POP_UP);
		MenuItem item = new MenuItem(tableMenu, SWT.PUSH);
		item.setText("add \"equals\" command");
		item.setImage(new Image(Display.getDefault(), getClass().getResourceAsStream("images/log_ctx_menu_add_equals.png")));
		item.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
				setFilterText(selection, 0);
			}
		});
		
		item = new MenuItem(tableMenu, SWT.PUSH);
		item.setText("add \"contains\" command");
		item.setImage(new Image(Display.getDefault(), getClass().getResourceAsStream("images/log_ctx_menu_add_contains.png")));
		item.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
				setFilterText(selection, 1);
			}
		});
		
		new MenuItem(tableMenu, SWT.SEPARATOR);
		item = new MenuItem(tableMenu, SWT.PUSH);
		item.setText("add \"starts with\" command");
		item.setImage(new Image(Display.getDefault(), getClass().getResourceAsStream("images/log_ctx_menu_add_startswith.png")));
		item.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
				setFilterText(selection, 2);
			}
		});
		
		item = new MenuItem(tableMenu, SWT.PUSH);
		item.setText("add \"ends with\" command");
		item.setImage(new Image(Display.getDefault(), getClass().getResourceAsStream("images/log_ctx_menu_add_endswith.png")));
		item.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
				setFilterText(selection, 3);
			}
		});
	}
	
	private String getSelectedCellText(int columnIndex, LogLine line) {
		String text = "";
		switch (columnIndex) {
			case 0: 
				text = line.getMessage();
				break;
			case 1:
				text = line.getLevel();
				break;
			case 2:
				text = line.getCategory();
				break;
			case 3:
				text = line.getTime();
				break;
			case 4:
				text = line.getThread();
				break;
			case 5:
				text = line.getExtra();
				break;
			default:
				break;
		}
		return text;
	}
	
	private void setFilterText(IStructuredSelection selection, int buttonIndex) {
		if (!selection.isEmpty()) {
			LogLine logline = (LogLine) selection.getFirstElement();
			String cellValue = getSelectedCellText(selectedColumnIndex, logline);
			String variableName = tableViewer.getTable().getColumn(selectedColumnIndex).getText().toLowerCase();
			String filter = filterText.getText();
			if (filter != "") {
				filter = "(" + filter + ") and ";
			}
			switch (buttonIndex) {
				case 0: 
					filterText.setText(filter + variableName + " == \"" + cellValue + "\"");
					break;
				case 1:
					filterText.setText(filter + variableName + ".contains(\"" + cellValue + "\")");
					break;
				case 2:
					filterText.setText(filter + variableName + ".startsWith(\"" + cellValue + "\")");
					break;
				case 3:
					filterText.setText(filter + variableName + ".endsWith(\"" + cellValue + "\")");
					break;
				default:
					break;
			}
			setLogFilter();
		}
	}
	
	private void setLogFilter() {
		try {
			logManager.setFilter(filterText.getText());
			logManager.setContinue(true);
		} catch (ServiceException e) {
			ConvertigoPlugin.logException(e, "Unable to set logs filter", true);
		}
	}

	private void createColumns(final Composite parent, final TableViewer viewer) {
		String[] titles = {"Message", "Level", "Category", "Time", "Thread", "Extra"};
		int[] bounds = {400, 50, 125, 150, 125, 100};
		headerMenu = new Menu(parent.getShell(), SWT.POP_UP);
		viewer.getTable().setMenu(headerMenu);
		
		TableColumnLayout layout = new TableColumnLayout();
		parent.setLayout(layout);
		
		TableViewerColumn col = createTableViewerColumn(titles[0], bounds[0], 0);;
		layout.setColumnData(col.getColumn(), new ColumnWeightData(20, ColumnWeightData.MINIMUM_WIDTH, true));

		col = createTableViewerColumn(titles[1], bounds[1], 1);
		layout.setColumnData(col.getColumn(), new ColumnWeightData(3, ColumnWeightData.MINIMUM_WIDTH, true));
		createMenuItem(headerMenu, col.getColumn());
		
		col = createTableViewerColumn(titles[2], bounds[2], 2);
		layout.setColumnData(col.getColumn(), new ColumnWeightData(5, ColumnWeightData.MINIMUM_WIDTH, true));
		createMenuItem(headerMenu, col.getColumn());
		
		col = createTableViewerColumn(titles[3], bounds[3], 3);
		layout.setColumnData(col.getColumn(), new ColumnWeightData(4, ColumnWeightData.MINIMUM_WIDTH, true));
		createMenuItem(headerMenu, col.getColumn());
		
		col = createTableViewerColumn(titles[4], bounds[4], 4);
		layout.setColumnData(col.getColumn(), new ColumnWeightData(3, ColumnWeightData.MINIMUM_WIDTH, true));
		createMenuItem(headerMenu, col.getColumn());
		
		col = createTableViewerColumn(titles[5], bounds[5], 5);
		layout.setColumnData(col.getColumn(), new ColumnWeightData(5, ColumnWeightData.MINIMUM_WIDTH, true));
		createMenuItem(headerMenu, col.getColumn());
	} 
	
	private TableViewerColumn createTableViewerColumn(String title, int bound, final int colNumber) {
		TableViewerColumn viewerColumn = new TableViewerColumn(tableViewer,SWT.VIRTUAL);
		TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setWidth(bound);
		column.setResizable(true);
		column.setMoveable(true);
		column.addSelectionListener(getSelectionAdapter(column, colNumber));
		return viewerColumn;
	}
	
	private void createMenuItem(Menu parent, final TableColumn column) {
		final MenuItem itemName = new MenuItem(parent, SWT.CHECK);
		itemName.setText(column.getText());
		itemName.setSelection(column.getResizable());
		itemName.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if (itemName.getSelection()) {
					column.setWidth(150);
					column.setResizable(true);
				} else {
					column.setWidth(0);
					column.setResizable(false);
				}
			}
		});
	}

	private SelectionAdapter getSelectionAdapter(final TableColumn column, final int index) {
		SelectionAdapter selectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				comparator.setColumn(index);
				int dir = comparator.getDirection();
				tableViewer.getTable().setSortDirection(dir);
				tableViewer.getTable().setSortColumn(column);
				tableViewer.refresh();
			}
		};
		return selectionAdapter;
	}
	
	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {
		tableViewer.getControl().setFocus();
	}

	public void objectSelected(CompositeEvent compositeEvent) {
	}

	public void objectChanged(CompositeEvent compositeEvent) {
	}	
	
	private void updateLogs() {
		try {
			JSONArray logs = logManager.getLines();
			boolean interrupted = false;
	        while (logs.length() == 0 && !interrupted && thread != null) {
	        	synchronized (appender) {
					try {
						appender.wait(5000);
					} catch (InterruptedException e) {
						interrupted = true;
					}
				}
	        	logs = logManager.getLines();
	        }
			
	        List<String> extraList = new LinkedList<String>();
			List<String> messageList = new LinkedList<String>();
			for (int i=0; i < logs.length(); i++) {
				JSONArray logLine = (JSONArray) logs.get(i);
				String extra = "";
				String fullExtra = "";
				extraList.clear();
				messageList.clear();
				for (int j=5; j < logLine.length(); j++) {
					extra += logLine.getString(j) + "   ";
					fullExtra += logLine.getString(j) + "\n";
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
			    		for (int k=0; k < messageList.size(); k++) {
			    			if (k > 0) subLine = true;
			    			if (k < extraList.size() && extraList.size() != 0) {
				    			logLines.add(new LogLine(logLine.getString(0), logLine.getString(1), logLine.getString(2), 
										logLine.getString(3), messageList.get(k), extraList.get(k), subLine, counter, logLine.getString(4), fullExtra));
			    			} else {
			    				logLines.add(new LogLine(logLine.getString(0), logLine.getString(1), logLine.getString(2), 
										logLine.getString(3), messageList.get(k), " ", subLine, counter, logLine.getString(4), fullExtra));
			    			}
				    	}
			    		counter++;
			    	}
			    } else {
					logLines.add(new LogLine(logLine.getString(0), logLine.getString(1), logLine.getString(2), 
								logLine.getString(3), logLine.getString(4), extra, false, counter, logLine.getString(4), extra));
					counter++;
			    }
			}
		} catch (IOException e) {
			ConvertigoPlugin.logException(e, "Error while loading the Engine logs", true);
		} catch (JSONException e) {
			ConvertigoPlugin.logException(e, "Unable to process received Engine logs", true);
		}
	}
}