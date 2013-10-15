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

package com.twinsoft.convertigo.eclipse;

import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;

public class ConvertigoPerspectiveListener implements IPerspectiveListener {

	public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {

		ConvertigoPlugin.logDebug3("IWorkbenchPage perspectiveActivated : " + page.getLabel() + "("
				+ page.toString() + ")");
		if (!perspective.getId().equalsIgnoreCase(ConvertigoPlugin.PLUGIN_PERSPECTIVE_ID)) {
		} else {
		}
	}

	public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {
		ConvertigoPlugin.logDebug3("IWorkbenchPage perspectiveChanged : " + page.getLabel() + "("
				+ perspective.getLabel() + " - " + changeId + ")");

		IViewReference introView = page.findViewReference("org.eclipse.ui.internal.introview");
		// Maximize the intro view if present
		if (introView != null) {
			page.setPartState(introView, IWorkbenchPage.STATE_MAXIMIZED);
		}
	}

}
