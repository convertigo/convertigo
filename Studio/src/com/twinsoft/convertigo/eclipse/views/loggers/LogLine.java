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

import java.util.HashMap;

public class LogLine {
	private String category;
	private String date;
	private String time;
	private String deltaTime;
	private String thread;
	private String message;
	private String level;
	boolean isSubLine;
	private int counter;
	private String fullMessage;
	private HashMap<String, String> allExtras;

	public LogLine(String category, String date, String time, String deltaTime, String level, String thread,
			String message, boolean isSubLine, int counter, String fullMessage,
			HashMap<String, String> allExtras) {
		super();
		this.category = category;
		this.date = date;
		this.time = time;
		this.deltaTime = deltaTime;
		this.thread = thread;
		this.message = message;
		this.level = level;
		this.isSubLine = isSubLine;
		this.counter = counter;
		this.fullMessage = fullMessage;
		this.allExtras = allExtras;
	}

	public String getCategory() {
		return category;
	}

	public String getDate() {
		return date;
	}

	public String getTime() {
		return time;
	}

	public String getDeltaTime() {
		return deltaTime;
	}

	public String getThread() {
		return thread;
	}

	public String getMessage() {
		return message;
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

	public String getClientIP() {
		return allExtras.get("clientip");
	}

	public String getConnector() {
		return allExtras.get("connector");
	}

	public String getContextID() {
		return allExtras.get("contextid");
	}

	public String getProject() {
		return allExtras.get("project");
	}

	public String getTransaction() {
		return allExtras.get("transaction");
	}

	public String getUID() {
		return allExtras.get("uid");
	}

	public String getUser() {
		return allExtras.get("user");
	}

	public String getSequence() {
		return allExtras.get("sequence");
	}

	public String getClientHostName() {
		return allExtras.get("clienthostname");
	}

	public String getUUID() {
		return allExtras.get("uuid");
	}

	public String getFullExtra() {
		return allExtras.toString();
	}
}
