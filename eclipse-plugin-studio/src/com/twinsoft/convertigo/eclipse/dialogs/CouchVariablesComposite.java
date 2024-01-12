/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.dialogs;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.beans.transactions.couchdb.AbstractCouchDbTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.CouchVariable;
import com.twinsoft.convertigo.beans.transactions.couchdb.ICouchParametersExtra;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.engine.enums.CouchExtraVariable;
import com.twinsoft.convertigo.engine.enums.CouchParam;

public class CouchVariablesComposite extends ScrolledComposite {
	private Group groupData = null, groupParameters = null, groupQueries = null;
	
	private Composite globalComposite = null;
	
	private List<String> parametersCouch = null;
	private List<RequestableVariable> allVariables = null;
	private List<CouchVariable> selectedVariable = null;
		
	public CouchVariablesComposite(Composite parent, int style, List<RequestableVariable> allVariables) {
		this(parent, style);
		this.allVariables = allVariables;
	}
	
	public CouchVariablesComposite(Composite parent, int style) {
		super(parent, style);
		parametersCouch = new ArrayList<String>();
		selectedVariable = new ArrayList<CouchVariable>();
		setExpandHorizontal(true);
	}

	public void setPropertyDescriptor(AbstractCouchDbTransaction couchDbTransaction, PropertyDescriptor[] propertyDescriptors, DatabaseObject parentObject) {	
		cleanGroups();
		
		/* Fill the Group widget */
		for (PropertyDescriptor property : propertyDescriptors) {
			String name = property.getName();
			String description = property.getShortDescription();
			if (!parametersCouch.contains(name)) {
				if ( (name.startsWith("q_") || name.startsWith("p_")) ) {
					if (!parentObject.getClass().getCanonicalName().equals(property.getValue(MySimpleBeanInfo.BLACK_LIST_PARENT_CLASS))) {
						Group choosenGroup = name.startsWith("q_") ? groupQueries : groupParameters;
						description = description.replaceFirst("\\|", "<br/>\n");
						addToComposite(choosenGroup, name, description, false);
					}
				} 
			}
		}
		
		Collection<CouchExtraVariable> extraVariables = null;
		if (couchDbTransaction instanceof ICouchParametersExtra){
			extraVariables = ((ICouchParametersExtra) couchDbTransaction).getCouchParametersExtra();
		}
		
		if (extraVariables != null ) {
			for (CouchExtraVariable extraVariable : extraVariables) {
				String name = extraVariable.getVariableName();
				Group group = name.startsWith("q_") ? groupQueries : groupData;
				addToComposite(group, name, extraVariable.getVariableDescription(), extraVariable.isMultiValued());
			}
		}
		
		if (groupQueries.getChildren().length == 1) {
			groupQueries.dispose();
		}
		
		if (groupParameters.getChildren().length == 1) {
			groupParameters.dispose();
		}
		
		if (groupData.getChildren().length == 1) {
			groupData.dispose();
		}
		
		setContent(globalComposite);
		globalComposite.pack(true);
		
		// Fix text no displayed in C8oBrowser
		LinkedList<Control> controls = new LinkedList<>();
		controls.add(globalComposite);
		Control c;
		while ((c = controls.pollFirst()) != null) {
			if (c instanceof C8oBrowser) {
				((C8oBrowser) c).reloadText();
			} else if (c instanceof Composite) {
				controls.addAll(Arrays.asList(((Composite) c).getChildren()));
			}
		}
	}
	
	private void cleanGroups(){	
		final Composite container = this;
		
		globalComposite = new Composite(this, SWT.NONE);
		GridLayout layout = new GridLayout(1, true);
		layout.verticalSpacing = 0;
		layout.marginBottom = 0;
		globalComposite.setLayout(layout);
		
		layout = new GridLayout(3, false);
		layout.horizontalSpacing = 30;	
		
		/* Parameters */		
		if ((groupParameters != null) && (!groupParameters.isDisposed())) {
			groupParameters.dispose();
		}
		
		groupParameters = new Group(globalComposite, SWT.SHADOW_ETCHED_OUT | SWT.V_SCROLL);
		groupParameters.setLayout(layout);
		groupParameters.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		Label label = new Label(groupParameters, SWT.NONE);
		label.setText("Parameters");
		GridData gdLabel = new GridData();
		gdLabel.horizontalSpan = 3;
		label.setLayoutData(gdLabel);
		
		layout = new GridLayout(3, false);
		layout.horizontalSpacing = 30;	
		
		/* Queries */
		if ((groupQueries != null) && (!groupQueries.isDisposed())) {
			groupQueries.dispose();
		}
		
		groupQueries = new Group(globalComposite, SWT.SHADOW_ETCHED_OUT | SWT.V_SCROLL);
		groupQueries.setLayout(layout);
		groupQueries.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		label = new Label(groupQueries, SWT.NONE);
		label.setText("Queries");
		gdLabel = new GridData();
		gdLabel.horizontalSpan = 3;
		label.setLayoutData(gdLabel);
		
		layout = new GridLayout(3, false);
		layout.horizontalSpacing = 30;	
		
		/* Extra variables */
		if ((groupData != null) && (!groupData.isDisposed())) {
			groupData.dispose();
		}
		
		groupData = new Group(globalComposite, SWT.SHADOW_ETCHED_OUT | SWT.V_SCROLL);
		groupData.setLayout(layout);
		groupData.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		label = new Label(groupData, SWT.NONE);
		label.setText("Data");
		gdLabel = new GridData();
		gdLabel.horizontalSpan = 3;
		label.setLayoutData(gdLabel);
		
		addListener(SWT.Resize, new Listener() {
			@Override
			public void handleEvent(Event event) {
				globalComposite.setSize(container.getSize().x, globalComposite.getSize().y);
			}
		});
		
		parametersCouch.clear();
	}
	
	private void addToComposite(Group choosenGroup, final String name, final String description, final boolean isMultiValued) {
		boolean isNotChecked = true;
		
		if (allVariables != null) {
			isNotChecked = !isChecked(allVariables, name);
		}
		
		if (isNotChecked) {
			final Button checkBtn = new Button(choosenGroup, SWT.CHECK);	
			checkBtn.addListener(SWT.Selection, new Listener() {
				
				@Override
				public void handleEvent(Event event) {
					if (checkBtn.getSelection()) {
						selectedVariable.add(new CouchVariable(name, description, isMultiValued));
					} else {
						selectedVariable.remove(getIndex(name, selectedVariable));
					}
				}
			});
			
			Label labelName = new Label(choosenGroup, SWT.NONE);
			FontData fontData = labelName.getFont().getFontData()[0];
			Font font = new Font(this.getDisplay(), new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
			labelName.setFont(font);
			
			String label = name;
			if (label.startsWith("p_") || label.startsWith("q_")) {
				label = name.substring(2);
			}
			if (isMultiValued) {
				label += " [ ]";
			}
			
			labelName.setText(label);
			C8oBrowser browserDescription = new C8oBrowser(choosenGroup, SWT.MULTI | SWT.WRAP);
			browserDescription.setUseExternalBrowser(true);
			if (SwtUtils.isDark()) {
				browserDescription.setBackground(getParent().getBackground());
			}
			GridData gd = new GridData(GridData.FILL, GridData.CENTER, true, true);
			gd.minimumHeight = 60;
			browserDescription.setLayoutData(gd);
			browserDescription.setText("<html>" +
				"<head>" +
				"<script type=\"text/javascript\">" +
					"document.oncontextmenu = new Function(\"return false\");" +
				"</script>" +
				"<style type=\"text/css\">" +
					  "body {" +
					    "margin: auto;" +
					    "height: 60px;" +
					    "display: table-cell;" +
					    "vertical-align: middle;" +
					    "font-family: Tahoma new, sans-serif;" +
					    "font-size: 0.7em;" +
					    "overflow-y: auto;" +
					    "color: $foreground$;" +
					    "background-color: $background$ } \n" +
					  "a { color: $link$; }" +
				"</style></head><body>" + description + "</body></html>");
			parametersCouch.add(name);
			
			Control[] children = choosenGroup.getChildren();
			for (int i = 0; i < children.length; i++) {
				if (children[i] instanceof Label) {
					String lab = ((Label) children[i]).getText();
					if (label.compareTo(lab) < 0) {
						labelName.moveAbove(children[i - 1]);
						checkBtn.moveAbove(labelName);
						browserDescription.moveBelow(labelName);
						break;
					}
				}
			}
		}
	}
	
	private boolean isChecked(List<RequestableVariable> requestableVariables, String name){
		boolean success = false;
		
		if (requestableVariables != null) {
			for (RequestableVariable requestableVariable : requestableVariables){
				String requestableVariableName = requestableVariable.getName();
				if (requestableVariableName.equals( 
						name.startsWith("p_") || name.startsWith("q_") ? CouchParam.prefix + name.substring(2) : name ) ) {
					return true;
				}
			}
		}
		
		return success;
	}

	private int getIndex(String name, List<CouchVariable> selectedVariable ){
		int index = 0;
		for (CouchVariable var : selectedVariable){
			if (var.getName().equals(name)){
				return index;
			}
			++index;
		}
		return -1;
	}
	
	public List<CouchVariable> getSelectedParameters() {
		return selectedVariable;
	}
} 