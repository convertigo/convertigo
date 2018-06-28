/*
 * Copyright (c) 2001-2018 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

function store_init() {
	$("#helpDownload").attr("href", getHelpUrl("download-the-store/"));
	$("#helpUpload").attr("href", getHelpUrl("upload-a-custom-store/"));
	$("#helpDelete").attr("href", getHelpUrl("delete-the-custom-store/"));
	
	$("#btnDownloadStore").button({
		icons : {
			primary : "ui-icon-arrowthick-1-s"
		}
	}).click(function () {
		downloadStoreDialog()
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
	}).click(function () {
		showConfirm("Are you sure you want to delete the custom Store ?", function () {
			startWait(50);
			callService("store.DeleteCustomStore", function (res) {
				setTimeout(function () {
					endWait()
					showInfo(res.querySelector("message").innerHTML);
				}, 1000);
			});
		});
	});
	
	// Upload custom store
	new AjaxUpload("btnUploadStore", {
		action : "services/store.UploadCustomStore",			
		onSubmit : function(file, extension) {			
			this._settings.action = "services/store.UploadCustomStore";
			var str = ".zip";
			if (file.match(str + "$") != str) {
				showError("<p>The custom Store '" + file + "' is not a valid archive (*.zip)</p>");
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
	
	// Select all checkbox
	$("#cbAll").click(function () {
		$(":checkbox:not(this)").prop("checked", $(this).prop("checked"));
	});
}

function store_update() {
}

function downloadStoreDialog() {
	$("#dialogDownloadStore").dialog({
		autoOpen : true,
		title : "Select the resources to download",
		modal : true,
        buttons: [
        {
            text: "Download",
            click: function () {
            	// At least one checkbox is checked
            	if ($(":checkbox:checked:not(#cbAll)").length > 0) {
	            	$("#dialogDownloadStore").submit();
	                $(this).dialog("close");
            	}
            	else {
            		showInfo("Please select at least one element.")
            	}
            }
        },
        {
            text: "Cancel",
            click: function () {
                    $(this).dialog("close");
            }
        }]
	});
}

function getHelpUrl(help_sub_url) {
	return "http://www.convertigo.com/document/latest/operating-guide/using-convertigo-administration-console/store/" + help_sub_url;
}
