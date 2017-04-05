package com.twinsoft.convertigo.eclipse.dialogs;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collection;
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
import com.twinsoft.convertigo.beans.transactions.couchdb.PostBulkDocumentsTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.PostDocumentTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.PostUpdateTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.PutUpdateTransaction;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.engine.enums.CouchExtraVariable;
import com.twinsoft.convertigo.engine.enums.CouchParam;

public class CouchVariablesComposite extends ScrolledComposite {
	private Group groupData = null, groupParameters = null, groupQueries = null;
	
	private Composite globalComposite = null;
	
	private List<String> parametersCouch = null;
	private List<RequestableVariable> allVariables = null;
	private List<CouchVariable> selectedVariable = null;
		
	public CouchVariablesComposite(Composite parent, int style, List<RequestableVariable> allVariables) {
		super(parent, style);	
		this.allVariables = allVariables;
		parametersCouch = new ArrayList<String>();
		selectedVariable = new ArrayList<CouchVariable>();
		createContents();
	}
	
	public CouchVariablesComposite(Composite parent, int style) {
		super(parent, style);
		parametersCouch = new ArrayList<String>();
		selectedVariable = new ArrayList<CouchVariable>();
		createContents();
	}

	protected void createContents() {
		setLayout(new GridLayout(1, true));
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
	}

	public void setPropertyDescriptor(AbstractCouchDbTransaction couchDbTransaction, PropertyDescriptor[] propertyDescriptors, DatabaseObject parentObject) {	
		cleanGroups();
		
		int height = 0;
		/* Fill the Group widget */
		for (PropertyDescriptor property : propertyDescriptors) {
			final String name = property.getName();
			final String description = property.getShortDescription();
			if (!parametersCouch.contains(name) ){

				if ( (name.startsWith("q_") || name.startsWith("p_")) ) {
					if (!parentObject.getClass().getCanonicalName().equals(property.getValue( 
								MySimpleBeanInfo.BLACK_LIST_PARENT_CLASS))) {
						Group choosenGroup = name.startsWith("q_") ? groupQueries : groupParameters;
						height += addToComposite(choosenGroup, name, description, false);
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
		
		/* */
		Collection<CouchExtraVariable> extraVariables = null;
		if (couchDbTransaction instanceof PostBulkDocumentsTransaction){
			extraVariables = ((PostBulkDocumentsTransaction) couchDbTransaction).getCouchParametersExtra();
		}
		if (couchDbTransaction instanceof PostDocumentTransaction) {
			extraVariables = ((PostDocumentTransaction) couchDbTransaction).getCouchParametersExtra();
		}
		if (couchDbTransaction instanceof PostUpdateTransaction) {
			extraVariables = ((PostUpdateTransaction) couchDbTransaction).getCouchParametersExtra();
		}
		if (couchDbTransaction instanceof PutUpdateTransaction) {
			extraVariables = ((PutUpdateTransaction) couchDbTransaction).getCouchParametersExtra();
		}
		
		if (extraVariables != null ){
			for (CouchExtraVariable extraVariable : extraVariables) {
				height += addToComposite(groupData, extraVariable.getVariableName(), extraVariable.getVariableDescription(), extraVariable.isMultiValued());
			}
		}
		
		if (groupData.getChildren().length == 0){
			groupData.dispose();
		}

		//Set the minimal size for the scrolled composite
		this.setMinSize(getSize().x, height );
		
		globalComposite.setSize(getSize());	
		this.setContent(globalComposite);
		this.setExpandVertical(true);
		
		this.layout(true);
	}
	
	private void cleanGroups(){	
		final Composite container = this;
				
		globalComposite = new Composite(this, SWT.NONE);
		globalComposite.setLayout(new GridLayout(1, true));
		globalComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		GridLayout layout = new GridLayout(3, false);
		layout.horizontalSpacing = 30;	
		
		/* Parameters */		
		if ((groupParameters != null) && (!groupParameters.isDisposed())) {
			groupParameters.dispose();
		}
		groupParameters = new Group(globalComposite, SWT.SHADOW_ETCHED_OUT | SWT.V_SCROLL);
		groupParameters.setLayout(layout);
		groupParameters.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		groupParameters.setText("Parameters");

		/* Queries */
		if ((groupQueries != null) && (!groupQueries.isDisposed())) {
			groupQueries.dispose();
		}
		groupQueries = new Group(globalComposite, SWT.SHADOW_ETCHED_OUT | SWT.V_SCROLL);
		groupQueries.setLayout(layout);
		
		groupQueries.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		groupQueries.setText("Queries");
		
		/* Extra variables */
		if ((groupData != null) && (!groupData.isDisposed())) {
			groupData.dispose();
		}
		groupData = new Group(globalComposite, SWT.SHADOW_ETCHED_OUT | SWT.V_SCROLL);
		groupData.setLayout(layout);
		groupData.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		groupData.setText("Data");
		
		if (groupData != null) {
			for (Control c : groupData.getChildren()){
				c.dispose();
			}
		}
		
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
		
		addListener(SWT.Resize, new Listener() {
			@Override
			public void handleEvent(Event event) {
				globalComposite.setSize(container.getSize().x, globalComposite.getSize().y);
			}
		});
		
		parametersCouch.clear();
	}
	
	private int addToComposite(Group choosenGroup, final String name, final String description, final boolean isMultiValued){
		int height = 0;
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
			labelName.setText((name.startsWith("p_") || name.startsWith("q_") ? name.substring(2) : name ));
			
			C8oBrowser browserDescription = new C8oBrowser(choosenGroup, SWT.MULTI | SWT.WRAP);
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
								    "margin-top: 5px;" +
								    "overflow-y: auto;" +
								    "background-color: #ECEBEB }"+
							"</style></head><p>" + description + "</p></html>");
			
			browserDescription.setSize(browserDescription.getSize().x, 40);

			parametersCouch.add(name);
			height+=browserDescription.getSize().y + 11;
		}
		
		return height;
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