function SqlConnectorEditorToolbar(container, projectsView, qname) {
    Toolbar.call(this, container, "sqlconnector-editor-action");

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

SqlConnectorEditorToolbar.prototype = Object.create(Toolbar.prototype);
SqlConnectorEditorToolbar.prototype.constructor = SqlConnectorEditorToolbar;
