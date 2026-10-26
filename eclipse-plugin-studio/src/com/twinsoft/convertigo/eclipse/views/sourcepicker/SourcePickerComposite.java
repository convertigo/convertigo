/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.views.sourcepicker;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.ExperimentalFeatures;
import com.twinsoft.convertigo.eclipse.dnd.StepSourceTransfer;
import com.twinsoft.convertigo.eclipse.property_editors.StepSourceXpathEvaluatorComposite;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.eclipse.views.jsonpicker.JsonTree;
import com.twinsoft.convertigo.eclipse.views.jsonpicker.NativeJson;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.StepSourceEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.json.JsonPath;
import com.twinsoft.convertigo.engine.json.NativeStepOutput;

class SourcePickerComposite extends Composite {

	private SourcePickerHelper sourcePicker = null;
	private DatabaseObject selectedDbo = null;
	
	private final String show_step_source 		= "Show step's source";
	private final String show_variable_source 	= "Show variable's source";
	private final String remove_source 			= "Remove source";
	
	SourcePickerComposite(Composite parent, int style) {
		super(parent, style);
		sourcePicker = new SourcePickerHelper();
		setLayout(new GridLayout(1, true));
		createHelpContent();
		createSashForm();
		StyledText xp = sourcePicker.getXpathEvaluator().getXpath();
		xp.setToolTipText(xp.getToolTipText() + "\nYou can drag the xPath edit zone on a project tree Step");
		sourcePicker.getXpathEvaluator().getLabel().setToolTipText("Drag me on a project tree Step");
	}
	
	private void createSashForm() {
		SashForm mainSashForm = new SashForm(this, SWT.VERTICAL);
		mainSashForm.setLayoutData(new GridData(GridData.FILL_BOTH));

		// the XML of the step; for a step of a JSON native sequence that gives JSON, its JSON too
		tabs = new CTabFolder(mainSashForm, SWT.BOTTOM | SWT.FLAT);
		Composite xmlComposite = new Composite(tabs, SWT.NONE);
		xmlComposite.setLayout(new GridLayout(1, false));
		sourcePicker.createXhtmlTree(xmlComposite);
		xmlItem = new CTabItem(tabs, SWT.NONE);
		xmlItem.setText("XML");
		xmlItem.setControl(xmlComposite);
		createJsonTab();
		
		// DND support
		int ops = DND.DROP_COPY | DND.DROP_MOVE;
		Transfer[] transfers = new Transfer[] {StepSourceTransfer.getInstance()};
		
		DragSource source = new DragSource(sourcePicker.getTwsDomTree().getTree(), ops);
		source.setTransfer(transfers);
		source.addDragListener(new DragSourceAdapter() {
			@Override
			public void dragStart(DragSourceEvent event) {
				event.doit = true;
				StepSourceTransfer.getInstance().setStepSource(sourcePicker.getDragData());
			}
		});
		Composite composite = new Composite(mainSashForm, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		Label label = dropLabel = new Label(composite, SWT.NONE);
		label.setText(DROP_XML);
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
		sourcePicker.createXPathEvaluator(new StepSourceXpathEvaluatorComposite(composite, SWT.NONE, sourcePicker));
		sourcePicker.getXpathEvaluator().setLayoutData(new GridData(GridData.FILL_BOTH));
		showJsonTab(false);
		
		mainSashForm.setWeights(70,30);
	}
	
	private ToolItem tiLink, showBtn, remBtn;

	private static final String DROP_XML = "Drag nodes or 'xPath' and drop on Steps";
	private static final String DROP_JSON = "Drag JSON values or the JSON path ($...) and drop on Steps";

	private CTabFolder tabs;
	private CTabItem xmlItem;
	private CTabItem jsonItem;
	private Composite jsonComposite;
	private Label jsonInfo;
	private JsonTree jsonTree;
	private Label dropLabel;
	private Step jsonStep;
	private boolean jsonUpdating = false;
	/** the JSON path source shown by "Show step's source", kept when the projects tree then selects its step */
	private DatabaseObject pendingDbo;
	private String pendingJsonPath;

	/** the JSON tab: the last JSON of the step in the Studio, a value picked there gives a JSON path source */
	private void createJsonTab() {
		jsonComposite = new Composite(tabs, SWT.NONE);
		jsonComposite.setLayout(new GridLayout(1, false));
		jsonInfo = new Label(jsonComposite, SWT.WRAP);
		jsonInfo.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		jsonTree = new JsonTree(jsonComposite, SWT.NONE);
		jsonTree.setLayoutData(new GridData(GridData.FILL_BOTH));
		jsonTree.setSelectionListener(path -> {
			if (!jsonUpdating && jsonStep != null) {
				sourcePicker.getXpathEvaluator().getXpath().setText(JsonTree.toJsonPath(null, path));
			}
		});

		DragSource source = new DragSource(jsonTree.getTree(), DND.DROP_COPY | DND.DROP_MOVE);
		source.setTransfer(new Transfer[] { StepSourceTransfer.getInstance() });
		source.addDragListener(new DragSourceAdapter() {
			@Override
			public void dragStart(DragSourceEvent event) {
				event.doit = jsonStep != null;
				StepSourceTransfer.getInstance().setStepSource(sourcePicker.getDragData());
			}
		});
	}

	private void showJsonTab(boolean show) {
		if (show && jsonItem == null) {
			jsonItem = new CTabItem(tabs, SWT.NONE, 0);
			jsonItem.setText("JSON");
			jsonItem.setControl(jsonComposite);
		} else if (!show && jsonItem != null) {
			jsonItem.dispose();
			jsonItem = null;
			jsonComposite.setVisible(false);
		}
		// a sequence of XML steps: the view as it was, without tabs
		tabs.setTabHeight(show ? -1 : 0);
		tabs.setSelection(show ? jsonItem : xmlItem);
		dropLabel.setText(show ? DROP_JSON : DROP_XML);
		dropLabel.getParent().layout(true);
		tabs.layout(true, true);
	}

	/** shows the JSON of a step of a JSON native sequence that gives JSON to the next steps, with a JSON path selected */
	private void updateJson(DatabaseObject dbo, String xpath) {
		Step step = dbo instanceof Step s ? s : null;
		boolean show = step != null && ExperimentalFeatures.isShown() && NativeJson.isJsonProducer(step) && NativeJson.isNativeSequence(step.getSequence());
		jsonStep = show ? step : null;
		showJsonTab(show);
		if (!show) {
			return;
		}
		NativeStepOutput sample = NativeJson.getSample(step);
		jsonUpdating = true;
		try {
			if (sample == null || sample.getJson().isEmpty()) {
				jsonTree.setJson("", null);
				jsonInfo.setText(sample == null
						? "No JSON yet for '" + step.getName() + "': execute the sequence once in the Studio to pick a JSON path in it."
						: "'" + step.getName() + "' writes its rows in stream into the response: they are in the JSON output of the sequence.");
			} else {
				jsonTree.setJson("$", sample.getJson());
				jsonInfo.setText("JSON of '" + step.getName() + "' in '" + NativeJson.getVariableName(step) + "' at its last execution"
						+ (sample.isTruncated() ? " (cut at " + NativeStepOutput.MAX_LENGTH + " characters)" : "")
						+ ". Pick a value: its JSON path ($...) is the source.");
				if (JsonPath.isJsonPath(xpath)) {
					try {
						jsonTree.select(JsonPath.parse(xpath).getSegments());
					} catch (IllegalArgumentException e) {
						// a broken path: nothing selected
					}
				}
			}
		} finally {
			jsonUpdating = false;
		}
		jsonComposite.layout(true, true);
	}
	
	private void createHelpContent() {
		ToolBar tb = new ToolBar(this, SWT.NONE);
		tb.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		tiLink = new ToolItem(tb, SWT.CHECK);		
		new ToolItem(tb, SWT.SEPARATOR);
		
		showBtn = new ToolItem(tb, SWT.NONE);
		showBtn.setEnabled(false);
		SwtUtils.setToolItemIcon(showBtn, "icons/studio/find.svg", "Source", show_step_source);
		showBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				remBtn.setEnabled(!showBtn.getSelection());
				showStep(selectedDbo, true);
			}
		});
		
		remBtn = new ToolItem(tb, SWT.PUSH);
		remBtn.setEnabled(false);
		SwtUtils.setToolItemIcon(remBtn, "icons/studio/delete.svg", "Remove", remove_source);
		remBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				removeSource();
			}
		});
		
		for (ToolItem ti: tb.getItems()) {
			ti.setData("style", "background: unset");
		}
	}
	
	void sourceSelected(StepSourceEvent stepSourceEvent) {
		sourcePicker.getTwsDomTree().removeAll();
		sourcePicker.getXpathEvaluator().removeAnchor();
		sourcePicker.getXpathEvaluator().getXpath().setText("");
		
		DatabaseObject dbo = (DatabaseObject)stepSourceEvent.getSource();
		String xpath  = stepSourceEvent.getXPath();
		if (dbo != null && pendingDbo != null && dbo.priority == pendingDbo.priority && pendingJsonPath != null) {
			xpath = pendingJsonPath;
		}
		pendingDbo = null;
		pendingJsonPath = null;
		String priority = ""+ dbo.priority;
		XMLVector<String> stepSourceDefinition = new XMLVector<String>();
		stepSourceDefinition.add(priority);
		stepSourceDefinition.add(xpath);
		sourcePicker.setStepSourceDefinition(stepSourceDefinition);
		selectedDbo = dbo;
		fillHelpContent();
		sourcePicker.displayTargetWsdlDom(dbo);
		updateJson(dbo, xpath);
	}

	private void fillHelpContent() {
		String textBtn = show_step_source;
		boolean enableBtn = false;
		if (selectedDbo != null) {
			textBtn = (selectedDbo instanceof Step) ? show_step_source:show_variable_source;
			if (selectedDbo instanceof IStepSourceContainer)
				enableBtn = !((IStepSourceContainer)selectedDbo).getSourceDefinition().isEmpty();
		}
		showBtn.setToolTipText(textBtn);
		showBtn.setEnabled(enableBtn);
		showBtn.setSelection(false);
		remBtn.setEnabled(enableBtn);
	}
	
	private void showStep(DatabaseObject dbo, boolean showSource) {
		if (selectedDbo == null) return;
		
		String priority, xpath;
		DatabaseObject dboToShow = null;
		if (showSource) {
			XMLVector<String> sourceDefinition = ((IStepSourceContainer) dbo).getSourceDefinition();
			if (!sourceDefinition.isEmpty()) {
				Long key = Long.valueOf(sourceDefinition.firstElement());
				priority = ""+key;
				xpath = sourceDefinition.lastElement();
				Step step = dbo instanceof Step ? (Step)dbo:(Step)dbo.getParent();
				dboToShow = step.getSequence().loadedSteps.get(key);
				if (dboToShow == null) {
					ConvertigoPlugin.infoMessageBox("Source is not valid!");
					return;
				}
			}
			else {
				ConvertigoPlugin.infoMessageBox("Source is empty!");
				return;
			}
		}
		else {
			dboToShow = dbo;
			priority = ""+ dboToShow.priority;
			xpath  = ".";
		}
		
		sourcePicker.getTwsDomTree().removeAll();
		sourcePicker.getXpathEvaluator().removeAnchor();
		sourcePicker.getXpathEvaluator().getXpath().setText("");
		
		XMLVector<String> stepSourceDefinition = new XMLVector<String>();
		stepSourceDefinition.add(priority);
		stepSourceDefinition.add(xpath);
		sourcePicker.setStepSourceDefinition(stepSourceDefinition);
		
		sourcePicker.displayTargetWsdlDom(dboToShow);
		updateJson(dboToShow, xpath);
		if (JsonPath.isJsonPath(xpath)) {
			pendingDbo = dboToShow;
			pendingJsonPath = xpath;
		}
		
		try {
			TreeObject treeObject = ConvertigoPlugin.getDefault().getProjectExplorerView().findTreeObjectByUserObject(dboToShow);
			ConvertigoPlugin.getDefault().getProjectExplorerView().setSelectedTreeObject(treeObject);
		}
		catch (Throwable t) {}
	}
	
	private void removeSource() {
		if (selectedDbo != null) {
			if (selectedDbo instanceof IStepSourceContainer) {
				try {
					DatabaseObjectTreeObject dboTreeObject = (DatabaseObjectTreeObject)ConvertigoPlugin.getDefault().getProjectExplorerView().findTreeObjectByUserObject(selectedDbo);
					ConvertigoPlugin.getDefault().getProjectExplorerView().setSelectedTreeObject(dboTreeObject);
					dboTreeObject.setPropertyValue("sourceDefinition", new XMLVector<String>());
					showBtn.setEnabled(false);
					showBtn.setSelection(false);
					remBtn.setEnabled(false);
				}
				catch (Throwable t) {}
			}
		}
	}
	
	void close() {
		selectedDbo = null;
		updateJson(null, null);
		sourcePicker.setStepSourceDefinition(null);
		fillHelpContent();
		sourcePicker.getTwsDomTree().removeAll();
		sourcePicker.getXpathEvaluator().removeAnchor();
		sourcePicker.getXpathEvaluator().getXpath().setText("");
	}

	public Object getObject() {
		return selectedDbo;
	}
	
	public ToolItem getTiLink() {
		return tiLink;
	}
}
