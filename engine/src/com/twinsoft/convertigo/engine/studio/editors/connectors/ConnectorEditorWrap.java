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

package com.twinsoft.convertigo.engine.studio.editors.connectors;

import org.w3c.dom.Document;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.engine.studio.editors.EditorPartWrap;

public class ConnectorEditorWrap extends EditorPartWrap {
    private ConnectorEditorPartWrap connectorEditorPart;
    private boolean dirty;
    
    public ConnectorEditorWrap(Connector connector) {
        connectorEditorPart = new ConnectorEditorPartWrap(this, connector);
    }

    public void getDocument(String transactionName, boolean isStubRequested) {
        getDocument(transactionName, null, isStubRequested);
    }

    public void getDocument(String transactionName, String testcaseName, boolean isStubRequested) {
        connectorEditorPart.getDocument(transactionName, testcaseName, isStubRequested);
    }

    public Document getLastGeneratedDocument() {
        return connectorEditorPart.lastGeneratedDocument;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }
}
