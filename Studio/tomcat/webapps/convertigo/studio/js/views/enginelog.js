function EngineLogView() {
    this.resetCounterLogsChars();
    this.nbLines = 0;

    this.latestLogJqXHR = null; // For real time logs (long poll mechanism)
	this.moreResults = false; // More logs
    this.previousTimestamp = 0;

    this.autoScroll = true;

    // Main div
    this.mainDiv = $("<div/>", {
        "class": "engineLogView"
    });

    this.scrollableDiv = null;

    // Table core
    var logTable = $("<table/>");
    var logThead = $("<thead/>");
    this.logTbody = $("<tbody/>");

    // Define columns
    var logColumns =
        "<tr>" +
            //"<th class=\"log-column-line-number\"></th>" +
            "<th class=\"" + this.createClassNameColumn(this.categoryColumn) + "\">" + this.categoryColumn + "</th>" +
            "<th class=\"" + this.createClassNameColumn(this.timeColumn) + "\">" + this.timeColumn + "</th>" +
            "<th class=\"" + this.createClassNameColumn(this.deltaTimeColumn) + "\">" + this.deltaTimeColumn + "</th>" +
            "<th class=\"" + this.createClassNameColumn(this.levelColumn) + "\">" + this.levelColumn + "</th>" +
            "<th class=\"" + this.createClassNameColumn(this.threadColumn) + "\">" + this.threadColumn + "</th>" +
            "<th class=\"" + this.createClassNameColumn(this.messageColumn) + "\">" + this.messageColumn + "</th>" +
            "<th class=\"" + this.createClassNameColumn(this.extraColumn) + "\">" + this.extraColumn + "</th>" +
        "</tr>";

    this.updateColumnsVisibility(this.getDisplayedColumns());

    // Construct table
    $(logThead).append(logColumns);
    $(logTable)
        .append(logThead)
        .append(this.logTbody);
    $(this.mainDiv).append(logTable);

    this.getLogs(); 
}

// Regexp to format logs
EngineLogView.regexpAllInferiorChars = /</g;
EngineLogView.regexpAllSuperiorChars = />/g;
EngineLogView.regexpDateTimeSeparatorChars = /[-:,\s]/;
EngineLogView.regexpLF = /\n/g;
EngineLogView.regexpSpace = / /g;
EngineLogView.regexpTabulation = /\t/g;
EngineLogView.limitLogsChars = 25000;

EngineLogView.prototype.clearLogs = function () {
    $(this.logTbody).empty();
    this.resetCounterLogsChars();
};

EngineLogView.prototype.getDiv = function () {
    return this.mainDiv;
};

EngineLogView.prototype.getLogs = function () {
    var that = this;

    that.latestLogJqXHR = Convertigo.callJSONService(
        "logs.Get",
        function (data, textStatus, jqXHR) {
            if (that.latestLogJqXHR != jqXHR) {
                // parallel getLogs, keep the latest
                return;
            }

            // Construct logs
            var formattedLines = "";
            for (var line in data.lines) {
                ++that.nbLines;

                formattedLines += that.formatLine(that.nbLines, data.lines[line]);
                if (that.counterLogsChars > EngineLogView.limitLogsChars) {
                    that.clearLogs();
                }
            }
            $(that.logTbody).append(formattedLines);

            // Auto-scroll
            if (that.autoScroll) {
                that.goToEnd();
            }

            // Get next logs
            that.moreResults = data.hasMoreResults;
            that.getLogs();
        }, {
            nbLines: 50,
            timeout : 1000,
            moreResults: that.moreResults,
            realtime: true
        }
    );
};

EngineLogView.prototype.getDisplayedColumns = function () {
    var engineLogColumns = localStorage.getItem("engineLogColumns");
    // If preferences does not exist yet
    if (!engineLogColumns) {
        // [ {columnName: showColumn}, {columnName: showColumn}, ...]
        // By default show all columns
        engineLogColumns = [
            VariableUtils.createObject(this.categoryColumn, true),
            VariableUtils.createObject(this.timeColumn, true),
            VariableUtils.createObject(this.deltaTimeColumn, true),
            VariableUtils.createObject(this.levelColumn, true),
            VariableUtils.createObject(this.threadColumn, true),
            VariableUtils.createObject(this.messageColumn, true),
            VariableUtils.createObject(this.extraColumn, true)
        ];
    }
    else {
        engineLogColumns = JSON.parse(engineLogColumns);
    }

    return engineLogColumns;
};

EngineLogView.prototype.getScrollableDiv = function () {
    if (this.scrollableDiv === null) {
        this.scrollableDiv = $(this.mainDiv).parent()[0];
    }

    return this.scrollableDiv;
};

EngineLogView.prototype.isAutoScrollEnabled = function () {
    return this.autoScroll;
};

EngineLogView.prototype.toggleLock = function () {
    this.autoScroll = !this.autoScroll;
};

EngineLogView.prototype.goToTop = function () {
    this.getScrollableDiv().scrollTop = 0;
};

EngineLogView.prototype.goToEnd = function () {
    this.getScrollableDiv().scrollTop = $(this.mainDiv)[0].scrollHeight;
};

EngineLogView.prototype.formatLine = function (nLine, line) {
    // Category + time
    var category = line[0];
    var time = line[1];

    var tdate = time.split(EngineLogView.regexpDateTimeSeparatorChars);
    var currentTimestamp = (new Date(tdate[0], tdate[1], tdate[2], tdate[3], tdate[4], tdate[5], tdate[6])).getTime();

    // Delta time
    var deltaTime;
    if (nLine == 1) {
        deltaTime = "+0ms";
    }
    else {
        deltaTime = currentTimestamp - this.previousTimestamp;
        if (deltaTime < 1000) {
            deltaTime = "+" + deltaTime + "ms";
        }
        else if (deltaTime < 60000) {
            deltaTime = "+" + (deltaTime / 1000) + "s";
        }
        else {
            deltaTime = Math.round(deltaTime / 1000);
            var minutes = Math.floor(deltaTime / 60);
            var seconds = deltaTime % 60;
            deltaTime = "+" + minutes + "min" + seconds + "s";
        }
    }
    this.previousTimestamp = currentTimestamp;

    // Level + thread
    var level = line[2];
    var thread = line[3];

    // Message

    // The message is build with its main part optionally followed by a LF character
    // and then then sub-lines part of the message
    var message = line[4]
                    .replace(EngineLogView.regexpAllInferiorChars, "&lt;")
                    .replace(EngineLogView.regexpAllSuperiorChars, "&gt;");

    var posLF = message.indexOf("\n");
    if (posLF != -1) {
        var mainMessage = message.substring(0, posLF);
        var sublinesMessage = message
            .substring(posLF + 1)
            .replace(EngineLogView.regexpSpace, "&nbsp;")
            .replace(EngineLogView.regexpTabulation, "&nbsp;&nbsp;&nbsp;&nbsp;")
            .replace(EngineLogView.regexpLF, "<br/>");

        message =
            "<div class=\"log-message\">" +
                mainMessage + "<br/>" +
                "<div class=\"log-message-sublines\">" +
                    this.ellipsis(sublinesMessage) +
                "</div>" +
            "</div>";

        this.counterLogsChars += mainMessage.length + sublinesMessage.length;
    }
    else {
        this.counterLogsChars += message.length;
        message = "<div class=\"log-message\">" + this.ellipsis(message) + "</div>";
    }

    // Extra columns are columns that contain "key=value"
    var extra = "";
    var j = 1;
    var limit = line.length - 1;
    for (var i = 5; i < line.length; ++i) {
        extra += "<span class=\"log-extra log-extra" + i + "\">" +
        line[i] + "</span>";
        if (i != limit) {
            extra += " <br/>"; //blank important
        }
        ++j;
    }

    return "<tr class=\"log-line log-line-" + (nLine % 2 == 0 ? "odd-" : "even-") + level + "\">" +
                //"<td class=\"log-column-line-number\">" + nLine + "</td>" +
                "<td class=\"" + this.createClassNameColumn(this.categoryColumn) + "\">" + category + "</td>" +
                "<td class=\"" + this.createClassNameColumn(this.timeColumn) + "\">" + time + "</td>" +
                "<td class=\"" + this.createClassNameColumn(this.deltaTimeColumn) + "\">" + deltaTime + "</td>" +
                "<td class=\"" + this.createClassNameColumn(this.levelColumn) + "\">" + level + "</td>" +
                "<td class=\"" + this.createClassNameColumn(this.threadColumn) + "\">" + thread + "</td>" +
                "<td class=\"" + this.createClassNameColumn(this.messageColumn) + "\">" + message + "</td>" +
                "<td class=\"" + this.createClassNameColumn(this.extraColumn) + "\">" + extra + "</td>" +
           "</tr>";
};

EngineLogView.prototype.resetCounterLogsChars = function () {
    this.counterLogsChars = 0;
};

EngineLogView.prototype.ellipsis = function (message) {
    return StringUtils.ellipsis(message, EngineLogView.limitLogsChars, true)
};

EngineLogView.prototype.updateColumnsVisibility = function (engineLogColumns) {
    localStorage.setItem("engineLogColumns", JSON.stringify(engineLogColumns));

    var that = this;
    // Update visibility
    engineLogColumns.forEach(function (elem) {
        var columnName = Object.keys(elem)[0];
        var showColumn = elem[columnName];
        var cssColumn = "." + that.createClassNameColumn(columnName);

        StyleUtils.changeCssByStylesheetId(
            Main.sheet.ownerNode.id,
            cssColumn,
            "display",
            // Show or hide
            showColumn ? "table-cell" : "none"
        );        
    });
};

EngineLogView.prototype.createClassNameColumn = function (columnName) {
    return this.columnClassPrefix + columnName.replace(/\s/g, "-").toLowerCase();
}

EngineLogView.prototype.columnClassPrefix = "log-column-";
EngineLogView.prototype.categoryColumn = "Category";
EngineLogView.prototype.timeColumn = "Time";
EngineLogView.prototype.deltaTimeColumn = "Delta time";
EngineLogView.prototype.levelColumn = "Level";
EngineLogView.prototype.threadColumn = "Thread";
EngineLogView.prototype.messageColumn = "Message";
EngineLogView.prototype.extraColumn = "Extra";
