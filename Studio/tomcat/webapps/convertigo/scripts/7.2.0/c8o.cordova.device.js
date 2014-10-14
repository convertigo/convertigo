/*
 * Copyright (c) 2001-2013 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

$.extend(true, C8O, {
	_define: {
		cordovaEnv: {
			applicationAuthorName: null,
			applicationAuthorEmail: null,
			applicationAuthorWebsite: null,
			applicationDescription: null,
			applicationId: null,
			applicationName: null,
			builtRevision: null,
			builtVersion: null,
			currentRevision: null,
			currentVersion: null,
			endPoint: null,
			platform: null,
			platformName: null,
			projectName: null,
			uuid: null
		}
	},
	
	fileEntryToURL: function (entry) {
		return entry.toURL().replace(new RegExp("(:/+)|(/)/*","g"), "$1$2").replace(new RegExp("/$"), "");
	}
});

if ("cordova" in window) {
	
	$.extend(true, C8O, {
		vars: {
			local_cache_parallel_downloads: 5
		},
		
		_define: {
			local_cache_dir: null,
			re_download_url: new RegExp("^https?://.*?(\\.\\w*?)(?:\\?.*)?$"),
			re_tail_url: new RegExp("([^#?]*)/.*")
		},
		
		deleteAllCacheEntries: function (success, error) {
			C8O.log.debug("c8o.cdv : deleteAllCacheEntries");
			
			C8O._get_cache_db(function (db) {
				C8O.log.trace("c8o.cdv : deleteAllCacheEntries retrieve db");
				
				db.readTransaction(function (tx) {
					C8O.log.trace("c8o.cdv : deleteAllCacheEntries retrieve tx");
					
					tx.executeSql("SELECT key FROM cacheIndex", [], function (tx, results) {
						var doneCpt = results.rows.length + 1;
						var checkDone = function () {
							if (--doneCpt == 0) {
								C8O.log.debug("c8o.cdv : deleteAllCacheEntries all entries really deleted");
								if (success) {
									success();
								}
							}
						};
						
						for (var i = 0; i < results.rows.length; i++) {
							C8O._local_cache_delete_entry(
								results.rows.item(i).key,
								checkDone,
								checkDone
							);
						}
						
						checkDone();
					}, function (err) {
						C8O.log.error("c8o.cdv : deleteAllCacheEntries failed to executeSql", err);
						
						if (error) {
							error(err);
						}
					});
				}, function(err) {
					C8O.log.error("c8o.cdv : deleteAllCacheEntries failed to optain tx", err);
					
					if (error) {
						error(err);
					}
				});
			}, function (err) {
				C8O.log.debug("c8o.cdv : deleteAllCacheEntries failed to getCacheDB", err);
				
				if (error) {
					error(err);
				}
			});
		},
		
		getCordovaEnv: function (key) {
			return C8O.isDefined(key) ? C8O._define.cordovaEnv[key] : C8O._define.cordovaEnv;
		},
		
		splashscreenHide: function () {
			if (navigator && navigator.splashscreen) {
				navigator.splashscreen.hide();
			}
		},
		
		splashscreenShow: function () {
			if (navigator && navigator.splashscreen) {
				navigator.splashscreen.show();
			}
		},
		
		_cordova_notify_push_server: function (token) {
			C8O.log.debug("c8o.cdv : _cordova_notify_push_server token: " + token);
			
			C8O._call({
				__project: "lib_PushManager",
				__sequence: "RegisterDevice",
				token: token
			});
		},
		
		_cordova_onNotificationGCM: function (event) {
			C8O.log.debug("c8o.cdv : _cordova_onNotificationGCM event", event);
			
			switch (event.event) {
				case "registered":
					C8O.log.debug("c8o.cdv : _cordova_onNotificationGCM registered");
					
					if (event.regid.length > 0) {
						C8O.log.info("c8o.cdv : _cordova_onNotificationGCM registered regid: " + event.regid);
						
						if (C8O._hook("push_register_success", event.regid)) {
							C8O._cordova_notify_push_server("gcm:" + event.regid);
						}
					}
					break;
	
				case "message":
					C8O.log.debug("c8o.cdv : _cordova_onNotificationGCM message");
					C8O._hook("push_notification", "GCM", event.payload.message, event);
					break;
	
				case "error":
					C8O.log.debug("c8o.cdv : _cordova_onNotificationGCM error");
					break;
					
				default:
					C8O.log.debug("c8o.cdv : _cordova_onNotificationGCM unknown GCM event :" + event.event);
					break;
			}
		},
		
		_cordova_onNotificationAPN: function (event) {
			C8O.log.debug("c8o.cdv : _cordova_onNotificationAPN event", event);
			
			if (C8O._hook("push_notification", "APN", event.alert, event)) {
				  if (event.sound) {
						var snd = new Media(event.sound);
						snd.play();
				  }
		
				  if (event.badge) {
						pushNotification.setApplicationIconBadgeNumber(function () {
							// TODO
						}, event.badge);
				  }
			}
		},
		
		_init_cordova: function (params) {
			var appBase = window.location.href.replace(C8O._define.re_tail_url, "$1");
			
			var url = appBase + "/env.json";
			
			C8O.log.debug("c8o.cdv : deviceready retrieve env from: " + url);
			
			$.ajax({
				dataType: "json",
				url: url,
				success: function (data) {
					try {
						$.extend(true, C8O._define.cordovaEnv, data);
						
						if (!C8O._define.log_remote_init_env) {
							C8O._define.log_remote_init_env = {};
						}
						
						C8O._define.log_remote_init_env.cordova_uuid = C8O.getCordovaEnv("uuid");
						
						if (C8O.getCordovaEnv("splashRemoveMode") != "manual") {
							C8O.splashscreenHide();
						}
					} catch (err) {
						C8O.log.error("c8o.cdv : deviceready catch init env", err);
					}
					
					C8O._init(params);
				},
				error: function (xhr, status, err) {
					C8O.log.error("c8o.cdv : deviceready failed to retrieve env.json file: " + url, err);
					
					C8O._init(params);					
				}
			});
		},
		
		_core_get_cache_db: C8O._get_cache_db,
		_get_cache_db: function (success, error) {
			C8O.log.debug("c8o.cdv : _get_cache_db");
			
			if (C8O._define.local_cache_dir == null) {
				C8O.log.debug("c8o.cdv : _get_cache_db initializing local_cache_dir");
				
				if (window.requestFileSystem) {
					var quota = 0;
					try {
						quota = device.platform == "blackberry10" ? Math.pow(1024, 3) : 0;
					} catch (err) {
						C8O.log.warn("c8o.cdv : _get_cache_db cannot determine quota from device.platform, use '0'", err);
					}
					
					window.requestFileSystem(LocalFileSystem.PERSISTENT, quota,
						function(fileSystem) {
							C8O.log.debug("c8o.cdv : _get_cache_db LocalFileSystem retrieved");
							
							fileSystem.root.getDirectory("www", {create: true}, function (www_dir) {
									www_dir.getDirectory("_c8o_local_cache", {create: true}, function (local_cache_dir) {
									C8O.log.debug("c8o.cdv : _get_cache_db '_c8o_local_cache' directory retrieved");
									
									C8O._define.local_cache_dir = local_cache_dir;
									C8O._core_get_cache_db(success, error);
								}, function (err) {
									C8O.log.error("c8o.cdv : _get_cache_db 'www/_c8o_local_cache' directory not retrieved", err);
									
									C8O._define.local_cache_dir = false;
									error(err);
								});
							}, function (err) {
								C8O.log.error("c8o.cdv : _get_cache_db 'www' directory not retrieved", err);
								
								C8O._define.local_cache_dir = false;
								error(err);
							});
						}, function (err) {
							C8O.log.error("c8o.cdv : _get_cache_db LocalFileSystem not retrieved", err);
							
							C8O._define.local_cache_dir = false;
							error(err);
						}
					);
				} else {
					C8O.log.error("c8o.cdv : _get_cache_db no window.requestFileSystem !");
					
					C8O._define.local_cache_dir = false;
					error("Filesystem not available on that phone !");
				}
			} else {
				C8O.log.trace("c8o.cdv : _get_cache_db local_cache_dir already initialized");
				
				C8O._core_get_cache_db(success, error);
			}
		},
		
		_local_cache_delete_entry: function (key, success, error) {
			C8O.log.debug("c8o.cdv : _local_cache_delete_entry for key: " + key);
			
			C8O._define.local_cache_db.readTransaction(function (tx) {
				C8O.log.debug("c8o.cdv : _local_cache_delete_entry retrieve tx");				
				
				tx.executeSql("SELECT data FROM cacheIndex WHERE key=?", [key], function (tx, results) {
					if (results.rows.length) {
						var dirName = results.rows.item(0).data;
						C8O.log.debug("c8o.cdv : _local_cache_delete_entry finds dirName: " + dirName + " for key: " + key);
						
						C8O._define.local_cache_db.transaction(function (tx) {
							tx.executeSql("DELETE FROM cacheIndex WHERE key=?", [key], function () {
								C8O.log.debug("c8o.cdv : _local_cache_delete_entry DELETE done: " + key);
								
								success();
							}, function (err) {
								C8O.log.error("c8o.cdv : _local_cache_delete_entry failed to DELETE", err);
								
								error(err);								
							});
						}, function(err) {
							C8O.log.error("c8o.cdv : _local_cache_delete_entry failed to optain tx for DELETE", err);
							
							error(err);
						});
						
						C8O._define.local_cache_dir.getDirectory(
							dirName,
							{create: false},
							function(dirEntry) {
								C8O.log.trace("c8o.cdv : _local_cache_delete_entry directory retrieved: " + C8O.fileEntryToURL(dirEntry));
								
								dirEntry.removeRecursively(function () {
									C8O.log.debug("c8o.cdv : _local_cache_delete_entry directory removed: " + C8O.fileEntryToURL(dirEntry));
								},
								function (err) {
									C8O.log.error("c8o.cdv : _local_cache_delete_entry error removing file entry", err);
								});
							},
							function (err) {
								C8O.log.error("c8o.cdv : _local_cache_delete_entry error getting directory", err);
							}
						);
					} else {
						C8O.log.warn("c8o.cdv : _local_cache_delete_entry doesn't find entry for key: " + key);
						
						success();
					}
				});
			}, function(err) {
				C8O.log.error("c8o.cdv : _local_cache_delete_entry failed to optain tx for SELECT", err);
				
				error(err);
			});
		},
		
		_local_cache_download_attachments: function (data, xml, dirName, callback) {
			if (!(C8O.vars.local_cache_parallel_downloads *= 1)) {
				C8O.log.debug("c8o.cdv : _local_cache_download_attachments disabled: C8O.vars.local_cache_parallel_downloads == " + C8O.vars.local_cache_parallel_downloads);
				return;
			}
			
			C8O.log.debug("c8o.cdv : _local_cache_download_attachments for directory: " + dirName);
			
			var urls_to_download = [];
			var urls_duplicated = {};
			
			C8O.walk(xml.documentElement, null, function (value) {
				var match = C8O._define.re_download_url.exec(value);
				if (match && C8O._hook("local_cache_check_attachment", value, this, data)) {
					var filename;
					if (C8O.isUndefined(urls_duplicated[value])) {
						urls_duplicated[value] = urls_to_download.length; 
						urls_to_download.push({
							nodes: [this],
							url: value,
							filepath: C8O.fileEntryToURL(C8O._define.local_cache_dir) + "/" + dirName + "/" + urls_to_download.length + match[1]
						});
						C8O.log.debug("c8o.cdv : _local_cache_download_attachments adding url:" + value + " to " + urls_to_download[urls_duplicated[value]].filepath);
					} else {
						urls_to_download[urls_duplicated[value]].nodes.push(this);
						C8O.log.trace("c8o.cdv : _local_cache_download_attachments reusing:" + urls_to_download[urls_duplicated[value]].filepath);
					}
				}
			});
			
			if (urls_to_download.length) {
				var done_cpt = urls_to_download.length;
				C8O.log.info("c8o.cdv : _local_cache_download_attachments prepare to download " + urls_to_download.length + " files with " + C8O.vars.local_cache_parallel_downloads + " parallel downloads");
				
				var downloadOne = function() {
					if (urls_to_download.length) {
						var current = urls_to_download.shift();
						var fileTransfer = new FileTransfer();
						
						C8O.log.trace("c8o.cdv : _local_cache_download_attachments launch download of " + current.url + " to " + current.filepath);
						fileTransfer.download(
							encodeURI(current.url),
							current.filepath,
							function (entry) {
								$.each(current.nodes, function () {
									switch (this.nodeType) {
									case Node.ATTRIBUTE_NODE: this.value = current.filepath; break;
									default: this.nodeValue = current.filepath; break;
									}
								});
								
								C8O.log.debug("c8o.cdv : _local_cache_download_attachments download complete: " + current.filepath + " remaining: " + (done_cpt - 1));
								(--done_cpt == 0) ? callback() : downloadOne();
							},
							function (err) {
								C8O.log.error("c8o.cdv : _local_cache_download_attachments download failed: " + current.url + " error: " + C8O.toJSON(err) + " remaining: " + (done_cpt - 1));
								(--done_cpt == 0) ? callback() : downloadOne();
							}
						);
					}
				};
				
				for (var i = 0; i < C8O.vars.local_cache_parallel_downloads; i++) {
					downloadOne();
				}
			} else {
				C8O.log.debug("c8o.cdv : _local_cache_download_attachments nothing to download");
			}
		},
				
		_local_cache_handle_expired: function (success, error) {
			var now = new Date();
			C8O.log.debug("c8o.cdv : _local_cache_handle_expired handle cache entries expired after: " + now.toISOString());
			
			C8O._define.local_cache_db.readTransaction(function (tx) {
				tx.executeSql("SELECT * FROM cacheIndex WHERE expirydate < ?", [now.getTime()], function(tx, results) {
					var toRemove = results.rows.length;
					
					if (toRemove) {
						var removed = function () {
							if (--toRemove == 0) {
								C8O.log.debug("c8o.cdv : _local_cache_handle_expired all entries removed");
								
								if (success) {
									success();
								}
							}
						};
						
						for (var i = 0; i < results.rows.length; i++) {
							var item = results.rows.item(i);
							
							C8O.log.debug("c8o.cdv : _local_cache_handle_expired entry '" + item.key + "' expired with data:" + item.data + " since:" + new Date(item.expirydate * 1).toISOString());
							C8O._local_cache_delete_entry(item.key, removed, function (err) {
								C8O.log.info("c8o.cdv : _local_cache_handle_expired failed to remove '" + item.key + "'", err);
								removed();
							});
						}
					} else {
						C8O.log.debug("c8o.cdv : _local_cache_handle_expired nothing to remove");
					}
				});
			}, function(err) {
				C8O.log.error("c8o.cdv : _local_cache_handle_expired failed to optain tx", err);
				
				if (error) {
					error(err);
				}
			});
		},
		
		_local_cache_insert: function (key, xml, success, error) {
			var cacheOptions = key.__localCache;
			delete key.__localCache;
			var tKey = C8O.toJSON(key);
			var tXml = C8O.serializeXML(xml);
			var now = new Date();

			// Compute expiry date
			var expDate = (cacheOptions.ttl) ? cacheOptions.ttl + now.getTime() : new Date("3000-01-01").getTime();

			C8O.log.debug("c8o.cdv : _local_cache_insert for key: " + tKey + " with expiry date: " + expDate + " (" + new Date(expDate).toISOString()+") in directory: " + now.getTime() + " with data lenght: " + tXml.length);
			
			if (C8O.canLog("trace")) {
				C8O.log.trace("c8o.cdv : _local_cache_insert data to cache is: " + tXml);
			}

			C8O._define.local_cache_dir.getDirectory(
				"" + now.getTime(),
				{create: true},
				function (dirEntry) {
					C8O.log.trace("c8o.cdv : _local_cache_insert the directory '" + C8O.fileEntryToURL(dirEntry) + "' created, requesting cache.xml");
					
					dirEntry.getFile(
						"cache.xml", 
						{create: true},
						function(fileEntry) {
							C8O.log.debug("c8o.cdv : _local_cache_insert file created: " + C8O.fileEntryToURL(fileEntry));
							
							fileEntry.createWriter(
								function(writer) {
									C8O.log.trace("c8o.cdv : _local_cache_insert writer created for file: " + C8O.fileEntryToURL(fileEntry));
									
									writer.onwriteend = function(evt) {
										C8O.log.debug("c8o.cdv : _local_cache_insert write done: " + C8O.toJSON(evt));
										
										C8O._local_cache_delete_entry(tKey, function () {
											C8O.log.debug("c8o.cdv : _local_cache_insert create a cache entry for: " + tKey);
											
											C8O._define.local_cache_db.transaction(function(tx) {
												tx.executeSql("INSERT INTO cacheIndex (key, data, expirydate) VALUES(? , ?, ?)", [tKey, dirEntry.name, expDate], function () {
													C8O.log.debug("c8o.cdv : _local_cache_insert created a cache entry for: " + tKey);
													
													C8O._local_cache_download_attachments(key, xml, dirEntry.name, function() {
														C8O.log.debug("c8o.cdv : _local_cache_insert attachments are downloaded for: " + dirEntry.name);
														tXml = C8O.serializeXML(xml);
														
														if (C8O.canLog("trace")) {
															C8O.log.debug("c8o.cdv : _local_cache_insert update XML in cache: " + tXml);
														}
														
														writer.onwriteend = function () {
															C8O.log.debug("c8o.cdv : _local_cache_insert updated xml write done for: " + tKey);
														};
														
														writer.onerror = function (err) {
															C8O.log.debug("c8o.cdv : _local_cache_insert updated xml write failed for: " + tKey + " with error", err);
														};
														
														writer.seek(0);
														writer.write(tXml);
													});
													
													success();
												}, function (err) {
													C8O.log.error("c8o.cdv : _local_cache_insert failed to INSERT the entry", err);
													
													error(err);	
												});
											}, function (err) {
												C8O.log.error("c8o.cdv : _local_cache_insert failed to optain tx", err);
												
												error(err);
											});
										}, function (err) {
											C8O.log.error("c8o.cdv : _local_cache_insert failed to delete entry", err);
											
											error(err);
										});
									};
									
									writer.onerror = function (err) {
										C8O.log.debug("c8o.cdv : _local_cache_insert write failed for: " + tKey + " with error", err);
										
										error(err);
									};
									
									writer.write(tXml);
								}, function(err) {
									C8O.log.error("c8o.cdv : _local_cache_insert failed to create the writer", err);
									
									error(err);
								}
							);
						}, function(err) {
							C8O.log.error("c8o.cdv : _local_cache_insert failed to create the file", err);
							
							error(err);
						}
					);
				}
			);
		},
		
		_core_local_cache_search_entry_success: C8O._local_cache_search_entry_success,
		
		_local_cache_search_entry_success: function (dirName, success, error) {
			var cacheURL = C8O.fileEntryToURL(C8O._define.local_cache_dir) + "/" + dirName + "/cache.xml";
			
			C8O.log.debug("c8o.cdv : _local_cache_search_entry_success try to ajax load: " + cacheURL);
			
			$.ajax({
				cache: false,
				dataType: "xml",
				url: cacheURL,
				success: function (xml) {
					if (C8O.canLog("trace")) {
						C8O.log.trace("c8o.cdv : _local_cache_search_entry_success data read from cache: " + C8O.serializeXML(xml));
					}
					
					success(xml);
				},
				error: function (xhr, status, err) {
					C8O.log.error("c8o.cdv : _local_cache_search_entry_success failed to read data from cache", {status: status, error: err});
					
					error(err);
				}
			});
		}
	});
	
	C8O._define.init_wait.push(C8O._init_cordova);
	
	$(document).on("deviceready", function() {
		C8O.log.info("c8o.cdv : cordova on deviceready");
		
		if (C8O._hook("device_ready")) {
			C8O.log.info("c8o.cdv : window.plugins defined ? " + C8O.isDefined(window.plugins));

			var devicePlatform = device.platform.toUpperCase();
			
			if (C8O.isDefined(window.plugins)) {
				C8O.log.info("c8o.cdv : window.plugins.pushNotification defined ? " + C8O.isDefined(window.plugins.pushNotification));
				
				if (C8O.isDefined(window.plugins.pushNotification)) {
					var pushNotification = window.plugins.pushNotification;

					C8O.log.info("c8o.cdv : pushNotification detected");

					var options;

					if (devicePlatform == "ANDROID") {
						C8O.log.info("c8o.cdv : Android detected");

						if (C8O.isDefined(C8O.cordova.androidSenderID) && C8O.cordova.androidSenderID.length > 0) {
							options = {
								"senderID": C8O.cordova.androidSenderID,
								"ecb": "C8O._cordova_onNotificationGCM"
							};
						} else {
							C8O.log.error("c8o.cdv : no senderID for ");
							C8O._hook("push_register_failed", "missing senderID");
						}
					} else {
						C8O.log.info("c8o.cdv : IOs detected");

						options = {
							"badge": "true",
							"sound": "true",
							"alert": "true",
							"ecb": "C8O._cordova_onNotificationAPN"
						};
					}

					pushNotification.register(
						function (result) {
							if (devicePlatform == "ANDROID") {
								C8O.log.info("c8o.cdv : PushNotificationRegistered for Android: " + result);
							} else if (devicePlatform == "IOS") {
								C8O.log.info("c8o.cdv : PushNotificationRegistered for iOS: " + result);
								if (typeof result == "string") {
									if (C8O._hook("push_register_success", result)) {
										C8O._cordova_notify_push_server("apns:" + result);
									}
								}
							}
						},
						function (error) {
							C8O.log.error("c8o.cdv : PushNotificationRegistered Failed: " + error);
							C8O._hook("push_register_failed", error);
						},
						options
					);
				}
			}
			
			if (devicePlatform == "IOS" && C8O.isDefined(StatusBar)) {
				C8O.log.debug("c8o.cdv : IOs detected");
				
				if (StatusBar.isVisible) {
					StatusBar.overlaysWebView(false);
					StatusBar.styleBlackOpaque();
					StatusBar.backgroundColorByName("black");
				}
			}
			
			if (C8O.isUndefined(window.openDatabase) && C8O.isDefined(sqlitePlugin)) {
				window.openDatabase = sqlitePlugin.openDatabase;
			}
		}
		C8O.log.debug("c8o.cdv : end deviceready");
	});

} else {
	C8O.log.warn("c8o.cdv : no cordova available, ignore c8o.cordova.device.js");
	
	$.extend(true, C8O, {
		deleteAllCacheEntries: function (success, error) {
			C8O.log.warn("c8o.cdv : deleteAllCacheEntries but no cordova available, just call success");
			
			if (success) {
				success();
			}
		},
		
		getCordovaEnv: function (key) {
			C8O.log.warn("c8o.cdv : getCordovaEnv but no cordova available, just return an empty value");

			return C8O.isDefined(key) ? C8O._define.cordovaEnv[key] : C8O._define.cordovaEnv;
		},
		
		splashscreenHide: function () {
			C8O.log.warn("c8o.cdv : splashscreenHide but no cordova available, do nothing");
		},
		
		splashscreenShow: function () {
			C8O.log.warn("c8o.cdv : splashscreenShow but no cordova available, do nothing");
		}
	});
}