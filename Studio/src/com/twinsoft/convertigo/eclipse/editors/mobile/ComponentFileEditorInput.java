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
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.eclipse.editors.mobile;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.part.FileEditorInput;

import com.twinsoft.convertigo.beans.core.MobileComponent;

public class ComponentFileEditorInput extends FileEditorInput {

	private MobileComponent component = null;
	
	public ComponentFileEditorInput(IFile file) {
		super(file);
	}
	
	public ComponentFileEditorInput(IFile file, MobileComponent component) {
		super(file);
		this.component = component;
	}

	public MobileComponent getComponent() {
		return component;
	}

	public void setComponent(MobileComponent component) {
		this.component = component;
	}

	public boolean is(MobileComponent component) {
		return component.equals(this.component) && component.getQName().equals(this.component.getQName());
	}
	
	public boolean isChildOf(MobileComponent component) {
		return this.component.getQName().startsWith(component.getQName());
	}
}
