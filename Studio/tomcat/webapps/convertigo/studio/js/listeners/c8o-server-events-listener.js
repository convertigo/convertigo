var C8OServerEventsListener = {
    init: function () {
        var that = this;
        var sse = $.SSE(Convertigo.getBaseConvertigoStudioUrl("getevents"), {
            events: {
                /**************************
                 * Sequence
                 **************************/
                "SequenceEditorPart.documentGenerated": function (e) {
                    var $response = that.getEditorEventResponse(e.data);
                    var $divElt = that.getSequenceEditorDiv($response);
                    that.onDocumentGenerated($divElt, ".sequence-editor code", "sequence_output");
                },
                "SequenceEditorPart.sequenceStarted": function (e) {
                    var $response = that.getEditorEventResponse(e.data);
                    var $divElt = that.getSequenceEditorDiv($response);
                    that.onStarted($divElt, ".sequence-editor code");
                },
                "SequenceEditorPart.sequenceFinished": function (e) {
                    var $response = that.getEditorEventResponse(e.data);
                    var $divElt = that.getSequenceEditorDiv($response);
                    that.onFinished($divElt);
                },
                "SequenceEditorComposite.dataChanged": function (e) {
                    var $response = that.getEditorEventResponse(e.data);
                    var $divElt = that.getSequenceEditorDiv($response);
                    if ($divElt) {
                        // Update Internal requester response
                        var $codeElt = $divElt.find(".internal-requester code");
                        if ($codeElt.length) {
                            var data = $response.find("internal_requester_output").text();
                            // If XML language, add highlight
                            if (data.startsWith('<?xml version="1.0" encoding="UTF-8"?>')) {
                                $codeElt.addClass("language-markup");
                            }
                            $codeElt.text($response.find("internal_requester_output").text());
                            Prism.highlightElement($codeElt[0]);
                        }
                    }
                },
                "SequenceEditorComposite.clearContent": function (e) {
                    var $response = that.getEditorEventResponse(e.data);
                    var $divElt = that.getSequenceEditorDiv($response);
                    if ($divElt) {
                        // Clear Internal request content
                        var $codeElt = $divElt.find(".calling code");
                        if ($codeElt.length) {
                            $codeElt.text(" ");
                            $codeElt[0].className = "";
                        }
                    }
                },

                /**************************
                 * Connectors
                 **************************/
                "ConnectorEditorPart.documentGenerated": function (e) {
                    var $response = that.getEditorEventResponse(e.data);
                    var $divElt = that.getConnectorEditorDiv($response);
                    that.onDocumentGenerated($divElt, ".connector-editor code", "connector_output");
                },
                "ConnectorEditorPart.transactionStarted": function (e) {
                    var $response = that.getEditorEventResponse(e.data);
                    var $divElt = that.getConnectorEditorDiv($response);
                    that.onStarted($divElt, ".connector-editor code");
                },
                "ConnectorEditorPart.transactionFinished": function (e) {
                    var $response = that.getEditorEventResponse(e.data);
                    var $divElt = that.getConnectorEditorDiv($response);
                    that.onFinished($divElt);
                }
            }
        });
        sse.start();
    },
    /*********************
     * Sequence
     *********************/
    getEditorEventResponse: function (data) {
        return $response = $(StringUtils.replaceTextCRLFByRealCRLF(data));
    },
    getConnectorEditorDiv: function ($response) {
        return $("#" + $response.attr("project") + "-" + $response.attr("connector") + "-" + $response.attr("type_editor"));
    },
    getSequenceEditorDiv: function ($response) {
        return $("#" + $response.attr("project") + "-" + $response.attr("sequence") + "-" + $response.attr("type_editor"));
    },
    onDocumentGenerated: function ($divElt, codeSelector, outputSelector) {
        if ($divElt.length) {
            // Update Connector response
            var $codeElt = $divElt.find(codeSelector);
            if ($codeElt.length) {
                $codeElt.text($response.find(outputSelector).text());
                Prism.highlightElement($codeElt[0]);
            }
        }
    },
    onStarted: function ($divElt, codeSelector) {
        if ($divElt.length) {
            // Clear code content
            var $codeElt = $divElt.find(codeSelector);
            if ($codeElt.length) {
                $codeElt.text(" ");
            }
    
            // Show loading animation
            var $topBar = $divElt.find(".editor-top-bar");
            $topBar.addClass("loading");
            $topBar.text("Loading");
        }
    },
    onFinished: function ($divElt) {
        if ($divElt.length) {
            // Remove loading animation
            var $topBar = $divElt.find(".editor-top-bar");
            $topBar.removeClass("loading");
            $topBar.text(" ");
        }
    }
};
