package com.twinsoft.convertigo.beans.transactions.couchdb;

public class CouchVariable{
	private String name;
	private String description;
	private boolean multiValued = false;
	
	public CouchVariable(String name, String description, boolean multiValued){
		this.name = name;
		this.description = description;
		this.multiValued = multiValued;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isMultiValued() {
		return multiValued;
	}

	public void setMultiValued(boolean multiValued) {
		this.multiValued = multiValued;
	}
}
