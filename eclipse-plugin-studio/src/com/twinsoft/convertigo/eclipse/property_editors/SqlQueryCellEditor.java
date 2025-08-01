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

package com.twinsoft.convertigo.eclipse.property_editors;

import java.beans.PropertyDescriptor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.FileInPlaceEditorInput;

import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;

public class SqlQueryCellEditor extends TextMultiGenericCellEditor {

	static final String header = "### Declare and use parameters with {simple-brace} to prevent SQL injection, {{double-brace}} for plain replacement and separate queries with semicolons ;\n\n";

	public SqlQueryCellEditor(Composite parent, DatabaseObjectTreeObject databaseObjectTreeObject, PropertyDescriptor propertyDescriptor) {
		super(parent, databaseObjectTreeObject, propertyDescriptor);
	}

	@Override
	protected FileInPlaceEditorInput getInput() {
		super.getInput();
		IFile file = input.getFile();
		if ("txt".equals(file.getFileExtension())) {
			file = ((IFolder) file.getParent()).getFile(file.getName().replaceFirst("\\-sqlQuery.txt$", ".sql"));
			input = new FileInPlaceEditorInput(file);
		}
		return input;
	}

	@Override
	protected void setNewValue(Object newValue) {
		if (newValue instanceof String txt) {
			newValue = txt.replaceFirst("^###.*", "").trim();
		}
		super.setNewValue(newValue);
	}

	@Override
	protected String editorInitValue() {
		var txt = super.editorInitValue();
		return header + txt + "\n";
	}

	public void open() {
		openEditor();
	}
}
