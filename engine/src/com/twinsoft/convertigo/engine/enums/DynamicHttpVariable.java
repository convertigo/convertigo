package com.twinsoft.convertigo.engine.enums;

import com.twinsoft.convertigo.beans.transactions.AbstractHttpTransaction;
import com.twinsoft.convertigo.beans.transactions.DownloadHttpTransaction;

public enum DynamicHttpVariable {
	__body("HTTP body", "Content <b><u>body</u></b> of the HTTP POST or PUT request."),
	__uri("URI", "Override <b><u>URI</u></b> of the request."),
	__POST_("POST variable", "Add dynamic <b><u>POST variable</u></b> for this request. You have to rename the <b><u>custom</u></b> part of the variable name.", "__POST_custom"),
	__GET_("GET variable", "Add dynamic <b><u>POST variable</u></b> for this request. You have to rename the <b><u>custom</u></b> part of the variable name.", "__GET_custom"),
	__contentType("Content-Type", "Override the <b><u>Content-Type</u></b> header of the request. Can be useful in combinaison of <b><u>body</u></b>."),
	__header_("Custom Header", "Add dynamic <b><u>custom header</u></b> for this request. You have to rename the <b><u>custom</u></b> part of the variable name.", "__header_custom"),
	__download_folder("Download folder", "Override the <b><u>Folder</u></b> property of this transaction", DownloadHttpTransaction.class),
	__download_filename("Download filename", "Override the <b><u>Filename</u></b> property of this transaction", DownloadHttpTransaction.class);
	
	String display;
	String description;
	String value;
	Class<?>[] onlyFor;
	
	DynamicHttpVariable(String display, String description, String value, Class<?>... onlyFor) {
		this.display = display;
		this.description = description;
		this.onlyFor = onlyFor;
		this.value = value == null ? name() : value;
	}
	
	DynamicHttpVariable(String display, String description, Class<?>... onlyFor) {
		this(display, description, null, onlyFor);
	}
	
	public String display() {
		return display;
	}
	
	public String description() {
		return description;
	}
	
	public String value() {
		return value;
	}
	
	public boolean can(AbstractHttpTransaction transaction) {
		if (onlyFor.length == 0) {
			return true;
		}
		Class<?> tc = transaction.getClass();
		for (Class<?> c: onlyFor) {
			if (tc.isAssignableFrom(c)) {
				return true;
			}
		}
		return false;
	}
}
