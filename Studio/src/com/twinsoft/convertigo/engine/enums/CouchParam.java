package com.twinsoft.convertigo.engine.enums;

public enum CouchParam {
	attname,
	destination,
	destination_rev,
	db,
	docid, rev, section, key, attpath, ddoc, view, source, target, create_target, continuous, cancel, doc_ids, proxy, password, name, func, value;
	
	public String param() {
		return "__" + db.name();
	}
}
