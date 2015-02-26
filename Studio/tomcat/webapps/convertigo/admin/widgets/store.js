function store_init() {
	$("#btnDownloadStore").button({
		icons : {
			primary : "ui-icon-arrowthick-1-s"
		}
	});
	
	$("#btnUploadStore").button({
		icons : {
			primary : "ui-icon-arrowthick-1-n"
		}
	});
	
	$("#btnDeleteStore").button({
		icons : {
			primary : "ui-icon-trash"
		}
	});
	
	// Upload custom store
	new AjaxUpload("btnUploadStore", {
		action : "services/store.UploadCustomStore",			
		onSubmit : function(file, extension) {			
			this._settings.action = "services/store.UploadCustomStore";
			var str = ".zip";
			if (file.match(str + "$") != str) {
				showError("<p>The custom store '" + file + "' is not a valid archive (*.zip)</p>");
				return false;
			}
	
			startWait(50);
		},
		onComplete : function(file, response) {
			clearInterval(this.tim_progress);
			endWait();
			if ($(response).find("error").length > 0) {
				showError("<p>An unexpected error has occured.</p>", $(response).text());
			} else {
				showInfo($(response).text());
				$("").dialog("close");
			}
		}
	});
}

function store_update() {
}