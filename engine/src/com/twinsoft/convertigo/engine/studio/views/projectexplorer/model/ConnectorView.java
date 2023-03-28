/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

package com.twinsoft.convertigo.engine.studio.views.projectexplorer.model;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.engine.studio.WrapStudio;
import com.twinsoft.convertigo.engine.studio.editors.IEditorPartWrap;
import com.twinsoft.convertigo.engine.studio.responses.connectors.SequenceExecuteSelectedOpenConnectorEditorResponse;

public class ConnectorView extends DatabaseObjectView {

    public ConnectorView(Connector connector, WrapStudio studio) {
        super(connector, studio);
    }

    public void launchEditor() {
        // Retrieve the project name
//        String projectName = getObject().getProject().getName();
//        try {
            // Refresh project resource
            //ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);

            // Open editor
            openConnectorEditor();

//        }
//        catch (CoreException e) {
            //ConvertigoPlugin.logException(e, "Unable to open project named '" + projectName + "'!");
//        }
    }

    public void openConnectorEditor() {
        // To add listeners
        getConnectorEditor(getObject());

        synchronized (studio) {
            try {
                studio.createResponse(
                    new SequenceExecuteSelectedOpenConnectorEditorResponse(getObject())
                        .toXml(studio.getDocument(), getObject().getQName())
                );
            }
            catch (Exception e1) {
            }

            studio.notify();

            try {
                studio.wait();
            }
            catch (InterruptedException e) {
            }
        }
    }

    private IEditorPartWrap getConnectorEditor(Connector connector) {
        return getProjectView().getConnectorEditor(connector);
    }

    @Override
    public Connector getObject() {
        return (Connector) super.getObject();
    }

    public ProjectView getProjectView() {
        return (ProjectView) getParent();
    }
}
