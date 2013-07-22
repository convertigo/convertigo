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

package com.twinsoft.convertigo.engine;

import com.twinsoft.convertigo.engine.requesters.PoolRequester;

public class ContextLauncher implements Runnable {
	private Context context;
	
	public ContextLauncher(Context context) {
		this.context = context;
	}
	
	public void run() {
		try {
			Engine.logEngine.debug("Launching the auto-start transaction \"" + context.transactionName + "\" for the context " + context.contextID);

			context.remoteAddr = "127.0.0.1";
			context.remoteHost = "localhost";
			context.userAgent = "Convertigo ContextManager pools launcher";

			PoolRequester poolRequester = new PoolRequester();
			poolRequester.processRequest(context);
		}
		catch(Exception e) {
			Engine.logEngine.error("Unable to launch the context " + context.contextID, e);
		}
	}
}
