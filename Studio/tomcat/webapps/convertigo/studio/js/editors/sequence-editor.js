function SequenceEditor(id) {
    var $tabContainer = $(".graphicEditorsView").last();
    $tabContainer.attr("id", id);

    $tabContainer.parent().parent().css("background-color", Main.isCheDarkTheme() ? "#222222" : "white");

    var $top = $("<div>", {
        text: " ",
        "class": "sequence-editor-top-bar"
    });

    // Internal requester output
    var $internalRequesterCode = $("<code/>", {
        "class": "language-none"
    });
    var $internalRequesterOutput = $("<pre/>", {
        "class": "output internal-requester-output"
    });
    $internalRequesterOutput.append($internalRequesterCode);
    var $divInternalRequester = $("<div/>", {
        "class": "internal-requester"
    });
    $divInternalRequester.append($internalRequesterOutput);

    // Sequence output
    var $sequenceCode = $("<code/>", {
        text: " ",
        "class": "language-markup"
    });
    var $sequenceOutput = $("<pre/>", {
        "class": "output sequence-output"
    });
    $sequenceOutput.append($sequenceCode);
    var $divSequence = $("<div/>", {
        "class": "sequence"
    });
    $divSequence.append($sequenceOutput);

    // Internal requester output + Sequence output container
    var $bottom = $("<div/>");
    $bottom
        .append($divInternalRequester)
        .append($divSequence);

    $tabContainer
        .append($top)
        .append($bottom);
}
