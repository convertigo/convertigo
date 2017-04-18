var ResponseActionManager = {
	responseNameToFunction: {
		SetPropertyResponse: function ($data) {
			DatabaseObjectManager.notifySetProperty($data.find("admin"));
		},
		MessageBoxResponse: function ($data) {
			var $response = $data.find("admin>*>*").first();
			Modal.createMessageBox(
				$response.find("title").text(),
				$response.find("message").text()
			);
		},
		MessageDialogResponse: function ($data) {
			var $response = $data.find("admin>*>*").first();
			
			// Create buttons
			var $buttons = $response.find("buttons").children();
			var allButtons = [];
			$buttons.each(function () {
				var that = this;
				var $button = $("<button/>", {
					type: "button",
					text: $(this).find("text").text(),
					click: function () {
						// Send response of the dialog to the server
						var code = $(that).find("response").text();
						ProjectsView.callServiceCallAction(null, null, code);
						$.modal.close();
					}
				});
				allButtons.push($button);
			});
			
			Modal.createMessageDialog(
				$response.find("title").text(),
				$response.find("message").text(),
				allButtons
			);
		},
		DatabaseObjectDeleteResponse: function ($data) {
			var $responses = $data.find("admin>*");
			$responses.each(function () {
				if ($(this).attr("doDelete") === "true") {
					var qnId = ProjectsView.computeNodeId($(this).attr("qname"));
					// Remove all nodes from the tree
					var idNodes = ProjectsView.tree.jstree().getIdNodes(qnId);
					for (var i = 0; i < idNodes.length; ++i) {
						var parentNodeId = ProjectsView.tree.jstree().get_parent(idNodes[i]);
						var parentNode = ProjectsView.tree.jstree().get_node(parentNodeId);
						
						ProjectsView.tree.jstree().delete_node(parentNode.children.length == 1 ?
							// Remove the parent node if the current element is the last child
						    parentNode :
						    // Just remove the element
						    idNodes[i]);
					}
					
					PropertiesView.removeTreeData();
				}
			})
		}
	},
	handleResponse: function (responseName, $data) {
		if (responseName in ResponseActionManager.responseNameToFunction) {
			ResponseActionManager.responseNameToFunction[responseName]($data);
		}
		else {
			console.debug("Handle response action not defined");
		}
	}
};
