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

public class FrontalUtils {

	public static boolean includeVariableIntoRequestString(String projectName, TransactionWithVariables transaction, String variableName) {
		
		if (variableName.equals("bQueue")) return false;
		if (variableName.equals("queueingDelay")) return false;
		
		// Handle Pobi applications
		if (!Pobi.includeVariableIntoRequestString(projectName, variableName))
			return false;
		
		// Handle Infogreffe application
		if (!Infogreffe.includeVariableIntoRequestString(projectName, transaction, variableName))
			return false;

		// Handle Altares (Intuiz) applications
		if (!Altares.includeVariableIntoRequestString(projectName, transaction, variableName))
			return false;

		return true;
	}
}
