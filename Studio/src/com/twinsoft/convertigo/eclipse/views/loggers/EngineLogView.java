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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.RetargetAction;
import org.eclipse.ui.part.ViewPart;

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
	final int TEXT_MARGIN = 0;
	private boolean scrollLock = false;
	private Action clearLogsAction;
	private RetargetAction scrollLockAction;
	private EngineLogViewLabelProvider labelProvider;
	private static final String ENGINE_LOG_VIEW_COL_ORDER = "EngineLogView.COL_ORDER";
	private IMemento memento;
	
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
		final Text filterText = new Text(parent, SWT.BORDER | SWT.SEARCH);
		filterText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		
		createActions();
		createToolbar();
		
		filterText.addListener(SWT.DefaultSelection, new Listener() {
		      public void handleEvent(Event e) {
		          try {
					logManager.setFilter(filterText.getText());
					logManager.setContinue(true);
				} catch (ServiceException e1) {
					e1.printStackTrace();
				}
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
			}
			tableViewer.getTable().setColumnOrder(columnOrder);
		}
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		// TODO Auto-generated method stub
		super.init(site, memento);
		this.memento = memento;
	}

	@Override
	public void saveState(IMemento memento) {
		// TODO Auto-generated method stub
		super.saveState(memento);
		Table table = tableViewer.getTable();
		int[] columnOrder = new int[table.getColumnCount()];
		columnOrder = table.getColumnOrder();
		for (int i=0; i < columnOrder.length; i++) {
			memento.putInteger(ENGINE_LOG_VIEW_COL_ORDER + "_" + i, columnOrder[i]);
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
		
		Table table = tableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		/*
		 * NOTE: MeasureItem, PaintItem and EraseItem are called repeatedly.
		 * Therefore, it is critical for performance that these methods be
		 * as efficient as possible.
		 */	
		table.addListener(SWT.EraseItem, new Listener() {
			public void handleEvent(Event event) {
				event.detail &= ~SWT.FOREGROUND;
			}
		});
		
		table.addListener(SWT.PaintItem, new Listener() {
			public void handleEvent(Event event) {
				TableItem item = (TableItem)event.item;
				String text = item.getText(event.index);
				event.gc.drawText(text, event.x, event.y, true);	
				Image image = item.getImage(event.index);
				if (image != null) {
					event.gc.drawImage(image, event.x, event.y);
				}
			}
		});
		table.pack();
		table.setToolTipText("");
		
		tableViewer.setLabelProvider(labelProvider);
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.setInput(logLines);
		tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
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
		
		thread = new Thread(new Runnable() {
			
			@Override
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
							@Override
							public void run() {
								tableViewer.refresh();
								if (!scrollLock) {
									tableViewer.reveal(tableViewer.getElementAt(tableViewer.getTable().getItemCount()-1));
								}
							}
						});
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		thread.setDaemon(true);
		thread.setName("StudioLogViewer");
		thread.start();
		// Make the selection available to other views
		getSite().setSelectionProvider(tableViewer);
	}

	private void createColumns(final Composite parent, final TableViewer viewer) {
		String[] titles = {"Message", "Level", "Category", "Time", "Thread", "Extra", " + "};
		int[] bounds = {400, 50, 125, 150, 125, 100, 10};
		
		TableColumnLayout layout = new TableColumnLayout();
		parent.setLayout(layout);
		
		TableViewerColumn col = createTableViewerColumn(titles[0], bounds[0], 0);;
		layout.setColumnData(col.getColumn(), new ColumnWeightData(20, ColumnWeightData.MINIMUM_WIDTH, true));

		col = createTableViewerColumn(titles[1], bounds[1], 1);
		layout.setColumnData(col.getColumn(), new ColumnWeightData(3, ColumnWeightData.MINIMUM_WIDTH, true));
		
		col = createTableViewerColumn(titles[2], bounds[2], 2);
		layout.setColumnData(col.getColumn(), new ColumnWeightData(5, ColumnWeightData.MINIMUM_WIDTH, true));
		
		col = createTableViewerColumn(titles[3], bounds[3], 3);
		layout.setColumnData(col.getColumn(), new ColumnWeightData(4, ColumnWeightData.MINIMUM_WIDTH, true));
		
		col = createTableViewerColumn(titles[4], bounds[4], 4);
		layout.setColumnData(col.getColumn(), new ColumnWeightData(3, ColumnWeightData.MINIMUM_WIDTH, true));
		
		col = createTableViewerColumn(titles[5], bounds[5], 5);
		layout.setColumnData(col.getColumn(), new ColumnWeightData(5, ColumnWeightData.MINIMUM_WIDTH, true));
		
		col = createTableViewerColumn(titles[6], bounds[6], 6);
		col.getColumn().setToolTipText("Has more lines");
		layout.setColumnData(col.getColumn(), new ColumnWeightData(1, true));
	} 
	
	private TableViewerColumn createTableViewerColumn(String title, int bound, final int colNumber) {
		TableViewerColumn viewerColumn = new TableViewerColumn(tableViewer,SWT.NONE);
		TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setWidth(bound);
		column.setResizable(true);
		column.setMoveable(true);
		column.addSelectionListener(getSelectionAdapter(column, colNumber));
		return viewerColumn;
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

	@Override
	public void objectSelected(CompositeEvent compositeEvent) {
		// TODO Auto-generated method stub
	}

	@Override
	public void objectChanged(CompositeEvent compositeEvent) {
		// TODO Auto-generated method stub
		
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
			
			for (int i=0; i < logs.length(); i++) {
				JSONArray logLine = (JSONArray) logs.get(i);
				String extra = "";
				for (int j=5; j < logLine.length(); j++) {
					extra += logLine.getString(j) + "\n";
				}
				logLines.add(new LogLine(logLine.getString(0), logLine.getString(1), logLine.getString(2), 
						logLine.getString(3), logLine.getString(4), extra, counter));
				counter++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
}