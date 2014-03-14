/*

/!\ Following workflow not up to date /!\

isLocal ?
 yes > isFlashUpdate
 no > getEnv, hasLocal

hasLocal ?
 yes > isLocalNewer
 no > isFlashUpdate

isLocalNewer ?
 yes > redirectLocal
 no > isFlashUpdate

isFlashUpdate ?
 yes > isRemoteNewer
 no > redirectApp
 
isRemoteNewer ?
 yes > doUpdate
 no > redirectApp
 
doUpdate, local ?
  yes > doRemoveUnexisting, downloadFiles
  no > downloadFiles
  
downloadFiles ?
  yes > redirectLocal
  no > redirectApp

*/

var F = {
	reTailUrl: new RegExp("([^#?]*)/.*"),
	startTime: new Date().getTime(),
	isLocal: false,
	currentFiles: null,
	remoteFiles: null,
	flashUpdateDir: null,
	platform: "n/a",
	uuid: "n/a",
	debugStream: "",
	remoteBase: null,
	endPoint: null,
	applicationName: null,
	projectName: null,
	localBase: null,
	webLocalBase: null,
	appBase: null,
	timeout: 0,
	firstLaunch: true,
	clickEvent: typeof(document.ontouchstart) == "undefined" ? "click" : "touchstart",
	
	debug: function (msg) {
		F.debugStream += msg + "\n";
		console.log("debug: " + msg);
	},
	
	error: function (msg, err) {
		if (typeof(err) != "undefined") {
    		var sErr = "" + err;
			try {
				sErr += " " + JSON.stringify(err);
			} catch (e) {}
    		msg += "\nException: " + sErr;
		}
		console.log("error: " + msg);
		alert(F.debugStream + "\n" + msg);
	},
	
	init: function () {
		F.debug("init");
		
		try {
			F.platform = device.platform;
			F.uuid = device.uuid;
		} catch (err) {
			// device feature disabled in config.xml
		}
		
		$.ajaxSetup({
			cache: false
		});
		
		var url = window.location.href.replace(F.reTailUrl, "$1/files.json");
		$.ajax({
			dataType: "json",
			url: url,
			success: function (data) {
				try {
					F.currentFiles = data;
					F.getEnv();
				} catch (err) {
					F.error("catch init currentFile", err);
				}
			},
			error: function (xhr, status, err) {
				F.error("failed to retrieve current file list: " + url, err);
			}
		});
	},
	
	getEnv: function () {
		F.debug("getEnv");
		
		F.appBase = window.location.href.replace(F.reTailUrl, "$1");
		
		var url = F.appBase + "/env.json";
		$.ajax({
			dataType: "json",
			url: url,
			success: function (data) {
				try {
					$.extend(F, data);
					F.remoteBase = F.endPoint + "/projects/" + F.projectName + "/_private/flashupdate";
					
					if (F.firstLaunch) {
						$("#main").show();
					}
					
					F.getFlashUpdateDir();
				} catch (err) {
					F.error("catch init env", err);
				}
			},
			error: function (xhr, status, err) {
				F.error("failed to retrieve env.json file: " + url, err);
			}
		});
	},
	
	getFlashUpdateDir: function () {
		F.debug("getFlashUpdateDir");
		
		window.requestFileSystem(LocalFileSystem.PERSISTENT, 0, function (fileSystem) {
			try {
				F.debug("getDirectory flashupdate");				
				fileSystem.root.getDirectory("flashupdate", {create: true}, function (flashUpdateDir) {
					F.flashUpdateDir = flashUpdateDir;
					F.localBase = flashUpdateDir.toURL();
					
					if (!F.localBase) {
						F.localBase = flashUpdateDir.fullPath;
					}
					
					F.webLocalBase = flashUpdateDir.nativeURL ? flashUpdateDir.nativeURL : F.localBase;
					if (!F.webLocalBase) {
						F.webLocalBase = flashUpdateDir.fullPath;
					}
					
					F.localBase = F.localBase.replace(new RegExp("/$"), "");
					F.webLocalBase = F.webLocalBase.replace(new RegExp("/$"), "");
					
					if (F.isLocal) {
						F.isFlashUpdate();
					} else {
						F.hasLocal();
					}
				});
			} catch (err) {
				F.error("getDirectory flashupdate failed", err);
			}
		}, function (err) {
			F.error("requestFileSystem failed", err);
		});
	},
 	
	hasLocal: function () {
		F.debug("hasLocal: check for " + F.localBase + "/files.json");
		
		$.ajax({
			dataType: "json",
			url: F.localBase + "/files.json",
			success: function (data) {
				try {
					F.isLocalNewer(data);
				} catch (err) {
					F.error("catch hasLocal success", err);
				}
			},
			error: function (xhr, status, err) {
				try {
					F.isFlashUpdate();
				} catch (err) {
					F.error("catch hasLocal error", err);
				}
			}
		});
	},
	
	isLocalNewer: function (files) {
		F.debug("isLocalNewer");
		
		if (F.currentFiles.date < files.date) {
			F.redirectLocal();
		} else {
			F.isFlashUpdate();
		}
	},
	
	redirectLocal: function () {
		F.debug("redirectLocal");
		
		var env = {
			applicationId: F.applicationId,
			applicationName: F.applicationName,
			projectName: F.projectName,
			endPoint: F.endPoint,
			firstLaunch: F.firstLaunch,
			isLocal: true
		};
		
		F.write("env.json", JSON.stringify(env), function () {
			F.debug("env.json written");
			
			if (F.isLocal) {
				window.location.reload();
			} else {
				var filesToCopy = ["cordova.js", "cordova_plugins.js"];
				if (typeof (cordova.define) != "undefined") {
					$.each(cordova.define.moduleMap["cordova/plugin_list"].exports, function (index, plugin) {
						filesToCopy.push(plugin.file);
					});
				}
				
				F.copyCordovaFiles(filesToCopy, function () {
					F.debug("all cordova files writen");
					window.location.href = F.webLocalBase + "/index.html";				
				});
			}
		});
	},
	
	copyCordovaFiles: function (files, success) {
		if (files.length) {
			var file = files.shift();
			
			$.ajax({
				dataType: "text",
				url: F.appBase + "/" + file,
				success: function (text) {
					try {
						F.write(file, text, function () {
							F.copyCordovaFiles(files, success);
						}, function (err) {
							F.error("write failed", err);
						});
					} catch (err) {
						F.error("catch redirectLocal success", err);
					}
				},
				error: function (xhr, status, err) {
					F.error("failed cordova.js", err);
				}
			});
		} else {
			success();
		}
	},
	
	isFlashUpdate: function () {
		F.debug("isFlashUpdate");
		
		$("#checkingUpdate").show();
		
		if (F.remoteFiles == null) {
			$(".dataProjectName").text(F.applicationName);
			
			$.ajax({
				dataType: "json",
				url: F.endPoint + "/admin/services/mobiles.GetResources",
				data: {
					application: F.projectName,
					platform: F.platform,
					uuid: F.uuid
				},
				success: function (data) {
					try {
						F.remoteFiles = data;
						F.isFlashUpdate();
					} catch (err) {
						F.error("catch isFlashUpdate success", err);
					}
				},
				error: function (xhr, status, err) {
					F.debug("error: mobiles.GetResources " + err);
					F.redirectApp();
				},
				timeout: F.timeout
			});
		} else {
			$("#checkingUpdate").hide();
			
			if (F.currentFiles.lightBuild) {
				if (F.remoteFiles.flashUpdateEnabled) {
					F.doUpdate();
				} else {
					F.error("Application is in light build mode and the flashupdate is disabled !")
				}
			} else if (F.remoteFiles.flashUpdateEnabled) {
				F.isRemoteNewer();
			} else {
				F.redirectApp();
			}
		}
	},
	
	isRemoteNewer: function () {
		F.debug("isRemoteNewer currentFiles: " + F.currentFiles.date + " remoteFiles: " + F.remoteFiles.date);
		
		if (F.currentFiles.date < F.remoteFiles.date) {
			if (!F.firstLaunch) {
				$("#main").show();
			} else {
				F.firstLaunch = false;
			}
			if (F.remoteFiles.requireUserConfirmation) {
				F.requireUserConfirmation();
			} else {
				F.doUpdate();
			}
		} else {
			F.redirectApp();
		}
	},
	
	requireUserConfirmation: function () {
		F.debug("requireUserConfirmation");
		
		$("#requireUserConfirmation").one(F.clickEvent, "#requireUserConfirmationYes, #requireUserConfirmationNo", function () {
			try {
				if ($(this).val() == "yes") {
					$("#requireUserConfirmation").hide();
					F.doUpdate();
				} else {
					F.redirectApp();
				}
			} catch (err) {
				F.error("catch requireUserConfirmation", err);
			}
		}).show();
	},
	
	doUpdate: function () {
		F.debug("doUpdate");
		
		if (F.isLocal) {
			F.doRemoveUnexisting();
		} else {
			F.downloadFiles();
		}
	},
	
	redirectApp: function () {
		F.debug("redirectApp");
		
		window.location.href = window.location.href.replace(F.reTailUrl, "$1/app.html");
	},
	
	filesIndexer: function (files) {
		var i, file;
		var indexedFiles = {};
		for (i = 0; i < files.length; i++) {
			file = files[i];
			indexedFiles[file.uri] = file;
		}
		return indexedFiles;
	},
	
	doRemoveUnexisting: function () {
		F.debug("doRemoveUnexisting");
		
		var indexedFiles = F.filesIndexer(F.remoteFiles.files);
		var i, curFile = 0;
		
		var checkDone = function () {
			try {
				if ((curFile++) == F.currentFiles.files.length) {
					F.downloadFiles();
				}
			} catch (err) {
				F.error("catch doRemoveUnexisting checkDone", err);
			}
		}
		
		$.each(F.currentFiles.files, function (i, file) {
			var remoteFile = indexedFiles[file.uri];
			if (typeof(remoteFile) == "undefined") {
				F.debug("try delete " + file.uri);
				F.flashUpdateDir.getFile(file.uri.substring(1), {create: false, exclusive: false}, function (fileEntry) {
					F.debug("delete file " + file.uri);
					try {
						fileEntry.remove(function () {
							F.debug("delete DONE " + file.uri);
							checkDone();
						}, function () {
							F.debug("delete FAIL " + file.uri);
							checkDone();
						});
					} catch (err) {
						F.debug("delete exception (" + err + ") " + file.uri);
						checkDone();
					}
				}, function () {
					F.debug("not existing " + file.uri);
					checkDone();
				});
			} else {
				checkDone();
			}
		});
		checkDone();
	},
	
	downloadFiles: function () {
		F.debug("downloadFiles");
		
		var indexedFiles = F.filesIndexer(F.currentFiles.files);
		var curFile = 0, nbTransfert = 0, totalSize = 0, curSize = 0;
		
		$("#progress").show();
		
		var $canvas = $("#progressGauge");
		var context = $canvas[0].getContext("2d");
		context.lineWidth = 30;
		context.strokeStyle = $("#progressGaugeColor1").css("color");
		
		context.beginPath();
		context.arc(100, 100, 70, 0, 2 * Math.PI, false);
		context.stroke();
		
		context.strokeStyle = $("#progressGaugeColor2").css("color");
		
		var checkDone = function (file) {
			try {
				if (typeof(file) != "undefined") {
					curSize += file.size;
					$("#progressCur").text(Math.floor(curSize / 1000));
					context.beginPath();
					context.arc(100, 100, 70, 1.5 * Math.PI, (2 * curSize / totalSize + 1.5) * Math.PI, false);
					context.stroke();
				}
				if ((curFile++) == F.remoteFiles.files.length) {
					$("#progress").hide();
					F.downloadFinished(nbTransfert);
				}
			} catch (err) {
				F.error("catch downloadFile checkDone", err);
			}
		}
		
		$.each(F.remoteFiles.files, function (index, file) {
			var localFile = indexedFiles[file.uri];
			if (!F.isLocal || (!localFile || file.date > localFile.date || file.size != localFile.size)) {
				F.debug("FileTransfer " + file.uri);
				
				nbTransfert++;
				totalSize += file.size;
				F.mkParentDirs(file.uri, function (parentDir, fileName) {
					new FileTransfer().download(
						encodeURI(F.remoteBase + file.uri + "?" + F.startTime),
						F.localBase + file.uri,
						function () {
							checkDone(file);
						},
						function (err) {
							F.error("failed to FileTransfer ", err);
							checkDone(file);
						}
					);
				}, function () {
					F.error("failed to mkParentDirs")
				});
			} else {
				checkDone();
			}
		});
		$("#progressTotal").text(Math.floor(totalSize / 1000));
		checkDone();
	},
	
	downloadFinished: function (nbTransfert) {
		F.debug("downloadFinished");
		
		F.write("files.json", JSON.stringify(F.remoteFiles), function () {
			F.debug("files.json writen");
			
			try {
				if (nbTransfert) {
					F.redirectLocal();
				} else {
					F.redirectApp();
				}
			} catch (err) {
				F.error("catch downloadFinished", err);
			}
		}, function (err) {
			F.error(err);
		});
	},
		
	write: function (filePath, content, success, error) {
		F.debug("try to write to " + filePath);
		
		F.mkParentDirs(filePath, function (parentDir, fileName) {
			parentDir.getFile(fileName, {create: true, exclusive: false}, function (fileEntry) {
				fileEntry.createWriter(function (writer) {
					writer.onwrite = success;
					writer.write(content);
				}, function (err) {
					F.error("createWriter failed", err);
				});
			}, function (err) {
				F.error("getFile failed", err);
			});
		}, error);
	},
	
	mkParentDirs: function (filePath, success, error) {
		var dirs = filePath.split("/");
		var mkDirs = function (parentDir) {
			if (dirs.length > 1) {
				var dir = dirs.shift();
				if (dir.length) {
					parentDir.getDirectory(dir, {create: true}, mkDirs, error);
				} else {
					mkDirs(parentDir);
				}
			} else {
				success(parentDir, dirs.shift());
			}
		};
		mkDirs(F.flashUpdateDir);
	},
	
	message: function (message) {
		$("<div/>").add("message").text(message).prependTo("#messages");
	}
};

$(function () {
	if (typeof(cordova) == "undefined") {
		F.redirectApp();
	} else {
		document.addEventListener("deviceready", function() {
			try {
				F.init();
			} catch (err) {
				F.error("catch deviceready", err);
			}
		});
	}
});