var C8OServerEventsListener = {
    init: function () {
        var that = this;
        var sse = $.SSE(Convertigo.getBaseConvertigoStudioUrl("getevents"), {
            events: {
                /**************************
                 * Sequence
                 **************************/
                "SequenceEditorPart.documentGenerated": function (event) {
                    that.onDocumentGenerated(event, "sequence_output");
                },
                "SequenceEditorPart.sequenceStarted": function (event) {
                    that.onStarted(event);
                },
                "SequenceEditorPart.sequenceFinished": function (event) {
                    that.onFinished(event);
                },
                "SequenceEditorComposite.dataChanged": function (event) {
                    var $response = that.getEditorEventResponse(event.data);
                    var data = $response.find("internal_requester_output").text();

                    var editor = EditorsManager.get($response.attr("qname"));
                    editor.setContent(data);
                },
                "SequenceEditorComposite.clearContent": function (event) {
                    var $response = that.getEditorEventResponse(event.data);

                    var editor = EditorsManager.get($response.attr("qname"));
                    editor.clearContent();
                },

                /**************************
                 * Connectors
                 **************************/
                "ConnectorEditorPart.documentGenerated": function (event) {
                    that.onDocumentGenerated(event, "connector_output");
                },
                "ConnectorEditorPart.transactionStarted": function (event) {
                    that.onStarted(event);
                },
                "ConnectorEditorPart.transactionFinished": function (event) {
                    that.onFinished(event);
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
    onDocumentGenerated: function (event, ouputSelector) {
        var $response = this.getEditorEventResponse(event.data);
        var output = $response.find(ouputSelector).text();

        var editor = EditorsManager.get($response.attr("qname"));
        editor.setText(output);
    },
    onStarted: function (event) {
        var $response = this.getEditorEventResponse(event.data);

        var editor = EditorsManager.get($response.attr("qname"));
        editor.clearResponse();
        editor.showLoadingAnimation();
    },
    onFinished: function (event) {
        var $response = this.getEditorEventResponse(event.data);

        var editor = EditorsManager.get($response.attr("qname"));
        editor.hideLoadingAnimation();
    }
};
