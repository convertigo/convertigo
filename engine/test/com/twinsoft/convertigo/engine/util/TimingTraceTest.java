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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

/** timing.trace: its lines are written while the logs are off (nolog), and the steps only above their threshold. */
public class TimingTraceTest {
	private final List<String> lines = new ArrayList<>();
	private final List<Object> noLogAtWrite = new ArrayList<>();
	private Logger previous;
	private Logger logger;
	private AppenderSkeleton appender;

	@Before
	public void setUp() throws Exception {
		EnginePropertiesManager.initProperties();
		EnginePropertiesManager.setProperty(PropertyName.TIMING_TRACE, "true");
		previous = Engine.logEngine;
		logger = Logger.getLogger("test.TimingTrace");
		logger.setLevel(Level.WARN);
		appender = new AppenderSkeleton() {
			@Override
			protected void append(LoggingEvent event) {
				lines.add("" + event.getMessage());
				noLogAtWrite.add(MDC.get("nolog"));
			}

			@Override
			public void close() {
			}

			@Override
			public boolean requiresLayout() {
				return false;
			}
		};
		logger.addAppender(appender);
		Engine.logEngine = logger;
	}

	@After
	public void tearDown() throws Exception {
		logger.removeAppender(appender);
		Engine.logEngine = previous;
		MDC.remove("nolog");
		EnginePropertiesManager.setProperty(PropertyName.TIMING_TRACE, PropertyName.TIMING_TRACE.getDefaultValue());
	}

	@Test
	public void writtenWhileTheLogsAreOffThenOffAgain() {
		MDC.put("nolog", true);
		TimingTrace.end(TimingTrace.start(), "requestable", "lib_BaseRow.formscommon_CheckConfigNative");
		assertEquals(1, lines.size());
		assertTrue(lines.get(0), lines.get(0).startsWith("(Timing) kind=requestable "));
		assertNull(noLogAtWrite.get(0));
		assertEquals(Boolean.TRUE, MDC.get("nolog"));
	}

	@Test
	public void aShortSectionUnderItsThresholdIsNotWritten() {
		TimingTrace.end(TimingTrace.start(), "step", "formssource_GetTableData/Init", 5);
		assertTrue(lines.isEmpty());
		TimingTrace.end(TimingTrace.start() - 6, "step", "formssource_GetTableData/CheckConfig", 5);
		assertEquals(1, lines.size());
		assertNull(MDC.get("nolog"));
	}

	@Test
	public void nothingWhenTheTraceIsOff() {
		EnginePropertiesManager.setProperty(PropertyName.TIMING_TRACE, "false");
		TimingTrace.end(TimingTrace.start(), "step", "any", 0);
		assertTrue(lines.isEmpty());
	}
}
