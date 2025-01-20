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

import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.swt.widgets.Composite;

import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.NgxUIComponentTreeObject;

public class MobileConfigTsImportsEditor extends TableEditor {

	public MobileConfigTsImportsEditor(Composite parent) {
		super(parent);
		
        dialogTitle = "Class imports";
        columnNames = new String[] { "Class", "Package"};
        templateData = new Object[] { "Component", "@angular/core" };
	}
	
	@Override
	public void setValidator(ICellEditorValidator validator) {
		super.setValidator(validator);
		if(this.databaseObjectTreeObject instanceof NgxUIComponentTreeObject tree) {
			if(tree.getObject().compareToTplVersion("8.4.0.3") >= 0) {
				 columnNames = new String[] { "Class", "Package", "Use default import syntax" };
			     templateData = new Object[] { "Component", "@angular/core", "false" };
			}
		}
	}
}
