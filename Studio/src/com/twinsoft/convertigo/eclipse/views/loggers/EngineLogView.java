package com.twinsoft.convertigo.eclipse.views.loggers;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
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
import org.eclipse.jface.preference.IPreferenceStore;
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
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
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
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.editors.CompositeListener;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.logmanager.LogManager;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;

public class EngineLogView extends ViewPart implements CompositeListener {
	private Thread thread;
	private LogManager logManager;
	private List<LogLine> logLines = new LinkedList<LogLine>();
	private Appender appender;
	private EngineLogViewComparator comparator;
	private int counter = 0;
	private static boolean realtime = false;
	private boolean scrollLock = false;
	private Action clearLogsAction, restoreLogsAction, hideLogsAction, realTimeLogsAction;
	private RetargetAction scrollLockAction;
	private EngineLogViewLabelProvider labelProvider;
	private static final String ENGINE_LOG_VIEW_COL_ORDER = "EngineLogView.COL_ORDER";
	private static final String ENGINE_LOG_VIEW_COL_HIDE = "EngineLogView.COL_HIDE";
	private Text filterText;
	private DateTime dateStart, dateEnd;
	private Combo hourStart, minuteStart, secondeStart, hourEnd, minuteEnd, secondeEnd;
	private Menu headerMenu, tableMenu;
	private MenuItem startDateItem, endDateItem, addVariableItem;
	private TableViewer tableViewer;
	private GridData gridDataCheck, gridDataDateTime;
	private Button applyOptions;
	private int selectedColumnIndex;
	private Button columns[] = new Button[14];
	private Composite compositeCheck, compositeDateTime;
	public static final DateFormat date_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS"), setdate = new SimpleDateFormat("yyyy-MM-dd");
	private String[] titles = {"Message", "Level", "Category", "Time", "Thread", "ClientIP", "Project", "Connector", "Transaction", "Sequence", "ContextID", "UID", "User", "ClientHostName"};
	private int[] bounds = {400, 50, 125, 150, 130, 50, 50, 50, 50, 50, 50, 50, 50, 50};
	private boolean firstLaunch = true;
	
	public EngineLogView() {		
		labelProvider = new EngineLogViewLabelProvider();
		appender = new AppenderSkeleton() {
			
        	@Override
        	protected void append(LoggingEvent arg0) {
        		synchronized (this) {
					this.notifyAll();
				}
        	}
        	
			public void close() {}

			public boolean requiresLayout() {
				return false;
			}
        };
	}
	
	@Override
	public void dispose() {
   		Engine.logConvertigo.removeAppender(appender);
   		super.dispose();
	}

	private Composite mainComposite;
	
	@Override
	public void createPartControl(Composite parent) {
		mainComposite = parent;
		
		GridLayout gridLayout = new GridLayout();
		RowLayout gridLayoutCheck = new RowLayout(SWT.HORIZONTAL); gridLayoutCheck.wrap = true;
		RowLayout gridLayoutDTime = new RowLayout(SWT.HORIZONTAL); gridLayoutDTime.wrap = true;
		compositeCheck = new Composite(parent, SWT.NONE);
		compositeDateTime = new Composite(parent, SWT.NONE);

		gridDataCheck = new GridData(SWT.FILL, SWT.VERTICAL, true, false);
		gridDataCheck.horizontalAlignment = SWT.HORIZONTAL;
		gridDataDateTime = new GridData(SWT.FILL, SWT.VERTICAL, true, false);
		gridDataDateTime.horizontalAlignment = SWT.HORIZONTAL;
		
		parent.setLayout(gridLayout);
		compositeCheck.setLayoutData(gridDataCheck);
		compositeCheck.setLayout(gridLayoutCheck);
		compositeDateTime.setLayoutData(gridDataDateTime);
		compositeDateTime.setLayout(gridLayoutDTime);
		
		Label checkColumns = new Label(compositeCheck, SWT.PUSH);
		checkColumns.setText("Selected columns:   ");
		createCheckColumns();
		
		Label dateFilter = new Label(compositeDateTime, SWT.NONE);
		dateFilter.setText("Start Date:  ");
		dateStart = new DateTime(compositeDateTime, SWT.DROP_DOWN);
		Label timeFilter = new Label(compositeDateTime, SWT.NONE);
		timeFilter.setText("Time:");
		hourStart = new Combo(compositeDateTime, SWT.PUSH | SWT.READ_ONLY);
		timeFilter = new Label(compositeDateTime, SWT.NONE);
		timeFilter.setText(":");
		minuteStart = new Combo(compositeDateTime, SWT.PUSH | SWT.READ_ONLY);
		timeFilter = new Label(compositeDateTime, SWT.NONE);
		timeFilter.setText(":");
		secondeStart = new Combo(compositeDateTime, SWT.PUSH | SWT.READ_ONLY);
		
		dateFilter = new Label(compositeDateTime, SWT.NONE);
		dateFilter.setText(" - End Date: ");
		dateEnd = new DateTime(compositeDateTime, SWT.DROP_DOWN);
		timeFilter = new Label(compositeDateTime, SWT.NONE);
		timeFilter.setText("Time:"); 	
		hourEnd = new Combo(compositeDateTime, SWT.PUSH | SWT.READ_ONLY);
		timeFilter = new Label(compositeDateTime, SWT.NONE);
		timeFilter.setText(":"); 	
		minuteEnd = new Combo(compositeDateTime, SWT.PUSH | SWT.READ_ONLY);
		timeFilter = new Label(compositeDateTime, SWT.NONE);
		timeFilter.setText(":");
		secondeEnd = new Combo(compositeDateTime, SWT.PUSH | SWT.READ_ONLY);
		
		applyOptions = new Button(compositeDateTime, SWT.PUSH);
		applyOptions.setText("Apply options");
		
		filterText = new Text(parent,  SWT.BORDER | SWT.VERTICAL | SWT.FILL);
		filterText.setLayoutData(new GridData(SWT.FILL, SWT.VERTICAL, true, false));
		
		createActions();
		createToolbar();
		
		filterText.addListener(SWT.DefaultSelection, new Listener() {
			public void handleEvent(Event e) {
				clearLogs();
				setLogFilter();
			}
		});

		applyOptions.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				firstLaunch = true;
				updateDates();
			}
		});
		createViewer(parent);
		initCheckColumns();
		initComboDateTime();
		
		comparator = new EngineLogViewComparator();
		tableViewer.setComparator(comparator);
		init();
	}

    public static boolean getColHideProperty(String key) {
    	IPreferenceStore preferenceStore = ConvertigoPlugin.getDefault().getPreferenceStore();      
        return preferenceStore.getString(key).equals("true") ? true : false;
    }
    
    public static void setColHideProperty(String key, Boolean value) {
    	IPreferenceStore preferenceStore = ConvertigoPlugin.getDefault().getPreferenceStore();
    	preferenceStore.setValue(key, value);
    }
    
    public static int getColOrderProperty(String key) {
    	IPreferenceStore preferenceStore = ConvertigoPlugin.getDefault().getPreferenceStore(); 
    	return preferenceStore.getInt(key);
    }
    
    public static void setColOrderProperty(String key, int value) {
    	IPreferenceStore preferenceStore = ConvertigoPlugin.getDefault().getPreferenceStore();
    	preferenceStore.setValue(key, value+"");
    }
	
    public static void setRealTime(boolean rltime){
    	realtime = rltime;
    }
    
    public static boolean getRealTime(){
        return realtime;
    }
    
    public void updateDates(){
    	String completeDateStart = dateStart.getYear()+"-"+((dateStart.getMonth()+1)<10 ? "0"+dateStart.getMonth()+1 : dateStart.getMonth()+1)+"-"+(dateStart.getDay()<10 ? "0"+dateStart.getDay() : dateStart.getDay())+" "+hourStart.getText()+":"+minuteStart.getText()+":"+secondeStart.getText()+",000";					
		String completeDateEnd = dateEnd.getYear()+"-"+((dateEnd.getMonth()+1)<10 ? "0"+dateEnd.getMonth()+1 : dateEnd.getMonth()+1)+"-"+(dateEnd.getDay()<10 ? "0"+dateEnd.getDay() : dateEnd.getDay())+" "+hourEnd.getText()+":"+minuteEnd.getText()+":"+secondeEnd.getText()+",999";
		
		try {
			if(date_format.parse(completeDateStart).before(date_format.parse(completeDateEnd))){
				if(!logManager.getDateStart().toString().equals(date_format.parse(completeDateStart).toString()) || 
						!logManager.getDateEnd().toString().equals(date_format.parse(completeDateEnd).toString()) || 
								!logManager.getCurrentFilter().equals(filterText.getText()))
					clearLogs();
				logManager.setDateStart(date_format.parse(completeDateStart));
				logManager.setDateEnd(date_format.parse(completeDateEnd));
				setLogFilter();
			}
			else{
				ConvertigoPlugin.errorMessageBox("The start date/end date isn't valid!");
			}
		} catch (ParseException ex) {
			ConvertigoPlugin.logException(ex, "The start date/end date isn't valid!");
		}
    }
    
    public void init(){
    	if(getColOrderProperty(ENGINE_LOG_VIEW_COL_ORDER + "_" + 6)==0 && getColOrderProperty(ENGINE_LOG_VIEW_COL_ORDER + "_" + 1)==0){
    		for (int i=0; i < tableViewer.getTable().getColumnCount(); i++) {
    			setColOrderProperty(ENGINE_LOG_VIEW_COL_ORDER + "_" + i, i);
    			if(i==1 || i==6 || i==12 || i==16)
    				setColHideProperty(ENGINE_LOG_VIEW_COL_HIDE + "_" + i, true);
    			else
    				setColHideProperty(ENGINE_LOG_VIEW_COL_HIDE + "_" + i, false);
    		}
    	}
		int[] columnOrder = new int[tableViewer.getTable().getColumnCount()];
		for (int i=0; i < tableViewer.getTable().getColumnCount(); i++) {
			int order = getColOrderProperty(ENGINE_LOG_VIEW_COL_ORDER + "_" + i);
			columnOrder[i] = order;
			boolean hide = getColHideProperty(ENGINE_LOG_VIEW_COL_HIDE + "_" + i);
			if (hide) {
				tableViewer.getTable().getColumn(i).setWidth(0);
				tableViewer.getTable().getColumn(i).setResizable(false);
			}
		}
		tableViewer.getTable().setColumnOrder(columnOrder);	
    }

	private void createActions() {
		hideLogsAction = new RetargetAction("Toggle","Hide Log Parameters", IAction.AS_CHECK_BOX) {
			public void runWithEvent(Event event) {
				gridDataCheck.exclude = hideLogsAction.isChecked();
				gridDataDateTime.exclude = hideLogsAction.isChecked();
				compositeDateTime.setVisible(!gridDataDateTime.exclude);
				compositeCheck.setVisible(!gridDataCheck.exclude);
				mainComposite.layout();
			}
		};
		
		hideLogsAction.setImageDescriptor(ImageDescriptor.createFromImage(new Image(Display.getDefault(), getClass().getResourceAsStream("images/hide_logs_parameters.png"))));
		hideLogsAction.setEnabled(true);

		realTimeLogsAction = new RetargetAction("Toggle","Real Time", IAction.AS_CHECK_BOX) {
			public void runWithEvent(Event event) {
				gridDataDateTime.exclude = realTimeLogsAction.isChecked();
				compositeDateTime.setVisible(!gridDataDateTime.exclude);
				mainComposite.layout();
				clearLogs();
				setRealTime(realTimeLogsAction.isChecked());
				if(!getRealTime()){
					initDateTime();
					updateDates();
				}
			}
		};
		
		realTimeLogsAction.setImageDescriptor(ImageDescriptor.createFromImage(new Image(Display.getDefault(), getClass().getResourceAsStream("images/real_time_logs.png"))));
		realTimeLogsAction.setEnabled(true);
		
		restoreLogsAction = new Action("Restore Log Viewer") {
			public void run() {
				//restore date and time
				initDateTime();
				//clean filter
				filterText.setText("");
				setLogFilter();
			}
		};
		restoreLogsAction.setImageDescriptor(ImageDescriptor.createFromImage(new Image(Display.getDefault(), getClass().getResourceAsStream("images/restore_logs.png"))));
		
		clearLogsAction = new Action("Clear Log Viewer") {
			public void run() {
				clearLogs();
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
		manager.add(hideLogsAction);
		manager.add(realTimeLogsAction);
		manager.add(restoreLogsAction);
		manager.add(clearLogsAction);
		manager.add(scrollLockAction);
	}
	
	private void initComboDateTime(){
		for (int i = 0; i < 10; i++) {
			hourStart.add("0" + i); hourEnd.add("0" + i);
			minuteStart.add("0" + i); minuteEnd.add("0" + i);
			secondeStart.add("0" + i); secondeEnd.add("0" + i);
		}
		for (int i = 10; i < 24; i++) {
			hourStart.add(i+""); hourEnd.add(i+"");
		}

		for (int i = 10; i < 60; i++) {
			minuteStart.add("" + i); minuteEnd.add("" + i);
			secondeStart.add("" + i); secondeEnd.add("" + i);
		}
		initDateTime();
	}
	
	private void initDateTime(){
		Date currentDate = new Date();
		Calendar calendar = GregorianCalendar.getInstance();
		currentDate.setTime(currentDate.getTime()-600000);
		calendar.setTime(currentDate);
		dateStart.setData(currentDate);
		dateEnd.setData(currentDate);
		
		hourStart.select((calendar.get(Calendar.HOUR)==24 ? 00 : ( calendar.get(Calendar.AM_PM) == Calendar.AM ? calendar.get(Calendar.HOUR) : calendar.get(Calendar.HOUR)+12)));
		minuteStart.select(calendar.get(Calendar.MINUTE));
		secondeStart.select(calendar.get(Calendar.SECOND));
		
		hourEnd.select(23);
		minuteEnd.select(59);
		secondeEnd.select(59);
	}
	
	private void createCheckColumns(){	
		for (int i = 0 ; i < titles.length ; i++){
			columns[i] = new Button(compositeCheck, SWT.CHECK);
			columns[i].setText(titles[i]);
		}
	}
	
	private void initCheckColumns(){
		for (int i = 0 ; i < titles.length ; i++){	
			final int j = i;
			columns[i].setSelection(!getColHideProperty(ENGINE_LOG_VIEW_COL_HIDE+"_"+i));
			headerMenu.getItem(i).setSelection(!getColHideProperty(ENGINE_LOG_VIEW_COL_HIDE+"_"+i));
			columns[i].addListener(SWT.Selection, new Listener(){
				public void handleEvent(Event e) {
					TableColumn column = tableViewer.getTable().getColumn(j);
					if(columns[j].getSelection()){
						column.setWidth(bounds[j]);
						column.setResizable(true);
						setColHideProperty(ENGINE_LOG_VIEW_COL_HIDE+"_"+j, false);
						headerMenu.getItem(j).setSelection(true);
					}else{
					    column.setWidth(0);
					    column.setResizable(false);
						setColHideProperty(ENGINE_LOG_VIEW_COL_HIDE+"_"+j, true);
						headerMenu.getItem(j).setSelection(false);
					}
				}
			});
		}
	}
	private void setDateTimeStart(String theDate){
		try {
			Date start = new Date(); start = date_format.parse(theDate);
			Calendar calendar = GregorianCalendar.getInstance();
			calendar.setTime(start);
			
			dateStart.setData(setdate.parse(theDate));
			hourStart.select((calendar.get(Calendar.HOUR)==24 ? 00 : calendar.get(Calendar.HOUR)+12));
			minuteStart.select(calendar.get(Calendar.MINUTE));
			secondeStart.select(calendar.get(Calendar.SECOND));
		} catch (ParseException e) {
			ConvertigoPlugin.logException(e, "Wrong format for start date!");
		}
	}
	
	private void setDateTimeEnd(String theDate){
		try {
			Date end = new Date(); end = date_format.parse(theDate);
			Calendar calendar = GregorianCalendar.getInstance();
			calendar.setTime(end);
			
			dateEnd.setData(setdate.parse(theDate));
			hourEnd.select((calendar.get(Calendar.HOUR)==24 ? 00 : calendar.get(Calendar.HOUR)+12));
			minuteEnd.select(calendar.get(Calendar.MINUTE));
			secondeEnd.select(calendar.get(Calendar.SECOND));
		} catch (ParseException e) {
			ConvertigoPlugin.logException(e, "Wrong format for end date!");
		}
	}
	
	private void createViewer(Composite parent) {
		Composite compositeTableViewer = new Composite(parent, SWT.NONE);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		compositeTableViewer.setLayoutData(gridData);
		
		tableViewer = new TableViewer(compositeTableViewer, SWT.RESIZE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER | SWT.VERTICAL | SWT.FILL);
		
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
				if(tableViewer.getCell(pt)!=null)
					selectedColumnIndex = tableViewer.getCell(pt).getColumnIndex();
				table.setMenu(header ? headerMenu : tableMenu);
				if(tableViewer.getTable().getColumn(selectedColumnIndex).getText().toLowerCase().equals("time")){
					startDateItem.setEnabled(true);		
					endDateItem.setEnabled(true);
				}else{
					startDateItem.setEnabled(false);
					endDateItem.setEnabled(false);
				}
				if(selectedColumnIndex >4 && selectedColumnIndex < 14)
					addVariableItem.setEnabled(true);		
				else
					addVariableItem.setEnabled(false);
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
					logManager.setDateStart(new Date(new Date().getTime() - 600000));
					logManager.setMaxLines(50);
					
					while (Thread.currentThread() == thread){
						if(getRealTime() || firstLaunch) {
							Thread.sleep(200);
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
		//Add "equals" command
		tableMenu = new Menu(parent.getShell(), SWT.POP_UP);
		MenuItem item = new MenuItem(tableMenu, SWT.PUSH);
		item.setText("Add \"equals\" command");
		item.setImage(new Image(Display.getDefault(), getClass().getResourceAsStream("images/log_ctx_menu_add_equals.png")));
		item.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
				setFilterText(selection, 0);
			}
		});

		//Add "contains" command
		item = new MenuItem(tableMenu, SWT.PUSH);
		item.setText("Add \"contains\" command");
		item.setImage(new Image(Display.getDefault(), getClass().getResourceAsStream("images/log_ctx_menu_add_contains.png")));
		item.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
				setFilterText(selection, 1);
			}
		});

		//Add "start with" command
		new MenuItem(tableMenu, SWT.SEPARATOR);
		item = new MenuItem(tableMenu, SWT.PUSH);
		item.setText("Add \"starts with\" command");
		item.setImage(new Image(Display.getDefault(), getClass().getResourceAsStream("images/log_ctx_menu_add_startswith.png")));
		item.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
				setFilterText(selection, 2);
			}
		});
		
		//Add "end with" command
		item = new MenuItem(tableMenu, SWT.PUSH);
		item.setText("Add \"ends with\" command");
		item.setImage(new Image(Display.getDefault(), getClass().getResourceAsStream("images/log_ctx_menu_add_endswith.png")));
		item.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
				setFilterText(selection, 3);
			}
		});
		
		//Add variable command
		new MenuItem(tableMenu, SWT.SEPARATOR);
		addVariableItem = new MenuItem(tableMenu, SWT.PUSH);
		addVariableItem.setText("Add variable");
		addVariableItem.setImage(new Image(Display.getDefault(), getClass().getResourceAsStream("images/log_ctx_menu_add_variable.png")));
		addVariableItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
				setFilterText(selection, 4);
			}
		});
		addVariableItem.setEnabled(false);
		
		//Add "start date" command
		new MenuItem(tableMenu, SWT.SEPARATOR);
		startDateItem = new MenuItem(tableMenu, SWT.PUSH);
		startDateItem.setText("Set start date");
		startDateItem.setImage(new Image(Display.getDefault(), getClass().getResourceAsStream("images/log_ctx_menu_set_startdate.png")));
		startDateItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
				setFilterText(selection, 5);
			}
		});
		
		//Add "end date" command
		endDateItem = new MenuItem(tableMenu, SWT.PUSH);
		endDateItem.setText("Set end date");
		endDateItem.setImage(new Image(Display.getDefault(), getClass().getResourceAsStream("images/log_ctx_menu_set_enddate.png")));
		endDateItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
				setFilterText(selection, 6);
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
				text = line.getClientIP();
				break;
			case 6:
				text = line.getConnector();
				break;
			case 7:
				text = line.getContextID();
				break;			
			case 8:
				text = line.getProject();
				break;
			case 9:
				text = line.getTransaction();
				break;			
			case 10:
				text = line.getUID();
				break;			
			case 11:
				text = line.getUser();
				break;
			case 12:
				text = line.getSequence();
				break;
			case 13:
				text = line.getClientHostName();
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
			String txt;
			if (filter.contains("==") || filter.contains("contains") || filter.contains("startsWith") || filter.contains("endsWith")) {
				filter = filter + " and ";
			}
			switch (buttonIndex) {
				case 0:
					txt = filter + "(" + variableName + " == \"" + cellValue.replaceAll("\"", "\\\\\"") + "\")";
					filterText.setText(txt);
					break;
				case 1:
					txt = filter + "(" + variableName + ".contains(\"" + cellValue.replaceAll("\"", "\\\\\"") + "\"))";
					filterText.setText(txt);
					break;
				case 2:
					txt = filter + "(" + variableName + ".startsWith(\"" + cellValue.replaceAll("\"", "\\\\\"")  + "\"))";
					filterText.setText(txt);
					break;
				case 3:
					txt = filter + "(" + variableName + ".endsWith(\"" + cellValue.replaceAll("\"", "\\\\\"")  + "\"))";
					filterText.setText(txt);
					break;
				case 4:
					//Add variable
					txt = filter + "(" + variableName + " == \"" + ((cellValue != null && cellValue != "") ? cellValue.replaceAll("\"", "\\\\\"") : "undefined" )+ "\")";
					filterText.setText(txt);
					break;
				case 5:
					//Set start date
					setDateTimeStart(cellValue);
					break;
				case 6:
					//Set end date
					setDateTimeEnd(cellValue);
					break;
				default:
					break;
			}
		}
	}
	
	private void clearLogs() {
		logLines.clear();
		tableViewer.getTable().clearAll();
		tableViewer.refresh();
	}

	private void setLogFilter() {
		try {
			logManager.setFilter(filterText.getText());
		} catch (ServiceException e) {
			ConvertigoPlugin.logException(e, "Unable to set logs filter", true);
		}
	}

	private void createColumns(final Composite parent, final TableViewer viewer) {
		headerMenu = new Menu(parent.getShell(), SWT.POP_UP);
		viewer.getTable().setMenu(headerMenu);
		
		TableColumnLayout layout = new TableColumnLayout();
		parent.setLayout(layout);
		
		TableViewerColumn col = createTableViewerColumn(titles[0], bounds[0], 0);;
		layout.setColumnData(col.getColumn(), new ColumnWeightData(20, ColumnWeightData.MINIMUM_WIDTH, true));
		createMenuItem(headerMenu, col.getColumn());

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
		
		for (int i = 5 ; i < titles.length ; i++){
			col = createTableViewerColumn(titles[i], bounds[i], i);
			layout.setColumnData(col.getColumn(), new ColumnWeightData(5, ColumnWeightData.MINIMUM_WIDTH, true));
			createMenuItem(headerMenu, col.getColumn());
		}
		//Order columns
		TableColumn tab[] = tableViewer.getTable().getColumns();
		for (int i = 0; i < tableViewer.getTable().getColumnCount(); i++){
			tab[i].addControlListener(new ControlListener() {
				public void controlResized(ControlEvent arg0) {}
				public void controlMoved(ControlEvent arg0) {
					int tab[] = tableViewer.getTable().getColumnOrder();
					for(int x = 0 ; x < titles.length; x++){
						setColOrderProperty(ENGINE_LOG_VIEW_COL_ORDER+"_"+x, tab[x]);
					}
				}
			});
		}
		final MenuItem[] header = headerMenu.getItems();
		for (int i = 0; i < headerMenu.getItemCount(); i++){
			final int j = i;
			header[i].addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent arg0) {
					setColHideProperty(ENGINE_LOG_VIEW_COL_HIDE+"_"+j, !header[j].getSelection());
					columns[j].setSelection(header[j].getSelection());
				}
				public void widgetDefaultSelected(SelectionEvent arg0) {}
			});
		}
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
						appender.wait(500);
					} catch (InterruptedException e) {
						interrupted = true;
					}
				}
	        	logs = logManager.getLines();
	        }
			
	        List<String> extraList = new LinkedList<String>();
			List<String> messageList = new LinkedList<String>();
			HashMap<String, String> allExtras = new HashMap<String, String>();
			for (int i=0; i < logs.length(); i++) {
				JSONArray logLine = (JSONArray) logs.get(i);
				String extract, extra = "";
				extraList.clear();
				messageList.clear();
				for (int j=5; j < logLine.length(); j++){
					extract = logLine.getString(j) + ";";
					allExtras.put(extract.substring(0, extract.indexOf("=")), extract.substring(extract.indexOf("=")+1, extract.indexOf(";")));
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
			    		for (int k=0; k < messageList.size(); k++) {
			    			if (k > 0) subLine = true;
			    			if (k < extraList.size() && extraList.size() != 0) {
				    			logLines.add(new LogLine(logLine.getString(0), logLine.getString(1), logLine.getString(2), 
										logLine.getString(3), messageList.get(k), extraList.get(k), subLine, counter, logLine.getString(4), allExtras));
			    			} else {
			    				logLines.add(new LogLine(logLine.getString(0), logLine.getString(1), logLine.getString(2), 
										logLine.getString(3), messageList.get(k), " ", subLine, counter, logLine.getString(4), allExtras));
			    			}
				    	}
			    		counter++;
			    	}
			    } else {
		    		logLines.add(new LogLine(logLine.getString(0), logLine.getString(1), logLine.getString(2), 
								logLine.getString(3), logLine.getString(4), extra, false, counter, logLine.getString(4), allExtras));
					counter++;
			    }
			}
		} catch (IOException e) {
			ConvertigoPlugin.logException(e, "Error while loading the Engine logs", true);
		} catch (JSONException e) {
			ConvertigoPlugin.logException(e, "Unable to process received Engine logs", true);
		}
		logManager.setContinue(true);
		if(!logManager.hasMoreResults())
			firstLaunch = false;
	}
}
