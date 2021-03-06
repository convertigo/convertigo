var ResponseActionManager = {
	projectView: null,

	continueAction: function(callAction) {
        // Recall the right service to continue the action
        if (callAction) {
            ResponseActionManager.projectView.callServiceCallAction(null, null, null);
        }
        else {
            ResponseActionManager.projectView.callServiceDblkAction(null);
        }  
	},
	responseNameToFunction: {
		SetPropertyResponse: function ($data) {
			DatabaseObjectManager.notifySetProperty($data.find("admin"));
		},
		/*****************************
         * Message
         *****************************/
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
		DatabaseObjectDeleteActionResponse: function ($data) {
			DatabaseObjectManager.notifyDatabaseObjectDelete($data.find("admin"));
		},
		/*****************************
         * Editor
         *****************************/
		OpenEditableEditorActionResponse: function ($data) {
            var $response = $data.find("admin>response");
            var editor = $response.attr("type_editor");
            switch (editor) {
                case "c8o_jscriptstepeditor":
                case "c8o_xsleditor":
                case "c8o_jscripttransactioneditor":
                    var filePath = $response.find(">filepath").text();
                    CheGWTOpenTextEditor(filePath, $response.attr("qname"));
                    break;

                default:
                    break;
            }
		},
		SequenceExecuteSelectedOpenSequenceEditorResponse: function ($data, callAction) {
		    var $response = $data.find("response");
		    // Open the editor in CHE
		    CheGWTOpenSequenceEditor($response.attr("project"), $response.attr("sequence"));

            // Continue the action when the related editor is opened
		    $(document).on("OpenGraphicEditor", function () {
		        ResponseActionManager.continueAction(callAction);
		        $(document).off("OpenGraphicEditor");
		    });
		},
		SequenceExecuteSelectedOpenConnectorEditorResponse: function ($data, callAction) {
		    var $response = $data.find("response");
            // Open the editor in CHE
            CheGWTOpenConnectorEditor($response.attr("project"), $response.attr("connector"),  $response.attr("type_editor"));

            // Continue the action when the related editor is opened
            $(document).on("OpenGraphicEditor", function () {
                ResponseActionManager.continueAction(callAction);
                $(document).off("OpenGraphicEditor");
            });
		},
		/*****************************
		 * Source Picker
		 *****************************/
		SourcePickerViewFillHelpContentResponse: function ($data, callAction) {
		    var $response = $data.find("response");
		    Main.getSourcePicker().fillHelpContent(
	            $response.find("tag").text(),
	            $response.find("type").text(),
	            $response.find("name").text(),
	            $response.find("comment").text(),
	            $response.find("text_show_btn").text(),
	            $response.find("enable_btn").text() === "true"
            );

            ResponseActionManager.continueAction(callAction);
		},
		TwsDomTreeFillDomTreeResponse: function ($data, callAction) {
		    var $response = $data.find("response");
		    Main.getSourcePicker().fillDomTree($response.find("dom_tree"));

            ResponseActionManager.continueAction(callAction);
		},
		TwsDomTreeRemoveAllResponse: function ($data, callAction) {
		    Main.getSourcePicker().removeAll();

            ResponseActionManager.continueAction(callAction);
		},
		XpathEvaluatorCompositeSetXpathTextResponse: function ($data, callAction) {
		    var $response = $data.find("response");
	        Main.getSourcePicker().setXpathText($data.find("xpath").text(), $data.find("anchor").text());

            ResponseActionManager.continueAction(callAction);
        },
        XpathEvaluatorCompositeRemoveAnchorResponse: function ($data, callAction) {
            Main.getSourcePicker().removeAnchor();

            ResponseActionManager.continueAction(callAction);
        }
	},
	handleResponse: function (responseName, $data, projectView, callAction = true) {
		if (responseName in ResponseActionManager.responseNameToFunction) {
			ResponseActionManager.projectView = projectView;
			ResponseActionManager.responseNameToFunction[responseName]($data, callAction);
		}
	}
};
