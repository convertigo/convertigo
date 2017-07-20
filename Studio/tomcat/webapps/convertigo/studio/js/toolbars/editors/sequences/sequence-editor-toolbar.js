function SequenceEditorToolbar(container, projectsView, qname) {
    Toolbar.call(this, container, "sequence-editor-action");

    // Generate XML
    this.addAction(
        "generate-xml-action",
        Convertigo.getBaseConvertigoStudioUrl("img/editors/xml.png"),
        "Generate XML",
        function () {
            projectsView.callServiceCallAction([qname], "com.twinsoft.convertigo.eclipse.popup.actions.SequenceExecuteSelectedAction", null)
        }
    );
}

SequenceEditorToolbar.prototype = Object.create(Toolbar.prototype);
SequenceEditorToolbar.prototype.constructor = SequenceEditorToolbar;
