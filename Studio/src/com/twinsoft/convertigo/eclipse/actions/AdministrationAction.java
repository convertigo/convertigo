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

package com.twinsoft.convertigo.eclipse.actions;

import java.security.InvalidParameterException;
import java.util.UUID;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;

public class AdministrationAction implements IWorkbenchWindowActionDelegate {
	
	private static String lastAuthToken;
	private static long lastAuthTokenExpiration;
	
	public static void checkAuthToken(String authToken) throws InvalidParameterException {
		boolean isAuth = authToken != null && authToken.equals(lastAuthToken) && System.currentTimeMillis() < lastAuthTokenExpiration;
		lastAuthToken = null;
		lastAuthTokenExpiration = 0;
		if (!isAuth) {
			throw new InvalidParameterException("authToken not valid");
		}
	}
	
	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
	}

	public void run(IAction action) {
		try {
			lastAuthToken = UUID.randomUUID().toString();
			lastAuthTokenExpiration = System.currentTimeMillis() + 5000;
			
			Program.launch(EnginePropertiesManager.PropertyName.APPLICATION_SERVER_CONVERTIGO_URL.getDefaultValue() + "/admin/login.html#authToken=" + lastAuthToken);
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Error while opening the Convertigo administration page");
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {

	}
	
}
