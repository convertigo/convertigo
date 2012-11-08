package com.twinsoft.convertigo.eclipse.views.loggers;

import java.util.HashMap;

public class LogLine {
	private String category;
	private String time;
	private String thread;
	private String message;
	private String extra;
	private String level;
	boolean isSubLine;
	private int counter;
	private String fullMessage;
	//private String fullExtra;
	private HashMap<String, String> allExtras;
	
	public LogLine(String category, String time, String level, String thread, String message, String extra, boolean isSubLine, int counter, String fullMessage, HashMap<String, String> allExtras) {
		super();
		this.category = category;
		this.time = time;
		this.thread = thread;
		this.message = message;
		this.extra = extra;
		this.level = level;
		this.isSubLine = isSubLine;
		this.counter = counter;
		//this.fullExtra = fullExtra;
		this.fullMessage = fullMessage;
		this.allExtras = allExtras;
	}
	
	public String getCategory() {
		return category;
	}

	public String getTime() {
		return time;
	}

	public String getThread() {
		return thread;
	}

	public String getMessage() {
		return message;
	}

	public String getExtra() {
		return extra;
	}

	public String getLevel() {
		return level;
	}

	public void setCounter(int counter) {
		this.counter = counter;
	}

	public int getCounter() {
		return counter;
	}

	public boolean isSubLine() {
		return isSubLine;
	}

	public void setSubLine(boolean isSubLine) {
		this.isSubLine = isSubLine;
	}

	public String getFullMessage() {
		return fullMessage;
	}

	public void setFullMessage(String fullMessage) {
		this.fullMessage = fullMessage;
	}
	
	public String getClientIP(){
		return allExtras.get("clientip");
	}
	
	public String getConnector(){
		return allExtras.get("connector");
	}
	public String getContextID(){
		return allExtras.get("contextid");
	}
	
	public String getProject(){
		return allExtras.get("project");
	}
	
	public String getTransaction(){
		return allExtras.get("transaction");
	}
	
	public String getUID(){
		return allExtras.get("uid");
	}
	
	public String getUser(){
		return allExtras.get("user");
	}
	
	public String getSequence(){
		return allExtras.get("sequence");
	}
	
	public String getClientHostName(){
		return allExtras.get("clienthostname");
	}
	public String getFullExtra() {
		//return fullExtra;
		return allExtras.toString();
	}
	
	//public void setFullExtra(String fullExtra) {
		//this.fullExtra = fullExtra;
	//}
}
