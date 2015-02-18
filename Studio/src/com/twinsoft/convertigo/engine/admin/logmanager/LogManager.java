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
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.engine.admin.logmanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SortedMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;


public class LogManager {
	public static final Date date_first = new Date(0);
	public static final Date date_last = new Date(Long.MAX_VALUE);
	public static final DateFormat date_format = new SimpleDateFormat(" yyyy-MM-dd HH:mm:ss,SSS ");
	public static final int date_format_offset = 31;
	private static final File log_directory = new File(Engine.LOG_PATH);
	private static final String log_name = "engine.log";
	
	private final Matcher delim = Pattern.compile("[! ]([^\\|]*[^|\\s])\\|?").matcher("");
	private final Matcher delim_extra = Pattern.compile("([^=]*)=(.*)").matcher("");
	private final Matcher js_and = Pattern.compile(" and ").matcher("");
	private final Matcher js_or = Pattern.compile(" or ").matcher("");
	

	private boolean bContinue = false;
	private Date date_start = date_first;
	private Date date_end = date_last;
	private String filter = "";
	private int max_lines = 100;
	private long timeout = 1000;

	private BufferedReader br = null;
	private TemporalInputStream is = null;
	private Script js_filter = null;
	private Scriptable js_utils = null;
	private boolean has_more_results = true;
	private boolean need_renew = true;

	private JsonArray candidate = null;
	private String line = null;
	
	public LogManager() {
		Context js_context = Context.enter();
		try {
			js_utils = js_context.initStandardObjects();
			js_context.compileString(
				"String.prototype.startsWith = function(str) {return this.indexOf(str)==0};"
					+ "String.prototype.endsWith = function(str) {return this.lastIndexOf(str)+str.length==this.length};"
					+ "String.prototype.contains = function(str) {return this.indexOf(str)!=-1};"
				, "utils", 0, null).exec(js_context, js_utils);
		} finally {
			Context.exit();
		}
	}
	
	public JsonArray getLines() throws IOException {
		long end_time = System.currentTimeMillis() + timeout;
		JsonArray json_array = new JsonArray();
		Context js_context = null;
		if (js_filter != null) {
			js_context = Context.enter();
		}
		try {
			if (need_renew) {
				renew();
			} else if (!bContinue) {
				reset();
			}
			
			if (line == null) {
				line = br.readLine();
			}
			while (line != null && json_array.size() < max_lines && (timeout == -1  || System.currentTimeMillis() < end_time)) {
				boolean find = false;
				if (line.startsWith("!")) {
					try {
						addCandidate(js_context, json_array);
						delim.reset(line);
						candidate = new JsonArray();
						candidate.add(new JsonPrimitive(nextToken())); // category
						candidate.add(new JsonPrimitive(nextToken())); // time
						candidate.add(new JsonPrimitive(nextToken())); // level
						candidate.add(new JsonPrimitive(nextToken())); // thread
						candidate.add(JsonNull.INSTANCE); // message
						String next  = nextToken();
						while (next.startsWith("$")) {
							candidate.add(new JsonPrimitive(next.substring(1)));
							next = nextToken();
						}
						candidate.set(4, new JsonPrimitive(endToken()));
						find = true;
					} catch (Exception e){
						// probably not a valid line
					};
				}
				if (!find && candidate != null) {
					String message = candidate.get(4).getAsString() + '\n' + line;
					candidate.set(4, new JsonPrimitive(message));
				}
				line = br.readLine();
			}
			has_more_results = (line != null);
			if (!has_more_results) {
				addCandidate(js_context, json_array);
			}
		} catch (Exception e) {
			throw new IOException("unlikely Exception : " + e.getMessage());
		} finally {
			if (js_context != null) {
				Context.exit();
			}
		}
		return json_array;
	}
	
	public boolean hasMoreResults() {
		return has_more_results || need_renew || !bContinue;
	}
	
	public void setContinue(boolean bContinue) {
		this.bContinue = bContinue;
	}
	
	public void setDateEnd(Date date_end) {
		if (!this.date_end.equals(date_end)) {
			this.date_end = date_end;
			need_renew = true;
		}
	}
	
	public void setDateStart(Date date_start) {
		if (!this.date_start.equals(date_start)) {
			this.date_start = date_start;
			need_renew = true;
		}
	}
	
	public Date getDateStart() {
		return date_start;
	}
	
	public Date getDateEnd() {
		return date_end;
	}
	
	public String getCurrentFilter() {
		return filter;
	}
	
	public void setFilter(String filter) throws ServiceException {
		if (filter == null) {
			filter = "";
		}
		if (!this.filter.equals(filter)) {
			this.filter = filter;
			if (filter.length() != 0) {
				Context js_context = Context.enter();
				try {
					js_and.reset(filter);
					js_or.reset(js_and.replaceAll(" && "));
					filter = js_or.replaceAll(" || ");
					js_filter = js_context.compileString(filter, "filter", 0, null);
				} catch (EvaluatorException e) {
					throw new ServiceException("Failed to compile JS filter : " + e.getMessage(), e);
				} finally {
					Context.exit();
				}
			} else {
				js_filter = null;
			}
			bContinue = false;
		}
	}
	
	public void setMaxLines(int max_lines) {
		this.max_lines = max_lines;
	}
	
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
	
	public void close() throws IOException {
		if (br != null) {
			br.close();
		}
	}
	
	public SortedMap<Date, File> getTimedFiles() throws IOException {
		if (is == null) {
			renew();
		}
		return is.getTimedFiles();
	}
	
	private void addCandidate(Context js_context, JsonArray result) {
		if (candidate != null) {
			if (js_filter != null) {
				try {
					Scriptable scope = js_context.initStandardObjects(null, true);
					scope.put("category", scope, candidate.get(0).getAsString());
					scope.put("time", scope, candidate.get(1).getAsString());
					scope.put("level", scope, candidate.get(2).getAsString());
					scope.put("thread", scope, candidate.get(3).getAsString());
					scope.put("message", scope, candidate.get(4).getAsString());
					StringBuffer extra = new StringBuffer();
					for (int i = 5; i < candidate.size(); i++) {
						String sub_extra = candidate.get(i).getAsString();
						extra.append(sub_extra).append(' ');
						delim_extra.reset(sub_extra);
						if (delim_extra.matches() && delim_extra.groupCount() == 2) {
							scope.put(delim_extra.group(1), scope, delim_extra.group(2));
						}
					}
					if (extra.length() > 0) {
						scope.put("extra", scope, extra.substring(0, extra.length() - 1));
					}
					scope.setParentScope(js_utils);
					if (Context.toBoolean(js_filter.exec(js_context, scope))) {
						result.add(candidate);
					}
				} catch (Exception e) {
					// TODO: handle exception
				}
			} else {
				result.add(candidate);
			}
			candidate = null;
		}
	}
	
	private String nextToken() {
		delim.find();
		return delim.group(1);
	}
	
	private String endToken() {
		return line.substring(delim.start(1));
	}
	
	private void renew() throws IOException {
		if (is != null) {
			is.close();
		}
		is = new TemporalInputStream(log_directory, log_name, date_format, date_format_offset, date_start, date_end);
		reset();
		need_renew = false;
	}
	
	private void reset() throws IOException {
		if (br != null) {
			br.close();
		}
		is.reset();
		br = new BufferedReader(new InputStreamReader(is, EnginePropertiesManager.getProperty(PropertyName.LOG4J_APPENDER_CEMSAPPENDER_ENCODING)));
		has_more_results = true;
		candidate = null;
		line = null;
	}
}
