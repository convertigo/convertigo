var Modal = {
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
		var $modal = $("<div/>");
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
		
		// After closing modal, delete it from the DOM
		$modal.on($.modal.AFTER_CLOSE, function (event, modal) {
			$(modal.elm).remove();
		});
		
		// Open modal
		$modal.modal({
			escapeClose: false,
			clickClose: false,
			showClose: false
		});
	}
};
