/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;

class CommentColumnLabelProvider extends ColumnLabelProvider {
	@Override
	public String getText(Object element) {
		if (element instanceof DatabaseObjectTreeObject) {
			DatabaseObjectTreeObject dbot = (DatabaseObjectTreeObject) element;
			DatabaseObject dbo = dbot.getObject();
			String comment = dbo.getComment();
			if (!StringUtils.isBlank(comment)) {
				int i = comment.indexOf('\n');
				if (i != -1) {
					comment = comment.substring(0, i);
				}
				return "// " + comment;
			}
			if (dbot.isSelected()) {
				return "…";
			}
		}
		return "";
	}

	@Override
	public Color getForeground(Object element) {
		return Display.getCurrent().getSystemColor(SwtUtils.isDark() ? SWT.COLOR_GREEN : SWT.COLOR_DARK_GREEN);
	}
}
