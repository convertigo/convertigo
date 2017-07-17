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

        // Open a CICS Connector editor
        document.addEventListener(
            "OpenGraphicEditor.cicsconnector",
            function (event) {
                if (!$("#" + event.detail.id).length) {
                    new CICSConnectorEditor(event.detail.id);
                }
            },
            false
        );

        // Open a CouchDB Connector editor
        document.addEventListener(
            "OpenGraphicEditor.couchdbconnector",
            function (event) {
                if (!$("#" + event.detail.id).length) {
                    new CouchDBConnectorEditor(event.detail.id);
                }
            },
            false
        );

        // Open a FullSync Connector editor
        document.addEventListener(
            "OpenGraphicEditor.fullsyncconnector",
            function (event) {
                if (!$("#" + event.detail.id).length) {
                    new FullSyncConnectorEditor(event.detail.id);
                }
            },
            false
        );

        // Open a Html Connector editor
        document.addEventListener(
            "OpenGraphicEditor.htmlconnector",
            function (event) {
                if (!$("#" + event.detail.id).length) {
                    new HtmlConnectorEditor(event.detail.id);
                }
            },
            false
        );

        // Open a Http Connector editor
        document.addEventListener(
            "OpenGraphicEditor.httpconnector",
            function (event) {
                if (!$("#" + event.detail.id).length) {
                    new HttpConnectorEditor(event.detail.id);
                }
            },
            false
        );

        // Open a Javelin Connector editor
        document.addEventListener(
            "OpenGraphicEditor.javelinconnector",
            function (event) {
                if (!$("#" + event.detail.id).length) {
                    new JavelinConnectorEditor(event.detail.id);
                }
            },
            false
        );

        // Open a Proxy Http Connector editor
        document.addEventListener(
            "OpenGraphicEditor.proxyhttpconnector",
            function (event) {
                if (!$("#" + event.detail.id).length) {
                    new ProxyHttpConnectorEditor(event.detail.id);
                }
            },
            false
        );
        
        // Open a HTTP Connector editor
        document.addEventListener(
            "OpenGraphicEditor.sapjcoconnector",
            function (event) {
                if (!$("#" + event.detail.id).length) {
                    new SapJcoConnectorEditor(event.detail.id);
                }
            },
            false
        );

        // Open a Site Clipper Connector editor
        document.addEventListener(
            "OpenGraphicEditor.siteclipperconnector",
            function (event) {
                if (!$("#" + event.detail.id).length) {
                    new SiteClipperConnectorEditor(event.detail.id);
                }
            },
            false
        );

        // Open a Sql Connector editor
        document.addEventListener(
            "OpenGraphicEditor.sqlconnector",
            function (event) {
                if (!$("#" + event.detail.id).length) {
                    new SqlConnectorEditor(event.detail.id);
                }
            },
            false
        );
    }
};
