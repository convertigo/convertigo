var GwtEventsListener = {
    init: function () {
        // Save bean expression event
        document.addEventListener(
            "SaveTextEditor.jscriptStep",
            function (event) {
                DatabaseObjectManager.setProperty([event.detail.qname], "expression", event.detail.content);
            },
            false
        );

        // Save handler transaction
        document.addEventListener(
            "SaveTextEditor.jscriptTransaction",
            function (event) {
                $.ajax({
                    dataType: "xml",
                    url: Convertigo.createServiceUrl("studio.database_objects.SaveHandlerTransaction"),
                    data: {
                        qname: event.detail.qname,
                        handlers: event.detail.content
                    },
                    success: function (data, textStatus, jqXHR) {
                    }
                });
            },
            false
        );

        // Open a Sequence editor
        document.addEventListener(
            "OpenGraphicEditor.sequence",
            function (event) {
                if (!$("#" + event.detail.id).length) {
                    new SequenceEditor(event.detail.id);
                }
            },
            false
        );

        // Open a Sql Connector editor
        document.addEventListener(
            "OpenGraphicEditor.sqlConnector",
            function (event) {
                console.log("OpenGraphicEditor.sqlConnector")
            },
            false
        );
    }
};
