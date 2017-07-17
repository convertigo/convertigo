function SqlConnectorEditor(id) {
    var $tabContainer = $(".graphicEditorsView").last();
    $tabContainer.attr("id", id);

    $tabContainer.parent().parent().css("background-color", Main.isCheDarkTheme() ? "#222222" : "white");

    var $top = $("<div>", {
        text: " ",
        "class": "editor-top-bar"
    });

    // Connector output
    var $connectorCode = $("<code/>", {
        text: " ",
        "class": "language-markup"
    });
    var $connectorOutput = $("<pre/>", {
        "class": "output sqlconnector-output"
    });
    $connectorOutput.append($connectorCode);
    var $divConnector = $("<div/>", {
        "class": "sqlconnector connector-editor"
    });
    $divConnector.append($connectorOutput);

    var $bottom = $("<div/>");
    $bottom
        .append($divConnector);

    $tabContainer
        .append($top)
        .append($bottom);
}
