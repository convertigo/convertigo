/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public enum ColorEnum {
	WHITE (0xFF,0xFF,0xFF),
	BLACK (0,0,0),
	
	LIGHT_RED (0xFF, 0x80, 0x80),
	RED (0xFF,0,0),
	DARK_RED (0x80,0,0),
	
	LIGHT_GREEN (0x80, 0xFF, 0x80),
	GREEN (0,0xFF,0),
	DARK_GREEN (0,0x80,0),
	
	LIGHT_BLUE (0x80, 0x80, 0xFF),
	BLUE (0,0,0xFF),
	DARK_BLUE (0,0,0x80),
	
	// too strong, changed to a tooltips like yellow.  jmc 12/10/2017
	LIGHT_YELLOW (0xFF,0xFF,0xE0),
	YELLOW (0xFF,0xFF,0),
	DARK_YELLOW (0x80,0x80,0),

	LIGHT_MAGENTA (0xFF,0x80,0xFF),
	MAGENTA (0xFF,0,0xFF),
	DARK_MAGENTA (0x80,0,0x80),

	LIGHTCYAN (0x80,0xFF,0xFF),
	CYAN (0,0xFF,0xFF),
	DARK_CYAN (0,0x80,0x80),

	LIGHT_GRAY (0xE0,0xE0,0xE0),
	GRAY (0xC0,0xC0,0xC0),
	DARK_GRAY (0x80,0x80,0x80),
		
	JAVASCRIPTABLE (0xA2, 0xC2, 0xFF),
	
	BACKGROUND_DARK (0x2F, 0x2F, 0x2F);
	
	int r;
	int g;
	int b;
	
	static Map<ColorEnum, Color> cache = new HashMap<ColorEnum, Color>(ColorEnum.values().length);

	ColorEnum (int r, int g, int b) {
		this.r = r;
		this.g = g;
		this.b = b;
	}
	
	public Color get() {
		Color c = cache.get(this);
		if (c == null) {
			cache.put(this, c = new Color(Display.getCurrent(), r, g, b));
		}
		return c;
	}
}
