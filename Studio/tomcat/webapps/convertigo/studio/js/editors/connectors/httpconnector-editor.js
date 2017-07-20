function HttpConnectorEditor(id, projectsView, qname) {
    AbstractEditor.call(this, id, "httpconnector", "httpconnector connector-editor");

    new HttpConnectorEditorToolbar(this.toolbarContainer, projectsView, qname);
}

HttpConnectorEditor.prototype = Object.create(AbstractEditor.prototype);
HttpConnectorEditor.prototype.constructor = HttpConnectorEditor;
