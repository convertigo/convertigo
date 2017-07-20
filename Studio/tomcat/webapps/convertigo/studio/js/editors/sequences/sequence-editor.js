function SequenceEditor(id, projectsView, qname) {
    AbstractEditor.call(this, id, "sequence", "sequence-editor");

    new SequenceEditorToolbar(this.toolbarContainer, projectsView, qname);

    // Internal requester output
    this.internalRequesterCode = $("<code/>");
    var $internalRequesterOutput = $("<pre/>", {
        "class": "internal-requester-output"
    });
    $internalRequesterOutput.append(this.internalRequesterCode);
    var $divInternalRequester = $("<div/>", {
        "class": "internal-requester"
    });
    $divInternalRequester.append($internalRequesterOutput);

    // Internal requester output + Sequence output container
    this.bottom.prepend($divInternalRequester)
}

SequenceEditor.prototype = Object.create(AbstractEditor.prototype);
SequenceEditor.prototype.constructor = SequenceEditor;

SequenceEditor.prototype.setContent = function (content) {
    if (content.startsWith('<?xml version="1.0" encoding="UTF-8"?>')) {
        this.internalRequesterCode.addClass("language-markup");
        content = StringUtils.decodeHTML(content);
    }
    this.internalRequesterCode.text(content);
    Prism.highlightElement(this.internalRequesterCode[0]);
};

SequenceEditor.prototype.clearContent = function () {
    this.internalRequesterCode.removeClass("language-markup");

    // ALT + 0160 character, to make the div to have a width/height, it might changed later
    this.internalRequesterCode.text(" ");
};
