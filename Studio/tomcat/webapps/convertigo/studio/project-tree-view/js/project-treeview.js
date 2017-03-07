(function () {
	// Injector
	var INJ = {
		// Inject a CSS
		injectLinkStyle: function (cssUrl, callback) {
			var linkStyleTag = document.createElement("link");
			if (callback) {
				linkStyleTag.addEventListener("load", callback);
			}
			linkStyleTag.rel = "stylesheet";
			linkStyleTag.href = cssUrl;
			document.head.appendChild(linkStyleTag);
		},
		// Inject a JS script
		injectScript: function (jsUrl, callback) {
			var scriptTag = document.createElement("script");
			if (callback) {
				scriptTag.addEventListener("load", callback);
			}
			scriptTag.src = jsUrl;
			document.head.appendChild(scriptTag);
		}
	};
	
	// Project Tree View
	var PTV = {
			// Variables
			tree: null,
			url: {
				baseUrl: null,
				baseUrlConvertigo: null,
				baseUrlConvertigoStudio: null,
				baseUrlConvertigoServices: null
			},
			delayOnJstreeScriptInjected: 250,
			callJstree: null,
			
			// Functions
			init: function (baseUrl, jstreeId) {
				// Initialize URLs
				PTV.url.baseUrl = baseUrl;
				PTV.url.baseUrlConvertigo = PTV.url.baseUrl + "convertigo/";
				PTV.url.baseUrlConvertigoStudio = PTV.url.baseUrlConvertigo + "studio/";
				PTV.url.baseUrlConvertigoServices = PTV.url.baseUrlConvertigo + "admin/services/";
				
				// Inject CSS
				INJ.injectLinkStyle(PTV.url.baseUrlConvertigoStudio + "project-tree-view/css/jstree/themes/default-dark/style.min.css");
				INJ.injectLinkStyle(PTV.createConvertigoServiceUrl("studio.database_objects.GetCSS"));
				INJ.injectLinkStyle(PTV.url.baseUrlConvertigoStudio + "project-tree-view/css/style.css");
				
				// Load jQuery library
				INJ.injectScript(PTV.url.baseUrlConvertigo + "scripts/jquery-2.1.4.js", function () {
					// Inject jstree library
					INJ.injectScript(PTV.url.baseUrlConvertigoStudio + "project-tree-view/js/jstree/jstree.min.js", function () {
						PTV.onJstreeScriptInjected(jstreeId);
					});
				});	
			},
			// Add a new node type for jstree
			addJstreeNodeType: function (classname) {
				// Replace all "." by "-"
				var nodeType = classname.replace(/\./g, "-");
				
				// Add the node type and specify the icon for these nodes
				PTV.callJstree.settings.types[nodeType] = {
					icon: nodeType
				}
				
				return nodeType;
			},
			createConvertigoServiceUrl: function (serviceName) {
				return PTV.url.baseUrlConvertigoServices + serviceName;
			},
			createNodeJsonDbo: function (dboElt) {
				return {
					text: $(dboElt).attr("name"),
					qname: $(dboElt).attr("qname"),
					// Add new type
					type: PTV.addJstreeNodeType($(dboElt).attr("classname")),
					// When the node is created, it is not clicked yet
					clicked: false
				}
			},
			createProjectNodes: function () {
				// Get list of projects
				$.ajax({
					url: PTV.createConvertigoServiceUrl("studio.database_objects.Get"),
					success: function (data, textStatus, jqXHR) {
						// Create a node for each project
						$(data).find("admin>dbo").each(function () {
							// Create the node
							PTV.callJstree.create_node(null, PTV.createNodeJsonDbo(this));					
						});
					}
				});
			},
			handleSelectNodeEvent: function (event, selectedNode) {
				if (!selectedNode.node.original.clicked) {
					selectedNode.node.original.clicked = true;
					
					// Dynamically get children of the selected node
					$.ajax({
						url: PTV.createConvertigoServiceUrl("studio.database_objects.Get"),
						data: {
							qname: selectedNode.node.original.qname
						},
						success: function (data, textStatus, jqXHR) {
							var categories = [];
							$(data).find("admin>dbo>dbo").each(function () {
								var category = $(this).attr("category");
								
								// Check if category (=Steps, Connector, Sequence, etc.) exists
								if (!categories[category]) {
									var categoryPlural = category + "s";
									// Check if one of the parents of the selected node is a category
									var parentCategoryFound = selectedNode.node.parents.find(function (parent) {
										return categoryPlural === PTV.callJstree.get_text(parent);
									});

									// If category not found, we create it
									if (!parentCategoryFound) {
										categories[category] =
											PTV.callJstree.create_node(
												selectedNode.node, {
													text: categoryPlural,
													clicked: true
												}
											);
									}
								}
								
								// Create node
								PTV.callJstree.create_node(
									// Choose the right parent : the category or the selected node
						    		categories[category] ? categories[category] : selectedNode.node,
							        PTV.createNodeJsonDbo(this),
							        "last",
							        // Expand node when created
							        function () {
						    			PTV.callJstree.open_node(selectedNode.node);
							        }
							    );
							});
						}
					});
				}
			},
			loadProjects: function (userName = "admin", password = "admin") {
				// We first have to authenticate to get the projects
				$.ajax({
					url: PTV.createConvertigoServiceUrl("engine.Authenticate"),
					data: {
						authUserName: userName,
						authPassword: password,
						authType: "login"
					},
					success: function (data, textStatus, jqXHR) {
						// User is authenticated : we can create project nodes
						if ($(data).find("role[name='AUTHENTICATED']").length !== 0) {
							PTV.createProjectNodes();
						}
					}
				});
			},
			onJstreeScriptInjected: function (jstreeId) {
				setTimeout(function () {						
					// Define AJAX setup
					$.ajaxSetup({
						type: "POST",
						dataType: "xml",
						xhrFields: {
							withCredentials: true
						}
					});
					
					// Initialize the jstree
					PTV.tree = $(jstreeId);
					$(PTV.tree)
						.jstree({
							core: {
								check_callback: true,
								themes: {
									name: "default-dark",
									dots: false
								}
							},
							plugins: [
								"contextmenu",
								"dnd",
								"types"
							],
							contextmenu: {
								// Do not trigger select node event when right click mouse is pressed
								select_node: false
							},
							types: {
								// default type = Node folder (=Steps, Sequences, Connectors...) 
								"default": {
									icon: "folder"
								}
							}
						})
						.on("select_node.jstree", PTV.handleSelectNodeEvent);
					PTV.callJstree = $(PTV.tree).jstree();
					
					// Create the base of the project tree view
					PTV.loadProjects();
				}, PTV.delayOnJstreeScriptInjected);
			}
		};
	
	var convertigoMachineUrl = "http://localhost:18080/";
	PTV.init(convertigoMachineUrl, "#projectTreeView");
})();