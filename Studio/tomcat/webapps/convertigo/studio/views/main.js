/**
 * Equivalent to $(document).ready().
 * Source: https://github.com/jfriend00/docReady
 */
(function(funcName, baseObj) {
    "use strict";
    // The public function name defaults to window.docReady
    // but you can modify the last line of this function to pass in a different object or method name
    // if you want to put them in a different namespace and those will be used instead of 
    // window.docReady(...)
    funcName = funcName || "docReady";
    baseObj = baseObj || window;
    var readyList = [];
    var readyFired = false;
    var readyEventHandlersInstalled = false;
    
    // call this when the document is ready
    // this function protects itself against being called more than once
    function ready() {
        if (!readyFired) {
            // this must be set to true before we start calling callbacks
            readyFired = true;
            for (var i = 0; i < readyList.length; i++) {
                // if a callback here happens to add new ready handlers,
                // the docReady() function will see that it already fired
                // and will schedule the callback to run right after
                // this event loop finishes so all handlers will still execute
                // in order and no new ones will be added to the readyList
                // while we are processing the list
                readyList[i].fn.call(window, readyList[i].ctx);
            }
            // allow any closures held by these functions to free
            readyList = [];
        }
    }
    
    function readyStateChange() {
        if ( document.readyState === "complete" ) {
            ready();
        }
    }
    
    // This is the one public interface
    // docReady(fn, context);
    // the context argument is optional - if present, it will be passed
    // as an argument to the callback
    baseObj[funcName] = function(callback, context) {
        if (typeof callback !== "function") {
            throw new TypeError("callback for docReady(fn) must be a function");
        }
        // if ready has already fired, then just schedule the callback
        // to fire asynchronously, but right away
        if (readyFired) {
            setTimeout(function() {callback(context);}, 1);
            return;
        } else {
            // add the function and context to the list
            readyList.push({fn: callback, ctx: context});
        }
        // if document already ready to go, schedule the ready function to run
        // IE only safe when readyState is "complete", others safe when readyState is "interactive"
        if (document.readyState === "complete" || (!document.attachEvent && document.readyState === "interactive")) {
            setTimeout(ready, 1);
        } else if (!readyEventHandlersInstalled) {
            // otherwise if we don't have event handlers installed, install them
            if (document.addEventListener) {
                // first choice is DOMContentLoaded event
                document.addEventListener("DOMContentLoaded", ready, false);
                // backup is window load event
                window.addEventListener("load", ready, false);
            } else {
                // must be IE
                document.attachEvent("onreadystatechange", readyStateChange);
                window.attachEvent("onload", ready);
            }
            readyEventHandlersInstalled = true;
        }
    }
})("docReady", window);

var Main = {
	url: {
		baseUrl: null,
		baseUrlConvertigo: null,
		baseUrlConvertigoStudio: null,
		baseUrlConvertigoServices: null
	},
	init: function (baseUrl, authUserName, authPassword) {
		// Initialize URLs
		this.url.baseUrl = baseUrl;
		this.url.baseUrlConvertigo = this.url.baseUrl + "convertigo/";
		this.url.baseUrlConvertigoStudio = this.url.baseUrlConvertigo + "studio/";
		this.url.baseUrlConvertigoServices = this.url.baseUrlConvertigo + "admin/services/";
		
		this.defineScripts();
		
		var that = this;
		
		require([
			"database_object_manager",
			"projects",
			"information",
			"injector",
			"response_action_manager",
			"string_utils",
			"jquery",
			"modal",
			"jquery.modal",
			"jstree",
			"jstreeutils",
			"jstreegrid"
		], function () {
			// Define AJAX setup
			$.ajaxSetup({
				type: "POST",
				dataType: "xml",
				xhrFields: {
					withCredentials: true
				}
			});
			
			that.authenticate(authUserName, authPassword, function () {
				Injector.injectLinkStyle(that.url.baseUrlConvertigoStudio + "css/jstree/themes/default-dark/style.min.css");
				Injector.injectLinkStyle(that.url.baseUrlConvertigoStudio + "css/style.css");
				Injector.injectLinkStyle(that.url.baseUrlConvertigoStudio + "css/jquery.modal.min.css");
				
				// Inject CSS the icons of each type of nodes
				Injector.injectLinkStyle(that.createConvertigoServiceUrl("studio.database_objects.GetCSS"));
				Injector.injectLinkStyle(that.createConvertigoServiceUrl("studio.database_objects.GetMenuIconsCSS"));
								
				var projectsView = new ProjectsView(".projectsView");
				
				// Property view jstree
				PropertiesView.init(".informationView");
				DatabaseObjectManager.addListener(projectsView);
				DatabaseObjectManager.addListener(PropertiesView);
				
				that.checkAuthentication();
			});
		});
	},
	defineScripts: function () {
		// All scripts are defined here
		require.config({
		    paths: {
		    	database_object_manager: this.url.baseUrlConvertigoStudio + "views/database-object-manager",
		        projects: this.url.baseUrlConvertigoStudio + "views/projects",
		        information: this.url.baseUrlConvertigoStudio + "views/information",
		        response_action_manager: this.url.baseUrlConvertigoStudio + "views/response-action-manager",
		        string_utils: this.url.baseUrlConvertigoStudio + "js/string-utils",
		        injector: this.url.baseUrlConvertigoStudio + "js/injector",
		        jquery: this.url.baseUrlConvertigo + "scripts/jquery-2.1.4",
		        "jquery.modal": this.url.baseUrlConvertigoStudio + "js/jquery.modal.min",
		        modal: this.url.baseUrlConvertigoStudio + "views/modal",
		        jstree: this.url.baseUrlConvertigoStudio + "js/jstree/jstree-3.3.3.min",
		        jstreeutils: this.url.baseUrlConvertigoStudio + "js/jstree/jstreeutils",
		        jstreegrid: this.url.baseUrlConvertigoStudio + "js/jstree/jstreegrid-3.5.14"
		    },
		    shim: {
		        "jquery.modal": ["jquery"]
		    }
		});
	},
	authenticate: function (userName, password, callback) {
		$.ajax({
			url: Main.createConvertigoServiceUrl("engine.Authenticate"),
			data: {
				authUserName: userName,
				authPassword: password,
				authType: "login"
			},
			success: function (data, textStatus, jqXHR) {
				if ($(data).find("role[name='AUTHENTICATED']").length !== 0) {
					callback();
				}
			}
		});
	},
	checkAuthentication: function () {
		$.ajax({
			type: "POST",
			url: Main.createConvertigoServiceUrl("engine.CheckAuthentication"),
			dataType: "xml",
			data: {},
			success: function (xml) {
				var $xml = $(xml);
				var $authenticated = $xml.find("authenticated");
				if ($authenticated.text() == "true") {
					setTimeout(function() {
						Main.checkAuthentication();
					}, 5000);
				} else {
				}
			},
			error: function (xhr, ajaxOptions, thrownError) {
				if (xhr.status == 503) {
				}
			},
	        global: false
		});
	},
	createConvertigoServiceUrl: function (serviceName) {
		return this.url.baseUrlConvertigoServices + serviceName;
	},
};


docReady(function () {
	Main.init("http://localhost:18080/", "admin", "admin");
	
	
});
