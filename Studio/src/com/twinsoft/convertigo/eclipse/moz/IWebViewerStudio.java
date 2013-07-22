/*
 * Copyright (c) 2001-2011 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.moz;

import com.twinsoft.convertigo.eclipse.learnproxy.http.gui.HttpProxyEventListener;
import com.twinsoft.convertigo.engine.parsers.DocumentCompletedListener;
import com.twinsoft.convertigo.engine.parsers.IWebViewer;
import com.twinsoft.convertigo.engine.parsers.SelectionChangedListener;

public interface IWebViewerStudio extends IWebViewer {
	public void setLayoutData(Object layoutData);
	
	public void addSelectionChangedListener(SelectionChangedListener listener);
	
	public boolean setFocus();
	
	public String getSelectedXpath();
	
	public void addDocumentCompletedListener(DocumentCompletedListener listener);
	
	public void addHttpProxyEventListener(HttpProxyEventListener listener);
	
	public void removeHttpProxyEventListener(HttpProxyEventListener listener);
	
	public void setSelectedXpath(String selectedXpath, boolean notifyChange);
}
