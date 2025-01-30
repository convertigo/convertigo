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

package com.twinsoft.convertigo.eclipse.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.util.URLUtils;

public class OpenSwaggerConsoleAction implements IWorkbenchWindowActionDelegate {

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
	}

	public void run(IAction action) {
		try {
			Program.launch(
					EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL) + "/swagger/dist/index.html?" 
					+ URLUtils.encodePart("url", EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL) + "/openapi?YAML")
					+ (Engine.isStudioMode() ? "&showErrors" : ""));
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Error while opening the Swagger console");
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {

	}
	
}
