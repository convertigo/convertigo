package com.twinsoft.convertigo.eclipse.dialogs;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
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

import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.CouchParam;

public class CouchVariablesComposite extends Composite {
	private GridLayout gridLayout = null;
	private Group groupParameters = null, groupQueries = null;
	
	private List<String> parametersCouch = null;
	private List<RequestableVariable> allVariables = null;
	private Map<String, String> selectedParameters = null;
		
	public CouchVariablesComposite(Composite parent, int style, List<RequestableVariable> allVariables) {
		super(parent, style);		
		this.allVariables = allVariables;
		parametersCouch = new ArrayList<String>();
		selectedParameters = new HashMap<String, String>();
		createContents();
	}
	
	public CouchVariablesComposite(Composite parent, int style) {
		super(parent, style);
		parametersCouch = new ArrayList<String>();
		selectedParameters = new HashMap<String, String>();
		createContents();
	}

	protected void createContents() {
		gridLayout = new GridLayout(3, false);
		gridLayout.horizontalSpacing = 18;
		setLayoutData(new GridData(GridData.FILL_BOTH));
		setLayout(gridLayout);	
	}

	public void setPropertyDescriptor(PropertyDescriptor[] propertyDescriptors) {	
		cleanGroups();
		
		/* Fill the Group widget */
		for (PropertyDescriptor property : propertyDescriptors) {
			final String name = property.getName();
			final String description = property.getShortDescription();
			if (!parametersCouch.contains(name) ){

				if (name.startsWith("q_") || name.startsWith("p_")) {
					
					Group choosenGroup = name.startsWith("q_") ? groupQueries : groupParameters;
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
									selectedParameters.put(name, description);
								} else {
									selectedParameters.remove(name);
								}
							}
						});
						
						Label labelName = new Label(choosenGroup, SWT.NONE);
						FontData fontData = labelName.getFont().getFontData()[0];
						Font font = new Font(this.getDisplay(), new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
						labelName.setFont(font);
						labelName.setText(name.substring(2));
						
						Browser browserDescription = new Browser(choosenGroup, SWT.MULTI | SWT.WRAP | (Engine.isLinux() ? SWT.MOZILLA : SWT.NONE));
						browserDescription.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
						browserDescription.setText("<html>" +
								"<head>" +
								"<script type=\"text/javascript\">"+
							        "document.oncontextmenu = new Function(\"return false\");"+
							    "</script>"+
										"<style type=\"text/css\">"+
											  "body {"+
											    "font-family: Tahoma new, sans-serif;" +
											    "font-size: 0.7em;"+
											    "margin-top: 5px;"+
											    "background-color: #ECEBEB }"+
										"</style></head><p>" + description + "</p></html>");
						
						browserDescription.setSize(browserDescription.getSize().x, 32);
						parametersCouch.add(name);
					}
				}
			}
		}
		if (groupQueries.getChildren().length == 0) {
			groupQueries.dispose();
		}
		if (groupParameters.getChildren().length == 0) {
			groupParameters.dispose();
		}
		
		this.layout(true);
	}
	
	private void cleanGroups(){	
		GridData gridData = new GridData(SWT.FILL, SWT.TOP | SWT.FILL, true, false);
		gridData.horizontalSpan = 3;
		
		if ((groupParameters != null) && (!groupParameters.isDisposed())) {
			groupParameters.dispose();
		}
		groupParameters = new Group(this, SWT.SHADOW_ETCHED_OUT | SWT.V_SCROLL);
		groupParameters.setLayout(gridLayout);
		groupParameters.setLayoutData(gridData);
		groupParameters.setText("Parameters");

		gridData = new GridData(SWT.FILL, SWT.TOP | SWT.FILL, true, false);
		gridData.horizontalSpan = 3;
			
		if ((groupQueries != null) && (!groupQueries.isDisposed())) {
			groupQueries.dispose();
		}
		groupQueries = new Group(this, SWT.SHADOW_ETCHED_OUT | SWT.V_SCROLL);
		groupQueries.setLayout(gridLayout);
		groupQueries.setLayoutData(gridData);
		groupQueries.setText("Queries");
		
		if (groupParameters != null) {
			for (Control c : groupParameters.getChildren()){
				c.dispose();
			}
		}

		if (groupQueries != null) {
			for (Control c : groupQueries.getChildren()){
				c.dispose();
			}	
		}

		parametersCouch.clear();
	}
	
	private boolean isChecked(List<RequestableVariable> requestableVariables, String name){
		boolean success = false;
		
		if (requestableVariables != null) {
			for (RequestableVariable requestableVariable : requestableVariables){
				String requestableVariableName = requestableVariable.getName();
				if (requestableVariableName.startsWith(CouchParam.prefix)){
					if (requestableVariableName.equals(CouchParam.prefix + name.substring(2))) {
						return true;
					}
				}
			}
		}
		
		return success;
	}

	public Map<String, String> getSelectedParameters() {
		return selectedParameters;
	}
	
} 