/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.editors.sequence;

import java.io.IOException;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ViewImageProvider;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.ContextManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineEvent;
import com.twinsoft.convertigo.engine.EngineListener;
import com.twinsoft.convertigo.engine.RequestableEngineEvent;
import com.twinsoft.convertigo.engine.enums.JsonOutput;
import com.twinsoft.convertigo.engine.enums.JsonOutput.JsonRoot;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.enums.RequestAttribute;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class SequenceEditorPart extends Composite implements EngineListener{

	private Image imageDebug = ViewImageProvider.getImageFromCache("/com/twinsoft/convertigo/eclipse/editors/images/debug.png");
	private Image imageDisableDebug = ViewImageProvider.getImageFromCache("/com/twinsoft/convertigo/eclipse/editors/images/debug.d.png");
	private Image imageRun = ViewImageProvider.getImageFromCache("/com/twinsoft/convertigo/eclipse/editors/images/run.png");
	private Image imageDisableRun = ViewImageProvider.getImageFromCache("/com/twinsoft/convertigo/eclipse/editors/images/run.d.png");
	private Image imagePause = ViewImageProvider.getImageFromCache("/com/twinsoft/convertigo/eclipse/editors/images/pause.png");
	private Image imageDisablePause = ViewImageProvider.getImageFromCache("/com/twinsoft/convertigo/eclipse/editors/images/pause.d.png");
	private Image imageStep = ViewImageProvider.getImageFromCache("/com/twinsoft/convertigo/eclipse/editors/images/step_by_step.png");
	private Image imageDisableStep = ViewImageProvider.getImageFromCache("/com/twinsoft/convertigo/eclipse/editors/images/step_by_step.d.png");
	private Image imageGenerate = ViewImageProvider.getImageFromCache("/com/twinsoft/convertigo/beans/steps/images/sequencestep_16x16.png");
	private Image imageRenderXml = ViewImageProvider.getImageFromCache("/com/twinsoft/convertigo/eclipse/editors/images/xml.png");
	private Image imageRenderJson = ViewImageProvider.getImageFromCache("/com/twinsoft/convertigo/eclipse/editors/images/json.png");
	private Image imageStop = ViewImageProvider.getImageFromCache("/com/twinsoft/convertigo/eclipse/editors/images/stop.d.png");
	private Image imageDisableStop = ViewImageProvider.getImageFromCache("/com/twinsoft/convertigo/eclipse/editors/images/stop.png");
	private Image imageDisabledFullResult = ViewImageProvider.getImageFromCache("/com/twinsoft/convertigo/eclipse/editors/images/forward_history.d.png");
	private Image imageFullResult = ViewImageProvider.getImageFromCache("/com/twinsoft/convertigo/eclipse/editors/images/forward_history.png");

	protected SequenceEditor editor = null;
	private Sequence sequence;
	private String contextID = null;
	private String projectName = null;

	protected Context context;
//	protected AbstractSequenceComposite compositeSequence = null;

	public org.w3c.dom.Document lastGeneratedDocument;
	
	private String shortResultXML;
	private String shortResultJSON;
	private String fullResultXML;
	private String fullResultJSON;
	
	private Map<String, String[]> lastParameters = null;
	
	private SequenceEditorInput inputXML = null;
	private SequenceEditorInput inputJSON = null;
	private SequenceEditorInput inputTXT = null;
	
	private boolean useType;
	private JsonRoot jsonRoot;

	public SequenceEditorPart(SequenceEditor editor, Sequence sequence, Composite parent, int style) {
		super(parent, style);
		this.editor = editor;
		this.sequence = sequence;
		this.context = getStudioContext();
		this.contextID = context.contextID;
		this.projectName = context.projectName;
		toolItemsIds = new HashMap<String, Integer>();
		
		inputXML = new SequenceEditorInput(sequence, sequence.getQName() + ".xml");
		inputJSON = new SequenceEditorInput(sequence, sequence.getQName() + ".json");
		inputTXT = new SequenceEditorInput(sequence, "wait.txt");
		
		initialize();

		// Registering as Engine listener
		Engine.theApp.addEngineListener(this);
	}

	public void close() {
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
		imageGenerate.dispose();
		imageRenderXml.dispose();
		imageRenderJson.dispose();
		imageStop.dispose();
		imageDisableStop.dispose();
		imageDisabledFullResult.dispose();
		imageFullResult.dispose();
		
		super.dispose();
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

	ToolItem toolItemGenerate = null;
	ToolItem toolItemRenderXml = null;
	ToolItem toolItemRenderJson = null;
	ToolItem toolItemStopSequence = null;
	ToolItem toolItemDebug = null;
	ToolItem toolItemRun = null;
	ToolItem toolItemPause = null;
	ToolItem toolItemStep = null;
	ToolItem toolItemFullResult = null;

	private void initialize() {
		this.setLayout(new FillLayout());
		createCompositeOutput(this);
		setSize(new org.eclipse.swt.graphics.Point(547,360));

		if (toolItemRenderXml != null)
			toolItemRenderXml.setEnabled(true);
		if (toolItemRenderJson != null) 
			toolItemRenderJson.setEnabled(true);
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

	private void createCompositeOutput(Composite parent) {
		GridLayout gridLayout = new GridLayout();
		gridLayout.horizontalSpacing = 0;
		gridLayout.marginHeight = 0;
		gridLayout.verticalSpacing = 0;
		gridLayout.numColumns = 1;
		gridLayout.marginWidth = 0;
		compositeOutput = new Composite(parent, SWT.NONE);
		compositeOutput.setLayout(gridLayout);
		createCompositeOutputHeader(compositeOutput);
		createCompositeXml(compositeOutput);
	}
	
	private Composite compositeOutput = null;
	private ToolBar toolBar = null;
	private Map<String, Integer> toolItemsIds = null;
	private Composite compositeXml = null;
	private Composite compositeOutputHeader = null;

	private void createCompositeOutputHeader(Composite parent) {
		final Color background = getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);

		GridLayout gridLayout3 = new GridLayout();
		gridLayout3.numColumns = 2;
		GridData gridData1 = new org.eclipse.swt.layout.GridData();
		gridData1.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData1.grabExcessHorizontalSpace = true;
		gridData1.grabExcessVerticalSpace = false;
		gridData1.verticalAlignment = org.eclipse.swt.layout.GridData.BEGINNING;
		compositeOutputHeader = new Composite(parent, SWT.NONE);
		compositeOutputHeader.setBackground(background);
		compositeOutputHeader.setLayoutData(gridData1);
		compositeOutputHeader.setLayout(gridLayout3);

		createToolBar();
	}

	protected boolean bDebug = false;
	protected boolean bShowBlocks = false;
	protected boolean bDebugStepByStep = false;
	protected DatabaseObject debugDatabaseObject = new Project();

	/**
	 * This method initializes toolBar
	 *
	 */
	private void createToolBar() {
		int incr = 0;

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
		toolItemDebug.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				if (ConvertigoPlugin.projectManager.currentProject == null) return;
				if (toolItemDebug.getSelection()) {
					try {
						ConvertigoPlugin.getDefault().debugConsoleStream.write("Starting debug mode in step by step state...\n");
					} catch (IOException ex) {}
					bDebug = true;
					bDebugStepByStep = Boolean.valueOf(true);
					toolItemRun.setEnabled(true);
					toolItemPause.setEnabled(false);
					toolItemStep.setEnabled(true);
				}
				else {
					try {
						ConvertigoPlugin.getDefault().debugConsoleStream.write("Stopping debug mode.\n");
					} catch (IOException ex) {}
					bDebug = false;
					bDebugStepByStep = Boolean.valueOf(false);
					toolItemRun.setEnabled(false);
					toolItemPause.setEnabled(false);
					toolItemStep.setEnabled(false);

					synchronized(debugDatabaseObject) {
						debugDatabaseObject.notify();
					}
				}
			}
			public void widgetDefaultSelected(
					SelectionEvent e) {
			}
		});
		toolItemsIds.put("Debug", Integer.valueOf(incr));
		incr ++;

		toolItemRun = new ToolItem(toolBar, SWT.PUSH);
		toolItemRun.setImage(imageRun);
		toolItemRun.setDisabledImage(imageDisableRun);
		toolItemRun.setToolTipText("Continuous debug mode");
		toolItemRun.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				if (ConvertigoPlugin.projectManager.currentProject == null) {
					return;
				}
				try {
					ConvertigoPlugin.getDefault().debugConsoleStream.write("Changing debug state to continuous\n");
				} catch (IOException ex) {}
				bDebugStepByStep = Boolean.valueOf(false);
				toolItemRun.setEnabled(false);
				toolItemStep.setEnabled(false);
				toolItemPause.setEnabled(true);
				synchronized(debugDatabaseObject) {
					debugDatabaseObject.notify();
				}
			}
			public void widgetDefaultSelected(
					SelectionEvent e) {
			}
		});
		toolItemsIds.put("Run", Integer.valueOf(incr));
		incr ++;

		toolItemPause = new ToolItem(toolBar, SWT.PUSH);
		toolItemPause.setImage(imagePause);
		toolItemPause.setDisabledImage(imageDisablePause);
		toolItemPause.setToolTipText("Pause the debug process");
		toolItemPause.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				if (ConvertigoPlugin.projectManager.currentProject == null) {
					return;
				}
				try {
					ConvertigoPlugin.getDefault().debugConsoleStream.write("Changing debug state to step by step\n");
				} catch (IOException ex) {}
				bDebugStepByStep = Boolean.valueOf(true);
				toolItemRun.setEnabled(true);
				toolItemStep.setEnabled(true);
				toolItemPause.setEnabled(false);
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		toolItemsIds.put("Pause", Integer.valueOf(incr));
		incr ++;

		toolItemStep = new ToolItem(toolBar, SWT.PUSH);
		toolItemStep.setImage(imageStep);
		toolItemStep.setDisabledImage(imageDisableStep);
		toolItemStep.setToolTipText("Step by step debug mode");
		toolItemStep.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				if (ConvertigoPlugin.projectManager.currentProject == null) return;
				synchronized(debugDatabaseObject) {
					debugDatabaseObject.notify();
					toolItemStep.setEnabled(false);
				}
			}
			public void widgetDefaultSelected(
					SelectionEvent e) {
			}
		});
		toolItemsIds.put("Step", Integer.valueOf(incr));
		incr ++;

		new ToolItem(toolBar, SWT.SEPARATOR);
		incr ++;

		toolItemGenerate = new ToolItem(toolBar, SWT.PUSH);
		toolItemGenerate.setImage(imageGenerate);
		toolItemGenerate.setToolTipText("Execute again");
		toolItemGenerate.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				editor.setDirty(true);
				toolItemRenderJson.setEnabled(false);
				toolItemRenderXml.setEnabled(false);
				if (lastParameters != null) {
					ConvertigoPlugin.getDefault().runRequestable(projectName, lastParameters);
				} else {
					getDocument(null, null, null, false);
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		toolItemsIds.put("Execute", Integer.valueOf(incr));
		incr ++;

		toolItemStopSequence = new ToolItem(toolBar, SWT.PUSH);
		toolItemStopSequence.setDisabledImage(imageStop);
		toolItemStopSequence.setToolTipText("Stop the current sequence");
		toolItemStopSequence.setImage(imageDisableStop);
		toolItemStopSequence.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				try {
					if (context != null) {
						context.abortRequestable();
					}
				}
				catch(NullPointerException npe) {
					// Silently ignore: means the runningTransaction pointer has been set to null
					// because of normal transaction termination... 
				}
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		toolItemsIds.put("StopTransaction", Integer.valueOf(incr));
		incr ++;

		new ToolItem(toolBar, SWT.SEPARATOR);
		incr ++;

		SelectionListener sl = new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				ConvertigoPlugin.setProperty(ConvertigoPlugin.PREFERENCE_EDITOR_OUTPUT_MODE, e.widget == toolItemRenderJson ? "json" : "xml");
				Engine.execute(() -> {
					renderDocument();
				});
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		};
		
		toolItemRenderXml = new ToolItem(toolBar, SWT.RADIO);
		toolItemRenderXml.setImage(imageRenderXml);
		toolItemRenderXml.setToolTipText("XML Requester");
		toolItemRenderXml.addSelectionListener(sl);
		toolItemsIds.put("RenderXML", Integer.valueOf(incr));
		incr ++;

		toolItemRenderJson = new ToolItem(toolBar, SWT.RADIO);
		toolItemRenderJson.setImage(imageRenderJson);
		toolItemRenderJson.setToolTipText("JSON Requester");
		toolItemRenderJson.addSelectionListener(sl);
		toolItemsIds.put("RenderJSON", Integer.valueOf(incr));

		if ("json".equals(ConvertigoPlugin.getProperty(ConvertigoPlugin.PREFERENCE_EDITOR_OUTPUT_MODE))) {
			toolItemRenderJson.setSelection(true);
		} else {
			toolItemRenderXml.setSelection(true);
		}
		incr ++;

		new ToolItem(toolBar, SWT.SEPARATOR);
		incr ++;

		toolItemFullResult = new ToolItem(toolBar, SWT.PUSH);
		toolItemFullResult.setDisabledImage(imageDisabledFullResult);
		toolItemFullResult.setToolTipText("Show the full result");
		toolItemFullResult.setImage(imageFullResult);
		toolItemFullResult.setEnabled(false);
		toolItemFullResult.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				if (toolItemRenderJson.getSelection()) {
					if (fullResultJSON != null) {
						getInput().fileWrite(fullResultJSON);
						fullResultJSON = null;
						toolItemFullResult.setEnabled(false);
					}
				} else if (fullResultXML != null) {
					getInput().fileWrite(fullResultXML);
					toolItemFullResult.setEnabled(false);
					fullResultXML = null;
				}
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		incr ++;
	}

	void toolBarSetEnable(String toolItemId, boolean enable) {
		final int i = toolItemsIds.get(toolItemId).intValue();
		final boolean enabled = enable;
		getDisplay().syncExec(() -> {
			ToolItem[] toolItems = toolBar.getItems();
			ToolItem toolItem = toolItems[i];
			if (toolItem != null)
				toolItem.setEnabled(enabled);
		});
	}

	void toolBarSetSelection(String toolItemId, boolean select) {
		final int i = toolItemsIds.get(toolItemId).intValue();
		final boolean selected = select;
		getDisplay().syncExec(() -> {
			ToolItem[] toolItems = toolBar.getItems();
			ToolItem toolItem = toolItems[i];
			if (toolItem != null)
				toolItem.setSelection(selected);
		});
	}

	/**
	 * This method initializes compositeXml	
	 *
	 */
	private void createCompositeXml(Composite parent) {
		compositeXml = new Composite(parent, SWT.NONE);
		GridData gd = new org.eclipse.swt.layout.GridData();
		gd.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		gd.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		compositeXml.setLayoutData(gd);
		compositeXml.setLayout(new FillLayout());

		editor.createEditorControl(compositeXml);
	}

	public void getDocument(String sequenceName, String testcaseName, String stubFileName, boolean isStubRequested) {
		final Map<String, String[]> parameters = new HashMap<String, String[]>();

		editor.setDirty(true);
		toolItemRenderJson.setEnabled(false);
		toolItemRenderXml.setEnabled(false);

		if (sequenceName == null) {
			sequenceName = sequence.getName();
		}

		parameters.put(Parameter.Sequence.getName(), new String[]{sequenceName});
		parameters.put(Parameter.Context.getName(), new String[]{contextID});

		if (testcaseName != null) {
			parameters.put(Parameter.Testcase.getName(), new String[]{testcaseName});
		}

		if (stubFileName != null) {
			parameters.put(Parameter.StubFilename.getName(), new String[]{stubFileName});
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
		shortResultJSON = shortResultXML = fullResultJSON = fullResultXML = null;
		renderDocument();
	}
	
	private void renderDocument() {
		String str;
		boolean[] isJsonMode = {false};
		getDisplay().syncExec(() -> {
			editor.setDirty(false);
			toolItemRenderJson.setEnabled(true);
			toolItemRenderXml.setEnabled(true);
			isJsonMode[0] = toolItemRenderJson.getSelection();
			
			if (isJsonMode[0]) {
				if (editor.getEditorInput() != inputJSON && shortResultJSON != null) {
					editor.setInput(inputJSON);
				}
				toolItemFullResult.setEnabled(fullResultJSON != null);
			} else if (!isJsonMode[0]) {
				if (editor.getEditorInput() != inputXML && shortResultXML != null) {
					editor.setInput(inputXML);
				}
				toolItemFullResult.setEnabled(fullResultXML != null);
			}
		});
		
		if ((isJsonMode[0] && shortResultJSON != null) || (!isJsonMode[0] && shortResultXML != null)) {
			return;
		}
		
		SequenceEditorInput input = getInput();
		if (isJsonMode[0]) {
			try {
				str =  XMLUtils.XmlToJson(lastGeneratedDocument.getDocumentElement(), true, useType, jsonRoot);
				str = str.replaceAll("\n( +)", "\n$1$1");
			} catch (JSONException e) {
				str = e.getMessage();
			}
			input = inputJSON;
		} else {
			str = XMLUtils.prettyPrintDOMWithEncoding(lastGeneratedDocument);
			input = inputXML;
		}
		
		boolean hasFull = str.length() > 10000;
		if (hasFull) {
			if (isJsonMode[0]) {
				fullResultJSON = str;
			} else {
				fullResultXML = str;
			}
			
			str = str.substring(0, 10000) + "\n... [reduced content, click the Full Result button in the toolbar to show the full version]";
		}
		
		if (isJsonMode[0]) {
			shortResultJSON = str;
		} else {
			shortResultXML = str;
		}
		
		input.fileWrite(str);
		
		SequenceEditorInput i = input;
		getDisplay().syncExec(() -> {
			toolItemFullResult.setEnabled(hasFull);
			if (editor.getEditorInput() != i) {
				editor.setInput(i);
			}
		});
	}

	public void clearEditor(EngineEvent engineEvent) {
		if (!checkEventSource(engineEvent))
			return;

		getDisplay().syncExec(() -> {
			if (!inputTXT.fileExists()) {
				inputTXT.fileWrite("Please wait during the Sequence execution.");
			}
			if (editor.getEditorInput() != inputTXT) {
				editor.setInput(inputTXT);
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
		if (engineEvent.getSource() instanceof Sequence seq) {
			RequestAttribute.debug.set(seq.context.httpServletRequest, bDebug);
			context = seq.context;
			useType = context.project != null && context.project.getJsonOutput() == JsonOutput.useType;
			jsonRoot = context.project != null ? context.project.getJsonRoot() : JsonRoot.docNode;
		}
		getDisplay().syncExec(() -> {
			toolItemStopSequence.setEnabled(true);
			toolItemGenerate.setEnabled(false);
			toolItemRenderJson.setEnabled(false);
			toolItemRenderXml.setEnabled(false);
		});
	}

	public void sequenceFinished(EngineEvent engineEvent) {
		if (!checkEventSource(engineEvent))
			return;
		
		if (context != null) {
			lastParameters = new HashMap<>(context.httpServletRequest.getParameterMap());
		}
		context = null;
		
		getDisplay().syncExec(() -> {
			toolItemRenderJson.setEnabled(true);
			toolItemRenderXml.setEnabled(true);
			if ("json".equals(ConvertigoPlugin.getProperty(ConvertigoPlugin.PREFERENCE_EDITOR_OUTPUT_MODE))) {
				toolItemRenderJson.setSelection(true);
				toolItemRenderXml.setSelection(false);
			} else {
				toolItemRenderXml.setSelection(true);
				toolItemRenderJson.setSelection(false);
			}
			toolItemStopSequence.setEnabled(false);
			toolItemGenerate.setEnabled(true);
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

	public void stepReached(EngineEvent engineEvent) {
		if (!checkEventSource(engineEvent))
			return;

		if (!bDebug) return;
//		if (!bDebugStepByStep) return;
		final Object source = engineEvent.getSource();
		
		String str;
		boolean[] isJsonMode = {false};
		getDisplay().syncExec(() -> {
			isJsonMode[0] = toolItemRenderJson.getSelection();
		});
		
		SequenceEditorInput input;
		if (isJsonMode[0]) {
			try {
				str =  XMLUtils.XmlToJson(context.outputDocument.getDocumentElement(), true, useType, jsonRoot);
				str = str.replaceAll("\n( +)", "\n$1$1");
			} catch (JSONException e) {
				str = e.getMessage();
			}
			input = inputJSON;
		} else {
			str = XMLUtils.prettyPrintDOMWithEncoding(context.outputDocument);
			input = inputXML;
		}
		
		input.fileWrite(str);
		
		if (editor.getEditorInput() != input) {
			SequenceEditorInput i = input;
			getDisplay().syncExec(() -> {
				editor.setInput(i);
			});
		}
		
		synchronized(debugDatabaseObject) {
			debugDatabaseObject = (DatabaseObject) source;
		}

		try {
			ConvertigoPlugin.getDefault().debugConsoleStream.write("Step reached before executing database object: "+ debugDatabaseObject.getName() +"\n");
		} catch (IOException e1) {}

		try {
			synchronized(debugDatabaseObject) {
				debugDatabaseObject.wait();
			}
			getDisplay().syncExec(() -> {
				toolItemStep.setEnabled(true);
			});
		}
		catch(InterruptedException e) {
			try {
				ConvertigoPlugin.getDefault().debugConsoleStream.write("Next step required\n");
			} catch (IOException ex) {}
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
	
	private SequenceEditorInput getInput() {
		return (SequenceEditorInput) editor.getEditorInput();
	}
}
