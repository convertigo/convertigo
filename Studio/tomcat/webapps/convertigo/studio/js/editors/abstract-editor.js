function AbstractEditor(id, prefixClassPreTag, classEditor) {
    this.tabContainer = $(".graphicEditorsView").last();
    this.tabContainer.attr("id", id);

    this.tabContainer.parent().parent().css("background-color", Main.isCheDarkTheme() ? "#222222" : "white");

    var $top = $("<div/>", {
        text: " ",
        "class": "editor-top-bar lm_header"
    });

    // Create loading animation
    this.loadAnimation = $("<div/>", {
        "class": "loading-animation"
    });
    var $blocks = $('<div class="barlittle block1"></div><div class="barlittle block2"></div><div class="barlittle block3"></div><div class="barlittle block4"></div><div class="barlittle block5"></div>');
    // ... but hide it for the moment
    this.hideLoadingAnimation();
    this.loadAnimation.append($blocks);

    this.toolbarContainer = $("<ul/>", {
        "class": "lm_controls"
    });
    $top.append(this.toolbarContainer);
    $top.append(this.loadAnimation);

    // Response output
    this.responseCode = $("<code/>", {
        text: " ",
        "class": "language-markup"
    });
    var $responseOutput = $("<pre/>", {
        "class": prefixClassPreTag + "-output"
    });
    $responseOutput.append(this.responseCode);
    var $divReponse = $("<div/>", {
        "class": classEditor
    });
    $divReponse.append($responseOutput);

    this.bottom = $("<div/>");
    this.bottom.append($divReponse);

    this.tabContainer
        .append($top)
        .append(this.bottom);
}

AbstractEditor.prototype.hideLoadingAnimation = function () {
    this.loadAnimation.hide();
};

AbstractEditor.prototype.showLoadingAnimation = function () {
    this.loadAnimation.show();
};

AbstractEditor.prototype.setText = function (text) {
    this.responseCode.text(StringUtils.decodeHTML(text));
    Prism.highlightElement(this.responseCode[0]);
};

AbstractEditor.prototype.clearResponse = function () {
    // ALT + 0160 character, to make the div to have a width/height, it might changed later
    this.responseCode.text(" ");
};
