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

package com.twinsoft.convertigo.eclipse.dnd;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.popup.actions.ClipboardAction;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class TreeDragListener extends DragSourceAdapter {

	private StructuredViewer viewer;
	private Color background = null;

	public TreeDragListener(StructuredViewer viewer) {
		this.viewer = viewer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.dnd.DragSourceAdapter#dragFinished(org.eclipse.swt.dnd.DragSourceEvent)
	 */
	@Override
	public void dragFinished(DragSourceEvent event) {
		ProjectExplorerView explorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
		if (background != null && explorerView != null) {
			explorerView.viewer.getTree().setBackground(background);
			background = null;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.dnd.DragSourceAdapter#dragSetData(org.eclipse.swt.dnd.DragSourceEvent)
	 */
	@Override
	public void dragSetData(DragSourceEvent event) {
		ProjectExplorerView explorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
		if (explorerView != null) {
			try {
				if (SwtUtils.isDark()) {
					if (background == null) {
						Tree tree = explorerView.viewer.getTree();
						background = tree.getBackground();
						tree.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY));
					}
				} else {
					background = null;
				}
				String sXml = ClipboardAction.dnd.copy(explorerView);
				if (sXml != null) {
					event.data = sXml;
				}
			} catch (EngineException e) {
				Engine.logStudio.warn("Cannot drag: (EngineException) "  + e.getMessage());
			} catch (ParserConfigurationException ee) {
				Engine.logStudio.warn("Cannot drag: (ParserConfigurationException) "  + ee.getMessage());
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.dnd.DragSourceAdapter#dragStart(org.eclipse.swt.dnd.DragSourceEvent)
	 */
	@Override
	public void dragStart(DragSourceEvent event) {
		event.doit = !viewer.getSelection().isEmpty();
	}

	
}
