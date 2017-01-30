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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.mobile.components.PageComponent;

public class PageComponentEditorInput implements IEditorInput {

	PageComponent page;
	private String qname;
	
	public PageComponentEditorInput(PageComponent page) {
		this.page = page;
		this.qname = page.getQName();
	}

	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		return null;
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public String getName() {
		return page.getName();
	}

	public String getQName() {
		return qname;
	}
	
	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return page.getProject().getName() + "/" + page.getParent().getName() + "/" + page.getName();
	}
	
	public boolean is(PageComponent page) {
		return page.equals(this.page) && page.getQName().equals(qname);
	}
	
	public boolean is(Project project) {
		return qname.startsWith(project.getQName());
	}

}
