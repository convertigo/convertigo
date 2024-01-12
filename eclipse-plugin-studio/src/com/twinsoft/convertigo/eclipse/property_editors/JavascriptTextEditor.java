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

package com.twinsoft.convertigo.eclipse.property_editors;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PartInitException;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IJScriptContainer;
import com.twinsoft.convertigo.eclipse.editors.jscript.JScriptEditorInput;
import com.twinsoft.convertigo.engine.Engine;

public class JavascriptTextEditor extends AbstractDialogCellEditor implements IJScriptContainer {

	public JavascriptTextEditor(Composite parent) {
		super(parent);

		dialogTitle = "Javascript expression";
		dialogCompositeClass = TextEditorComposite.class;
	}

	@Override
	protected Object openDialogBox(Control cellEditorWindow) {
		try {
			JScriptEditorInput.openJScriptEditor(databaseObjectTreeObject, this);
		} catch (PartInitException e) {
			Engine.logStudio.error("failed to open editor", e);
		}
		return null;
	}

	@Override
	public String getExpression() {
		return getValue().toString();
	}

	@Override
	public void setExpression(String expression) {
		databaseObjectTreeObject.setPropertyValue(propertyDescriptor.getId(), expression);
	}

	@Override
	public String getName() {
		return databaseObjectTreeObject.getName() + ":" + propertyDescriptor.getDisplayName();
	}
	
	@Override
	public DatabaseObject getDatabaseObject() {
		return databaseObjectTreeObject.getObject();
	}
}
