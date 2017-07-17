var ResponseActionManager = {
	projectView: null,

	responseNameToFunction: {
		SetPropertyResponse: function ($data) {
			DatabaseObjectManager.notifySetProperty($data.find("admin"));
		},
		MessageBoxResponse: function ($data) {
			var $response = $data.find("admin>*>*").first();
			ModalUtils.createMessageBox(
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
						ResponseActionManager.projectView.callServiceCallAction(null, null, code);
						$.modal.close();
					}
				});
				allButtons.push($button);
			});

			ModalUtils.createMessageDialog(
				$response.find("title").text(),
				$response.find("message").text(),
				allButtons
			);
		},
		DatabaseObjectDeleteResponse: function ($data) {
			DatabaseObjectManager.notifyDatabaseObjectDelete($data.find("admin"));
		},
		SequenceExecuteSelectedOpenSequenceEditor: function ($data) {
		    var $response = $data.find("response");

		    // Open the editor in CHE
		    CheGWTOpenSequenceEditor($response.attr("project"), $response.attr("sequence"));

		    // Recall the service to continue the action
            ResponseActionManager.projectView.callServiceCallAction(null, null, null);
		},
		SequenceExecuteSelectedOpenConnectorEditor: function ($data) {
		    var $response = $data.find("response");

            // Open the editor in CHE
            CheGWTOpenConnectorEditor($response.attr("project"), $response.attr("connector"),  $response.attr("type_editor"));

		    // Recall the service to continue the action
            ResponseActionManager.projectView.callServiceCallAction(null, null, null);
		}
	},
	handleResponse: function (responseName, $data, projectView) {
		if (responseName in ResponseActionManager.responseNameToFunction) {
			ResponseActionManager.projectView = projectView;
			ResponseActionManager.responseNameToFunction[responseName]($data);
		}
	}
};
