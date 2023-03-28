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

package com.twinsoft.convertigo.engine.studio;

import java.util.List;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.WrapObject;
import com.twinsoft.convertigo.engine.studio.views.sourcepicker.SourcePickerViewWrap;

public interface WrapStudio {

	List<WrapObject> getSelectedObjects();
	WrapObject getFirstSelectedTreeObject();
	Object getFirstSelectedDatabaseObject();
	void addSelectedObject(DatabaseObject dbo);
	void addSelectedObjects(DatabaseObject ...dbos);

	int openMessageDialog(String title, Object object, String msg, String string, String[] buttons, int defaultIndex);
	int openMessageBox(String title, String msg, String[] buttons);

	void setResponse(int reponse);	
	boolean isActionDone();

	SourcePickerViewWrap getSourcePickerView();
}
