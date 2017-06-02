var EditorListener = {
    init: function () {
        // save bean expression event
        document.addEventListener(
            "save_JscriptStep",
            function (event) {
                DatabaseObjectManager.setProperty([event.detail.qname], "expression", event.detail.content);
            },
            false
        );
        
        // save handler transaction
        document.addEventListener(
            "save_JscriptTransaction",
            function (event) {
                $.ajax({
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
    }
};

EditorListener.init();
