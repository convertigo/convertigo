package com.twinsoft.convertigo.eclipse.views.loggers;

public class LogLine {
	private String category;
	private String time;
	private String thread;
	private String message;
	private String extra;
	private String level;
	private int counter;
	
	public LogLine(String category, String time, String level, String thread, String message, String extra, int counter) {
		super();
		this.category = category;
		this.time = time;
		this.thread = thread;
		this.message = message;
		this.extra = extra;
		this.level = level;
		this.counter = counter;
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
}
