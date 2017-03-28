var DatabaseObjectManager = {
	listeners: [],
	
	addListener: function (listener) {
		DatabaseObjectManager.listeners.push(listener);
	},
	setProperty: function (qnames, property, value) {
		$.ajax({
			url: ProjectsView.createConvertigoServiceUrl("studio.database_objects.Set"),
			data: {
				qnames: qnames,
				property: property,
				value: value
			},
			success: function (data, textStatus, jqXHR) {
				console.log($(data).find("response").attr("message"));
				
				// Update the property view
				$.each(DatabaseObjectManager.listeners, function () {
					$(this).trigger("set_property.database-object-manager", [qnames, property, value, $(data).find("admin")]);
				});
			}
		});
	} 
};
