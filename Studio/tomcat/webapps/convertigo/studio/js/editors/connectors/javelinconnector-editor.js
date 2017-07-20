function JavelinConnectorEditor(id, projectsView, qname) {
    AbstractEditor.call(this, id, "javelinconnector", "javelinconnector connector-editor");

    new JavelinConnectorEditorToolbar(this.toolbarContainer, projectsView, qname);
}

JavelinConnectorEditor.prototype = Object.create(AbstractEditor.prototype);
JavelinConnectorEditor.prototype.constructor = JavelinConnectorEditor;
