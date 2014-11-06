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

package com.twinsoft.convertigo.eclipse.views.sourcepicker;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dnd.StepSourceTransfer;
import com.twinsoft.convertigo.eclipse.property_editors.StepSourceXpathEvaluatorComposite;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.StepSourceEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.StepSourceListener;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

public class SourcePickerView extends ViewPart implements StepSourceListener {

	private SourcePickerHelper sourcePicker = null;
	private Composite content;
	private SashForm mainSashForm, treesSashForm, xpathSashForm;
	private DatabaseObject selectedDbo = null;
	
	private final String show_step_source 		= "   Show step's source   ";
	private final String show_variable_source 	= "Show variable's source";
	private final String remove_source 			= "Remove source";
	
	public SourcePickerView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		sourcePicker = new SourcePickerHelper();
		GridLayout gl = new GridLayout(3,false);
		content = new Composite(parent, SWT.NONE);
		content.setLayout(gl);
		createHelpContent();
		createSashForm();
	}
	
	private void createSashForm() {
		GridData gd = new org.eclipse.swt.layout.GridData();
		gd.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		gd.horizontalSpan = 3;
		mainSashForm = new SashForm(content, SWT.NONE);
		mainSashForm.setOrientation(SWT.VERTICAL );
		mainSashForm.setLayoutData(gd);
		createTreeSashForm();
		createXpathSashForm();
	}
	
	private void createTreeSashForm() {
		GridData gd = new org.eclipse.swt.layout.GridData();
		gd.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		treesSashForm = new SashForm(mainSashForm, SWT.NONE);
		treesSashForm.setOrientation(SWT.HORIZONTAL );
		treesSashForm.setLayoutData(gd);
		createXhtmlTree();
	}
	
	private void createXpathSashForm() {
		GridData gd = new org.eclipse.swt.layout.GridData();
		gd.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		xpathSashForm = new SashForm(mainSashForm, SWT.NONE);
		xpathSashForm.setOrientation(SWT.HORIZONTAL );
		xpathSashForm.setLayoutData(gd);
		createXPathEvaluator();
	}
	
	private Text stepTag, stepType, stepName, stepComment;
	private Button showBtn, remBtn;
	
	private void createHelpContent() {
		// Tag
		Label tagLabel = new Label (content, SWT.NONE);
		tagLabel.setText("Tag :");
		
		GridData stepTagData = new GridData();
		stepTagData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		stepTag = new Text(content, SWT.NONE);
		stepTag.setText("");
		stepTag.setEnabled(false);
		stepTag.setLayoutData(stepTagData);

		// Help
		GridData helpTextData = new GridData();
		helpTextData.verticalSpan = 5;
		helpTextData.grabExcessHorizontalSpace = true;
		helpTextData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		Text helpText = new Text(content, SWT.BORDER|SWT.MULTI);
		helpText.setEditable(false);
		helpText.setText("Note :\nDrag items to a step \nin the Projects view \nto link the source.");
		helpText.setLayoutData(helpTextData);

		// Type
		Label typeLabel = new Label(content, SWT.NONE);
		typeLabel.setText("Type :");
		
		GridData stepTypeData = new GridData();
		stepTypeData.minimumWidth = 250;
		stepTypeData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		stepType = new Text(content, SWT.NONE);
		stepType.setText("");
		stepType.setEnabled(false);
		stepType.setLayoutData(stepTypeData);

		// Name
		Label nameLabel = new Label(content, SWT.NONE);
		nameLabel.setText("Name :");
		
		GridData stepNameData = new GridData();
		stepNameData.minimumWidth = 250;
		stepNameData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		stepName = new Text(content, SWT.NONE);
		stepName.setText("");
		stepName.setEnabled(false);
		stepName.setLayoutData(stepNameData);

		// Comment
		GridData commentLabelData = new GridData();
		commentLabelData.verticalAlignment = org.eclipse.swt.layout.GridData.BEGINNING;
		Label commentLabel = new Label(content, SWT.NONE);
		commentLabel.setText("Comment :");
		commentLabel.setLayoutData(commentLabelData);
		
		GridData stepCommentData = new GridData();
		stepCommentData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		stepComment = new Text(content, SWT.MULTI|SWT.WRAP);
		stepComment.setText("\n\n");
		stepComment.setEnabled(false);
		stepComment.setLayoutData(stepCommentData);
		
		// Buttons
		FillLayout fl = new FillLayout();
		fl.spacing = 10;
		GridData data4 = new GridData();
		data4.horizontalSpan=2;
		data4.verticalAlignment = org.eclipse.swt.layout.GridData.BEGINNING;
		data4.horizontalAlignment = org.eclipse.swt.layout.GridData.END;
		Composite cbtns = new Composite(content, SWT.NONE);
		cbtns.setLayout(fl);
		cbtns.setLayoutData(data4);
		
		showBtn = new Button(cbtns, SWT.NONE | SWT.TOGGLE);
		showBtn.setText(show_step_source);
		showBtn.setEnabled(false);
		showBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				remBtn.setEnabled(!showBtn.getSelection());
				showStep(selectedDbo, showBtn.getSelection());
			}
		});
		
		
		remBtn = new Button(cbtns, SWT.NONE);
		remBtn.setText(remove_source);
		remBtn.setEnabled(false);
		remBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				removeSource();
			}
		});
	}
	
	private void createXhtmlTree() {
		sourcePicker.createXhtmlTree(treesSashForm);
		
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
		
	}
	
	private void createXPathEvaluator() {
		sourcePicker.createXPathEvaluator(new StepSourceXpathEvaluatorComposite(xpathSashForm, SWT.NONE, sourcePicker));
	}
	
	@Override
	public void setFocus() {

	}
	
	public void sourceSelected(StepSourceEvent stepSourceEvent) {
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
		String tag = "", type = "", name = "", comment = "";
		String textBtn = show_step_source;
		boolean enableBtn = false;
		if (selectedDbo != null) {
			if (selectedDbo instanceof Step)
				tag = ((Step)selectedDbo).getStepNodeName();
			type = selectedDbo.getClass().getSimpleName();
			name = selectedDbo.getName();
			comment = selectedDbo.getComment();
			textBtn = (selectedDbo instanceof Step) ? show_step_source:show_variable_source;
			if (selectedDbo instanceof IStepSourceContainer)
				enableBtn = !((IStepSourceContainer)selectedDbo).getSourceDefinition().isEmpty();
		}
		
		stepTag.setText(tag);
		stepType.setText(type);
		stepName.setText(name);
		stepComment.setText(comment);
		showBtn.setText(textBtn);
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
				Long key = new Long(sourceDefinition.firstElement());
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
	
	public void close() {
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
}
