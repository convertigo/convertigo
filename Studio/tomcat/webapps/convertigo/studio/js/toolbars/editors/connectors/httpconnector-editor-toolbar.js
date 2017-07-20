function HttpConnectorEditorToolbar(container, projectsView, qname) {
    Toolbar.call(this, container, "htmlconnector-editor-action");

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

HttpConnectorEditorToolbar.prototype = Object.create(Toolbar.prototype);
HttpConnectorEditorToolbar.prototype.constructor = HttpConnectorEditorToolbar;
