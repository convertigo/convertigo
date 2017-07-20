function CouchDBConnectorEditorToolbar(container, projectsView, qname) {
    Toolbar.call(this, container, "couchdbconnector-editor-action");

    // Generate XML
    this.addAction(
        "generate-xml-action",
        Convertigo.getBaseConvertigoStudioUrl("img/editors/xml.png"),
        "Generate XML",
        function () {
            projectsView.callServiceCallAction([qname], "com.twinsoft.convertigo.eclipse.popup.actions.TransactionExecuteDefaultAction", null)
        }
    );
}

CouchDBConnectorEditorToolbar.prototype = Object.create(Toolbar.prototype);
CouchDBConnectorEditorToolbar.prototype.constructor = CouchDBConnectorEditorToolbar;
