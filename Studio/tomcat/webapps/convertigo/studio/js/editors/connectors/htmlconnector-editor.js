function HtmlConnectorEditor(id, projectsView, qname) {
    AbstractEditor.call(this, id, "htmlconnector", "htmlconnector connector-editor");

    new HtmlConnectorEditorToolbar(this.toolbarContainer, projectsView, qname);
}

HtmlConnectorEditor.prototype = Object.create(AbstractEditor.prototype);
HtmlConnectorEditor.prototype.constructor = HtmlConnectorEditor;
