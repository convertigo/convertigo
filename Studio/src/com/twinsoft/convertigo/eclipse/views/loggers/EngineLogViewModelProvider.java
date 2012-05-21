//package com.twinsoft.convertigo.eclipse.views.loggers;
//
//import java.io.IOException;
//import java.util.Date;
//import java.util.LinkedList;
//import java.util.List;
//
//import org.apache.log4j.Appender;
//import org.apache.log4j.AppenderSkeleton;
//import org.apache.log4j.spi.LoggingEvent;
//import org.codehaus.jettison.json.JSONArray;
//import org.codehaus.jettison.json.JSONException;
//
//import com.twinsoft.convertigo.engine.admin.logmanager.LogManager;
//import com.twinsoft.convertigo.engine.admin.services.ServiceException;
//
//public enum EngineLogViewModelProvider {
//	INSTANCE;
//	
//	private LogManager logManager;
//	private List<LogLine> logLines = new LinkedList<LogLine>();
//	private Appender appender;
//
//	private EngineLogViewModelProvider() {
//		logManager = new LogManager();
//		logManager.setContinue(true);
//		logManager.setDateStart(new Date());
//		logManager.setMaxLines(50);
//		
//		setAppender(new AppenderSkeleton() {
//			
//        	@Override
//        	protected void append(LoggingEvent arg0) {
//        		synchronized (this) {
//					this.notifyAll();
//				}
//        	}
//
//			public void close() {
//			}
//
//			public boolean requiresLayout() {
//				return false;
//			}
//        });
//	}
//
//	public List<LogLine> getLogLines() {
//		return logLines;
//	}
//	
//	public void updateLogs() {
//		try {
//			JSONArray logs = logManager.getLines();
//			
//	        while (logs.length() == 0) {
//	        	try {
//					Thread.sleep(2000);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//	        	logs = logManager.getLines();
//	        }
//			
//			for (int i=0; i < logs.length(); i++) {
//				JSONArray logLine = (JSONArray) logs.get(i);
//				logLines.add(new LogLine(logLine.getString(0), logLine.getString(1), logLine.getString(2), 
//						logLine.getString(3), logLine.getString(4), String.valueOf(i) , i));
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (JSONException e) {
//			e.printStackTrace();
//		}
//	}
//	
//	public void setFilter(String filter) {
//		try {
//			logManager.setFilter(filter);
//		} catch (ServiceException e) {
//			e.printStackTrace();
//		}
//	}
//	
//	public LogManager getLogManager() {
//		return logManager;
//	}
//
//	public void setLogManager(LogManager logManager) {
//		this.logManager = logManager;
//	}
//	
//	public void setAppender(Appender appender) {
//		this.appender = appender;
//	}
//
//	public Appender getAppender() {
//		return appender;
//	}
//}
