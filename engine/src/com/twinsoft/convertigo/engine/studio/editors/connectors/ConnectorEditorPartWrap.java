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

import java.lang.reflect.Constructor;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.w3c.dom.Document;

import com.twinsoft.convertigo.beans.connectors.CicsConnector;
import com.twinsoft.convertigo.beans.connectors.CouchDbConnector;
import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.connectors.SapJcoConnector;
import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector;
import com.twinsoft.convertigo.beans.connectors.SqlConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.ContextManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineEvent;
import com.twinsoft.convertigo.engine.EngineListener;
import com.twinsoft.convertigo.engine.RequestableEngineEvent;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.requesters.InternalHttpServletRequest;
import com.twinsoft.convertigo.engine.requesters.InternalRequester;
import com.twinsoft.convertigo.engine.servlets.GetEvents;
import com.twinsoft.convertigo.engine.studio.events.AbstractEvent;
import com.twinsoft.convertigo.engine.studio.events.connectors.ConnectorEditorPartDocumentGeneratedEvent;
import com.twinsoft.convertigo.engine.studio.events.connectors.ConnectorEditorPartTransactionFinishedEvent;
import com.twinsoft.convertigo.engine.studio.events.connectors.ConnectorEditorPartTransactionStartedEvent;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class ConnectorEditorPartWrap implements EngineListener {

    private Connector connector;
    protected ConnectorEditorWrap editor = null;

    public Document lastGeneratedDocument;
    private HttpSession session;

    protected Context context;
    private String contextID = null;
    private String projectName = null;

    protected AbstractConnectorCompositeWrap compositeConnector = null;
    private Class<?> compositeConnectorClass; // @jve:decl-index=0:

    public ConnectorEditorPartWrap(ConnectorEditorWrap editor, Connector connector) {
        this.connector = connector;
        this.editor = editor;

        this.context = getStudioContext();
        this.contextID = context.contextID;
        this.projectName = context.projectName;

        getConnectorCompositeClass();
        createCompositeConnector();

        Engine.theApp.addEngineListener(this);
    }

    private void getConnectorCompositeClass() {
        if (connector instanceof JavelinConnector) {
            compositeConnectorClass = JavelinConnectorCompositeWrap.class;
        } else if (connector instanceof HttpConnector) {
            compositeConnectorClass = HttpConnectorCompositeWrap.class;
        } else if (connector instanceof CicsConnector) {
            compositeConnectorClass = CicsConnectorCompositeWrap.class;
        } else if (connector instanceof SqlConnector) {
            compositeConnectorClass = SqlConnectorCompositeWrap.class;
        } else if (connector instanceof SapJcoConnector) {
            compositeConnectorClass = SapJcoConnectorCompositeWrap.class;
        } else if (connector instanceof SiteClipperConnector) {
            compositeConnectorClass = SiteClipperConnectorCompositeWrap.class;
        } else if (connector instanceof CouchDbConnector) {
            compositeConnectorClass = CouchDbConnectorCompositeWrap.class;
        } else {
            throw new IllegalArgumentException("The connector class is not handled: "
                    + connector.getClass().getName());
        }
    }

    private void createCompositeConnector() {
        try {
            Constructor<?> constructor = compositeConnectorClass.getConstructor(ConnectorEditorPartWrap.class, Connector.class);
            compositeConnector = (AbstractConnectorCompositeWrap) constructor.newInstance(this, connector);
        }
        catch (Exception e) {
        }
    }

    @Override
    public void blocksChanged(EngineEvent engineEvent) {
        if (!checkEventSource(engineEvent)) {
            return;
        }
    }

    @Override
    public void objectDetected(EngineEvent engineEvent) {
        if (!checkEventSource(engineEvent)) {
            return;
        }
    }

    @Override
    public void documentGenerated(EngineEvent engineEvent) {
        if (!checkEventSource(engineEvent)) {
            return;
        }

        lastGeneratedDocument = (org.w3c.dom.Document) engineEvent.getSource();
        final String strXML = XMLUtils.prettyPrintDOMWithEncoding(lastGeneratedDocument);
        AbstractEvent event = new ConnectorEditorPartDocumentGeneratedEvent(connector, strXML);
        GetEvents.addEvent(event);
        editor.setDirty(false);
    }

    @Override
    public void stepReached(EngineEvent engineEvent) {
        if (!checkEventSource(engineEvent)) {
            return;
        }
    }

    @Override
    public void transactionStarted(EngineEvent engineEvent) {
        if (!checkEventSource(engineEvent)) {
            return;
        }

        AbstractEvent event = new ConnectorEditorPartTransactionStartedEvent(connector);
        com.twinsoft.convertigo.engine.servlets.GetEvents.addEvent(event);
    }

    @Override
    public void transactionFinished(EngineEvent engineEvent) {
        if (!checkEventSource(engineEvent)) {
            return;
        }

        AbstractEvent event = new ConnectorEditorPartTransactionFinishedEvent(connector);
        com.twinsoft.convertigo.engine.servlets.GetEvents.addEvent(event);
    }

    @Override
    public void sequenceStarted(EngineEvent engineEvent) {
        if (!checkEventSource(engineEvent)) {
            return;
        }
    }

    @Override
    public void sequenceFinished(EngineEvent engineEvent) {
        if (!checkEventSource(engineEvent)) {
            return;
        }
    }

    @Override
    public void clearEditor(EngineEvent engineEvent) {
        if (!checkEventSource(engineEvent)) {
            return;
        }
    }

    private boolean checkEventSource(EventObject event) {
        boolean isSourceFromConnector = false;
        if (event instanceof RequestableEngineEvent) {
            RequestableEngineEvent requestableEvent = (RequestableEngineEvent) event;

            String connectorName = requestableEvent.getConnectorName();
            if (connectorName != null) {
                if (connectorName.equals(connector.getName()) && requestableEvent.getProjectName().equals(connector.getProject().getName())) {
                    isSourceFromConnector = true;
                }
            }
        }
        else if (event instanceof EngineEvent) {
            Object ob = ((EngineEvent)event).getSource();
            if (ob instanceof DatabaseObject) {
                try {
                    String projectName = ((DatabaseObject)ob).getProject().getName();
                    String connectorName = ((DatabaseObject)ob).getConnector().getName();
                    if (connectorName.equals(connector.getName()) && projectName.equals(connector.getProject().getName())) {
                        isSourceFromConnector = true;
                    }
                }
                catch (Exception e){}
            }
        }
        return isSourceFromConnector;
    }

    private Context getStudioContext() {
        return getStudioContext(false);
    }

    private Context getStudioContext(boolean bForce) {
        String projectName = connector.getParent().getName();
        String connectorName = connector.getName();
        String contextType = ContextManager.CONTEXT_TYPE_TRANSACTION;
        String contextID = Engine.theApp.contextManager.computeStudioContextName(contextType, projectName, connectorName);

        Context ctx = Engine.theApp.contextManager.get(contextID);
        if ((ctx == null) || bForce) {
            ctx = new Context(contextID);
            if (connector instanceof HtmlConnector) {
                ctx.cleanXpathApi();
                ctx.htmlParser = ((HtmlConnector) connector).getHtmlParser();
            }
            ctx.contextID = contextID;
            ctx.name = contextID;
            ctx.projectName = projectName;
            ctx.setConnector(connector);
            ctx.lastAccessTime = System.currentTimeMillis();

            Engine.theApp.contextManager.add(ctx);
        }
        return ctx;
    }

    protected void getDocument() {
        getDocument(null, null, false);
    }

    public void getDocument(String transactionName, String testcaseName, boolean isStubRequested) {
        final Map<String, String[]> parameters = new HashMap<String, String[]>();

        editor.setDirty(true);

        parameters.put(Parameter.Connector.getName(), new String[]{connector.getName()});

        if (transactionName != null) {
            parameters.put(Parameter.Transaction.getName(), new String[]{transactionName});
        }

        parameters.put(Parameter.Context.getName(), new String[]{contextID});

        if (testcaseName != null) {
            parameters.put(Parameter.Testcase.getName(), new String[]{testcaseName});
        }

        if (isStubRequested) {
            parameters.put(Parameter.Stub.getName(), new String[]{"true"});
        }

        runRequestable(projectName, parameters);
    }

    private void runRequestable(final String projectName, final Map<String, String[]> parameters) {
        if (!Engine.isStartFailed && Engine.isStarted) {
            parameters.put(Parameter.Project.getName(), new String[] {projectName});
            new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        InternalHttpServletRequest request;
                        if (session == null) {
                            request = new InternalHttpServletRequest();
                            session = request.getSession();
                        }
                        else {
                            request = new InternalHttpServletRequest(session);
                        }

                        new InternalRequester(GenericUtils.<Map<String, Object>>cast(parameters), request).processRequest();
                    }
                    catch (Exception e) {
                        //logException(e, "Failed to run the requestable of project " + projectName);
                    }
                }

            }).start();
        }
        else {
            //logInfo("Cannot run the requestable of project " + projectName + ", the embedded tomcat is not correctly started.");
        }
    }
}
