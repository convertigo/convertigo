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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/branches/6.2.x/Studio/src/com/twinsoft/convertigo/eclipse/views/projectexplorer/ClipboardManager2.java $
 * $Author: nicolasa $
 * $Revision: 31165 $
 * $Date: 2012-07-20 17:45:54 +0200 (ven., 20 juil. 2012) $
 */

package com.twinsoft.convertigo.eclipse.views.loggers;

import java.lang.reflect.Method;

import org.apache.log4j.Level;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerColumn;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;

import com.twinsoft.convertigo.engine.util.GenericUtils;

public class EngineLogViewLabelProvider extends CellLabelProvider implements
		ITableLabelProvider, ITableFontProvider, ITableColorProvider {

	public Color getForeground(Object element, int columnIndex) {
		return null;
	}
	
	private TableViewer tableViewer;
	
	@Override
	protected void initialize(ColumnViewer viewer, ViewerColumn column) {
		super.initialize(viewer, column);
		tableViewer = (TableViewer) viewer;
	}
	
	public Color getBackground(Object element, int columnIndex) {
		LogLine line = (LogLine) element;
		String level = line.getLevel();
		if (level.equals(Level.ERROR.toString())) {
			if (line.getCounter() % 2 == 0) {
				return new Color(Display.getCurrent(), 255, 158, 147);
			} else {
				return new Color(Display.getCurrent(), 255, 186, 178);
			}
		} else if (level.equals(Level.INFO.toString())) {
			if (line.getCounter() % 2 == 0) {
				return new Color(Display.getCurrent(), 225, 242, 228);
			} else {
				return new Color(Display.getCurrent(), 237, 255, 241);
			}
		} else if (level.equals(Level.DEBUG.toString())) {
			if (line.getCounter() % 2 == 0) {
				return new Color(Display.getCurrent(), 249, 249, 177);
			} else {
				return new Color(Display.getCurrent(), 255, 255, 196);
			}
		} else if (level.equals(Level.WARN.toString())) {
			if (line.getCounter() % 2 == 0) {
				return new Color(Display.getCurrent(), 242, 196, 208);
			} else {
				return new Color(Display.getCurrent(), 255, 204, 217);
			}
		}
		return null;
	}

	public Font getFont(Object element, int columnIndex) {
		return JFaceResources.getFont(JFaceResources.TEXT_FONT);
	}

	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	public String getColumnText(Object element, int columnIndex) {
		LogLine line = (LogLine) element;
		
		Table table = tableViewer.getTable();		
		String columnName = table.getColumn(columnIndex).getText();
		
		Class<LogLine> logLineClass = GenericUtils.cast(line.getClass());
		
		Object result;
		try {
			Method getMethod = logLineClass.getMethod("get" + columnName);
			result = getMethod.invoke(line);

			if ("Message".equals(columnName)) {
				return (String) result;
			}
			else {
				if (line.isSubLine) {
					return "";
				}
				else {
					return (String) result;
				}
			}
		} catch (Exception e) {
			return "";
		}
	}

	@Override
	public void update(ViewerCell cell) {}
}
