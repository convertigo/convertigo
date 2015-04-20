package com.twinsoft.convertigo.engine.enums;

import java.util.Map;

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
	include_docs,
	json_base,
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
	
	public void put(Map<String, String> query, String value) {
		query.put(name(), value);
	}
}
