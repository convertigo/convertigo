var Main = {
	init: function (authUserName, authPassword) {
		this.defineScripts();

		// Require order is important
		require([
            
			/**
			 * Libs
			 */
			"jquery",
			"jstree",
			"jstreegrid",
			"jstreeutils",
			"jquery-ui",
			"jquery.modal",
			"accordion",
			"attrchange",

			/**
             * Listeners
             */
            "editor-listener",

	    	/**
	    	 * Managers
	    	 */
	    	"database-object-manager",
	        "response-action-manager",

	        /**
	         * Tabs
	         */
	    	"tab",
	        "studio-tabs",
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
            "goldenlayout",
	        "injector-utils",
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
		    var isCheDarkTheme = localStorage.getItem("codenvy-theme") === "DarkTheme";

		    var theme = "default";
		    if (isCheDarkTheme) {
		        theme += "-dark"
		    }

			// Inject CSS
	        InjectorUtils.injectLinkStyle(Convertigo.getBaseConvertigoStudioUrl("css/jquery/jstree/themes/" + theme + "/style.min-3.3.3.css"));
			InjectorUtils.injectLinkStyle(Convertigo.getBaseConvertigoStudioUrl("css/jquery/jquery-ui.min-1.12.1.css"));
			InjectorUtils.injectLinkStyle(Convertigo.getBaseConvertigoStudioUrl("css/jquery/jquery.modal.min-0.8.0.css"));
			InjectorUtils.injectLinkStyle(Convertigo.getBaseConvertigoStudioUrl("css/accordion.css"));
			InjectorUtils.injectLinkStyle(Convertigo.getBaseConvertigoStudioUrl("css/style.css"));
			InjectorUtils.injectLinkStyle(Convertigo.getBaseConvertigoStudioUrl("css/theme/" + theme + ".css"));
			InjectorUtils.injectLinkStyle(Convertigo.getBaseConvertigoStudioUrl("css/jquery/goldenlayout-base-1.5.8.css"));
		    InjectorUtils.injectLinkStyle(Convertigo.getBaseConvertigoStudioUrl("css/jquery/goldenlayout-" + (isCheDarkTheme ? "dark" : "light") + "-theme-1.5.8.css"));

			// To iterate in reverse order
			jQuery.fn.reverse = [].reverse;

			// Define AJAX setup
			$.ajaxSetup({
				type: "POST",
				xhrFields: {
					withCredentials: true
				}
			});

			// Connect to the Convertigo server
			Convertigo.authenticate(authUserName, authPassword, function () {
				// Inject CSS that needed an authentification to the Convertigo server
				InjectorUtils.injectLinkStyle(Convertigo.createServiceUrl("studio.database_objects.GetMenuIconsCSS"));
				InjectorUtils.injectLinkStyle(Convertigo.createServiceUrl("studio.database_objects.GetPaletteIconsCSS"));
				InjectorUtils.injectLinkStyle(Convertigo.createServiceUrl("studio.database_objects.GetTreeIconsCSS"));

				var studioTabs = new StudioTabs();

				// Source Picker
				var sourcePicker = new SourcePicker();
				studioTabs.addTab(sourcePicker);

				// References
				var references = new References();
				studioTabs.addTab(references);

				// Palette
				var palette = new Palette();
				studioTabs.addTab(palette);

				studioTabs.renderTabs();

                var propertiesView = new PropertiesView(theme);
				var projectsView = new ProjectsView(propertiesView, [palette], theme);

                // Will contain projects view + tabs
                var $projectsViewDiv = $(".projectsView");
                $projectsViewDiv.css("height", "100%");
                $projectsViewDiv.parent().css("height", "100%");
                new ProjectsToolbar($projectsViewDiv, projectsView); 
                
                var enginelogView = new EngineLogView();
				
				var $engineLogView = $(".engineLogView:first");
				$engineLogView.css("height", "100%");
				$engineLogView.parent().css("height", "100%");
				
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
					if (state.view == 'projectsView') {
						$elt.append(projectsView.getDivWrapperTree());
					} else if (state.view == 'palette') {
						$elt.append(studioTabs.getDiv());
					} else if (state.view == 'propertiesView') {
						$elt.append(propertiesView.getDivWrapperTree());
					} else if (state.view == 'enginelogView') {
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
	                	var localKey = name + 'Config';
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
	                	
	                	gl.on('stateChanged', function() {
	                		if (gl.isInitialised) {
							    var state = JSON.stringify(gl.toConfig());
							    localStorage.setItem(localKey, state);
	                		}
						});
						
						gl.registerComponent('view', registerComponent);
						gl.init();
					}, 0);
                };
                
                // Register resize and init events
				$(".gwt-SplitLayoutPanel>*").attrchange({callback: updateSize});
				$(window).resize(updateSize);
				$(document)
					.on("click", "div[title='Projects'],div[title='Engine Log']", updateSize)
					.one("click", "div[title='Projects']", function() {
						initGl("glLeft", $projectsViewDiv, {
							settings : {
								showPopoutIcon : false,
								showCloseIcon : false
							},
							content : [ {
								type : 'row',
								content : [{
									type : 'component',
									componentName : 'view',
									componentState : {
										view : 'projectsView'
									},
									isClosable : false,
									title : 'Projects'
								}, {
									type : 'stack',
									content : [{
										type : 'component',
										componentName : 'view',
										componentState : {
											view : 'palette'
										},
										isClosable : false,
										title : 'Palette'
									}, {
										type : 'component',
										componentName : 'view',
										componentState : {
											view : 'sourcePicker'
										},
										isClosable : false,
										title : 'Source Picker'
									}, {
										type : 'component',
										componentName : 'view',
										componentState : {
											view : 'references'
										},
										isClosable : false,
										title : 'References'
									} ]
								} ]
							} ]
						});
					}).one("click", "div[title='Engine Log']", function() {
						initGl("glBottom", $engineLogView, {
							settings : {
								showPopoutIcon : false,
								showCloseIcon : false
							},
							content : [{
								type : 'row',
								content : [ {
									type : 'component',
									componentName : 'view',
									componentState : {
										view : 'propertiesView'
									},
									isClosable : false,
									width : 20,
									title : 'Properties'
								}, {
									type : 'component',
									componentName : 'view',
									componentState : {
										view : 'enginelogView'
									},
									isClosable : false,
									title : 'Engine Log'
								} ]
							} ]
						});
					});
                
				// Automatically open these tabs (only works with Che)
				$("div[title='Projects']>:first-child").click();
				$("div[title='Engine Log']>:first-child").click();

				// Open palette (for the moment)
				palette.focus();

				DatabaseObjectManager.addListener(projectsView);
				DatabaseObjectManager.addListener(propertiesView);

				// Call check authentication to stay authenticated
				Convertigo.checkAuthentication();
			});
		});
	},
	defineScripts: function () {
		// All scripts are defined here
		require.config({
		    paths: {
		    	/**
		    	 * Libs
		    	 */
		        jquery: Convertigo.getBaseConvertigoUrl("scripts/jquery2.min"),
		        jstree: Convertigo.getBaseConvertigoStudioUrl("js/libs/jquery/jstree/jstree-3.3.3.min"),
		        jstreegrid: Convertigo.getBaseConvertigoStudioUrl("js/libs/jquery/jstree/plugins/jstreegrid-3.5.14"),
		        jstreeutils: Convertigo.getBaseConvertigoStudioUrl("js/libs/jquery/jstree/plugins/jstreeutils"),
		        "jquery-ui": Convertigo.getBaseConvertigoStudioUrl("js/libs/jquery/jquery-ui.min-1.12.1"),
		        "jquery.modal": Convertigo.getBaseConvertigoStudioUrl("js/libs/jquery/jquery.modal.min-0.8.0"),
		        accordion: Convertigo.getBaseConvertigoStudioUrl("js/libs/accordion"),
	            goldenlayout: Convertigo.getBaseConvertigoStudioUrl("js/libs/jquery/goldenlayout.min-1.5.8"),
	            attrchange: Convertigo.getBaseConvertigoStudioUrl("js/libs/jquery/attrchange"),

		        /**
		         * Listeners
		         */
		        "editor-listener": Convertigo.getBaseConvertigoStudioUrl("js/listeners/editor-listener"),

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
		    	"studio-tabs": Convertigo.getBaseConvertigoStudioUrl("js/tabs/studio-tabs"),

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
		        "injector-utils": Convertigo.getBaseConvertigoStudioUrl("js/utils/injector-utils"),
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
		        "jquery-ui": ["jquery"],
		        "goldenlayout": ["jquery"],
		        "attrchange": ["jquery"]
		    }
		});
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
