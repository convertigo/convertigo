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

import org.w3c.dom.Document;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.engine.studio.editors.EditorPartWrap;

public class SequenceEditorWrap extends EditorPartWrap {
    private SequenceEditorPartWrap sequenceEditorPart;
    private boolean dirty;

    public SequenceEditorWrap(Sequence sequence) {
        sequenceEditorPart = new SequenceEditorPartWrap(this, sequence);
    }

    public SequenceEditorPartWrap getSequenceEditorPart() {
        return sequenceEditorPart;
    }

    public void getDocument(String sequenceName, boolean isStubRequested) {
        getDocument(sequenceName, null, isStubRequested);
    }

    public void getDocument(String sequenceName, String testcaseName, boolean isStubRequested) {
        sequenceEditorPart.getDocument(sequenceName, testcaseName, isStubRequested);
    }

    public Document getLastGeneratedDocument() {
        return sequenceEditorPart.lastGeneratedDocument;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}
