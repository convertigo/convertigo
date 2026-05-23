/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

public class LogCleaner extends AppenderSkeleton {
	private record DatedLogFile(File file, long end) {
	}

	static private LogCleaner singleton;
	
	private long nextCheck = 0;
	private File logDir;
	private Matcher isC8oLogFile;
	
	private LogCleaner() {
		logDir = new File(Engine.LOG_PATH);
		Matcher filename = Pattern.compile("(.*)\\.(.*?)").matcher(Engine.LOG_ENGINE_NAME);
		if (filename.matches()) {
			isC8oLogFile = Pattern.compile(filename.group(1) + "\\.([\\w]*)\\.([\\w]*)\\." + filename.group(2)).matcher("");
		}
	}

	@Override
	public void close() {
	}

	@Override
	public boolean requiresLayout() {
		return false;
	}

	@Override
	protected void append(LoggingEvent evt) {
		if (evt.timeStamp > nextCheck) {
			nextCheck = evt.timeStamp + 10000;
			long count = 0;
			List<DatedLogFile> c8oLogFiles = new ArrayList<DatedLogFile>();
			File[] logFiles = logDir.listFiles();
			if (logFiles == null || isC8oLogFile == null) {
				return;
			}
			for (File file: logFiles) {
				if (file.getName().startsWith(Engine.LOG_ENGINE_NAME)) {
					count++;
				}
				if (isC8oLogFile.reset(file.getName()).matches()) {
					count++;
					try {
						c8oLogFiles.add(new DatedLogFile(file, Long.parseLong(isC8oLogFile.group(2), Character.MAX_RADIX)));
					} catch (NumberFormatException e) {
						c8oLogFiles.add(new DatedLogFile(file, file.lastModified()));
					}
				}
			}
			c8oLogFiles.sort(Comparator.comparingLong(DatedLogFile::end).thenComparing(entry -> entry.file().getName()));
			
			count -= EnginePropertiesManager.getPropertyAsLong(PropertyName.LOG4J_APPENDER_CEMSAPPENDER_MAXBACKUPINDEX);
			while (count-- > 0 && !c8oLogFiles.isEmpty()) {
				c8oLogFiles.remove(0).file().delete();
			}
		}
	}

	static public void start() {
		stop();
		Engine.logContext.addAppender(singleton = new LogCleaner());
	}
	
	static public void stop() {
		try {
			Engine.logContext.removeAppender(singleton);
		} catch (Exception e) { }
		singleton = null;
	}
}
