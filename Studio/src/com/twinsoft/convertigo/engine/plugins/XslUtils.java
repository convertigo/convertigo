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

package com.twinsoft.convertigo.engine.plugins;

import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;

public class XslUtils {
	
	public static String getContextValue(String contextId, String key) {
		try {
			Context context = Engine.theApp.contextManager.get(contextId);
			return (String) context.get(key);
		}
		catch(Exception e) {
			Engine.logBillers.error("Error while trying to retrieve the context variable '" + key + "'", e);
			return "";
		}
	}

	public static String getContextVariableValue(String contextId, String key) {
		try {
			Context context = Engine.theApp.contextManager.get(contextId);
			Object value = ((TransactionWithVariables) context.requestedObject).getVariableValue(key);
			return ((value==null) ? "":value.toString()); // value might be null since 5.0.3 beans version
		}
		catch(Exception e) {
			Engine.logBillers.error("Error while trying to retrieve the requestable variable '" + key + "'", e);
			return "";
		}
	}

}
