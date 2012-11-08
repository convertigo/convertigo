package com.twinsoft.convertigo.eclipse.views.loggers;

import org.apache.log4j.Level;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class EngineLogViewLabelProvider extends CellLabelProvider implements
		ITableLabelProvider, ITableFontProvider, ITableColorProvider {

	FontRegistry registry = new FontRegistry();
	TableViewer viewer;

	public Color getForeground(Object element, int columnIndex) {
		return null;
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
		return registry.get(Display.getCurrent().getSystemFont().getFontData()[0].getName());
	}

	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	public String getColumnText(Object element, int columnIndex) {
		LogLine line = (LogLine) element;
		String text = "";
		switch (columnIndex) {
			case 0: 
				text = line.getMessage();
				break;
			case 1:
				if (!line.isSubLine) {
					text = line.getLevel();
				}
				break;
			case 2:
				if (!line.isSubLine) {
					text = line.getCategory();
				}
				break;
			case 3:
				if (!line.isSubLine) {
					text = line.getTime();
				}
				break;
			case 4:
				if (!line.isSubLine) {
					text = line.getThread();
				}
				break;
			case 5:
				//text = line.getExtra();
				if (!line.isSubLine) {
					text = line.getClientIP();
				}
				break;
			case 6:
				if (!line.isSubLine) {
					text = line.getConnector();
				}
				break;
			case 7:
				if (!line.isSubLine) {
					text = line.getContextID();
				}
				break;
			case 8:
				if (!line.isSubLine) {
					text = line.getProject();
				}
				break;
			case 9:
				if (!line.isSubLine) {
					text = line.getTransaction();
				}
				break;
			case 10:
				if (!line.isSubLine) {
					text = line.getUID();
				}
				break;
			case 11:
				if (!line.isSubLine) {
					text = line.getUser();
				}
				break;
			case 12:
				if (!line.isSubLine) {
					text = line.getSequence();
				}
				break;
			case 13:
				if (!line.isSubLine) {
					text = line.getClientHostName();
				}
				break;
			default:
				break;
		}
		return text;
	}

	@Override
	public void update(ViewerCell cell) {
	}
}
