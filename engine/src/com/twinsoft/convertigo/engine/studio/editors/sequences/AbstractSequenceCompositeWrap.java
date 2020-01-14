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

package com.twinsoft.convertigo.engine.studio.editors.sequences;

import java.util.EventObject;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.SequenceEvent;
import com.twinsoft.convertigo.beans.core.Step;

public abstract class AbstractSequenceCompositeWrap {

    protected SequenceEditorPartWrap sequenceEditorPart;
    protected Sequence sequence;

    public AbstractSequenceCompositeWrap(SequenceEditorPartWrap sequenceEditorPart, Sequence sequence) {
        this.sequenceEditorPart = sequenceEditorPart;
        this.sequence = sequence;
    }

    protected boolean checkEventSource(EventObject event) {
        boolean isSourceFromSequence = false;
        Object source = event.getSource();
        if (event instanceof SequenceEvent) {
            if ((source instanceof Sequence) || (source instanceof Step)) {
                Sequence sequence = null;
                if (source instanceof Sequence) sequence = (Sequence)source;
                if (source instanceof Step) sequence = ((Step)source).getParentSequence();
                if ((sequence != null) && (sequence.equals(this.sequence) || sequence.getOriginal().equals(this.sequence)))
                    isSourceFromSequence = true;
            }
        }
        return isSourceFromSequence;
    }

    protected abstract void clearContent();
}
