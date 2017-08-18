var GwtEventsListener = {
    init: function (projectsView) {
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
                Convertigo.callService(
                    "studio.database_objects.SaveHandlerTransaction",
                    function (data, textStatus, jqXHR) {
                    }, {
                        qname: event.detail.qname,
                        handlers: event.detail.content
                    }
                );
            },
            false
        );

        // Open a Sequence editor
        document.addEventListener(
            "OpenGraphicEditor.sequence",
            function (event) {
                if (!$("#" + event.detail.id).length) {
                    EditorsManager.put(
                        event.detail.qname,
                        new SequenceEditor(event.detail.id, projectsView, event.detail.qname)
                    );
                }
                $(document).trigger("OpenGraphicEditor");
            },
            false
        );

        // Open a CICS Connector editor
        document.addEventListener(
            "OpenGraphicEditor.cicsconnector",
            function (event) {
                if (!$("#" + event.detail.id).length) {
                    EditorsManager.put(
                        event.detail.qname,
                        new CICSConnectorEditor(event.detail.id, projectsView, event.detail.qname)
                    );
                }
                $(document).trigger("OpenGraphicEditor");
            },
            false
        );

        // Open a CouchDB Connector editor
        document.addEventListener(
            "OpenGraphicEditor.couchdbconnector",
            function (event) {
                if (!$("#" + event.detail.id).length) {
                    EditorsManager.put(
                        event.detail.qname,
                        new CouchDBConnectorEditor(event.detail.id, projectsView, event.detail.qname)
                    );
                }
                $(document).trigger("OpenGraphicEditor");
            },
            false
        );

        // Open a FullSync Connector editor
        document.addEventListener(
            "OpenGraphicEditor.fullsyncconnector",
            function (event) {
                if (!$("#" + event.detail.id).length) {
                    EditorsManager.put(
                        event.detail.qname,
                        new FullSyncConnectorEditor(event.detail.id, projectsView, event.detail.qname)
                    );
                }
                $(document).trigger("OpenGraphicEditor");
            },
            false
        );

        // Open a Html Connector editor
        document.addEventListener(
            "OpenGraphicEditor.htmlconnector",
            function (event) {
                if (!$("#" + event.detail.id).length) {
                    EditorsManager.put(
                        event.detail.qname,
                        new HtmlConnectorEditor(event.detail.id, projectsView, event.detail.qname)
                    );
                }
                $(document).trigger("OpenGraphicEditor");
            },
            false
        );

        // Open a Http Connector editor
        document.addEventListener(
            "OpenGraphicEditor.httpconnector",
            function (event) {
                if (!$("#" + event.detail.id).length) {
                    EditorsManager.put(
                        event.detail.qname,
                        new HttpConnectorEditor(event.detail.id, projectsView, event.detail.qname)
                    );
                }
                $(document).trigger("OpenGraphicEditor");
            },
            false
        );

        // Open a Javelin Connector editor
        document.addEventListener(
            "OpenGraphicEditor.javelinconnector",
            function (event) {
                if (!$("#" + event.detail.id).length) {
                    EditorsManager.put(
                        event.detail.qname,
                        new JavelinConnectorEditor(event.detail.id, projectsView, event.detail.qname)
                    );
                }
                $(document).trigger("OpenGraphicEditor");
            },
            false
        );

        // Open a Proxy Http Connector editor
        document.addEventListener(
            "OpenGraphicEditor.proxyhttpconnector",
            function (event) {
                if (!$("#" + event.detail.id).length) {
                    EditorsManager.put(
                        event.detail.qname,
                        new ProxyHttpConnectorEditor(event.detail.id, projectsView, event.detail.qname)
                    );
                }
                $(document).trigger("OpenGraphicEditor");
            },
            false
        );
        
        // Open a Sap Jco Connector editor
        document.addEventListener(
            "OpenGraphicEditor.sapjcoconnector",
            function (event) {
                if (!$("#" + event.detail.id).length) {
                    EditorsManager.put(
                        event.detail.qname,
                        new SapJcoConnectorEditor(event.detail.id, projectsView, event.detail.qname)
                    );
                }
                $(document).trigger("OpenGraphicEditor");
            },
            false
        );

        // Open a Site Clipper Connector editor
        document.addEventListener(
            "OpenGraphicEditor.siteclipperconnector",
            function (event) {
                if (!$("#" + event.detail.id).length) {
                    EditorsManager.put(
                        event.detail.qname,
                        new SiteClipperConnectorEditor(event.detail.id, projectsView, event.detail.qname)
                    );
                }
                $(document).trigger("OpenGraphicEditor");
            },
            false
        );

        // Open a Sql Connector editor
        document.addEventListener(
            "OpenGraphicEditor.sqlconnector",
            function (event) {
                if (!$("#" + event.detail.id).length) {
                    EditorsManager.put(
                        event.detail.qname,
                        new SqlConnectorEditor(event.detail.id, projectsView, event.detail.qname)
                    );
                }
                $(document).trigger("OpenGraphicEditor");
            },
            false
        );
    }
};
