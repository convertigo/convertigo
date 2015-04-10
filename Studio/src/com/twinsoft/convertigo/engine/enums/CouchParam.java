package com.twinsoft.convertigo.engine.enums;

public enum CouchParam {
	all_or_nothing,
	attname,
	attpath,
	cancel,
	continuous,
	create_target,
	db,
	ddoc,
	destination,
	destination_rev,
	doc_ids,
	docid,
	func,
	key,
	name,
	new_edits,
	password,
	proxy,
	rev,
	section,
	source,
	target,
	value,
	view;
	
	public static final String prefix = "_use_";
	
	public String param() {
		return prefix + name();
	}
}
