/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL, version 3 or later. */
package com.twinsoft.convertigo.eclipse.views.rhino;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.part.ViewPart;
import com.twinsoft.convertigo.eclipse.editors.jscript.JScriptEditorInput;
import com.twinsoft.convertigo.engine.debug.JavascriptDebugger;
import com.twinsoft.convertigo.engine.debug.JavascriptDebugSession;
import com.twinsoft.convertigo.engine.requesters.InternalHttpServletRequest;
import com.twinsoft.convertigo.engine.util.CopilotHelper;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowserPostMessageHelper;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.eclipse.views.admin.AdminView;
import com.twinsoft.convertigo.eclipse.views.admin.AuthenticatedBrowser;

/** Embedded administrator web debugger. The existing Swing debugger remains available separately. */
public class JavascriptDebugView extends ViewPart {
    private static JavascriptDebugView instance;
    private C8oBrowser browser;
    private JavascriptDebugSession observed;
    private final Map<String, Set<Integer>> installed = new HashMap<>();
    private final Map<String, IFile> files = new HashMap<>();
    private IAnnotationModel annotationModel;
    private Annotation annotation;
    private long displayedPause = -1;
    private boolean synchronizationWarning;

    /** Called before a launch enters the engine: markers are installed before the first script runs. */
    public static void prepareRequest(InternalHttpServletRequest request) {
        if (JavascriptDebugger.studioSession() == null) return;
        var display = org.eclipse.swt.widgets.Display.getDefault();
        if (display.isDisposed()) return;
        display.syncExec(() -> {
            if (instance != null && instance.browser != null && !instance.browser.isDisposed())
                instance.synchronize();
        });
        JavascriptDebugger.selectStudioRequest(request);
    }

    private String sourceUrl(IFile file) throws Exception {
        var stored = file.getPersistentProperty(JScriptEditorInput.DEBUG_SOURCE);
        if (stored != null) return stored;
        var path = file.getProjectRelativePath().toString();
        return path.endsWith(".js") && !path.startsWith("_private/")
                ? file.getProject().getName() + "/" + path : null;
    }
    private int offset(IFile file) throws Exception {
        return file.getPersistentProperty(JScriptEditorInput.DEBUG_SOURCE) != null ? 0
                : CopilotHelper.addInstruction(null, "").split("\n", -1).length - 1;
    }
    private void clearAnnotation() {
        if (annotationModel != null && annotation != null) annotationModel.removeAnnotation(annotation);
        annotation = null;
        annotationModel = null;
    }
    private void synchronize() {
        var session = JavascriptDebugger.studioSession();
        if (session != observed) {
            observed = session;
            installed.clear();
            displayedPause = -1;
            clearAnnotation();
        }
        if (session == null) return;
        try {
            var desired = new HashMap<String, Set<Integer>>();
            files.clear();
            // Include closed editors: the exact property URL is stored with the generated editor file.
            var manager = DebugPlugin.getDefault().getBreakpointManager();
            if (manager.isEnabled()) for (var breakpoint : manager.getBreakpoints()) {
                if (!breakpoint.isEnabled() || !(breakpoint.getMarker().getResource() instanceof IFile file)) continue;
                var url = sourceUrl(file);
                int line = breakpoint.getMarker().getAttribute(IMarker.LINE_NUMBER, -1);
                if (url == null || line < 1) continue;
                files.put(url, file);
                desired.computeIfAbsent(url, key -> new HashSet<>()).add(line + offset(file));
            }
            for (var ref : getSite().getPage().getEditorReferences()) {
                if (ref.getEditorInput() instanceof IFileEditorInput input) {
                    var url = sourceUrl(input.getFile());
                    if (url != null) files.put(url, input.getFile());
                }
            }
            for (var entry : installed.entrySet()) for (int line : entry.getValue())
                if (!desired.getOrDefault(entry.getKey(), Set.of()).contains(line))
                    session.breakpoint(entry.getKey(), line, false);
            for (var entry : desired.entrySet()) for (int line : entry.getValue())
                if (!installed.getOrDefault(entry.getKey(), Set.of()).contains(line))
                    session.breakpoint(entry.getKey(), line, true);
            installed.clear();
            installed.putAll(desired);
            var state = session.state();
            if (!state.getBoolean("stopped")) {
                clearAnnotation();
                displayedPause = -1;
            } else if (state.getLong("pauseId") != displayedPause) {
                var frame = state.getJSONArray("frames").optJSONObject(0);
                if (frame != null) showStopLine(frame.getString("url"), frame.getInt("line"));
                displayedPause = state.getLong("pauseId");
            }
            synchronizationWarning = false;
        } catch (Exception e) {
            // Source, values and expressions must never enter the generic Studio logs.
            if (!synchronizationWarning) com.twinsoft.convertigo.eclipse.ConvertigoPlugin.logWarning(
                    null, "Unable to synchronize Eclipse JavaScript debugger markers.", false);
            synchronizationWarning = true;
        }
    }
    private void showStopLine(String url, int line) throws Exception {
        clearAnnotation();
        var file = files.get(url);
        if (file == null && url.contains("/")) {
            int slash = url.indexOf('/');
            var project = ResourcesPlugin.getWorkspace().getRoot().getProject(url.substring(0, slash));
            if (project.exists()) file = project.getFile(url.substring(slash + 1));
        }
        if (file == null || !file.exists()) return;
        var page = getSite().getPage();
        if (!(IDE.openEditor(page, file, false) instanceof ITextEditor editor)) return;
        var document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        int editorLine = line - offset(file) - 1;
        if (editorLine < 0 || editorLine >= document.getNumberOfLines()) return;
        var region = document.getLineInformation(editorLine);
        annotationModel = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
        annotation = new Annotation("org.eclipse.debug.ui.currentIP", false, "JavaScript execution paused");
        annotationModel.addAnnotation(annotation, new Position(region.getOffset(), region.getLength()));
        editor.selectAndReveal(region.getOffset(), region.getLength());
    }
    private void poll() {
        if (browser == null || browser.isDisposed()) return;
        synchronize();
        browser.getDisplay().timerExec(250, this::poll);
    }

    private AuthenticatedBrowser authentication;

    @Override public void createPartControl(Composite parent) {
        SwtUtils.refreshTheme();
        parent.setLayout(new FillLayout());
        browser = new C8oBrowser(parent, SWT.NONE);
        browser.setUseExternalBrowser(false);
        new C8oBrowserPostMessageHelper(browser);
        authentication = new AuthenticatedBrowser(browser,
                () -> AdminView.getStudioAuthenticatedUrl("/admin/eclipse/javascript-debugger"));
        authentication.load();
        instance = this;
        poll();
        parent.layout(true, true);
    }
    @Override public void setFocus() { if (browser != null) browser.setFocus(); }
    @Override public void dispose() {
        if (instance == this) instance = null;
        JavascriptDebugger.stopStudioSession();
        clearAnnotation();
        installed.clear();
        files.clear();
        if (authentication != null) { authentication.dispose(); authentication = null; }
        if (browser != null) { browser.dispose(); browser = null; }
        super.dispose();
    }
}
