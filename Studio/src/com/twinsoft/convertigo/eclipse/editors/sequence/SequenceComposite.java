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

package com.twinsoft.convertigo.eclipse.editors.sequence;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.SequenceEvent;
import com.twinsoft.convertigo.beans.core.SequenceListener;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class SequenceComposite extends AbstractSequenceComposite implements SequenceListener {

	private Text httpData;
	
	public SequenceComposite(SequenceEditorPart sequenceEditorPart, Sequence sequence, Composite parent, int style) {
		super(sequenceEditorPart, sequence, parent, style);
		this.sequence.addSequenceListener(this);
	}
	
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.eclipse.editors.sequence.AbstractSequenceComposite#close()
	 */
	public void close() {
		sequence.removeSequenceListener(this);
		super.close();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	public void dispose() {
		super.dispose();
	}
	
	protected void initialize() {
		GridData gridData = new org.eclipse.swt.layout.GridData();
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		httpData = new Text(this, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		httpData.setLayoutData(gridData);
		httpData.setText("");
		this.setLayout(new GridLayout());
		setSize(new Point(300, 200));
	}
	
	protected void clearContent() {
		setTextData("");
	}

	public void dataChanged(SequenceEvent sequenceEvent) {
		if (!checkEventSource(sequenceEvent))
			return;
		try {
			Object data = sequenceEvent.data;
			if (sequenceEvent.data instanceof Document) {
				setTextData(XMLUtils.prettyPrintDOM((Document) data));
			}
			else {
				setTextData((byte[]) data);
			}
		}
		catch (Exception e) {}
	}
	
	private void setTextData(String data) {
		if (data != null) {
			httpData.getDisplay().asyncExec(new Runnable() {
				public void run() {
					try {
						if (data.length() < 10000) {
							httpData.setText(data);	
						} else {
							httpData.setText(data.substring(0, 10000));
						}
					}
					catch (Exception e) {;}
				};
			});
		}
	}

	private void setTextData(byte[] data) {
		if (data != null) {
			final byte[] buf = data;
			httpData.getDisplay().asyncExec(new Runnable() {
				public void run() {
					try {
						httpData.setText(new String(buf));
					}
					catch (Exception e) {;}
				};
			});
		}
	}
	
	public void initSequence(Sequence sequence) {
		// TODO Auto-generated method stub
		
	}
}
