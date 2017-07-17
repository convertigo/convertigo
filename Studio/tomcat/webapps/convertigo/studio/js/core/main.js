var Main = {
    sheet: null,
	init: function (authUserName, authPassword) {
		this.defineScripts();

        var that = this;
		require([
			/**
			 * Libs
			 */
		    // jquery
			"jquery",
			"jstree",
			"jstreegrid",
			"jstreeutils",
			"jquery.modal",
	        "jquery.sse",
            "attrchange",
            "goldenlayout",

            // others
			"accordion",
			"prism",

            /**
             * Editors
             */
			// Sequence
            "sequence-editor",

			// Connnectors
            "cicsconnector-editor",
            "couchdbconnector-editor",
            "fullsyncconnector-editor",
            "htmlconnector-editor",
            "httpconnector-editor",
            "javelinconnector-editor",
            "proxyhttpconnector-editor",
            "sapjcoconnector-editor",
            "siteclipperconnector-editor",
            "sqlconnector-editor",

			/**
             * Listeners
             */
            "c8o-server-events-listener",
            "gwt-events-listener",

	    	/**
	    	 * Managers
	    	 */
	    	"database-object-manager",
	        "response-action-manager",

	        /**
	         * Tabs
	         */
	    	"tab",
	    	"palette",
	    	"references",
	    	"source-picker",

            /**
             * Toolbars
             */
             "toolbar",
             "enginelog-toolbar",
             "projects-toolbar",

	        /**
	         * Utils
	         */
            "dom-utils",
            "golden-layout-utils",
	        "style-utils",
	        "modal-utils",
	        "string-utils",
	        "variable-utils",

	        /**
	         * Views
	         */
	        "tree-view-container",
	        "enginelog",
	        "properties",
	        "projects"
		], function () {
		    // Current Che theme that can be changed in preferences
		    var isCheDarkTheme = that.isCheDarkTheme();

		    var theme = "default";
		    if (isCheDarkTheme) {
		        theme += "-dark"
		    }

			// Inject CSS
	        StyleUtils.injectLinkStyle(Convertigo.getBaseConvertigoStudioUrl("css/jquery/jstree-3.3.3/themes/" + theme + "/style.min.css"));
			StyleUtils.injectLinkStyle(Convertigo.getBaseConvertigoStudioUrl("css/jquery/jquery.modal-0.8.0.min.css"));
			StyleUtils.injectLinkStyle(Convertigo.getBaseConvertigoStudioUrl("css/accordion.css"));
	        StyleUtils.injectLinkStyle(Convertigo.getBaseConvertigoStudioUrl("css/prism-1.6.0/themes/" + theme + ".css"));
			StyleUtils.injectLinkStyle(Convertigo.getBaseConvertigoStudioUrl("css/style.css"));
			StyleUtils.injectLinkStyle(Convertigo.getBaseConvertigoStudioUrl("css/themes/" + theme + ".css"));
			StyleUtils.injectLinkStyle(Convertigo.getBaseConvertigoStudioUrl("css/jquery/golden-layout-1.5.8/goldenlayout-base.css"));
		    StyleUtils.injectLinkStyle(Convertigo.getBaseConvertigoStudioUrl("css/jquery/golden-layout-1.5.8/themes/" + theme + ".css"));

			// To iterate in reverse order
			jQuery.fn.reverse = [].reverse;

			// Define AJAX setup
			$.ajaxSetup({
				type: "POST",
				xhrFields: {
					withCredentials: true
				}
			});

			// To add CSS rules dynamically
			that.sheet =
			    $("<style/>", {
    			    "id": "dynamic-style"
    			}).appendTo("head:first").get(0).sheet;

			// Connect to the Convertigo server
			Convertigo.authenticate(authUserName, authPassword, function () {
				// Inject CSS that needed an authentification to the Convertigo server
				StyleUtils.injectLinkStyle(Convertigo.createServiceUrl("studio.database_objects.GetMenuIconsCSS"));
				StyleUtils.injectLinkStyle(Convertigo.createServiceUrl("studio.database_objects.GetPaletteIconsCSS"));
				StyleUtils.injectLinkStyle(Convertigo.createServiceUrl("studio.database_objects.GetTreeIconsCSS"));

				that.initListeners();

				// All tabs
				var sourcePicker = new SourcePicker();
				var references = new References();
				var palette = new Palette();

                var propertiesView = new PropertiesView(theme);
				var projectsView = new ProjectsView(propertiesView, [palette], theme);

                // Will contain projects view + tabs
                var $mainTabViewDiv = $(".mainTabView");
                $mainTabViewDiv.css("height", "100%");
                $mainTabViewDiv.parent().css("height", "100%");

                var enginelogView = new EngineLogView();

				var $secondTabDiv = $(".secondTabView");
				$secondTabDiv.css("height", "100%");
				$secondTabDiv.parent().css("height", "100%");

				// Extract all views from a GL config
                var getViews = function (config, views) {
                	if (!views) {
                		views = {};
                	}
                	try {
                		views[config.componentState.view] = true;
                	} catch (e) {}
                	try {
                		$.each(config.content, function (i, v) {
                			getViews(v, views);
                		});
                	} catch (e) {}
                	return views;
                };

                // Define how to build our GL components
                var registerComponent = function (container, state) {
					var $elt = container.getElement();
					if (state.view == "projectsView") {
						$elt.append(projectsView.getDivWrapperTree());
					} else if (state.view == "palette") {
						$elt.append(palette.getDiv());
					} else if (state.view == "sourcePicker") {
					    $elt.append(sourcePicker.getDiv());
					} else if (state.view == "references") {
					    $elt.append(references.getDiv());
                    } else if (state.view == "propertiesView") {
						$elt.append(propertiesView.getDivWrapperTree());
					} else if (state.view == "enginelogView") {
						$elt.append(enginelogView.getDiv());
					} else {
						$elt.append("<h2>No implemented</h2>");
					}                	
                };

                // Update GL layouts
				var updateSize = function (e) {
					try {
						Convertigo.glLeft.updateSize();
					} catch (e) {}
					try {
						Convertigo.glBottom.updateSize();
					} catch (e) {}
				};

				// Init GL, load from localStorage if the config is compatible
                var initGl = function (name, $div, config) {
					window.setTimeout(function () {
	                	var localKey = name + "Config";
	                	if (localStorage[localKey]) {
	                		try {
	                			var localConfig = JSON.parse(localStorage[localKey]);
	                			var localViews = getViews(localConfig);
	                			var defaultViews = getViews(config);
	                			if (JSON.stringify(localViews) == JSON.stringify(defaultViews)) {
	                				config = localConfig;
	                			}
	                		} catch (e) {
	                			console.log("failed to load GL config: " + e);
	                		}
	                	}
	                	var gl = Convertigo[name] = new (require("goldenlayout"))(config, $div);

	                	gl.on("stateChanged", function () {
	                		if (gl.isInitialised) {
							    var state = JSON.stringify(gl.toConfig());
							    localStorage.setItem(localKey, state);
	                		}
						});

	                	gl.on("stackCreated", function (stack) {
	                	    var projectsToolbar = null;
	                	    var engineLogToolbar = null;

	                	    stack.on("activeContentItemChanged", function (contentItem) {
	                	        // Remove all buttons from the toolbar (= header)
	                	        stack.header.controlsContainer.find(".img-action").hide();

	                	        // Regenerate the correct toolbar
	                	        if (contentItem.config.title === "Projects") {
	                	            if (!projectsToolbar) {
	                	                projectsToolbar = new ProjectsToolbar(stack.header.controlsContainer, projectsView);
	                	            }
	                	            else {
	                	                stack.header.controlsContainer.find("." + projectsToolbar.getClassAction()).show();
	                	            }
	                	        }
	                	        else if (contentItem.config.title === "Engine Log") {
                                    if (!engineLogToolbar) {
                                        engineLogToolbar = new EngineLogToolbar(stack.header.controlsContainer, enginelogView);
                                    }
                                    else {
                                        stack.header.controlsContainer.find("." + engineLogToolbar.getClassAction()).show();
                                    }
	                	        }
	                	    });
	                	});

						gl.registerComponent("view", registerComponent);
						gl.init();
					}, 0);
                };

                // Register resize and init events
				$(".gwt-SplitLayoutPanel>*").attrchange({callback: updateSize});
				$(window).resize(updateSize);
				$(document)
					.on("click", "div[title='Convertigo']", updateSize)
					.one("click", "#gwt-debug-leftPanel div[title='Convertigo']", function () {
						initGl("glLeft", $mainTabViewDiv, {
							settings : {
								showPopoutIcon : false,
								showCloseIcon : false
							},
							content : [ {
								type : "row",
								content : [{
									type : "component",
									componentName : "view",
									componentState : {
										view : "projectsView"
									},
									isClosable : false,
									title : "Projects"
								}, {
									type : "stack",
									content : [{
										type : "component",
										componentName : "view",
										componentState : {
											view : "palette"
										},
										isClosable : false,
										title : "Palette"
									}, {
										type : "component",
										componentName : "view",
										componentState : {
											view : "sourcePicker"
										},
										isClosable : false,
										title : "Source Picker"
									}, {
										type : "component",
										componentName : "view",
										componentState : {
											view : "references"
										},
										isClosable : false,
										title : "References"
									} ]
								} ]
							} ]
						});
					}).one("click", "#gwt-debug-bottomPanel div[title='Convertigo']", function () {
						initGl("glBottom", $secondTabDiv, {
							settings : {
								showPopoutIcon : false,
								showCloseIcon : false
							},
							content : [{
								type : "row",
								content : [ {
									type : "component",
									componentName : "view",
									componentState : {
										view : "propertiesView"
									},
									isClosable : false,
									width : 20,
									title : 'Properties'
								}, {
									type : "component",
									componentName : "view",
									componentState : {
										view : "enginelogView"
									},
									isClosable : false,
									title : "Engine Log"
								} ]
							} ]
						});
					});

				// Automatically open Convertigo parts (only works with Che)
				$("#gwt-debug-leftPanel div[title='Convertigo']>:first-child").click();
				$("#gwt-debug-bottomPanel div[title='Convertigo']>:first-child").click();
		
				DatabaseObjectManager.addListener(projectsView);
				DatabaseObjectManager.addListener(propertiesView);

				// Call check authentication to stay authenticated
				Convertigo.checkAuthentication();
			});
		});
	},
	initListeners: function () {
	    C8OServerEventsListener.init();
	    GwtEventsListener.init();
	},
	defineScripts: function () {
		// All scripts are defined here
		require.config({
		    paths: {
		    	/**
		    	 * Libs
		    	 */
		        jquery: Convertigo.getBaseConvertigoUrl("scripts/jquery2.min"),
		        jstree: Convertigo.getBaseConvertigoStudioUrl("js/libs/jquery/jstree-3.3.3/jstree.min"),
		        jstreegrid: Convertigo.getBaseConvertigoStudioUrl("js/libs/jquery/jstree-3.3.3/plugins/jstreegrid-3.5.14"),
		        jstreeutils: Convertigo.getBaseConvertigoStudioUrl("js/libs/jquery/jstree-3.3.3/plugins/jstreeutils"),
		        "jquery.modal": Convertigo.getBaseConvertigoStudioUrl("js/libs/jquery/jquery.modal-0.8.0.min"),
	            "jquery.sse": Convertigo.getBaseConvertigoStudioUrl("js/libs/jquery/jquery.sse-0.1.3.min"),
		        accordion: Convertigo.getBaseConvertigoStudioUrl("js/libs/accordion"),
		        prism: Convertigo.getBaseConvertigoStudioUrl("js/libs/prism-1.6.0.min"),
		        goldenlayout: Convertigo.getBaseConvertigoStudioUrl("js/libs/jquery/goldenlayout-1.5.8"),
		        attrchange: Convertigo.getBaseConvertigoStudioUrl("js/libs/jquery/attrchange"),

		        /**
		         * Editors
		         */
		        // Sequence
	              "sequence-editor":  Convertigo.getBaseConvertigoStudioUrl("js/editors/sequences/sequence-editor"),

	            // Connectors
		        "cicsconnector-editor":  Convertigo.getBaseConvertigoStudioUrl("js/editors/connectors/cicsconnector-editor"),
		        "couchdbconnector-editor":  Convertigo.getBaseConvertigoStudioUrl("js/editors/connectors/couchdbconnector-editor"),
		        "fullsyncconnector-editor":  Convertigo.getBaseConvertigoStudioUrl("js/editors/connectors/fullsyncconnector-editor"),
		        "htmlconnector-editor":  Convertigo.getBaseConvertigoStudioUrl("js/editors/connectors/htmlconnector-editor"),
		        "httpconnector-editor":  Convertigo.getBaseConvertigoStudioUrl("js/editors/connectors/httpconnector-editor"),
		        "javelinconnector-editor":  Convertigo.getBaseConvertigoStudioUrl("js/editors/connectors/javelinconnector-editor"),
		        "proxyhttpconnector-editor":  Convertigo.getBaseConvertigoStudioUrl("js/editors/connectors/proxyhttpconnector-editor"),
                "sapjcoconnector-editor":  Convertigo.getBaseConvertigoStudioUrl("js/editors/connectors/sapjcoconnector-editor"),
                "siteclipperconnector-editor":  Convertigo.getBaseConvertigoStudioUrl("js/editors/connectors/siteclipperconnector-editor"),
	            "sqlconnector-editor":  Convertigo.getBaseConvertigoStudioUrl("js/editors/connectors/sqlconnector-editor"),

		        /**
		         * Listeners
		         */
	            "c8o-server-events-listener": Convertigo.getBaseConvertigoStudioUrl("js/listeners/c8o-server-events-listener"),
		        "gwt-events-listener": Convertigo.getBaseConvertigoStudioUrl("js/listeners/gwt-events-listener"),

		    	/**
		    	 * Managers
		    	 */
		    	"database-object-manager": Convertigo.getBaseConvertigoStudioUrl("js/managers/database-object-manager"),
		        "response-action-manager": Convertigo.getBaseConvertigoStudioUrl("js/managers/response-action-manager"),

		        /**
		         * Tabs
		         */
	            tab: Convertigo.getBaseConvertigoStudioUrl("js/tabs/tab"),
		    	palette: Convertigo.getBaseConvertigoStudioUrl("js/tabs/palette"),
		    	references:  Convertigo.getBaseConvertigoStudioUrl("js/tabs/references"),
		    	"source-picker": Convertigo.getBaseConvertigoStudioUrl("js/tabs/source-picker"),

		    	/**
		    	 * Toolbars
		    	 */
	             "toolbar": Convertigo.getBaseConvertigoStudioUrl("js/toolbars/toolbar"),
	             "enginelog-toolbar": Convertigo.getBaseConvertigoStudioUrl("js/toolbars/enginelog-toolbar"),
	             "projects-toolbar": Convertigo.getBaseConvertigoStudioUrl("js/toolbars/projects-toolbar"),

		        /**
		         * Utils
		         */
	            "dom-utils": Convertigo.getBaseConvertigoStudioUrl("js/utils/dom-utils"),
	            "golden-layout-utils": Convertigo.getBaseConvertigoStudioUrl("js/utils/golden-layout-utils"),
		        "style-utils": Convertigo.getBaseConvertigoStudioUrl("js/utils/style-utils"),
		        "modal-utils": Convertigo.getBaseConvertigoStudioUrl("js/utils/modal-utils"),
		        "string-utils": Convertigo.getBaseConvertigoStudioUrl("js/utils/string-utils"),
		        "variable-utils": Convertigo.getBaseConvertigoStudioUrl("js/utils/variable-utils"),

		        /**
		         * Views
		         */
	            "tree-view-container": Convertigo.getBaseConvertigoStudioUrl("js/views/tree-view-container"),
	            enginelog: Convertigo.getBaseConvertigoStudioUrl("js/views/enginelog"),
		        properties: Convertigo.getBaseConvertigoStudioUrl("js/views/properties"),
		        projects: Convertigo.getBaseConvertigoStudioUrl("js/views/projects")
		    },
		    // To resolve jQuery conflicts
		    shim: {
		        "jquery.modal": ["jquery"],
		        "jquery.sse": ["jquery"],
		        "goldenlayout": ["jquery"],
		        "attrchange": ["jquery"]
		    }
		});
	},
	isCheDarkTheme: function () {
	    return localStorage.getItem("codenvy-theme") === "DarkTheme";
	}
};

docReady(function () {
	//var baseConvertigoUrl = "http://localhost:18080/";
    var baseConvertigoUrl = localStorage.getItem("convertigoMachineUrl");
	var authUserName = "admin";
	var authPassword = "admin";

	Convertigo.init(baseConvertigoUrl);
	Main.init(authUserName, authPassword);
});
