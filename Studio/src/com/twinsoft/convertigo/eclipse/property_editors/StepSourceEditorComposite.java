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

package com.twinsoft.convertigo.eclipse.property_editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.eclipse.views.sourcepicker.SourcePickerHelper;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class StepSourceEditorComposite extends AbstractDialogComposite {

	private SourcePickerHelper sourcePicker = null;
	private SashForm sashForm = null;
	private SashForm treesSashForm = null;
	private SashForm xpathSashForm = null;
	private TreeItem lastSelectedItem = null;
	private TreeItem lastSelectableItem = null;
	private Tree tree = null;
	private Step step = null;
	private Button buttonNew = null;
	private Button buttonRemove = null;
	private Label noPreviousLabel = null;

	private static boolean stepFound;
	private static TreeItem stepItem = null;
	
	private Step sourceStep = null;
	private String sourceXpath = null;
	private boolean sourceChanged = false;
	
	public StepSourceEditorComposite(Composite parent, int style, AbstractDialogCellEditor cellEditor) {
		super(parent, style, cellEditor);
		
		sourcePicker = new SourcePickerHelper() {
			@Override
			protected void clean() {
				super.clean();
				enableOK(false);
			}
			
			@Override
			protected String onDisplayXhtml(String xpath) {
				if (lastSelectedItem == null) {
					sourceStep = step;
					sourceXpath = xpath;
				}
				else {
					sourceChanged = true;
					if (sourceStep == null)
						sourceStep = step;
					if (sourceXpath == null)
						sourceXpath = xpath;
				}

				xpath = ((!sourceChanged || (step.priority == sourceStep.priority)) ? sourceXpath:".");
				enableOK(true);
				return xpath;
			}
		};
		
		if (cellEditor.databaseObjectTreeObject == null) {
			Composite parentEditorComposite = cellEditor.getControl().getParent();
			while (!(parentEditorComposite instanceof TableEditorComposite)) {
				parentEditorComposite = parentEditorComposite.getParent();
			}
			step = (Step)((TableEditorComposite)parentEditorComposite).cellEditor.databaseObjectTreeObject.getObject();
		}
		else {
			Object object = cellEditor.databaseObjectTreeObject.getObject();
			if (object instanceof Step)
				step = (Step)object;
			else // Variable
				step = (Step)((Variable)object).getParent();
		}
		
		sourcePicker.setStepSourceDefinition(GenericUtils.<XMLVector<String>>cast(GenericUtils.clone(cellEditor.getEditorData())));
				
		initialize();
	}

	private void initialize() {
		GridLayout gridLayout = new GridLayout ();
		gridLayout.numColumns = 8;
		this.setLayout (gridLayout);

		createButtons();
		createSashForm();
		selectResult();
		
		if (lastSelectableItem != null) {
			noPreviousLabel.setVisible(false);
		}
	}

	private void addNewSource() {
		XMLVector<String> stepSourceDefinition = new XMLVector<String>();
		stepSourceDefinition.add("0");
		stepSourceDefinition.add(".");
		sourcePicker.setStepSourceDefinition(stepSourceDefinition);
	}
	
	private void setSourcePriority(long priority) {
		sourcePicker.getStepSourceDefinition().setElementAt(""+priority, 0);
	}
	
	public Object getValue() {
		return sourcePicker.getStepSourceDefinition();
	}
	
	private void createButtons() {
		int nbResults = sourcePicker.getStepSourceDefinition().size();
		
		buttonNew = new Button(this, SWT.PUSH);
		buttonNew.setText("New Source");
		buttonNew.setEnabled(nbResults == 0);
		buttonNew.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				createSource();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		buttonRemove = new Button(this, SWT.PUSH);
		buttonRemove.setText("Remove Source");
		buttonRemove.setEnabled(nbResults > 0);
		buttonRemove.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				removeSource();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		noPreviousLabel = new Label(this, SWT.NONE);
		noPreviousLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		noPreviousLabel.setText("no previous step is available for source selection");
		
		GridData gd = new GridData ();
		gd.horizontalSpan = 6;
		noPreviousLabel.setLayoutData(gd);
	}
	
	private void selectResult() {
		lastSelectedItem = null;
		XMLVector<String> stepSourceDefinition = sourcePicker.getStepSourceDefinition();
		if (sourcePicker.getStepSourceDefinition().size() > 0) {
			tree.setEnabled(true);
			long priority = Long.parseLong(stepSourceDefinition.get(0), 10);
			TreeItem tItem = null;

			try {
				if (priority == 0) {
					tItem = lastSelectableItem != null ? lastSelectableItem : findParentStepInTree();
				} else {
					tItem = findStepInTree(null, priority);
				}
			} catch (Exception e) {}
			
			if (tItem != null) {
				tree.setSelection(tItem);
				Event event = new Event();
				event.item = tItem;
				tree.notifyListeners(SWT.Selection, event);
				tree.setFocus();
			} else {
				buttonRemove.setText("Remove Broken Source!");
				layout(true);
				tree.setEnabled(false);
			}
		}
	}
	
	private TreeItem findParentStepInTree() {
		try {
			long priority = ((Step)step.getParent()).priority;
			return findStepInTree(null,priority);
		} catch (Exception e) {
			if (lastSelectableItem.getForeground().equals(Display.getCurrent().getSystemColor(SWT.COLOR_RED))) {
				return null;
			}
			return lastSelectableItem;
		}
	}
	
	private void createSource() {
		addNewSource();
		tree.deselectAll();
		sourcePicker.getTwsDomTree().getTree().removeAll();
		buttonNew.setEnabled(sourcePicker.getStepSourceDefinition().size() == 0);
		buttonRemove.setEnabled(true);
		selectResult();
	}
	
	private void removeSource() {
		lastSelectedItem = null;
		tree.deselectAll();
		tree.setEnabled(false);
		sourcePicker.getTwsDomTree().getTree().removeAll();
		
		sourcePicker.getXpathEvaluator().removeAnchor();
		sourcePicker.setStepSourceDefinition(new XMLVector<String>());

		buttonRemove.setEnabled(false);
		buttonNew.setEnabled(true);
		buttonRemove.setText("Remove Source");
		
		enableOK(true);
	}
	
	private void createSashForm() {
		GridData gd = new org.eclipse.swt.layout.GridData();
		gd.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		gd.horizontalSpan = 8;
		sashForm = new SashForm(this, SWT.NONE);
		sashForm.setOrientation(SWT.VERTICAL );
		sashForm.setLayoutData(gd);
		createTreesSashForm();
		createXpathSashForm();
		sashForm.setWeights(new int[]{70,30});
	}
	
	private void createTreesSashForm() {
		GridData gd = new org.eclipse.swt.layout.GridData();
		gd.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		treesSashForm = new SashForm(sashForm, SWT.NONE);
		treesSashForm.setOrientation(SWT.HORIZONTAL );
		treesSashForm.setLayoutData(gd);
		createSequenceTree();
		sourcePicker.createXhtmlTree(treesSashForm);
		treesSashForm.setWeights(new int[]{40,60});
	}
	
	private void createXpathSashForm() {
		GridData gd = new org.eclipse.swt.layout.GridData();
		gd.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		xpathSashForm = new SashForm(sashForm, SWT.NONE);
		xpathSashForm.setOrientation(SWT.HORIZONTAL );
		xpathSashForm.setLayoutData(gd);
		
		sourcePicker.createXPathEvaluator(new StepXpathEvaluatorComposite(xpathSashForm, SWT.NONE, sourcePicker));
	}
	
	private void createSequenceTree() {
		GridData gd = new GridData ();
		gd.horizontalAlignment = GridData.FILL;
		gd.verticalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;

		tree = new Tree(treesSashForm, SWT.BORDER);
		tree.setLayoutData (gd);
		tree.setEnabled(sourcePicker.getStepSourceDefinition().size()>0);
		tree.addSelectionListener(
			new org.eclipse.swt.events.SelectionListener() {
				public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
					TreeItem tItem = (TreeItem)e.item;
					if (tItem.getForeground().equals(Display.getCurrent().getSystemColor(SWT.COLOR_RED))) {
						if (lastSelectedItem != null) tree.setSelection(lastSelectedItem);
						return;
					}
					if ((lastSelectedItem == null) || ((lastSelectedItem != null) && !lastSelectedItem.equals(tItem))) {
						DatabaseObject databaseObject = (DatabaseObject)tItem.getData();
						if (databaseObject instanceof Step) {
							setSourcePriority(databaseObject.priority);
							sourcePicker.displayTargetWsdlDom(databaseObject);
						}
					}
					lastSelectedItem = tItem;
				}
				public void widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent e) {
				}
			}
		);
		
		addStepsInTree(tree, step.getParentSequence());
		disableStepsInTree();
		
		if (lastSelectableItem != null) {
			tree.showItem(lastSelectableItem);
		}
	}
	
	private void addStepsInTree(Object parent, DatabaseObject databaseObject) {
		TreeItem tItem;
		
		if (parent instanceof Tree) {
			stepFound = false;
			tItem = new TreeItem((Tree)parent, SWT.NONE);
			tItem.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		}
		else {
			tItem = new TreeItem((TreeItem)parent,  SWT.NONE);
			
			if (databaseObject instanceof Step) {
				Step step = (Step) databaseObject;
				
				if (!step.isPickable()) {
					tItem.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
				}
			}
		}

		// associate our object with the tree Item.
		tItem.setData(databaseObject);
		
		if (databaseObject.priority == step.priority) {
			tItem.setText("* " + databaseObject.toString());
			tree.showItem(tItem);
			stepFound = true;
			stepItem = tItem;
		} else {
			tItem.setText(databaseObject.toString());
		}
		
		if (!stepFound && !(tItem.getForeground().equals(Display.getCurrent().getSystemColor(SWT.COLOR_RED)))) {
			lastSelectableItem = tItem;
		}
		
		// disable all steps after founded one
		if (stepFound) {
			tItem.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		}
		
		// now recurse on children steps
		if (databaseObject instanceof StepWithExpressions) {
			for (Step step: ((StepWithExpressions) databaseObject).getSteps()) {
				addStepsInTree(tItem, step);			
			}
		} else if (databaseObject instanceof Sequence) {
			for (Step step: ((Sequence) databaseObject).getSteps()) {
				addStepsInTree(tItem, step);			
			}
		}
	}
	
	private void disableStepsInTree() {
		if (stepItem != null) {
			// disable parents 'XML' steps if needed
			if (step.isXml()) {
				TreeItem tItem = stepItem;
				while (tItem.getParentItem().getData() instanceof Step) {
					tItem = tItem.getParentItem();
					if (((Step) tItem.getData()).isXml()) {
						tItem.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
					}
				}
			}
		}
	}
	
	private TreeItem findStepInTree(Object parent, long priority) {
		TreeItem[] items = null;
		
		if (parent == null) {
			items = tree.getItems();
		}
		else {
			DatabaseObject databaseObject = (DatabaseObject)((TreeItem)parent).getData();
			if (databaseObject instanceof Step) {
				if ((databaseObject).priority == priority) {
					return (TreeItem)parent;
				}
			}
			items = ((TreeItem)parent).getItems();
		}
		
		for (int i=0; i<items.length; i++) {
			TreeItem tItem = findStepInTree(items[i],priority);
			if (tItem != null) return tItem;
		}
		return null;
	}
	
	private void enableOK(boolean enabled) {
		if (parentDialog != null) {
			((EditorFrameworkDialog)parentDialog).enableOK(enabled);
		}
	}
}
