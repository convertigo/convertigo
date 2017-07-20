function FullSyncConnectorEditor(id, projectsView, qname) {
    AbstractEditor.call(this, id, "fullsyncconnector", "fullsyncconnector connector-editor");

    new FullSyncConnectorEditorToolbar(this.toolbarContainer, projectsView, qname);
}

FullSyncConnectorEditor.prototype = Object.create(AbstractEditor.prototype);
FullSyncConnectorEditor.prototype.constructor = FullSyncConnectorEditor;
