var DatabaseObjectManager = {
	listeners: [],

	addListener: function (listener) {
		DatabaseObjectManager.listeners.push(listener);
	},
	setProperty: function (qnames, property, value) {
		$.ajax({
		    dataType: "xml",
			url: Convertigo.createServiceUrl("studio.database_objects.Set"),
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
	notifyDatabaseObjectDelete: function (data) {
		var $responses = $(data).find("admin>*");
		var qnamesDbosToDelete = [];
		$responses.each(function () {
			if ($(this).attr("doDelete") === "true") {
				qnamesDbosToDelete.push($(this).attr("qname"));
			}
		});

		// Notify all listeners
		$.each(DatabaseObjectManager.listeners, function () {
			$(this).trigger("database_object_delete.dbo-manager", [qnamesDbosToDelete]);
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
			$(this).trigger("set_property.dbo-manager", [qnames, propertyName, propertyValue, data]);
		});
	}
};
