var ModalUtils = {
    createEmptyModal: function (id) {
        // Create div modal with an ID
        var $modal = $("<div/>");
        if (!VariableUtils.isUndefinedOrNullOrEmpty(id)) {
            $modal.attr("id", id);
        }

        // Delete the modal from the DOM after close
        $modal.on($.modal.AFTER_CLOSE, function (event, modal) {
            $(modal.elm).remove();
        });

        return $modal;
    },
	createMessageBox: function (title, message) {
		// Create Ok button
		var $okBtn = $("<button/>", {
			type: "button",
			text: "Ok",
			click: function () {
				$.modal.close();
			}
		});
		this.createMessageDialog(title, message, [$okBtn]);
	},
	createMessageDialog: function (title, message, allButtons) {		
		// Create title and message for the modal
		var $modal = ModalUtils.createEmptyModal();
		$modal
			.append($("<h3/>", {
				text: title
			}))
			.append($("<hr/>"))
			.append($("<p/>", {
				text: message
			}));

		// Create buttons
		var $buttons = $("<p/>", {
			"class": "align-right"
		});
		for (var i = 0; i < allButtons.length; ++i) {
			$buttons.append($(allButtons[i]));
		}
		$modal.append($buttons);

		// Open modal
		$modal.modal({
			closeExisting: false, // Allow multiple modals
			escapeClose: false,
			clickClose: false,
			showClose: false
		});
	}
};
