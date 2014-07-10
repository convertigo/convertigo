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
	canCopyFromApp: false,
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
	platformName: null,
	localBase: null,
	webLocalBase: null,
	appBase: null,
	fsProtocol: null,
	timeout: 0,
	firstLaunch: true,
	cordovaVersion: null,
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
		
		if (typeof(FILESYSTEM_PROTOCOL) != "undefined") {
			F.fsProtocol = FILESYSTEM_PROTOCOL;
			F.debug("fsProtocol: " + F.fsProtocol);
		}
		
		if (typeof(cordova.version) != "undefined") {
			F.cordovaVersion = cordova.version;
			F.debug("cordovaVersion: " + F.cordovaVersion);
		}
		
		try {
			F.platform = device.platform;
			F.uuid = device.uuid;
			
			if ((F.platform == "Android" && F.cordovaVersion) || F.platform == "blackberry10" || F.platform == "Win32NT") {
				F.canCopyFromApp = true;
			}
		} catch (err) {
			// device feature disabled in config.xml
		}
		
		if (F.platform == "blackberry10") {
			// unsupported platform
			F.redirectApp();
		} else {
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
		}
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
		var quota = F.platform == "blackberry10" ? Math.pow(1024, 3) : 0;
		
		window.requestFileSystem(LocalFileSystem.PERSISTENT, quota, function (fileSystem) {
			try {
				var fuPath = "flashupdate";
				
				if (!F.fsProtocol && F.platform == "Android") {
					fuPath = "/data/data/" + F.applicationId + "/flashupdate";
				}
				
				F.debug("getDirectory " + fuPath);
							
				fileSystem.root.getDirectory(fuPath, {create: true}, function (flashUpdateDir) {
					F.flashUpdateDir = flashUpdateDir;
					
					if (F.fsProtocol || F.platform == "blackberry10") {
						F.localBase = flashUpdateDir.toURL();
					}
					
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
					if (F.canCopyFromApp) {
						F.downloadFiles(true);
					} else {
						F.isFlashUpdate();
					}
				} catch (err) {
					F.error("catch hasLocal error", err);
				}
			}
		});
	},
	
	isLocalNewer: function (files) {
		F.debug("isLocalNewer");
		
		if (F.currentFiles.date <= files.date) {
			F.redirectLocal();
		} else {
			if (F.canCopyFromApp) {
				F.doRemoveUnexisting(true, files.files);
			} else {
				F.isFlashUpdate();
			}
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
			remoteBase: F.remoteBase,
			platformName: F.platformName,
			timeout: F.timeout,
			isLocal: true
		};
		
		F.write("env.json", JSON.stringify(env), function () {
			F.debug("env.json written");
			
			if (F.isLocal) {
				window.location.reload();
			} else {
				var filesToCopy = ["cordova.js"];
				if (F.cordovaVersion) {
					filesToCopy.push("cordova_plugins.js");
					if (typeof (cordova.define) != "undefined") {
						$.each(cordova.define.moduleMap["cordova/plugin_list"].exports, function (index, plugin) {
							filesToCopy.push(plugin.file);
						});
					}
				}
				
				F.copyCordovaFiles(filesToCopy, function () {
					F.debug("all cordova files writen");
					window.location.href = F.webLocalBase + "/index.html";				
				});
			}
		});
	},
	
	copyCordovaFiles: function (files, success) {
		var curFile = 0;
		
		var checkDone = function () {
			try {
				if ((++curFile) == files.length) {
					success();
				}
			} catch (err) {
				F.error("catch copyCordovaFiles checkDone", err);
			}
		}
		
		$.each(files, function (index, file) {
			$.ajax({
				dataType: "text",
				url: F.appBase + "/" + file,
				success: function (text) {
					try {
						F.write(file, text, function () {
							checkDone();
						}, function (err) {
							F.error("write failed: " + file, err);
						});
					} catch (err) {
						F.error("catch copyCordovaFiles ajax success: " + file, err);
					}
				},
				error: function (xhr, status, err) {
					F.error("failed copyCordovaFiles ajax: " + file, err);
				}
			});
		});
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
					project: F.projectName,
					platform: F.platformName,
					platformDetected: F.platfom,
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
			F.doRemoveUnexisting(false);
		} else {
			F.downloadFiles(false);
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
	
	doRemoveUnexisting: function (fromApp, files) {
		F.debug("doRemoveUnexisting");
		
		var indexedFiles = fromApp ? {} : F.filesIndexer(F.remoteFiles.files);
		var i, curFile = 0;
		
		if (typeof(files) == "undefined") {
			files = F.currentFiles.files;
		}
		
		var checkDone = function () {
			try {
				if ((curFile++) == files.length) {
					F.downloadFiles(fromApp);
				}
			} catch (err) {
				F.error("catch doRemoveUnexisting checkDone", err);
			}
		}
		
		$.each(files, function (i, file) {
			var remoteFile = indexedFiles[file.uri];
			if (typeof(remoteFile) == "undefined") {
				F.debug("try delete " + file.uri);
				F.flashUpdateDir.getFile(file.uri, {create: false, exclusive: false}, function (fileEntry) {
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
	
	downloadFiles: function (fromApp) {
		F.debug("downloadFiles");
		
		var indexedFiles = F.filesIndexer(F.currentFiles.files);
		var curFile = 0, nbTransfert = 0, totalSize = 0, curSize = 0;

		if (fromApp) {
			F.remoteFiles = F.currentFiles;
		} else {
			$("#progress").show();
		}
		
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
					var source = (fromApp ? F.appBase : F.remoteBase) + "/" + file.uri + "?" + F.startTime;
					var destination = F.localBase + "/" + file.uri;
					new FileTransfer().download(
						encodeURI(source),
						destination,
						function () {
							checkDone(file);
						},
						function (err) {
							if (file.uri != "config.xml") {
								F.error("failed to FileTransfer '" + source + "' to '" + destination + "'", err);
							}
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
		F.mkParentDirs(filePath, function (parentDir, fileName) {
			parentDir.getFile(fileName, {create: true, exclusive: false}, function (fileEntry) {
				fileEntry.createWriter(function (writer) {
					writer.onwrite = success;
					writer.onerror = error;
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
				navigator.splashscreen.hide();
				F.init();
			} catch (err) {
				F.error("catch deviceready", err);
			}
		});
	}
});