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
 * $URL: svn://devus.twinsoft.fr/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/eclipse/actions/AdministrationAction.java $
 * $Author: nicolasa $
 * $Revision: 37908 $
 * $Date: 2014-08-27 16:49:07 +0200 (mer., 27 août 2014) $
 */

package com.twinsoft.convertigo.eclipse.actions;

import java.net.URLEncoder;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;

public class OpenSwaggerConsoleAction implements IWorkbenchWindowActionDelegate {

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
	}

	public void run(IAction action) {
		try {
			Program.launch(
					EnginePropertiesManager.PropertyName.APPLICATION_SERVER_CONVERTIGO_URL.getDefaultValue()+"/swagger/ui/index.html?url=" + 
					URLEncoder.encode(EnginePropertiesManager.PropertyName.APPLICATION_SERVER_CONVERTIGO_URL.getDefaultValue()+"/api?YAML","UTF-8"));
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Error while opening the Swagger console");
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {

	}
	
}
