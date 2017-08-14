var DatabaseObjectManager = {
	listeners: [],

	addListener: function (listener) {
		DatabaseObjectManager.listeners.push(listener);
	},
	setProperty: function (qnames, property, value) {
        Convertigo.callService(
            "studio.properties.Set",
            function (data, textStatus, jqXHR) {                
                DatabaseObjectManager.notifySetProperty($(data).find("admin"));
            }, {
                qnames: qnames,
                property: property,
                value: value
            }
        );
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
	notifySetProperty: function (data, listenersToIgnore = []) {
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
		    if (!listenersToIgnore.includes(this)) {
		        $(this).trigger("set_property.dbo-manager", [qnames, propertyName, propertyValue, data]);
		    }
		});
	}
};
