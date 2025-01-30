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

import java.util.Map;

import org.apache.log4j.MDC;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.LogParameters;

public class Log4jHelper {
	
	public static enum mdcKeys {ClientIP, Connector, ContextID, Project, Transaction, UID, User, Sequence, ClientHostName, UUID};
		
	static public void mdcInit(Context context) {
		if (context.logParameters != null) {
			context.logParameters.clear();
		}
		MDC.put("ContextualParameters", context.logParameters);
	}

	static public void mdcClear() {
		Map<?, ?> context = MDC.getContext();
		if (context != null) {
			context.clear();
		}
	}

	static public void mdcSet(LogParameters logParameters) {
		if (logParameters != null) MDC.put("ContextualParameters", logParameters);
	}

	static public void mdcPut(mdcKeys key, Object value) {
		LogParameters logParameters = (LogParameters) MDC.get("ContextualParameters");
		
		if (logParameters == null) {
			throw new IllegalStateException("ContextualParameters is null: call mdcInit() before!");
		}

		logParameters.put(key.toString().toLowerCase(), value);
		MDC.put("ContextualParameters", logParameters);
	}

}
