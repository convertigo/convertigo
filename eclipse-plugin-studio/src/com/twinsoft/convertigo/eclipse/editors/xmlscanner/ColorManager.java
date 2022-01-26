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

package com.twinsoft.convertigo.eclipse.editors.xmlscanner;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import com.twinsoft.convertigo.eclipse.swt.SwtUtils;

public class ColorManager {

	protected boolean isDark;
	protected Map<RGB, Color> fColorTable = new HashMap<RGB, Color>(10);
	
	public ColorManager() {
		isDark = SwtUtils.isDark();
	}

	public void dispose() {
		Iterator<Color> e = fColorTable.values().iterator();
		while (e.hasNext())
			 e.next().dispose();
	}
	public Color getColor(RGB rgb) {
		Color color = fColorTable.get(rgb);
		if (color == null) {
			RGB keyRGB = rgb;
			if (isDark) {
				if (rgb == IXMLColorConstants.ATTRIBUTE) {
					rgb = IXMLColorDarkConstants.ATTRIBUTE;
				} else if (rgb == IXMLColorConstants.DEFAULT) {
					rgb = IXMLColorDarkConstants.DEFAULT;
				} else if (rgb == IXMLColorConstants.PROC_INSTR) {
					rgb = IXMLColorDarkConstants.PROC_INSTR;
				} else if (rgb == IXMLColorConstants.STRING) {
					rgb = IXMLColorDarkConstants.STRING;
				} else if (rgb == IXMLColorConstants.TAG) {
					rgb = IXMLColorDarkConstants.TAG;
				} else if (rgb == IXMLColorConstants.XML_COMMENT) {
					rgb = IXMLColorDarkConstants.XML_COMMENT;
				}
			}
			color = new Color(Display.getCurrent(), rgb);
			fColorTable.put(keyRGB, color);
		}
		return color;
	}
}
