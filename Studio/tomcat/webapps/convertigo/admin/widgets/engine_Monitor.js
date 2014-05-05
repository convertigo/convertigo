/*
 * Copyright (c) 2001-2014 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

var dataGraphThreads = [];
var dataGraphContexts = [];
var dataGraphRequests = [];

var currentTick = 0;
var nbSecondsOfHistory = 60;
var samplingMilliseconds = 1000;

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
			colors : [ "#fff", "#eee" ]
		}
	}
};

function engine_Monitor_init() {
	var graphThreads = $("#graphThreads");
	var graphContexts = $("#graphContexts");
	var graphRequests = $("#graphRequests");

	var data = [];
	var dataSize = nbSecondsOfHistory * 1000 / samplingMilliseconds;
	for (var i = 0; i < dataSize; i++) {
		data.push( [ i, 0 ]);
	}
	currentTick = dataSize;
	dataGraphThreads = [ {
	    label: 'Number of threads',
	    color: 1,
	    data: data.slice()
	} ];
	dataGraphContexts = [ {
	    label: 'Number of contexts',
	    color: 2,
	    data: data.slice()
	} ];
	dataGraphRequests = [ {
	    label: 'Average request duration (in milliseconds)',
	    color: 3,
	    data: data.slice()
	} ];

	graphThreadsPlot = $.plot(graphThreads, dataGraphThreads, graphOptions);
	graphContextsPlot = $.plot(graphContexts, dataGraphContexts, graphOptions);
	graphRequestsPlot = $.plot(graphRequests, dataGraphRequests, graphOptions);

	engine_Monitor_update();
}

function updateGraphData(graphPlot, graphData, lastValueToAdd) {
	var data = graphData[0].data;
	if (graphData) {
		if (data.length >= nbSecondsOfHistory * 1000 / samplingMilliseconds) {
			data.shift();
		}
		data.push( [ currentTick, lastValueToAdd ]);

		graphPlot.setData(graphData);
		graphPlot.setupGrid();
		graphPlot.draw();
	}
}

function engine_Monitor_update() {
	callService("engine.Monitor", function(xml) {
		if ($("#graphThreads:visible").length > 0) {
			updateGraphData(graphThreadsPlot, dataGraphThreads, $(xml).find("threads").text());
			updateGraphData(graphContextsPlot, dataGraphContexts, $(xml).find("contexts").text());
			updateGraphData(graphRequestsPlot, dataGraphRequests, $(xml).find("requests").text());
			currentTick++;

			setTimeout(engine_Monitor_update, samplingMilliseconds);
		}
	});
}
