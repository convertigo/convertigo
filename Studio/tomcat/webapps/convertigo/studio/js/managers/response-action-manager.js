var ResponseActionManager = {
	projectViews: null,

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
						ResponseActionManager.projectViews.callServiceCallAction(null, null, code);
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
		}
	},
	handleResponse: function (responseName, $data, projectViews) {
		if (responseName in ResponseActionManager.responseNameToFunction) {
			ResponseActionManager.projectViews = projectViews;
			ResponseActionManager.responseNameToFunction[responseName]($data);
		}
	}
};
