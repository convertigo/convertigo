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

package com.twinsoft.convertigo.eclipse;

import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;

import com.twinsoft.convertigo.engine.print.PrintStatus;

public class SwtStatus implements PrintStatus{
	
	private ProgressBar progressBar;
	private Label label;
	
	public SwtStatus(ProgressBar progressBar,Label label){
		this.progressBar=progressBar;
		this.label=label;
	}
	
	public int getStatus() {		
		return progressBar.getSelection();
	}

	public void setStatus(int value) {		
		progressBar.setSelection(value%101);
	}

	public String getMessage() {		
		return label.getText();
	}

	public void setMessage(String message) {
		label.setText(message);
	}

	public void set(int value, String message) {
		setStatus(value);
		setMessage(message);		
	}

	
}
