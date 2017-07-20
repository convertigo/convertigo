function ProxyHttpConnectorEditor(id, projectsView, qname) {
    AbstractEditor.call(this, id, "proxyhttpconnector-output", "proxyhttpconnector connector-editor");

    new ProxyHttpConnectorEditorToolbar(this.toolbarContainer, projectsView, qname);
}

ProxyHttpConnectorEditor.prototype = Object.create(AbstractEditor.prototype);
ProxyHttpConnectorEditor.prototype.constructor = ProxyHttpConnectorEditor;
