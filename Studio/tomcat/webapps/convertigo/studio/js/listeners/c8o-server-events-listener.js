var C8OServerEventsListener = {
    init: function () {
        var that = this;
        var sse = $.SSE(Convertigo.getBaseConvertigoStudioUrl("getevents"), {
            events: {
                /**************************
                 * Sequence
                 **************************/
                "SequenceEditor.documentGenerated": function (e) {
                    var $response = that.getSequenceEditorResponse(e.data);
                    var $divElt = that.getSequenceEditorDiv($response);
                    if ($divElt.length) {
                        // Update Sequence response
                        var $codeElt = $divElt.find(".sequence code");
                        if ($codeElt.length) {
                            $codeElt.text($response.find("sequence_output").text());
                            Prism.highlightElement($codeElt[0]);
                        }
                    }
                },
                "SequenceEditor.sequenceStarted": function (e) {
                    var $response = that.getSequenceEditorResponse(e.data);
                    var $divElt = that.getSequenceEditorDiv($response);
                    if ($divElt.length) {
                        // Clear Sequence content
                        var $codeElt = $divElt.find(".sequence code");
                        if ($codeElt.length) {
                            $codeElt.text(" ");
                        }

                        // Show loading animation
                        var $topBar = $divElt.find(".sequence-editor-top-bar");
                        $topBar.addClass("loading");
                        $topBar.text("Loading");
                    }
                },
                "SequenceEditor.sequenceFinished": function (e) {
                    var $response = that.getSequenceEditorResponse(e.data);
                    var $divElt = that.getSequenceEditorDiv($response);
                    if ($divElt.length) {
                        // Remove loading animation
                        var $topBar = $divElt.find(".sequence-editor-top-bar");
                        $topBar.removeClass("loading");
                        $topBar.text(" ");
                    }
                },
                "SequenceEditorComposite.dataChanged": function (e) {
                    var $response = that.getSequenceEditorResponse(e.data);
                    var $divElt = that.getSequenceEditorDiv($response);
                    if ($divElt) {
                        // Update Internal requester response
                        var $codeElt = $divElt.find(".internal-requester code");
                        if ($codeElt.length) {
                            $codeElt.text($response.find("internal_requester_output").text());
                            Prism.highlightElement($codeElt[0]);
                        }
                    }
                },
                "SequenceEditorComposite.clearContent": function (e) {
                    var $response = that.getSequenceEditorResponse(e.data);
                    var $divElt = that.getSequenceEditorDiv($response);
                    if ($divElt) {
                        // Clear Internal request content
                        var $codeElt = $divElt.find(".calling code");
                        if ($codeElt.length) {
                            $codeElt.text(" ");
                        }
                    }
                }
            }
        });
        sse.start();
    },
    /*********************
     * Sequence
     *********************/
    runningSequences: new Set(),
    getSequenceEditorResponse: function (data) {
        return $response = $(StringUtils.replaceTextCRLFByRealCRLF(data));
    },
    getSequenceEditorDiv: function ($response) {
        return $("#" + $response.attr("project") + "-" + $response.attr("sequence") + "-" + $response.attr("type_editor"));
    }
};
