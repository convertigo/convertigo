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

package com.twinsoft.convertigo.engine.studio.editors.connectors;

import java.util.EventObject;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.ConnectorEvent;
import com.twinsoft.convertigo.beans.core.DatabaseObject;

public abstract class AbstractConnectorCompositeWrap {

    protected Connector connector;
    protected ConnectorEditorPartWrap connectorEditorPart;

    public AbstractConnectorCompositeWrap(ConnectorEditorPartWrap connectorEditorPart, Connector connector) {
        this.connectorEditorPart = connectorEditorPart;
        this.connector = connector;
    }

    protected boolean checkEventSource(EventObject event) {
        boolean isSourceFromConnector = false;
        Object source = event.getSource();
        if (event instanceof ConnectorEvent) {
            if (source instanceof DatabaseObject) {
                Connector connector = ((DatabaseObject)source).getConnector();
                if ((connector != null) && (connector.equals(this.connector)))
                    isSourceFromConnector = true;
            }
        }
        return isSourceFromConnector;
    }
}
