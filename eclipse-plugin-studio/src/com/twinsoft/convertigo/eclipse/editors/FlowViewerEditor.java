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

package com.twinsoft.convertigo.eclipse.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

public class FlowViewerEditor extends EditorPart {

	public static final String ID = "com.twinsoft.convertigo.eclipse.editors.FlowViewerEditor";

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
//		FlowViewerInput si = (FlowViewerInput) getEditorInput();
		
		parent.setLayout(new GridLayout(1, true));;
		
		C8oBrowser browser = new C8oBrowser(parent, SWT.NONE);
		browser.setLayoutData(new GridData(GridData.FILL_BOTH));
		browser.setUseExternalBrowser(true);
		
		
		String url = EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL);
		url += "/projects/C8oStudio/DisplayObjects/mobile/path-to-layout";
		browser.setUrl(url);
	}

	@Override
	public void setFocus() {
	}

	public static IEditorInput makeInput(Sequence sequence) {
		return new FlowViewerInput(sequence);
	}
	
	public static class FlowViewerInput implements IEditorInput {
		Sequence sequence;
		
		FlowViewerInput(Sequence sequence) {
			this.sequence = sequence;
		}
		
		@Override
		public <T> T getAdapter(Class<T> adapter) {
			return null;
		}
		
		@Override
		public String getToolTipText() {
			return null;
		}
		
		@Override
		public IPersistableElement getPersistable() {
			return null;
		}
		
		@Override
		public String getName() {
			return sequence.getName();
		}
		
		@Override
		public ImageDescriptor getImageDescriptor() {
			return null;
		}

		@Override
		public boolean exists() {
			return false;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof FlowViewerInput)) {
				return false;
			}
			FlowViewerInput other = (FlowViewerInput) obj;
			return sequence.equals(other.sequence);
		}
		
		public boolean isForProject(String projectName) {
			return sequence.getProject().getName().equals(projectName);
		}
	}

}
