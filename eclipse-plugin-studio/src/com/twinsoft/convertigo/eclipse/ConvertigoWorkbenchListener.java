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

package com.twinsoft.convertigo.eclipse;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;

/**
 * The Convertigo workbench listener.
 *
 */
class ConvertigoWorkbenchListener implements IWorkbenchListener {

	public ConvertigoWorkbenchListener() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchListener#postShutdown(org.eclipse.ui.IWorkbench)
	 */
	public void postShutdown(IWorkbench workbench) {
		ConvertigoPlugin.logDebug2("Workbench postShutdown");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchListener#preShutdown(org.eclipse.ui.IWorkbench, boolean)
	 */
	public boolean preShutdown(IWorkbench workbench, boolean forced) {
		ConvertigoPlugin.logDebug2("Workbench preShutdown");
		ConvertigoPlugin.getDefault().setShuttingDown(true);
		return true;
	}
	
}
