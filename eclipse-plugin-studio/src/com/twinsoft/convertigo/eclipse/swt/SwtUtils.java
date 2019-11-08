/*
 * Copyright (c) 2001-2019 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.swt;

import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

public class SwtUtils {
	static public GridLayout newGridLayout(int numColumns, boolean makeColumnsEqualWidth, int horizontalSpacing, int verticalSpacing, int marginWidth, int marginHeight) {
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = numColumns;
		gridLayout.makeColumnsEqualWidth = makeColumnsEqualWidth;
		gridLayout.horizontalSpacing = horizontalSpacing;
		gridLayout.verticalSpacing = verticalSpacing;
		gridLayout.marginWidth = marginWidth;
		gridLayout.marginHeight = marginHeight;
		return gridLayout;
	}
	
	static public GridLayout newGridLayout(int numColumns, boolean makeColumnsEqualWidth, int horizontalSpacing, int verticalSpacing, int marginWidth, int marginHeight, int marginRight,
															int marginTop, int marginBottom, int marginLeft) {
		GridLayout gridLayout = newGridLayout(numColumns, makeColumnsEqualWidth, horizontalSpacing, verticalSpacing, marginWidth, marginHeight);
		gridLayout.marginBottom = marginBottom;
		gridLayout.marginLeft = marginLeft;
		gridLayout.marginRight = marginRight;
		gridLayout.marginTop = marginTop;
		return gridLayout;
	}

	private static boolean lastDark = false;
	public static boolean isDark() {
		try {
			return lastDark = PlatformUI.getWorkbench().getWorkbenchWindows()[0].getShell().getBackground().getRed() < 128;
		} catch (Exception e) {
			return lastDark;
		}
	}

	public static void refreshTheme() {
		try {
			IThemeEngine themeEngine = (IThemeEngine) Display.getDefault().getData("org.eclipse.e4.ui.css.swt.theme");
			themeEngine.setTheme(themeEngine.getActiveTheme(), true);
		} catch (Exception e) {
			//e.printStackTrace();
		}
	}
}
