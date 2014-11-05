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

package com.twinsoft.convertigo.eclipse.property_editors;

import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;

import com.twinsoft.convertigo.eclipse.dnd.StepSourceTransfer;

public class StepSourceXpathEvaluatorComposite extends StepXpathEvaluatorComposite {

	public StepSourceXpathEvaluatorComposite(Composite parent, int style, IStepSourceEditor stepSourceEditorComposite) {
		super(parent, style, stepSourceEditorComposite);
	}

	@Override
	protected void AddDndSupport() {
		// DND support
		int ops = DND.DROP_COPY | DND.DROP_MOVE;
		Transfer[] dropTransfers = new Transfer[] {TextTransfer.getInstance()};
		Transfer[] dragTransfers = new Transfer[] {StepSourceTransfer.getInstance()};
		
		DropTarget target = new DropTarget(getXpath(), ops);
		target.setTransfer(dropTransfers);
		target.addDropListener(new DropTargetAdapter() {
			@Override
			public void drop(DropTargetEvent droptargetevent) {
				getXpath().setText(droptargetevent.data.toString());
				performCalcXpath();
			}
		});
		
		DragSourceListener listener = new DragSourceAdapter() {
			@Override
			public void dragStart(DragSourceEvent event) {
				event.doit = !getXpath().getText().equals("");
				if (event.doit) {
					StepSourceTransfer.getInstance().setStepSource(getDragData());
				}
			}
		};
		
		DragSource source = new DragSource(getXpath(), ops);
		source.setTransfer(dragTransfers);
		source.addDragListener(listener);
		
		source = new DragSource(getLabel(), ops);
		source.setTransfer(dragTransfers);
		source.addDragListener(listener);
	}
	
}
