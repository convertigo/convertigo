var DatabaseObjectManager = {
	listeners: [],
	
	addListener: function (listener) {
		DatabaseObjectManager.listeners.push(listener);
	},
	setProperty: function (qname, property, value) {
		$.ajax({
			url: ProjectsView.createConvertigoServiceUrl("studio.database_objects.Set"),
			data: {
				qname: qname,
				property: property,
				value: value
			},
			success: function (data, textStatus, jqXHR) {
				console.log($(data).find("response").attr("message"));
				
				// Update the property view
				$.each(DatabaseObjectManager.listeners, function () {
					$(this).trigger("set_property.database-object-manager", [$(data).find("admin > *").first(), property, value]);
				});
			}
		});
	} 
};
