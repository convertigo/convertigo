function SiteClipperConnectorEditor(id, projectsView, qname) {
    AbstractEditor.call(this, id, "siteclipperconnector", "siteclipperconnector connector-editor");

    new SiteClipperConnectorEditorToolbar(this.toolbarContainer, projectsView, qname);
}

SiteClipperConnectorEditor.prototype = Object.create(AbstractEditor.prototype);
SiteClipperConnectorEditor.prototype.constructor = SiteClipperConnectorEditor;
