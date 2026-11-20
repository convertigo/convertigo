package com.twinsoft.convertigo.engine.debug;

import java.util.concurrent.atomic.AtomicReference;
import com.twinsoft.convertigo.beans.core.*;
import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.beans.transactions.HttpTransaction;
import com.twinsoft.convertigo.engine.*;
import com.twinsoft.convertigo.engine.requesters.JsonServletRequester;
import org.mozilla.javascript.Scriptable;

/** Real requestable deadline/cancellation owner, with no connector, I/O or second watchdog. */
public final class DebugRequestableFixture implements AutoCloseable {
    public interface Action { void run(RequestableObject.InvocationGuard guard) throws Exception; }
    private final Engine previous = Engine.theApp;
    public final Context context;
    public final RequestableObject root;
    public final AtomicReference<Throwable> failure = new AtomicReference<>();
    public volatile RequestableObject.RequestableThread worker;
    public DebugRequestableFixture(boolean transaction, int seconds, Action action) throws Exception {
        EnginePropertiesManager.initProperties();
        for (var field : Engine.class.getFields()) if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) && field.getType() == org.apache.log4j.Logger.class)
            field.set(null, org.apache.log4j.Logger.getLogger("debugger-requestable-test"));
        Engine.theApp = new Engine(); Engine.theApp.pluginsManager = new PluginsManager();
        context = new Context("debugger_budget_test");
        var project = new Project() { @Override public ClassLoader getProjectClassLoader() { return getClass().getClassLoader(); } };
        project.setName("DebuggerTest"); context.project = project; context.projectName = project.getName(); context.nativeJson = true;
        root = transaction ? new HttpTransaction() {
            @Override protected boolean requiresJavascriptScope() { return false; }
            @Override public void prepareForRequestable(Context c, org.mozilla.javascript.Context x, Scriptable s) { }
            @Override public void handleRequestableEvent(String e, org.mozilla.javascript.Context x) { }
            @Override public void fireRequestableEvent(String e) { }
            @Override public boolean hasToRunCore() { return true; }
            @Override protected void cleanup() { }
            @Override public void runCore() { perform(action); }
        } : new GenericSequence() {
            @Override protected boolean requiresJavascriptScope() { return false; }
            @Override public void prepareForRequestable(Context c, org.mozilla.javascript.Context x, Scriptable s) { }
            @Override public void handleRequestableEvent(String e, org.mozilla.javascript.Context x) { }
            @Override public void fireRequestableEvent(String e) { }
            @Override public boolean hasToRunCore() { return true; }
            @Override protected void cleanup() { }
            @Override public void runCore() { perform(action); }
        };
        root.setName("Owner"); root.setParent(project); root.setResponseTimeout(seconds); context.requestedObject = root;
    }
    private void perform(Action action) {
        worker = root.runningThread;
        try { action.run(RequestableObject.currentInvocationGuard()); } catch (Throwable e) { failure.set(e); }
    }
    public void run() throws EngineException { root.run(new JsonServletRequester(), context); }
    @Override public void close() throws Exception {
        if (worker != null) {
            // Tests publish cancellation explicitly. Cleanup only joins their private worker;
            // aborting a completed transaction races its cleared runningThread.
            worker.join(100);
            if (worker.isAlive()) { worker.interrupt(); worker.join(3000); }
        }
        Engine.theApp = previous; EnginePropertiesManager.initProperties();
    }
}
