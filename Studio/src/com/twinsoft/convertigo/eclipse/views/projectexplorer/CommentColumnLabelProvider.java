package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IToolTipProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;

public class CommentColumnLabelProvider extends ColumnLabelProvider implements IToolTipProvider {
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
		return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN);
	}
}
