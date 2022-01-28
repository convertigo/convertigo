/*
 * Copyright (c) 2001-2022 Convertigo SA.
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
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

public class FlowViewerEditor extends EditorPart {

	public static final String ID = "com.twinsoft.convertigo.eclipse.editors.FlowViewerEditor";
	
	C8oBrowser c8oBrowser;

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
		FlowViewerInput si = (FlowViewerInput) input;
		String partName = si.sequence.getName() + " [flowViewer]";
		setPartName(partName);
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
		FlowViewerInput si = (FlowViewerInput) getEditorInput();
		
		parent.setLayout(new GridLayout(1, true));;
		
		c8oBrowser = new C8oBrowser(parent, SWT.NONE);
		c8oBrowser.setLayoutData(new GridData(GridData.FILL_BOTH));
		c8oBrowser.setUseExternalBrowser(false);
		
//		c8oBrowser.getBrowser().set(InjectJsCallback.class, params -> {
//			String url = params.frame().browser().url();
//			System.out.println("url=" + url);
//				try {
//					Frame frame = params.frame();
//					frame.executeJavaScript(
//						"navigator.__defineGetter__('userAgent', function(){ return 'Android'});\n"
//					);
////					params.frame().document().get().
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
////			browser.setZoomLevel(zoomFactor.zoomLevel());
//			return Response.proceed();
//		});
		Engine.logStudio.debug("(FlowViwer) Browser debug url: " + c8oBrowser.getDebugUrl());
		
		String url = EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL);
		if (!"false".equals(ConvertigoPlugin.getProperty(ConvertigoPlugin.PREFERENCE_USE_SYSTEM_FLOWVIEWER))) {
			url += "/system";
		};
		url += "/projects/lib_FlowViewer/DisplayObjects/mobile/?qname=" + si.sequence.getFullQName();
		c8oBrowser.setUrl(url);
	}

	@Override
	public void setFocus() {
	}
	
	@Override
	public void dispose() {
		if (c8oBrowser != null) {
			c8oBrowser.dispose();
		}
		super.dispose();
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
