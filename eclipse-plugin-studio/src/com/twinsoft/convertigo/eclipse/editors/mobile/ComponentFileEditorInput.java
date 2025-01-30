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

package com.twinsoft.convertigo.eclipse.editors.mobile;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.part.FileEditorInput;

import com.twinsoft.convertigo.beans.core.MobileObject;

public class ComponentFileEditorInput extends FileEditorInput {

	private MobileObject component = null;
	
	public ComponentFileEditorInput(IFile file, MobileObject component) {
		super(file);
		this.component = component;
	}

	public MobileObject getComponent() {
		return component;
	}

	public void setComponent(MobileObject component) {
		this.component = component;
	}

	public boolean is(MobileObject component) {
		return component.equals(this.component) && component.getQName().equals(this.component.getQName());
	}
	
	public boolean isChildOf(MobileObject component) {
		return this.component.getQName().startsWith(component.getQName());
	}
}
