function CouchDBConnectorEditor(id, projectsView, qname) {
    AbstractEditor.call(this, id, "couchdbconnector", "couchdbconnector connector-editor");

    new CouchDBConnectorEditorToolbar(this.toolbarContainer, projectsView, qname);
}

CouchDBConnectorEditor.prototype = Object.create(AbstractEditor.prototype);
CouchDBConnectorEditor.prototype.constructor = CouchDBConnectorEditor;
