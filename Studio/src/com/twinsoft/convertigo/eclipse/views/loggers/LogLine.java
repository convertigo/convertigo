package com.twinsoft.convertigo.eclipse.views.loggers;

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
	private String fullExtra;
	
	public LogLine(String category, String time, String level, String thread, String message, String extra, boolean isSubLine, int counter, String fullMessage, String fullExtra) {
		super();
		this.category = category;
		this.time = time;
		this.thread = thread;
		this.message = message;
		this.extra = extra;
		this.level = level;
		this.isSubLine = isSubLine;
		this.counter = counter;
		this.fullExtra = fullExtra;
		this.fullMessage = fullMessage;
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

	public String getFullExtra() {
		return fullExtra;
	}

	public void setFullExtra(String fullExtra) {
		this.fullExtra = fullExtra;
	}
}
