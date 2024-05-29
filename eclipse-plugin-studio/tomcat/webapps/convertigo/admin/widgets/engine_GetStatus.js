/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

var instanceUpdateDate=0;

function engine_GetStatus_init() {
	$("#statusRestartEngine").button({ icons: { primary: "ui-icon-power" } });
	$("#statusRestartEngine").click(function() {
		callService("engine.Stop", function() {
			callService("engine.Start", function() {				
				engine_GetStatus_update();				
			});
		});
	});
	
	$('.tdStartEngine').hide();

	$('.btnStartEngine').click(function() {
		callService("engine.Start", function() {
		});
	});

	$('.btnStopEngine').click(function() {
		callService("engine.Stop", function() {
		});

	});

	engine_GetStatus_update();
}

function engine_GetStatus_update() {
	callService("engine.GetStatus", function(xml) {
		if ($(xml).find("mode").text() == "server") {
			$(".status-toolbar").show();
		}
		if ($(xml).find("engineState").text() == "started") {
			$("#statusStartedLed").attr("src", "images/convertigo-administration-picto-bullet-green.png");
			$("#statusStoppedLed").attr("src", "images/convertigo-administration-picto-bullet-gray.png");
		} else {
			$("#statusStartedLed").attr("src", "images/convertigo-administration-picto-bullet-gray.png");
			$("#statusStoppedLed").attr("src", "images/convertigo-administration-picto-bullet-red.png");}

		$("#statusStartStopVerb").text($(xml).find("engineState").text());
		var startDate = new Date(parseInt($(xml).find("startStopDate").text()));
		$("#statusStartDate").text(startDate);
		$("#statusUptimeDays").text($(xml).find("runningElapse").attr("days"));
		$("#statusUptimeHours").text($(xml).find("runningElapse").attr("hours"));
		$("#statusUptimeMinutes").text($(xml).find("runningElapse").attr("minutes"));
		$("#statusUptimeSeconds").text($(xml).find("runningElapse").attr("seconds"));
		$("#statusProduct").text($(xml).find("version").attr("product"));
		$("#statusBuild").text($(xml).find("version").attr("build"));
		$("#statusEngine").text($(xml).find("version").attr("engine"));
		$("#statusObjects").text($(xml).find("version").attr("beans"));	
		$("#statusBuildDate").text($(xml).find("build").attr("date"));
		$("#statusBuildFilename").text($(xml).find("build").attr("filename"));	
		
		$("#statusLicenceType").text($(xml).find("version").attr("licence-type"));	
		$("#statusLicenceNumber").text($(xml).find("version").attr("licence-number"));
		var lses = $(xml).find("version").attr("licence-sessions");
		$("#statusLicenceSessions").text((lses == "") ? "n/a":lses);
		var lend = $(xml).find("version").attr("licence-end");
		if ((lend == "n/a") || (lend == "unlimited"))
			$("#statusLicenceEndDate").text(lend);
		else
			$("#statusLicenceEndDate").text((lend == "") ? "n/a":dateFormat(lend, "ddd, mmm dS yyyy"));
		
		if ($(xml).find("version").attr("licence-expired") === "true") {
			$("#statusLicenceEndDate").css("color", "red");
			$("#statusLicenceEndDate").css("font-weight", "bold");
		} else {
			$("#statusLicenceEndDate").css("color", "#595252");
			$("#statusLicenceEndDate").css("font-weight", "normal");
		} 
		$("#statusEndpoint").text($(xml).find("endpoint").text());
		
		instanceUpdateDate++;
		init_date_started_from(instanceUpdateDate);		
	});
}


function init_date_started_from(instance){
	dateUpdateRunning=true;
	var dateFrom=$("#statusStartDateRunningIE").text();
	var daysInit=parseInt(dateFrom.replace(/day(.*)/g,""));
	var hoursInit=parseInt(dateFrom.replace(/hour(.*)/g,"").replace(/(.*)day/g,"").replace(/[^\d]/g,""));
	var minutesInit=parseInt(dateFrom.replace(/minute(.*)/g,"").replace(/(.*)hour/g,"").replace(/[^\d]/g,""));
	var secondsInit=parseInt(dateFrom.replace(/(.*)minute/g,"").replace(/[^\d]/g,""));		
	var dateSince1970=new Date().getTime();
	dateSince1970-=1000*(secondsInit+(60*(minutesInit+(60*(hoursInit+(24*daysInit))))));
	engine_GetStatus_update_hour(dateSince1970,instance);
}


function engine_GetStatus_update_hour(dateSince1970,instance){	
	var newDate = new Date().getTime();	
	var diff = newDate - dateSince1970;
	diff /= 1000;//time in seconds
	var seconds = parseInt(diff % 60);
	diff /= 60;//time in minutes
	var minutes = parseInt(diff % 60);
	diff /= 60;//time in hours
	var hours = parseInt(diff % 24);
	var days = parseInt(diff / 24);//time in days
	
	if (instance == instanceUpdateDate) {
		$("#statusStartDateRunningIE").html(""+
				days+" day"+plurialSuffix(days)+", "+
				hours+" hour"+plurialSuffix(hours)+", "+
				minutes+" minute"+plurialSuffix(minutes)+", "+			
				seconds+" second"+plurialSuffix(seconds)+", "			
		);	
			
		if ($("#engine_GetStatus:visible").length > 0) {
			setTimeout(function() {			
				engine_GetStatus_update_hour(dateSince1970,instance);			
			}, 1000);
		}
	}
}

function plurialSuffix(value){
	if(value>0)
		return "s";
	return "";
}
