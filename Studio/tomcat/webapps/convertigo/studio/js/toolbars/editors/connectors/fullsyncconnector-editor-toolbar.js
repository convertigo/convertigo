function FullSyncConnectorEditorToolbar(container, projectsView, qname) {
    Toolbar.call(this, container, "fullsyncconnector-editor-action");

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

FullSyncConnectorEditorToolbar.prototype = Object.create(Toolbar.prototype);
FullSyncConnectorEditorToolbar.prototype.constructor = FullSyncConnectorEditorToolbar;
