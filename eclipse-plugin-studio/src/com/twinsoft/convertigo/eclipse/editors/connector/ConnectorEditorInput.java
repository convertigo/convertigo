/*
 * Copyright (c) 2001-2021 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.editors.connector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.part.FileInPlaceEditorInput;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;

public class ConnectorEditorInput extends FileInPlaceEditorInput {

	public Connector connector;
	private String qname;
	
	static IFile getTmpFile(Connector connector) {
		try {
			return ConvertigoPlugin.getDefault().getProjectPluginResource(connector.getProject().getName()).getFile("_private/editor/wait.txt");
		} catch (Exception e) {
		}
		return null;
	}
	
	static IFile getTmpFile(Connector connector, String extension) {
		try {
			return ConvertigoPlugin.getDefault().getProjectPluginResource(connector.getProject().getName()).getFile("_private/editor/" + connector.getQName() + extension);
		} catch (Exception e) {
		}
		return null;
	}
	
	public ConnectorEditorInput(Connector connector) {
		super(getTmpFile(connector));
		this.connector = connector;
		qname = connector.getQName();
	}
	
	public ConnectorEditorInput(Connector connector, String extension) {
		super(getTmpFile(connector, extension));
		this.connector = connector;
		qname = connector.getQName();
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
		return getFile().getName();
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return connector.getParent().getName() + "/" + connector.getName();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		return null;
	}
	
	public boolean is(Connector connector) {
		return connector.equals(this.connector) && connector.getQName().equals(qname);
	}
	
	public boolean is(Project project) {
		return qname.startsWith(project.getQName());
	}
	
	public Connector getConnector() {
		return connector;
	}
	
	public void fileDelete() {
		try {
			getFile().delete(true, null);
		} catch (CoreException e) {
		}
	}
	
	public boolean fileExists() {
		return getFile().exists();
	}
	
	public void fileWrite(String str) {
		SwtUtils.fillFile(getFile(), str);
	}
}
