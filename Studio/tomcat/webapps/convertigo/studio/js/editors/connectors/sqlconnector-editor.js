function SqlConnectorEditor(id, projectsView, qname) {
    AbstractEditor.call(this, id, "sqlconnector", "sqlconnector connector-editor");

    new SqlConnectorEditorToolbar(this.toolbarContainer, projectsView, qname);
}

SqlConnectorEditor.prototype = Object.create(AbstractEditor.prototype);
SqlConnectorEditor.prototype.constructor = SqlConnectorEditor;
