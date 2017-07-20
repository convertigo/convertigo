function CICSConnectorEditor(id, projectsView, qname) {
    AbstractEditor.call(this, id, "cicsconnector", "cicsconnector connector-editor");

    new CICSConnectorEditorToolbar(this.toolbarContainer, projectsView, qname);
}

CICSConnectorEditor.prototype = Object.create(AbstractEditor.prototype);
CICSConnectorEditor.prototype.constructor = CICSConnectorEditor;
