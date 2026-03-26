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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.eclipse.swt.ClearableText;

public final class DatabaseObjectPickerFilterSupport {

	private DatabaseObjectPickerFilterSupport() {
	}

	public static Text createFilterText(Composite parent) {
		ClearableText filterText = new ClearableText(parent, SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH);
		filterText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		filterText.setMessage("Filter");
		return filterText.getTextControl();
	}

	public static void bind(Text filterText, DatabaseObjectPickerFilter filter, Runnable refresher) {
		filterText.addModifyListener(event -> {
			filter.setFilterText(filterText.getText());
			refresher.run();
		});
	}
}
