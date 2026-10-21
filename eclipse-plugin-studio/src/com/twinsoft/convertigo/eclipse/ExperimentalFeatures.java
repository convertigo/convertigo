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

package com.twinsoft.convertigo.eclipse;

import java.util.Set;

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.PropertySheet;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.eclipse.views.palette.PaletteView;

/**
 * The Studio preference that shows the new experimental features (JSON native sequences, their native steps, the
 * JSON path sources). Unchecked, the default, the Studio offers no way to create or turn them on: the native steps
 * are not in the palette nor in the creation wizards, the JSON native properties of a sequence are hidden while they
 * are off, and the JSON pickers and their menus are hidden. What a project already holds is always shown and
 * editable: a native step, a native sequence, a JSON path source.
 */
public class ExperimentalFeatures {
	private static final Set<String> NEW_BEANS = Set.of(
			"com.twinsoft.convertigo.beans.steps.JsonFormatCellsStep",
			"com.twinsoft.convertigo.beans.steps.JsonDistinctStep",
			"com.twinsoft.convertigo.beans.steps.JsonGroupStep",
			"com.twinsoft.convertigo.beans.steps.JsonSelectColumnsStep",
			"com.twinsoft.convertigo.beans.steps.JsonStreamRowsStep",
			"com.twinsoft.convertigo.beans.steps.JsonReadStep");

	private static final Set<String> NEW_SEQUENCE_PROPERTIES = Set.of("jsonNativeOutput", "nativeExecution");

	/** changes at each change of the preference: the cached property descriptors are built again */
	private static volatile int generation = 0;

	private ExperimentalFeatures() {
	}

	/** the preference is checked */
	public static boolean isShown() {
		return "true".equals(ConvertigoPlugin.getProperty(ConvertigoPlugin.PREFERENCE_EXPERIMENTAL_FEATURES));
	}

	public static int getGeneration() {
		return generation;
	}

	/** a bean the palette and the creation wizards do not offer while the preference is unchecked */
	public static boolean isHiddenBean(String className) {
		return !isShown() && NEW_BEANS.contains(className);
	}

	/** a new property of a sequence, hidden while the preference is unchecked and the property is off */
	public static boolean isHiddenProperty(DatabaseObject databaseObject, String propertyName) {
		if (isShown() || !(databaseObject instanceof Sequence sequence) || !NEW_SEQUENCE_PROPERTIES.contains(propertyName)) {
			return false;
		}
		// a sequence that is already native keeps the properties that make it native
		return !("jsonNativeOutput".equals(propertyName) ? sequence.isJsonNativeOutput() : sequence.isNativeExecution());
	}

	/** after a change of the preference: the palette and the Properties view show it at once */
	public static void changed() {
		generation++;
		ConvertigoPlugin.asyncExec(() -> {
			try {
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				for (String id : new String[] { "com.twinsoft.convertigo.eclipse.views.palette.PaletteView", "org.eclipse.ui.views.PropertySheet" }) {
					IViewPart view = page.findView(id);
					if (view instanceof PaletteView palette) {
						palette.refresh();
					} else if (view instanceof PropertySheet sheet && sheet.getCurrentPage() instanceof org.eclipse.ui.views.properties.PropertySheetPage propertyPage) {
						propertyPage.refresh();
					}
				}
			} catch (Exception e) {
				ConvertigoPlugin.logDebug("Unable to refresh the views for the experimental features: " + e.getMessage());
			}
		});
	}
}
