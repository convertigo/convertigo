package com.twinsoft.convertigo.engine.enums;

public enum CouchParam {
	attname,
	destination,
	destination_rev,
	db,
	docid, rev, section, key, attpath, ddoc, view, source, target, create_target, continuous, cancel, doc_ids, proxy, password, name, func, value;
	
	public static final String prefix = "_use_";
	
	public String param() {
		return prefix + db.name();
	}
}
