/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.views.picker;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

public class DatabaseObjectPickerFilter extends ViewerFilter {

	private String filterText = "";

	public void setFilterText(String filterText) {
		this.filterText = filterText == null ? "" : filterText.strip();
	}

	public boolean isActive() {
		return !filterText.isEmpty();
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (!isActive() || !(element instanceof DatabaseObjectPickerNode node)) {
			return true;
		}
		return matches(node);
	}

	private boolean matches(DatabaseObjectPickerNode node) {
		if (node.matches(filterText)) {
			return true;
		}
		for (DatabaseObjectPickerNode child : node.getChildren()) {
			if (matches(child)) {
				return true;
			}
		}
		return false;
	}
}
