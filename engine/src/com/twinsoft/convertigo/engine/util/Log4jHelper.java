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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.MDC;
import org.apache.logging.log4j.ThreadContext;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.LogParameters;

public class Log4jHelper {
	private static final String CONTEXTUAL_PARAMETERS = "ContextualParameters";
	
	public static enum mdcKeys {ClientIP, Connector, ContextID, Project, Transaction, UID, User, Sequence, ClientHostName, UUID};
		
	static public void mdcInit(Context context) {
		// A previous request may still have asynchronous work using its parameters.
		context.logParameters = new LogParameters();
		mdcSet(context.logParameters);
	}

	static public void mdcClear() {
		// getContext() returns a copy in the Log4j 1.x bridge.
		MDC.clear();
	}

	static public void mdcSet(LogParameters logParameters) {
		if (logParameters == null) {
			MDC.remove(CONTEXTUAL_PARAMETERS);
		} else {
			MDC.put(CONTEXTUAL_PARAMETERS, logParameters);
		}
	}

	static public void mdcPut(mdcKeys key, Object value) {
		LogParameters logParameters = (LogParameters) MDC.get(CONTEXTUAL_PARAMETERS);
		
		if (logParameters == null) {
			throw new IllegalStateException("ContextualParameters is null: call mdcInit() before!");
		}

		logParameters.put(key.toString().toLowerCase(), value);
		mdcSet(logParameters);
	}

	/** Captures both APIs, including Boolean flags used by the legacy log filters. */
	public static MdcSnapshot mdcSnapshot() {
		return new MdcSnapshot();
	}

	/** Restores the caller's context even if a nested request or its cleanup fails. */
	public static MdcScope mdcScope() {
		return new MdcScope();
	}

	/** Captures at submission time; pooled workers must be empty between tasks. */
	public static Runnable withMdc(Runnable runnable) {
		MdcSnapshot snapshot = mdcSnapshot();
		return () -> {
			try {
				snapshot.install();
				runnable.run();
			} finally {
				mdcClear();
			}
		};
	}

	public static final class MdcSnapshot {
		private final Map<String, Object> legacy = new HashMap<>();
		private final Map<String, String> current = ThreadContext.getContext();

		private MdcSnapshot() {
			MDC.getContext().forEach((key, value) -> legacy.put(key, copy(value)));
		}

		private static Object copy(Object value) {
			return value instanceof LogParameters parameters ? parameters.clone() : value;
		}

		/** Each installation owns its parameters, even when this snapshot is reused. */
		public void install() {
			mdcClear();
			ThreadContext.putAll(current);
			legacy.forEach((key, value) -> MDC.put(key, copy(value)));
		}
	}

	public static final class MdcScope implements AutoCloseable {
		private final MdcSnapshot previous = mdcSnapshot();

		private MdcScope() {
		}

		@Override
		public void close() {
			previous.install();
		}
	}
}
