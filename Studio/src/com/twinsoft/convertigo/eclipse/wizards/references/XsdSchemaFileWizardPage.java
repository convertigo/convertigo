/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.eclipse.wizards.references;

import com.twinsoft.convertigo.beans.references.XsdSchemaReference;

public class XsdSchemaFileWizardPage extends SchemaFileWizardPage {

	public XsdSchemaFileWizardPage(Object parentObject) {
		super(parentObject, "XsdSchemaFileWizardPage");
		setFilterExtension(new String[]{"*.xsd"});
		setFilterNames(new String[]{"XSD files"});
	}

	@Override
	protected void setDboFilePath(String filepath) {
		((XsdSchemaReference)getDbo()).setFilepath(filepath);
	}

	@Override
	protected void setDboUrlPath(String urlpath) {
		((XsdSchemaReference)getDbo()).setUrlpath(urlpath);
	}
	
	@Override
	public void dialogChanged() {
		// TODO Auto-generated method stub
		super.dialogChanged();
	}

	@Override
	public void comboChanged() {
		// TODO Auto-generated method stub
		super.comboChanged();
	}

	@Override
	public void editorChanged() {
		// TODO Auto-generated method stub
		super.editorChanged();
	}

	@Override
	public void setTextStatus(String message) {
		// TODO Auto-generated method stub
		super.setTextStatus(message);
	}
}
