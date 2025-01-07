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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent;

public class ApplicationComponentEditorInput implements IEditorInput {

	ApplicationComponent application;
	private String qname;
	private boolean autoBuild;
	
	public ApplicationComponentEditorInput(ApplicationComponent application, boolean autoBuild) {
		this.application = application;
		this.qname = application.getQName();
		this.autoBuild = autoBuild;
	}

	@SuppressWarnings("unchecked")
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
		return application.getName();
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
		return application.getProject().getName() + "/" + application.getParentName() + "/" + application.getName();
	}
	
	public boolean is(ApplicationComponent application) {
		return application.equals(this.application) && application.getQName().equals(qname);
	}

	public ApplicationComponent getApplication() {
		return application;
	}
	
	public boolean isAutoBuild() {
		return autoBuild;
	}
	
}
