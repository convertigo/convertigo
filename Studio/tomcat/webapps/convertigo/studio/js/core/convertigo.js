var Convertigo = {
	url: {
		baseUrl: null,
		baseConvertigoUrl: null,
		baseConvertigoStudioUrl: null,
		baseConvertigoAdminServicesUrl: null
	},
	init: function (baseUrl) {
		// Initialize URLs
		this.url.baseUrl = baseUrl;
		this.url.baseConvertigoUrl = this.url.baseUrl + "/convertigo/";
		this.url.baseConvertigoStudioUrl = this.url.baseConvertigoUrl + "studio/";
		this.url.baseConvertigoAdminServicesUrl = this.url.baseConvertigoUrl + "admin/services/";
	},
	createServiceUrl: function (serviceName) {
		return this.url.baseConvertigoAdminServicesUrl + serviceName;
	},
	authenticate: function (authUserName, authPassword, callback) {
		$.ajax({
		    dataType: "xml",
			url: this.createServiceUrl("engine.Authenticate"),
			data: {
				authUserName: authUserName,
				authPassword: authPassword,
				authType: "login"
			},
			success: function (data, textStatus, jqXHR) {
				if ($(data).find("role[name='AUTHENTICATED']").length !== 0) {
					callback();
				}
			}
		});
	},
	checkAuthentication: function (everyMs = 10000) {
		var that = this;
		$.ajax({
		    dataType: "xml",
			url: that.createServiceUrl("engine.CheckAuthentication"),
			success: function (xml) {
				var $xml = $(xml);
				var $authenticated = $xml.find("authenticated");
				if ($authenticated.text() == "true") {
					// Recall check authentication each everyMs ms
					setTimeout(function() {
						that.checkAuthentication();
					}, everyMs);
				}
			},
			error: function (xhr, ajaxOptions, thrownError) {
			}
		});
	},
	getBaseConvertigoUrl: function (childUrl = "") {
		return this.url.baseConvertigoUrl + childUrl;
	},
	getBaseConvertigoStudioUrl: function (childUrl = "") {
		return this.url.baseConvertigoStudioUrl + childUrl;
	}
};
