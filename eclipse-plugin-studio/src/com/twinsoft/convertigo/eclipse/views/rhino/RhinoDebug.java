/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.twinsoft.convertigo.eclipse.views.rhino;

import java.awt.AWTEvent;
import java.awt.ActiveEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.MenuComponent;
import java.awt.Panel;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.text.Document;
import javax.swing.text.Segment;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.tools.debugger.Dim;
import org.mozilla.javascript.tools.debugger.GuiCallback;
import org.mozilla.javascript.tools.debugger.treetable.JTreeTable;
import org.mozilla.javascript.tools.debugger.treetable.TreeTableModel;
import org.mozilla.javascript.tools.debugger.treetable.TreeTableModelAdapter;

import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils.SelectionListener;
import com.twinsoft.convertigo.engine.util.RhinoUtils;

/** GUI for the Rhino debugger. */
public class RhinoDebug extends Composite implements GuiCallback {
	
	Frame self;
    
	/** The debugger. */
    Dim dim;
    
	private Panel root;
	private ToolBar tb;

    /** The {@link JPanel} that shows information about the context. */
    private ContextWindow context;

    /** The status bar. */
    private JLabel statusBar;
    
    /** Hash table of internal frame names to the internal frames themselves. */
    private final Map<String, JFrame> toplevels =
            Collections.synchronizedMap(new HashMap<String, JFrame>());

    private final Map<String, IEditorReference> editorRefs =
            Collections.synchronizedMap(new TreeMap<String, IEditorReference>());

    private Annotation lastAnnotation;
    private IAnnotationModel lastAnnotationModel;


    /**
     * The AWT EventQueue. Used for manually pumping AWT events from {@link
     * #dispatchNextGuiEvent()}.
     */
    private EventQueue awtEventQueue;

    /** Creates a new SwingGui. */
    public RhinoDebug(Composite parent, int style) {
        super(parent, style);
        setLayout(SwtUtils.newGridLayout(1, true, 0, 0, 0, 0));
        dim = new Dim();
        dim.setGuiCallback(this);
        makeToolbar();
        if (SwtUtils.isDark()) {
        	FlatMacDarkLaf.setup();
        } else {
        	FlatMacLightLaf.setup();
        }
    	RhinoUtils.debugMode = true;
    	var wrapper = new Composite(this, SWT.EMBEDDED | SWT.NO_BACKGROUND);
    	wrapper.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL));
        self = SWT_AWT.new_Frame(wrapper);
        root = new Panel();
        root.setLayout(new BorderLayout());
        self.add(root);
        init();	
		updateEnabled(false);
    }
    
    private void makeToolbar() {
    	var bars = new Composite(this, SWT.NONE);
    	bars.setLayout(SwtUtils.newGridLayout(3, false, 0, 0, 1, 1));
		bars.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		var toolbar = new ToolBar(bars, SWT.NONE);
    	toolbar.setLayoutData(new GridData(GridData.CENTER));
		
    	var self = new ToolItem(toolbar, SWT.CHECK);
    	self.addSelectionListener((SelectionListener) e ->{
			if (self.getSelection()) {
				SwtUtils.setToolItemIcon(self, "icons/connector_color_16x16.gif", "Enable Debugger", "Enable Debugger");
				dim.clearAllBreakpoints();
				dim.setReturnValue(Dim.GO);
				updateEnabled(false);
		        dim.detach();
		        RhinoDebug.this.self.setVisible(false);
			} else {
				SwtUtils.setToolItemIcon(self, "icons/studio/disconnect.gif", "Disable Debugger", "Disable Debugger");
		        dim.attachTo(ContextFactory.getGlobal());
		        if (RhinoDebug.this.self != null) {
		        	RhinoDebug.this.self.setVisible(true);
		        }
			}
		});
    	self.notifyListeners(SWT.Selection, new Event());
		
		new ToolItem(toolbar, SWT.SEPARATOR);
    	
		var toolitem = new ToolItem(toolbar, SWT.CHECK);
		SwtUtils.setToolItemIcon(toolitem, "icons/studio/pause.d.gif", "Break Exception", "Break on Exception");
		toolitem.addSelectionListener((SelectionListener) e -> dim.setBreakOnExceptions(((ToolItem) e.widget).getSelection()));
		
		toolitem = new ToolItem(toolbar, SWT.CHECK);
		SwtUtils.setToolItemIcon(toolitem, "icons/studio/handlers_sc_entry.gif", "Break Funct in", "Break on Function enter");
		toolitem.addSelectionListener((SelectionListener) e -> dim.setBreakOnEnter(((ToolItem) e.widget).getSelection()));
		
		toolitem = new ToolItem(toolbar, SWT.CHECK);
		SwtUtils.setToolItemIcon(toolitem, "icons/studio/handlers_sc_exit.gif", "Break Funct out", "Break on Function return");
		toolitem.addSelectionListener((SelectionListener) e -> dim.setBreakOnReturn(((ToolItem) e.widget).getSelection()));
		
		var gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 0;
		new Composite(bars, SWT.NONE).setLayoutData(gd);
		
		toolbar = tb = new ToolBar(bars, SWT.NONE);
    	toolbar.setLayoutData(new GridData(GridData.CENTER));
    	
    	toolitem = new ToolItem(toolbar, SWT.PUSH);
		SwtUtils.setToolItemIcon(toolitem, "icons/studio/pause.gif", "Break", "Break on next execution");
		toolitem.addSelectionListener((SelectionListener) e -> dim.setBreak());

		toolitem = new ToolItem(toolbar, SWT.PUSH);
		SwtUtils.setToolItemIcon(toolitem, "icons/studio/run.gif", "Go", "Continue execution");
		toolitem.addSelectionListener((SelectionListener) e -> {
			updateEnabled(false);
			dim.setReturnValue(Dim.GO);
		});
		
		toolitem = new ToolItem(toolbar, SWT.PUSH);
		SwtUtils.setToolItemIcon(toolitem, "icons/studio/step_in.png", "Into", "Step into");
		toolitem.addSelectionListener((SelectionListener) e -> {
			updateEnabled(false);
			dim.setReturnValue(Dim.STEP_INTO);
		});
		
		toolitem = new ToolItem(toolbar, SWT.PUSH);
		SwtUtils.setToolItemIcon(toolitem, "icons/studio/step_by_step.gif", "Over", "Step Over");
		toolitem.addSelectionListener((SelectionListener) e -> {
			updateEnabled(false);
			dim.setReturnValue(Dim.STEP_OVER);
		});
		
		toolitem = new ToolItem(toolbar, SWT.PUSH);
		SwtUtils.setToolItemIcon(toolitem, "icons/studio/step_out.png", "Out", "Step Out");
		toolitem.addSelectionListener((SelectionListener) e -> {
			updateEnabled(false);
			dim.setReturnValue(Dim.STEP_OUT);
		});
    }
    
    /** Records a new internal frame. */
    void addTopLevel(String key, JFrame frame) {
        if (frame != self) {
            toplevels.put(key, frame);
        }
    }
    
    /** Constructs the debugger GUI. */
    private void init() {
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());
        root.add(contentPane, BorderLayout.CENTER);
        context = new ContextWindow(this);
        context.setPreferredSize(new Dimension(600, 120));
        context.setMinimumSize(new Dimension(50, 50));
        contentPane.add(context);
        statusBar = new JLabel();
        statusBar.setText("Thread: ");
        contentPane.add(statusBar, BorderLayout.SOUTH);
    }
    
    IEditorReference getEditorReference(String url) {
    	var editorRef = new IEditorReference[] { editorRefs.get(url) };
    	
    	if (editorRef[0] != null && editorRef[0].getEditor(false) == null) {
    		editorRefs.remove(url);
    		editorRef[0] = null;
    	}
    	
    	if (editorRef[0] == null) {
    		execSync(() -> {
    			try {
    				for (var ref: PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences()) {
    					if (ref.getEditorInput() instanceof FileEditorInput editor) {
    						var path = editor.getFile().getProjectRelativePath().toString();
    						var name = editor.getFile().getParent().getName();
    						if (url.startsWith(name) || path.endsWith(url)) {
    							editorRefs.put(url, editorRef[0] = ref);
    							break;
    						};
    					}
    				}
    			} catch (Exception e) {
    				e.printStackTrace();
    			}
    		});
    	}
    	return editorRef[0];
    }
    
    /** Shows the line at which execution in the given stack frame just stopped. */
    boolean showStopLine(Dim.StackFrame frame) {
    	var ref = getEditorReference(frame.getUrl());
    	if (ref == null) {
    		return false;
    	}
    	var ed = (ITextEditor) ref.getEditor(false);
    	int lineNumber = frame.getLineNumber();
    	var source = frame.sourceInfo().source();
    	var start = 0;
    	var index = source.indexOf('\n');
    	var length = Math.max(0, index);
    	while (--lineNumber > 0) {
    		start = start + length + 1;
    		index = source.indexOf('\n', start);
    		if (index == -1) {
    			index = source.length();
    		}
    		length = Math.max(0, index - start);
    	}
    	Position pos = new Position(start, length);

    	clearLastAnnotation();
    	lastAnnotationModel = ed.getDocumentProvider().getAnnotationModel(ed.getEditorInput());
    	lastAnnotation = new Annotation("org.eclipse.debug.ui.currentIP", false, "breaked");
    	lastAnnotationModel.addAnnotation(lastAnnotation, pos);
    	execAsync(() -> {
    		try {
    			var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    			page.bringToTop(ed);
    			var view = page.findViewReference("com.twinsoft.convertigo.eclipse.views.rhino.RhinoDebugView");
    			page.bringToTop(view.getView(false));
    		} catch (Exception e) {
				e.printStackTrace();
    		}
    	});
    	return true;
    }
    
    void clearLastAnnotation() {
		if (lastAnnotationModel != null && lastAnnotation != null) {
			lastAnnotationModel.removeAnnotation(lastAnnotation);
		}
		lastAnnotation = null;
		lastAnnotationModel = null;
    }
    
    /** Handles script interruption. */
    void enterInterruptImpl(Dim.StackFrame lastFrame, String threadTitle, String alertMessage) {
        if (!showStopLine(lastFrame)) {
        	dim.setReturnValue(Dim.GO);
        	return;
        }
        statusBar.setText("Thread: " + threadTitle);
        
        if (alertMessage != null) {
            MessageDialogWrapper.showMessageDialog(
                    self, alertMessage, "Exception in Script", JOptionPane.ERROR_MESSAGE);
        }

        updateEnabled(true);

        Dim.ContextData contextData = lastFrame.contextData();

        JComboBox<String> ctx = context.context;
        List<String> toolTips = context.toolTips;
        context.disableUpdate();
        int frameCount = contextData.frameCount();
        ctx.removeAllItems();
        // workaround for JDK 1.4 bug that caches selected value even after
        // removeAllItems() is called
        ctx.setSelectedItem(null);
        toolTips.clear();
        for (int i = 0; i < frameCount; i++) {
            Dim.StackFrame frame = contextData.getFrame(i);
            String url = frame.getUrl();
            int lineNumber = frame.getLineNumber();
            String shortName = url;
            if (url.length() > 20) {
                shortName = "..." + url.substring(url.length() - 17);
            }
            String location = "\"" + shortName + "\", line " + lineNumber;
            ctx.insertItemAt(location, i);
            location = "\"" + url + "\", line " + lineNumber;
            toolTips.add(location);
        }
        context.enableUpdate();
        ctx.setSelectedIndex(0);
        ctx.setMinimumSize(new Dimension(50, ctx.getMinimumSize().height));
    }

    /** Enables or disables the menu and tool bars with respect to the state of script execution. */
    private void updateEnabled(boolean interrupted) {
    	execAsync(() -> {
    		int count = tb.getItemCount();
    		tb.getItem(0).setEnabled(!interrupted);
    		for (int i = 1; i < count; i++) {
    			tb.getItem(i).setEnabled(interrupted);
    		}
    	});
    	if (interrupted) {
        	context.setEnabled(true);
    	} else {
    		clearLastAnnotation();
        }
    }
    
	/**
     * Calls {@link JSplitPane#setResizeWeight} via reflection. For compatibility, since JDK &lt;
     * 1.3 does not have this method.
     */
    static void setResizeWeight(JSplitPane pane, double weight) {
        try {
            Method m = JSplitPane.class.getMethod("setResizeWeight", new Class[] {double.class});
            m.invoke(pane, new Object[] {Double.valueOf(weight)});
        } catch (NoSuchMethodException exc) {
        } catch (IllegalAccessException exc) {
        } catch (java.lang.reflect.InvocationTargetException exc) {
        }
    }

    // GuiCallback

    /** Called when the source text for a script has been updated. */
    @Override
    public void updateSourceText(Dim.SourceInfo sourceInfo) {
    	var url = sourceInfo.url();
    	String ignoreList = "utils";
    	if (ignoreList.contains(url)) {
    		return;
    	}
    	var editorRef = getEditorReference(url);
    	if (editorRef == null) {
    		return;
    	}
		sourceInfo.removeAllBreakpoints();
		
    	execSync(() -> {
    		var files = new HashSet<IResource>();
    		var bpManager = DebugPlugin.getDefault().getBreakpointManager();
    		for (var bp: bpManager.getBreakpoints()) {
    			try {
    				var marker = bp.getMarker();
    				var file = marker.getResource();
    				if (!files.add(file)) {
    					continue;
    				}
    				
    				var editor = (ITextEditor) editorRef.getEditor(false);
    				if (editor.getEditorInput() instanceof FileEditorInput input
    						&& file.equals(input.getFile())
    						&& editorRef.getEditor(false) != null) {
    				} else {
    					continue;
    				}

    				var mks = file.findMarkers(marker.getType(), false, IResource.DEPTH_ZERO);
    				for (var mk: mks) {
    					var ln = mk.getAttribute(IMarker.LINE_NUMBER);
    					sourceInfo.breakpoint((int) ln, true);
    				}
    				return;
    			} catch (Exception e) {
    				e.printStackTrace();
    			}
    		}
    	});
    }

    /** Called when the interrupt loop has been entered. */
    @Override
    public void enterInterrupt(Dim.StackFrame lastFrame, String threadTitle, String alertMessage) {
        if (SwingUtilities.isEventDispatchThread()) {
            enterInterruptImpl(lastFrame, threadTitle, alertMessage);
        } else {
            RunProxy proxy = new RunProxy(this, RunProxy.ENTER_INTERRUPT);
            proxy.lastFrame = lastFrame;
            proxy.threadTitle = threadTitle;
            proxy.alertMessage = alertMessage;
            SwingUtilities.invokeLater(proxy);
        }
    }

    /** Returns whether the current thread is the GUI event thread. */
    @Override
    public boolean isGuiEventThread() {
        return SwingUtilities.isEventDispatchThread();
    }

    /** Processes the next GUI event. */
    @Override
    public void dispatchNextGuiEvent() throws InterruptedException {
        EventQueue queue = awtEventQueue;
        if (queue == null) {
            queue = Toolkit.getDefaultToolkit().getSystemEventQueue();
            awtEventQueue = queue;
        }
        AWTEvent event = queue.getNextEvent();
        if (event instanceof ActiveEvent) {
            ((ActiveEvent) event).dispatch();
        } else {
            Object source = event.getSource();
            if (source instanceof Component) {
                Component comp = (Component) source;
                comp.dispatchEvent(event);
            } else if (source instanceof MenuComponent) {
                ((MenuComponent) source).dispatchEvent(event);
            }
        }
    }

	@Override
	public boolean setFocus() {
		SwingUtilities.invokeLater(() -> self.requestFocus());
		return super.setFocus();
	}
    
    @Override
	public void dispose() {
    	clearLastAnnotation();
    	dim.clearAllBreakpoints();
		dim.setReturnValue(Dim.GO);
        dim.setReturnValue(Dim.EXIT);
		dim.detach();
		RhinoUtils.debugMode = false;
		SwingUtilities.invokeLater(() -> self.dispose());
		super.dispose();
	}
    
    void execAsync(Runnable run) {
    	if (Thread.currentThread() == getDisplay().getThread()) {
    		run.run();
    	} else {
    		getDisplay().asyncExec(run);
    	}
    }
    
    void execSync(Runnable run) {
    	if (Thread.currentThread() == getDisplay().getThread()) {
    		run.run();
    	} else {
    		getDisplay().syncExec(run);
    	}
    }
}

/** Helper class for showing a message dialog. */
class MessageDialogWrapper {

    /** Shows a message dialog, wrapping the <code>msg</code> at 60 columns. */
    public static void showMessageDialog(Component parent, String msg, String title, int flags) {
        if (msg.length() > 60) {
            StringBuilder buf = new StringBuilder();
            int len = msg.length();
            int j = 0;
            int i;
            for (i = 0; i < len; i++, j++) {
                char c = msg.charAt(i);
                buf.append(c);
                if (Character.isWhitespace(c)) {
                    int k;
                    for (k = i + 1; k < len; k++) {
                        if (Character.isWhitespace(msg.charAt(k))) {
                            break;
                        }
                    }
                    if (k < len) {
                        int nextWordLen = k - i;
                        if (j + nextWordLen > 60) {
                            buf.append('\n');
                            j = 0;
                        }
                    }
                }
            }
            msg = buf.toString();
        }
        JOptionPane.showMessageDialog(parent, msg, title, flags);
    }
}

/** Extension of JTextArea for script evaluation input. */
class EvalTextArea extends JTextArea implements KeyListener, DocumentListener {

    /** Serializable magic number. */
    private static final long serialVersionUID = -3918033649601064194L;

    /** The debugger GUI. */
    private RhinoDebug debugGui;

    /** History of expressions that have been evaluated */
    private List<String> history;

    /** Index of the selected history item. */
    private int historyIndex = -1;

    /** Position in the display where output should go. */
    private int outputMark;

    /** Creates a new EvalTextArea. */
    public EvalTextArea(RhinoDebug debugGui) {
        this.debugGui = debugGui;
        history = Collections.synchronizedList(new ArrayList<String>());
        Document doc = getDocument();
        doc.addDocumentListener(this);
        addKeyListener(this);
        setLineWrap(true);
        setFont(new Font("Monospaced", 0, Math.max(12, UIManager.getFont("Label.font").getSize())));
        append("% ");
        outputMark = doc.getLength();
    }

    /** Selects a subrange of the text. */
    @Override
    public void select(int start, int end) {
        // requestFocus();
        super.select(start, end);
    }

    /** Called when Enter is pressed. */
    private synchronized void returnPressed() {
        Document doc = getDocument();
        int len = doc.getLength();
        Segment segment = new Segment();
        try {
            doc.getText(outputMark, len - outputMark, segment);
        } catch (javax.swing.text.BadLocationException ignored) {
            ignored.printStackTrace();
        }
        String text = segment.toString();
        if (debugGui.dim.stringIsCompilableUnit(text)) {
            if (text.trim().length() > 0) {
                history.add(text);
                historyIndex = history.size();
            }
            append("\n");
            String result = debugGui.dim.eval(text);
            if (result.length() > 0) {
                append(result);
                append("\n");
            }
            append("% ");
            outputMark = doc.getLength();
        } else {
            append("\n");
        }
    }

    /** Writes output into the text area. */
    public synchronized void write(String str) {
        insert(str, outputMark);
        int len = str.length();
        outputMark += len;
        select(outputMark, outputMark);
    }

    // KeyListener

    /** Called when a key is pressed. */
    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_BACK_SPACE || code == KeyEvent.VK_LEFT) {
            if (outputMark == getCaretPosition()) {
                e.consume();
            }
        } else if (code == KeyEvent.VK_HOME) {
            int caretPos = getCaretPosition();
            if (caretPos == outputMark) {
                e.consume();
            } else if (caretPos > outputMark) {
                if (!e.isControlDown()) {
                    if (e.isShiftDown()) {
                        moveCaretPosition(outputMark);
                    } else {
                        setCaretPosition(outputMark);
                    }
                    e.consume();
                }
            }
        } else if (code == KeyEvent.VK_ENTER) {
            returnPressed();
            e.consume();
        } else if (code == KeyEvent.VK_UP) {
            historyIndex--;
            if (historyIndex >= 0) {
                if (historyIndex >= history.size()) {
                    historyIndex = history.size() - 1;
                }
                if (historyIndex >= 0) {
                    String str = history.get(historyIndex);
                    int len = getDocument().getLength();
                    replaceRange(str, outputMark, len);
                    int caretPos = outputMark + str.length();
                    select(caretPos, caretPos);
                } else {
                    historyIndex++;
                }
            } else {
                historyIndex++;
            }
            e.consume();
        } else if (code == KeyEvent.VK_DOWN) {
            int caretPos = outputMark;
            if (history.size() > 0) {
                historyIndex++;
                if (historyIndex < 0) {
                    historyIndex = 0;
                }
                int len = getDocument().getLength();
                if (historyIndex < history.size()) {
                    String str = history.get(historyIndex);
                    replaceRange(str, outputMark, len);
                    caretPos = outputMark + str.length();
                } else {
                    historyIndex = history.size();
                    replaceRange("", outputMark, len);
                }
            }
            select(caretPos, caretPos);
            e.consume();
        }
    }

    /** Called when a key is typed. */
    @Override
    public void keyTyped(KeyEvent e) {
        int keyChar = e.getKeyChar();
        if (keyChar == 0x8 /* KeyEvent.VK_BACK_SPACE */) {
            if (outputMark == getCaretPosition()) {
                e.consume();
            }
        } else if (getCaretPosition() < outputMark) {
            setCaretPosition(outputMark);
        }
    }

    /** Called when a key is released. */
    @Override
    public synchronized void keyReleased(KeyEvent e) {}

    // DocumentListener

    /** Called when text was inserted into the text area. */
    @Override
    public synchronized void insertUpdate(DocumentEvent e) {
        int len = e.getLength();
        int off = e.getOffset();
        if (outputMark > off) {
            outputMark += len;
        }
    }

    /** Called when text was removed from the text area. */
    @Override
    public synchronized void removeUpdate(DocumentEvent e) {
        int len = e.getLength();
        int off = e.getOffset();
        if (outputMark > off) {
            if (outputMark >= off + len) {
                outputMark -= len;
            } else {
                outputMark = off;
            }
        }
    }

    /** Attempts to clean up the damage done by {@link #updateUI()}. */
    public synchronized void postUpdateUI() {
        requestFocus();
        setCaret(getCaret());
        select(outputMark, outputMark);
    }

    /** Called when text has changed in the text area. */
    @Override
    public synchronized void changedUpdate(DocumentEvent e) {}
}

/** An internal frame for evaluating script. */
class EvalWindow extends JInternalFrame implements ActionListener {

    /** Serializable magic number. */
    private static final long serialVersionUID = -2860585845212160176L;

    /** The text area into which expressions can be typed. */
    private EvalTextArea evalTextArea;

    /** Creates a new EvalWindow. */
    public EvalWindow(String name, RhinoDebug debugGui) {
        super(name, true, false, true, true);
        evalTextArea = new EvalTextArea(debugGui);
        evalTextArea.setRows(24);
        evalTextArea.setColumns(80);
        JScrollPane scroller = new JScrollPane(evalTextArea);
        setContentPane(scroller);
        // scroller.setPreferredSize(new Dimension(600, 400));
        pack();
        setVisible(true);
    }

    /** Sets whether the text area is enabled. */
    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        evalTextArea.setEnabled(b);
    }

    // ActionListener

    /** Performs an action on the text area. */
    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (cmd.equals("Cut")) {
            evalTextArea.cut();
        } else if (cmd.equals("Copy")) {
            evalTextArea.copy();
        } else if (cmd.equals("Paste")) {
            evalTextArea.paste();
        }
    }
}

/** Table model class for watched expressions. */
class MyTableModel extends AbstractTableModel {

    /** Serializable magic number. */
    private static final long serialVersionUID = 2971618907207577000L;

    /** The debugger GUI. */
    private RhinoDebug debugGui;

    /** List of watched expressions. */
    private List<String> expressions;

    /** List of values from evaluated from {@link #expressions}. */
    private List<String> values;

    /** Creates a new MyTableModel. */
    public MyTableModel(RhinoDebug debugGui) {
        this.debugGui = debugGui;
        expressions = Collections.synchronizedList(new ArrayList<String>());
        values = Collections.synchronizedList(new ArrayList<String>());
        expressions.add("");
        values.add("");
    }

    /** Returns the number of columns in the table (2). */
    @Override
    public int getColumnCount() {
        return 2;
    }

    /** Returns the number of rows in the table. */
    @Override
    public int getRowCount() {
        return expressions.size();
    }

    /** Returns the name of the given column. */
    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return "Expression";
            case 1:
                return "Value";
        }
        return null;
    }

    /** Returns whether the given cell is editable. */
    @Override
    public boolean isCellEditable(int row, int column) {
        return true;
    }

    /** Returns the value in the given cell. */
    @Override
    public Object getValueAt(int row, int column) {
        switch (column) {
            case 0:
                return expressions.get(row);
            case 1:
                return values.get(row);
        }
        return "";
    }

    /** Sets the value in the given cell. */
    @Override
    public void setValueAt(Object value, int row, int column) {
        switch (column) {
            case 0:
                String expr = value.toString();
                expressions.set(row, expr);
                String result = "";
                if (expr.length() > 0) {
                    result = debugGui.dim.eval(expr);
                    if (result == null) result = "";
                }
                values.set(row, result);
                updateModel();
                if (row + 1 == expressions.size()) {
                    expressions.add("");
                    values.add("");
                    fireTableRowsInserted(row + 1, row + 1);
                }
                break;
            case 1:
                // just reset column 2; ignore edits
                fireTableDataChanged();
        }
    }

    /** Re-evaluates the expressions in the table. */
    void updateModel() {
        for (int i = 0; i < expressions.size(); ++i) {
            String expr = expressions.get(i);
            String result = "";
            if (expr.length() > 0) {
                result = debugGui.dim.eval(expr);
                if (result == null) result = "";
            } else {
                result = "";
            }
            result = result.replace('\n', ' ');
            values.set(i, result);
        }
        fireTableDataChanged();
    }
}

/** A table for evaluated expressions. */
class Evaluator extends JTable {

    /** Serializable magic number. */
    private static final long serialVersionUID = 8133672432982594256L;

    /** The {@link TableModel} for this table. */
    MyTableModel tableModel;

    /** Creates a new Evaluator. */
    public Evaluator(RhinoDebug debugGui) {
        super(new MyTableModel(debugGui));
        tableModel = (MyTableModel) getModel();
    }
}

/** Tree model for script object inspection. */
class VariableModel implements TreeTableModel {

    /** Serializable magic number. */
    private static final String[] cNames = {" Name", " Value"};

    /** Tree column types. */
    private static final Class<?>[] cTypes = {TreeTableModel.class, String.class};

    /** Empty {@link VariableNode} array. */
    private static final VariableNode[] CHILDLESS = new VariableNode[0];

    /** The debugger. */
    private Dim debugger;

    /** The root node. */
    private VariableNode root;
    
    private Set<String> filter;

    /** Creates a new VariableModel. */
    public VariableModel() {}


    public VariableModel(Dim debugger, Object scope) {
    	this(debugger, scope, Collections.emptySet());
    }
    
    /** Creates a new VariableModel. */
    public VariableModel(Dim debugger, Object scope, Collection<String> filter) {
        this.debugger = debugger;
        this.root = new VariableNode(scope, "this");
        this.filter = new HashSet<>(filter);
    }

    // TreeTableModel

    /** Returns the root node of the tree. */
    @Override
    public Object getRoot() {
        if (debugger == null) {
            return null;
        }
        return root;
    }

    /** Returns the number of children of the given node. */
    @Override
    public int getChildCount(Object nodeObj) {
        if (debugger == null) {
            return 0;
        }
        VariableNode node = (VariableNode) nodeObj;
        return children(node).length;
    }

    /** Returns a child of the given node. */
    @Override
    public Object getChild(Object nodeObj, int i) {
        if (debugger == null) {
            return null;
        }
        VariableNode node = (VariableNode) nodeObj;
        return children(node)[i];
    }

    /** Returns whether the given node is a leaf node. */
    @Override
    public boolean isLeaf(Object nodeObj) {
        if (debugger == null) {
            return true;
        }
        VariableNode node = (VariableNode) nodeObj;
        return children(node).length == 0;
    }

    /** Returns the index of a node under its parent. */
    @Override
    public int getIndexOfChild(Object parentObj, Object childObj) {
        if (debugger == null) {
            return -1;
        }
        VariableNode parent = (VariableNode) parentObj;
        VariableNode child = (VariableNode) childObj;
        VariableNode[] children = children(parent);
        for (int i = 0; i != children.length; i++) {
            if (children[i] == child) {
                return i;
            }
        }
        return -1;
    }

    /** Returns whether the given cell is editable. */
    @Override
    public boolean isCellEditable(Object node, int column) {
        return column == 0;
    }

    /** Sets the value at the given cell. */
    @Override
    public void setValueAt(Object value, Object node, int column) {}

    /** Adds a TreeModelListener to this tree. */
    @Override
    public void addTreeModelListener(TreeModelListener l) {}

    /** Removes a TreeModelListener from this tree. */
    @Override
    public void removeTreeModelListener(TreeModelListener l) {}

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {}

    // TreeTableNode

    /** Returns the number of columns. */
    @Override
    public int getColumnCount() {
        return cNames.length;
    }

    /** Returns the name of the given column. */
    @Override
    public String getColumnName(int column) {
        return cNames[column];
    }

    /** Returns the type of value stored in the given column. */
    @Override
    public Class<?> getColumnClass(int column) {
    	if (column >= 0 && column < cTypes.length) {
    		return cTypes[column];
    	}
    	return null;
    }

    /** Returns the value at the given cell. */
    @Override
    public Object getValueAt(Object nodeObj, int column) {
        if (debugger == null) {
            return null;
        }
        VariableNode node = (VariableNode) nodeObj;
        switch (column) {
            case 0: // Name
                return node.toString();
            case 1: // Value
                String result;
                try {
                    result = debugger.objectToString(getValue(node));
                } catch (RuntimeException exc) {
                    result = exc.getMessage();
                    if (result == null) {
                        result = exc.toString();
                    }
                }
                StringBuilder buf = new StringBuilder();
                int len = result.length();
                for (int i = 0; i < len; i++) {
                    char ch = result.charAt(i);
                    if (Character.isISOControl(ch)) {
                        ch = ' ';
                    }
                    buf.append(ch);
                }
                return buf.toString();
        }
        return null;
    }

    /** Returns an array of the children of the given node. */
    private VariableNode[] children(VariableNode node) {
        if (node.children != null) {
            return node.children;
        }

        VariableNode[] children;

        Object value = getValue(node);
        Object[] ids = debugger.getObjectIds(value);
        if (node == root && !filter.isEmpty()) {
        	var copy = new ArrayList<Object>();
        	for (var id: ids) {
        		if (!filter.contains(id)) {
        			copy.add(id);
        		}
        	}
        	ids = copy.toArray();
        }
        if (ids == null || ids.length == 0) {
            children = CHILDLESS;
        } else {
            Arrays.sort(
                    ids,
                    new Comparator<Object>() {
                        @Override
                        public int compare(Object l, Object r) {
                            if (l instanceof String) {
                                if (r instanceof Integer) {
                                    return -1;
                                }
                                return ((String) l).compareToIgnoreCase((String) r);
                            }
                            if (r instanceof String) {
                                return 1;
                            }
                            int lint = ((Integer) l).intValue();
                            int rint = ((Integer) r).intValue();
                            return lint - rint;
                        }
                    });
            children = new VariableNode[ids.length];
            for (int i = 0; i != ids.length; ++i) {
                children[i] = new VariableNode(value, ids[i]);
            }
        }
        node.children = children;
        return children;
    }

    /** Returns the value of the given node. */
    public Object getValue(VariableNode node) {
        try {
            return debugger.getObjectProperty(node.object, node.id);
        } catch (Exception exc) {
            return "undefined";
        }
    }

    /** A variable node in the tree. */
    private static class VariableNode {

        /** The script object. */
        private Object object;

        /** The object name. Either a String or an Integer. */
        private Object id;

        /** Array of child nodes. This is filled with the properties of the object. */
        private VariableNode[] children;

        /** Creates a new VariableNode. */
        public VariableNode(Object object, Object id) {
            this.object = object;
            this.id = id;
        }

        /** Returns a string representation of this node. */
        @Override
        public String toString() {
            return id instanceof String ? (String) id : "[" + ((Integer) id).intValue() + "]";
        }
    }
}

/** A tree table for browsing script objects. */
class MyTreeTable extends JTreeTable {

    /** Serializable magic number. */
    private static final long serialVersionUID = 3457265548184453049L;

    /** Creates a new MyTreeTable. */
    public MyTreeTable(VariableModel model) {
        super(model);
    }

    /** Initializes a tree for this tree table. */
    public JTree resetTree(TreeTableModel treeTableModel) {
        tree = new TreeTableCellRenderer(treeTableModel);

        // Install a tableModel representing the visible rows in the tree.
        super.setModel(new TreeTableModelAdapter(treeTableModel, tree));

        // Force the JTable and JTree to share their row selection models.
        ListToTreeSelectionModelWrapper selectionWrapper = new ListToTreeSelectionModelWrapper();
        tree.setSelectionModel(selectionWrapper);
        setSelectionModel(selectionWrapper.getListSelectionModel());

        // Make the tree and table row heights the same.
        if (tree.getRowHeight() < 1) {
            // Metal looks better like this.
            setRowHeight(18);
        } else if (tree.getRowHeight() != getRowHeight()) {
            tree.setRowHeight(getRowHeight());
        }

        // Install the tree editor renderer and editor.
        setDefaultRenderer(TreeTableModel.class, tree);
        setDefaultEditor(TreeTableModel.class, new TreeTableCellEditor());
        setShowGrid(true);
        setIntercellSpacing(new Dimension(1, 1));
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        DefaultTreeCellRenderer r = (DefaultTreeCellRenderer) tree.getCellRenderer();
        r.setOpenIcon(null);
        r.setClosedIcon(null);
        r.setLeafIcon(null);
        return tree;
    }

    /**
     * Returns whether the cell under the coordinates of the mouse in the {@link EventObject} is
     * editable.
     */
    public boolean isCellEditable(EventObject e) {
        if (e instanceof MouseEvent) {
            MouseEvent me = (MouseEvent) e;
            // If the modifiers are not 0 (or the left mouse button),
            // tree may try and toggle the selection, and table
            // will then try and toggle, resulting in the
            // selection remaining the same. To avoid this, we
            // only dispatch when the modifiers are 0 (or the left mouse
            // button).
            if (me.getModifiersEx() == 0
                    || ((me.getModifiersEx() & (InputEvent.BUTTON1_DOWN_MASK | 1024)) != 0
                            && (me.getModifiersEx()
                                            & (InputEvent.SHIFT_DOWN_MASK
                                                    | InputEvent.CTRL_DOWN_MASK
                                                    | InputEvent.ALT_DOWN_MASK
                                                    | InputEvent.BUTTON2_DOWN_MASK
                                                    | InputEvent.BUTTON3_DOWN_MASK
                                                    | 64
                                                    | // SHIFT_DOWN_MASK
                                                    128
                                                    | // CTRL_DOWN_MASK
                                                    512
                                                    | // ALT_DOWN_MASK
                                                    2048
                                                    | // BUTTON2_DOWN_MASK
                                                    4096 // BUTTON3_DOWN_MASK
                                            ))
                                    == 0)) {
                int row = rowAtPoint(me.getPoint());
                for (int counter = getColumnCount() - 1; counter >= 0; counter--) {
                    if (TreeTableModel.class == getColumnClass(counter)) {
                        MouseEvent newME =
                                new MouseEvent(
                                        MyTreeTable.this.tree,
                                        me.getID(),
                                        me.getWhen(),
                                        me.getModifiersEx(),
                                        me.getX() - getCellRect(row, counter, true).x,
                                        me.getY(),
                                        me.getClickCount(),
                                        me.isPopupTrigger());
                        MyTreeTable.this.tree.dispatchEvent(newME);
                        break;
                    }
                }
            }
            if (me.getClickCount() >= 3) {
                return true;
            }
            return false;
        }
        if (e == null) {
            return true;
        }
        return false;
    }
}

/** Panel that shows information about the context. */
class ContextWindow extends JPanel implements ActionListener {

    /** Serializable magic number. */
    private static final long serialVersionUID = 2306040975490228051L;

    /** The debugger GUI. */
    private RhinoDebug debugGui;

    /** The combo box that holds the stack frames. */
    JComboBox<String> context;

    /** Tool tips for the stack frames. */
    List<String> toolTips;

    /** Tabbed pane for "this" and "locals". */
    private JTabbedPane tabs;

    /** Tabbed pane for "watch" and "evaluate". */
    private JTabbedPane tabs2;

    /** The table showing the "this" object. */
    private MyTreeTable thisTable;

    /** The table showing the stack local variables. */
    private MyTreeTable localsTable;

    /** The table showing all objects. */
    private MyTreeTable allTable;


    /** The {@link #evaluator}'s table model. */
    private MyTableModel tableModel;

    /** The script evaluator table. */
    private Evaluator evaluator;

    /** The script evaluation text area. */
    private EvalTextArea cmdLine;

    /** The split pane. */
    JSplitPane split;

    /** Whether the ContextWindow is enabled. */
    private boolean enabled;

    /** Creates a new ContextWindow. */
    public ContextWindow(final RhinoDebug debugGui) {
        this.debugGui = debugGui;
        enabled = false;
        JPanel left = new JPanel();
        JToolBar t1 = new JToolBar();
        t1.setName("Variables");
        t1.setLayout(new GridLayout());
        t1.add(left);
        JPanel p1 = new JPanel();
        p1.setLayout(new GridLayout());
        JPanel p2 = new JPanel();
        p2.setLayout(new GridLayout());
        p1.add(t1);
        JLabel label = new JLabel("Context:");
        context = new JComboBox<>();
        context.setLightWeightPopupEnabled(false);
        toolTips = Collections.synchronizedList(new java.util.ArrayList<String>());
        label.setBorder(context.getBorder());
        context.addActionListener(this);
        context.setActionCommand("ContextSwitch");
        GridBagLayout layout = new GridBagLayout();
        left.setLayout(layout);
        GridBagConstraints lc = new GridBagConstraints();
        lc.insets.left = 5;
        lc.anchor = GridBagConstraints.WEST;
        lc.ipadx = 5;
        layout.setConstraints(label, lc);
        left.add(label);
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        layout.setConstraints(context, c);
        left.add(context);
        tabs = new JTabbedPane(SwingConstants.BOTTOM);
        tabs.setPreferredSize(new Dimension(500, 300));
        thisTable = new MyTreeTable(new VariableModel());
        JScrollPane jsp = new JScrollPane(thisTable);
        jsp.getViewport().setViewSize(new Dimension(5, 2));
        tabs.add("this", jsp);
        localsTable = new MyTreeTable(new VariableModel());
        localsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        localsTable.setPreferredSize(null);
        jsp = new JScrollPane(localsTable);
        tabs.add("Locals", jsp);
        allTable = new MyTreeTable(new VariableModel());
        allTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        allTable.setPreferredSize(null);
        jsp = new JScrollPane(allTable);
        tabs.add("Alls", jsp);
        c.weightx = c.weighty = 1;
        c.gridheight = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.WEST;
        layout.setConstraints(tabs, c);
        left.add(tabs);
        evaluator = new Evaluator(debugGui);
        cmdLine = new EvalTextArea(debugGui);
        // cmdLine.requestFocus();
        tableModel = evaluator.tableModel;
        jsp = new JScrollPane(evaluator);
        JToolBar t2 = new JToolBar();
        t2.setName("Evaluate");
        tabs2 = new JTabbedPane(SwingConstants.BOTTOM);
        tabs2.add("Watch", jsp);
        tabs2.add("Evaluate", new JScrollPane(cmdLine));
        tabs2.setPreferredSize(new Dimension(500, 300));
        t2.setLayout(new GridLayout());
        t2.add(tabs2);
        p2.add(t2);
        evaluator.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, p1, p2);
        split.setOneTouchExpandable(true);
        RhinoDebug.setResizeWeight(split, 0.5);
        setLayout(new BorderLayout());
        add(split, BorderLayout.CENTER);

        final JToolBar finalT1 = t1;
        final JToolBar finalT2 = t2;
        final JPanel finalP1 = p1;
        final JPanel finalP2 = p2;
        final JSplitPane finalSplit = split;
        final JPanel finalThis = this;

        ComponentListener clistener =
                new ComponentListener() {
                    boolean t2Docked = true;

                    void check(Component comp) {
                        Component thisParent = finalThis.getParent();
                        if (thisParent == null) {
                            return;
                        }
                        Component parent = finalT1.getParent();
                        boolean leftDocked = true;
                        boolean rightDocked = true;
                        boolean adjustVerticalSplit = false;
                        if (parent != null) {
                            if (parent != finalP1) {
                                while (!(parent instanceof JFrame)) {
                                    parent = parent.getParent();
                                }
                                JFrame frame = (JFrame) parent;
                                debugGui.addTopLevel("Variables", frame);

                                // We need the following hacks because:
                                // - We want an undocked toolbar to be
                                //   resizable.
                                // - We are using JToolbar as a container of a
                                //   JComboBox. Without this JComboBox's popup
                                //   can get left floating when the toolbar is
                                //   re-docked.
                                //
                                // We make the frame resizable and then
                                // remove JToolbar's window listener
                                // and insert one of our own that first ensures
                                // the JComboBox's popup window is closed
                                // and then calls JToolbar's window listener.
                                if (!frame.isResizable()) {
                                    frame.setResizable(true);
                                    frame.setDefaultCloseOperation(
                                            WindowConstants.DO_NOTHING_ON_CLOSE);
                                    final WindowListener[] l =
                                            frame.getListeners(WindowListener.class);
                                    frame.removeWindowListener(l[0]);
                                    frame.addWindowListener(
                                            new WindowAdapter() {
                                                @Override
                                                public void windowClosing(WindowEvent e) {
                                                    context.hidePopup();
                                                    l[0].windowClosing(e);
                                                }
                                            });
                                    // adjustVerticalSplit = true;
                                }
                                leftDocked = false;
                            } else {
                                leftDocked = true;
                            }
                        }
                        parent = finalT2.getParent();
                        if (parent != null) {
                            if (parent != finalP2) {
                                while (!(parent instanceof JFrame)) {
                                    parent = parent.getParent();
                                }
                                JFrame frame = (JFrame) parent;
                                debugGui.addTopLevel("Evaluate", frame);
                                frame.setResizable(true);
                                rightDocked = false;
                            } else {
                                rightDocked = true;
                            }
                        }
                        if (leftDocked && t2Docked && rightDocked && t2Docked) {
                            // no change
                            return;
                        }
                        t2Docked = rightDocked;
                        JSplitPane split = (JSplitPane) thisParent;
                        if (leftDocked) {
                            if (rightDocked) {
                                finalSplit.setDividerLocation(0.5);
                            } else {
                                finalSplit.setDividerLocation(1.0);
                            }
                            if (adjustVerticalSplit) {
                                split.setDividerLocation(0.66);
                            }

                        } else if (rightDocked) {
                            finalSplit.setDividerLocation(0.0);
                            split.setDividerLocation(0.66);
                        } else {
                            // both undocked
                            split.setDividerLocation(1.0);
                        }
                    }

                    @Override
                    public void componentHidden(ComponentEvent e) {
                        check(e.getComponent());
                    }

                    @Override
                    public void componentMoved(ComponentEvent e) {
                        check(e.getComponent());
                    }

                    @Override
                    public void componentResized(ComponentEvent e) {
                        check(e.getComponent());
                    }

                    @Override
                    public void componentShown(ComponentEvent e) {
                        check(e.getComponent());
                    }
                };
        p1.addContainerListener(
                new ContainerListener() {
                    @Override
                    public void componentAdded(ContainerEvent e) {
                        Component thisParent = finalThis.getParent();
                        JSplitPane split = (JSplitPane) thisParent;
                        if (e.getChild() == finalT1) {
                            if (finalT2.getParent() == finalP2) {
                                // both docked
                                finalSplit.setDividerLocation(0.5);
                            } else {
                                // left docked only
                                finalSplit.setDividerLocation(1.0);
                            }
                            split.setDividerLocation(0.66);
                        }
                    }

                    @Override
                    public void componentRemoved(ContainerEvent e) {
                        Component thisParent = finalThis.getParent();
                        JSplitPane split = (JSplitPane) thisParent;
                        if (e.getChild() == finalT1) {
                            if (finalT2.getParent() == finalP2) {
                                // right docked only
                                finalSplit.setDividerLocation(0.0);
                                split.setDividerLocation(0.66);
                            } else {
                                // both undocked
                                split.setDividerLocation(1.0);
                            }
                        }
                    }
                });
        t1.addComponentListener(clistener);
        t2.addComponentListener(clistener);
        setEnabled(false);
    }

    /** Enables or disables the component. */
    @Override
    public void setEnabled(boolean enabled) {
        context.setEnabled(enabled);
        thisTable.setEnabled(enabled);
        localsTable.setEnabled(enabled);
        allTable.setEnabled(enabled);
        evaluator.setEnabled(enabled);
        cmdLine.setEnabled(enabled);
    }

    /** Disables updating of the component. */
    public void disableUpdate() {
        enabled = false;
    }

    /** Enables updating of the component. */
    public void enableUpdate() {
        enabled = true;
    }

    // ActionListener

    /** Performs an action. */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!enabled) return;
        if (e.getActionCommand().equals("ContextSwitch")) {
            Dim.ContextData contextData = debugGui.dim.currentContextData();
            if (contextData == null) {
                return;
            }
            int frameIndex = context.getSelectedIndex();
            context.setToolTipText(toolTips.get(frameIndex));
            int frameCount = contextData.frameCount();
            if (frameIndex >= frameCount) {
                return;
            }
            Dim.StackFrame frame = contextData.getFrame(frameIndex);
            Object scope = frame.scope();
            Object thisObj = frame.thisObj();
            thisTable.resetTree(new VariableModel(debugGui.dim, thisObj, filters));
            allTable.resetTree(new VariableModel(debugGui.dim, thisObj));
            VariableModel scopeModel;
            if (scope != thisObj) {
                scopeModel = new VariableModel(debugGui.dim, scope);
            } else {
                scopeModel = new VariableModel();
            }
            localsTable.resetTree(scopeModel);
            debugGui.dim.contextSwitch(frameIndex);
            debugGui.showStopLine(frame);
            tableModel.updateModel();
        }
    }
    
	final static Collection<String> filters = Arrays.asList("__proto__", "Function", "Object", "Error", "CallSite",
			"decodeURI", "decodeURIComponent", "encodeURI", "encodeURIComponent", "escape", "eval", "isFinite", "isNaN",
			"isXMLName", "parseFloat", "parseInt", "unescape", "uneval", "NaN", "Infinity", "undefined", "globalThis",
			"EvalError", "RangeError", "ReferenceError", "SyntaxError", "TypeError", "URIError", "InternalError",
			"JavaException", "Array", "String", "Boolean", "Number", "Math", "JSON", "With", "Call", "Script",
			"Iterator", "StopIteration", "RegExp", "Continuation", "XML", "XMLList", "Namespace", "QName",
			"ArrayBuffer", "Int8Array", "Uint8Array", "Uint8ClampedArray", "Int16Array", "Uint16Array", "Int32Array",
			"Uint32Array", "Float32Array", "Float64Array", "DataView", "Symbol", "Map", "Promise", "Set", "WeakMap",
			"WeakSet", "BigInt", "Packages", "getClass", "JavaAdapter", "JavaImporter", "java", "javax", "org", "com",
			"edu", "net", "steps", "use", "include", "synchronized", "Date");
}

/**
 * Class to consolidate all cases that require to implement Runnable to avoid class generation
 * bloat.
 */
class RunProxy implements Runnable {

    // Constants for 'type'.
    static final int OPEN_FILE = 1;
    static final int LOAD_FILE = 2;
    static final int UPDATE_SOURCE_TEXT = 3;
    static final int ENTER_INTERRUPT = 4;

    /** The debugger GUI. */
    private RhinoDebug debugGui;

    /** The type of Runnable this object is. Takes one of the constants defined in this class. */
    private int type;

    /** The name of the file to open or load. */
    String fileName;

    /** The source text to update. */
    String text;

    /** The source for which to update the text. */
    Dim.SourceInfo sourceInfo;

    /** The frame to interrupt in. */
    Dim.StackFrame lastFrame;

    /** The name of the interrupted thread. */
    String threadTitle;

    /** The message of the exception thrown that caused the thread interruption, if any. */
    String alertMessage;

    /** Creates a new RunProxy. */
    public RunProxy(RhinoDebug debugGui, int type) {
        this.debugGui = debugGui;
        this.type = type;
    }

    /** Runs this Runnable. */
    @Override
    public void run() {
        switch (type) {
            case OPEN_FILE:
                try {
                    debugGui.dim.compileScript(fileName, text);
                } catch (RuntimeException ex) {
                    MessageDialogWrapper.showMessageDialog(
                            debugGui.self,
                            ex.getMessage(),
                            "Error Compiling " + fileName,
                            JOptionPane.ERROR_MESSAGE);
                }
                break;

            case LOAD_FILE:
                try {
                    debugGui.dim.evalScript(fileName, text);
                } catch (RuntimeException ex) {
                    MessageDialogWrapper.showMessageDialog(
                    		debugGui.self,
                            ex.getMessage(),
                            "Run error for " + fileName,
                            JOptionPane.ERROR_MESSAGE);
                }
                break;

            case UPDATE_SOURCE_TEXT:
                break;

            case ENTER_INTERRUPT:
                debugGui.enterInterruptImpl(lastFrame, threadTitle, alertMessage);
                break;

            default:
                throw new IllegalArgumentException(String.valueOf(type));
        }
    }
}
