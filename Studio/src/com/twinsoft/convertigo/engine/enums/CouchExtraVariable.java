package com.twinsoft.convertigo.engine.enums;

public enum CouchExtraVariable {
	_id("_id", false, "Document id"), 
	_ids("_id", true, "Document ids"),
	data("data", false, "Document content"),
	datas("data", true, "Documents contents"),
	_rev("_rev", false, "Document revision"),
	_revs("_rev", true, "Documents revisions"),
	_deleted("_deleted", false, "Delete document with 'true'"),
	_deleteds("_deleted", true, "Delete documents with 'true'");
	
	private String variableName = "";
	private String variableDescription = "";
	private boolean multiValued = false;
	
	CouchExtraVariable(String variableName, boolean multiValued, String variableDescription) {
		this.variableName = variableName;
		this.multiValued = multiValued;
		this.variableDescription = variableDescription;
	}

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}

	public String getVariableDescription() {
		return variableDescription;
	}

	public void setVariableDescription(String variableDescription) {
		this.variableDescription = variableDescription;
	}

	public boolean isMultiValued() {
		return multiValued;
	}

	public void setMultiValued(boolean multiValued) {
		this.multiValued = multiValued;
	}
		
}
