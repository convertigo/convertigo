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

package com.twinsoft.convertigo.engine.admin.services.studio.treeview;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.ngx.components.MobileComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicMenuItems;
import com.twinsoft.convertigo.beans.ngx.components.UIText;
import com.twinsoft.convertigo.engine.admin.services.studio.ngxbuilder.BuilderUtils;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;

/**
 * The "Enable I18n recursively" and "Disable I18n recursively" actions of the Eclipse Studio on the NGX
 * components: the texts and the automatic menu items under a component are translated or not.
 */
class NgxI18n {

	private NgxI18n() {
	}

	static boolean handles(DatabaseObject dbo) {
		return dbo instanceof MobileComponent;
	}

	/**
	 * @return the number of texts and menu items changed
	 */
	static int i18n(DatabaseObject dbo, boolean enable) throws Exception {
		var changed = new int[] { 0 };
		new WalkHelper() {

			@Override
			protected void walk(DatabaseObject databaseObject) throws Exception {
				if (databaseObject instanceof UIText text && text.isI18n() != enable) {
					text.setI18n(enable);
					changed(databaseObject);
				} else if (databaseObject instanceof UIDynamicMenuItems items && items.isI18n() != enable) {
					items.setI18n(enable);
					changed(databaseObject);
				}
				super.walk(databaseObject);
			}

			private void changed(DatabaseObject databaseObject) {
				databaseObject.hasChanged = true;
				changed[0]++;
				// the application is generated again with the translated texts
				BuilderUtils.dboChanged(databaseObject, "i18n", !enable, enable);
			}
		}.init(dbo);
		return changed[0];
	}
}
