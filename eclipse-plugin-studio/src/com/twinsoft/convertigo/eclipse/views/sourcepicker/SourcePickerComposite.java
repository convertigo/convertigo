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

package com.twinsoft.convertigo.eclipse.views.sourcepicker;

import org.eclipse.swt.SWT;
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
import com.twinsoft.convertigo.eclipse.dnd.StepSourceTransfer;
import com.twinsoft.convertigo.eclipse.property_editors.StepSourceXpathEvaluatorComposite;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.StepSourceEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

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
		sourcePicker.createXhtmlTree(mainSashForm);
		
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
		Label label = new Label(composite, SWT.NONE);
		label.setText("Drag nodes or 'xPath' and drop on Steps");
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
		sourcePicker.createXPathEvaluator(new StepSourceXpathEvaluatorComposite(composite, SWT.NONE, sourcePicker));
		sourcePicker.getXpathEvaluator().setLayoutData(new GridData(GridData.FILL_BOTH));
		
		mainSashForm.setWeights(70,30);
	}
	
	private ToolItem tiLink, showBtn, remBtn;
	
	private void createHelpContent() {
		ToolBar tb = new ToolBar(this, SWT.NONE);
		tb.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		tiLink = new ToolItem(tb, SWT.CHECK);		
		new ToolItem(tb, SWT.SEPARATOR);
		
		showBtn = new ToolItem(tb, SWT.NONE);
		showBtn.setEnabled(false);
		SwtUtils.setToolItemIcon(showBtn, "icons/studio/find.gif", "Source", show_step_source);
		showBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				remBtn.setEnabled(!showBtn.getSelection());
				showStep(selectedDbo, true);
			}
		});
		
		remBtn = new ToolItem(tb, SWT.PUSH);
		remBtn.setEnabled(false);
		SwtUtils.setToolItemIcon(remBtn, "icons/studio/delete.gif", "Remove", remove_source);
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
		String priority = ""+ dbo.priority;
		XMLVector<String> stepSourceDefinition = new XMLVector<String>();
		stepSourceDefinition.add(priority);
		stepSourceDefinition.add(xpath);
		sourcePicker.setStepSourceDefinition(stepSourceDefinition);
		selectedDbo = dbo;
		fillHelpContent();
		sourcePicker.displayTargetWsdlDom(dbo);
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
