/*
 * Copyright (c) 2001-2024 Convertigo SA.
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;

import com.twinsoft.convertigo.eclipse.ColorEnum;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class EngineLogViewLabelProvider extends CellLabelProvider implements
		ITableLabelProvider, ITableFontProvider, ITableColorProvider {

	private Color error;
	private Color error_bis;
	private Color warn;
	private Color warn_bis;
	private Color info;
	private Color info_bis;
	private Color debug;
	private Color debug_bis;
	private Color trace;
	private Color trace_bis;
	
	boolean isDark = false;
	
	public Color getForeground(Object element, int columnIndex) {
		return isDark ? getColor(element) : Display.getCurrent().getSystemColor(SWT.COLOR_BLACK);
	}
	
	private TableViewer tableViewer;
	
	@Override
	protected void initialize(ColumnViewer viewer, ViewerColumn column) {
		super.initialize(viewer, column);
		tableViewer = (TableViewer) viewer;
		isDark = SwtUtils.isDark();
		Display d = viewer.getControl().getDisplay();
		if (isDark) {
			error = error_bis = new Color(d, 255, 0, 0);
			warn = warn_bis = new Color(d, 255, 155, 0);
			info = info_bis = new Color(d, 9, 255, 0);
			debug = debug_bis = new Color(d, 0, 255, 249);
			trace = trace_bis = new Color(d, 240, 255, 0);
		} else {
			error = new Color(d, 255, 158, 147);
			error_bis = new Color(d, 255, 186, 178);
			warn = new Color(d, 242, 196, 208);
			warn_bis = new Color(d, 255, 204, 217);
			info = new Color(d, 225, 242, 228);
			info_bis = new Color(d, 237, 255, 241);
			debug = new Color(d, 249, 249, 177);
			debug_bis = new Color(d, 255, 255, 196);
			trace = new Color(d, 252, 252, 223);
			trace_bis = new Color(d, 252, 252, 232);
		}
	}
	
	public Color getBackground(Object element, int columnIndex) {
		return isDark ? ColorEnum.BACKGROUND_DARK.get() : getColor(element);
	}
	
	public Color getColor(Object element) {
		LogLine line = (LogLine) element;
		String level = line.getLevel();
		boolean odd = line.getCounter() % 2 == 0;
		if (level.equals(Level.ERROR.toString()) || level.equals(Level.FATAL.toString())) {
			return odd ? error : error_bis;
		} else if (level.equals(Level.WARN.toString())) {
			return odd ? warn : warn_bis;
		} else if (level.equals(Level.INFO.toString())) {
			return odd ? info : info_bis;
		} else if (level.equals(Level.DEBUG.toString())) {
			return odd ? debug : debug_bis;
		} else if (level.equals(Level.TRACE.toString())) {
			return odd ? trace : trace_bis;
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

	@Override
	public void dispose(ColumnViewer viewer, ViewerColumn column) {
		error.dispose();
		error_bis.dispose();
		warn.dispose();
		warn_bis.dispose();
		info.dispose();
		info_bis.dispose();
		debug.dispose();
		debug_bis.dispose();
		trace.dispose();
		trace_bis.dispose();
		super.dispose(viewer, column);
	}
	
	
}
