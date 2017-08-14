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
	ajaxCall: function (httpMethod, url, dataType, data, successFunction, errorFunction, extra) {
	    var parameters = {
            type: httpMethod,
            url: url,
            dataType: dataType,
            data: data,
            success: successFunction,
            error: function (XMLHttpRequest, typeError, extra) {
                ModalUtils.createStackStraceMessageDialog(
                    "Convertigo",
                    $(XMLHttpRequest.responseXML).find("message").text(),
                    $(XMLHttpRequest.responseXML).find("error>stacktrace").text()
                );
            },
            beforeSend: function (jqXHR, settings) {
            }
        };
        if (typeof(extra) !== undefined) {
            $.extend(parameters, extra)
        }
        return $.ajax(parameters);
	},
	callService: function (serviceName, successFunction, parameters, errorFunction, extra) {
	    return Convertigo.ajaxCall("POST", Convertigo.createServiceUrl(serviceName), "xml", parameters, successFunction, errorFunction, extra);
	},
	callJSONService: function (serviceName, successFunction, parameters, errorFunction, extra) {
	    return Convertigo.ajaxCall("POST", Convertigo.createServiceUrl(serviceName), "json", parameters, successFunction, errorFunction, extra);
	},
	createServiceUrl: function (serviceName) {
		return this.url.baseConvertigoAdminServicesUrl + serviceName;
	},
	authenticate: function (authUserName, authPassword, callback) {
	    Convertigo.callService(
            "engine.Authenticate",
            function (data, textStatus, jqXHR) {
                if ($(data).find("role[name='AUTHENTICATED']").length !== 0) {
                    callback();
                }
            }, {
                authUserName: authUserName,
                authPassword: authPassword,
                authType: "login"
            }
        );
	},
	checkAuthentication: function (everyMs = 180000 /* 3 minutes */) {
		var that = this;

		Convertigo.callService(
	        "engine.CheckAuthentication",
	        function (xml) {
                var $xml = $(xml);
                var $authenticated = $xml.find("authenticated");
                if ($authenticated.text() == "true") {
                    // Recall check authentication each everyMs ms
                    setTimeout(function() {
                        that.checkAuthentication();
                    }, everyMs);
                }
            }
	    );
	},
	getBaseConvertigoUrl: function (childUrl = "") {
		return this.url.baseConvertigoUrl + childUrl;
	},
	getBaseConvertigoStudioUrl: function (childUrl = "") {
		return this.url.baseConvertigoStudioUrl + childUrl;
	}
};
