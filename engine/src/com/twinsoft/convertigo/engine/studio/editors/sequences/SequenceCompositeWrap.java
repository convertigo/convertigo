/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

import java.nio.charset.StandardCharsets;

import org.w3c.dom.Document;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.SequenceEvent;
import com.twinsoft.convertigo.beans.core.SequenceListener;
import com.twinsoft.convertigo.engine.studio.events.AbstractEvent;
import com.twinsoft.convertigo.engine.studio.events.sequences.SequenceEditorCompositeClearContentEvent;
import com.twinsoft.convertigo.engine.studio.events.sequences.SequenceEditorCompositeDataChangedEvent;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class SequenceCompositeWrap extends AbstractSequenceCompositeWrap implements SequenceListener {

    public SequenceCompositeWrap(SequenceEditorPartWrap sequenceEditorPart, Sequence sequence) {
        super(sequenceEditorPart, sequence);
        sequence.addSequenceListener(this);
    }

    @Override
    public void dataChanged(SequenceEvent sequenceEvent) {
        if (!checkEventSource(sequenceEvent)) {
            return;
        }

        try {
            Object data = sequenceEvent.data;
            if (sequenceEvent.data instanceof Document) {
                setTextData(XMLUtils.prettyPrintDOM((Document) data));
            }
            else {
                setTextData((byte[]) data);
            }
        }
        catch (Exception e) {
        }
    }

    private void setTextData(String data) {
        if (data != null) {
            AbstractEvent event = new SequenceEditorCompositeDataChangedEvent(sequence, data);
            com.twinsoft.convertigo.engine.servlets.GetEvents.addEvent(event);
        }
    }

    private void setTextData(byte[] data) {
        if (data != null) {
            final byte[] buf = data;
            setTextData(new String(buf, StandardCharsets.UTF_8));
        }
    }

    @Override
    protected void clearContent() {
        AbstractEvent event = new SequenceEditorCompositeClearContentEvent(sequence);
        com.twinsoft.convertigo.engine.servlets.GetEvents.addEvent(event);
    }
}
