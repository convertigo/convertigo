package com.twinsoft.convertigo.engine.studio.popup.actions;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.ContextManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.requesters.InternalHttpServletRequest;
import com.twinsoft.convertigo.engine.requesters.InternalRequester;
import com.twinsoft.convertigo.engine.studio.wrappers.WrapDatabaseObject;
import com.twinsoft.convertigo.engine.studio.wrappers.WrapStudio;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class SequenceExecuteSelectedAction extends AbstractRunnableAction {

    private Sequence sequence;
    private HttpSession session;

    public SequenceExecuteSelectedAction(WrapStudio studio) {
        super(studio);
    }

    @Override
    protected void run2() {
        //...

        WrapDatabaseObject treeObject = (WrapDatabaseObject) studio.getFirstSelectedTreeObject();
        if (treeObject != null && treeObject.instanceOf(Sequence.class)) {
            sequence = (Sequence) treeObject.getObject();
            // openEditors...
            
//            if (sequenceEditor != null) {
//                getActivePage().activate(sequenceEditor);
                  getDocument(sequence.getName(), null, false);
//            }
        }

        //...
    }

    public void getDocument(String sequenceName, String testcaseName, boolean isStubRequested) {
        final Map<String, String[]> parameters = new HashMap<String, String[]>();

        if (sequenceName == null) {
            sequenceName = sequence.getName();
        }

        parameters.put(Parameter.Sequence.getName(), new String[]{sequenceName});
        parameters.put(Parameter.Context.getName(), new String[]{getStudioContext(false).contextID});

        if (testcaseName != null) {
            parameters.put(Parameter.Testcase.getName(), new String[]{testcaseName});
        }

        if (isStubRequested) {
            parameters.put(Parameter.Stub.getName(), new String[]{"true"});
        }

        runRequestable(sequence.getProject().getName(), parameters);
    }

    public void runRequestable(final String projectName, final Map<String, String[]> parameters) {
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
                        } else {
                            request = new InternalHttpServletRequest(session);
                        }
                        
                        new InternalRequester(GenericUtils.<Map<String, Object>>cast(parameters), request).processRequest();
                    } catch (Exception e) {
                        //logException(e, "Failed to run the requestable of project " + projectName);
                    }
                }
                
            }).start();
        } else {
            //logInfo("Cannot run the requestable of project " + projectName + ", the embedded tomcat is not correctly started.");
        }
    }

    private Context getStudioContext(boolean bForce) {
        String projectName = sequence.getParent().getName();
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
}
