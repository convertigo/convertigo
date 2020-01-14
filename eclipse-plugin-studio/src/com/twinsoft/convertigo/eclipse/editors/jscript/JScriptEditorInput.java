/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.editors.jscript;

import java.io.ByteArrayInputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.part.FileEditorInput;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IJScriptContainer;
import com.twinsoft.convertigo.beans.core.Project;

public class JScriptEditorInput extends FileEditorInput {
	
	static private IFile makeFile(IJScriptContainer jsContainer, IProject project) {
		String fullname = jsContainer.getFullName();
		IFile file = project.getFile("_private/editor." + fullname + ".js");
		try {
			if (!file.exists()) {
				file.create(new ByteArrayInputStream(new byte[0]), false, null);
			}
		} catch (Exception e) {
		}
		return file;
	}
	
	private IJScriptContainer jsContainer;
		
	public JScriptEditorInput(IJScriptContainer jsContainer, IProject project) {
		super(makeFile(jsContainer, project));
		this.jsContainer = jsContainer;
	}

	public IJScriptContainer getJScriptContainer() {
		return jsContainer;
	}
	
	public DatabaseObject getDatabaseObject() {
		return jsContainer.getDatabaseObject();
	}
	
	public Project getProject() {
		DatabaseObject dbo = jsContainer.getDatabaseObject();
		return dbo != null ? dbo.getProject() : null;
	}

	@Override
	public String getName() {
		return jsContainer.getName();
	}

	@Override
	public String getToolTipText() {
		return jsContainer.getFullName();
	}
	
	public boolean is(DatabaseObject dbo) {
		DatabaseObject d = this.jsContainer.getDatabaseObject();
		do {
			if (d == dbo) {
				return true;
			}
			d = d.getParent();
		} while (!(d instanceof Project));
		return false;
	}
}
