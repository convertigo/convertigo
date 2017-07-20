function JavelinConnectorEditorToolbar(container, projectsView, qname) {
    Toolbar.call(this, container, "javelinconnector-editor-action");

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

JavelinConnectorEditorToolbar.prototype = Object.create(Toolbar.prototype);
JavelinConnectorEditorToolbar.prototype.constructor = JavelinConnectorEditorToolbar;
