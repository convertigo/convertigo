function CICSConnectorEditorToolbar(container, projectsView, qname) {
    Toolbar.call(this, container, "cicsconnector-editor-action");

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

CICSConnectorEditorToolbar.prototype = Object.create(Toolbar.prototype);
CICSConnectorEditorToolbar.prototype.constructor = CICSConnectorEditorToolbar;
