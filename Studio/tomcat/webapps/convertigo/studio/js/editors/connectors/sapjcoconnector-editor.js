function SapJcoConnectorEditor(id, projectsView, qname) {
    AbstractEditor.call(this, id, "sapjcoconnector", "sapjcoconnector connector-editor");

    new SapJcoConnectorEditorToolbar(this.toolbarContainer, projectsView, qname);
}

SapJcoConnectorEditor.prototype = Object.create(AbstractEditor.prototype);
SapJcoConnectorEditor.prototype.constructor = SapJcoConnectorEditor;
