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
				DatabaseObjectManager.notifySetProperty($(data).find("admin"));
			}
		});
	},
	notifySetProperty: function (data) {
		var qnames = [];
		var propertyName = null;
		var propertyValue = null;
		
		// Construct qnames, property name and value
		$(data).find(">*").each(function () {
			qnames.push($(this).attr("qname"));
			
			if (propertyName == null) {
				var $property =  $(this).find("property");
				propertyName = $property.attr("name");
				propertyValue = $property.find("[value]").attr("value");
			}
		});
		
		// Notify all listeners
		$.each(DatabaseObjectManager.listeners, function () {
			$(this).trigger("set_property.database-object-manager", [qnames, propertyName, propertyValue, data]);
		});
	}
};
