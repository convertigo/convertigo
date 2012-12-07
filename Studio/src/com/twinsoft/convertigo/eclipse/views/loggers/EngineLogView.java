package com.twinsoft.convertigo.eclipse.views.loggers;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
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
	private Thread logViewThread;
	private LogManager logManager;
	private List<LogLine> logLines = new LinkedList<LogLine>();
	private Appender appender;
	private EngineLogViewComparator comparator;
	private int counter = 0;
	private boolean realtime = false;
	private boolean scrollLock = false;
	private Action clearLogsAction, restoreLogsAction, hideLogsAction, realTimeLogsAction, stopLogsAction;
	private RetargetAction scrollLockAction;
	private EngineLogViewLabelProvider labelProvider;
	private static final String ENGINE_LOG_VIEW_COL_ORDER = "EngineLogView.COL_ORDER";
	private static final String ENGINE_LOG_VIEW_COL_HIDE = "EngineLogView.COL_HIDE";
	private static final String ENGINE_LOG_VIEW_COL_SIZE = "EngineLogView.COL_SIZE";
	private Text filterText, searchText;
	private Label infoSearch;
	private DateTime dateStart, dateEnd;
	private Combo hourStart, minuteStart, secondeStart, hourEnd, minuteEnd, secondeEnd;
	private Menu headerMenu, tableMenu;
	private MenuItem startDateItem, endDateItem, addVariableItem;
	private TableViewer tableViewer;
	private GridData gridDataCheck, gridDataDateTime, gridDataSearch;
	private Button applyOptions, previousSearch, nextSearch;
	private int selectedColumnIndex;
	private Composite compositeCheck, compositeDateTimeAndSearch, compositeDateTime, compositeSearch;
	public static final DateFormat date_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS"), setdate = new SimpleDateFormat("yyyy-MM-dd");
	private String[] titles = {"Message", "Level", "Category", "Time", "Thread", "ClientIP", "Project", "Connector", 
			"Transaction", "Sequence", "ContextID", "UID", "User", "ClientHostName"};
	private int[] defaultOrderColumns = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};
	private int[] defaultSizeColumns = {400, 50, 125, 150, 130, 45, 45, 45, 45, 45, 45, 45, 45, 45};
	private Button columns[] = new Button[defaultSizeColumns.length];
	private boolean[] defaultVisibilityColumns = {true, false, true, true, true, true, false, true, true, true, true, true, false, false};
	private boolean bStopLogViewThread = false;
	private int loopIndex = 0;
	private ArrayList<Integer> searchIndex = new ArrayList<Integer>();
	
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
   		Thread logViewThreadBackup = logViewThread;
   		logViewThread = null;
		synchronized (logViewThreadBackup) {
			logViewThreadBackup.notifyAll();
		}
   		
   		super.dispose();
	}

	private Composite mainComposite;
	
	@Override
	public void createPartControl(Composite parent) {
		mainComposite = parent;
		
		GridLayout gridLayout = new GridLayout();
		RowLayout gridLayoutCheck = new RowLayout(SWT.HORIZONTAL); gridLayoutCheck.wrap = true;
		RowLayout gridLayoutDTime = new RowLayout(SWT.HORIZONTAL); gridLayoutDTime.wrap = true;
		RowLayout gridLayoutSearch = new RowLayout(SWT.HORIZONTAL); gridLayoutSearch.wrap = true; 
		
		compositeCheck = new Composite(parent, SWT.NONE);
		compositeDateTimeAndSearch = new Composite(parent, SWT.NONE);
		compositeDateTimeAndSearch.setLayout(new GridLayout(2, false));
		GridData gridDataDT_Search = new GridData(GridData.FILL_HORIZONTAL);
		compositeDateTimeAndSearch.setLayoutData(gridDataDT_Search);
		compositeDateTime = new Composite(compositeDateTimeAndSearch, SWT.NONE);
		
		gridDataCheck = new GridData(SWT.FILL, SWT.VERTICAL, true, false);
		gridDataDateTime = new GridData(SWT.FILL, SWT.VERTICAL, true, false);
		gridDataDateTime.horizontalAlignment = GridData.HORIZONTAL_ALIGN_BEGINNING;
		gridDataSearch = new GridData(GridData.FILL_HORIZONTAL);
		
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
		
		compositeSearch = new Composite(compositeDateTimeAndSearch, SWT.BORDER);
		
		compositeSearch.setLayoutData(gridDataSearch);
		gridDataSearch.horizontalAlignment = GridData.HORIZONTAL_ALIGN_END;
		compositeSearch.setLayout(new GridLayout(4, false));
		
		searchText = new Text(compositeSearch, SWT.BORDER);
		infoSearch = new Label(compositeSearch, SWT.NONE); infoSearch.setEnabled(false); infoSearch.setVisible(false);
		previousSearch = new Button(compositeSearch, SWT.NONE); previousSearch.setText("^"); previousSearch.setEnabled(false);
		nextSearch = new Button(compositeSearch, SWT.NONE); nextSearch.setText("v"); nextSearch.setEnabled(false);
		

		filterText = new Text(parent, SWT.BORDER);
		filterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));  
		
		createActions();
		createToolbar();
		createViewer(parent);
		
		searchText.addListener(SWT.DefaultSelection, new Listener() {
			public void handleEvent(Event e) {
				searchIndex.clear();
				for (int i = 0 ; i < tableViewer.getTable().getItemCount(); i++){
					if(tableViewer.getTable().getItem(i).getText(0).contains(searchText.getText()))
						searchIndex.add(i);
				}
				if(searchIndex.isEmpty())
					infoSearch.setText("0");
				else{
					infoSearch.setText((loopIndex+1)+"/"+searchIndex.size());
					//On the first match
					tableViewer.getTable().setSelection(searchIndex.get(loopIndex));
					tableViewer.getTable().setFocus();
					nextSearch.setEnabled(true);
				}
				infoSearch.setVisible(true); 
				compositeSearch.layout();
			}
		});
		
		searchText.addMouseListener(new MouseListener() {
			
			public void mouseUp(MouseEvent arg0) {
				searchIndex.clear();
				infoSearch.setText("");
				searchText.setText("");
				compositeSearch.layout();
				nextSearch.setEnabled(false);
				previousSearch.setEnabled(false);
			}
			
			public void mouseDown(MouseEvent arg0) {}
			
			public void mouseDoubleClick(MouseEvent arg0) {}
		});
		
		nextSearch.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				navigationSearch(1, nextSearch, previousSearch);
			}
		});
		
		previousSearch.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				navigationSearch(-1, nextSearch, previousSearch);
			}
		});
		
		filterText.addListener(SWT.DefaultSelection, new Listener() {
			public void handleEvent(Event e) {
				clearLogs();
				setLogFilter();
			}
		});

		applyOptions.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateDates();
			}
		});
		
		comparator = new EngineLogViewComparator();
		tableViewer.setComparator(comparator);
		init();
	}
	
	private void navigationSearch(int sens, Button first, Button second){		
		loopIndex+=sens;
		tableViewer.getTable().setSelection(searchIndex.get(loopIndex));

		//Disable "second" if is the beginning
		if(searchIndex.get(loopIndex)==searchIndex.get(0))
			second.setEnabled(false);
		else
			second.setEnabled(true);
		
		//Disable "first" if is the end
		if(searchIndex.get(loopIndex)==searchIndex.get(searchIndex.size()-1))
			first.setEnabled(false);
		else
			first.setEnabled(true);
		infoSearch.setText((loopIndex+1)+"/"+searchIndex.size());
		compositeSearch.layout();
		tableViewer.getTable().setFocus();
	}
	
    public static boolean getColHideProperty(String key) {
    	IPreferenceStore preferenceStore = ConvertigoPlugin.getDefault().getPreferenceStore();      
        return preferenceStore.getString(key).equals("true") ? true : false;
    }
    
    public static void setColHideProperty(String key, boolean value) {
    	IPreferenceStore preferenceStore = ConvertigoPlugin.getDefault().getPreferenceStore();
    	preferenceStore.setValue(key, value);
    }
    
    public static int[] getColOrderProperty(String key) {
    	IPreferenceStore preferenceStore = ConvertigoPlugin.getDefault().getPreferenceStore(); 
    	int[] order = new int[14];
    	for (int i = 0 ; i < order.length; i++)
    		order[i] = preferenceStore.getInt(key+"_"+i);
    	return order;
    }
    
    public static void setColOrderProperty(String key, int[] value) {
    	IPreferenceStore preferenceStore = ConvertigoPlugin.getDefault().getPreferenceStore();
    	for(int i=0; i<value.length; i++)
    		preferenceStore.setValue(key+"_"+i, value[i]);
    }
	
    public static int getColSizeProperty(String key) {
    	IPreferenceStore preferenceStore = ConvertigoPlugin.getDefault().getPreferenceStore(); 
    	return preferenceStore.getInt(key);
    }
    
    public static void setColSizeProperty(String key, int value) {
    	IPreferenceStore preferenceStore = ConvertigoPlugin.getDefault().getPreferenceStore();
    	preferenceStore.setValue(key, value);
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
				ConvertigoPlugin.errorMessageBox("Start Date are after end date!");
			}
		} catch (ParseException ex) {
			ConvertigoPlugin.logException(ex, "Start Date are after end date");
		}
    }
    
    public void init(){
		initCheckColumns();
		createItemComboDateTime();
    	
	   	try{
			for (int i=0; i < tableViewer.getTable().getColumnCount(); i++) {
				boolean hide = getColHideProperty(ENGINE_LOG_VIEW_COL_HIDE + "_" + i);
				tableViewer.getTable().getColumn(i).setWidth(hide ? 0 : getColSizeProperty(ENGINE_LOG_VIEW_COL_SIZE + "_" + i));
				tableViewer.getTable().getColumn(i).setMoveable(!hide);
				tableViewer.getTable().getColumn(i).setResizable(!hide);
			}
			tableViewer.getTable().setColumnOrder(getColOrderProperty(ENGINE_LOG_VIEW_COL_ORDER));
			
    	}catch(Exception e){
    		//if no data saved
    		setColOrderProperty(ENGINE_LOG_VIEW_COL_ORDER, defaultOrderColumns);
    		for(int i = 0; i <titles.length; i++){			
				setColHideProperty(ENGINE_LOG_VIEW_COL_HIDE + "_" + i, defaultVisibilityColumns[i]);
				setColSizeProperty(ENGINE_LOG_VIEW_COL_SIZE + "_" + i, defaultSizeColumns[i]);
			}
    		init();
    	}
    }

	private void createActions() {
		stopLogsAction = new Action("Stop Loading Logs") {
			public void run() {
				bStopLogViewThread = true;
				if(realtime){
					clearLogs();
					realtime = false;
					realTimeLogsAction.setChecked(false);
					gridDataCheck.exclude = hideLogsAction.isChecked();
					compositeCheck.setVisible(!gridDataCheck.exclude);
					mainComposite.layout();
				}
			}
		};
		stopLogsAction.setImageDescriptor(ImageDescriptor.createFromImage(new Image(Display.getDefault(), getClass().getResourceAsStream("images/stop_logs_action.png"))));
		
		hideLogsAction = new RetargetAction("Toggle","Hide Log Parameters", IAction.AS_CHECK_BOX) {
			public void runWithEvent(Event event) {
				gridDataCheck.exclude = hideLogsAction.isChecked();
				gridDataDateTime.exclude = hideLogsAction.isChecked();
				gridDataSearch.exclude = hideLogsAction.isChecked();
				compositeDateTimeAndSearch.setVisible(!gridDataDateTime.exclude);
				compositeCheck.setVisible(!gridDataCheck.exclude);
				mainComposite.layout();
			}
		};
		
		hideLogsAction.setImageDescriptor(ImageDescriptor.createFromImage(new Image(Display.getDefault(), getClass().getResourceAsStream("images/hide_logs_parameters.png"))));
		hideLogsAction.setEnabled(true);

		realTimeLogsAction = new RetargetAction("Toggle","Real Time", IAction.AS_CHECK_BOX) {
			public void runWithEvent(Event event) {
				boolean bRealtime = realTimeLogsAction.isChecked();
				gridDataDateTime.exclude = bRealtime;
				compositeDateTime.setVisible(!gridDataDateTime.exclude);
				mainComposite.layout();
				clearLogs();
				realtime = bRealtime;
				initDateTime();
				setLogFilter();
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
				for(int i = 0; i < columns.length; i++){
					//set check
					columns[i].setSelection(defaultVisibilityColumns[i]);
					//set visible column
					tableViewer.getTable().getColumn(i).setWidth((!defaultVisibilityColumns[i] ? 0 : getColSizeProperty(ENGINE_LOG_VIEW_COL_SIZE+"_"+i)));
					tableViewer.getTable().getColumn(i).setResizable(defaultVisibilityColumns[i]);
					//set header column menu
					headerMenu.getItem(i).setSelection(defaultVisibilityColumns[i]);
				}
				tableViewer.getTable().setColumnOrder(defaultOrderColumns);
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
		manager.add(stopLogsAction);
		manager.add(hideLogsAction);
		manager.add(realTimeLogsAction);
		manager.add(restoreLogsAction);
		manager.add(clearLogsAction);
		manager.add(scrollLockAction);
	}
	
	private void createItemComboDateTime(){
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
		//if no dates saved
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
					column.setWidth(columns[j].getSelection() ? defaultSizeColumns[j] : 0);
					column.setResizable(columns[j].getSelection());
					headerMenu.getItem(j).setSelection(columns[j].getSelection());					
					setColHideProperty(ENGINE_LOG_VIEW_COL_HIDE+"_"+j, !tableViewer.getTable().getColumn(j).getResizable());
					setColSizeProperty(ENGINE_LOG_VIEW_COL_SIZE+"_"+j, tableViewer.getTable().getColumn(j).getWidth());
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
		
		tableViewer = new TableViewer(compositeTableViewer, 
				SWT.RESIZE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER | SWT.VERTICAL | SWT.FILL);
		
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
					startDateItem.setEnabled(tableViewer.getTable().getColumn(selectedColumnIndex).getText().toLowerCase().equals("time") ? 
							true : false);		
					endDateItem.setEnabled(tableViewer.getTable().getColumn(selectedColumnIndex).getText().toLowerCase().equals("time") ? 
							true : false);
					addVariableItem.setEnabled(selectedColumnIndex >4 && selectedColumnIndex < 14 ? true : false);
			}
		});
		/* IMPORTANT: Dispose the menus (only the current menu, set with setMenu(), will be automatically disposed) */
		table.addListener(SWT.Dispose, new Listener() {
			public void handleEvent(Event event) {
				headerMenu.dispose();
				tableMenu.dispose();
			}
		});
		
		logViewThread = new Thread(new Runnable() {
			public void run() {
				try {
					// Wait for the Convertigo engine fully starts the log objects
					while (Engine.logConvertigo == null) {
							Thread.sleep(180);
					}
			        Engine.logConvertigo.addAppender(appender);

					logManager = new LogManager();
					logManager.setContinue(true);
					logManager.setDateStart(new Date(new Date().getTime() - 600000));
					logManager.setMaxLines(50);

					//TODO
					// While the log view thread is the relevant thread (i.e. if we close the view and reopen it,
					// we will have another log view thread instance, and thus the first one should quit).
					while (Thread.currentThread() == logViewThread) { 

						while (!bStopLogViewThread && (realtime || logManager.hasMoreResults())) {
							// Get the newest available lines
							updateLogs();
							
							// Refresh the list view
							Display.getDefault().asyncExec(new Runnable() {
								public void run() {
									tableViewer.refresh();
									if (!scrollLock) {
										tableViewer.reveal(tableViewer.getElementAt(tableViewer.getTable().getItemCount()-1));
									}
								}
							});

							// We must release some CPU time in order to allow the GUI to be refreshed
							Thread.sleep(540);
						}
						// Wait for option changes
						synchronized(logViewThread) {
			        		// TODO: enabled filter and options
							logViewThread.wait();
						}
					}
				} catch (InterruptedException e) {
					ConvertigoPlugin.logException(e, "The \"StudioLogViewer\" thread has been interrupted");
				} 
			}
		});
		logViewThread.setDaemon(true);
		logViewThread.setName("EngineLogViewerThread");
		logViewThread.start();
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
			case 0 :
				break;
			case 1: 
				text = line.getMessage();
				break;
			case 2:
				text = line.getLevel();
				break;
			case 3:
				text = line.getCategory();
				break;
			case 4:
				text = line.getTime();
				break;
			case 5:
				text = line.getThread();
				break;
			case 6:
				text = line.getClientIP();
				break;
			case 7:
				text = line.getConnector();
				break;
			case 8:
				text = line.getContextID();
				break;			
			case 9:
				text = line.getProject();
				break;
			case 10:
				text = line.getTransaction();
				break;			
			case 11:
				text = line.getUID();
				break;			
			case 12:
				text = line.getUser();
				break;
			case 13:
				text = line.getSequence();
				break;
			case 14:
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
		counter=0;
	}

	private void setLogFilter() {
		try {
    		// TODO: disabled options and filter GUI
    		synchronized (logViewThread) {
				logViewThread.notifyAll();	
				logManager.setFilter(filterText.getText());
			}
		} catch (ServiceException e) {
			ConvertigoPlugin.logException(e, "Unable to set logs filter", true);
		}
	}

	private void createColumns(final Composite parent, final TableViewer viewer) {
		headerMenu = new Menu(parent.getShell(), SWT.POP_UP);
		viewer.getTable().setMenu(headerMenu);
		
		TableColumnLayout layout = new TableColumnLayout();
		parent.setLayout(layout);
		TableViewerColumn col; 
		
		for (int i = 0; i < titles.length ; i++){
			final int j = i;
			col = createTableViewerColumn(i);
			layout.setColumnData(col.getColumn(), new ColumnWeightData(4, ColumnWeightData.MINIMUM_WIDTH, true));
			createMenuItem(headerMenu, col.getColumn());
			headerMenu.getItem(i).addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent arg0) {
					columns[j].setSelection(headerMenu.getItem(j).getSelection());
					setColHideProperty(ENGINE_LOG_VIEW_COL_HIDE+"_"+j, !tableViewer.getTable().getColumn(j).getResizable());
				}
				public void widgetDefaultSelected(SelectionEvent arg0) {}
			});
		}
		
	} 
	
	private TableViewerColumn createTableViewerColumn(final int colNumber) {	
		TableViewerColumn viewerColumn = new TableViewerColumn(tableViewer, SWT.VIRTUAL);
		TableColumn column = viewerColumn.getColumn();
			column.setText(titles[colNumber]);
			column.setWidth(defaultSizeColumns[colNumber]);
			column.addControlListener(new ControlListener() {
				public void controlResized(ControlEvent arg0) {
					if(counter!=0){
						for(int i = 0; i < tableViewer.getTable().getColumnCount(); i++)
							setColSizeProperty(ENGINE_LOG_VIEW_COL_SIZE+"_"+i, tableViewer.getTable().getColumn(i).getWidth());
					}
				}
				public void controlMoved(ControlEvent arg0) {
					if(counter!=0)
						setColOrderProperty(ENGINE_LOG_VIEW_COL_ORDER, tableViewer.getTable().getColumnOrder());
				}
			});
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
				column.setWidth(itemName.getSelection() ? getColSizeProperty(ENGINE_LOG_VIEW_COL_SIZE+"_"+itemName.getID()) : 0);
				column.setResizable(itemName.getSelection() ? true : false);
				column.setMoveable(itemName.getSelection() ? true : false);
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
	public void objectSelected(CompositeEvent compositeEvent) {}
	public void objectChanged(CompositeEvent compositeEvent) {}	
	
	private void updateLogs() {
		try {
			JSONArray logs = logManager.getLines();
			boolean interrupted = false;
	        while (logs.length() == 0 && !interrupted && logViewThread != null) {
	        	synchronized (appender) {
					try {
						appender.wait(300);
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
	}
}
