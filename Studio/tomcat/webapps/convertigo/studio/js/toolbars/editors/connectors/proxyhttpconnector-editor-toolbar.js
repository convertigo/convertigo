function ProxyHttpConnectorEditorToolbar(container, projectsView, qname) {
    Toolbar.call(this, container, "proxyhttpconnector-editor-action");

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

ProxyHttpConnectorEditorToolbar.prototype = Object.create(Toolbar.prototype);
ProxyHttpConnectorEditorToolbar.prototype.constructor = ProxyHttpConnectorEditorToolbar;
