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

import org.apache.log4j.MDC;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

/**
 * Diagnostic (timing.trace, off by default): the start and the duration of each request, requestable, HTTP call and
 * CouchDB transaction, logged on cems.Engine at WARN to break down the time of a request.
 */
public final class TimingTrace {
	private TimingTrace() {
	}

	public static boolean isEnabled() {
		try {
			return EnginePropertiesManager.getPropertyAsBoolean(PropertyName.TIMING_TRACE);
		} catch (Throwable t) {
			return false;
		}
	}

	/** the start of a timed section, 0 when the trace is off */
	public static long start() {
		return isEnabled() ? System.currentTimeMillis() : 0;
	}

	public static void end(long start, String kind, String name) {
		end(start, kind, name, 0);
	}

	/**
	 * Logs the section when it lasted at least minMs. The line holds only a kind, a name and durations, so it is
	 * written even while the logs are off (MDC nolog, set around the calls that answer a token): without that, the
	 * time of those calls is missing from the trace.
	 */
	public static void end(long start, String kind, String name, long minMs) {
		if (start == 0) {
			return;
		}
		long now = System.currentTimeMillis();
		if (now - start < minMs) {
			return;
		}
		Object nolog = MDC.get("nolog");
		if (nolog != null) {
			MDC.remove("nolog");
		}
		try {
			Engine.logEngine.warn("(Timing) kind=" + kind + " start=" + start + " ms=" + (now - start) + " thread="
					+ Thread.currentThread().getName().replace(' ', '_') + " name=" + name);
		} finally {
			if (nolog != null) {
				MDC.put("nolog", nolog);
			}
		}
	}
}
