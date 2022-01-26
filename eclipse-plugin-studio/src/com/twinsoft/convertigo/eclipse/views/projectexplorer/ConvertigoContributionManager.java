/*
 * Copyright (c) 2001-2022 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import java.util.regex.Pattern;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IContributionManagerOverrides;

class ConvertigoContributionManager implements IContributionManagerOverrides {

	@Override
	public Boolean getEnabled(IContributionItem item) {
		return item.isEnabled();
	}

	@Override
	public Integer getAccelerator(IContributionItem item) {
		if (item instanceof ActionContributionItem) {
			System.out.println("getAccelerator " + item + " = " + ((ActionContributionItem) item).getAction().getAccelerator());
			return ((ActionContributionItem) item).getAction().getAccelerator();
		}
		System.out.println("getAccelerator " + item);
		return 0;
	}

	@Override
	public String getAcceleratorText(IContributionItem item) {
		if (item instanceof ActionContributionItem) {
			System.out.println("getAcceleratorText " + item + " = " + ((ActionContributionItem) item).getAction().getText());
			return ((ActionContributionItem) item).getAction().getText();
		}
		System.out.println("getAcceleratorText " + item);
		return null;
	}

	@Override
	public String getText(IContributionItem item) {
		if (item instanceof ActionContributionItem) {
			return ((ActionContributionItem) item).getAction().getText();
		}
		return item.getId();
	}

	@Override
	public Boolean getVisible(IContributionItem item) {
		if (!item.isVisible()) {
			return false;
		}
		String id = item.getId();
		
		if (id == null) {
			return true;
		}
		
		if (id.contains("convertigo")) {
			return true;
		}
		
		String cn = item.getClass().getSimpleName();
		if (Pattern.matches("DynamicMenuContributionItem|HandledContributionItem", cn)) {
			return id.startsWith("org.eclipse.egit.") || id.startsWith("org.eclipse.buildship.ui");
		}
		if (cn.equals("MenuManager")) {
			return Pattern.matches("\\d+|egit\\..*|team\\..*|org\\.eclipse\\.buildship\\..*|compareWithMenu|replaceWithMenu", id);
		}
		if (cn.equals("PluginActionContributionItem")) {
			return id.contains("org.eclipse.team");
		}
		return true;
	}

}
