/*
 * Copyright (c) 2001-2022 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

var dataGraphMemory = [];
var dataGraphThreads = [];
var dataGraphContexts = [];
var dataGraphRequests = [];

var currentTick = 0;
var nbSecondsOfHistory = 60;
var samplingMilliseconds = 1000;

var graphMemoryPlot;
var graphThreadsPlot;
var graphContextsPlot;
var graphRequestsPlot;

var graphOptions = {
	lines : {
		show : true
	},
	points : {
		show : true
	},
	xaxis : {
		tickDecimals : 0,
		tickSize : 100000 // trick for "hiding" xAxis ticks
	},
	yaxis : {
		tickDecimals : 0,
		ticks : 5,
		min : 0
	},
	grid : {
		backgroundColor : {
			colors : [ "#fff", "#fff" ]
		},
		hoverable: true
	}
};

function engine_Monitor_init() {
	var graphMemory = $("#graphMemory");
	var graphThreads = $("#graphThreads");
	var graphContexts = $("#graphContexts");
	var graphRequests = $("#graphRequests");

	var data = [];
	var dataSize = nbSecondsOfHistory * 1000 / samplingMilliseconds;
	for (var i = 0; i < dataSize; i++) {
		data.push( [ i, 0 ]);
	}
	currentTick = dataSize;
	dataGraphMemory = [ {
	    label: 'Maximum memory',
	    color: "rgb(255, 174, 0)",
	    data: data.slice()
	}, {
	    label: 'Total memory',
	    color: "rgb(147, 213, 13)",
	    data: data.slice()
	}, {
	    label: 'Used memory',
	    color: "rgb(0, 202, 255)",
	    data: data.slice()
	} ];
	dataGraphThreads = [ {
	    label: 'Number of threads',
	    color: "rgb(0, 202, 255)",
	    data: data.slice()
	} ];
	dataGraphContexts = [ {
	    label: 'Number of contexts',
	    color: "rgb(255, 174, 0)",
	    data: data.slice()
	} ];
	dataGraphRequests = [ {
	    label: 'Average request duration (in milliseconds)',
	    color: "rgb(147, 213, 13)",
	    data: data.slice()
	} ];

	graphMemoryPlot = $.plot(graphMemory, dataGraphMemory, graphOptions);
	graphThreadsPlot = $.plot(graphThreads, dataGraphThreads, graphOptions);
	graphContextsPlot = $.plot(graphContexts, dataGraphContexts, graphOptions);
	graphRequestsPlot = $.plot(graphRequests, dataGraphRequests, graphOptions);

	graphMemory.on("plothover", function (event, pos, item) {
		this.title = item ? item.datapoint[1] : "";
	});

	graphThreads.on("plothover", function (event, pos, item) {
		this.title = item ? item.datapoint[1] : "";
	});

	graphContexts.on("plothover", function (event, pos, item) {
		this.title = item ? item.datapoint[1] : "";
	});

	graphRequests.on("plothover", function (event, pos, item) {
		this.title = item ? item.datapoint[1] : "";
	});
	
	engine_Monitor_update();
}

function updateGraphData(graphPlot, graphData, lastValueToAdd) {
	for (i = 0; i < graphData.length; i++) {
		var data = graphData[i].data;
		if (data.length >= nbSecondsOfHistory * 1000 / samplingMilliseconds) {
			data.shift();
		}
		data.push([ currentTick, lastValueToAdd[i] ]);
	}
	graphPlot.setData(graphData);
	graphPlot.setupGrid();
	graphPlot.draw();
}

function engine_Monitor_update() {
	callService("engine.Monitor", function(xml) {
		if ($("#graphThreads:visible").length > 0) {
			updateGraphData(graphMemoryPlot, dataGraphMemory, [
				$(xml).find("memoryMaximal").text(),
				$(xml).find("memoryTotal").text(),
				$(xml).find("memoryUsed").text()
			]);
			updateGraphData(graphThreadsPlot, dataGraphThreads, [$(xml).find("threads").text()]);
			updateGraphData(graphContextsPlot, dataGraphContexts, [$(xml).find("contexts").text()]);
			updateGraphData(graphRequestsPlot, dataGraphRequests, [$(xml).find("requests").text()]);
			currentTick++;

			setTimeout(engine_Monitor_update, samplingMilliseconds);
		}
	});
}
