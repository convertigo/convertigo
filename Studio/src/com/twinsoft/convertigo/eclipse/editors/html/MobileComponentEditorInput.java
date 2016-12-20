/*
 * Copyright (c) 2001-2016 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.editors.html;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.part.FileEditorInput;

import com.twinsoft.convertigo.beans.core.MobileComponent;

public class MobileComponentEditorInput extends FileEditorInput {

	private MobileComponent component = null;
	
	public MobileComponentEditorInput(IFile file) {
		super(file);
	}
	
	public MobileComponentEditorInput(IFile file, MobileComponent component) {
		super(file);
		this.component = component;
	}

	public MobileComponent getComponent() {
		return component;
	}

	public void setComponent(MobileComponent component) {
		this.component = component;
	}

}
