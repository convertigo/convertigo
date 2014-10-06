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
 * $URL: http://sourceus.twinsoft.fr/svn/CEMS/trunk/Studio/src/com/twinsoft/convertigo/eclipse/actions/AdministrationAction.java $
 * $Author: fabienb $
 * $Revision: 28379 $
 * $Date: 2011-09-27 11:38:59 +0200 (mar., 27 sept. 2011) $
 */

package com.twinsoft.convertigo.eclipse.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.discovery.DiscoveryConnectorsHelper;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;

public class OpenDiscoveryConnectorsAction implements IWorkbenchWindowActionDelegate {
	
	public void run(IAction action) {
		try {
			ConvertigoPlugin.logInfo("OpenDiscoveryConnectorsAction launch");
			Job job = new Job(SVNUIMessages.Operation_DiscoveryConnectors) {
				protected IStatus run(IProgressMonitor monitor) {
					ConvertigoPlugin.logInfo("OpenDiscoveryConnectorsAction job started");
					try {
						DiscoveryConnectorsHelper discovery = new DiscoveryConnectorsHelper();														
						discovery.run(monitor);
						
						ConvertigoPlugin.logInfo("OpenDiscoveryConnectorsAction finished");
					} catch (Throwable t) {
						//shouldn't prevent plug-in start 
						ConvertigoPlugin.logException(t, SVNUIMessages.Operation_DiscoveryConnectors_Error); 
					}
					return Status.OK_STATUS;
				}
			};
			job.setSystem(true);
			job.setUser(false);
			job.schedule();
		} catch (Exception e) {
			e.printStackTrace();
		}
//		org.eclipse.swt.program.Program.launch(Engine.USER_WORKSPACE_PATH);
	}

	public void selectionChanged(IAction action, ISelection selection) {

	}

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
	}
	
}
