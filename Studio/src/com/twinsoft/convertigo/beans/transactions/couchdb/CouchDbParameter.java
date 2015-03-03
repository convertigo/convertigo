package com.twinsoft.convertigo.beans.transactions.couchdb;

import java.util.List;

import com.twinsoft.convertigo.beans.variables.RequestableMultiValuedVariable;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.CouchKey;

public enum CouchDbParameter {
	Private_id(CouchKey._id.key(),false,-1),
	Private_ids(CouchKey._id.key(),true,-1),
	Private_rev(CouchKey._rev.key(),false,-1),
	
	Path_database("dbname",false,0),
	Path_docid("docid", false,0),
	Path_filename("filename", false,0),
	Path_key("key",false,0),
	Path_section("section",false,0),
	Path_value("value",false,0),
	Path_viewname("viewname", false,0),
	
	Param_cancel("cancel", false,1),
	Param_continuous("continuous", false,1),
	Param_count("count",false,1),
	Param_create_target("create_target", false,1),
	Param_data("data", false,1),
	Param_datas("data", true,1),
	Param_doc_ids("doc_ids", true,1),
	Param_docrev("docrev", false,1),
	Param_feed("feed",false,1),
	Param_filepath("filepath", false,1),
	Param_heartbeat("heartbeat",false,1),
	Param_log_bytes("bytes",false,1),
	Param_log_offset("offset",false,1),
	Param_proxy("proxy",false,1),
	Param_source("source",false,1),
	Param_target("target",false,1),
	Param_timeout("timeout",false,1),
	Param_user_name("name",false,1),
	Param_user_password("password",false,1),
	Param_view_endkey("endkey", false,1),
	Param_view_key("key", false,1),
	Param_view_limit("limit", false,1),
	Param_view_skip("skip", false,1),
	Param_view_startkey("startkey", false,1);
	
	static final public CouchDbParameter[] empty = new CouchDbParameter[0];
	
	final int TYPE_PRIVATE = -1;
	final int TYPE_QUERY_PATH = 0;
	final int TYPE_QUERY_PARAMETER = 1;
	
//	private int type;
	private String variableName;
	private boolean multiValued;
	CouchDbParameter(String variableName, boolean multiValued, int type) {
		this.variableName = variableName;
		this.multiValued = multiValued;
//		this.type = type;
	}
	
	private RequestableVariable create() throws EngineException {
		RequestableVariable variable = multiValued ? new RequestableMultiValuedVariable():new RequestableVariable();
		variable.setName(variableName);
		variable.bNew = true;
		return variable;
	}
	
	public String variableName() {
		return variableName;
	}
	
	public static RequestableVariable create(String param_name) throws EngineException {
		CouchDbParameter param = CouchDbParameter.valueOf(param_name);
		return param.create();
	}
	
	public static boolean contains(List<CouchDbParameter> params, String variableName) {
		return find(params, variableName) != null;
	}
	
	public static CouchDbParameter find(List<CouchDbParameter> params, String variableName) {
		if (params != null) {
			for (CouchDbParameter param : params) {
				if (param.variableName().equalsIgnoreCase(variableName)) {
					return param;
				}
			}
		}
		return null;
	}
	
}
