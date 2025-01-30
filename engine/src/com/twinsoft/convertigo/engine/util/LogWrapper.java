/*
 * Copyright (c) 2001-2025 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.util;

import java.io.OutputStream;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import com.twinsoft.util.Log;

public class LogWrapper extends Log {
	private Logger logger;
	
	public LogWrapper(Logger logger) {
		this.logger = logger;
	}
	
	public void fatal(String message) {
		logger.fatal(message);
	}
	
	@Override
	public void error(String message) {
		logger.fatal(message);
	}
	
	public void error(String message, Throwable e) {
		logger.error(message, e);
	}
	
	@Override
	public void exception(Throwable e, String message) {
		logger.error(message, e);
	}
	
	@Override
	public synchronized void writeToLog(int messageLogLevel, String message) {
		Priority priority = Level.TRACE;
		if (messageLogLevel == Log.LOGLEVEL_FATAL_ERROR) priority = Level.FATAL;
		else if (messageLogLevel == Log.LOGLEVEL_EXCEPTION) priority = Level.ERROR;
		else if (messageLogLevel == Log.LOGLEVEL_WARNING) priority = Level.WARN;
		else if (messageLogLevel == Log.LOGLEVEL_MESSAGE) priority = Level.INFO;
		else if (messageLogLevel == Log.LOGLEVEL_DEBUG) priority = Level.DEBUG;
		else if (messageLogLevel == Log.LOGLEVEL_DEBUG2) priority = Level.TRACE;
		else if (messageLogLevel == Log.LOGLEVEL_DEBUG3) priority = Level.TRACE;
		logger.log(priority, message);
	}
	
	public void warn(String message) {
		logger.warn(message);
	}

	@Override
	public void warning(String message) {
		logger.warn(message);
	}
	
	public void info(String message) {
		logger.info(message);
	}
	
	@Override
	public void message(String message) {
		logger.info(message);
	}
	
	@Override
	public void debug(String message) {
		logger.debug(message);
	}
	
	public void trace(String message) {
		logger.trace(message);
	}
	
	@Override
	public void debug2(String message) {
		logger.trace(message);
	}
	
	@Override
	public void debug3(String message) {
		logger.trace(message);
	}
	
	static public void initWrapper(final Logger logger) {
		Log.factory = new Factory() {

			@Override
			protected Log newLog() {
				return new LogWrapper(logger);
			}

			@Override
			protected Log newLog(OutputStream outputStream) {
				return newLog();
			}
			
		};
	}
}