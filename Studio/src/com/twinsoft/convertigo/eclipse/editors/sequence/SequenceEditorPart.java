/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.eclipse.editors.sequence;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Map;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.wst.sse.ui.internal.StructuredTextViewer;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.eclipse.AnimatedGif;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.xmlscanner.ColorManager;
import com.twinsoft.convertigo.eclipse.editors.xmlscanner.XMLConfiguration;
import com.twinsoft.convertigo.eclipse.editors.xmlscanner.XMLPartitionScanner;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.ContextManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineEvent;
import com.twinsoft.convertigo.engine.EngineListener;
import com.twinsoft.convertigo.engine.RequestableEngineEvent;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.util.XMLUtils;

@SuppressWarnings("restriction")
public class SequenceEditorPart extends Composite implements EngineListener{

	private Image imageDebug = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/debug.png"));
	private Image imageDisableDebug = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/debug.d.png"));
	private Image imageRun = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/run.png"));
	private Image imageDisableRun = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/run.d.png"));
	private Image imagePause = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/pause.png"));
	private Image imageDisablePause = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/pause.d.png"));
	private Image imageStep = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/step_by_step.png"));
	private Image imageDisableStep = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/step_by_step.d.png"));
	private Image imageGenerateXml = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/xml.png"));
	private Image imageStop = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/stop.d.png"));
	private Image imageDisableStop = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/stop.png"));
	
    protected SequenceEditor editor = null;
	private Sequence sequence;
    private String contextID = null;
    private String projectName = null;
    
	protected Context context;
	protected AbstractSequenceComposite compositeSequence = null;

	public org.w3c.dom.Document lastGeneratedDocument;
	
	private AnimatedGif animatedWait;
	
	public SequenceEditorPart(SequenceEditor editor, Sequence sequence, Composite parent, int style) {
		super(parent, style);
		this.editor = editor;
		this.sequence = sequence;
		this.context = getStudioContext();
		this.contextID = context.contextID;
		this.projectName = context.projectName;
		toolItemsIds = new HashMap<String, Integer>();
		initialize();
		
		// Registering as Engine listener
		Engine.theApp.addEngineListener(this);
	    
	    animatedWait = new AnimatedGif(getDisplay(), canvas, "/com/twinsoft/convertigo/eclipse/editors/images/wait-ani.gif");
	}

	public void close() {
		// Must stop the GIF animation before closing the sequence editor
    	getDisplay().syncExec(new Runnable() {
			public void run() {
				animatedWait.stop();
			}
		});

		compositeSequence.close();
		
		// Remove Studio context
		Engine.theApp.contextManager.remove(context);
		
		// Deregister as Engine listener
		Engine.theApp.removeEngineListener(this);

	}

	@Override
	public void dispose() {
		imageDebug.dispose();
		imageDisableDebug.dispose();
		imageRun.dispose();
		imageDisableRun.dispose();
		imagePause.dispose();
		imageDisablePause.dispose();
		imageStep.dispose();
		imageDisableStep.dispose();
		imageGenerateXml.dispose();
		imageStop.dispose();
		imageDisableStop.dispose();
		
		canvas.dispose();
		colorManager.dispose();
		super.dispose();
	}
	
	private Context getStudioContext() {
		return getStudioContext(false);
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
	
	ToolItem toolItemGenerateXml = null;
	ToolItem toolItemStopSequence = null;
	ToolItem toolItemDebug = null;
	ToolItem toolItemRun = null;
	ToolItem toolItemPause = null;
	ToolItem toolItemStep = null;
	
	private void initialize() {
		GridLayout gridLayout1 = new GridLayout();
		gridLayout1.horizontalSpacing = 0;
		gridLayout1.marginWidth = 0;
		gridLayout1.marginHeight = 0;
		gridLayout1.verticalSpacing = 0;
		this.setLayout(gridLayout1);
		createTabFolderOutputDesign();
		setSize(new org.eclipse.swt.graphics.Point(547,360));

		if (toolItemGenerateXml != null)
			toolItemGenerateXml.setEnabled(true);
		if (toolItemStopSequence != null)
			toolItemStopSequence.setEnabled(false);

		if (toolItemDebug != null)
			toolItemDebug.setEnabled(true);
		if (toolItemRun != null)
			toolItemRun.setEnabled(false);
		if (toolItemPause != null)
			toolItemPause.setEnabled(false);
		if (toolItemStep != null)
			toolItemStep.setEnabled(false);
	}
	
	private TabFolder tabFolderOutputDesign = null;
	private TabItem tabItemOutput = null;
	
	private void createTabFolderOutputDesign() {
		GridData gridData2 = new org.eclipse.swt.layout.GridData();
		gridData2.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData2.grabExcessVerticalSpace = true;
		gridData2.grabExcessHorizontalSpace = true;
		gridData2.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		tabFolderOutputDesign = new TabFolder(this, SWT.BOTTOM);
		tabFolderOutputDesign.setLayoutData(gridData2);
		createCompositeOutput();
		tabItemOutput = new TabItem(tabFolderOutputDesign, SWT.NONE);
		tabItemOutput.setText("Output");
		tabItemOutput.setControl(compositeOutput);
	}
	
	private void createCompositeOutput() {
		GridLayout gridLayout = new GridLayout();
		gridLayout.horizontalSpacing = 0;
		gridLayout.marginHeight = 0;
		gridLayout.verticalSpacing = 0;
		gridLayout.numColumns = 1;
		gridLayout.marginWidth = 0;
		GridData gridData = new org.eclipse.swt.layout.GridData();
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		compositeOutput = new Composite(tabFolderOutputDesign, SWT.NONE);
		compositeOutput.setLayout(gridLayout);
		createCompositeOutputHeader();
		createSashForm();
		createComposite();
	}
	
	private SashForm sashForm = null;
	private Composite compositeOutput = null;
	private ToolBar toolBar = null;
	private Map<String, Integer> toolItemsIds = null;
	private Composite compositeXml = null;
	private Composite compositeOutputHeader = null;
	private Composite compositeOutputFooter = null;
    private Canvas canvas = null;
	
	private void createCompositeOutputHeader() {
		final Color background = getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
		
		GridLayout gridLayout3 = new GridLayout();
		gridLayout3.numColumns = 2;
		GridData gridData1 = new org.eclipse.swt.layout.GridData();
		gridData1.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData1.grabExcessHorizontalSpace = true;
		gridData1.grabExcessVerticalSpace = false;
		gridData1.verticalAlignment = org.eclipse.swt.layout.GridData.BEGINNING;
		compositeOutputHeader = new Composite(compositeOutput, SWT.NONE);
		compositeOutputHeader.setBackground(background);
		compositeOutputHeader.setLayoutData(gridData1);
		compositeOutputHeader.setLayout(gridLayout3);

		createToolBar();

		GridData gridData6 = new org.eclipse.swt.layout.GridData();
		gridData6.horizontalAlignment = org.eclipse.swt.layout.GridData.END;
		gridData6.heightHint = 16;
		gridData6.widthHint = 104;
		gridData6.verticalAlignment = org.eclipse.swt.layout.GridData.BEGINNING;
	    
	    canvas = new Canvas(compositeOutputHeader, SWT.NONE);
	    canvas.setLayoutData(gridData6);
	    canvas.setVisible(false);
	}
	
	/**
	 * This method initializes composite	
	 *
	 */
	private void createComposite() {
		GridData gridData7 = new org.eclipse.swt.layout.GridData();
		gridData7.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData7.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		GridData gridData4 = new org.eclipse.swt.layout.GridData();
		gridData4.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData4.grabExcessHorizontalSpace = false;
		gridData4.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		GridLayout gridLayout4 = new GridLayout();
		gridLayout4.numColumns = 2;
		compositeOutputFooter = new Composite(compositeOutput, SWT.NONE);
		compositeOutputFooter.setBackground(new Color(Display.getCurrent(), 162, 194, 250));
		compositeOutputFooter.setLayout(gridLayout4);
		compositeOutputFooter.setLayoutData(gridData4);
	}
	
    protected boolean bDebug = false;
    protected boolean bShowBlocks = false;
	protected Boolean bDebugStepByStep = new Boolean(false);
	protected DatabaseObject debugDatabaseObject = new Project();
	
	/**
	 * This method initializes toolBar	
	 *
	 */
	private void createToolBar() {
		int incr = 0;
		getSequenceCompositeClass();
		
		GridData gridData5 = new org.eclipse.swt.layout.GridData();
		gridData5.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData5.grabExcessHorizontalSpace = true;
		gridData5.verticalAlignment = org.eclipse.swt.layout.GridData.BEGINNING;
		toolBar = new ToolBar(compositeOutputHeader, SWT.FLAT);
		toolBar.setLayoutData(gridData5);
		
		toolItemDebug = new ToolItem(toolBar, SWT.CHECK);
		toolItemDebug.setImage(imageDebug);
		toolItemDebug.setDisabledImage(imageDisableDebug);
		toolItemDebug.setToolTipText("Debug mode");
		toolItemDebug.addSelectionListener(new org.eclipse.swt.events.SelectionListener() {
					public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
						if (ConvertigoPlugin.projectManager.currentProject == null) return;
						if (toolItemDebug.getSelection()) {
				        	try {
								ConvertigoPlugin.getDefault().debugConsoleStream.write("Starting debug mode in step by step state...\n");
							} catch (IOException ex) {}								
				            //Studio.theApp.consolePanel.jTabbedPane.setSelectedComponent(Studio.theApp.consolePanel.jScrollPaneDebug);
				            bDebug = true;
				            bDebugStepByStep = new Boolean(true);
				            toolItemRun.setEnabled(true);
				            toolItemPause.setEnabled(false);
				            toolItemStep.setEnabled(true);
				        }
				        else {
				        	try {
								ConvertigoPlugin.getDefault().debugConsoleStream.write("Stopping debug mode.\n");
							} catch (IOException ex) {}								
				            bDebug = false;
				            bDebugStepByStep = new Boolean(false);
				            toolItemRun.setEnabled(false);
				            toolItemPause.setEnabled(false);
				            toolItemStep.setEnabled(false);
				            
				            synchronized(debugDatabaseObject) {
				                debugDatabaseObject.notify();
				            }
				        }
					}
					public void widgetDefaultSelected(
							org.eclipse.swt.events.SelectionEvent e) {
					}
				});
		toolItemsIds.put("Debug", new Integer(incr));
		incr ++;

		toolItemRun = new ToolItem(toolBar, SWT.PUSH);
		toolItemRun.setImage(imageRun);
		toolItemRun.setDisabledImage(imageDisableRun);
		toolItemRun.setToolTipText("Continuous debug mode");
		toolItemRun.addSelectionListener(new org.eclipse.swt.events.SelectionListener() {
					public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				        if (ConvertigoPlugin.projectManager.currentProject == null) return;
				        synchronized(bDebugStepByStep) {
				        	try {
								ConvertigoPlugin.getDefault().debugConsoleStream.write("Changing debug state to continuous\n");
							} catch (IOException ex) {}								
				            bDebugStepByStep = new Boolean(false);
				            toolItemRun.setEnabled(false);
				            toolItemStep.setEnabled(false);
				            toolItemPause.setEnabled(true);
				        }
				        synchronized(debugDatabaseObject) {
				            debugDatabaseObject.notify();
				        }
					}
					public void widgetDefaultSelected(
							org.eclipse.swt.events.SelectionEvent e) {
					}
				});
		toolItemsIds.put("Run", new Integer(incr));
		incr ++;
		
		toolItemPause = new ToolItem(toolBar, SWT.PUSH);
		toolItemPause.setImage(imagePause);
		toolItemPause.setDisabledImage(imageDisablePause);
		toolItemPause.setToolTipText("Pause the debug process");
		toolItemPause.addSelectionListener(new org.eclipse.swt.events.SelectionListener() {
					public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				        if (ConvertigoPlugin.projectManager.currentProject == null) return;
				        synchronized(bDebugStepByStep) {
				        	try {
								ConvertigoPlugin.getDefault().debugConsoleStream.write("Changing debug state to step by step\n");
							} catch (IOException ex) {}								
				            bDebugStepByStep = new Boolean(true);
				            toolItemRun.setEnabled(true);
				            toolItemStep.setEnabled(true);
				            toolItemPause.setEnabled(false);
				        }
						
					}
					public void widgetDefaultSelected(
							org.eclipse.swt.events.SelectionEvent e) {
					}
				});
		toolItemsIds.put("Pause", new Integer(incr));
		incr ++;
		
		toolItemStep = new ToolItem(toolBar, SWT.PUSH);
		toolItemStep.setImage(imageStep);
		toolItemStep.setDisabledImage(imageDisableStep);
		toolItemStep.setToolTipText("Step by step debug mode");
		toolItemStep.addSelectionListener(new org.eclipse.swt.events.SelectionListener() {
					public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				        if (ConvertigoPlugin.projectManager.currentProject == null) return;
				        synchronized(debugDatabaseObject) {
				            debugDatabaseObject.notify();
				            toolItemStep.setEnabled(false);
				        }
					}
					public void widgetDefaultSelected(
							org.eclipse.swt.events.SelectionEvent e) {
					}
				});
		toolItemsIds.put("Step", new Integer(incr));
		incr ++;

		new ToolItem(toolBar, SWT.SEPARATOR);
		incr ++;

		toolItemGenerateXml = new ToolItem(toolBar, SWT.PUSH);
		toolItemGenerateXml.setImage(imageGenerateXml);
		toolItemGenerateXml.setToolTipText("Generate XML");
		toolItemGenerateXml.addSelectionListener(new org.eclipse.swt.events.SelectionListener() {
					public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
						getDocument();
					}

					public void widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent e) {
					}
				});
		toolItemsIds.put("GenerateXML", new Integer(incr));
		incr ++;

		toolItemStopSequence = new ToolItem(toolBar, SWT.PUSH);
		toolItemStopSequence.setDisabledImage(imageStop);
		toolItemStopSequence.setToolTipText("Stop the current sequence");
		toolItemStopSequence.setImage(imageDisableStop);
		toolItemStopSequence.addSelectionListener(new org.eclipse.swt.events.SelectionListener() {
					public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
						try {
							context.abortRequestable();
						}
						catch(NullPointerException npe) {
							// Silently ignore: means the runningTransaction pointer has been set to null
							// because of normal transaction termination... 
						}
					}
					public void widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent e) {
					}
				});
		toolItemsIds.put("StopTransaction", new Integer(incr));
		incr ++;
	}

	void toolBarSetEnable(String toolItemId, boolean enable) {
		final int i = toolItemsIds.get(toolItemId).intValue();
		final boolean enabled = enable;
		getDisplay().syncExec(new Runnable() {
				public void run() {
					ToolItem[] toolItems = toolBar.getItems();
					ToolItem toolItem = toolItems[i];
					if (toolItem != null)
						toolItem.setEnabled(enabled);
				}
		});
	}

	void toolBarSetSelection(String toolItemId, boolean select) {
		final int i = toolItemsIds.get(toolItemId).intValue();
		final boolean selected = select;
		getDisplay().syncExec(new Runnable() {
				public void run() {
					ToolItem[] toolItems = toolBar.getItems();
					ToolItem toolItem = toolItems[i];
					if (toolItem != null)
						toolItem.setSelection(selected);
				}
		});
	}

	/**
	 * This method initializes sashForm	
	 *
	 */
	private void createSashForm() {
		GridData gridData3 = new org.eclipse.swt.layout.GridData();
		gridData3.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData3.grabExcessHorizontalSpace = true;
		gridData3.grabExcessVerticalSpace = true;
		gridData3.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		sashForm = new SashForm(compositeOutput, SWT.NONE);
		sashForm.setLayoutData(gridData3);
		createCompositeSequence();
		createCompositeXml();
		sashForm.setWeights(new int[]{20, 80});
	}

	private Class<?> compositeSequenceClass;  //  @jve:decl-index=0:
	
	private void getSequenceCompositeClass() {
		if (sequence instanceof GenericSequence) {
			compositeSequenceClass = SequenceComposite.class;
		}
		else {
			throw new IllegalArgumentException("The sequence class is not handled: " + sequence.getClass().getName());
		}
	}
	
	/**
	 * This method initializes compositeSequence
	 *
	 */
	private void createCompositeSequence() {
		GridLayout gridLayout2 = new GridLayout();
		gridLayout2.horizontalSpacing = 0;
		gridLayout2.marginWidth = 0;
		gridLayout2.marginHeight = 0;
		gridLayout2.verticalSpacing = 0;
		
		try {
			Constructor<?> constructor = compositeSequenceClass.getConstructor(new Class[] { SequenceEditorPart.class, Sequence.class, Composite.class, int.class});
			compositeSequence = (AbstractSequenceComposite) constructor.newInstance(new Object[] {this, sequence, sashForm, new Integer(SWT.NONE) });

			compositeSequence.setParent(sashForm);
			compositeSequence.setLayout(gridLayout2);
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "An unexpected exception has occured while creating the sequence composite.");
		}
	}

	private ColorManager colorManager;  //  @jve:decl-index=0:
	
	public StructuredTextViewer xmlView = null;
	
	/**
	 * This method initializes compositeXml	
	 *
	 */
	private void createCompositeXml() {
		compositeXml = new Composite(sashForm, SWT.NONE);
		compositeXml.setLayout(new FillLayout());
		
		xmlView = new StructuredTextViewer(compositeXml, null, null, false, SWT.H_SCROLL | SWT.V_SCROLL);
		xmlView.setEditable(false);

		colorManager = new ColorManager();
		xmlView.configure(new XMLConfiguration(colorManager));

		Document document = new Document("Click on the XML generation button to view the XML document generated by Convertigo.");
		IDocumentPartitioner partitioner =
			new FastPartitioner(
				new XMLPartitionScanner(),
				new String[] { 
					XMLPartitionScanner.XML_TAG,
					XMLPartitionScanner.XML_COMMENT,
				}
			);
		partitioner.connect(document);
		document.setDocumentPartitioner(partitioner);
		xmlView.setDocument(document);
	}
	
	protected void getDocument() {
        getDocument(null, null, false);
    }
	
	public void getDocument(String sequenceName, String testcaseName, boolean isStubRequested) {
    	final Map<String, String[]> parameters = new HashMap<String, String[]>();
    	
    	editor.setDirty(true);
    	
		if (sequenceName == null) {
        	sequenceName = sequence.getName();
		}
		
    	parameters.put(Parameter.Sequence.getName(), new String[]{sequenceName});
    	parameters.put(Parameter.Context.getName(), new String[]{contextID});
    	
    	if (testcaseName != null) {
    		parameters.put(Parameter.Testcase.getName(), new String[]{testcaseName});
    	}
    	
        if (isStubRequested) {
        	parameters.put(Parameter.Stub.getName(), new String[]{"true"});
        }
        
        ConvertigoPlugin.getDefault().runRequestable(projectName, parameters);
	}

	public void blocksChanged(EngineEvent engineEvent) {
		// TODO Auto-generated method stub
		
	}

	public void documentGenerated(EngineEvent engineEvent) {
		if (!checkEventSource(engineEvent))
			return;
		
        if (bDebug) {
        	try {
				ConvertigoPlugin.getDefault().debugConsoleStream.write("The XML document has been successfully generated.\n");
			} catch (IOException e) {}
        }

		lastGeneratedDocument = (org.w3c.dom.Document) engineEvent.getSource();
		final String strXML = XMLUtils.prettyPrintDOMWithEncoding(lastGeneratedDocument);
		getDisplay().asyncExec(new Runnable() {
			public void run() {
				xmlView.getDocument().set(strXML);
		    	editor.setDirty(false);
			}
		});
	}
	
	public void clearEditor(EngineEvent engineEvent) {
		if (!checkEventSource(engineEvent))
			return;
		
		getDisplay().asyncExec(new Runnable() {
			public void run() {
				compositeSequence.clearContent();
				xmlView.getDocument().set("Click on the XML generation button to view the XML document generated by Convertigo.");
			}
		});
	}

	public void objectDetected(EngineEvent engineEvent) {
		if (!checkEventSource(engineEvent)) {
			return;
		}
	}

	public void sequenceStarted(EngineEvent engineEvent) {
		if (!checkEventSource(engineEvent))
			return;
		clearEditor(engineEvent);
		getDisplay().syncExec(new Runnable() {
			public void run() {
		        toolItemStopSequence.setEnabled(true);
		        toolItemGenerateXml.setEnabled(false);
		        
		        animatedWait.start();
			}
		});
	}

	public void sequenceFinished(EngineEvent engineEvent) {
		if (!checkEventSource(engineEvent))
			return;
    	
    	getDisplay().asyncExec(new Runnable() {
			public void run() {
				animatedWait.stop();
				
				toolItemStopSequence.setEnabled(false);
		    	toolItemGenerateXml.setEnabled(true);
			}
		});
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
		return isSourceFromSequence;
	}
	
	public void stepReached(EngineEvent engineEvent) {
		if (!checkEventSource(engineEvent))
			return;
		
		if (!bDebug) return;
		final Object source = engineEvent.getSource();
        synchronized(debugDatabaseObject) {
            debugDatabaseObject = (DatabaseObject) source;
        }
        
        try {
			ConvertigoPlugin.getDefault().debugConsoleStream.write("Step reached before executing database object: "+ debugDatabaseObject.getName() +"\n");
		} catch (IOException e1) {}
        
        if (bDebugStepByStep.booleanValue()) {
            try {
                synchronized(debugDatabaseObject) {
            		getDisplay().syncExec(new Runnable() {
            			public void run() {
            				toolItemStep.setEnabled(true);
            			}
            		});
                    debugDatabaseObject.wait();
                }
            }
            catch(InterruptedException e) {
	        	try {
					ConvertigoPlugin.getDefault().debugConsoleStream.write("Next step required\n");
				} catch (IOException ex) {}								
            }
        }
	}

	public void transactionFinished(EngineEvent engineEvent) {
		if (!checkEventSource(engineEvent))
			return;
	}

	public void transactionStarted(EngineEvent engineEvent) {
		if (!checkEventSource(engineEvent))
			return;
	}
}
