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

package com.twinsoft.convertigo.engine.proxy.translated;

import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.engine.EngineException;

public class ProxyTransaction extends TransactionWithVariables {

	private static final long serialVersionUID = -1205804839413234938L;

	public void runCore() throws EngineException {
		// Do nothing
		// TODO: intégrer le code du proxy servlet dans le runcore
	}
	
	public void setStatisticsOfRequestFromCache() {
		// Do nothing
	}
	
}
