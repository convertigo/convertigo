function SapJcoConnectorEditorToolbar(container, projectsView, qname) {
    Toolbar.call(this, container, "sapjcoconnector-editor-action");

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

SapJcoConnectorEditorToolbar.prototype = Object.create(Toolbar.prototype);
SapJcoConnectorEditorToolbar.prototype.constructor = SapJcoConnectorEditorToolbar;
