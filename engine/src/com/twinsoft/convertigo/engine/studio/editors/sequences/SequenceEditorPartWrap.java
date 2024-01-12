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

import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.w3c.dom.Document;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.ContextManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineEvent;
import com.twinsoft.convertigo.engine.EngineListener;
import com.twinsoft.convertigo.engine.RequestableEngineEvent;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.requesters.InternalHttpServletRequest;
import com.twinsoft.convertigo.engine.requesters.InternalRequester;
import com.twinsoft.convertigo.engine.studio.events.AbstractEvent;
import com.twinsoft.convertigo.engine.studio.events.sequences.SequenceEditorPartDocumentGeneratedEvent;
import com.twinsoft.convertigo.engine.studio.events.sequences.SequenceEditorPartSequenceFinishedEvent;
import com.twinsoft.convertigo.engine.studio.events.sequences.SequenceEditorPartSequenceStartedEvent;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class SequenceEditorPartWrap implements EngineListener {

    private Sequence sequence;
    protected SequenceEditorWrap editor = null;

    public Document lastGeneratedDocument;
    private HttpSession session;

    protected Context context;
    private String contextID = null;
    private String projectName = null;

    protected AbstractSequenceCompositeWrap compositeSequence = null;

    public SequenceEditorPartWrap(SequenceEditorWrap editor, Sequence sequence) {
        this.sequence = sequence;
        this.editor = editor;

        this.context = getStudioContext();
        this.contextID = context.contextID;
        this.projectName = context.projectName;

        createCompositeSequence();
        Engine.theApp.addEngineListener(this);
    }

    @Override
    public void blocksChanged(EngineEvent engineEvent) {        
    }

    @Override
    public void objectDetected(EngineEvent engineEvent) {        
    }

    @Override
    public void documentGenerated(EngineEvent engineEvent) {
        if (!checkEventSource(engineEvent)) {
            return;
        }

        lastGeneratedDocument = (Document) engineEvent.getSource();
        String sequenceOutput = XMLUtils.prettyPrintDOMWithEncoding(lastGeneratedDocument);
        AbstractEvent event = new SequenceEditorPartDocumentGeneratedEvent(sequence, sequenceOutput);
        com.twinsoft.convertigo.engine.servlets.GetEvents.addEvent(event);
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
    }

    @Override
    public void transactionFinished(EngineEvent engineEvent) {
        if (!checkEventSource(engineEvent)) {
            return;
        }
    }

    @Override
    public void sequenceStarted(EngineEvent engineEvent) {
        if (!checkEventSource(engineEvent)) {
            return;
        }

        clearEditor(engineEvent);
        AbstractEvent event = new SequenceEditorPartSequenceStartedEvent(sequence);
        com.twinsoft.convertigo.engine.servlets.GetEvents.addEvent(event);
    }

    @Override
    public void sequenceFinished(EngineEvent engineEvent) {
        if (!checkEventSource(engineEvent)) {
            return;
        }

        AbstractEvent event = new SequenceEditorPartSequenceFinishedEvent(sequence);
        com.twinsoft.convertigo.engine.servlets.GetEvents.addEvent(event);
    }

    @Override
    public void clearEditor(EngineEvent engineEvent) {
        if (!checkEventSource(engineEvent)) {
            return;
        }

        compositeSequence.clearContent();
    }

    private void createCompositeSequence() {
        compositeSequence = new SequenceCompositeWrap(this, sequence);
    }

    private boolean checkEventSource(EventObject event) {
        boolean isSourceFromSequence = false;
        if (event instanceof RequestableEngineEvent) {
            RequestableEngineEvent requestableEvent = (RequestableEngineEvent) event;
            String sequenceName = requestableEvent.getSequenceName();
            if (sequenceName != null) {
                if (sequenceName.equals(sequence.getName()) && requestableEvent.getProjectName().equals(sequence.getProject().getName())) {
                    isSourceFromSequence = true;
                }
            }
        }
        else if (event instanceof EngineEvent) {
            Object ob = ((EngineEvent)event).getSource();
            if (ob instanceof Step) {
                try {
                    String projectName = ((Step)ob).getProject().getName();
                    String sequenceName =  ((Step)ob).getSequence().getName();
                    if (sequenceName.equals(sequence.getName()) && projectName.equals(sequence.getProject().getName())) {
                        isSourceFromSequence = true;
                    }
                }
                catch (Exception e){}
            }
        }
        return isSourceFromSequence;
    }

    private Context getStudioContext() {
        return getStudioContext(false);
    }

    private Context getStudioContext(boolean bForce) {
        String projectName = sequence.getParentName();
        String sequenceName = sequence.getName();
        String contextType = ContextManager.CONTEXT_TYPE_SEQUENCE;
        String contextID = Engine.theApp.contextManager.computeStudioContextName(contextType, projectName, sequenceName);

        Context ctx = Engine.theApp.contextManager.get(contextID);
        if ((ctx == null) || bForce) {
            ctx = new Context(contextID);
            ctx.cleanXpathApi();
            ctx.contextID = contextID;
            ctx.name = contextID;
            ctx.projectName = projectName;
            ctx.sequenceName = sequenceName;
            ctx.requestedObject = sequence;
            ctx.requestedObject.context = ctx;//
            ctx.lastAccessTime = System.currentTimeMillis();

            Engine.theApp.contextManager.add(ctx);
        }
        return ctx;
    }

    public void getDocument(String sequenceName, String testcaseName, boolean isStubRequested) {
        final Map<String, String[]> parameters = new HashMap<String, String[]>();

        editor.setDirty(true);

        if (sequenceName == null) {
            sequenceName = sequence.getName();
        }

        parameters.put(Parameter.Sequence.getName(), new String[] {sequenceName});
        parameters.put(Parameter.Context.getName(), new String[] {contextID});

        if (testcaseName != null) {
            parameters.put(Parameter.Testcase.getName(), new String[]{testcaseName});
        }

        if (isStubRequested) {
            parameters.put(Parameter.Stub.getName(), new String[] {"true"});
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
