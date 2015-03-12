$.extend(true, C8O, {
	init_vars: {
		fs_server: "http://127.0.0.1:5984",
		fs_username: null,
		fs_password: null
	},
	
	vars: {
		fs_default_db: null,
		fs_default_design: null,
		fs_token: null
	},
	
	_define: {
		re_fs_match_db: new RegExp("^fs://([^/]*)"),
		re_fs_parse_headers: new RegExp("^\\s*(.*?): (.*)"),
		re_fs_get_policy: new RegExp("^post(?:_(.*))?")
	},
	
	_pouch: {
		dbs: {},
		
		handle: function (err, doc, callback) {
			callback(doc);
		},
		
		getDb: function (db) {
			return C8O._pouch.dbs[db] ? C8O._pouch.dbs[db] : (C8O._pouch.dbs[db] = new PouchDB(db));
		},
		
		getAllDocs: function (db, callback) {
			var options = {};
			C8O._pouch.getDb(db).allDocs(options, function (err, doc) {
				C8O._pouch.handle(err, doc, callback);
			});
		},
		
		getDocument: function (db, docId, docRev, callback) {
			var options = {};
			if (docRev) {
				options.rev = docRev;
			}
			C8O._pouch.getDb(db).get(docId, options, function (err, doc) {
				C8O._pouch.handle(err, doc, callback);
			});
		},
		
		getDocumentRev: function (db, docId, callback) {
			C8O._pouch.getDb(db).get(docId, options, function (err, doc) {
				callback(doc._rev);
			});
		},
		
		postDocument: function (db, document, policy, callback) {
			var options = {};
			C8O._fs.applyPolicy(db, document, policy, function (document) {
				C8O._pouch.getDb(db).post(document, options, function (err, doc) {
					C8O._pouch.handle(err, doc, callback);
				});
			});
		},
		
		deleteDocument: function (db, docId, docRev, callback) {
			var options = {};
			var addDocRev = function (docRev) {
				C8O._pouch.getDb(db).remove(docId, docRev, options, function (err, doc) {
					C8O._pouch.handle(err, doc, callback);
				});
			}
			
			if (!docRev) {
				C8O._pouch.getDocumentRev(db, docId, addDocRev);
			} else {
				addDocRev(docRev);
			}
		},
		
		getView: function (db, docId, viewName, options, callback) {
			var view = docId.replace("_design/", "") + "/" + viewName;
			C8O._pouch.getDb(db).query(view, options, function (err, doc) {
				C8O._pouch.handle(err, doc, callback);
			});
		},
		
		postReplicate: function (source, target, create_target, continuous, cancel, callback) {
			var options = {
				live: continuous,
				retry: continuous
			};
			
			source = source.indexOf("://") != -1 ? source : C8O._pouch.getDb(source);
			target = target.indexOf("://") != -1 ? target : C8O._pouch.getDb(target);
			
			var rep = PouchDB.replicate(source, target, options).on("complete", function (info) {
				callback(info);
			});
			
			if (cancel) {
				rep.cancel();
			}
		}
	},
	_fs: {
		server: null,
		auth: null,
		
		getRemoteUrl: function (db) {
			return C8O._fs.remote + (C8O.vars.fs_token ? "/~" + C8O.vars.fs_token : "") + "/" + db
		},
		
		getDatabaseUrl: function (db) {
			return C8O._fs.server + '/' + db;
		},
		
		getDocumentUrl: function (db, docId) {
			return C8O._fs.server + '/' + db + '/' + docId;
		},
		
		putDatabase: function (db, callback) {
			var request = {type: "PUT", url: C8O._fs.getDatabaseUrl(db)};
			
			C8O._fs.execute(request, callback);
		},
		
		getAllDocs: function (db, callback) {
			var request = {type: "GET", url: C8O._fs.getDatabaseUrl(db) + "/_all_docs"};
			
			C8O._fs.execute(request, callback);
		},
		
		headDocument: function (db, docId, callback) {
			var request = {db: db, type: "HEAD", url: C8O._fs.getDocumentUrl(db, docId)};
			
			C8O._fs.execute(request, callback);
		},
		
		getDocument: function (db, docId, docRev, callback) {
			var request = {db: db, type: "GET", url: C8O._fs.getDocumentUrl(db, docId)};
			
			if (docRev) {
				request.headers = {"If-Match": docRev};
			}
			
			C8O._fs.execute(request, callback);
		},
		
		getDocumentRev: function (db, docId, callback) {
			headDocument(db, docId, function (head) {
				var rev = null;
				try {
					var _c8oMeta = head._c8oMeta;
					if ("success" == _c8oMeta.status) {
						rev = _c8oMeta.headers.ETag;
						rev = rev.substring(1, rev.length() - 1);
					}
				} catch (e) {
					// TODO Auto-generated catch block
				}
				callback(rev);
			});
		},
		
		postDocument: function (db, document, policy, callback) {
			C8O._fs.applyPolicy(db, document, policy, function (document) {
				var request = {db: db, type: "POST", url: C8O._fs.getDatabaseUrl(db)};
				C8O._fs.setJsonEntity(request, document);			
				
				C8O._fs.execute(request, callback);
			});
		},
		
		deleteDocument: function (db, docId, docRev, callback) {
			var request = {db: db, type: "DELETE", url: C8O._fs.getDocumentUrl(db, docId)};
			
			var addDocRev = function (docRev) {
				if (docRev) {
					request.headers = {"If-Match": docRev};
				}
				C8O._fs.execute(request, callback);
			}
			
			if (!docRev) {
				C8O._fs.getDocumentRev(db, docId, addDocRev);
			} else {
				addDocRev(docRev);
			}
		},
		
		getView: function (db, docId, viewName, options, callback) {
			if (docId.indexOf("_design/") != 0) {
				docId = "_design/" + docId;
			}
			var request = {db: db, type: "GET", url: C8O._fs.getDocumentUrl(db, docId) + "/_view/" + viewName + "?" + $.param(options)};
						
			C8O._fs.execute(request, callback);
		},
		
		postReplicate: function (source, target, create_target, continuous, cancel, callback) {
			var request = {type: "POST", url: C8O._fs.server + "/_replicate"};
			
			var json = {
				source: source,
				target: target,
				create_target: create_target,
				continuous: continuous,
				cancel: cancel
			};
			
			C8O._fs.setJsonEntity(request, json);
			
			return C8O._fs.execute(request, callback);
		},
		
		applyPolicy: function (db, document, policy, callback) {
			delete document._c8oMeta;
			
			if (policy == "none") {
				// don't modify
				callback(document);
			} if (policy == "create") {
				delete document._id;
				delete document._rev;
				
				callback(document);
			} else {
				var docId = document._id || null;
				
				if (docId != null) {
					if (policy == "override") {
						C8O._fs.getDocumentRev(db, docId, function (rev) {
							if (rev != null) {
								document._rev = rev;
							}
							callback(document);
						});
					} else if (policy == "merge") {
						C8O._fs.getDocument(db, docId, undefined, function (dbDocument) {
							if (dbDocument._id) {
								delete dbDocument._c8oMeta;
								// merge documents
								delete document._rev;
								callback($.extend(true, dbDocument, document));
							}
						});
					}
				}
			}
		},
		
		setJsonEntity: function (request, json) {
			request.data = C8O.toJSON(json);
			request.headers = $.extend(true, request.headers, {"Content-Type": "application/json"});
		},
		
		execute: function (request, callback) {
			if (!request.headers || !request.headers["Accept"]) {
				request.headers = $.extend(true, request.headers, {"Accept": "application/json"});
			}
			
			if (C8O._fs.auth != null) {
				request.headers = $.extend(true, request.headers, C8O._fs.auth);
			}
			
			request.dataType = "text";
			request.processData = false;
			
			C8O.log.debug("c8o.fs  : execute url " + request.type + " " + request.url);
			
			$.ajax(request).always(function (response, status, jqXHR) {
				if (!$.isPlainObject(response)) {
					response = jqXHR;
				}
				
				var json = null;
				
				var contentType = response.getResponseHeader("Content-Type");
				
				if (contentType == "application/json" || contentType == "text/plain") {
					json = $.parseJSON(response.responseText);
				}
				
				json = json || {};
				
				var code = response.status;
				
				if (request.db && code == 404 && "no_db_file" == json.reason) {
					C8O._fs.putDatabase(request.db, function (putResponse) {
						if (putResponse.ok) {
							C8O._fs.execute(request, callback);
						} else {
							//TODO
						}
					});
					return;
				}
				
				status =
					code < 100 ? "unknown" :
					code < 200 ? "informational" :
					code < 300 ? "success" :
					code < 400 ? "redirection" :
					code < 500 ? "client error" :
					code < 600 ? "server error" : "unknown";
				
				var responseDetails = {
					statusCode: code,
					status: status,
					reasonPhrase: response.statusText,
					headers: {}
				};
				
				var headers = response.getAllResponseHeaders();
				for (var header = C8O._define.re_fs_parse_headers.exec(headers); header != null; header = C8O._define.re_fs_parse_headers.exec(headers)) {
					headers = headers.substring(header[0].length);
					responseDetails.headers[header[1]] = header[2];
				}
				
				json._c8oMeta = responseDetails;
				
				if (callback) {
					callback(json);
				}
			});
		},
		
		removeDoubleUnderscore: function (data) {
			for (var key in data) {
				if (key.indexOf("__") == 0) {
					delete data[key];
				}
			}
		}
	},
	
	fs_replicate: function (options, callback) {
		C8O.fs_update_device(options, callback);
		C8O.fs_update_remote(options, callback);
	},
	
	fs_update_device: function (options, callback) {
		var db = options.db || C8O.vars.fs_default_db;
		var continuous = options.continuous || false;
		var cancel = options.cancel || false;
		
		callback = callback || function (doc) {
			C8O.log.info("c8o.fs  : fs_update_device return " + C8O.toJSON(doc));
		};
		
		C8O._fs.postReplicate(C8O._fs.getRemoteUrl(db), db + "_device", true, continuous, cancel, callback);
	},
	
	fs_update_remote: function (options, callback) {
		var db = options.db || C8O.vars.fs_default_db;
		var continuous = options.continuous || false;
		var cancel = options.cancel || false;
		
		callback = callback || function (doc) {
			C8O.log.info("c8o.fs  : fs_update_remote return " + C8O.toJSON(doc));
		};
		
		C8O._fs.postReplicate(db + "_device", C8O._fs.getRemoteUrl(db), false, continuous, cancel, callback);
	}
});

C8O._init.locks.fullsync = true;
C8O._init.tasks.push(function () {
    C8O.log.info("c8o.fs  : initializing FullSync");

    if (C8O.isDefined(window.PouchDB)) {
        C8O.log.info("c8o.fs  : using PouchDB for FullSync");
		$.extend(true, C8O._fs, C8O._pouch);
	}
    
    C8O._fs.server = C8O.init_vars.fs_server;
    C8O._fs.remote = C8O._define.convertigo_path + "/fullsync";
    
    if (C8O.init_vars.fs_username != null && C8O.init_vars.fs_password != null) {
    	C8O._fs.auth = {
    		"Authorization":
    		"Basic " + btoa(C8O._remove(C8O.init_vars, "fs_username") + ":" + C8O._remove(C8O.init_vars, "fs_password"))
    	};
    }
    
    delete C8O._init.locks.fullsync;
    C8O._init.check();
});

C8O.addHook("_call_fs", function (data) {
	var db;
	if ((db = C8O._define.re_fs_match_db.exec(data.__project)) != null) {
		db = (db[1] ? db[1] : C8O.vars.fs_default_db) + "_device";
		C8O.log.debug("c8o.fs  : database used '" + db + "'");
		
		var callback = function (json) {
			C8O.log.debug("c8o.fs  : json response\n" + JSON.stringify(json));
			
			var xmlData = $.parseXML("<couchdb_output/>");
			C8O._jsonToXml(undefined, json, xmlData.documentElement);
			C8O.log.debug("c8o.fs  : xml response\n" + C8O.serializeXML(xmlData));
			
			var fakeXHR = {
				C8O_data: data
			};
			
			C8O._onCallComplete(fakeXHR, "success");
			C8O._onCallSuccess(xmlData, "success", fakeXHR);
		};
		
		if (data.__sequence) {		
			if (data.__sequence == "get") {
				C8O._fs.getDocument(db, data.docid, data.docrev, callback);
			} else if (data.__sequence == "head") {
				C8O._fs.headDocument(db, data.docid, callback);
			} else if (data.__sequence.indexOf("post") == 0) {
				var policy = C8O._define.re_fs_get_policy.exec(data.__sequence)[1] || "none";
				policy = data.__postPolicy || policy;
				
				var postData = $.extend({}, data);
				
				C8O._fs.removeDoubleUnderscore(postData);
				
				C8O._fs.postDocument(db, postData, policy, callback);
			} else if (data.__sequence == "delete") {
				C8O._fs.deleteDocument(db, data.docid, callback);
			} else if (data.__sequence == "view") {
				var docid = data.docid || C8O.vars.fs_default_design;
				var viewname = data.viewname;
				
				var viewData = $.extend({}, data);
				
				delete viewData.docid;
				delete viewData.viewname;
				
				C8O._fs.removeDoubleUnderscore(viewData);
				
				C8O._fs.getView(db, docid, viewname, viewData, callback);
			} else if (data.__sequence == "all") {
				C8O._fs.getAllDocs(db, callback);
			} else {
				callback({error: "invalid command '" + data.__sequence + "'"});
			}
		}
		return false;
	}
});

$.support.cors = true;
