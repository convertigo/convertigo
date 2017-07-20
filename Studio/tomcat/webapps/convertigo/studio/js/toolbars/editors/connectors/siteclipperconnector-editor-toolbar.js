function SiteClipperConnectorEditorToolbar(container, projectsView, qname) {
    Toolbar.call(this, container, "siteclipperconnector-editor-action");

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

SiteClipperConnectorEditorToolbar.prototype = Object.create(Toolbar.prototype);
SiteClipperConnectorEditorToolbar.prototype.constructor = SiteClipperConnectorEditorToolbar;
